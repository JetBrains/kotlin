@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import hidden

public final class Container: KotlinRuntime.KotlinBase {
    public var member_property: ExportedKotlinPackages.hidden.HiddenClass {
        get {
            fatalError()
        }
    }
    public init() {
        let __kt = __root___Container_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Container_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func member_consuming_hidden(
        arg: ExportedKotlinPackages.hidden.HiddenClass
    ) -> Swift.Void {
        fatalError()
    }
    public func untouched_member(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return Container_untouched_member__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), arg)
    }
}
public final class ImplementsHiddenInterface: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = __root___ImplementsHiddenInterface_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ImplementsHiddenInterface_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class InheritsAndImplements: ExportedKotlinPackages.hidden.HiddenOpenClass {
    public init() {
        let __kt = __root___InheritsAndImplements_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___InheritsAndImplements_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class InheritsHiddenClass: ExportedKotlinPackages.hidden.HiddenOpenClass {
    public init() {
        let __kt = __root___InheritsHiddenClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___InheritsHiddenClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public var hidden_val: ExportedKotlinPackages.hidden.HiddenClass {
    get {
        fatalError()
    }
}
public func consume_hidden_class(
    arg: ExportedKotlinPackages.hidden.HiddenClass
) -> Swift.Void {
    fatalError()
}
public func consume_hidden_interface(
    arg: any ExportedKotlinPackages.hidden.HiddenInterface
) -> Swift.Void {
    fatalError()
}
public func produce_hidden_class() -> ExportedKotlinPackages.hidden.HiddenClass {
    fatalError()
}
public func untouched_function(
    arg: Swift.Int32
) -> Swift.Int32 {
    return __root___untouched_function__TypesOfArguments__Swift_Int32__(arg)
}
