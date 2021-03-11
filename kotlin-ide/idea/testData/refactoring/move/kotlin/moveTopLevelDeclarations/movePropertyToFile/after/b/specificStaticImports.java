package b;

import static b.DependencyKt.getTest;
import static b.DependencyKt.setTest;

class J {
    void bar() {
        setTest("");
        System.out.println(getTest());
    }
}
