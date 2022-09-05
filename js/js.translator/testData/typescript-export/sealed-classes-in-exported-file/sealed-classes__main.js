"use strict";
var TestSealed = JS_TESTS.foo.TestSealed;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(new TestSealed.AA().name == "AA");
    assert(new TestSealed.AA().bar() == "bar");
    assert(new TestSealed.BB().name == "BB");
    assert(new TestSealed.BB().baz() == "baz");
    return "OK";
}
