package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.BlobStore;
import com.alflabs.rig4.EntryPoint;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.google.auto.value.AutoValue;
import com.google.common.base.Charsets;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class Exp {
    private static final String TAG = Exp.class.getSimpleName();

    private static final String EXP_DOC_ID = "exp-doc-id";
    private static final String EXP_DEST_DIR = "exp-dest-dir";
    private static final String EXP_GA_UID = "exp-ga-uid";
    private static final String EXP_SITE_TITLE = "exp-site-title";
    private static final String EXP_SITE_BANNER = "exp-site-banner";
    /** Base URL is expected to be in the format http(s)://some.host(/folder)/ */
    private static final String EXP_SITE_BASE_URL = "exp-site-base-url";

    private static final boolean CONVERT_IMAGES = true;
    private static final boolean COMPOSITE_GRAPHICS_TO_WHITE = true;

    private final Flags mFlags;
    private final ILogger mLogger;
    private final FileOps mFileOps;
    private final Timing mTiming;
    private final GDocReader mGDocReader;
    private final BlobStore mBlobStore;
    private final HashStore mHashStore;
    private final Templater mTemplater;
    private final HtmlTransformer mHtmlTransformer;

    @Inject
    public Exp(
            Flags flags,
            ILogger logger,
            FileOps fileOps,
            Timing timing,
            GDocReader gDocReader,
            BlobStore blobStore,
            HashStore hashStore,
            Templater templater,
            HtmlTransformer htmlTransformer) {
        mFlags = flags;
        mLogger = logger;
        mFileOps = fileOps;
        mTiming = timing;
        mGDocReader = gDocReader;
        mBlobStore = blobStore;
        mHashStore = hashStore;
        mTemplater = templater;
        mHtmlTransformer = htmlTransformer;
    }

    public void declareFlags() {
        mFlags.addString(EXP_DOC_ID,      "",           "Exp gdoc id");
        mFlags.addString(EXP_DEST_DIR,    "",           "Exp dest dir");
        mFlags.addString(EXP_GA_UID,      "",           "Exp GA UID");
        mFlags.addString(EXP_SITE_TITLE,  "Site Title", "Web site title");
        mFlags.addString(EXP_SITE_BANNER, "header.jpg", "Web site banner filename");
        mFlags.addString(EXP_SITE_BASE_URL, "http://localhost/folder/", "Web site base URL");
    }

    public void start() throws IOException, URISyntaxException, InvocationTargetException, IllegalAccessException, ParseException {
        Timing.TimeAccumulator timing = mTiming.get("Total").start();
        boolean changed = checkVersionChanged();
        Index index = readIndex();
        processEntries(index.getHtmlEntries(), changed);
        timing.end();
        mTiming.printToLog();
    }

    // ---

    private boolean checkVersionChanged() throws IOException {
        final String versionKey = "version";

        String currVersion = EntryPoint.getVersion();

        boolean changed = !currVersion.equals(mHashStore.getString(versionKey));
        if (changed) {
            mHashStore.putString(versionKey, currVersion);
            mLogger.d(TAG, "Regenerating for new rig4j version: " + currVersion);
        }

        return changed;
    }


    private static final Pattern sArticleLineRe = Pattern.compile("^([a-z0-9_-]+.html)\\s+([a-zA-Z0-9_-]+)\\s*");
    private static final Pattern sBlogLineRe    = Pattern.compile("^blog\\s+([a-zA-Z0-9_-]+)\\s*");

    @NonNull
    private Index readIndex() throws IOException {
        mLogger.d(TAG, "Processing document: index");
        String indexId = mFlags.getString(EXP_DOC_ID);
        Entity entity = getGDoc(indexId, "text/plain");
        String content = new String(entity.getContent(), Charsets.UTF_8);

        List<HtmlEntry> entries = new ArrayList<>();
        List<String> blogIds = new ArrayList<>();

        for (String line : content.split("\n")) {
            line = line.trim();
            Matcher matcher = sArticleLineRe.matcher(line);
            if (matcher.find()) {
                entries.add(HtmlEntry.create(matcher.group(2), matcher.group(1)));
                continue;
            }
            matcher = sBlogLineRe.matcher(line);
            if (matcher.find()) {
                blogIds.add(matcher.group(1));
            }
        }

        return Index.create(entries, blogIds);
    }

    private void processEntries(@NonNull List<HtmlEntry> entries, boolean changed)
            throws IOException, URISyntaxException, InvocationTargetException, IllegalAccessException, ParseException {
        String destDir = mFlags.getString(EXP_DEST_DIR);
        mLogger.d(TAG, "        Site URL: " + mFlags.getString(EXP_SITE_BASE_URL));
        mLogger.d(TAG, "     Destination: " + destDir);

        for (HtmlEntry entry : entries) {
            String destName = entry.getDestName();
            File destFile = new File(destDir, destName);

            mLogger.d(TAG, "Process document: " + destName);

            Entity entity = getGDoc(entry.getFileId(), "text/html");
            byte[] docContent = entity.getContent();
            String title = entity.getMetadata().getTitle();
            boolean keepExisting = !changed && entity.isUpdateToDate() && mFileOps.isFile(destFile);

            String htmlHashKey = "html-hash-" + destFile.getPath();
            if (keepExisting) {
                String htmlHash = mHashStore.getString(htmlHashKey);
                keepExisting = htmlHash != null && htmlHash.equals(entity.getMetadata().getContentHash());
            }

            if (keepExisting) {
                mLogger.d(TAG, "   Keep existing: " + destName);
            } else {
                String htmlBody = processHtml(docContent, title, destFile);

                Templater.TemplateData data = Templater.TemplateData.create(
                        "", // css
                        mFlags.getString(EXP_GA_UID),
                        title,
                        destName,
                        mFlags.getString(EXP_SITE_TITLE),
                        mFlags.getString(EXP_SITE_BASE_URL),
                        mFlags.getString(EXP_SITE_BANNER),
                        htmlBody);
                String html = mTemplater.generate(data);
                byte[] htmlContent = html.getBytes(Charsets.UTF_8);

                mFileOps.createParentDirs(destFile);
                mFileOps.writeBytes(htmlContent, destFile);
                mHashStore.putString(htmlHashKey, entity.getMetadata().getContentHash());
            }
        }
    }

    @NonNull
    private String processHtml(@NonNull byte[] content, @NonNull String title, File destFile) throws IOException, URISyntaxException {
        String htmlBody = mHtmlTransformer.simplify(
                content,
                new HtmlTransformer.Callback() {
                    @Override
                    public String processDrawing(String id, int width, int height) throws IOException {
                        return Exp.this.downloadDrawing(id, destFile, width, height);
                    }

                    @Override
                    public String processImage(URI uri, int width, int height) throws IOException {
                        return Exp.this.downloadImage(uri, destFile, width, height);
                    }
                });
        return htmlBody;
    }

    private String downloadDrawing(String id, File destFile, int width, int height) throws IOException {
        Timing.TimeAccumulator timing = mTiming.get("Html.Drawing").start();
        try {
            // Note: There is no Drive API for embedded drawings.
            // Experience shows that we can't even get the metadata like for a normal gdoc.
            // Instead we just download them every time the doc is generated.

            String extension = "png";
            String destName = destFile.getName();
            destName = destName.replace(".html", "_");
            destName = destName.replace(".", "_");
            destName += DigestUtils.shaHex("_drawing_" + id) + "d";

            mLogger.d(TAG, "         Drawing: " + destName + ", " + width + "x" + height);

            URL url = new URL("https://docs.google.com/drawings/d/" + id + "/export/" + extension);
            InputStream stream = mGDocReader.getDataByUrl(url);
            BufferedImage image = ImageIO.read(stream);

            final String keyImageHash = destName;
            final String keyImageName = destName + "_name";
            String imageHash = computeImageHash(image, width, height);
            String storedImageHash = mHashStore.getString(keyImageHash);
            if (imageHash.equals(storedImageHash)) {
                String storedImageName = mHashStore.getString(keyImageName);
                if (storedImageName != null) {
                    File actualFile = new File(destFile.getParentFile(), storedImageName);
                    if (mFileOps.isFile(actualFile)) {
                        mLogger.d(TAG, "           Reuse: " + storedImageName);
                        return storedImageName;
                    }
                }
            }

            if (width > 0 && height > 0) {
                image = cropAndResizeDrawing(image, width, height);
            }

            if (CONVERT_IMAGES) {
                destName = writeImageJpgOrPng(destFile, destName, image, width, height);
            } else {
                destName += "." + extension;
                destFile = new File(destFile.getParentFile(), destName);
                ImageIO.write(image, extension, destFile);
            }

            mHashStore.putString(keyImageHash, imageHash);
            mHashStore.putString(keyImageName, destName);

            return destName;
        } finally {
            timing.end();
        }
    }

    private String computeImageHash(BufferedImage image, int width, int height) {
        Timing.TimeAccumulator timing = mTiming.get("Html.Image.Hash").start();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }

        String size = "w" + width + "h" + height + ".";
        digest.update(size.getBytes(Charsets.UTF_8));

        WritableRaster raster = image.getRaster();
        DataBufferByte data = (DataBufferByte) raster.getDataBuffer();
        digest.update(data.getData());

        String hash = new String(Hex.encodeHex(digest.digest()));
        timing.end();
        return hash;
    }

    private BufferedImage cropAndResizeDrawing(BufferedImage image, int width, int height) throws IOException {
        Timing.TimeAccumulator timing = mTiming.get("Html.Drawing.Crop").start();
        int srcw = image.getWidth();
        int srch = image.getHeight();

        WritableRaster raster = image.getRaster();
        ColorModel model = image.getColorModel();

        int x1 = srcw;
        int y1 = srch;
        int x2 = 0;
        int y2 = 0;

        for (int k = 0, y = 0; y < srch; y++) {
            for (int x = 0; x < srcw; x++) {
                Object elements = raster.getDataElements(x, y, null);
                int a = model.getAlpha(elements);

                if (a != 0) {
                    if (x < x1) {
                        x1 = x;
                    } else if (x > x2) {
                        x2 = x;
                    }
                    if (y < y1) {
                        y1 = y;
                    } else if (y > y2) {
                        y2 = y;
                    }
                }
            }
        }

        // If we desired size is larger, then try to center it
        int destw = x2 - x1 + 1;
        int desth = y2 - y1 + 1;
        if (width <= srcw && width > destw && height <= srch && height > desth) {
            double w2 = width / 2.;
            double h2 = height / 2.;
            double cx = x1 + destw / 2.;
            double cy = y1 + desth / 2.;
            if (cx - w2 < 0) {
                x1 = 0;
            } else if (cx + w2 > srcw) {
                x1 = srcw - width;
            }
            destw = width;

            if (cy - h2 < 0) {
                y1 = 0;
            } else if (cy + h2 > srch) {
                y1 = srch - height;
            }
            desth = height;
        }

        image = image.getSubimage(x1, y1, destw, desth);

        if (destw > width && desth > height) {
            image = Thumbnails.of(image).size(width, height).asBufferedImage();
            destw = image.getWidth();
            desth = image.getHeight();
        }

        if (COMPOSITE_GRAPHICS_TO_WHITE) {
            BufferedImage white = new BufferedImage(destw, desth, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = white.createGraphics();
            g2d.drawImage(image, 0, 0, Color.WHITE, null /*observer*/ );
            image = white;
        }

        mLogger.d(TAG, String.format("        Resizing: from [%dx%d] to (%dx%d)+[%dx%d]",
                srcw, srch, x1, y1, destw, desth));

        timing.end();
        return image;
    }

    private String downloadImage(URI uri, File destFile, int width, int height) throws IOException {
        Timing.TimeAccumulator timing = mTiming.get("Html.Image").start();
        try {
            String path = uri.getPath();

            String destName = destFile.getName();
            destName = destName.replace(".html", "_");
            destName = destName.replace(".", "_");
            destName += DigestUtils.shaHex("_image_" + path) + "i";
            mLogger.d(TAG, "         Image  : " + destName + ", " + width + "x" + height);

            if (CONVERT_IMAGES) {
                // Download the image, then compares whether a PNG or JPG would be more compact.
                //
                // The gdoc exported images seem to always be PNG, even when copied from photos.
                // Drawings are fairly compact in PNG, but not photos.

                BufferedImage image = ImageIO.read(uri.toURL());

                final String keyImageHash = destName;
                final String keyImageName = destName + "_name";
                String imageHash = computeImageHash(image, width, height);
                String storedImageHash = mHashStore.getString(keyImageHash);
                if (imageHash.equals(storedImageHash)) {
                    String storedImageName = mHashStore.getString(keyImageName);
                    if (storedImageName != null) {
                        File actualFile = new File(destFile.getParentFile(), storedImageName);
                        if (mFileOps.isFile(actualFile)) {
                            mLogger.d(TAG, "           Reuse: " + storedImageName);
                            return storedImageName;
                        }
                    }
                }

                destName = writeImageJpgOrPng(destFile, destName, image, width, height);

                mHashStore.putString(keyImageHash, imageHash);
                mHashStore.putString(keyImageName, destName);

            } else {
                // OBSOLETE. Can be removed now.

                // The stuff from gdocs appears to be mostly (if not always) PNG but there's
                // no way to really know before actually downloading it.
                String extension = "png";
                destName += "." + extension;
                destFile = new File(destFile.getParentFile(), destName);

                // Download as-is and do not convert
                ByteSource reader = Resources.asByteSource(uri.toURL());
                ByteSink writer = Files.asByteSink(destFile);
                reader.copyTo(writer);
            }

            return destName;
        } finally {
            timing.end();
        }
    }

    /**
     * Writes the image to a file destDir/destName, either as PNG or JPG, whichever is more compact.
     * <p/>
     * If both width and height are zero, the image size is used.<br/>
     * Otherwise, a zero value is computed to match a scaled aspect ratio.<br/>
     * A typical case is to have width=some value and height=0, in which case height is recomputed
     * to match the scaled width.
     * <p/>
     * Side effect: this loads both the original image and the generated JPG and PNG streams in
     * memory. It also means the original image is always decoded then re-encoded, even if it's
     * in the same format.
     *
     * @param destDir Destination direction.
     * @param destName Base name (without the .extension)
     * @param image Image to write
     * @param width Desired width or 0
     * @param height Desired heith or 0
     * @return The final destName with extension
     * @throws IOException
     */
    private String writeImageJpgOrPng(File destDir, String destName, BufferedImage image, int width, int height) throws IOException {
        Timing.TimeAccumulator timing = mTiming.get("Html.JpegOrPng").start();
        int w = image.getWidth();
        int h = image.getHeight();

        if (width > 0 && height <= 0) {
            height = (int) Math.round(h * (double) w / (double) width);
        } else if (width <= 0 && height <= 0) {
            width = image.getWidth();
            height = image.getHeight();
        }

        ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
        ByteArrayOutputStream jpgStream = new ByteArrayOutputStream();
        Thumbnails.of(image)
                .size(width, height)
                .antialiasing(Antialiasing.ON)
                .outputFormat("png")
                .toOutputStream(pngStream);
        Thumbnails.of(image)
                .size(width, height)
                .antialiasing(Antialiasing.ON)
                .outputFormat("jpg")
                .outputQuality(0.9f)
                .toOutputStream(jpgStream);

        pngStream.close();
        jpgStream.close();

        int pngSize = pngStream.size();
        int jpgSize = jpgStream.size();
        ByteArrayOutputStream result = pngSize < jpgSize ? pngStream : jpgStream;
        String extension = pngSize < jpgSize ? "png" : "jpg";
        destName += "." + extension;
        File destFile = new File(destDir.getParentFile(), destName);

        mLogger.d(TAG, "         Writing: " + destName
                + ", " + width + "x" + height
                + ", [png: " + pngSize + " " + (pngSize < jpgSize ? "<" : ">") + " jpg: " + jpgSize + "]");
        ByteSink writer = Files.asByteSink(destFile);
        writer.write(result.toByteArray());
        timing.end();
        return destName;
    }


    // ---

    @Null
    private Entity getGDoc(@NonNull String fileId, @NonNull String mimeType) {
        final String metadataKey = "gdoc-hash-" + fileId;
        final String contentKey = "gdoc-content-" + fileId + "-" + mimeType;

        // Known implementation issue: the gdoc API calls to retrieve the file content
        // and the freshness hash are not part of an atomic call. There's a chance the
        // server-side data has changed when retrieving both. However we get the hash
        // first (say v1) and later get the content (say v2). In the store we keep
        // hash(v1) + data(v2). Next time this method is checked, it will check the hash
        // and get hash(v2) from the server. It does not match and thus retrieves again
        // data(v2).
        // If synchronization were important, a way to mitigate this is to get the hash
        // to check the freshness. When getting the data, get the hash again and retry
        // few times if it keeps changing.
        // In the current context of rig with very little server-side changes and a daily
        // check, the current flaw is acceptable enough.

        // Check store data
        byte[] content = null;
        try {
            content = mBlobStore.getBytes(contentKey);
        } catch (IOException ignore) {}

        GDocMetadata metadata;
        try {
            metadata = mGDocReader.getMetadataById(fileId);
        } catch (IOException e) {
            mLogger.d(TAG, "Get metadata failed for " + fileId);
            return null;
        }

        boolean updateToDate = false;
        if (content != null) {
            // Check freshness
            try {
                String storeHash = mHashStore.getString(metadataKey);
                updateToDate = metadata.getContentHash().equals(storeHash);
            } catch (IOException ignore) {}
        }

        if (!updateToDate) {
            try {
                mLogger.d(TAG, "        Fetching: " + fileId);
                content = mGDocReader.readFileById(fileId, mimeType);

                if (content != null) {
                    // Update the store
                    mBlobStore.putBytes(contentKey, content);
                    mHashStore.putString(metadataKey, metadata.getContentHash());
                }
            } catch (IOException ignore) {}
        }

        return new Entity(metadata, updateToDate, content);
    }

    @AutoValue
    static abstract class Index {
        public static Index create(@NonNull List<HtmlEntry> htmlEntries, @NonNull List<String> blogIds) {
            return new AutoValue_Exp_Index(htmlEntries, blogIds);
        }

        abstract List<HtmlEntry> getHtmlEntries();
        abstract List<String> getBlogIds();
    }

    @AutoValue
    static abstract class HtmlEntry {
        public static HtmlEntry create(@NonNull String fileId, @NonNull String destName) {
            return new AutoValue_Exp_HtmlEntry(fileId, destName);
        }

        abstract String getFileId();
        abstract String getDestName();
    }

    static class Entity {
        private final GDocMetadata mMetadata;
        private final boolean mUpdateToDate;
        private final byte[] mContent;

        public Entity(GDocMetadata metadata, boolean updateToDate, byte[] content) {
            mMetadata = metadata;
            mUpdateToDate = updateToDate;
            mContent = content;
        }

        public GDocMetadata getMetadata() {
            return mMetadata;
        }

        public boolean isUpdateToDate() {
            return mUpdateToDate;
        }

        public byte[] getContent() {
            return mContent;
        }
    }


}
