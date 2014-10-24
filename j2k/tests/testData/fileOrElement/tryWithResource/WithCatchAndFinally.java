//file
import java.io.*;

public class C {
    void foo() {
        try(InputStream stream = new ByteArrayInputStream(new byte[10])) {
            // reading something
            int c = stream.read();
            System.out.println(c);
        }
        catch (IOException e) {
            System.out.println(e);
        }
        finally {
            System.out.println("Finally!");
        }
    }
}
