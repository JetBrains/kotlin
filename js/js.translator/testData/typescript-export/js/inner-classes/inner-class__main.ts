import TestInner = JS_TESTS.foo.TestInner;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    const outer = new TestInner("Hello ")
    const inner = new outer.Inner("World")
    const innerFromNumber = outer.Inner.fromNumber(1654)
    const secondInner = new inner.SecondLayerInner("!")
    const subclassOfAbstractInner = new outer.SubclassOfAbstractInnerClass("42")
    const subclassOfOpenInner = new outer.SubclassOfOpenInnerClass("24")

    assert(outer instanceof TestInner)
    assert(inner instanceof TestInner.Inner)
    assert(innerFromNumber instanceof TestInner.Inner)
    assert(secondInner instanceof TestInner.Inner.SecondLayerInner)

    assert(inner.a == "World")
    assert(inner.concat == "Hello World")

    assert(innerFromNumber.a == "1654")
    assert(innerFromNumber.concat == "Hello 1654")

    assert(secondInner.a == "!")
    assert(secondInner.concat == "Hello World!")

    assert(subclassOfAbstractInner.a == "42")
    assert(subclassOfAbstractInner.concat == "Hello 42")

    assert(subclassOfOpenInner.a == "24")
    assert(subclassOfOpenInner.concat == "Hello 24")

    return "OK";
}