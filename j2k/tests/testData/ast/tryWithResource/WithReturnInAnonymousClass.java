//file
import java.io.*;

interface I {
    int doIt(InputStream stream) throws IOException;
}

public class C {
    void foo() throws IOException {
        try(InputStream stream = new FileInputStream("foo")) {
            bar(new I() {
                @Override
                public int doIt(InputStream stream) throws IOException {
                    return stream.available();
                }
            }, stream);
        }
    }

    int bar(I i, InputStream stream) throws IOException {
        return i.doIt(stream);
    }
}
