"use strict";
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
var sum = JS_TESTS.foo.sum;
var generic1 = JS_TESTS.foo.generic1;
var generic2 = JS_TESTS.foo.generic2;
var generic3 = JS_TESTS.foo.generic3;
var inlineFun = JS_TESTS.foo.inlineFun;
var varargInt = JS_TESTS.foo.varargInt;
var sumNullable = JS_TESTS.foo.sumNullable;
var defaultParameters = JS_TESTS.foo.defaultParameters;
var varargNullableInt = JS_TESTS.foo.varargNullableInt;
var varargWithOtherParameters = JS_TESTS.foo.varargWithOtherParameters;
var varargWithComplexType = JS_TESTS.foo.varargWithComplexType;
var genericWithConstraint = JS_TESTS.foo.genericWithConstraint;
var genericWithMultipleConstraints = JS_TESTS.foo.genericWithMultipleConstraints;
var formatList = JS_TESTS.foo.formatList;
var createList = JS_TESTS.foo.createList;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(sum(10, 20) === 30);
    assert(varargInt(new Int32Array([1, 2, 3])) === 3);
    assert(varargNullableInt([10, 20, 30, null, undefined, 40]) === 6);
    assert(varargWithOtherParameters("1234", ["1", "2", "3"], "12") === 9);
    assert(varargWithComplexType([]) === 0);
    assert(varargWithComplexType([
        function (x) { return x; },
        function () { return [new Int32Array([1, 2, 3])]; },
        function () { return []; },
    ]) === 3);
    assert(sumNullable(10, null) === 10);
    assert(sumNullable(undefined, 20) === 20);
    assert(sumNullable(1, 2) === 3);
    assert(defaultParameters("", 20, "OK") === "20OK");
    assert(generic1("FOO") === "FOO");
    assert(generic1({ x: 10 }).x === 10);
    assert(generic2(null));
    assert(generic2(undefined));
    assert(!generic2(10));
    assert(generic3(10, true, "__", {}) === null);
    assert(genericWithConstraint("Test") === "Test");
    var regExpMatchError = __assign(__assign({}, new Error("Test")), "test test".match(/tes/g));
    assert(genericWithMultipleConstraints(regExpMatchError) === regExpMatchError);
    var result = 0;
    inlineFun(10, function (x) { result = x; });
    assert(result === 10);
    assert(formatList(createList()) === "1, 2, 3");
    return "OK";
}
