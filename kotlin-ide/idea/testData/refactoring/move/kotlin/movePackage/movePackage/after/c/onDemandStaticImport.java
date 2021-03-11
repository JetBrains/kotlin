package c;

import static b.a.APackage.*;

class J {
    void bar() {
        b.a.A t = new b.a.A();
        foo();
        System.out.println(getX());
        setX("");
    }
}
