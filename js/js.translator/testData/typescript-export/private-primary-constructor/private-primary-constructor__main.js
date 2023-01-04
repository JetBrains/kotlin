"use strict";
var SomeBaseClass = JS_TESTS.foo.SomeBaseClass;
var SomeExtendingClass = JS_TESTS.foo.SomeExtendingClass;
var FinalClassInChain = JS_TESTS.foo.FinalClassInChain;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    // @ts-expect-error "the constructor is private and can't be used from JS/TS code"
    var baseClass = new SomeBaseClass(4);
    assert(baseClass.answer === 4);
    // @ts-expect-error "the constructor is private and can't be used from JS/TS code"
    var extendingClass = new SomeExtendingClass();
    assert(extendingClass.answer === 42);
    var finalClassInChain = new FinalClassInChain();
    assert(finalClassInChain.answer === 42);
    return "OK";
}
