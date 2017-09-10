/*
 * ------------------------------------------------------------------------------
 * Lonami Exo <lonamiwebs.github.io> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me
 * a coke in return. Provided as is without any kind of warranty. Do not blame
 * or sue me if something goes wrong. No attribution required.
 *                                                             - Lonami Exo
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package net.gsantner.opoc.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.github.lonamiwebs.stringlate.interfaces.Callback;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "SpellCheckingInspection", "deprecation"})
public class ZipUtils {

    private static final int BUFFER_SIZE = 4096;

    //region Public methods

    public static boolean unzip(final File zipFile, final File dstRoot, boolean flatten) {
        return unzip(zipFile, dstRoot, flatten, null);
    }

    public static boolean unzip(final File zipFile, final File dstRoot, final boolean flatten,
                                final Callback<Float> progressCallback) {
        try {
            final float knownLength = progressCallback == null ? -1f : getZipLength(zipFile);
            return unzip(new FileInputStream(zipFile), dstRoot, flatten, progressCallback, knownLength);
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean unzip(final InputStream input, final File dstRoot, final boolean flatten,
                                final Callback<Float> progressCallback, final float knownLength) throws IOException {
        String filename;
        final ZipInputStream in = new ZipInputStream(new BufferedInputStream(input));

        int count;
        int written = 0;
        final byte[] buffer = new byte[BUFFER_SIZE];
        float invLength = 1f / knownLength;

        ZipEntry ze;
        while ((ze = in.getNextEntry()) != null) {
            filename = ze.getName();
            if (!flatten && ze.isDirectory()) {
                if (!new File(dstRoot, filename).mkdirs())
                    return false;
            } else {
                if (flatten) {
                    final int idx = filename.lastIndexOf("/");
                    if (idx != -1)
                        filename = filename.substring(idx + 1);
                }

                final FileOutputStream out = new FileOutputStream(new File(dstRoot, filename));
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                    if (invLength != -1f) {
                        written += count;
                        progressCallback.onCallback(written * invLength);
                    }
                }

                out.close();
                in.closeEntry();
            }
        }
        in.close();
        return true;
    }

    // TODO Maybe there's a way to avoid reading the zip twice,
    // but ZipEntry.getSize() may return -1 so we can't just cace the ZipEntries
    private static long getZipLength(final File zipFile) {
        int count;
        long totalSize = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            final ZipInputStream  in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));

            ZipEntry ze;
            while ((ze = in.getNextEntry()) != null) {
                if (!ze.isDirectory()) {
                    if (ze.getSize() == -1) {
                        while ((count = in.read(buffer)) != -1)
                            totalSize += count;
                    } else {
                        totalSize += ze.getSize();
                    }
                }
            }

            in.close();
            return totalSize;
        } catch (IOException ignored) {
            return -1;
        }
    }

    public static void zipFolder(final File srcFolder, final OutputStream out) throws IOException {
        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(out);
            addFolderToZip("", srcFolder, zip);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
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
                    } catch (IOException ignored) {
                    }
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
