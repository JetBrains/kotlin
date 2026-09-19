import Inheritance
import Testing

private class Sub: Base {}

@Test
func swiftSubclassCanCallInheritedMethodWithCachedBridges() throws {
    #expect(Base().foo() == "foo")
    // KT-89121: without the cached class adapter, the generated bridge throws
    // ClassCastException: class Sub cannot be cast to class Base.
    #expect(Sub().foo() == "foo")
}
