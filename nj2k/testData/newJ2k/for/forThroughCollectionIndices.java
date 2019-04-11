import java.lang.System;
import java.util.Collection;

class C{
    void foo1(Collection<String> collection) {
        for (int i = 0; i < collection.size(); i++) {
            System.out.print(i);
        }
    }
}
