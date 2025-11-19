import bar = JS_TESTS.foo.bar
import funktion = JS_TESTS.foo.funktion

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(bar == "Test");
    assert(funktion() == undefined);

    return "OK";
}