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
import generic3 = JS_TESTS.foo.generic3
import genericWithConstraint = JS_TESTS.foo.genericWithConstraint
import acceptTest = JS_TESTS.foo.acceptTest
import generateOneMoreChildOfTest = JS_TESTS.foo.generateOneMoreChildOfTest;
import ExportedChild = JS_TESTS.foo.ExportedChild;
import acceptExportedChild = JS_TESTS.foo.acceptExportedChild;
import inlineFun = JS_TESTS.foo.inlineFun;
import inlineChain = JS_TESTS.foo.inlineChain;
import suspendExtensionFun = JS_TESTS.foo.suspendExtensionFun;
import suspendFunWithContext = JS_TESTS.foo.suspendFunWithContext;
import WithSuspendExtensionFunAndContext = JS_TESTS.foo.WithSuspendExtensionFunAndContext;
import acceptHolderOfSum = JS_TESTS.foo.acceptHolderOfSum;

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
    assert(await generic3<number, string, boolean, (p0: number) => number, any>(42, "42", true, x => x) == null);
    assert(await genericWithConstraint("constrained") === "constrained");

    const test = new Test();
    await acceptTest(test);
    await acceptHolderOfSum(test);

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
    assert(await test.generic3<number, string, boolean, (p0: number) => number, any>(42, "42", true, x => x) == null);
    assert(await test.genericWithConstraint("constrained") === "constrained");


    assert(await inlineFun(42, x => x) == 42);
    assert(await inlineChain(42) == 42);
    assert(await suspendExtensionFun(42) == 42);
    assert(await suspendFunWithContext(42) == 42);
    const withSuspendExtensionFunAndContext = new WithSuspendExtensionFunAndContext();
    assert(await withSuspendExtensionFunAndContext.suspendFun(4, 2) == 6);

    const testChild = new TestChild();
    await acceptTest(testChild);
    await acceptHolderOfSum(testChild);
    assert(await testChild.varargInt(new Int32Array([1, 2, 3])) === 5);
    assert(await testChild.sumNullable(null, null) === 2);
    const testChildResult = await testChild.generic3<number, string, boolean, number, string>(1, "test", true, 1.0);
    assert(testChildResult === "OK");

    const anotherTestChild = generateOneMoreChildOfTest()
    await acceptTest(anotherTestChild);
    await acceptHolderOfSum(anotherTestChild);
    assert(await anotherTestChild.sum(1, 2) === 42);
    assert(await anotherTestChild.varargNullableInt([1, null, 3]) === 43);
    assert(await anotherTestChild.sumNullable(null, null) === 44);
    const anotherTestChildResult = await anotherTestChild.generic3<number, string, boolean, number, string>(1, "test", true, 1.0);
    assert(anotherTestChildResult === "45");

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
    await acceptHolderOfSum(new TypeScriptTest());

    const exportedChild = new ExportedChild()
    await acceptExportedChild(exportedChild)
    assert(await exportedChild.childSuspendFun() === "ExportedChild");

    class TypeScriptExportedChild extends ExportedChild {
        override async childSuspendFun(): Promise<string> {
            return "TypeScriptChild"
        }
    }

    const typeScriptChild = new TypeScriptExportedChild()
    await acceptExportedChild(typeScriptChild)
    assert(await typeScriptChild.childSuspendFun() === "TypeScriptChild");

    return "OK";
}