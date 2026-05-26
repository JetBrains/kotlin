public struct Foo {
    public let constantValue: Swift.Bool
    public var storedValue: Swift.Int
    public var readOnlyValue: Swift.String {
        get {
            "hello"
        }
    }
    public var writeableValue: Swift.Int {
        get {
            storedValue
        }
        set {
            storedValue = newValue
        }
    }
    public init() {
        constantValue = true
        storedValue = 1
    }
}
