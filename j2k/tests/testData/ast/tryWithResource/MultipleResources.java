//file
import java.io.*;

public class C {
    void foo() throws IOException {
        try(InputStream input = new FileInputStream("foo");
            OutputStream output = new FileOutputStream("bar")) {
            output.write(input.read());
            output.write(0);
        }
    }
}
