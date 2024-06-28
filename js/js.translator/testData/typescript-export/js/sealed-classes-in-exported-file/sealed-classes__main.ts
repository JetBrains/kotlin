import TestSealed = JS_TESTS.foo.TestSealed;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(new TestSealed.AA().name == "AA");
    assert(new TestSealed.AA().bar() == "bar");
    assert(new TestSealed.BB().name == "BB");
    assert(new TestSealed.BB().baz() == "baz");

    return "OK";
}