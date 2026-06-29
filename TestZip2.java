import java.io.*;
import java.util.zip.*;
import java.util.jar.*;

public class TestZip2 {
    public static void main(String[] args) throws Exception {
        File archive = new File("test2.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archive))) {
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            String mf = "Manifest-Version: 1.0\r\nMulti-Release: true\r\n\r\n";
            zos.write(mf.getBytes());
            zos.closeEntry();
        }

        File tempArchive = new File("test2.tmp.zip");
        try (ZipInputStream input = new ZipInputStream(new FileInputStream(archive));
             ZipOutputStream output = new ZipOutputStream(new FileOutputStream(tempArchive))) {
            ZipEntry entry = input.getNextEntry();
            while (entry != null) {
                System.out.println("Entry: " + entry.getName());
                ZipEntry replacement = new ZipEntry(entry.getName());
                output.putNextEntry(replacement);
                if (entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                    Manifest manifest = new Manifest(input);
                    manifest.getMainAttributes().remove(new Attributes.Name("Multi-Release"));
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
