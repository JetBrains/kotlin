import bar = JS_TESTS.foo.bar
import foo = JS_TESTS.foo.foo

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(bar == "Test");
    assert(foo() == undefined);

    return "OK";
}