"use strict";
var Test = JS_TESTS.foo.Test;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    var test = new Test();
    assert(test._val === 1);
    assert(test._var === 1);
    test._var = 1000;
    assert(test._var === 1000);
    assert(test._valCustom === 1);
    assert(test._valCustomWithField === 2);
    assert(test._varCustom === 1);
    test._varCustom = 20;
    assert(test._varCustom === 1);
    assert(test._varCustomWithField === 10);
    test._varCustomWithField = 10;
    assert(test._varCustomWithField === 1000);
    return "OK";
}
