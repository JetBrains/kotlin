//file
import java.io.*;

public class C {
    void foo() throws IOException {
        try(InputStream stream = new FileInputStream("foo")) {
            System.out.println(stream.read());
        }
    }
}
