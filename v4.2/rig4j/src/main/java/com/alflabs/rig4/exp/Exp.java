package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.BlobStore;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.google.auto.value.AutoValue;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.digest.DigestUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
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

    private final Flags mFlags;
    private final ILogger mLogger;
    private final FileOps mFileOps;
    private final GDocReader mGDocReader;
    private final BlobStore mBlobStore;
    private final HtmlTransformer mHtmlTransformer;

    @Inject
    public Exp(
            Flags flags,
            ILogger logger,
            FileOps fileOps,
            GDocReader gDocReader,
            BlobStore blobStore,
            HtmlTransformer htmlTransformer) {
        mFlags = flags;
        mLogger = logger;
        mFileOps = fileOps;
        mGDocReader = gDocReader;
        mBlobStore = blobStore;
        mHtmlTransformer = htmlTransformer;
    }

    public void declareFlags() {
        mFlags.addString(EXP_DOC_ID, "", null);
        mFlags.addString(EXP_DEST_DIR, "", null);
        mFlags.addString(EXP_GA_UID, "", null);
    }

    public void start() throws IOException, URISyntaxException {
        List<HtmlEntry> entries = readIndex();
        processEntries(entries);
    }

    // ---

    private Pattern indexLineRe = Pattern.compile("^([a-z0-9_-]+.html)\\s+([a-zA-Z0-9_-]+)\\s*");

    @NonNull
    private List<HtmlEntry> readIndex() throws IOException {
        mLogger.d(TAG, "Processing document: index");
        String indexId = mFlags.getString(EXP_DOC_ID);
        GDocMetadata gdoc = getGDoc(indexId, "text/plain");
        String content = new String(gdoc.getContent(), Charsets.UTF_8);

        List<HtmlEntry> entries = new ArrayList<>();

        for (String line : content.split("\n")) {
            line = line.trim();
            Matcher matcher = indexLineRe.matcher(line);
            if (matcher.find()) {
                entries.add(HtmlEntry.create(matcher.group(2), matcher.group(1)));
            }
        }

        return entries;
    }

    private void processEntries(@NonNull List<HtmlEntry> entries) throws IOException, URISyntaxException {
        String destDir = mFlags.getString(EXP_DEST_DIR);

        for (HtmlEntry entry : entries) {
            String destName = entry.getDestName();
            File destFile = new File(destDir, destName);

            mLogger.d(TAG, "Process document: " + destName);

            GDocMetadata gdoc = getGDoc(entry.getFileId(), "text/html");
            byte[] content = gdoc.getContent();
            String title = gdoc.getTitle();

            content = processHtml(content, title, destFile);

            mFileOps.createParentDirs(destFile);
            mFileOps.writeBytes(content, destFile);
        }
    }

    @NonNull
    private byte[] processHtml(@NonNull byte[] content, @NonNull String title, File destFile) throws IOException, URISyntaxException {
        content = mHtmlTransformer.simplify(
                content,
                (id, width, height) -> downloadDrawing(id, destFile, width, height));
        return content;
    }

    private String downloadDrawing(String id, File destFile, int width, int height) throws IOException {
        // Note: There is no Drive API for embedded drawings.
        // Experience shows that we can't even get the metadata like for a normal gdoc.
        // Instead we just download them everytime the doc is generated.

        String extension = "png";
        String destName = destFile.getName();
        destName = destName.replace(".html", "_");
        destName = destName.replace(".", "_");
        destName += DigestUtils.shaHex("_drawing_" + id) + "." + extension;
        destFile = new File(destFile.getParentFile(), destName);

        mLogger.d(TAG, "     Downloading: " + destName + " [" + width + "x" + height + "]");

        URL url = new URL("https://docs.google.com/drawings/d/" + id + "/export/" + extension);
        InputStream stream = mGDocReader.getDataByUrl(url);
        BufferedImage image = ImageIO.read(stream);

        if (width > 0 && height > 0) {
            image = resizeImage(image, width, height);
        }

        ImageIO.write(image, extension, destFile);
        return destName;
    }

    private BufferedImage resizeImage(BufferedImage image, int width, int height) throws IOException {
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

        mLogger.d(TAG, String.format("resize [%dx%d]: src [%dx%d], sub image (%dx%d)+[%dx%d]",
                width, height, srcw, srch, x1, y1, destw, desth));

        return image;
    }


    // ---

    @Null
    private GDocMetadata getGDoc(@NonNull String fileId, @NonNull String mimeType) {
        final String metadataKey = "hash-" + fileId;
        final String contentKey = "content-" + fileId + "-" + mimeType;

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

        GDocMetadata gdoc;
        try {
            gdoc = mGDocReader.getMetadataById(fileId);
        } catch (IOException e) {
            mLogger.d(TAG, "Get metadata failed for " + fileId);
            return null;
        }

        boolean retrieve = true;
        if (content != null) {
            // Check freshness
            try {
                String storeHash = mBlobStore.getString(metadataKey);
                retrieve = !gdoc.getContentHash().equals(storeHash);
            } catch (IOException ignore) {}
        }

        if (retrieve) {
            try {
                mLogger.d(TAG, "        Fetching: " + fileId);
                content = mGDocReader.readFileById(fileId, mimeType);

                if (content != null) {
                    // Update the store
                    mBlobStore.putBytes(contentKey, content);
                    mBlobStore.putString(metadataKey, gdoc.getContentHash());
                }
            } catch (IOException ignore) {}
        }

        gdoc.setContent(content);
        return gdoc;
    }

    @AutoValue
    static abstract class HtmlEntry {
        public static HtmlEntry create(@NonNull String fileId, @NonNull String destName) {
            return new AutoValue_Exp_HtmlEntry(fileId, destName);
        }

        abstract String getFileId();
        abstract String getDestName();
    }
}
