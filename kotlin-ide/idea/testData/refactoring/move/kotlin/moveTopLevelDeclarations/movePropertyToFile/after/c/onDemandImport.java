package c;

import b.DependencyKt;

class J {
    void bar() {
        DependencyKt.setTest("");
        System.out.println(DependencyKt.getTest());
    }
}
