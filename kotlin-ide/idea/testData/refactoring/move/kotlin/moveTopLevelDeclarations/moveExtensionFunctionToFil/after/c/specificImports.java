package c;

import a.Test;
import b.DependencyKt;

class J {
    void bar() {
        DependencyKt.test(new Test());
    }
}
