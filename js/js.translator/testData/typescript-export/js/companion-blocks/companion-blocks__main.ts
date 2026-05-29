import MyClass = JS_TESTS.foo.MyClass;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(MyClass.bar() === "BARRRR")
    assert(MyClass.foo === "FOOOO")
    assert(MyClass.baz === "BAZZZZ")
    assert(MyClass.mutable === "INITIAL")
    MyClass.mutable = "CHANGED"
    assert(MyClass.mutable === "CHANGED")

    return "OK";
}
