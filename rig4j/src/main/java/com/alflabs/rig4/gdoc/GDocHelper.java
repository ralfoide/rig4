package com.alflabs.rig4.gdoc;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.BlobStore;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.struct.GDocEntity;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GDocHelper {
    private static final String TAG = GDocHelper.class.getSimpleName();

    private static final boolean COMPOSITE_GRAPHICS_TO_WHITE = true;

    private final ILogger mLogger;
    private final FileOps mFileOps;
    private final Timing mTiming;
    private final GDocReader mGDocReader;
    private final BlobStore mBlobStore;
    private final HashStore mHashStore;

    @Inject
    public GDocHelper(
            ILogger logger,
            FileOps fileOps,
            Timing timing,
            GDocReader gDocReader,
            BlobStore blobStore,
            HashStore hashStore) {
        mLogger = logger;
        mFileOps = fileOps;
        mTiming = timing;
        mGDocReader = gDocReader;
        mBlobStore = blobStore;
        mHashStore = hashStore;
    }

    public String downloadDrawing(String id, File destFile, int width, int height, boolean useCache) throws IOException {
        Timing.TimeAccumulator timing = mTiming.get("Html.Drawing").start();
        try {
            String cacheKey = String.format("dl_drawing_fullpath_I%s_D%s_W%d_H%d", id, destFile.getPath(), width, height);
            if (useCache) {
                String cachedFilePath = mHashStore.getString(cacheKey);
                if (cachedFilePath != null) {
                    File cachedFile = new File(cachedFilePath);
                    if (mFileOps.isFile(cachedFile)) {
                        String cachedName = cachedFile.getName();
                        mLogger.d(TAG, "         Cached : " + cachedName + ", " + width + "x" + height);
                        return cachedName;
                    }
                }
            }

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
                        mHashStore.putString(cacheKey, actualFile.getPath());
                        return storedImageName;
                    }
                }
            }

            if (width > 0 && height > 0) {
                image = cropAndResizeDrawing(image, width, height);
            }

            File imgFile = writeImageJpgOrPng(destFile, destName, image, width, height);
            destName = imgFile.getName();

            mHashStore.putString(cacheKey, imgFile.getPath());
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

    public String downloadImage(URI uri, File destFile, int width, int height, boolean useCache) throws IOException {
        Timing.TimeAccumulator timing = mTiming.get("Html.Image").start();
        try {
            String cacheKey = String.format("dl_image_fullpath_U%s_D%s_W%d_H%d", uri, destFile.getPath(), width, height);
            if (useCache) {
                String cachedFilePath = mHashStore.getString(cacheKey);
                if (cachedFilePath != null) {
                    File cachedFile = new File(cachedFilePath);
                    if (mFileOps.isFile(cachedFile)) {
                        String cachedName = cachedFile.getName();
                        mLogger.d(TAG, "         Cached : " + cachedName + ", " + width + "x" + height);
                        return cachedName;
                    }
                }
            }

            String path = uri.getPath();

            String destName = destFile.getName();
            destName = destName.replace(".html", "_");
            destName = destName.replace(".", "_");
            destName += DigestUtils.shaHex("_image_" + path) + "i";
            mLogger.d(TAG, "         Image  : " + destName + ", " + width + "x" + height);

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
                        mHashStore.putString(cacheKey, actualFile.getPath());
                        return storedImageName;
                    }
                }
            }

            File imgFile = writeImageJpgOrPng(destFile, destName, image, width, height);
            destName = imgFile.getName();

            mHashStore.putString(cacheKey, imgFile.getPath());
            mHashStore.putString(keyImageHash, imageHash);
            mHashStore.putString(keyImageName, destName);

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
     * @return The generated file full path with extension
     * @throws IOException
     */
    private File writeImageJpgOrPng(File destDir, String destName, BufferedImage image, int width, int height) throws IOException {
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
        return destFile;
    }


    // ---

    /**
     * Retrieves both the content and the metadata for the given GDoc id immediately.
     * The freshness "up-to-date" flag is computed using the metadata.
     * <p/>
     * If the entity is up-to-date, the content from the blog store is used (if available).
     * Otherwise the content is fetched immediately.
     * <p/>
     * Which one to use: <br/>
     * - {@link #getGDocSync} when the caller is going to fetch and use the content no matter what.
     * <br/>
     * - {@link #getGDocAsync} when the caller doesn't need the content if the metadata is up-to-date.
     */
    @Null
    public GDocEntity getGDocSync(@NonNull String fileId, @NonNull String mimeType) {
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
            mLogger.d(TAG, "If this fails, try re-issuing a new OAuth2 token (e.g. gdoc-store/credentials).");
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
                Preconditions.checkNotNull(content); // fail fast
                mLogger.d(TAG, "        Fetched sync size: " + content.length);

                // Update the store
                mBlobStore.putBytes(contentKey, content);
                mHashStore.putString(metadataKey, metadata.getContentHash());
            } catch (IOException e) {
                mLogger.d(TAG, "        Fetching " + mimeType + " sync failed", e);
                throw new RuntimeException(e);
            }
        }

        return new GDocEntity(metadata, updateToDate, content);
    }

    /**
     * Retrieves only the metadata for the given GDoc id immediately.
     * Content retrieval is deferred till actually needed.
     * The freshness "up-to-date" flag is computed only using the metadata.
     * <p/>
     * The content fetcher also tries to use the blog store's content if available and the
     * metadata indicates the content should be up-to-date.
     * <p/>
     * The major difference with {@link #getGDocSync(String, String)} is that the content
     * fetch does not happen immediately (whether it's from the blog store or gdoc), nor
     * are the blog/hash stores updated immediately.
     * <p/>
     * Which one to use: <br/>
     * - {@link #getGDocSync} when the caller is going to fetch and use the content no matter what.
     * <br/>
     * - {@link #getGDocAsync} when the caller doesn't need the content if the metadata is up-to-date.
     */
    @Null
    public GDocEntity getGDocAsync(@NonNull String fileId, @NonNull String mimeType) {
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

        GDocMetadata metadata;
        try {
            metadata = mGDocReader.getMetadataById(fileId);
        } catch (IOException e) {
            mLogger.d(TAG, "Get metadata failed for " + fileId);
            return null;
        }

        boolean updateToDate = false;
        // Check freshness using metadata only.
        try {
            String storeHash = mHashStore.getString(metadataKey);
            updateToDate = metadata.getContentHash().equals(storeHash);
        } catch (IOException ignore) {}

        GDocEntity.ContentFetcher fetcher = (entity) -> {
            byte[] content = null;
            if (entity.isUpdateToDate()) {
                try {
                    content = mBlobStore.getBytes(contentKey);
                } catch (IOException ignore) {
                }
            }
            if (content == null) {
                try {
                    mLogger.d(TAG, "        Fetching " + mimeType + ": " + fileId);
                    // 2023-06-08 Use file.export via readFileById() started to fail.
                    // Instead, we're not using the direct exportLinks URL if available.
                    String exportLink = metadata.getExportLinks().get(mimeType);
                    if (exportLink != null) {
                        URL url = new URL(exportLink);
                        try (InputStream inputStream = mGDocReader.getDataByUrl(url)) {
                            content = ByteStreams.toByteArray(inputStream);
                        }
                    } else {
                        // Legacy.
                        content = mGDocReader.readFileById(fileId, mimeType);
                    }
                    Preconditions.checkNotNull(content); // fail fast
                    mLogger.d(TAG, "        Fetched async size: " + content.length);
                } catch (IOException e) {
                    mLogger.d(TAG, "        Fetching async failed", e);
                    throw new RuntimeException(e);
                }
            }
            return content;
        };

        GDocEntity.Syncer syncToStore = (entity) -> {
            if (entity.isUpdateToDate()) {
                return;
            }
            try {
                if (entity.isContentFetched() && entity.getContent() != null) {
                    mBlobStore.putBytes(contentKey, entity.getContent());
                }
                mHashStore.putString(metadataKey, entity.getMetadata().getContentHash());
            } catch (IOException e) {
                mLogger.d(TAG, "syncToStore failed", e);
            }
        };

        return new GDocEntity(metadata, updateToDate, fetcher, syncToStore);
    }

}
