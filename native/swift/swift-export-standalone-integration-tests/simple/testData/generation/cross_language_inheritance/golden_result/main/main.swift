@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public protocol Boxed: KotlinRuntime.KotlinBase, main._Boxed {
    var boxLabel: Swift.String {
        get
    }
    func label() -> Swift.String
    func unbox() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
}
public protocol Defaulter: KotlinRuntime.KotlinBase, main._Defaulter {
    var kind: Swift.String {
        get
    }
    func describe() -> Swift.String
    func tag() -> Swift.String
}
public protocol Greeter: KotlinRuntime.KotlinBase, main._Greeter {
    var mood: Swift.String {
        get
        set
    }
    func greet(
        name: Swift.String
    ) -> Swift.String
    func salutation() -> Swift.String
}
@objc(_Boxed)
public protocol _Boxed {
}
@objc(_Defaulter)
public protocol _Defaulter {
}
@objc(_Greeter)
public protocol _Greeter {
}
public protocol __Boxed: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __Defaulter: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __Greeter: KotlinRuntimeSupport._KotlinBridgeable {
}
open class AbstractBase: KotlinRuntime.KotlinBase {
    package init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func abstractMethod() -> Swift.String {
        if Self.self == main.AbstractBase.self {
            return AbstractBase_abstractMethod(self.__externalRCRef())
        } else {
            fatalError("Cannot invoke the inherited implementation of abstract member 'main.AbstractBase.abstractMethod': a Swift subclass must override it and must not call super.")
        }
    }
    open func concreteMethod() -> Swift.Int32 {
        if Self.self == main.AbstractBase.self {
            return AbstractBase_concreteMethod(self.__externalRCRef())
        } else {
            return AbstractBase_concreteMethod_direct(self.__externalRCRef())
        }
    }
}
open class Base: KotlinRuntime.KotlinBase {
    open var name: Swift.String {
        get {
            if Self.self == main.Base.self {
                return Base_name_get(self.__externalRCRef())
            } else {
                return Base_name_get_direct(self.__externalRCRef())
            }
        }
        set {
            if Self.self == main.Base.self {
                return { Base_name_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
            } else {
                return { Base_name_set__TypesOfArguments__Swift_String___direct(self.__externalRCRef(), newValue); return () }()
            }
        }
    }
    public final var notOpenValue: Swift.String {
        get {
            return Base_notOpenValue_get(self.__externalRCRef())
        }
    }
    open var size: Swift.Int32 {
        get {
            if Self.self == main.Base.self {
                return Base_size_get(self.__externalRCRef())
            } else {
                return Base_size_get_direct(self.__externalRCRef())
            }
        }
    }
    public init() {
        let __kt = __root___Base_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Base_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func count() -> Swift.Int32 {
        if Self.self == main.Base.self {
            return Base_count(self.__externalRCRef())
        } else {
            return Base_count_direct(self.__externalRCRef())
        }
    }
    open func greet(
        name: Swift.String
    ) -> Swift.String {
        if Self.self == main.Base.self {
            return Base_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
        } else {
            return Base_greet__TypesOfArguments__Swift_String___direct(self.__externalRCRef(), name)
        }
    }
    public final func notOpen() -> Swift.String {
        return Base_notOpen(self.__externalRCRef())
    }
}
open class GreeterBase: KotlinRuntime.KotlinBase, main.Greeter, main.__Greeter {
    open var mood: Swift.String {
        get {
            if Self.self == main.GreeterBase.self {
                return GreeterBase_mood_get(self.__externalRCRef())
            } else {
                return GreeterBase_mood_get_direct(self.__externalRCRef())
            }
        }
        set {
            if Self.self == main.GreeterBase.self {
                return { GreeterBase_mood_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
            } else {
                return { GreeterBase_mood_set__TypesOfArguments__Swift_String___direct(self.__externalRCRef(), newValue); return () }()
            }
        }
    }
    public init() {
        let __kt = __root___GreeterBase_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___GreeterBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func greet(
        name: Swift.String
    ) -> Swift.String {
        if Self.self == main.GreeterBase.self {
            return GreeterBase_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
        } else {
            return GreeterBase_greet__TypesOfArguments__Swift_String___direct(self.__externalRCRef(), name)
        }
    }
    open func salutation() -> Swift.String {
        if Self.self == main.GreeterBase.self {
            return GreeterBase_salutation(self.__externalRCRef())
        } else {
            return GreeterBase_salutation_direct(self.__externalRCRef())
        }
    }
}
extension main.Boxed where Self : main.__Boxed {
    public var boxLabel: Swift.String {
        get {
            return Boxed_boxLabel_get(self.__externalRCRef())
        }
    }
    public func label() -> Swift.String {
        return Boxed_label(self.__externalRCRef())
    }
    public func unbox() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch Boxed_unbox(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
extension main.Boxed {
    public var boxLabel: Swift.String {
        get {
            return Boxed_boxLabel_get_direct(self.__externalRCRef())
        }
    }
    public func label() -> Swift.String {
        return Boxed_label_direct(self.__externalRCRef())
    }
}
extension main.Defaulter where Self : main.__Defaulter {
    public var kind: Swift.String {
        get {
            return Defaulter_kind_get(self.__externalRCRef())
        }
    }
    public func describe() -> Swift.String {
        return Defaulter_describe(self.__externalRCRef())
    }
    public func tag() -> Swift.String {
        return Defaulter_tag(self.__externalRCRef())
    }
}
extension main.Defaulter {
    public var kind: Swift.String {
        get {
            return Defaulter_kind_get_direct(self.__externalRCRef())
        }
    }
    public func describe() -> Swift.String {
        return Defaulter_describe_direct(self.__externalRCRef())
    }
}
extension main.Greeter where Self : main.__Greeter {
    public var mood: Swift.String {
        get {
            return Greeter_mood_get(self.__externalRCRef())
        }
        set {
            return { Greeter_mood_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
        }
    }
    public func greet(
        name: Swift.String
    ) -> Swift.String {
        return Greeter_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
    }
    public func salutation() -> Swift.String {
        return Greeter_salutation(self.__externalRCRef())
    }
}
extension main.Greeter {
}
extension KotlinRuntimeSupport._KotlinExistential: main.Greeter, main.__Greeter where Wrapped : main._Greeter {
}
extension KotlinRuntimeSupport._KotlinExistential: main.Defaulter, main.__Defaulter where Wrapped : main._Defaulter {
}
extension KotlinRuntimeSupport._KotlinExistential: main.Boxed, main.__Boxed where Wrapped : main._Boxed {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Greeter {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Defaulter {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Boxed {
}
@_cdecl("AbstractBase_abstractMethod__reverse_swift")
package func AbstractBase_abstractMethod__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = main.AbstractBase.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.abstractMethod()
    return _result
}

@_cdecl("AbstractBase_concreteMethod__reverse_swift")
package func AbstractBase_concreteMethod__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = main.AbstractBase.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.concreteMethod()
    return _result
}

@_cdecl("Base_count__reverse_swift")
package func Base_count__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = main.Base.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.count()
    return _result
}

@_cdecl("Base_greet__TypesOfArguments__Swift_String____reverse_swift")
package func Base_greet__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String) -> Swift.String {
    let _self = main.Base.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.greet(name: name)
    return _result
}

@_cdecl("Base_name_get__reverse_swift")
package func Base_name_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = main.Base.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.name
    return _result
}

@_cdecl("Base_name_set__TypesOfArguments__Swift_String____reverse_swift")
package func Base_name_set__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ newValue: Swift.String) -> Swift.Bool {
    let _self = main.Base.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = { _self.name = newValue }()
    return { _result; return true }()
}

@_cdecl("Base_size_get__reverse_swift")
package func Base_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = main.Base.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.size
    return _result
}

@_cdecl("Boxed_boxLabel_get__reverse_swift")
package func Boxed_boxLabel_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Boxed
    let _result: Swift.String = _self.boxLabel
    return _result
}

@_cdecl("Boxed_label__reverse_swift")
package func Boxed_label__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Boxed
    let _result: Swift.String = _self.label()
    return _result
}

@_cdecl("Boxed_unbox__reverse_swift")
package func Boxed_unbox__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Boxed
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.unbox()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("Defaulter_describe__reverse_swift")
package func Defaulter_describe__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Defaulter
    let _result: Swift.String = _self.describe()
    return _result
}

@_cdecl("Defaulter_kind_get__reverse_swift")
package func Defaulter_kind_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Defaulter
    let _result: Swift.String = _self.kind
    return _result
}

@_cdecl("Defaulter_tag__reverse_swift")
package func Defaulter_tag__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Defaulter
    let _result: Swift.String = _self.tag()
    return _result
}

@_cdecl("GreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift")
package func GreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String) -> Swift.String {
    let _self = main.GreeterBase.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.greet(name: name)
    return _result
}

@_cdecl("GreeterBase_mood_get__reverse_swift")
package func GreeterBase_mood_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = main.GreeterBase.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.mood
    return _result
}

@_cdecl("GreeterBase_mood_set__TypesOfArguments__Swift_String____reverse_swift")
package func GreeterBase_mood_set__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ newValue: Swift.String) -> Swift.Bool {
    let _self = main.GreeterBase.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = { _self.mood = newValue }()
    return { _result; return true }()
}

@_cdecl("GreeterBase_salutation__reverse_swift")
package func GreeterBase_salutation__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = main.GreeterBase.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.salutation()
    return _result
}

@_cdecl("Greeter_greet__TypesOfArguments__Swift_String____reverse_swift")
package func Greeter_greet__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Greeter
    let _result: Swift.String = _self.greet(name: name)
    return _result
}

@_cdecl("Greeter_mood_get__reverse_swift")
package func Greeter_mood_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Greeter
    let _result: Swift.String = _self.mood
    return _result
}

@_cdecl("Greeter_mood_set__TypesOfArguments__Swift_String____reverse_swift")
package func Greeter_mood_set__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ newValue: Swift.String) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Greeter
    let _result: Swift.Void = { _self.mood = newValue }()
    return { _result; return true }()
}

@_cdecl("Greeter_salutation__reverse_swift")
package func Greeter_salutation__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Greeter
    let _result: Swift.String = _self.salutation()
    return _result
}
