import Test = JS_TESTS.foo.Test;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(Test.bar() === "BARRRR");
    assert(Test.foo === "FOOOO");
    assert(Test.baz === "BAZZZZ");
    assert(Test.mutable === "INITIAL");
    Test.mutable = "CHANGED"
    assert(Test.mutable === "CHANGED")

    return "OK";
}