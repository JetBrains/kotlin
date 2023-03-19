"use strict";
var bar = JS_TESTS.foo.bar;
var foo = JS_TESTS.foo.foo;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(bar == "Test");
    assert(foo() == undefined);
    return "OK";
}
