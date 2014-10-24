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
        catch (Exception e) {
            System.err.println(e);
        }
    }
}
