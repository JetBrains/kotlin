@_implementationOnly import KotlinBridges_inheritance
import KotlinRuntime

public final class INHERITANCE_SINGLE_CLASS : inheritance.OPEN_CLASS {
    public var value: Swift.Int32 {
        get {
            return INHERITANCE_SINGLE_CLASS_value_get(self.__externalRCRef())
        }
        set {
            return INHERITANCE_SINGLE_CLASS_value_set__TypesOfArguments__int32_t__(self.__externalRCRef(), newValue)
        }
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        value: Swift.Int32
    ) {
        let __kt = __root___INHERITANCE_SINGLE_CLASS_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___INHERITANCE_SINGLE_CLASS_init_initialize__TypesOfArguments__uintptr_t_int32_t__(__kt, value)
    }
}
public final class OBJECT_WITH_CLASS_INHERITANCE : inheritance.OPEN_CLASS {
    public static var shared: inheritance.OBJECT_WITH_CLASS_INHERITANCE {
        get {
            return inheritance.OBJECT_WITH_CLASS_INHERITANCE(__externalRCRef: __root___OBJECT_WITH_CLASS_INHERITANCE_get())
        }
    }
    private override init() {
        fatalError()
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
open class OPEN_CLASS : KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___OPEN_CLASS_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___OPEN_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
