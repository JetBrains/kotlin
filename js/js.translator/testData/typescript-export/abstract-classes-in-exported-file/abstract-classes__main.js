"use strict";
var TestAbstract = JS_TESTS.foo.TestAbstract;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(new TestAbstract.AA().name == "AA");
    assert(new TestAbstract.AA().bar() == "bar");
    assert(new TestAbstract.BB().name == "BB");
    assert(new TestAbstract.BB().baz() == "baz");
    return "OK";
}
