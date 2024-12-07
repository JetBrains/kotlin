@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime

public var BOOLEAN_CONST: Swift.Bool {
    get {
        return __root___BOOLEAN_CONST_get()
    }
}
public var BYTE_CONST: Swift.Int8 {
    get {
        return __root___BYTE_CONST_get()
    }
}
public var CHAR_CONST: Swift.Unicode.UTF16.CodeUnit {
    get {
        return __root___CHAR_CONST_get()
    }
}
public var DOUBLE_CONST: Swift.Double {
    get {
        return __root___DOUBLE_CONST_get()
    }
}
public var FLOAT_CONST: Swift.Float {
    get {
        return __root___FLOAT_CONST_get()
    }
}
public var INT_CONST: Swift.Int32 {
    get {
        return __root___INT_CONST_get()
    }
}
public var LONG_CONST: Swift.Int64 {
    get {
        return __root___LONG_CONST_get()
    }
}
public var SHORT_CONST: Swift.Int16 {
    get {
        return __root___SHORT_CONST_get()
    }
}
public var STRING_CONST: Swift.String {
    get {
        return __root___STRING_CONST_get()
    }
}
public var UBYTE_CONST: Swift.UInt8 {
    get {
        return __root___UBYTE_CONST_get()
    }
}
public var UINT_CONST: Swift.UInt32 {
    get {
        return __root___UINT_CONST_get()
    }
}
public var ULONG_CONST: Swift.UInt64 {
    get {
        return __root___ULONG_CONST_get()
    }
}
public var USHORT_CONST: Swift.UInt16 {
    get {
        return __root___USHORT_CONST_get()
    }
}
public var baz: Swift.Int32 {
    get {
        return __root___baz_get()
    }
}
public var foo: KotlinRuntime.KotlinBase {
    get {
        return KotlinRuntime.KotlinBase(__externalRCRef: __root___foo_get())
    }
    set {
        return __root___foo_set__TypesOfArguments__KotlinRuntime_KotlinBase__(newValue.__externalRCRef())
    }
}
public var lateinit_foo: KotlinRuntime.KotlinBase {
    get {
        return KotlinRuntime.KotlinBase(__externalRCRef: __root___lateinit_foo_get())
    }
    set {
        return __root___lateinit_foo_set__TypesOfArguments__KotlinRuntime_KotlinBase__(newValue.__externalRCRef())
    }
}
public extension ExportedKotlinPackages.namespace.main {
    public static var bar: Swift.Int32 {
        get {
            return namespace_main_bar_get()
        }
        set {
            return namespace_main_bar_set__TypesOfArguments__Swift_Int32__(newValue)
        }
    }
    public static var foo: Swift.Int32 {
        get {
            return namespace_main_foo_get()
        }
    }
    public static func foobar(
        param: Swift.Int32
    ) -> Swift.Int32 {
        return namespace_main_foobar__TypesOfArguments__Swift_Int32__(param)
    }
}
