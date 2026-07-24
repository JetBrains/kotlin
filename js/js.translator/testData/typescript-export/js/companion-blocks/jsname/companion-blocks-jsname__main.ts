import WithJsName = JS_TESTS.foo.WithJsName;

function assert(condition: boolean, message: string) {
    if (!condition) {
        throw `FAIL: ${message}`;
    }
}

function box(): string {
    assert(WithJsName.renamedVal === "K", "renamedVal");
    assert(WithJsName.renamedFun() === "OK", "renamedFun");

    return "OK";
}
