package b;

import a.*;

class J {
    void bar() {
        DependencyKt.getTest(new Test());
        DependencyKt.setTest(new Test(), 0);
    }
}
