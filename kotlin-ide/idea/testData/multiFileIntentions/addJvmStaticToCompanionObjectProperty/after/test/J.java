package test;

class J {
    void test(C.Companion companion) {
        companion.getFoo();
        companion.setFoo(1);

        C.getFoo();
        C.setFoo(2);
    }
}