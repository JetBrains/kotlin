class Foo {
    Foo(int a, int b) {}
}

class C {
final Foo <caret>f = new Foo(1, 2);
}
