package c;

import static b.DependencyKt.getTest;
import static b.DependencyKt.setTest;

class J {
    void bar() {
        getTest(new a.Test());
        setTest(new a.Test(), 0);
    }
}
