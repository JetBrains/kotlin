import java.util.Iterator;
import java.util.List;

public class C {

    public static void consume1(C c) {

    }

    public static void consume2(C c) {

    }

    public static void foo(List<C> cList) {
        for (Iterator iter = cList.iterator(); iter.hasNext();) {
            C c = (C) iter.next();
            consume1(c);
            consume2(c);
        }
    }
}