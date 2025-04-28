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
    public init(
        value: Swift.Int32
    ) {
        precondition(Self.self == cross_module_inheritance.CLASS_ACROSS_MODULES.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from cross_module_inheritance.CLASS_ACROSS_MODULES ")
        let __kt = __root___CLASS_ACROSS_MODULES_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___CLASS_ACROSS_MODULES_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, value)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
