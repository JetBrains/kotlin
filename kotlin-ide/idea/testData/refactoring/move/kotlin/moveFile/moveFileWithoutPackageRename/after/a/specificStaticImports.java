package a;

import static APackage.test;
import static APackage.getTEST;
import static APackage.setTEST;

class J {
    void bar() {
        Test t = new Test();
        test();
        System.out.println(getTEST());
        setTEST("");
    }
}
