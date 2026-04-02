@_implementationOnly import KotlinBridges_lib
import KotlinRuntime
import KotlinRuntimeSupport

@_spi(InternalLibApi)
public typealias InternalLibAlias = Swift.String
@_spi(InternalLibApi)
public protocol InternalLibInterface: KotlinRuntime.KotlinBase {
    @_spi(InternalLibApi)
    var foo: Swift.String {
        @_spi(InternalLibApi)
        get
        @_spi(InternalLibApi)
        set
    }
    @_spi(InternalLibApi)
    func bar() -> Swift.Void
}
@objc(_InternalLibInterface)
package protocol _InternalLibInterface {
}
@_spi(ExperimentalLibApi)
public final class ExperimentalLibClass: KotlinRuntime.KotlinBase {
    @_spi(ExperimentalLibApi)
    public var foo: Swift.String {
        @_spi(ExperimentalLibApi)
        get {
            return ExperimentalLibClass_foo_get(self.__externalRCRef())
        }
        @_spi(ExperimentalLibApi)
        set {
            return { ExperimentalLibClass_foo_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
        }
    }
    @_spi(ExperimentalLibApi)
    public init() {
        if Self.self != lib.ExperimentalLibClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from lib.ExperimentalLibClass ") }
        let __kt = __root___ExperimentalLibClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ExperimentalLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    @_spi(ExperimentalLibApi)
    public func bar() -> Swift.Void {
        return { ExperimentalLibClass_bar(self.__externalRCRef()); return () }()
    }
}
public final class RegularLibClass: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != lib.RegularLibClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from lib.RegularLibClass ") }
        let __kt = __root___RegularLibClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    @_spi(InternalLibApi)
    public init(
        a: Swift.String
    ) {
        if Self.self != lib.RegularLibClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from lib.RegularLibClass ") }
        let __kt = __root___RegularLibClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, a); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public var experimentalLibSetter: Swift.String {
    get {
        return __root___experimentalLibSetter_get()
    }
    @_spi(ExperimentalLibApi)
    set {
        return { __root___experimentalLibSetter_set__TypesOfArguments__Swift_String__(newValue); return () }()
    }
}
@_spi(ExperimentalLibApi)
public var experimentalProperty: Swift.String {
    @_spi(ExperimentalLibApi)
    get {
        return __root___experimentalProperty_get()
    }
    @_spi(ExperimentalLibApi)
    set {
        return { __root___experimentalProperty_set__TypesOfArguments__Swift_String__(newValue); return () }()
    }
}
@_spi(InternalLibApi)
public var fooA: any lib.InternalLibInterface {
    @_spi(InternalLibApi)
    get {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___fooA_get()) as! any lib.InternalLibInterface
    }
}
@_spi(ExperimentalLibApi)
public var fooB: lib.ExperimentalLibClass {
    @_spi(ExperimentalLibApi)
    get {
        return lib.ExperimentalLibClass.__createClassWrapper(externalRCRef: __root___fooB_get())
    }
}
public var internalLibSetter: Swift.String {
    get {
        return __root___internalLibSetter_get()
    }
    @_spi(InternalLibApi)
    set {
        return { __root___internalLibSetter_set__TypesOfArguments__Swift_String__(newValue); return () }()
    }
}
@_spi(InternalLibApi)
public var internalProperty: Swift.String {
    @_spi(InternalLibApi)
    get {
        return __root___internalProperty_get()
    }
    @_spi(InternalLibApi)
    set {
        return { __root___internalProperty_set__TypesOfArguments__Swift_String__(newValue); return () }()
    }
}
@_spi(ExperimentalLibApi)
public func experimentalLibFunction() -> Swift.Void {
    return { __root___experimentalLibFunction(); return () }()
}
@_spi(ExperimentalLibApi) @_spi(InternalLibApi)
public func fooA(
    _ context: any lib.InternalLibInterface,
    b: lib.ExperimentalLibClass
) -> Swift.Void {
    let (a) = context
    return { __root___fooA__TypesOfArgumentsC1__lib_ExperimentalLibClass_anyU20lib_InternalLibInterface__(b.__externalRCRef(), a.__externalRCRef()); return () }()
}
@_spi(ExperimentalLibApi) @_spi(InternalLibApi)
public func fooB(
    _ receiver: any lib.InternalLibInterface
) -> lib.ExperimentalLibClass {
    return lib.ExperimentalLibClass.__createClassWrapper(externalRCRef: __root___fooB__TypesOfArgumentsE__anyU20lib_InternalLibInterface__(receiver.__externalRCRef()))
}
@_spi(InternalLibApi)
public func fooC() -> any lib.InternalLibInterface {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___fooC()) as! any lib.InternalLibInterface
}
@_spi(ExperimentalLibApi)
public func fooD() -> lib.ExperimentalLibClass {
    return lib.ExperimentalLibClass.__createClassWrapper(externalRCRef: __root___fooD())
}
@_spi(InternalLibApi)
public func genericFunction(
    a: any lib.InternalLibInterface
) -> Swift.String {
    return __root___genericFunction__TypesOfArguments__anyU20lib_InternalLibInterface__(a.__externalRCRef())
}
@_spi(InternalLibApi)
public func getGenericProperty(
    _ receiver: any lib.InternalLibInterface
) -> Swift.String {
    return __root___genericProperty_get__TypesOfArgumentsE__anyU20lib_InternalLibInterface__(receiver.__externalRCRef())
}
@_spi(InternalLibApi)
public func internalLibFunction() -> Swift.Void {
    return { __root___internalLibFunction(); return () }()
}
public func normalLibFunction() -> Swift.Void {
    return { __root___normalLibFunction(); return () }()
}
@_spi(InternalLibApi)
public func returnAlias() -> lib.InternalLibAlias {
    return __root___returnAlias()
}
extension lib.InternalLibInterface where Self : KotlinRuntimeSupport._KotlinBridgeable {
    @_spi(InternalLibApi)
    public var foo: Swift.String {
        @_spi(InternalLibApi)
        get {
            return InternalLibInterface_foo_get(self.__externalRCRef())
        }
        @_spi(InternalLibApi)
        set {
            return { InternalLibInterface_foo_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
        }
    }
    @_spi(InternalLibApi)
    public func bar() -> Swift.Void {
        return { InternalLibInterface_bar(self.__externalRCRef()); return () }()
    }
}
extension lib.InternalLibInterface {
}
@_spi(InternalLibApi)
extension KotlinRuntimeSupport._KotlinExistential: lib.InternalLibInterface where Wrapped : lib._InternalLibInterface {
}
