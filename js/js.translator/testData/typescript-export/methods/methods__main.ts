import Test = JS_TESTS.foo.Test;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    const test = new Test()
    assert(test.sum(10, 20) === 30);

    assert(test.varargInt(new Int32Array([1, 2, 3])) === 3);
    assert(test.varargNullableInt([10, 20, 30, null, undefined, 40]) === 6);
    assert(test.varargWithOtherParameters("1234", ["1", "2", "3"], "12") === 9);
    assert(test.varargWithComplexType([]) === 0);
    assert(test.varargWithComplexType([
        x => x,
        x => [new Int32Array([1, 2, 3])],
        x => [],
    ]) === 3);

    assert(test.sumNullable(10, null) === 10);
    assert(test.sumNullable(undefined, 20) === 20);
    assert(test.sumNullable(1, 2) === 3);

    assert(test.defaultParameters("", 20, "OK") === "20OK");

    assert(test.generic1<string>("FOO") === "FOO");
    assert(test.generic1({x: 10}).x === 10);
    assert(test.generic2(null) === true);
    assert(test.generic2(undefined) === true);
    assert(test.generic2(10) === false);
    assert(test.generic3(10, true, "__", {}) === null);

    assert(test.genericWithConstraint("Test") === "Test")

    const regExpMatchError: any = { ...new Error("Test"), ..."test test".match(/tes/g) }
    assert(test.genericWithMultipleConstraints(regExpMatchError) === regExpMatchError)

    let result: number = 0;
    test.inlineFun(10, x => { result = x; });
    assert(result === 10);

    return "OK";
}