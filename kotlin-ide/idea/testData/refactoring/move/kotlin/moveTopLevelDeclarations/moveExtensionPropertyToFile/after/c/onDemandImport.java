package c;

import a.*;
import b.DependencyKt;

class J {
    void bar() {
        DependencyKt.getTest(new Test());
        DependencyKt.setTest(new Test(), 0);
    }
}
