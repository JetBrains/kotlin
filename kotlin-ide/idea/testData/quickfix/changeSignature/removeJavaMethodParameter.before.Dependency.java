class Foo {
    static void foo(int n, int m) {

    }
}

class Test {
    static void test() {
        Foo.foo();
        Foo.foo(1);
        Foo.foo(1, 2);
        Foo.foo(3, 4);
    }
}