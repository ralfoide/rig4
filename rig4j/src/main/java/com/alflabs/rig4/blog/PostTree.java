package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/**
 * The post tree contains a model of files to be generated based on the input source.
 * Each blog is composed of one index file, paged files, and individual post files.
 */
class PostTree {
    private final static String TAG = PostTree.class.getSimpleName();
    final static String BLOG_ROOT = "blog";
    static final String HTML = ".html";
    static final String ATOM_XML = "atom.xml";
    static final String INDEX_HTML = "index.html";

    private final Map<String, Blog> mBlogs = new TreeMap<>();

    public void add(@NonNull Blog blog) {
        mBlogs.put(blog.getCategory(), blog);
    }

    @Null
    public Blog get(@NonNull String category) {
        return mBlogs.get(category);
    }

    public void generate(@NonNull BlogGenerator.Generator generator) throws Exception {
        for (Blog blog : mBlogs.values()) {
            blog.generate(generator);
        }
    }

    public void saveMetadata() {
    }

    public static class FileItem {
        private final File mDir;
        private final String mName;

        public FileItem(@NonNull File dir, @NonNull String name) {
            mDir = dir;
            mName = name;
        }

        @NonNull
        public File getDir() {
            return mDir;
        }

        @NonNull
        public String getName() {
            return mName;
        }

        @NonNull
        public String getLeafDir() {
            String path = mDir.getPath();
            path = path.replace("..", "");
            return path;
        }

        @NonNull
        public String getLeafDirWeb() {
            String path = getLeafDir().replace(File.separatorChar, '/');
            if (!path.endsWith("/")) {
                path += "/";
            }
            return path;
        }

        @NonNull
        public String getLeafFile() {
            File file = new File(mDir, mName);
            String path = file.getPath();
            path = path.replace("..", "");
            return path;
        }
    }

}
