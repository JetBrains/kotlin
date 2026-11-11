import Foundation

public class Foo: NSObject {
    public override init() {}

    public func bar() {}

    public func baz() {}

    public static func staticFunction() {}

    public func withParameter(x: Int) {}

    public func withResult() -> Int { 42 }

    public func throwing() throws {}

    func internalMethod() {}
}

public class PureSwift {
    public init() {}

    public init(x: Int) {}

    public init?(failable: Void) { nil }

    public func foo() {}
}

public class Generic<T> {
    public init() {}

    public func foo() {}
}

public struct NotAClass {
    public func foo() {}
}

class InternalClass {
    func foo() {}
}
