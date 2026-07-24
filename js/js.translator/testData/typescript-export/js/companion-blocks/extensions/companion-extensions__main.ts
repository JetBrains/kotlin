import append = JS_TESTS.foo.append;

function assert(condition: boolean, message: string) {
    if (!condition) {
        throw `FAIL: ${message}`;
    }
}

function box(): string {
    assert(append() === "OK", "append default");
    assert(append("L") === "OL", "append argument");

    return "OK";
}
