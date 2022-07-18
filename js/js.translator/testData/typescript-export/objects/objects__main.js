"use strict";
var O = JS_TESTS.foo.O;
var O0 = JS_TESTS.foo.O0;
var Parent = JS_TESTS.foo.Parent;
var takesO = JS_TESTS.foo.takesO;
var getParent = JS_TESTS.foo.getParent;
var createNested1 = JS_TESTS.foo.createNested1;
var createNested2 = JS_TESTS.foo.createNested2;
var createNested3 = JS_TESTS.foo.createNested3;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(typeof O0 === "object" && O0 != null);
    assert(O.x === 10);
    assert(O.foo() === 20);
    assert(takesO(O) === 30);
    // Do not strip types from those test cases (it is a check of nested objects types usability)
    var parent = Parent;
    var nested1 = Parent.Nested1;
    var nested2 = new Parent.Nested1.Nested2();
    var nested3 = new Parent.Nested1.Nested2.Companion.Nested3();
    assert(nested1.value === "Nested1");
    assert(getParent() === parent);
    assert(createNested1() === nested1);
    assert(createNested2() !== nested2 && createNested2() instanceof Parent.Nested1.Nested2);
    assert(createNested3() !== nested3 && createNested3() instanceof Parent.Nested1.Nested2.Companion.Nested3);
    return "OK";
}
