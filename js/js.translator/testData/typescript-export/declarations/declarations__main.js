"use strict";
var foo = JS_TESTS.foo;
var varargInt = JS_TESTS.foo.varargInt;
var varargNullableInt = JS_TESTS.foo.varargNullableInt;
var varargWithOtherParameters = JS_TESTS.foo.varargWithOtherParameters;
var varargWithComplexType = JS_TESTS.foo.varargWithComplexType;
var sumNullable = JS_TESTS.foo.sumNullable;
var defaultParameters = JS_TESTS.foo.defaultParameters;
var generic1 = JS_TESTS.foo.generic1;
var generic2 = JS_TESTS.foo.generic2;
var generic3 = JS_TESTS.foo.generic3;
var inlineFun = JS_TESTS.foo.inlineFun;
var _const_val = JS_TESTS.foo._const_val;
var _val = JS_TESTS.foo._val;
var _var = JS_TESTS.foo._var;
var A = JS_TESTS.foo.A;
var A1 = JS_TESTS.foo.A1;
var A2 = JS_TESTS.foo.A2;
var A3 = JS_TESTS.foo.A3;
var _valCustom = JS_TESTS.foo._valCustom;
var _valCustomWithField = JS_TESTS.foo._valCustomWithField;
var A4 = JS_TESTS.foo.A4;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(foo.sum(10, 20) === 30);
    assert(varargInt(new Int32Array([1, 2, 3])) === 3);
    assert(varargNullableInt([10, 20, 30, null, undefined, 40]) === 6);
    assert(varargWithOtherParameters("1234", ["1", "2", "3"], "12") === 9);
    assert(varargWithComplexType([]) === 0);
    assert(varargWithComplexType([
        function (x) { return x; },
        function (x) { return [new Int32Array([1, 2, 3])]; },
        function (x) { return []; },
    ]) === 3);
    assert(sumNullable(10, null) === 10);
    assert(sumNullable(undefined, 20) === 20);
    assert(sumNullable(1, 2) === 3);
    assert(defaultParameters(20, "OK") === "20OK");
    assert(generic1("FOO") === "FOO");
    assert(generic1({ x: 10 }).x === 10);
    assert(generic2(null) === true);
    assert(generic2(undefined) === true);
    assert(generic2(10) === false);
    assert(generic3(10, true, "__", {}) === null);
    var result = 0;
    inlineFun(10, function (x) { result = x; });
    assert(result === 10);
    assert(_const_val === 1);
    assert(_val === 1);
    assert(_var === 1);
    foo._var = 1000;
    assert(foo._var === 1000);
    assert(foo._valCustom === 1);
    assert(foo._valCustomWithField === 2);
    assert(foo._varCustom === 1);
    foo._varCustom = 20;
    assert(foo._varCustom === 1);
    assert(foo._varCustomWithField === 10);
    foo._varCustomWithField = 10;
    assert(foo._varCustomWithField === 1000);
    new A();
    assert(new A1(10).x === 10);
    assert(new A2("10", true).x === "10");
    assert(new A3().x === 100);
    var a4 = new A4();
    assert(a4._valCustom === 1);
    assert(a4._valCustomWithField === 2);
    assert(a4._varCustom === 1);
    a4._varCustom = 20;
    assert(a4._varCustom === 1);
    assert(a4._varCustomWithField === 10);
    a4._varCustomWithField = 10;
    assert(a4._varCustomWithField === 1000);
    return "OK";
}
