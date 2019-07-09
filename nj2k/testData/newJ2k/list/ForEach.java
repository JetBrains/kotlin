//file
import java.util.*;

public class ForEach {
    public void test() {
        ArrayList<Object> xs = new ArrayList<Object>();
        List<Object> ys = new LinkedList<Object>();
        for (Object x : xs) {
            ys.add(x);
        }
        for (Object y : ys) {
            xs.add(y);
        }
    }
}