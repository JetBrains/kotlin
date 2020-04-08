//file
import java.io.*;

public class C {
    void foo() throws IOException {
        try(InputStream stream = new ByteArrayInputStream(new byte[10])) {
            System.out.println(stream.read());
        }
    }
}
