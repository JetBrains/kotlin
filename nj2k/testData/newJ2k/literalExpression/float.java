class A {
    private float f1 = 1.0f;
    private float f2 = 1.0F;
    private float f3 = 1f;
    private float f4 = 1;
    private float f5 = 1F;
    private float f6 = -1;
    private float f7 = -1F;
    private float f8 = +1;
    private float f9 = 1.f;
    private float f10 = 1.F;

    void foo1(float f){}
    void foo2(Float f){}

    void bar() {
        foo1(1);
        foo2(1f);
        foo1(1f);
        foo1(1L);
        foo1(-1);
        foo1(-1L);
        f1 = 1;
        f4 = 1.0f;
    }
}