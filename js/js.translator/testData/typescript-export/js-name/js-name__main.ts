import JsNameTest = JS_TESTS.foo.JsNameTest;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    const jsNameTest = JsNameTest.NotCompanion.create();

    assert(jsNameTest.value === 4)
    assert(jsNameTest.runTest() === "JsNameTest")
    assert(jsNameTest.acceptObject(Object) === "Function")

    const jsNameNestedTest = JsNameTest.NotCompanion.createChild(42);

    assert(jsNameNestedTest.value === 42)

    return "OK";
}