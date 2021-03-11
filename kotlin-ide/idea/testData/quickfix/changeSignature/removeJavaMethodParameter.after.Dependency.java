class Foo {
    static void foo(int n) {

    }
}

class Test {
    static void test() {
        Foo.foo();
        Foo.foo(1);
        Foo.foo(1);
        Foo.foo(3);
    }
}