import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestMutableCollection {
    public final List<String> list = new ArrayList<>();

    public void test() {
        for (Iterator<String> it = list.iterator(); it.hasNext(); ) {
            String s = it.next();
            if (s.equals("")) it.remove();
        }
    }
}