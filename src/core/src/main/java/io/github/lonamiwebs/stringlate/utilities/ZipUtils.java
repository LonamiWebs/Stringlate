package io.github.lonamiwebs.stringlate.utilities;

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

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class ZipUtils {
    private final static int BUFFER_SIZE = 4096;

    public static void zipFolder(final File srcFolder, final OutputStream out) throws IOException {
        ZipOutputStream outZip = null;
        try {
            outZip = new ZipOutputStream(out);
            addFolderToZip("", srcFolder, outZip);
        } finally {
            if (outZip != null) {
                try {
                    outZip.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void unzipRecursive(final InputStream is, final File destRootFolder) throws IOException {
        ZipInputStream inZip = new ZipInputStream(is);
        ZipEntry entry;
        while ((entry = inZip.getNextEntry()) != null) {
            File destFile = new File(destRootFolder, entry.getName());
            File parent = destFile.getParentFile();

            if (!parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("Could not create root directory: " + parent.getAbsolutePath());
            }
            if (!entry.isDirectory()) {
                FileOutputStream fileOut = null;
                try {
                    fileOut = new FileOutputStream(destFile);

                    int count;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((count = inZip.read(buffer, 0, BUFFER_SIZE)) != -1)
                        fileOut.write(buffer, 0, count);
                } finally {
                    try {
                        inZip.closeEntry();
                    } catch (IOException ignored) {
                    }
                    if (fileOut != null) {
                        try {
                            fileOut.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
    }

    private static void addFileToZip(final String pathInsideZip, final File fileToZip, final ZipOutputStream outZip) throws IOException {
        if (fileToZip.isDirectory()) {
            addFolderToZip(pathInsideZip, fileToZip, outZip);
        } else {
            FileInputStream in = null;
            try {
                in = new FileInputStream(fileToZip);
                outZip.putNextEntry(new ZipEntry(pathInsideZip + "/" + fileToZip.getName()));

                int count;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((count = in.read(buffer)) > 0)
                    outZip.write(buffer, 0, count);

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

    private static void addFolderToZip(String pathInsideZip, final File folderToZip, final ZipOutputStream outZip) throws IOException {
        pathInsideZip = pathInsideZip.isEmpty() ?
                folderToZip.getName() :
                pathInsideZip + "/" + folderToZip.getName();

        File[] files = folderToZip.listFiles();
        if (files != null) {
            for (File file : files)
                addFileToZip(pathInsideZip, file, outZip);
        }
    }

    public static boolean unpackZip(File pathToZipFile, File targetDir, boolean unzipSubdirs) {
        return unpackZip(pathToZipFile, targetDir, unzipSubdirs, null);
    }

    public static boolean unpackZip(File pathToZipFile, File targetDir, boolean unzipSubdirs, Callback<Float> progressCallback) {
        InputStream is;
        ZipInputStream inZip;
        try {
            String filename;
            is = new FileInputStream(pathToZipFile);
            inZip = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            int written = 0;
            float length = progressCallback == null ? -1f : getZipLength(pathToZipFile);

            while ((ze = inZip.getNextEntry()) != null) {
                filename = ze.getName();
                if (unzipSubdirs && ze.isDirectory()) {
                    //noinspection ResultOfMethodCallIgnored
                    new File(targetDir, filename).mkdirs();
                } else {
                    if (unzipSubdirs || !filename.contains("/")) {
                        FileOutputStream out = new FileOutputStream(new File(targetDir, filename));
                        while ((count = inZip.read(buffer)) != -1) {
                            out.write(buffer, 0, count);
                            if (length != -1f) {
                                written += count;
                                progressCallback.onCallback(written / length);
                            }
                        }
                        out.close();
                        inZip.closeEntry();
                    }
                }
            }
            inZip.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // TODO Maybe there's a way to avoid reading the zip twice,
    // but ZipEntry.getSize() may return -1 so we can't just cache the ZipEntries
    private static long getZipLength(File zipFile) {
        InputStream is;
        ZipInputStream zis;
        int count;
        long totalSize = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
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
