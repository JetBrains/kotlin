import foo = JS_TESTS.foo;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

assert(foo._long === 1n);
assert(foo._long_array instanceof BigInt64Array);
assert(foo._array_long instanceof Array);
assert(foo._n_long === 1n);

function box(): string {
    return "OK";
}