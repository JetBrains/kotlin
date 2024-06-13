@_exported import ExportedKotlinPackages
import KotlinBridges_main
import KotlinRuntime

public class Foo : KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__uintptr_t__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
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
public var CHAR_CONST: KotlinRuntime.KotlinBase {
    get {
        fatalError()
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
public var STRING_CONST: KotlinRuntime.KotlinBase {
    get {
        fatalError()
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
public var foo: main.Foo {
    get {
        return main.Foo(__externalRCRef: __root___foo_get())
    }
    set {
        return __root___foo_set__TypesOfArguments__uintptr_t__(newValue.__externalRCRef())
    }
}
public extension ExportedKotlinPackages.namespace.main {
    public static var bar: Swift.Int32 {
        get {
            return namespace_main_bar_get()
        }
        set {
            return namespace_main_bar_set__TypesOfArguments__int32_t__(newValue)
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
        return namespace_main_foobar__TypesOfArguments__int32_t__(param)
    }
}
