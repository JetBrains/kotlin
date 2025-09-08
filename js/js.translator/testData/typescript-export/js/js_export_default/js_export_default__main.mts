import Parent, { getParent } from "./js_export_default-lib_v5.mjs";

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

export function box(): string {
    const parent = Parent.getInstance()
    const nested1 = parent.Nested1
    const nested2 = new parent.Nested1.Nested2()
    const nested3 = new parent.Nested1.Nested2.Companion.Nested3()

    assert(nested1.value === "Nested1")
    assert(getParent() === parent)
    assert(getParent().Nested1 === nested1)
    assert(nested2 instanceof getParent().Nested1.Nested2)
    assert(nested3 instanceof getParent().Nested1.Nested2.Companion.Nested3)

    return "OK";
}