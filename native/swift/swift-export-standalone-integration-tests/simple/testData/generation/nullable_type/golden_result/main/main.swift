@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public typealias NonoptionalRef = main.Bar
public typealias OptOptRef = main.OptionalRef
public typealias OptToNonOptTypealias = main.NonoptionalRef?
public typealias OptionalRef = main.Bar?
public final class Bar: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.Bar.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Bar ") }
        let __kt = __root___Bar_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class Foo: KotlinRuntime.KotlinBase {
    public var any_value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch Foo_any_value_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any KotlinRuntimeSupport._KotlinBridgeable; } }()
        }
    }
    public var value: main.Bar? {
        get {
            return { switch Foo_value_get(self.__externalRCRef()) { case nil: .none; case let res: main.Bar.__createClassWrapper(externalRCRef: res); } }()
        }
    }
    public var variable: main.Bar? {
        get {
            return { switch Foo_variable_get(self.__externalRCRef()) { case nil: .none; case let res: main.Bar.__createClassWrapper(externalRCRef: res); } }()
        }
        set {
            return Foo_variable_set__TypesOfArguments__Swift_Optional_main_Bar___(self.__externalRCRef(), newValue.map { it in it.__externalRCRef() } ?? nil)
        }
    }
    public init(
        b: main.Bar?
    ) {
        if Self.self != main.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Foo ") }
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_main_Bar___(__kt, b.map { it in it.__externalRCRef() } ?? nil)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func accept(
        b: main.Bar?
    ) -> Swift.Void {
        return Foo_accept__TypesOfArguments__Swift_Optional_main_Bar___(self.__externalRCRef(), b.map { it in it.__externalRCRef() } ?? nil)
    }
    public func produce() -> main.Bar? {
        return { switch Foo_produce(self.__externalRCRef()) { case nil: .none; case let res: main.Bar.__createClassWrapper(externalRCRef: res); } }()
    }
}
public var primitive: Swift.Double? {
    get {
        return __root___primitive_get().map { it in it.doubleValue }
    }
    set {
        return __root___primitive_set__TypesOfArguments__Swift_Optional_Swift_Double___(newValue.map { it in NSNumber(value: it) } ?? nil)
    }
}
public var str: Swift.String? {
    get {
        return __root___str_get()
    }
    set {
        return __root___str_set__TypesOfArguments__Swift_Optional_Swift_String___(newValue ?? nil)
    }
}
public func foo(
    a: main.Bar
) -> Swift.Void {
    return __root___foo__TypesOfArguments__main_Bar__(a.__externalRCRef())
}
public func foo(
    a: main.Bar?
) -> Swift.Void {
    return __root___foo__TypesOfArguments__Swift_Optional_main_Bar___(a.map { it in it.__externalRCRef() } ?? nil)
}
public func foo_any(
    a: any KotlinRuntimeSupport._KotlinBridgeable
) -> Swift.Void {
    return __root___foo_any__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(a.__externalRCRef())
}
public func foo_any(
    a: (any KotlinRuntimeSupport._KotlinBridgeable)?
) -> Swift.Void {
    return __root___foo_any__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(a.map { it in it.__externalRCRef() } ?? nil)
}
public func opt_to_non_opt_usage(
    i: main.OptToNonOptTypealias
) -> Swift.Void {
    return __root___opt_to_non_opt_usage__TypesOfArguments__Swift_Optional_main_Bar___(i.map { it in it.__externalRCRef() } ?? nil)
}
public func p() -> main.Bar? {
    return { switch __root___p() { case nil: .none; case let res: main.Bar.__createClassWrapper(externalRCRef: res); } }()
}
public func p_any() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return { switch __root___p_any() { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any KotlinRuntimeSupport._KotlinBridgeable; } }()
}
public func p_opt_opt_in(
    input: main.OptOptRef
) -> Swift.Void {
    return __root___p_opt_opt_in__TypesOfArguments__Swift_Optional_main_Bar___(input.map { it in it.__externalRCRef() } ?? nil)
}
public func p_opt_opt_out() -> main.OptOptRef {
    return { switch __root___p_opt_opt_out() { case nil: .none; case let res: main.Bar.__createClassWrapper(externalRCRef: res); } }()
}
public func primitive_in(
    arg1: Swift.Bool?,
    arg2: Swift.Int8?,
    arg3: Swift.Int16?,
    arg4: Swift.Int32?,
    arg5: Swift.Int64?,
    arg6: Swift.UInt8?,
    arg7: Swift.UInt16?,
    arg8: Swift.UInt32?,
    arg9: Swift.UInt64?,
    arg10: Swift.Float?,
    arg11: Swift.Double?,
    arg12: Swift.Unicode.UTF16.CodeUnit?
) -> Swift.Void {
    return __root___primitive_in__TypesOfArguments__Swift_Optional_Swift_Bool__Swift_Optional_Swift_Int8__Swift_Optional_Swift_Int16__Swift_Optional_Swift_Int32__Swift_Optional_Swift_Int64__Swift_Optional_Swift_UInt8__Swift_Optional_Swift_UInt16__Swift_Optional_Swift_UInt32__Swift_Optional_Swift_UInt64__Swift_Optional_Swift_Float__Swift_Optional_Swift_Double__Swift_Optional_Swift_Unicode_UTF16_CodeUnit___(arg1.map { it in NSNumber(value: it) } ?? nil, arg2.map { it in NSNumber(value: it) } ?? nil, arg3.map { it in NSNumber(value: it) } ?? nil, arg4.map { it in NSNumber(value: it) } ?? nil, arg5.map { it in NSNumber(value: it) } ?? nil, arg6.map { it in NSNumber(value: it) } ?? nil, arg7.map { it in NSNumber(value: it) } ?? nil, arg8.map { it in NSNumber(value: it) } ?? nil, arg9.map { it in NSNumber(value: it) } ?? nil, arg10.map { it in NSNumber(value: it) } ?? nil, arg11.map { it in NSNumber(value: it) } ?? nil, arg12.map { it in NSNumber(value: it) } ?? nil)
}
public func primitive_out() -> Swift.Bool? {
    return __root___primitive_out().map { it in it.boolValue }
}
public func string_in(
    a: Swift.String?
) -> Swift.Void {
    return __root___string_in__TypesOfArguments__Swift_Optional_Swift_String___(a ?? nil)
}
public func string_out() -> Swift.String? {
    return __root___string_out()
}
