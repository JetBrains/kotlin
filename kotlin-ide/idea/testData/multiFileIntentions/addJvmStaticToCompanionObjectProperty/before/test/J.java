package test;

class J {
    void test(C.Companion companion) {
        companion.getFoo();
        companion.setFoo(1);

        C.Companion.getFoo();
        C.Companion.setFoo(2);
    }
}