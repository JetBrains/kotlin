class A {
    private double d1 = 1;
    private double d2 = 1.0;
    private double d3 = 1.0f;
    private double d4 = 1.0d;
    private double d5 = 1.0D;
    private double d6 = 1.0F;
    private double d7 = Math.sqrt(2) - 1;
    private double d8 = 1.;
    private double d9 = 1.d;
    private double x = 1 / (1. + 0);

    void foo1(double d){}
    void foo2(Double d){}

    void bar() {
        foo1(1);
        foo1(1f);
        foo1(1.0);
        foo2(1.0);
        d1 = 1.0;
        d2 = 1;
    }
}