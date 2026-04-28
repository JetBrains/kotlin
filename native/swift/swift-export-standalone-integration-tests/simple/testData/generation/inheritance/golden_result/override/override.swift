@_implementationOnly import KotlinBridges_override
import KotlinRuntime
import KotlinRuntimeSupport

public protocol P: KotlinRuntime.KotlinBase, override._P {
    func f() -> Swift.Void
}
@objc(_P)
public protocol _P {
}
open class Base: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = __root___Base_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Base_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func g(
        x: any override.P
    ) -> Swift.Void {
        return { Base_g__TypesOfArguments__anyU20override_P__(self.__externalRCRef(), x.__externalRCRef()); return () }()
    }
}
open class Sub: override.Base {
    public override init() {
        let __kt = __root___Sub_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Sub_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open override func g(
        x: any override.P
    ) -> Swift.Void {
        return { Sub_g__TypesOfArguments__anyU20override_P__(self.__externalRCRef(), x.__externalRCRef()); return () }()
    }
}
extension override.P where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func f() -> Swift.Void {
        return { P_f(self.__externalRCRef()); return () }()
    }
}
extension override.P {
}
extension KotlinRuntimeSupport._KotlinExistential: override.P where Wrapped : override._P {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: override._P {
}
@_cdecl("Base_g__TypesOfArguments__anyU20override_P____reverse_swift")
public func Base_g__TypesOfArguments__anyU20override_P____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ x: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = override.Base.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.g(x: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: x) as! any override.P)
    return { _result; return true }()
}

@_cdecl("P_f__reverse_swift")
public func P_f__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any override.P
    let _result: Swift.Void = _self.f()
    return { _result; return true }()
}

@_cdecl("Sub_g__TypesOfArguments__anyU20override_P____reverse_swift")
public func Sub_g__TypesOfArguments__anyU20override_P____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ x: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = override.Sub.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.g(x: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: x) as! any override.P)
    return { _result; return true }()
}
