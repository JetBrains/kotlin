class A {
    private long l1 = 1L;
    private long l2 = 1;
    private long l3 = 1l;
    private long l4 = -1;
    private long l5 = 123456789101112;
    private long l6 = -123456789101112;
    private long l7 = +1;
    private long l8 = +1L;

    void foo1(long l){}
    void foo2(Long l){}

    void bar() {
        foo1(1);
        foo1(1L);
        foo2(1L);
        foo1(-1);
        l1 = 10
        l2 = 10L
        l4 = 10
    }

    void foo(long z) {
        boolean b1 = z == 1;
        boolean b2 = z != 1;
    }
}