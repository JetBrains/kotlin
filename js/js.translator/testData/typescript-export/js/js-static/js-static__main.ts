import WithIgnoredCompanion = JS_TESTS.foo.WithIgnoredCompanion;
import WithoutIgnoredCompanion = JS_TESTS.foo.WithoutIgnoredCompanion;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

async function box(): Promise<string> {
    assert(WithIgnoredCompanion.bar() === "BARRRR");
    assert(WithIgnoredCompanion.foo === "FOOOO");
    assert(WithIgnoredCompanion.baz === "BAZZZZ");
    assert(WithIgnoredCompanion.mutable === "INITIAL");
    WithIgnoredCompanion.mutable = "CHANGED"
    assert(WithIgnoredCompanion.mutable === "CHANGED")
    assert(await WithIgnoredCompanion.staticSuspend() === "STATIC SUSPEND")

    assert(WithoutIgnoredCompanion.bar() === "BARRRR");
    assert(WithoutIgnoredCompanion.foo === "FOOOO");
    assert(WithoutIgnoredCompanion.baz === "BAZZZZ");
    assert(WithoutIgnoredCompanion.mutable === "INITIAL");
    WithoutIgnoredCompanion.mutable = "CHANGED"
    assert(WithoutIgnoredCompanion.mutable === "CHANGED")
    assert(await WithoutIgnoredCompanion.staticSuspend() === "STATIC SUSPEND")

    assert(WithoutIgnoredCompanion.Companion.hidden() === "BARRRR");
    assert(WithoutIgnoredCompanion.Companion.delegated === "BAZZZZ");
    assert(await WithoutIgnoredCompanion.Companion.companionSuspend() === "SUSPEND")

    return "OK";
}