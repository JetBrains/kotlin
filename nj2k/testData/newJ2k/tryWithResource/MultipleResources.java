//file
import java.io.*;

public class C {
    void foo() throws IOException {
        try(ByteArrayInputStream input = new ByteArrayInputStream(new byte[10]);
            OutputStream output = new ByteArrayOutputStream()) {
            output.write(input.read());
            output.write(0);
        }
    }
}
