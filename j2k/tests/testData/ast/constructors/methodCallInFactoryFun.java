class C {
    private final int arg1;
    private final int arg2;
    private final int arg3;

    int foo(int p){ return p; }
    private static int staticFoo(int p){ return p; }

    C(int arg1, int arg2, int arg3) {
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
    }

    C(int arg1, int arg2, C other) {
        this(arg1, arg2, 0);
        System.out.println(foo(1) + this.foo(2) + other.foo(3) + staticFoo(4) + C.staticFoo(5));
    }
}
