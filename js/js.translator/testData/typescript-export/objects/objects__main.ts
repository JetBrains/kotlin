import O = JS_TESTS.foo.O;
import O0 = JS_TESTS.foo.O0;
import Parent = JS_TESTS.foo.Parent;
import takesO = JS_TESTS.foo.takesO;
import getParent = JS_TESTS.foo.getParent;
import createNested1 = JS_TESTS.foo.createNested1;
import createNested2 = JS_TESTS.foo.createNested2;
import createNested3 = JS_TESTS.foo.createNested3;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(typeof O0 === "object" && O0 != null);
    assert(O.x === 10);
    assert(O.foo() === 20);
    assert(takesO(O) === 30)

    // Do not strip types from those test cases (it is a check of nested objects types usability)
    const parent: typeof Parent = Parent
    const nested1: typeof Parent.Nested1 = Parent.Nested1
    const nested2: Parent.Nested1.Nested2 = new Parent.Nested1.Nested2()
    const nested3: Parent.Nested1.Nested2.Companion.Nested3 = new Parent.Nested1.Nested2.Companion.Nested3()

    assert(nested1.value === "Nested1")
    assert(getParent() === parent)
    assert(createNested1() === nested1)
    assert(createNested2() !== nested2 && createNested2() instanceof Parent.Nested1.Nested2)
    assert(createNested3() !== nested3 && createNested3() instanceof Parent.Nested1.Nested2.Companion.Nested3)
    return "OK";
}