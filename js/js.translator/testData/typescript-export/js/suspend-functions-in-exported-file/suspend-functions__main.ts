import Test = JS_TESTS.foo.Test;
import TestChild = JS_TESTS.foo.TestChild;
import sum = JS_TESTS.foo.sum;
import varargInt = JS_TESTS.foo.varargInt
import varargNullableInt = JS_TESTS.foo.varargNullableInt
import varargWithOtherParameters= JS_TESTS.foo.varargWithOtherParameters
import varargWithComplexType = JS_TESTS.foo.varargWithComplexType
import sumNullable = JS_TESTS.foo.sumNullable
import defaultParameters = JS_TESTS.foo.defaultParameters
import generic1 = JS_TESTS.foo.generic1
import generic2 = JS_TESTS.foo.generic2
import genericWithConstraint = JS_TESTS.foo.genericWithConstraint
import acceptTest = JS_TESTS.foo.acceptTest

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

async function box(): Promise<string> {
    assert(await sum(1, 2) === 3);
    assert(await varargInt(new Int32Array([1, 2, 3])) === 3);
    assert(await varargNullableInt([1, null, 3]) === 3);
    assert(await varargWithOtherParameters("start", ["a", "b"], "end") === 10);
    assert(await varargWithComplexType([(x: Array<Int32Array>) => x]) === 1);
    assert(await sumNullable(null, 5) === 5);
    assert(await defaultParameters("test") === "test10OK");
    assert(await defaultParameters("test", 20) === "test20OK");
    assert(await defaultParameters("test", 20, "custom") === "test20custom");
    assert(await generic1("string") === "string");
    assert(await generic2<string>(null));
    assert(await genericWithConstraint("constrained") === "constrained");

    const test = new Test();
    await acceptTest(test);
    assert(await test.sum(1, 2) === 3);
    assert(await test.varargInt(new Int32Array([1, 2, 3])) === 3);
    assert(await test.varargNullableInt([1, null, 3]) === 3);
    assert(await test.varargWithOtherParameters("start", ["a", "b"], "end") === 10);
    assert(await test.varargWithComplexType([(x: Array<Int32Array>) => x]) === 1);
    assert(await test.sumNullable(null, 5) === 5);
    assert(await test.defaultParameters("test") === "test10OK");
    assert(await test.generic1("string") === "string");
    assert(await test.generic2<string>(null));
    assert(await test.genericWithConstraint("constrained") === "constrained");

    const testChild = new TestChild();
    await acceptTest(testChild);
    assert(await testChild.varargInt(new Int32Array([1, 2, 3])) === 5);
    assert(await testChild.sumNullable(null, null) === 2);
    const result = await testChild.generic3<number, string, boolean, number, string>(1, "test", true, 1.0);
    assert(typeof result === "string");

    class TypeScriptTest extends Test {
        override async varargInt(x: Int32Array): Promise<number> {
           return x.length + 2
        }

        override async defaultParameters(a: string, x: number = "O".charCodeAt(0), y: string = "K"): Promise<string> {
            return String.fromCharCode(x) + y;
        }

        override async generic2<T>(x: T | null | undefined): Promise<boolean> {
            return false
        }
    }

    const typeScriptTest = new TypeScriptTest();
    assert(await typeScriptTest.varargInt(new Int32Array([1, 2, 3])) === 5);
    assert(await typeScriptTest.defaultParameters("test") === "OK");
    assert(!(await typeScriptTest.generic2<string>(null)));

    await acceptTest(new TypeScriptTest())

    return "OK";
}