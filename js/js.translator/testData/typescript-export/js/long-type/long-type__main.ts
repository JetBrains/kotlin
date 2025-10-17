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
foo.myVar = 1n
assert(foo.myVar === 2n);

assert(foo.funWithLongParameters(1n, 1n) === 2n);
assert(foo.funWithLongDefaultParameters() === 2n);
assert(foo.funWithLongDefaultParameters(2n, 1n) === 3n);
assert(foo.funWithTypeParameter(1n, 1n) === 2n);
assert(foo.funWithTypeParameterWithTwoUpperBounds(1n, 1n) === 2n);
assert(foo.funWithContextParameter(1n) === 1n);
assert(foo.inlineFun(1n, 1n) === 2n);
assert(foo.inlineFunWithTypeParameter(1n, 1n) === 2n);
assert(foo.inlineFunDefaultParameters() === 2n);
assert(foo.inlineFunDefaultParameters(2n, 1n) === 3n);
assert(foo.extensionFun(1n) === 1n);
assert(foo.globalFun(1n) === 1n)
assert(foo.objectWithLong.long === 1n)

const classA = new foo.A(1n);
assert(classA.a === 1n);
// @ts-expect-error
const badClassB = new foo.B(1n);
const classB = foo.B.snd_constructor();
assert(classB.b === 1n);
const classC = new foo.C(1n);
assert(classC.a === 1n);
const nestedClass = new foo.D.N(1n);
assert(nestedClass.n === 1n);
const classD = new foo.D();
const innerClass = new classD.I(1n);
assert(innerClass.i === 1n);

assert(foo.funInterfaceInheritor1.getLong(1n) === 1n)
assert(foo.funInterfaceInheritor2.getLong(1n) === 1n)


function box(): string {
    return "OK";
}