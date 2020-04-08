package c;

import a.Test;
import a.MainKt;

class J {
    void bar() {
        Test t = new Test();
        MainKt.test();
        MainKt.test(t);
        System.out.println(MainKt.getTEST());
        System.out.println(MainKt.getTEST(t));
        MainKt.setTEST("");
        MainKt.setTEST(t, "");
    }
}
