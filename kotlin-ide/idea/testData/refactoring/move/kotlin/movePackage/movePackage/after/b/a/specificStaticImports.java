package b.a;

import static APackage.foo;
import static APackage.getX;
import static APackage.setX;

class J {
    void bar() {
        A t = new A();
        foo();
        System.out.println(getX());
        setX("");
    }
}
