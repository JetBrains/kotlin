package test;

class J {
    void test(O o) {
        o.getFoo();
        o.setFoo(1);

        O.getFoo();
        O.setFoo(2);
    }
}