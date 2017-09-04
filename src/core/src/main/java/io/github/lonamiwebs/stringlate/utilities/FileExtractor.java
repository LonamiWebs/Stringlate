package io.github.lonamiwebs.stringlate.utilities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.github.lonamiwebs.stringlate.interfaces.Callback;

public class FileExtractor {

    // http://stackoverflow.com/a/10997886/4759433
    public static boolean unpackZip(File zipFile, File outDir,
                                    boolean subDirectories) {
        return unpackZip(zipFile, outDir, subDirectories, null);
    }

    public static boolean unpackZip(File zipFile, File outDir,
                                    boolean subDirectories,
                                    Callback<Float> progressCallback) {
        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = new FileInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[4096];
            int count;
            int written = 0;
            float length = progressCallback == null ? -1f : getZipLength(zipFile);

            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();
                if (subDirectories && ze.isDirectory()) {
                    new File(outDir, filename).mkdirs();
                } else {
                    if (subDirectories || !filename.contains("/")) {
                        FileOutputStream out = new FileOutputStream(new File(outDir, filename));
                        while ((count = zis.read(buffer)) != -1) {
                            out.write(buffer, 0, count);
                            if (length != -1f) {
                                written += count;
                                progressCallback.onCallback(written / length);
                            }
                        }

                        out.close();
                        zis.closeEntry();
                    }
                }
            }
            zis.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // TODO Maybe there's a way to avoid reading the zip twice,
    // but ZipEntry.getSize() may return -1 so we can't just cace the ZipEntries
    private static long getZipLength(File zipFile) {
        InputStream is;
        ZipInputStream zis;
        int count;
        long totalSize = 0;
        byte[] buffer = new byte[4096];
        try {
            is = new FileInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(is));

            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (!ze.isDirectory()) {
                    if (ze.getSize() == -1) {
                        while ((count = zis.read(buffer)) != -1)
                            totalSize += count;
                    } else {
                        totalSize += ze.getSize();
                    }
                }
            }

            zis.close();
            return totalSize;
        } catch (IOException ignored) {
            return -1;
        }
    }
}
