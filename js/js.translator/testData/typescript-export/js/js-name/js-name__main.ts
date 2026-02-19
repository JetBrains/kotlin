import JsNameTest = JS_TESTS.foo.JsNameTest;
import TestInterface = JS_TESTS.foo.TestInterface;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    const jsNameTest = JsNameTest.NotCompanion.create() as TestInterface;

    assert(jsNameTest.value === 4)
    assert(jsNameTest.runTest() === "JsNameTest")
    assert(jsNameTest.acceptObject({ constructor: Function }) === "Function")

    assert(jsNameTest.testName1() === "name1")
    assert(jsNameTest.testName2() === "name2")
    assert(jsNameTest.getWithSetter1() === "name1")
    assert(jsNameTest.getWithSetter2() === "name2")

    jsNameTest.setWithSetter1("changed name1")
    assert(jsNameTest.getWithSetter1() === "name1")

    jsNameTest.setWithSetter2("changed name2")
    assert(jsNameTest.getWithSetter2() === "changed name2")

    const jsNameNestedTest = JsNameTest.NotCompanion.createChild(42);

    assert(jsNameNestedTest.value === 42)

    return "OK";
}