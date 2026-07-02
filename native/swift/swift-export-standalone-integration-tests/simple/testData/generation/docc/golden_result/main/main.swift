@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

/// An interface named `Bar`
public protocol Bar: KotlinRuntime.KotlinBase, main._Bar {
    /// A property inside an interface
    var bar: Swift.String {
        get
        set
    }
    /// A function inside an interface
    func foo() -> Swift.Void
}
@objc(_Bar)
public protocol _Bar {
}
public protocol __Bar: KotlinRuntimeSupport._KotlinBridgeable {
}
/// An object named `Baz`
public final class Baz: KotlinRuntime.KotlinBase {
    public static var shared: main.Baz {
        get {
            return main.Baz.__createClassWrapper(externalRCRef: __root___Baz_get())
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
    /// This function does some internal things and should only be called by the library.
    @_documentation(visibility: internal)
    public func someInternalLibFunction() -> Swift.Void {
        return { Baz_someInternalLibFunction(self.__externalRCRef()); return () }()
    }
    /// Just a regular function
    ///
    /// - myinfo: Just some info with a custom tag
    public func someNormalFunction() -> Swift.String {
        return Baz_someNormalFunction(self.__externalRCRef())
    }
    public func someUndocumentedFunction() -> Swift.Int32 {
        return Baz_someUndocumentedFunction(self.__externalRCRef())
    }
}
/// A class named `Foo`.
///
/// Some more information about this class.
///
/// - Author: Kodee
/// - Author: Swift Export Team
public final class Foo: KotlinRuntime.KotlinBase {
    /// A string property
    public var a: Swift.String {
        get {
            return Foo_a_get(self.__externalRCRef())
        }
    }
    /// A boolean property
    public var d: Swift.Bool {
        get {
            return Foo_d_get(self.__externalRCRef())
        }
        set {
            return { Foo_d_set__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), newValue); return () }()
        }
    }
    /// A readonly property
    public var f: Swift.String {
        get {
            return Foo_f_get(self.__externalRCRef())
        }
    }
    /// An int property named `b`.
    /// Although in Swift this is named `z` (and we are testing a new line here).
    public var z: Swift.Int32 {
        get {
            return Foo_b_get(self.__externalRCRef())
        }
    }
    /// Secondary constructor without parameters
    public init() {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    /// Secondary constructor with some undocumented parameter/property
    public init(
        a: Swift.String
    ) {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, a); return () }()
    }
    /// Secondary constructor with a single parameter
    ///
    /// - Parameters:
    ///   - arg: A regular parameter that accepts any object
    public init(
        arg: any KotlinRuntimeSupport._KotlinBridgeable
    ) {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__(__kt, arg.__externalRCRef()); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    /// The default constructor
    ///
    /// - Parameters:
    ///   - a: A string property
    ///   - z: An int property named `b`.
    ///     Although in Swift this is named `z` (and we are testing a new line here).
    ///   - c: A boolean constructor parameter
    public init(
        a: Swift.String,
        z: Swift.Int32,
        c: Swift.Bool
    ) {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Int32_Swift_Bool__(__kt, a, z, c); return () }()
    }
    /// A funciton with multiple context parameters
    ///
    /// - Parameters:
    ///   - context:
    ///     - `a`: A string context parameter
    ///     - `b`: An int context parameter named `b`
    ///
    /// - Returns: A boolean
    ///
    /// - See: foo
    public func bar(
        _ context: (a: Swift.String, b: Swift.Int32)
    ) -> Swift.Bool {
        let (a, b) = context
        return Foo_bar__TypesOfArgumentsC2__Swift_String_Swift_Int32__(self.__externalRCRef(), a, b)
    }
    /// A function with a receiver parameter
    ///
    /// - Parameters:
    ///   - receiver: A string receiver parameter
    ///
    /// - Returns: An int value
    public func baz(
        _ receiver: Swift.String
    ) -> Swift.Int32 {
        return Foo_baz__TypesOfArgumentsE__Swift_String__(self.__externalRCRef(), receiver)
    }
    /// A function with a context and regular parameter
    ///
    /// - Parameters:
    ///   - context: A string context parameter
    ///   - c: A regular int parameter named `b`
    ///
    /// - Throws:
    ///   - `RuntimeException`: In case something goes wrong
    ///   - `IllegalArgumentException`: In case `b` is negative
    public func foo(
        _ context: Swift.String,
        c: Swift.Int32
    ) -> Swift.Void {
        let (a) = context
        return { Foo_foo__TypesOfArgumentsC1__Swift_Int32_Swift_String__(self.__externalRCRef(), c, a); return () }()
    }
    /// A property with a context parameter
    ///
    /// - Parameters:
    ///   - context: A context parameter accepting any object
    public func getE(
        _ context: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> Swift.Int32 {
        let (c) = context
        return Foo_e_get__TypesOfArgumentsC1__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), c.__externalRCRef())
    }
    /// A property with a receiver parameter
    ///
    /// - Parameters:
    ///   - receiver: A string receiver parameter
    ///
    /// - Throws:
    ///   - `RuntimeException`: In case something goes wrong
    public func getG(
        _ receiver: Swift.String
    ) -> Swift.Int32 {
        return Foo_g_get__TypesOfArgumentsE__Swift_String__(self.__externalRCRef(), receiver)
    }
    /// A property with a context parameter
    ///
    /// - Parameters:
    ///   - context: A context parameter accepting any object
    public func setE(
        _ context: any KotlinRuntimeSupport._KotlinBridgeable,
        `_`: Swift.Int32
    ) -> Swift.Void {
        let (c) = context
        return { Foo_e_set__TypesOfArgumentsC1__Swift_Int32_anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), `_`, c.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension main.Bar where Self : main.__Bar {
    /// A property inside an interface
    public var bar: Swift.String {
        get {
            return Bar_bar_get(self.__externalRCRef())
        }
        set {
            return { Bar_bar_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
        }
    }
    /// A function inside an interface
    public func foo() -> Swift.Void {
        return { Bar_foo(self.__externalRCRef()); return () }()
    }
}
extension main.Bar {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: main.Bar, main.__Bar where Wrapped : main._Bar {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Bar {
}
@_cdecl("Bar_bar_get__reverse_swift")
package func Bar_bar_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Bar
    let _result: Swift.String = _self.bar
    return _result
}

@_cdecl("Bar_bar_set__TypesOfArguments__Swift_String____reverse_swift")
package func Bar_bar_set__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ newValue: Swift.String) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Bar
    let _result: Swift.Void = { _self.bar = newValue }()
    return { _result; return true }()
}

@_cdecl("Bar_foo__reverse_swift")
package func Bar_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Bar
    let _result: Swift.Void = _self.foo()
    return { _result; return true }()
}
