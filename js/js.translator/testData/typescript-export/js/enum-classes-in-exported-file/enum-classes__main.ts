import TestEnumClass = JS_TESTS.foo.TestEnumClass;
import OuterClass = JS_TESTS.foo.OuterClass;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(TestEnumClass.A.foo == 0)
    assert(TestEnumClass.B.foo == 1)
    assert(TestEnumClass.A.bar("aBar") == "aBar")
    assert(TestEnumClass.B.bar("bBar") == "bBar")
    assert(TestEnumClass.A.bay() == "A")
    assert(TestEnumClass.B.bay() == "B")
    assert(TestEnumClass.A.constructorParameter == "aConstructorParameter")
    assert(TestEnumClass.B.constructorParameter == "bConstructorParameter")

    assert(TestEnumClass.valueOf("A") === TestEnumClass.A)
    assert(TestEnumClass.valueOf("B") === TestEnumClass.B)

    assert(TestEnumClass.values().indexOf(TestEnumClass.A) != -1)
    assert(TestEnumClass.values().indexOf(TestEnumClass.B) != -1)

    assert(TestEnumClass.A.name === "A")
    assert(TestEnumClass.B.name === "B")
    assert(TestEnumClass.A.ordinal === 0)
    assert(TestEnumClass.B.ordinal === 1)

    assert(new TestEnumClass.Nested().prop == "hello2")

    assert(OuterClass.NestedEnum.valueOf("A") === OuterClass.NestedEnum.A)
    assert(OuterClass.NestedEnum.valueOf("B") === OuterClass.NestedEnum.B)

    assert(OuterClass.NestedEnum.values().indexOf(OuterClass.NestedEnum.A) != -1)
    assert(OuterClass.NestedEnum.values().indexOf(OuterClass.NestedEnum.B) != -1)

    assert(OuterClass.NestedEnum.A.name === "A")
    assert(OuterClass.NestedEnum.B.name === "B")
    assert(OuterClass.NestedEnum.A.ordinal === 0)
    assert(OuterClass.NestedEnum.B.ordinal === 1)

    return "OK";
}