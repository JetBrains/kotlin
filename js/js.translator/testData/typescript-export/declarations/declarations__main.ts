import {
    sum,
    varargInt,
    varargNullableInt,
    varargWithOtherParameters,
    varargWithComplexType,
    sumNullable,
    defaultParameters,
    generic1,
    generic2,
    generic3,
    inlineFun,
    A,
    A1,
    A2,
    A4,
    A3
} from "./JS_TESTS/index.js";

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

assert(sum(10, 20) === 30);
assert(varargInt(new Int32Array([1, 2, 3])) === 3);
assert(varargNullableInt([10, 20, 30, null, undefined, 40]) === 6);
assert(varargWithOtherParameters("1234", ["1", "2", "3"], "12") === 9);
assert(varargWithComplexType([]) === 0);
assert(varargWithComplexType([
    x => x,
    x => [new Int32Array([1, 2, 3])],
    x => [],
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
let result = 0;
inlineFun(10, (x) => { result = x; });
assert(result === 10);
new A();
assert(new A1(10).x === 10);
assert(new A2("10", true).x === "10");
assert(new A3().x === 100);
const a4 = new A4();
assert(a4._valCustom === 1);
assert(a4._valCustomWithField === 2);
assert(a4._varCustom === 1);
a4._varCustom = 20;
assert(a4._varCustom === 1);
assert(a4._varCustomWithField === 10);
a4._varCustomWithField = 10;
assert(a4._varCustomWithField === 1000);