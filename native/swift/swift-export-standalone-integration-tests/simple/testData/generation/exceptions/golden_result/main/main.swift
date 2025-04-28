@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Object: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public init(
        arg: Swift.Bool
    ) throws {
        precondition(Self.self == main.Object.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Object ")
        let __kt = __root___Object_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        var __error: UnsafeMutableRawPointer? = nil
        __root___Object_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(__kt, arg, &__error)
        guard __error == .none else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: __error)) }
    }
    public init(
        arg: Swift.Unicode.UTF16.CodeUnit
    ) throws {
        precondition(Self.self == main.Object.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Object ")
        let __kt = __root___Object_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        var __error: UnsafeMutableRawPointer? = nil
        __root___Object_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Unicode_UTF16_CodeUnit__(__kt, arg, &__error)
        guard __error == .none else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: __error)) }
    }
    public init(
        arg: Swift.Double
    ) throws {
        precondition(Self.self == main.Object.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Object ")
        let __kt = __root___Object_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        var __error: UnsafeMutableRawPointer? = nil
        __root___Object_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Double__(__kt, arg, &__error)
        guard __error == .none else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: __error)) }
    }
    public init(
        arg: Swift.Int32
    ) throws {
        precondition(Self.self == main.Object.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Object ")
        let __kt = __root___Object_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        var __error: UnsafeMutableRawPointer? = nil
        __root___Object_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, arg, &__error)
        guard __error == .none else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: __error)) }
    }
    public init(
        arg: KotlinRuntime.KotlinBase
    ) throws {
        precondition(Self.self == main.Object.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Object ")
        let __kt = __root___Object_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        var __error: UnsafeMutableRawPointer? = nil
        __root___Object_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_KotlinRuntime_KotlinBase__(__kt, arg.__externalRCRef(), &__error)
        guard __error == .none else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: __error)) }
    }
    public init(
        arg: main.Object
    ) throws {
        precondition(Self.self == main.Object.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Object ")
        let __kt = __root___Object_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        var __error: UnsafeMutableRawPointer? = nil
        __root___Object_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Object__(__kt, arg.__externalRCRef(), &__error)
        guard __error == .none else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: __error)) }
    }
    public init(
        arg: KotlinRuntime.KotlinBase?
    ) throws {
        precondition(Self.self == main.Object.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Object ")
        let __kt = __root___Object_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        var __error: UnsafeMutableRawPointer? = nil
        __root___Object_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_KotlinRuntime_KotlinBase___(__kt, arg.map { it in it.__externalRCRef() } ?? nil, &__error)
        guard __error == .none else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: __error)) }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public func throwing_fun_any() throws -> KotlinRuntime.KotlinBase {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_any(&_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return KotlinRuntime.KotlinBase.__create(externalRCRef: _result)
}
public func throwing_fun_any(
    arg: KotlinRuntime.KotlinBase
) throws -> KotlinRuntime.KotlinBase {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_any__TypesOfArguments__KotlinRuntime_KotlinBase__(arg.__externalRCRef(), &_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return KotlinRuntime.KotlinBase.__create(externalRCRef: _result)
}
public func throwing_fun_boolean() throws -> Swift.Bool {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_boolean(&_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return _result
}
public func throwing_fun_boolean(
    arg: Swift.Bool
) throws -> Swift.Bool {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_boolean__TypesOfArguments__Swift_Bool__(arg, &_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return _result
}
public func throwing_fun_char() throws -> Swift.Unicode.UTF16.CodeUnit {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_char(&_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return _result
}
public func throwing_fun_char(
    arg: Swift.Unicode.UTF16.CodeUnit
) throws -> Swift.Unicode.UTF16.CodeUnit {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_char__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(arg, &_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return _result
}
public func throwing_fun_double() throws -> Swift.Double {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_double(&_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return _result
}
public func throwing_fun_double(
    arg: Swift.Double
) throws -> Swift.Double {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_double__TypesOfArguments__Swift_Double__(arg, &_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return _result
}
public func throwing_fun_int() throws -> Swift.Int32 {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_int(&_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return _result
}
public func throwing_fun_int(
    arg: Swift.Int32
) throws -> Swift.Int32 {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_int__TypesOfArguments__Swift_Int32__(arg, &_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return _result
}
public func throwing_fun_never() throws -> Swift.Never {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_never(&_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return _result
}
public func throwing_fun_nullable() throws -> KotlinRuntime.KotlinBase? {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_nullable(&_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return { switch _result { case nil: .none; case let res: KotlinRuntime.KotlinBase.__create(externalRCRef: res); } }()
}
public func throwing_fun_nullable(
    arg: KotlinRuntime.KotlinBase?
) throws -> KotlinRuntime.KotlinBase? {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_nullable__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(arg.map { it in it.__externalRCRef() } ?? nil, &_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return { switch _result { case nil: .none; case let res: KotlinRuntime.KotlinBase.__create(externalRCRef: res); } }()
}
public func throwing_fun_object() throws -> main.Object {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_object(&_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return main.Object.__create(externalRCRef: _result)
}
public func throwing_fun_object(
    arg: KotlinRuntime.KotlinBase
) throws -> main.Object {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_object__TypesOfArguments__KotlinRuntime_KotlinBase__(arg.__externalRCRef(), &_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return main.Object.__create(externalRCRef: _result)
}
public func throwing_fun_void() throws -> Swift.Void {
    var _out_error: UnsafeMutableRawPointer? = nil
    let _result = __root___throwing_fun_void(&_out_error)
    guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
    return _result
}
