import KotlinRuntime

public typealias X = ERROR_TYPE
public class MyClass : KotlinRuntime.KotlinBase {
    public var x: ERROR_TYPE {
        get {
            fatalError()
        }
    }
    public var y: ERROR_TYPE {
        get {
            fatalError()
        }
    }
    public override init() {
        fatalError()
    }
    public func method(
        arg: Swift.Int32
    ) -> ERROR_TYPE {
        fatalError()
    }
}
public var fakeUsage: ERROR_TYPE {
    get {
        fatalError()
    }
}
public var x: UNSUPPORTED_TYPE {
    get {
        fatalError()
    }
}
public func foo() -> ERROR_TYPE {
    fatalError()
}
public func parametrized() -> UNSUPPORTED_TYPE {
    fatalError()
}
