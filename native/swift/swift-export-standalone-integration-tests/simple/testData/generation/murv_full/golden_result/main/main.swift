@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import dependency

public final class Derived: ExportedKotlinPackages.dependency.DependencyBase {
    public override init() {
        let __kt = __root___Derived_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Derived_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public override func checked() -> Swift.Int32 {
        return Derived_checked(self.__externalRCRef())
    }
    @discardableResult
    public override func ignorable() -> Swift.Int32 {
        return Derived_ignorable(self.__externalRCRef())
    }
}
public final class Foo: KotlinRuntime.KotlinBase {
    public var property: Swift.Int32 {
        get {
            return Foo_property_get(self.__externalRCRef())
        }
    }
    public override init() {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    @discardableResult
    public func ignorableMember() -> Swift.Int32 {
        return Foo_ignorableMember(self.__externalRCRef())
    }
    public func member() -> Swift.Int32 {
        return Foo_member(self.__externalRCRef())
    }
}
public func function() -> Swift.Int32 {
    return __root___function()
}
@discardableResult
public func ignorable() -> Swift.Int32 {
    return __root___ignorable()
}
public func nothingFunction() -> Swift.Never {
    return { __root___nothingFunction(); fatalError() }()
}
public func unitFunction() -> Swift.Void {
    return { __root___unitFunction(); return () }()
}
