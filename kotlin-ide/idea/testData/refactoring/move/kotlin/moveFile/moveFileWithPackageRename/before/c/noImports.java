package c;

class J {
    void bar() {
        a.Test t = new a.Test();
        a.MainKt.test();
        a.MainKt.test(t);
        System.out.println(a.MainKt.getTEST());
        System.out.println(a.MainKt.getTEST(t));
        a.MainKt.setTEST("");
        a.MainKt.setTEST(t, "");
    }
}
