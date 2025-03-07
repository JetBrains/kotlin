@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public override init() {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public func ext(
        _ receiver: Swift.String
    ) -> Swift.Void {
        return Foo_ext__TypesOfArguments__Swift_String__(self.__externalRCRef(), receiver)
    }
    public func getExtVal(
        _ receiver: Swift.String
    ) -> Swift.String {
        return Foo_extVal_get__TypesOfArguments__Swift_String__(self.__externalRCRef(), receiver)
    }
    public func getExtVar(
        _ receiver: Swift.String
    ) -> Swift.String {
        return Foo_extVar_get__TypesOfArguments__Swift_String__(self.__externalRCRef(), receiver)
    }
    public func setExtVar(
        _ receiver: Swift.String,
        v: Swift.String
    ) -> Swift.Void {
        return Foo_extVar_set__TypesOfArguments__Swift_String_Swift_String__(self.__externalRCRef(), receiver, v)
    }
}
public func foo(
    _ receiver: Swift.Int32
) -> Swift.Void {
    return __root___foo__TypesOfArguments__Swift_Int32__(receiver)
}
public func foo(
    _ receiver: Swift.Int32?
) -> Swift.Void {
    return __root___foo__TypesOfArguments__Swift_Optional_Swift_Int32___(receiver.map { it in NSNumber(value: it) } ?? nil)
}
public func foo(
    _ receiver: main.Foo
) -> Swift.Void {
    return __root___foo__TypesOfArguments__main_Foo__(receiver.__externalRCRef())
}
public func foo(
    _ receiver: main.Foo?
) -> Swift.Void {
    return __root___foo__TypesOfArguments__Swift_Optional_main_Foo___(receiver.map { it in it.__externalRCRef() } ?? 0)
}
public func foo() -> Swift.Int32 {
    return __root___foo()
}
public func getBar(
    _ receiver: Swift.Int32
) -> Swift.String {
    return __root___bar_get__TypesOfArguments__Swift_Int32__(receiver)
}
public func getBar(
    _ receiver: Swift.Int32?
) -> Swift.String {
    return __root___bar_get__TypesOfArguments__Swift_Optional_Swift_Int32___(receiver.map { it in NSNumber(value: it) } ?? nil)
}
public func getBar(
    _ receiver: main.Foo
) -> Swift.String {
    return __root___bar_get__TypesOfArguments__main_Foo__(receiver.__externalRCRef())
}
public func getBar(
    _ receiver: main.Foo?
) -> Swift.String {
    return __root___bar_get__TypesOfArguments__Swift_Optional_main_Foo___(receiver.map { it in it.__externalRCRef() } ?? 0)
}
public func getFoo(
    _ receiver: Swift.Int32
) -> Swift.String {
    return __root___foo_get__TypesOfArguments__Swift_Int32__(receiver)
}
public func getFoo(
    _ receiver: Swift.Int32?
) -> Swift.String {
    return __root___foo_get__TypesOfArguments__Swift_Optional_Swift_Int32___(receiver.map { it in NSNumber(value: it) } ?? nil)
}
public func getFoo(
    _ receiver: main.Foo
) -> Swift.String {
    return __root___foo_get__TypesOfArguments__main_Foo__(receiver.__externalRCRef())
}
public func getFoo(
    _ receiver: main.Foo?
) -> Swift.String {
    return __root___foo_get__TypesOfArguments__Swift_Optional_main_Foo___(receiver.map { it in it.__externalRCRef() } ?? 0)
}
public func return_any_should_append_runtime_import() -> KotlinRuntime.KotlinBase {
    return KotlinRuntime.KotlinBase(__externalRCRef: __root___return_any_should_append_runtime_import())
}
public func setFoo(
    _ receiver: Swift.Int32,
    v: Swift.String
) -> Swift.Void {
    return __root___foo_set__TypesOfArguments__Swift_Int32_Swift_String__(receiver, v)
}
public func setFoo(
    _ receiver: Swift.Int32?,
    v: Swift.String
) -> Swift.Void {
    return __root___foo_set__TypesOfArguments__Swift_Optional_Swift_Int32__Swift_String__(receiver.map { it in NSNumber(value: it) } ?? nil, v)
}
public func setFoo(
    _ receiver: main.Foo,
    v: Swift.String
) -> Swift.Void {
    return __root___foo_set__TypesOfArguments__main_Foo_Swift_String__(receiver.__externalRCRef(), v)
}
public func setFoo(
    _ receiver: main.Foo?,
    v: Swift.String
) -> Swift.Void {
    return __root___foo_set__TypesOfArguments__Swift_Optional_main_Foo__Swift_String__(receiver.map { it in it.__externalRCRef() } ?? 0, v)
}
public extension ExportedKotlinPackages.namespace1.local_functions {
    public static func foo() -> Swift.Void {
        return namespace1_local_functions_foo()
    }
}
public extension ExportedKotlinPackages.namespace1.main {
    public static func all_args(
        arg1: Swift.Bool,
        arg2: Swift.Int8,
        arg3: Swift.Int16,
        arg4: Swift.Int32,
        arg5: Swift.Int64,
        arg6: Swift.UInt8,
        arg7: Swift.UInt16,
        arg8: Swift.UInt32,
        arg9: Swift.UInt64,
        arg10: Swift.Float,
        arg11: Swift.Double,
        arg12: Swift.Unicode.UTF16.CodeUnit
    ) -> Swift.Void {
        return namespace1_main_all_args__TypesOfArguments__Swift_Bool_Swift_Int8_Swift_Int16_Swift_Int32_Swift_Int64_Swift_UInt8_Swift_UInt16_Swift_UInt32_Swift_UInt64_Swift_Float_Swift_Double_Swift_Unicode_UTF16_CodeUnit__(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12)
    }
    public static func foobar(
        param: Swift.Int32
    ) -> Swift.Int32 {
        return namespace1_main_foobar__TypesOfArguments__Swift_Int32__(param)
    }
}
public extension ExportedKotlinPackages.namespace1 {
    public static func bar() -> Swift.Int32 {
        return namespace1_bar()
    }
}
public extension ExportedKotlinPackages.namespace2 {
    public static func foo(
        arg1: Swift.Int32
    ) -> Swift.Int32 {
        return namespace2_foo__TypesOfArguments__Swift_Int32__(arg1)
    }
}
public extension ExportedKotlinPackages.overload {
    public final class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = overload_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            overload_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static func foo(
        arg1: Swift.Double
    ) -> Swift.Int32 {
        return overload_foo__TypesOfArguments__Swift_Double__(arg1)
    }
    public static func foo(
        arg1: ExportedKotlinPackages.overload.Foo
    ) -> Swift.Void {
        return overload_foo__TypesOfArguments__ExportedKotlinPackages_overload_Foo__(arg1.__externalRCRef())
    }
    public static func foo(
        arg1: Swift.Int32
    ) -> Swift.Int32 {
        return overload_foo__TypesOfArguments__Swift_Int32__(arg1)
    }
    public static func foo(
        arg1: ExportedKotlinPackages.overload.Foo?
    ) -> Swift.Void {
        return overload_foo__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_overload_Foo___(arg1.map { it in it.__externalRCRef() } ?? 0)
    }
}
