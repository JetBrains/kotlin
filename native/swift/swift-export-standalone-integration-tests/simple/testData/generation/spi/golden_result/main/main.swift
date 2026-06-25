@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
@_spi(ExperimentalLibApi) @_spi(InternalLibApi) @_spi(OpenClassOptIn) @_spi(InterfaceOptInOne) @_spi(InterfaceOptInTwo) import lib

@_spi(InternalLibApi)
public typealias MyAliasAlias = lib.InternalLibAlias
@_spi(InternalLibApi)
public typealias MyInterfaceAlias = any lib.InternalLibInterface
public protocol MyInterface: KotlinRuntime.KotlinBase, main._MyInterface {
    var foo: Swift.String {
        get
        set
    }
    @_spi(MyOptInApi)
    var optInProp: Swift.String {
        @_spi(MyOptInApi)
        get
        @_spi(MyOptInApi)
        set
    }
    func bar() -> Swift.Void
    @_spi(MyOptInApi)
    func optInFun() -> Swift.Void
}
@objc(_MyInterface)
public protocol _MyInterface {
}
public protocol __MyInterface: KotlinRuntimeSupport._KotlinBridgeable {
}
@_spi(InternalLibApi)
public final class MyImplementation: KotlinRuntime.KotlinBase, lib.InternalLibInterface, lib.__InternalLibInterface {
    @_spi(ExperimentalLibApi) @_spi(InternalLibApi)
    public var experimentalProp: Swift.String {
        @_spi(ExperimentalLibApi) @_spi(InternalLibApi)
        get {
            return MyImplementation_experimentalProp_get(self.__externalRCRef())
        }
        @_spi(ExperimentalLibApi) @_spi(InternalLibApi)
        set {
            return { MyImplementation_experimentalProp_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
        }
    }
    @_spi(InternalLibApi)
    public var foo: Swift.String {
        @_spi(InternalLibApi)
        get {
            return MyImplementation_foo_get(self.__externalRCRef())
        }
        @_spi(InternalLibApi)
        set {
            return { MyImplementation_foo_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
        }
    }
    @_spi(InternalLibApi)
    public var internalProp: Swift.String {
        @_spi(InternalLibApi)
        get {
            return MyImplementation_internalProp_get(self.__externalRCRef())
        }
        @_spi(InternalLibApi)
        set {
            return { MyImplementation_internalProp_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
        }
    }
    @_spi(InternalLibApi)
    public init() {
        let __kt = __root___MyImplementation_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___MyImplementation_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    @_spi(InternalLibApi)
    public func bar() -> Swift.Void {
        return { MyImplementation_bar(self.__externalRCRef()); return () }()
    }
    @_spi(ExperimentalLibApi) @_spi(InternalLibApi)
    public func experimentalFun() -> Swift.Void {
        return { MyImplementation_experimentalFun(self.__externalRCRef()); return () }()
    }
    @_spi(InternalLibApi)
    public func internalFun() -> Swift.Void {
        return { MyImplementation_internalFun(self.__externalRCRef()); return () }()
    }
}
@_spi(MyOptInApi)
public final class MyOptInClass: KotlinRuntime.KotlinBase {
    @_spi(MyOptInApi)
    public init() {
        let __kt = __root___MyOptInClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___MyOptInClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@_spi(InterfaceOptInOne) @_spi(OpenClassOptIn)
public final class MySubClass: lib.OpenClass, lib.InterfaceOne, lib.__InterfaceOne {
    @_spi(InterfaceOptInOne) @_spi(OpenClassOptIn)
    public override init() {
        let __kt = __root___MySubClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___MySubClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@_spi(InterfaceOptInTwo)
public final class MySubInterface: KotlinRuntime.KotlinBase, lib.InterfaceTwo, lib.__InterfaceTwo {
    @_spi(InterfaceOptInTwo)
    public init() {
        let __kt = __root___MySubInterface_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___MySubInterface_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@_spi(MyOptInApi)
public var functionalTypePropertyA: (main.MyOptInClass) -> Swift.Void {
    @_spi(MyOptInApi)
    get {
        return {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___functionalTypePropertyA_get(), options: .asBestFittingWrapper)!
            return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_MyOptInClass__(pointerToBlock.__externalRCRef()!, _1.__externalRCRef()); return () }() }
        }()
    }
    @_spi(MyOptInApi)
    set {
        return { __root___functionalTypePropertyA_set__TypesOfArguments__U28main_MyOptInClassU29202D_U20Swift_Void__({
            let originalBlock: (main.MyOptInClass) -> Swift.Void = newValue
            return { (arg0: Swift.UnsafeMutableRawPointer) in
                let _arg0: main.MyOptInClass = main.MyOptInClass.__createClassWrapper(externalRCRef: arg0)
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }()); return () }()
    }
}
@_spi(InternalLibApi)
public var functionalTypePropertyB: (any lib.InternalLibInterface) -> Swift.Void {
    @_spi(InternalLibApi)
    get {
        return {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___functionalTypePropertyB_get(), options: .asBestFittingWrapper)!
            return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20lib_InternalLibInterface__(pointerToBlock.__externalRCRef()!, _1.__externalRCRef()); return () }() }
        }()
    }
    @_spi(InternalLibApi)
    set {
        return { __root___functionalTypePropertyB_set__TypesOfArguments__U28anyU20lib_InternalLibInterfaceU29202D_U20Swift_Void__({
            let originalBlock: (any lib.InternalLibInterface) -> Swift.Void = newValue
            return { (arg0: Swift.UnsafeMutableRawPointer) in
                let _arg0: any lib.InternalLibInterface = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any lib.InternalLibInterface
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }()); return () }()
    }
}
@_spi(MyOptInApi)
public func callbackFunction(
    action: @escaping () -> main.MyOptInClass
) -> Swift.Void {
    return { __root___callbackFunction__TypesOfArguments__U2829202D_U20main_MyOptInClass__({
        let originalBlock: () -> main.MyOptInClass = action
        return {
            let _result = originalBlock()
            return _result.__externalRCRef()
        }
    }()); return () }()
}
@_spi(MyOptInApi)
public func optInFunctionA() -> Swift.Void {
    return { __root___optInFunctionA(); return () }()
}
@_spi(ExperimentalLibApi)
public func optInFunctionB() -> Swift.Void {
    return { __root___optInFunctionB(); return () }()
}
@_spi(ExperimentalLibApi)
public func optInFunctionC() -> lib.ExperimentalLibClass {
    return lib.ExperimentalLibClass.__createClassWrapper(externalRCRef: __root___optInFunctionC())
}
@_spi(InternalLibApi)
public func optInFunctionD() -> main.MyInterfaceAlias {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___optInFunctionD()) as! any lib.InternalLibInterface
}
@_spi(InternalLibApi)
public func optInFunctionE() -> main.MyAliasAlias {
    return __root___optInFunctionE()
}
@_spi(MyOptInApi)
public func optInFunctionF() -> Swift.Void {
    return { __root___optInFunctionF(); return () }()
}
public func regularFunctionA() -> Swift.Void {
    return { __root___regularFunctionA(); return () }()
}
public func regularFunctionB() -> Swift.Void {
    return { __root___regularFunctionB(); return () }()
}
public func regularFunctionC() -> lib.RegularLibClass {
    return lib.RegularLibClass.__createClassWrapper(externalRCRef: __root___regularFunctionC())
}
extension main.MyInterface where Self : main.__MyInterface {
    public var foo: Swift.String {
        get {
            return MyInterface_foo_get(self.__externalRCRef())
        }
        set {
            return { MyInterface_foo_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
        }
    }
    @_spi(MyOptInApi)
    public var optInProp: Swift.String {
        @_spi(MyOptInApi)
        get {
            return MyInterface_optInProp_get(self.__externalRCRef())
        }
        @_spi(MyOptInApi)
        set {
            return { MyInterface_optInProp_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
        }
    }
    public func bar() -> Swift.Void {
        return { MyInterface_bar(self.__externalRCRef()); return () }()
    }
    @_spi(MyOptInApi)
    public func optInFun() -> Swift.Void {
        return { MyInterface_optInFun(self.__externalRCRef()); return () }()
    }
}
extension main.MyInterface {
    @_spi(MyOptInApi)
    public var optInProp: Swift.String {
        @_spi(MyOptInApi)
        get {
            fatalError("'optInProp' is an @_spi requirement that must be implemented by Swift conformers")
        }
        @_spi(MyOptInApi)
        set {
            fatalError("'optInProp' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(MyOptInApi)
    public func optInFun() -> Swift.Void {
        fatalError("'optInFun' is an @_spi requirement that must be implemented by Swift conformers")
    }
}
extension KotlinRuntimeSupport._KotlinExistential: main.MyInterface, main.__MyInterface where Wrapped : main._MyInterface {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._MyInterface {
}
@_cdecl("MyInterface_bar__reverse_swift")
package func MyInterface_bar__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.MyInterface
    let _result: Swift.Void = _self.bar()
    return { _result; return true }()
}

@_cdecl("MyInterface_optInFun__reverse_swift")
package func MyInterface_optInFun__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.MyInterface
    let _result: Swift.Void = _self.optInFun()
    return { _result; return true }()
}
