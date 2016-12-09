package io.github.lonamiwebs.stringlate.utilities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileExtractor {

    // http://stackoverflow.com/a/10997886/4759433
    public static boolean unpackZip(File zipFile, File outDir,
                                    boolean subDirectories)
    {
        InputStream is;
        ZipInputStream zis;
        try
        {
            String filename;
            is = new FileInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();
                if (subDirectories && ze.isDirectory()) {
                    new File(outDir, filename).mkdirs();
                } else {
                    if (subDirectories || !filename.contains("/")) {
                        FileOutputStream out = new FileOutputStream(new File(outDir, filename));
                        while ((count = zis.read(buffer)) != -1)
                            out.write(buffer, 0, count);

                        out.close();
                        zis.closeEntry();
                    }
                }
            }
            zis.close();
            return true;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
