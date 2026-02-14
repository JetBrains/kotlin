@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Context: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.Context.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Context ") }
        let __kt = __root___Context_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Context_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class ContextA: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.ContextA.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.ContextA ") }
        let __kt = __root___ContextA_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___ContextA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class ContextB: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.ContextB.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.ContextB ") }
        let __kt = __root___ContextB_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___ContextB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class Foo: KotlinRuntime.KotlinBase {
    public static var shared: main.Foo {
        get {
            return main.Foo.__createClassWrapper(externalRCRef: __root___Foo_get())
        }
    }
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func bar(
        _ context: (main.Context),
        arg: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> any KotlinRuntimeSupport._KotlinBridgeable {
        let (ctx) = context
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: Foo_bar__TypesOfArguments__main_Context_anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), ctx.__externalRCRef(), arg.__externalRCRef())) as! any KotlinRuntimeSupport._KotlinBridgeable
    }
    public func complexContextFunction(
        _ context: (contextA: main.ContextA, context: main.Context, contextB: main.ContextB),
        _ receiver: Swift.String,
        count: Swift.Int32
    ) -> Swift.Bool {
        let (contextA, context, contextB) = context
        return Foo_complexContextFunction__TypesOfArguments__main_ContextA_main_Context_main_ContextB_Swift_String_Swift_Int32__(self.__externalRCRef(), contextA.__externalRCRef(), context.__externalRCRef(), contextB.__externalRCRef(), receiver, count)
    }
    public func foo(
        _ context: (main.Context)
    ) -> Swift.Void {
        let (ctx) = context
        return Foo_foo__TypesOfArguments__main_Context__(self.__externalRCRef(), ctx.__externalRCRef())
    }
    public func getBaz(
        _ context: (main.Context)
    ) -> any KotlinRuntimeSupport._KotlinBridgeable {
        let (ctx) = context
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: Foo_baz_get__TypesOfArguments__main_Context__(self.__externalRCRef(), ctx.__externalRCRef())) as! any KotlinRuntimeSupport._KotlinBridgeable
    }
    public func getComplexContextProperty(
        _ context: (contextB: main.ContextB, contextA: main.ContextA),
        _ receiver: Swift.String
    ) -> Swift.Int32 {
        let (contextB, contextA) = context
        return Foo_complexContextProperty_get__TypesOfArguments__main_ContextB_main_ContextA_Swift_String__(self.__externalRCRef(), contextB.__externalRCRef(), contextA.__externalRCRef(), receiver)
    }
    public func getUnnamedContextParametersProperty(
        _ context: (ctx: main.ContextA, main.Context)
    ) -> Swift.Void {
        let (ctx, _1) = context
        return Foo_unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__(self.__externalRCRef(), ctx.__externalRCRef(), _1.__externalRCRef())
    }
    public func setComplexContextProperty(
        _ context: (contextB: main.ContextB, contextA: main.ContextA),
        _ receiver: Swift.String,
        value: Swift.Int32
    ) -> Swift.Void {
        let (contextB, contextA) = context
        return Foo_complexContextProperty_set__TypesOfArguments__main_ContextB_main_ContextA_Swift_String_Swift_Int32__(self.__externalRCRef(), contextB.__externalRCRef(), contextA.__externalRCRef(), receiver, value)
    }
    public func setUnnamedContextParametersProperty(
        _ context: (ctx: main.ContextA, main.Context),
        value: Swift.Void
    ) -> Swift.Void {
        let (ctx, _1) = context
        return Foo_unnamedContextParametersProperty_set__TypesOfArguments__main_ContextA_main_Context_Swift_Void__(self.__externalRCRef(), ctx.__externalRCRef(), _1.__externalRCRef())
    }
    public func unnamedContextParametersFunction(
        _ context: (main.Context, ctx: main.ContextB)
    ) -> Swift.Void {
        let (_0, ctx) = context
        return Foo_unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__(self.__externalRCRef(), _0.__externalRCRef(), ctx.__externalRCRef())
    }
}
public func bar(
    _ context: (main.Context),
    arg: any KotlinRuntimeSupport._KotlinBridgeable
) -> any KotlinRuntimeSupport._KotlinBridgeable {
    let (ctx) = context
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___bar__TypesOfArguments__main_Context_anyU20KotlinRuntimeSupport__KotlinBridgeable__(ctx.__externalRCRef(), arg.__externalRCRef())) as! any KotlinRuntimeSupport._KotlinBridgeable
}
public func complexContextFunction(
    _ context: (context: main.Context, contextA: main.ContextA, contextB: main.ContextB),
    _ receiver: Swift.String,
    yes: Swift.Bool
) -> Swift.Int32 {
    let (context, contextA, contextB) = context
    return __root___complexContextFunction__TypesOfArguments__main_Context_main_ContextA_main_ContextB_Swift_String_Swift_Bool__(context.__externalRCRef(), contextA.__externalRCRef(), contextB.__externalRCRef(), receiver, yes)
}
public func foo(
    _ context: (main.Context)
) -> Swift.Void {
    let (ctx) = context
    return __root___foo__TypesOfArguments__main_Context__(ctx.__externalRCRef())
}
public func getBaz(
    _ context: (main.Context)
) -> any KotlinRuntimeSupport._KotlinBridgeable {
    let (ctx) = context
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___baz_get__TypesOfArguments__main_Context__(ctx.__externalRCRef())) as! any KotlinRuntimeSupport._KotlinBridgeable
}
public func getComplexContextProperty(
    _ context: (contextA: main.ContextA, contextB: main.ContextB),
    _ receiver: Swift.String
) -> Swift.Bool {
    let (contextA, contextB) = context
    return __root___complexContextProperty_get__TypesOfArguments__main_ContextA_main_ContextB_Swift_String__(contextA.__externalRCRef(), contextB.__externalRCRef(), receiver)
}
public func getUnnamedContextParametersProperty(
    _ context: (main.ContextA, ctx: main.Context)
) -> Swift.Void {
    let (_0, ctx) = context
    return __root___unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__(_0.__externalRCRef(), ctx.__externalRCRef())
}
public func setComplexContextProperty(
    _ context: (contextA: main.ContextA, contextB: main.ContextB),
    _ receiver: Swift.String,
    value: Swift.Bool
) -> Swift.Void {
    let (contextA, contextB) = context
    return __root___complexContextProperty_set__TypesOfArguments__main_ContextA_main_ContextB_Swift_String_Swift_Bool__(contextA.__externalRCRef(), contextB.__externalRCRef(), receiver, value)
}
public func setUnnamedContextParametersProperty(
    _ context: (main.ContextA, ctx: main.Context),
    value: Swift.Void
) -> Swift.Void {
    let (_0, ctx) = context
    return __root___unnamedContextParametersProperty_set__TypesOfArguments__main_ContextA_main_Context_Swift_Void__(_0.__externalRCRef(), ctx.__externalRCRef())
}
public func unnamedContextParametersFunction(
    _ context: (ctx: main.Context, main.ContextB)
) -> Swift.Void {
    let (ctx, _1) = context
    return __root___unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__(ctx.__externalRCRef(), _1.__externalRCRef())
}
