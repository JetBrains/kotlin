import SomeBaseClass = JS_TESTS.foo.SomeBaseClass;
import SomeExtendingClass = JS_TESTS.foo.SomeExtendingClass;
import FinalClassInChain = JS_TESTS.foo.FinalClassInChain;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    // @ts-expect-error "the constructor is private and can't be used from JS/TS code"
    const baseClass = new SomeBaseClass(4)
    assert(baseClass.answer === 4)

    // @ts-expect-error "the constructor is private and can't be used from JS/TS code"
    const extendingClass = new SomeExtendingClass()
    assert(extendingClass.answer === 42)

    const finalClassInChain = new FinalClassInChain()
    assert(finalClassInChain.answer === 42)

    return "OK";
}