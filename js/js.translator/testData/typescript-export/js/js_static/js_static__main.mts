import { ObjectWithJsStatic, WithIgnoredCompanion, WithoutIgnoredCompanion } from "./js_static-lib_v5.mjs";

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

export async function box(): Promise<string> {
    assert(WithIgnoredCompanion.bar() === "BARRRR");
    assert(WithIgnoredCompanion.foo === "FOOOO");
    assert(WithIgnoredCompanion.baz === "BAZZZZ");
    assert(WithIgnoredCompanion.mutable === "INITIAL");
    WithIgnoredCompanion.mutable = "CHANGED"
    assert(WithIgnoredCompanion.mutable === "CHANGED")
    assert(await WithIgnoredCompanion.staticSuspend() === "STATIC SUSPEND")
    assert(await WithIgnoredCompanion.staticSuspendWithDefault() === "DEFAULT STATIC SUSPEND")
    assert(await WithIgnoredCompanion.staticSuspendWithDefault("PROVIDED VALUE") === "PROVIDED VALUE")

    assert(WithoutIgnoredCompanion.bar() === "BARRRR");
    assert(WithoutIgnoredCompanion.foo === "FOOOO");
    assert(WithoutIgnoredCompanion.baz === "BAZZZZ");
    assert(WithoutIgnoredCompanion.mutable === "INITIAL");
    WithoutIgnoredCompanion.mutable = "CHANGED"
    assert(WithoutIgnoredCompanion.mutable === "CHANGED")
    assert(await WithoutIgnoredCompanion.staticSuspend() === "STATIC SUSPEND")
    assert(await WithoutIgnoredCompanion.staticSuspendWithDefault() === "DEFAULT STATIC SUSPEND")
    assert(await WithoutIgnoredCompanion.staticSuspendWithDefault("PROVIDED VALUE") === "PROVIDED VALUE")

    assert(WithoutIgnoredCompanion.Companion.hidden() === "BARRRR");
    assert(WithoutIgnoredCompanion.Companion.delegated === "BAZZZZ");
    assert(await WithoutIgnoredCompanion.Companion.companionSuspend() === "SUSPEND")
    assert(await WithoutIgnoredCompanion.Companion.companionSuspendWithDefault() === "DEFAULT COMPANION SUSPEND")
    assert(await WithoutIgnoredCompanion.Companion.companionSuspendWithDefault("PROVIDED VALUE") === "PROVIDED VALUE")

    assert(ObjectWithJsStatic.bar() === "BARRRR");
    assert(ObjectWithJsStatic.foo === "FOOOO");
    assert(ObjectWithJsStatic.baz === "BAZZZZ");
    assert(ObjectWithJsStatic.mutable === "INITIAL");
    ObjectWithJsStatic.mutable = "CHANGED"
    assert(ObjectWithJsStatic.mutable === "CHANGED")
    assert(await ObjectWithJsStatic.staticSuspend() === "STATIC SUSPEND")
    assert(await ObjectWithJsStatic.staticSuspendWithDefault() === "DEFAULT STATIC SUSPEND")
    assert(await ObjectWithJsStatic.staticSuspendWithDefault("PROVIDED VALUE") === "PROVIDED VALUE")

    assert(ObjectWithJsStatic.getInstance().hidden() === "BARRRR");
    assert(ObjectWithJsStatic.getInstance().delegated === "BAZZZZ");
    assert(await ObjectWithJsStatic.getInstance().companionSuspend() === "SUSPEND")
    assert(await ObjectWithJsStatic.getInstance().companionSuspendWithDefault() === "DEFAULT COMPANION SUSPEND")
    assert(await ObjectWithJsStatic.getInstance().companionSuspendWithDefault("PROVIDED VALUE") === "PROVIDED VALUE")

    return "OK";
}
