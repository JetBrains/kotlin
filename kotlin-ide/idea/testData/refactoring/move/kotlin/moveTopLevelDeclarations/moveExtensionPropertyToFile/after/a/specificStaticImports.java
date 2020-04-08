package a;

import static b.DependencyKt.getTest;
import static b.DependencyKt.setTest;

class J {
    void bar() {
        getTest(new Test());
        setTest(new Test(), 0);
    }
}
