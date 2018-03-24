package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.rig4.gdoc.GDocHelper;
import com.alflabs.rig4.struct.GDocEntity;
import com.alflabs.rig4.struct.ArticleEntry;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.google.common.base.Charsets;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

import static com.alflabs.rig4.exp.Exp.EXP_DEST_DIR;
import static com.alflabs.rig4.exp.Exp.EXP_GA_UID;
import static com.alflabs.rig4.exp.Exp.EXP_SITE_BANNER;
import static com.alflabs.rig4.exp.Exp.EXP_SITE_BASE_URL;
import static com.alflabs.rig4.exp.Exp.EXP_SITE_TITLE;

public class ArticleGenerator {
    private static final String TAG = ArticleGenerator.class.getSimpleName();

    private final Flags mFlags;
    private final ILogger mLogger;
    private final FileOps mFileOps;
    private final GDocHelper mGDocHelper;
    private final HashStore mHashStore;
    private final Templater mTemplater;
    private final HtmlTransformer mHtmlTransformer;

    @Inject
    public ArticleGenerator(
            Flags flags,
            ILogger logger,
            FileOps fileOps,
            GDocHelper gDocHelper,
            HashStore hashStore,
            Templater templater,
            HtmlTransformer htmlTransformer) {
        mFlags = flags;
        mLogger = logger;
        mFileOps = fileOps;
        mGDocHelper = gDocHelper;
        mHashStore = hashStore;
        mTemplater = templater;
        mHtmlTransformer = htmlTransformer;
    }

    void processEntries(@NonNull List<ArticleEntry> entries, boolean allChanged)
            throws IOException, URISyntaxException, InvocationTargetException, IllegalAccessException, ParseException {
        String destDir = mFlags.getString(EXP_DEST_DIR);
        mLogger.d(TAG, "        Site URL: " + mFlags.getString(EXP_SITE_BASE_URL));
        mLogger.d(TAG, "     Destination: " + destDir);

        for (ArticleEntry entry : entries) {
            String destName = entry.getDestName();
            File destFile = new File(destDir, destName);

            mLogger.d(TAG, "Process document: " + destName);

            GDocEntity entity = mGDocHelper.getGDoc(entry.getFileId(), "text/html");
            byte[] docContent = entity.getContent();
            String title = entity.getMetadata().getTitle();
            boolean keepExisting = !allChanged && entity.isUpdateToDate() && mFileOps.isFile(destFile);

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
                        return mGDocHelper.downloadDrawing(id, destFile, width, height);
                    }

                    @Override
                    public String processImage(URI uri, int width, int height) throws IOException {
                        return mGDocHelper.downloadImage(uri, destFile, width, height);
                    }
                });
        return htmlBody;
    }
}
