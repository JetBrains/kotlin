/// Example docstring
@available(*, unavailable, message: "Unavailable typealias")
public typealias myVariable = Swift.Bool
@available(*, deprecated, message: "Deprecated class")
public class OPEN_INTERNAL {
    // Check that nested attributes handled properly
    @available(*, deprecated, message: "Deprecated method")
    public func method() -> Swift.Bool {
        stub()
    }
}
/// Example docstring
@available(*, deprecated, message: "Deprecated variable")
public var myVariable: Swift.Bool {
    get {
        stub()
    }
}
@available(*, deprecated, message: "Oh no")
public func foo() -> Swift.Bool {
    stub()
}