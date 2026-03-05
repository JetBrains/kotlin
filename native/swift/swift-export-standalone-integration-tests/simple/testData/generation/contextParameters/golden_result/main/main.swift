@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Context: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.Context.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Context ") }
        let __kt = __root___Context_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Context_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class ContextA: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.ContextA.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.ContextA ") }
        let __kt = __root___ContextA_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ContextA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class ContextB: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.ContextB.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.ContextB ") }
        let __kt = __root___ContextB_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ContextB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func bar(
        _ context: main.Context,
        arg: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> any KotlinRuntimeSupport._KotlinBridgeable {
        let (ctx) = context
        return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: Foo_bar__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_main_Context__(self.__externalRCRef(), arg.__externalRCRef(), ctx.__externalRCRef()))
    }
    public func complexContextFunction(
        _ context: (contextA: main.ContextA, context: main.Context, contextB: main.ContextB),
        _ receiver: Swift.String,
        count: Swift.Int32
    ) -> Swift.Bool {
        let (contextA, context, contextB) = context
        return Foo_complexContextFunction__TypesOfArguments__Swift_String_Swift_Int32_main_ContextA_main_Context_main_ContextB__(self.__externalRCRef(), receiver, count, contextA.__externalRCRef(), context.__externalRCRef(), contextB.__externalRCRef())
    }
    public func foo(
        _ context: main.Context
    ) -> Swift.Void {
        let (ctx) = context
        return { Foo_foo__TypesOfArguments__main_Context__(self.__externalRCRef(), ctx.__externalRCRef()); return () }()
    }
    public func getBaz(
        _ context: main.Context
    ) -> any KotlinRuntimeSupport._KotlinBridgeable {
        let (ctx) = context
        return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: Foo_baz_get__TypesOfArguments__main_Context__(self.__externalRCRef(), ctx.__externalRCRef()))
    }
    public func getComplexContextProperty(
        _ context: (contextB: main.ContextB, contextA: main.ContextA),
        _ receiver: Swift.String
    ) -> Swift.Int32 {
        let (contextB, contextA) = context
        return Foo_complexContextProperty_get__TypesOfArguments__Swift_String_main_ContextB_main_ContextA__(self.__externalRCRef(), receiver, contextB.__externalRCRef(), contextA.__externalRCRef())
    }
    public func getUnnamedContextParametersProperty(
        _ context: (ctx: main.ContextA, main.Context)
    ) -> Swift.String {
        let (ctx, _1) = context
        return Foo_unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__(self.__externalRCRef(), ctx.__externalRCRef(), _1.__externalRCRef())
    }
    public func setComplexContextProperty(
        _ context: (contextB: main.ContextB, contextA: main.ContextA),
        _ receiver: Swift.String,
        value: Swift.Int32
    ) -> Swift.Void {
        let (contextB, contextA) = context
        return { Foo_complexContextProperty_set__TypesOfArguments__Swift_String_Swift_Int32_main_ContextB_main_ContextA__(self.__externalRCRef(), receiver, value, contextB.__externalRCRef(), contextA.__externalRCRef()); return () }()
    }
    public func setUnnamedContextParametersProperty(
        _ context: (ctx: main.ContextA, main.Context),
        value: Swift.String
    ) -> Swift.Void {
        let (ctx, _2) = context
        return { Foo_unnamedContextParametersProperty_set__TypesOfArguments__Swift_String_main_ContextA_main_Context__(self.__externalRCRef(), value, ctx.__externalRCRef(), _2.__externalRCRef()); return () }()
    }
    public func unnamedContextParametersFunction(
        _ context: (main.Context, ctx: main.ContextB)
    ) -> Swift.Void {
        let (_0, ctx) = context
        return { Foo_unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__(self.__externalRCRef(), _0.__externalRCRef(), ctx.__externalRCRef()); return () }()
    }
}
public func bar(
    _ context: main.Context,
    arg: any KotlinRuntimeSupport._KotlinBridgeable
) -> any KotlinRuntimeSupport._KotlinBridgeable {
    let (ctx) = context
    return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: __root___bar__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_main_Context__(arg.__externalRCRef(), ctx.__externalRCRef()))
}
public func complexContextFunction(
    _ context: (context: main.Context, contextA: main.ContextA, contextB: main.ContextB),
    _ receiver: Swift.String,
    yes: Swift.Bool
) -> Swift.Int32 {
    let (context, contextA, contextB) = context
    return __root___complexContextFunction__TypesOfArguments__Swift_String_Swift_Bool_main_Context_main_ContextA_main_ContextB__(receiver, yes, context.__externalRCRef(), contextA.__externalRCRef(), contextB.__externalRCRef())
}
public func contextBlockA(
    block: @escaping ((main.ContextA, main.ContextB), Swift.Int32, Swift.String) -> Swift.Void
) -> Swift.Void {
    return { __root___contextBlockA__TypesOfArguments__U28Swift_Int32_U20Swift_StringU29202D_U20Swift_Void__({
        let originalBlock = block
        return { ctx0, ctx1, arg0, arg1 in return { originalBlock((main.ContextA.__createClassWrapper(externalRCRef: ctx0),main.ContextB.__createClassWrapper(externalRCRef: ctx1)), arg0, arg1); return true }() }
    }()); return () }()
}
public func contextBlockB() -> ((main.ContextB, main.ContextA), Swift.String, Swift.Int32) -> Swift.Void {
    return {
        let pointerToBlock = __root___contextBlockB()
        return { context, _3, _4 in let (ctx0, ctx1) = context;return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_ContextB_main_ContextA_Swift_String_Swift_Int32__(pointerToBlock, ctx0.__externalRCRef(), ctx1.__externalRCRef(), _3, _4); return () }() }
    }()
}
public func contextBlockC(
    block: @escaping (main.Context, Swift.String) -> Swift.Void
) -> Swift.Void {
    return { __root___contextBlockC__TypesOfArguments__U28Swift_StringU29202D_U20Swift_Void__({
        let originalBlock = block
        return { ctx0, arg0 in return { originalBlock((main.Context.__createClassWrapper(externalRCRef: ctx0)), arg0); return true }() }
    }()); return () }()
}
public func contextBlockD() -> (main.Context, Swift.Int32) -> Swift.Void {
    return {
        let pointerToBlock = __root___contextBlockD()
        return { context, _2 in let (ctx0) = context;return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Context_Swift_Int32__(pointerToBlock, ctx0.__externalRCRef(), _2); return () }() }
    }()
}
public func foo(
    _ context: main.Context
) -> Swift.Void {
    let (ctx) = context
    return { __root___foo__TypesOfArguments__main_Context__(ctx.__externalRCRef()); return () }()
}
public func getBaz(
    _ context: main.Context
) -> any KotlinRuntimeSupport._KotlinBridgeable {
    let (ctx) = context
    return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: __root___baz_get__TypesOfArguments__main_Context__(ctx.__externalRCRef()))
}
public func getComplexContextProperty(
    _ context: (contextA: main.ContextA, contextB: main.ContextB),
    _ receiver: Swift.String
) -> Swift.Bool {
    let (contextA, contextB) = context
    return __root___complexContextProperty_get__TypesOfArguments__Swift_String_main_ContextA_main_ContextB__(receiver, contextA.__externalRCRef(), contextB.__externalRCRef())
}
public func getUnnamedContextParametersProperty(
    _ context: (main.ContextA, ctx: main.Context)
) -> Swift.Int32 {
    let (_0, ctx) = context
    return __root___unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__(_0.__externalRCRef(), ctx.__externalRCRef())
}
public func setComplexContextProperty(
    _ context: (contextA: main.ContextA, contextB: main.ContextB),
    _ receiver: Swift.String,
    value: Swift.Bool
) -> Swift.Void {
    let (contextA, contextB) = context
    return { __root___complexContextProperty_set__TypesOfArguments__Swift_String_Swift_Bool_main_ContextA_main_ContextB__(receiver, value, contextA.__externalRCRef(), contextB.__externalRCRef()); return () }()
}
public func setUnnamedContextParametersProperty(
    _ context: (main.ContextA, ctx: main.Context),
    value: Swift.Int32
) -> Swift.Void {
    let (_1, ctx) = context
    return { __root___unnamedContextParametersProperty_set__TypesOfArguments__Swift_Int32_main_ContextA_main_Context__(value, _1.__externalRCRef(), ctx.__externalRCRef()); return () }()
}
public func unnamedContextParametersFunction(
    _ context: (ctx: main.Context, main.ContextB)
) -> Swift.Void {
    let (ctx, _1) = context
    return { __root___unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__(ctx.__externalRCRef(), _1.__externalRCRef()); return () }()
}
