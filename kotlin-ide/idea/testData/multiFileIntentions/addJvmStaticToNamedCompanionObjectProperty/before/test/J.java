package test;

class J {
    void test(C.O companion) {
        companion.getFoo();
        companion.setFoo(1);

        C.O.getFoo();
        C.O.setFoo(2);
    }
}