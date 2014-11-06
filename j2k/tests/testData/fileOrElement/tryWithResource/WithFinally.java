//file
import java.io.*;

public class C {
    void foo() throws IOException {
        try(InputStream stream = new ByteArrayInputStream(new byte[10])) {
            // reading something
            int c = stream.read();
            System.out.println(c);
        }
        finally {
            // dispose something else
        }
    }
}
