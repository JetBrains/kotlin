import sum = JS_TESTS.foo.sum;
import generic1 = JS_TESTS.foo.generic1;
import generic2 = JS_TESTS.foo.generic2;
import generic3 = JS_TESTS.foo.generic3;
import inlineFun = JS_TESTS.foo.inlineFun;
import varargInt = JS_TESTS.foo.varargInt;
import sumNullable = JS_TESTS.foo.sumNullable;
import defaultParameters = JS_TESTS.foo.defaultParameters;
import varargNullableInt = JS_TESTS.foo.varargNullableInt;
import varargWithOtherParameters = JS_TESTS.foo.varargWithOtherParameters;
import varargWithComplexType = JS_TESTS.foo.varargWithComplexType;
import genericWithConstraint = JS_TESTS.foo.genericWithConstraint;
import genericWithMultipleConstraints = JS_TESTS.foo.genericWithMultipleConstraints;
import formatList = JS_TESTS.foo.formatList;
import createList = JS_TESTS.foo.createList;
import defaultParametersAtTheBegining = JS_TESTS.foo.defaultParametersAtTheBegining;
import nonDefaultParametersInBetween = JS_TESTS.foo.nonDefaultParameterInBetween;
function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(sum(10, 20) === 30);

    assert(varargInt(new Int32Array([1, 2, 3])) === 3);
    assert(varargNullableInt([10, 20, 30, null, undefined, 40]) === 6);
    assert(varargWithOtherParameters("1234", ["1", "2", "3"], "12") === 9);
    assert(varargWithComplexType([]) === 0);
    assert(varargWithComplexType([
        x => x,
        () => [new Int32Array([1, 2, 3])],
        () => [],
    ]) === 3);

    assert(sumNullable(10, null) === 10);
    assert(sumNullable(undefined, 20) === 20);
    assert(sumNullable(1, 2) === 3);

    assert(defaultParameters("", 20, "OK") === "20OK");

    assert(generic1<string>("FOO") === "FOO");
    assert(generic1({x: 10}).x === 10);
    assert(generic2(null));
    assert(generic2(undefined));
    assert(!generic2(10));
    assert(generic3(10, true, "__", {}) === null);

    assert(genericWithConstraint("Test") === "Test")

    const regExpMatchError: any = { ...new Error("Test"), ..."test test".match(/tes/g) }
    assert(genericWithMultipleConstraints(regExpMatchError) === regExpMatchError)


    let result: number = 0;
    inlineFun(10, x => { result = x; });
    assert(result === 10);

    assert(formatList(createList()) === "1, 2, 3")

    assert(defaultParametersAtTheBegining("A", "B") == "A and B")
    assert(defaultParametersAtTheBegining(undefined, "B") == "Default Value and B")

    assert(nonDefaultParametersInBetween("A",  "B", "C") == "A and B and C")
    assert(nonDefaultParametersInBetween("A",  "B") == "A and B and Default C")
    assert(nonDefaultParametersInBetween(undefined,  "B", "C") == "Default A and B and C")
    assert(nonDefaultParametersInBetween(undefined,  "B") == "Default A and B and Default C")

    return "OK";
}
