"use strict";
var _const_val = JS_TESTS.foo._const_val;
var _val = JS_TESTS.foo._val;
var _var = JS_TESTS.foo._var;
var _valCustom = JS_TESTS.foo._valCustom;
var _valCustomWithField = JS_TESTS.foo._valCustomWithField;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(_const_val === 1);
    assert(_val === 1);
    assert(_var === 1);
    JS_TESTS.foo._var = 1000;
    assert(JS_TESTS.foo._var === 1000);
    assert(_valCustom === 1);
    assert(_valCustomWithField === 2);
    assert(JS_TESTS.foo._varCustom === 1);
    JS_TESTS.foo._varCustom = 20;
    assert(JS_TESTS.foo._varCustom === 1);
    assert(JS_TESTS.foo._varCustomWithField === 10);
    JS_TESTS.foo._varCustomWithField = 10;
    assert(JS_TESTS.foo._varCustomWithField === 1000);
    return "OK";
}
