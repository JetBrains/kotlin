import TestInterfaceImpl = JS_TESTS.foo.TestInterfaceImpl;
import ChildTestInterfaceImpl = JS_TESTS.foo.ChildTestInterfaceImpl;
import processInterface = JS_TESTS.foo.processInterface;
import processOptionalInterface = JS_TESTS.foo.processOptionalInterface;
import WithTheCompanion = JS_TESTS.foo.WithTheCompanion;
import ImplementorOfInterfaceWithDefaultArguments = JS_TESTS.foo.ImplementorOfInterfaceWithDefaultArguments;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(processInterface(new TestInterfaceImpl("bar")) === "Owner TestInterfaceImpl has value 'bar'")
    assert(processInterface(new ChildTestInterfaceImpl()) === "Owner TestInterfaceImpl has value 'Test'")

    // @ts-expect-error "Just test that this code will throw compilation error for a user"
    assert(processInterface({ value: "bar", getOwnerName: () => "RandomObject" }) === "Owner RandomObject has value 'bar'")

    assert(processOptionalInterface({ required: 4 }) == "4unknown")
    assert(processOptionalInterface({ required: 4, notRequired: null }) == "4unknown")
    assert(processOptionalInterface({ required: 4, notRequired: 5 }) == "45")

    assert(WithTheCompanion.companionFunction() == "FUNCTION")

    const instance = new ImplementorOfInterfaceWithDefaultArguments()
    assert(instance.foo() === 0);
    assert(instance.foo(2) === 2);
    assert(instance.bar() === 1);
    assert(instance.bar(2) === 3);

    return "OK";
}