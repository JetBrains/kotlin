package c;

import b.Test;

class J {
    void bar() {
        Test t = new Test();
        b.MainKt.test();
        b.MainKt.test(t);
        System.out.println(b.MainKt.getTEST());
        System.out.println(b.MainKt.getTEST(t));
        b.MainKt.setTEST("");
        b.MainKt.setTEST(t, "");
    }
}
