package c;

import a.A;
import a.APackage;

class J {
    void bar() {
        Test t = new Test();
        APackage.test();
        System.out.println(APackage.getTEST());
        APackage.setTEST("");
    }
}
