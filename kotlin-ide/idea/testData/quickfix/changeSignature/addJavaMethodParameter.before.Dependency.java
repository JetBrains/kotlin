class Foo {
    static void foo(int n, int m) {

    }
}

class Test {
    static void test() {
        Foo.foo();
        Foo.foo(1);
        Foo.foo(1, 2);
        Foo.foo(1, 2, 3);
        Foo.foo(4, 5, 6);
    }
}