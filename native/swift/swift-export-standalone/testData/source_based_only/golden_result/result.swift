import KotlinBridges
import KotlinRuntime

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
        let __kt = __root___MyClass_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___MyClass_init_initialize__TypesOfArguments__uintptr_t__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
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
