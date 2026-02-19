import SomeBaseClass = JS_TESTS.foo.SomeBaseClass;
import SomeExtendingClass = JS_TESTS.foo.SomeExtendingClass;
import FinalClassInChain = JS_TESTS.foo.FinalClassInChain;

function box(): string {
    // @ts-expect-error "the constructor is private and can't be used from JS/TS code"
    new SomeBaseClass(4)

    // @ts-expect-error "the constructor is private and can't be used from JS/TS code"
    new SomeExtendingClass()

    new FinalClassInChain()

    return "OK";
}