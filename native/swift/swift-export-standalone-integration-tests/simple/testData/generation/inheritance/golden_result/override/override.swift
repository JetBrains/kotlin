@_implementationOnly import KotlinBridges_override
import KotlinRuntime
import KotlinRuntimeSupport

public protocol P: KotlinRuntime.KotlinBase {
    func f() -> Swift.Void
}
@objc(_P)
package protocol _P {
}
open class Base: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != override.Base.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from override.Base ") }
        let __kt = __root___Base_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Base_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    open func g(
        x: any override.P
    ) -> Swift.Void {
        return Base_g__TypesOfArguments__anyU20override_P__(self.__externalRCRef(), x.__externalRCRef())
    }
}
open class Sub: override.Base {
    public override init() {
        if Self.self != override.Sub.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from override.Sub ") }
        let __kt = __root___Sub_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Sub_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    open override func g(
        x: any override.P
    ) -> Swift.Void {
        return Sub_g__TypesOfArguments__anyU20override_P__(self.__externalRCRef(), x.__externalRCRef())
    }
}
extension override.P where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func f() -> Swift.Void {
        return P_f(self.__externalRCRef())
    }
}
extension override.P {
}
extension KotlinRuntimeSupport._KotlinExistential: override.P where Wrapped : override._P {
}
