package com.github.blindpirate.gogradle.util;

import com.github.blindpirate.gogradle.crossplatform.Os;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static com.github.blindpirate.gogradle.GogradleGlobal.DEFAULT_CHARSET;
import static com.github.blindpirate.gogradle.GogradleGlobal.MAX_DFS_DEPTH;

/**
 * Encapsulation of {@link FileUtils} and {@link org.apache.commons.io.IOUtils},
 * it never throws checked exceptions.
 */
public final class IOUtils {

    public static void forceMkdir(File directory) {
        try {
            FileUtils.forceMkdir(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static File mkdir(File baseDir, String newDirName) {
        File ret = new File(baseDir, newDirName);
        forceMkdir(ret);
        return ret;
    }

    public static void forceDelete(final File file) {
        try {
            if (file != null) {
                FileUtils.forceDelete(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void deleteQuitely(final File file) {
        FileUtils.deleteQuietly(file);
    }

    public static boolean dirIsEmpty(File directory) {
        String[] files = Assert.isNotNull(directory.list());
        return files.length == 0;
    }

    public static Path ensureDirExistAndWritable(Path path) {
        Assert.isTrue(path.isAbsolute(), "path must be absolute!");
        File dir = path.toFile();
        IOUtils.forceMkdir(dir);
        Assert.isTrue(Files.isWritable(dir.toPath()), "Cannot write to directory:" + path);
        return path;
    }

    public static Path ensureDirExistAndWritable(Path base, String relativePath) {
        return ensureDirExistAndWritable(base.resolve(Paths.get(relativePath)));
    }

    public static void copyDirectory(File src, File dest) {
        try {
            org.apache.commons.io.FileUtils.copyDirectory(src, dest);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] toByteArray(ByteBuffer buf) {
        buf.position(0);
        byte[] ret = new byte[buf.remaining()];
        buf.get(ret);
        return ret;
    }

    public static void copyDirectory(final File srcDir, final File destDir,
                                     final FileFilter filter) {
        try {
            FileUtils.copyDirectory(srcDir, destDir, filter);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copyFile(File src, File dest) {
        try {
            FileUtils.copyFile(src, dest);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void touch(File file) {
        try {
            org.apache.commons.io.FileUtils.touch(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(File file, CharSequence data) {
        try {
            org.apache.commons.io.FileUtils.write(file, data, DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static File write(File baseDir, String fileName, CharSequence data) {
        Assert.isNotNull(baseDir);
        File targetFile = new File(baseDir, fileName);
        forceMkdir(targetFile.getParentFile());
        write(targetFile, data);
        return targetFile;
    }

    public static String toString(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return org.apache.commons.io.IOUtils.toString(is, DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<String> safeList(File dir) {
        String[] files = dir.list();
        return files == null ? Collections.emptyList() : Arrays.asList(files);
    }

    public static List<File> safeListFiles(File dir) {
        File[] files = dir.listFiles();
        return files == null ? Collections.emptyList() : Arrays.asList(files);
    }

    public static boolean isValidDirectory(File dir) {
        return dir.exists() && dir.isDirectory();
    }

    public static String toString(InputStream inputStream) {
        try {
            return org.apache.commons.io.IOUtils.toString(inputStream, DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void append(File file, String str) {
        write(file, toString(file) + str);
    }

    public static List<String> readLines(File file) {
        String content = toString(file);
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(content.split("\n"));
        }
    }

    public static void walkFileTreeSafely(Path path, FileVisitor<? super Path> visitor) {
        try {
            Files.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), MAX_DFS_DEPTH, visitor);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Collection<File> filterFilesRecursively(File dir, IOFileFilter filter) {
        return FileUtils.listFiles(dir, filter, filter);
    }

    public static Collection<File> filterFilesRecursively(File dir, IOFileFilter fileFilter, IOFileFilter dirFilter) {
        return FileUtils.listFiles(dir, fileFilter, dirFilter);
    }

    public static void clearDirectory(File dir) {
        try {
            if (dir == null || !dir.exists()) {
                return;
            }
            FileUtils.cleanDirectory(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void chmodAddX(Path filePath) {
        try {
            if (Os.getHostOs() != Os.WINDOWS) {
                Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwx------"));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Path toRealPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String byteCountToDisplaySize(long size) {
        return FileUtils.byteCountToDisplaySize(size);
    }

    public static String encodeInternally(String s) {
        try {
            return URLEncoder.encode(s, "UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String decodeInternally(String encoded) {
        try {
            return URLDecoder.decode(encoded, "UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    // It is said that this method has very bad performance
    // http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
    public static long countLines(Path path) {
        try (Stream<String> lines = Files.lines(path)) {
            return lines.count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copyURLToFile(URL url, File dest) {
        try {
            FileUtils.copyURLToFile(url, dest);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void serialize(Object obj, File file) {
        if (!file.exists()) {
            write(file, "");
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(obj);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Object deserialize(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw ExceptionHandler.uncheckException(e);
        }
    }

}
