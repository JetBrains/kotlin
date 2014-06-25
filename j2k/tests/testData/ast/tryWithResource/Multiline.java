//file
import java.io.*;

public class C {
    void foo() throws IOException {
        try(InputStream stream = new FileInputStream("foo")) {
            // reading something
            int c = stream.read();
            System.out.println(c);
        }
    }
}
