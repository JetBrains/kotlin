@available(*, deprecated, message: "Deprecated class")
public class OPEN_INTERNAL {
    // Check that nested attributes handled properly
    @available(*, message: "Available method")
    public func method() -> Swift.Bool {
        stub()
    }
}
/// Example docstring
@available(*, obsoleted, message: "Obsolete variable")
public var myVariable: Swift.Bool {
    get {
        stub()
    }
}
@available(*, deprecated, obsoleted, message: "Oh no")
public func foo() -> Swift.Bool {
    stub()
}