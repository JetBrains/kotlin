import withIntroducedAt = JS_TESTS.foo.withIntroducedAt;
import ConstructorVersioning = JS_TESTS.foo.ConstructorVersioning;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(withIntroducedAt(42) == "OK42");
    assert(new ConstructorVersioning(42).y === 42)
    assert(new ConstructorVersioning(4).ok1 === "OK")
    assert(new ConstructorVersioning(2).ok2 === "OK")

    return "OK";
}
