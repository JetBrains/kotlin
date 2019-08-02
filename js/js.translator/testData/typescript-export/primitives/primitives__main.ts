import foo = JS_TESTS.foo;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

assert(typeof foo._any === "object");
assert(foo._string === "ZZZ");
assert(foo._boolean === true);
assert(foo._byte === 1);
assert(foo._short === 1);
assert(foo._float === 1);
assert(foo._double === 1);
assert(foo._byte_array instanceof Int8Array);
assert(foo._short_array instanceof Int16Array);
assert(foo._int_array instanceof Int32Array);
assert(foo._float_array instanceof Float32Array);
assert(foo._double_array instanceof Float64Array);
assert(foo._array_byte instanceof Array);
assert(foo._array_short instanceof Array);
assert(foo._array_int instanceof Array);
assert(foo._array_float instanceof Array);
assert(foo._array_double instanceof Array);
assert(foo._array_string instanceof Array);
assert(foo._array_boolean instanceof Array);

assert(foo._array_array_string instanceof Array);
assert(foo._array_array_string[0] instanceof Array);

assert(foo._array_array_int_array instanceof Array);
assert(foo._array_array_int_array[0] instanceof Array);
assert(foo._array_array_int_array[0][0] instanceof Int32Array);

foo._fun_unit();
foo._fun_int_unit(10);
assert(foo._fun_boolean_int_string_intarray(true, 20, "A") instanceof Int32Array);
assert(foo._curried_fun(1)(2)(3)(4)(5) == 15);
assert(foo._higher_order_fun((n) => String(n), (s) => s.length)(1000) == 4);
assert(foo._n_any != null);
assert(foo._n_nothing == null);
assert(foo._n_throwable instanceof Error);
assert(foo._n_string === "ZZZ");
assert(foo._n_boolean === true);
assert(foo._n_byte === 1);
assert(foo._n_short_array instanceof Int16Array);
assert(foo._n_array_int instanceof Array);
assert(foo._array_n_int instanceof Array);
assert(foo._n_array_n_int instanceof Array);

assert(foo._array_n_array_string instanceof Array);
let x = foo._array_n_array_string[0];
assert(x != null);
assert(x instanceof Array);
assert(x == null ? false : (x[0] === ":)"));

foo._fun_n_int_unit(null);

assert(foo._fun_n_boolean_n_int_n_string_n_intarray(false, undefined, "ZZZ") == null);
assert(foo._n_curried_fun(10)(null)(30) === 40);
assert(foo._n_higher_order_fun(
    n => String(n),
    s => (s == null ? 10 : s.length)
)(1000) === 4);

function box(): string {
    return "OK";
}