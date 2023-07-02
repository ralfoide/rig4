package com.alflabs.rig4k.exp

import com.alflabs.rig4k.common.BlobStore
import com.alflabs.rig4k.common.HashStore
import com.alflabs.rig4k.common.Timing
import com.alflabs.rig4k.dl.GDocMetadata
import com.alflabs.rig4k.dl.GDocReader
import com.alflabs.utils.FileOps
import com.alflabs.utils.ILogger
import com.google.common.base.Charsets
import com.google.common.base.Preconditions
import com.google.common.io.ByteSink
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.resizers.configurations.Antialiasing
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.DataBufferByte
import java.awt.image.WritableRaster
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.imageio.ImageIO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpGDocHelper @Inject constructor(
    private val logger: ILogger,
    private val fileOps: FileOps,
    private val timing: Timing,
    private val gDocReader: GDocReader,
    private val blobStore: BlobStore,
    private val hashStore: HashStore
) {
    companion object {
        private val TAG = ExpGDocHelper::class.java.simpleName
        private const val COMPOSITE_GRAPHICS_TO_WHITE = true
    }

    @Throws(IOException::class)
    fun downloadDrawing(
        id: String,
        destFile: File,
        width: Int,
        height: Int,
        useCache: Boolean
    ): String {
        val timing = timing.get("Html.Drawing").start()
        return try {
            val cacheKey = String.format(
                "dl_drawing_fullpath_I%s_D%s_W%d_H%d",
                id,
                destFile.path,
                width,
                height
            )
            if (useCache) {
                val cachedFilePath: String? = hashStore.getString(cacheKey)
                if (cachedFilePath != null) {
                    val cachedFile = File(cachedFilePath)
                    if (fileOps.isFile(cachedFile)) {
                        val cachedName = cachedFile.name
                        logger.d(
                            TAG,
                            "         Cached : " + cachedName + ", " + width + "x" + height
                        )
                        return cachedName
                    }
                }
            }

            // Note: There is no Drive API for embedded drawings.
            // Experience shows that we can't even get the metadata like for a normal gdoc.
            // Instead we just download them every time the doc is generated.
            val extension = "png"
            var destName = destFile.name
            destName = destName.replace(".html", "_")
            destName = destName.replace(".", "_")
            destName += DigestUtils.sha1Hex("_drawing_$id") + "d"
            logger.d(
                TAG,
                "         Drawing: " + destName + ", " + width + "x" + height
            )
            val url = URL("https://docs.google.com/drawings/d/$id/export/$extension")
            val stream = gDocReader.getDataByUrl(url)
            var image: BufferedImage = ImageIO.read(stream)
            val keyImageHash = destName
            val keyImageName = destName + "_name"
            val imageHash = computeImageHash(image, width, height)
            val storedImageHash: String? = hashStore.getString(keyImageHash)
            if (imageHash == storedImageHash) {
                val storedImageName: String? = hashStore.getString(keyImageName)
                if (storedImageName != null) {
                    val actualFile = File(destFile.parentFile, storedImageName)
                    if (fileOps.isFile(actualFile)) {
                        hashStore.putString(cacheKey, actualFile.path)
                        return storedImageName
                    }
                }
            }
            if (width > 0 && height > 0) {
                image = cropAndResizeDrawing(image, width, height)
            }
            val imgFile = writeImageJpgOrPng(destFile, destName, image, width, height)
            destName = imgFile.name
            hashStore.putString(cacheKey, imgFile.path)
            hashStore.putString(keyImageHash, imageHash)
            hashStore.putString(keyImageName, destName)
            destName
        } finally {
            timing.end()
        }
    }

    private fun computeImageHash(image: BufferedImage, width: Int, height: Int): String {
        val timing = timing.get("Html.Image.Hash").start()
        val digest: MessageDigest = try {
            MessageDigest.getInstance("SHA")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e.message)
        }
        val size = "w" + width + "h" + height + "."
        digest.update(size.toByteArray(Charsets.UTF_8))
        val raster: WritableRaster = image.raster
        val data: DataBufferByte = raster.dataBuffer as DataBufferByte
        digest.update(data.data)
        val hash: String = String(Hex.encodeHex(digest.digest()))
        timing.end()
        return hash
    }

    @Throws(IOException::class)
    private fun cropAndResizeDrawing(image: BufferedImage, width: Int, height: Int): BufferedImage {
        val timing = timing.get("Html.Drawing.Crop").start()
        val srcw: Int = image.width
        val srch: Int = image.height
        val raster: WritableRaster = image.raster
        val model: ColorModel = image.colorModel
        var x1 = srcw
        var y1 = srch
        var x2 = 0
        var y2 = 0
        val k = 0
        var y = 0
        while (y < srch) {
            for (x in 0 until srcw) {
                val elements: Any = raster.getDataElements(x, y, null)
                val a = model.getAlpha(elements)
                if (a != 0) {
                    if (x < x1) {
                        x1 = x
                    } else if (x > x2) {
                        x2 = x
                    }
                    if (y < y1) {
                        y1 = y
                    } else if (y > y2) {
                        y2 = y
                    }
                }
            }
            y++
        }

        // If we desired size is larger, then try to center it
        var destw = x2 - x1 + 1
        var desth = y2 - y1 + 1
        if (width <= srcw && width > destw && height <= srch && height > desth) {
            val w2 = width / 2.0
            val h2 = height / 2.0
            val cx = x1 + destw / 2.0
            val cy = y1 + desth / 2.0
            if (cx - w2 < 0) {
                x1 = 0
            } else if (cx + w2 > srcw) {
                x1 = srcw - width
            }
            destw = width
            if (cy - h2 < 0) {
                y1 = 0
            } else if (cy + h2 > srch) {
                y1 = srch - height
            }
            desth = height
        }
        var subImage = image.getSubimage(x1, y1, destw, desth)
        if (destw > width && desth > height) {
            subImage = Thumbnails.of(subImage).size(width, height).asBufferedImage()
            destw = subImage.width
            desth = subImage.height
        }
        if (COMPOSITE_GRAPHICS_TO_WHITE) {
            val white = BufferedImage(destw, desth, BufferedImage.TYPE_INT_ARGB)
            val g2d: Graphics2D = white.createGraphics()
            g2d.drawImage(subImage, 0, 0, Color.WHITE, null /*observer*/)
            subImage = white
        }
        logger.d(
            TAG, String.format(
                "        Resizing: from [%dx%d] to (%dx%d)+[%dx%d]",
                srcw, srch, x1, y1, destw, desth
            )
        )
        timing.end()
        return subImage
    }

    @Throws(IOException::class)
    fun downloadImage(
        uri: URI,
        destFile: File,
        width: Int,
        height: Int,
        useCache: Boolean
    ): String {
        val timing = timing.get("Html.Image").start()
        return try {
            val cacheKey = String.format(
                "dl_image_fullpath_U%s_D%s_W%d_H%d",
                uri,
                destFile.path,
                width,
                height
            )
            if (useCache) {
                val cachedFilePath: String? = hashStore.getString(cacheKey)
                if (cachedFilePath != null) {
                    val cachedFile = File(cachedFilePath)
                    if (fileOps.isFile(cachedFile)) {
                        val cachedName = cachedFile.name
                        logger.d(
                            TAG,
                            "         Cached : " + cachedName + ", " + width + "x" + height
                        )
                        return cachedName
                    }
                }
            }
            val path = uri.path
            var destName = destFile.name
            destName = destName.replace(".html", "_")
            destName = destName.replace(".", "_")
            destName += DigestUtils.sha1Hex("_image_$path") + "i"
            logger.d(
                TAG,
                "         Image  : " + destName + ", " + width + "x" + height
            )

            // Download the image, then compares whether a PNG or JPG would be more compact.
            //
            // The gdoc exported images seem to always be PNG, even when copied from photos.
            // Drawings are fairly compact in PNG, but not photos.
            val image: BufferedImage = ImageIO.read(uri.toURL())
            val keyImageHash = destName
            val keyImageName = destName + "_name"
            val imageHash = computeImageHash(image, width, height)
            val storedImageHash: String? = hashStore.getString(keyImageHash)
            if (imageHash == storedImageHash) {
                val storedImageName: String? = hashStore.getString(keyImageName)
                if (storedImageName != null) {
                    val actualFile = File(destFile.parentFile, storedImageName)
                    if (fileOps.isFile(actualFile)) {
                        hashStore.putString(cacheKey, actualFile.path)
                        return storedImageName
                    }
                }
            }
            val imgFile = writeImageJpgOrPng(destFile, destName, image, width, height)
            destName = imgFile.name
            hashStore.putString(cacheKey, imgFile.path)
            hashStore.putString(keyImageHash, imageHash)
            hashStore.putString(keyImageName, destName)
            destName
        } finally {
            timing.end()
        }
    }

    /**
     * Writes the image to a file destDir/destName, either as PNG or JPG, whichever is more compact.
     *
     *
     * If both width and height are zero, the image size is used.<br></br>
     * Otherwise, a zero value is computed to match a scaled aspect ratio.<br></br>
     * A typical case is to have width=some value and height=0, in which case height is recomputed
     * to match the scaled width.
     *
     *
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
    @Throws(IOException::class)
    private fun writeImageJpgOrPng(
        destDir: File,
        destName: String,
        image: BufferedImage,
        width: Int,
        height: Int
    ): File {
        var _destName = destName
        var _width = width
        var _height = height
        val timing = timing.get("Html.JpegOrPng").start()
        val w: Int = image.width
        val h: Int = image.height
        if (_width > 0 && _height <= 0) {
            _height = Math.round(h * w.toDouble() / _width.toDouble()).toInt()
        } else if (_width <= 0 && _height <= 0) {
            _width = image.width
            _height = image.height
        }
        val pngStream = ByteArrayOutputStream()
        val jpgStream = ByteArrayOutputStream()
        Thumbnails.of(image)
            .size(_width, _height)
            .antialiasing(Antialiasing.ON)
            .outputFormat("png")
            .toOutputStream(pngStream)
        Thumbnails.of(image)
            .size(_width, _height)
            .antialiasing(Antialiasing.ON)
            .outputFormat("jpg")
            .outputQuality(0.9f)
            .toOutputStream(jpgStream)
        pngStream.close()
        jpgStream.close()
        val pngSize: Int = pngStream.size()
        val jpgSize: Int = jpgStream.size()
        val result: ByteArrayOutputStream = if (pngSize < jpgSize) pngStream else jpgStream
        val extension = if (pngSize < jpgSize) "png" else "jpg"
        _destName += ".$extension"
        val destFile = File(destDir.parentFile, _destName)
        logger.d(
            TAG, "         Writing: " + _destName
                    + ", " + _width + "x" + _height
                    + ", [png: " + pngSize + " " + (if (pngSize < jpgSize) "<" else ">") + " jpg: " + jpgSize + "]"
        )
        val writer: ByteSink = Files.asByteSink(destFile)
        writer.write(result.toByteArray())
        timing.end()
        return destFile
    }

    // ---

    /**
     * Retrieves both the content and the metadata for the given GDoc id immediately.
     * The freshness "up-to-date" flag is computed using the metadata.
     *
     * If the entity is up-to-date, the content from the blog store is used (if available).
     * Otherwise, the content is fetched immediately.
     *
     * Which one to use: <br></br>
     * - [.getGDocSync] when the caller is going to fetch and use the content no matter what.
     * - [.getGDocAsync] when the caller doesn't need the content if the metadata is up-to-date.
     */
    fun getGDocSync(fileId: String, mimeType: String): ExpGDocEntity? {
        val metadataKey = "gdoc-hash-$fileId"
        val contentKey = "gdoc-content-$fileId-$mimeType"

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
        var content: ByteArray? = null
        try {
            content = blobStore.getBytes(contentKey)
        } catch (ignore: IOException) {
        }
        val metadata: GDocMetadata = try {
            gDocReader.getMetadataById(fileId)
        } catch (e: IOException) {
            logger.d(TAG, "Get metadata failed for $fileId")
            logger.d(
                TAG,
                "If this fails, try re-issuing a new OAuth2 token (e.g. gdoc-store/credentials)."
            )
            return null
        }
        var isUpToDate = false
        if (content != null) {
            // Check freshness
            try {
                val storeHash: String? = hashStore.getString(metadataKey)
                isUpToDate = metadata.contentHash == storeHash
            } catch (ignore: IOException) {
            }
        }
        if (!isUpToDate) {
            try {
                logger.d(TAG, "        Fetching: $fileId")
                content = gDocReader.readFileById(fileId, mimeType)
                Preconditions.checkNotNull(content) // fail fast
                logger.d(TAG, "        Fetched sync size: " + content.size)

                // Update the store
                blobStore.putBytes(contentKey, content)
                hashStore.putString(metadataKey, metadata.contentHash)
            } catch (e: IOException) {
                logger.d(TAG, "        Fetching $mimeType sync failed", e)
                throw RuntimeException(e)
            }
        }
        return ExpGDocEntity(metadata, isUpToDate, content)
    }

    /**
     * Retrieves only the metadata for the given GDoc id immediately.
     * Content retrieval is deferred till actually needed.
     * The freshness "up-to-date" flag is computed only using the metadata.
     *
     * The content fetcher also tries to use the blog store's content if available and the
     * metadata indicates the content should be up-to-date.
     *
     * The major difference with [.getGDocSync] is that the content
     * fetch does not happen immediately (whether it's from the blog store or gdoc), nor
     * are the blog/hash stores updated immediately.
     *
     * Which one to use: <br></br>
     * - [.getGDocSync] when the caller is going to fetch and use the content no matter what.
     * - [.getGDocAsync] when the caller doesn't need the content if the metadata is up-to-date.
     */
    fun getGDocAsync(fileId: String, mimeType: String): ExpGDocEntity? {
        val metadataKey = "gdoc-hash-$fileId"
        val contentKey = "gdoc-content-$fileId-$mimeType"

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
        val metadata: GDocMetadata = try {
            gDocReader.getMetadataById(fileId)
        } catch (e: IOException) {
            logger.d(TAG, "Get metadata failed for $fileId")
            return null
        }
        var isUpToDate = false
        // Check freshness using metadata only.
        try {
            val storeHash: String? = hashStore.getString(metadataKey)
            isUpToDate = metadata.contentHash == storeHash
        } catch (ignore: IOException) {
        }
        val fetcher: ExpGDocEntity.ContentFetcher = ExpGDocEntity.ContentFetcher { entity ->
            var content: ByteArray? = null
            if (entity.isToDate) {
                try {
                    content = blobStore.getBytes(contentKey)
                } catch (ignore: IOException) {
                }
            }
            if (content == null) {
                try {
                    logger.d(TAG, "        Fetching $mimeType: $fileId")
                    // 2023-06-08 Use file.export via readFileById() started to fail.
                    // Instead, we're now using the direct exportLinks URL if available.
                    val exportLink = metadata.exportLinks[mimeType]
                    if (exportLink != null) {
                        val url = URL(exportLink)
                        gDocReader.getDataByUrl(url)
                            .use { inputStream -> content = ByteStreams.toByteArray(inputStream) }
                    } else {
                        // Legacy.
                        content = gDocReader.readFileById(fileId, mimeType)
                    }
                    Preconditions.checkNotNull<ByteArray>(content) // fail fast
                    logger.d(TAG, "        Fetched async size: " + content!!.size)
                } catch (e: IOException) {
                    logger.d(TAG, "        Fetching async failed", e)
                    throw RuntimeException(e)
                }
            }
            content
        }
        val syncToStore: ExpGDocEntity.Syncer = ExpGDocEntity.Syncer { entity ->
            if (entity.isToDate) {
                return@Syncer
            }
            try {
                if (entity.isContentFetched && entity.getContent() != null) {
                    blobStore.putBytes(contentKey, entity.getContent()!!)
                }
                hashStore.putString(metadataKey, entity.metadata.contentHash)
            } catch (e: IOException) {
                logger.d(TAG, "syncToStore failed", e)
            }
        }
        return ExpGDocEntity(metadata, isUpToDate, fetcher, syncToStore)
    }
}
