"use strict";
var JsNameTest = JS_TESTS.foo.JsNameTest;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    var jsNameTest = JsNameTest.NotCompanion.create();
    assert(jsNameTest.value === 4);
    assert(jsNameTest.runTest() === "JsNameTest");
    assert(jsNameTest.acceptObject(Object) === "Function");
    var jsNameNestedTest = JsNameTest.NotCompanion.createChild(42);
    assert(jsNameNestedTest.value === 42);
    return "OK";
}
