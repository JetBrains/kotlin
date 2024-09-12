import KotlinRuntime
@_implementationOnly import KotlinBridges_main

public typealias NonoptionalRef = main.Bar
public typealias OptOptRef = main.OptionalRef
public typealias OptToNonOptTypealias = main.NonoptionalRef?
public typealias OptionalRef = main.Bar?
public final class Bar : KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___Bar_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Bar_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class Foo : KotlinRuntime.KotlinBase {
    public var any_value: KotlinRuntime.KotlinBase? {
        get {
            return switch Foo_any_value_get(self.__externalRCRef()) { case 0: .none; case let res: KotlinRuntime.KotlinBase(__externalRCRef: res); }
        }
    }
    public var value: main.Bar? {
        get {
            return switch Foo_value_get(self.__externalRCRef()) { case 0: .none; case let res: main.Bar(__externalRCRef: res); }
        }
    }
    public var variable: main.Bar? {
        get {
            return switch Foo_variable_get(self.__externalRCRef()) { case 0: .none; case let res: main.Bar(__externalRCRef: res); }
        }
        set {
            return Foo_variable_set__TypesOfArguments__main_Bar_opt___(self.__externalRCRef(), newValue?.__externalRCRef() ?? 0)
        }
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        b: main.Bar?
    ) {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UInt_main_Bar_opt___(__kt, b?.__externalRCRef() ?? 0)
    }
    public func accept(
        b: main.Bar?
    ) -> Swift.Void {
        return Foo_accept__TypesOfArguments__main_Bar_opt___(self.__externalRCRef(), b?.__externalRCRef() ?? 0)
    }
    public func produce() -> main.Bar? {
        return switch Foo_produce(self.__externalRCRef()) { case 0: .none; case let res: main.Bar(__externalRCRef: res); }
    }
}
public var str: Swift.String? {
    get {
        return __root___str_get()
    }
    set {
        return __root___str_set__TypesOfArguments__Swift_String_opt___(newValue)
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
    return __root___foo__TypesOfArguments__main_Bar_opt___(a?.__externalRCRef() ?? 0)
}
public func foo_any(
    a: KotlinRuntime.KotlinBase
) -> Swift.Void {
    return __root___foo_any__TypesOfArguments__KotlinRuntime_KotlinBase__(a.__externalRCRef())
}
public func foo_any(
    a: KotlinRuntime.KotlinBase?
) -> Swift.Void {
    return __root___foo_any__TypesOfArguments__KotlinRuntime_KotlinBase_opt___(a?.__externalRCRef() ?? 0)
}
public func opt_to_non_opt_usage(
    i: main.OptToNonOptTypealias
) -> Swift.Void {
    return __root___opt_to_non_opt_usage__TypesOfArguments__main_Bar_opt___(i?.__externalRCRef() ?? 0)
}
public func p() -> main.Bar? {
    return switch __root___p() { case 0: .none; case let res: main.Bar(__externalRCRef: res); }
}
public func p_any() -> KotlinRuntime.KotlinBase? {
    return switch __root___p_any() { case 0: .none; case let res: KotlinRuntime.KotlinBase(__externalRCRef: res); }
}
public func p_opt_opt_in(
    input: main.OptOptRef
) -> Swift.Void {
    return __root___p_opt_opt_in__TypesOfArguments__main_Bar_opt___(input?.__externalRCRef() ?? 0)
}
public func p_opt_opt_out() -> main.OptOptRef {
    return switch __root___p_opt_opt_out() { case 0: .none; case let res: main.Bar(__externalRCRef: res); }
}
public func string_in(
    a: Swift.String?
) -> Swift.Void {
    return __root___string_in__TypesOfArguments__Swift_String_opt___(a)
}
public func string_out() -> Swift.String? {
    return __root___string_out()
}
