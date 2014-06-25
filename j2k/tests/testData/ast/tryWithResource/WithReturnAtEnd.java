//file
import java.io.*;

public class C {
    int foo() {
        try(InputStream stream = new FileInputStream("foo")) {
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
