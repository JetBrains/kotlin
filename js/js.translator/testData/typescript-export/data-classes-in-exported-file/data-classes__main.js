"use strict";
var TestDataClass = JS_TESTS.foo.TestDataClass;
var KT39423 = JS_TESTS.foo.KT39423;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(new TestDataClass("Test").name === "Test");
    assert(new TestDataClass("Test").component1() === "Test");
    assert(new TestDataClass("Test").copy("NewTest").name === "NewTest");
    assert(new TestDataClass("Test").toString() === "TestDataClass(name=Test)");
    assert(new TestDataClass("Test").hashCode() === new TestDataClass("Test").hashCode());
    assert(new TestDataClass("Test").hashCode() !== new TestDataClass("AnotherTest").hashCode());
    assert(new TestDataClass("Test").equals(new TestDataClass("Test")));
    assert(!new TestDataClass("Test").equals(new TestDataClass("AnotherTest")));
    assert(new TestDataClass.Nested().prop === "hello");
    assert(new KT39423("Test").a === "Test");
    assert(new KT39423("Test").b === null);
    assert(new KT39423("Test", null).a === "Test");
    assert(new KT39423("Test", null).b === null);
    assert(new KT39423("Test", 42).a === "Test");
    assert(new KT39423("Test", 42).b === 42);
    assert(new KT39423("Test", 42).component1() === "Test");
    assert(new KT39423("Test", 42).component2() === 42);
    assert(new KT39423("Test", 42).copy("NewTest").a === "NewTest");
    assert(new KT39423("Test", 42).copy("NewTest").b === 42);
    assert(new KT39423("Test", 42).copy("Test", null).a === "Test");
    assert(new KT39423("Test", 42).copy("Test", null).b === null);
    assert(new KT39423("Test").toString() === "KT39423(a=Test, b=null)");
    assert(new KT39423("Test", null).toString() === "KT39423(a=Test, b=null)");
    assert(new KT39423("Test", 42).toString() === "KT39423(a=Test, b=42)");
    return "OK";
}
