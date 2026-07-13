import TestDataClass = JS_TESTS.foo.TestDataClass;
import KT39423 = JS_TESTS.foo.KT39423;
import Test2 = JS_TESTS.foo.Test2;
import shortNameBasedDestructuring = JS_TESTS.foo.shortNameBasedDestructuring;
import fullNameBasedDestructuring = JS_TESTS.foo.fullNameBasedDestructuring;
import shortPositionBasedDestructuring = JS_TESTS.foo.shortPositionBasedDestructuring;
import fullPositionBasedDestructuring = JS_TESTS.foo.fullPositionBasedDestructuring;
import WithIgnoredPrimaryAndPropertyAndHiddenCopy = JS_TESTS.foo.WithIgnoredPrimaryAndPropertyAndHiddenCopy;
import WithIgnoredPrimaryAndPropertyAndExposedCopy = JS_TESTS.foo.WithIgnoredPrimaryAndPropertyAndExposedCopy;
import WithIgnoredPropertyAndExposedCopy = JS_TESTS.foo.WithIgnoredPropertyAndExposedCopy;
import createWithIgnoredPrimaryAndHiddenCopyWithoutSecondary = JS_TESTS.foo.createWithIgnoredPrimaryAndHiddenCopyWithoutSecondary;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(new TestDataClass("Test").name === "Test");
    assert(new TestDataClass("Test").copy("NewTest").name === "NewTest");
    assert(new TestDataClass("Test").toString() === "TestDataClass(name=Test)");

    assert(new TestDataClass("Test").hashCode() === new TestDataClass("Test").hashCode());
    assert(new TestDataClass("Test").hashCode() !== new TestDataClass("AnotherTest").hashCode());

    assert(new TestDataClass("Test").equals(new TestDataClass("Test")));
    assert(!new TestDataClass("Test").equals(new TestDataClass("AnotherTest")));

    assert(new TestDataClass.Nested().prop === "hello");

    assert(new KT39423("Test").a === "Test")
    assert(new KT39423("Test").b === null)
    assert(new KT39423("Test", null).a === "Test")
    assert(new KT39423("Test", null).b === null)

    assert(new KT39423("Test", 42).a === "Test")
    assert(new KT39423("Test", 42).b === 42)

    assert(new KT39423("Test", 42).copy("NewTest").a === "NewTest")
    assert(new KT39423("Test", 42).copy("NewTest").b === 42)
    assert(new KT39423("Test", 42).copy("Test", null).a === "Test")
    assert(new KT39423("Test", 42).copy("Test", null).b === null)

    assert(new KT39423("Test").toString() === "KT39423(a=Test, b=null)")
    assert(new KT39423("Test", null).toString() === "KT39423(a=Test, b=null)")
    assert(new KT39423("Test", 42).toString() === "KT39423(a=Test, b=42)")

    assert(new Test2("1", "2").value1 === "1")
    assert(new Test2("1", "2").value2 === "2")

    assert(new Test2("1", "2").copy("3").value1 === "3")
    assert(new Test2("1", "2").copy("3").value2 === "2")
    assert(new Test2("1", "2").copy(undefined, "3").value1 === "1")
    assert(new Test2("1", "2").copy(undefined, "3").value2 === "3")

    assert(new Test2("1", "2").component1() === "1")

    assert(shortNameBasedDestructuring() === "42")
    assert(fullNameBasedDestructuring() === "4 2")
    assert(shortPositionBasedDestructuring() === "42")
    assert(fullPositionBasedDestructuring() === "4 2")

    const hiddenCopy = WithIgnoredPrimaryAndPropertyAndHiddenCopy.create(7, "Hidden");
    assert(hiddenCopy.a === 7);
    assert(hiddenCopy.b === "Hidden");
    assert(typeof (hiddenCopy as any).copy === "undefined");

    const exposedCopy = WithIgnoredPrimaryAndPropertyAndExposedCopy.create(1, "Exposed");
    assert(exposedCopy.a === 1);
    assert(exposedCopy.b === "Exposed");
    const exposedCopyResult = exposedCopy.copy(2, "Copy");
    assert(exposedCopyResult.a === 2);
    assert(exposedCopyResult.b === "Copy");
    const exposedCopyResultWithDefaultA = exposedCopy.copy(undefined, "DefaultA");
    assert(exposedCopyResultWithDefaultA.a === 1);
    assert(exposedCopyResultWithDefaultA.b === "DefaultA");

    const ignoredProperty = new WithIgnoredPropertyAndExposedCopy(7, 11);
    assert(ignoredProperty.value === 7);
    assert(typeof (ignoredProperty as any).hidden === "undefined");
    const copiedIgnoredProperty = ignoredProperty.copy(17, 23);
    assert(copiedIgnoredProperty.value === 17);
    assert(typeof (copiedIgnoredProperty as any).hidden === "undefined");

    const hiddenCopyWithoutSecondary = createWithIgnoredPrimaryAndHiddenCopyWithoutSecondary(11);
    assert(hiddenCopyWithoutSecondary.value === 11);
    assert(typeof (hiddenCopyWithoutSecondary as any).copy === "undefined");

    return "OK";
}
