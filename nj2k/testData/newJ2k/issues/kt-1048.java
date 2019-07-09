//file
import java.util.HashMap;

class G <T extends String> {
  public <T> G(T t) {
  }
}

public class Java {
    void test() {
        HashMap m = new HashMap();
        m.put(1, 1);
    }
    void test2() {
        HashMap<?, ?> m = new HashMap();
        G g = new G("");
        G<String> g2 = new G<String>("");
    }
}