import java.io.*;
import java.util.zip.*;
import java.util.jar.*;

public class TestZip {
    public static void main(String[] args) throws Exception {
        File archive = new File("test.zip");
        // create a zip with a manifest
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archive))) {
            zos.putNextEntry(new ZipEntry("META-INF/"));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            Manifest m = new Manifest();
            m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            m.write(zos);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("test.txt"));
            zos.write("hello".getBytes());
            zos.closeEntry();
        }

        File tempArchive = new File("test.tmp.zip");
        try (ZipInputStream input = new ZipInputStream(new FileInputStream(archive));
             ZipOutputStream output = new ZipOutputStream(new FileOutputStream(tempArchive))) {
            ZipEntry entry = input.getNextEntry();
            while (entry != null) {
                System.out.println("Entry: " + entry.getName());
                ZipEntry replacement = new ZipEntry(entry.getName());
                output.putNextEntry(replacement);
                if (entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                    Manifest manifest = new Manifest(input);
                    manifest.write(output);
                } else {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = input.read(buf)) > 0) {
                        output.write(buf, 0, len);
                    }
                }
                output.closeEntry();
                input.closeEntry();
                entry = input.getNextEntry();
            }
        }
        System.out.println("Done!");
    }
}
