"use strict";
var TestInterfaceImpl = JS_TESTS.foo.TestInterfaceImpl;
var ChildTestInterfaceImpl = JS_TESTS.foo.ChildTestInterfaceImpl;
var processInterface = JS_TESTS.foo.processInterface;
var processOptionalInterface = JS_TESTS.foo.processOptionalInterface;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(processInterface(new TestInterfaceImpl("bar")) === "Owner TestInterfaceImpl has value 'bar'");
    assert(processInterface(new ChildTestInterfaceImpl()) === "Owner TestInterfaceImpl has value 'Test'");
    // @ts-expect-error "Just test that this code will throw compilation error for a user"
    assert(processInterface({ value: "bar", getOwnerName: function () { return "RandomObject"; } }) === "Owner RandomObject has value 'bar'");
    assert(processOptionalInterface({ required: 4 }) == "4unknown");
    assert(processOptionalInterface({ required: 4, notRequired: null }) == "4unknown");
    assert(processOptionalInterface({ required: 4, notRequired: 5 }) == "45");
    return "OK";
}
