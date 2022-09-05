import TestInterfaceImpl = JS_TESTS.foo.TestInterfaceImpl;
import ChildTestInterfaceImpl = JS_TESTS.foo.ChildTestInterfaceImpl;
import processInterface = JS_TESTS.foo.processInterface;

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

    return "OK";
}