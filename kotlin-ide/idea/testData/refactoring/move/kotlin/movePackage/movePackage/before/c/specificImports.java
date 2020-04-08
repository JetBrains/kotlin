package c;

import a.A;
import a.APackage;

class J {
    void bar() {
        A t = new A();
        APackage.foo();
        System.out.println(APackage.getX());
        APackage.setX("");
    }
}
