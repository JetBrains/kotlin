//file
import java.io.IOException;

class A {
    void foo() {
        try {
            bar()
        }
        catch(RuntimeException | IOException e) {
            e.printStackTrace(); // print stack trace
        }
    }
}