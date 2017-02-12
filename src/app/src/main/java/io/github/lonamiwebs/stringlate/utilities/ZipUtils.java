package io.github.lonamiwebs.stringlate.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    //region Constant values

    private final static int BUFFER_SIZE = 4096;

    //endregion

    //region Public methods

    public static void zipFolder(final File srcFolder, final OutputStream out) throws IOException {
        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(out);
            addFolderToZip("", srcFolder, zip);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) { }
            }
        }
    }

    public static void unzipRecursive(final File srcZipFile, final File dstRoot) throws IOException {
        ZipFile zip = new ZipFile(srcZipFile);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File dstFile = new File(dstRoot, entry.getName());

            File parent = dstFile.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs())
                throw new IOException("Could not create root directory: "+parent.getAbsolutePath());

            if (!entry.isDirectory()) {
                InputStream in = null;
                FileOutputStream out = null;
                try {
                    in = zip.getInputStream(entry);
                    out = new FileOutputStream(dstFile);

                    int count;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((count = in.read(buffer, 0, BUFFER_SIZE)) != -1)
                        out.write(buffer, 0, count);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException ignored) { }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ignored) { }
                    }
                }
            }
        }
    }

    //endregion

    //region Private methods

    private static void addFileToZip(final String path, final File srcFile,
                                     final ZipOutputStream zip) throws IOException {
        if (srcFile.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            FileInputStream in = null;
            try {
                in = new FileInputStream(srcFile);
                zip.putNextEntry(new ZipEntry(path + "/" + srcFile.getName()));

                int count;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((count = in.read(buffer)) > 0)
                    zip.write(buffer, 0, count);

            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) { }
                }
            }
        }
    }

    private static void addFolderToZip(String path, final File srcFolder,
                                       final ZipOutputStream zip) throws IOException {
        path = path.isEmpty() ?
                srcFolder.getName() :
                path + "/" + srcFolder.getName();

        for (File file : srcFolder.listFiles())
            addFileToZip(path, file, zip);
    }

    //endregion
}
