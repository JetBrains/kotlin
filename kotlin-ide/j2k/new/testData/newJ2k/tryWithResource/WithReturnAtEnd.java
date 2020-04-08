//file
import java.io.*;

public class C {
    int foo() {
        try(InputStream stream = new ByteArrayInputStream(new byte[10])) {
            // reading something
            int c = stream.read();
            return c;
        }
        catch (IOException e) {
            System.out.println(e);
            return -1;
        }
    }
}
