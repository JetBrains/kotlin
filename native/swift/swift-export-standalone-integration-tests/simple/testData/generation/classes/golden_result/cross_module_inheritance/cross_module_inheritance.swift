@_implementationOnly import KotlinBridges_cross_module_inheritance
import KotlinRuntime
import KotlinRuntimeSupport
import inheritance

public final class CLASS_ACROSS_MODULES: inheritance.OPEN_CLASS {
    public var value: Swift.Int32 {
        get {
            return CLASS_ACROSS_MODULES_value_get(self.__externalRCRef())
        }
        set {
            return CLASS_ACROSS_MODULES_value_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        value: Swift.Int32
    ) {
        let __kt = __root___CLASS_ACROSS_MODULES_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___CLASS_ACROSS_MODULES_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, value)
    }
}
