@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Container: KotlinRuntime.KotlinBase {
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public var member_property: Swift.Never {
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
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public func member_consuming_hidden(
        arg: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
    public func untouched_member(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return Container_untouched_member__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), arg)
    }
}
public final class WithHiddenCtorParam: KotlinRuntime.KotlinBase {
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public var x: Swift.Never {
        get {
            fatalError()
        }
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public init(
        x: Swift.Never
    ) {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public var hidden_val: Swift.Never {
    get {
        fatalError()
    }
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public var hidden_var: Swift.Never {
    get {
        fatalError()
    }
    set {
        fatalError()
    }
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func consume_hidden_class(
    arg: Swift.Never
) -> Swift.Void {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func consume_hidden_interface(
    arg: Swift.Never
) -> Swift.Void {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func consume_hidden_lambda(
    arg: @escaping (Swift.Never) -> Swift.Void
) -> Swift.Void {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func overloaded(
    arg: Swift.Never
) -> Swift.Void {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func produce_hidden_class() -> Swift.Never {
    fatalError()
}
public func untouched_function(
    arg: Swift.Int32
) -> Swift.Int32 {
    return __root___untouched_function__TypesOfArguments__Swift_Int32__(arg)
}
