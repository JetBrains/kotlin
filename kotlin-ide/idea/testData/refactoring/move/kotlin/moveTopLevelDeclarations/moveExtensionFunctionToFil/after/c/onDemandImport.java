package c;

import a.*;
import b.DependencyKt;

class J {
    void bar() {
        DependencyKt.test(new Test());
    }
}
