public protocol my_protocol {
}
extension Swift.Int32 {
}
private extension Swift.Int32 {
}
///
/// this is a documented extension
/// (is it even possible? Printer don't actually care)
///
extension Swift.Int32 {
}
extension Swift.Int32 {
    public class Foo {
    }
    public var my_variable1: Swift.Bool {
        get {
            stub()
        }
    }
    public func foo() -> Swift.Bool {
        stub()
    }
}
extension Test.my_enum {
    public class Foo {
    }
}
extension MyDependencyModule.my_external_enum {
    public class Foo {
    }
}
extension Test.my_enum: Test.my_protocol where Self == Test.my_protocol {
}
extension Test.my_enum: Test.my_protocol where NestedType1.NestedType2 == Test.my_protocol, NestedType1.NestedType2 : Test.my_protocol {
}
public enum my_enum {
}