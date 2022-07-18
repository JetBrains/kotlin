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
var Test = JS_TESTS.foo.Test;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    var test = new Test();
    assert(test.sum(10, 20) === 30);
    assert(test.varargInt(new Int32Array([1, 2, 3])) === 3);
    assert(test.varargNullableInt([10, 20, 30, null, undefined, 40]) === 6);
    assert(test.varargWithOtherParameters("1234", ["1", "2", "3"], "12") === 9);
    assert(test.varargWithComplexType([]) === 0);
    assert(test.varargWithComplexType([
        function (x) { return x; },
        function (x) { return [new Int32Array([1, 2, 3])]; },
        function (x) { return []; },
    ]) === 3);
    assert(test.sumNullable(10, null) === 10);
    assert(test.sumNullable(undefined, 20) === 20);
    assert(test.sumNullable(1, 2) === 3);
    assert(test.defaultParameters("", 20, "OK") === "20OK");
    assert(test.generic1("FOO") === "FOO");
    assert(test.generic1({ x: 10 }).x === 10);
    assert(test.generic2(null) === true);
    assert(test.generic2(undefined) === true);
    assert(test.generic2(10) === false);
    assert(test.generic3(10, true, "__", {}) === null);
    assert(test.genericWithConstraint("Test") === "Test");
    var regExpMatchError = __assign(__assign({}, new Error("Test")), "test test".match(/tes/g));
    assert(test.genericWithMultipleConstraints(regExpMatchError) === regExpMatchError);
    var result = 0;
    test.inlineFun(10, function (x) { result = x; });
    assert(result === 10);
    return "OK";
}
