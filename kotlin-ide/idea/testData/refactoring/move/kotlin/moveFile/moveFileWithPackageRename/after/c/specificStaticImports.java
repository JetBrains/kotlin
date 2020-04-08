package c;

import b.Test;

import static b.MainKt.test;
import static b.MainKt.getTEST;
import static b.MainKt.setTEST;

class J {
    void bar() {
        Test t = new Test();
        test();
        test(t);
        System.out.println(getTEST());
        System.out.println(getTEST(t));
        setTEST("");
        setTEST(t, "");
    }
}
