@_implementationOnly import KotlinBridges_inheritance
import KotlinRuntime
import KotlinRuntimeSupport

public final class INHERITANCE_SINGLE_CLASS: inheritance.OPEN_CLASS {
    public var value: Swift.Int32 {
        get {
            return INHERITANCE_SINGLE_CLASS_value_get(self.__externalRCRef())
        }
        set {
            return INHERITANCE_SINGLE_CLASS_value_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    public init(
        value: Swift.Int32
    ) {
        if Self.self != inheritance.INHERITANCE_SINGLE_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from inheritance.INHERITANCE_SINGLE_CLASS ") }
        let __kt = __root___INHERITANCE_SINGLE_CLASS_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___INHERITANCE_SINGLE_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, value)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class OBJECT_WITH_CLASS_INHERITANCE: inheritance.OPEN_CLASS {
    public static var shared: inheritance.OBJECT_WITH_CLASS_INHERITANCE {
        get {
            return inheritance.OBJECT_WITH_CLASS_INHERITANCE.__createClassWrapper(externalRCRef: __root___OBJECT_WITH_CLASS_INHERITANCE_get())
        }
    }
    private override init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
open class OPEN_CLASS: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != inheritance.OPEN_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from inheritance.OPEN_CLASS ") }
        let __kt = __root___OPEN_CLASS_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
open class OPEN_CLASS_WITH_PROTECTED_FUNCTION: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != inheritance.OPEN_CLASS_WITH_PROTECTED_FUNCTION.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from inheritance.OPEN_CLASS_WITH_PROTECTED_FUNCTION ") }
        let __kt = __root___OPEN_CLASS_WITH_PROTECTED_FUNCTION_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___OPEN_CLASS_WITH_PROTECTED_FUNCTION_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
