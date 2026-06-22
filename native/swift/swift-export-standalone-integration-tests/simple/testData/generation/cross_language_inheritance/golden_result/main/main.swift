@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public protocol Greeter: KotlinRuntime.KotlinBase, main._Greeter {
    func greet(
        name: Swift.String
    ) -> Swift.String
    func salutation() -> Swift.String
}
@objc(_Greeter)
public protocol _Greeter {
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
open class GreeterBase: KotlinRuntime.KotlinBase, main.Greeter, main._Greeter, main.__Greeter {
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
extension main.Greeter where Self : main.__Greeter {
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
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Greeter {
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

@_cdecl("GreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift")
package func GreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String) -> Swift.String {
    let _self = main.GreeterBase.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.greet(name: name)
    return _result
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

@_cdecl("Greeter_salutation__reverse_swift")
package func Greeter_salutation__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Greeter
    let _result: Swift.String = _self.salutation()
    return _result
}
