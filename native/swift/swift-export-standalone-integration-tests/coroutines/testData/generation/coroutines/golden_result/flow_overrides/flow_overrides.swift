@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_flow_overrides
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinxCoroutinesCore

public protocol _ExportedKotlinPackages_namespace_I1_I2: KotlinRuntime.KotlinBase, ExportedKotlinPackages.namespace.I1, flow_overrides.__ExportedKotlinPackages_namespace_I1_I2 {
}
@objc(__ExportedKotlinPackages_namespace_I1_I2)
public protocol __ExportedKotlinPackages_namespace_I1_I2: ExportedKotlinPackages.namespace._I1 {
}
extension ExportedKotlinPackages.namespace.I1 where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.namespace.I1 {
    typealias I2 = flow_overrides._ExportedKotlinPackages_namespace_I1_I2
}
extension flow_overrides._ExportedKotlinPackages_namespace_I1_I2 where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension flow_overrides._ExportedKotlinPackages_namespace_I1_I2 {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.namespace.I1 where Wrapped : ExportedKotlinPackages.namespace._I1 {
}
extension KotlinRuntimeSupport._KotlinExistential: flow_overrides._ExportedKotlinPackages_namespace_I1_I2 where Wrapped : flow_overrides.__ExportedKotlinPackages_namespace_I1_I2 {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.namespace._I1 {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: flow_overrides.__ExportedKotlinPackages_namespace_I1_I2 {
}
extension ExportedKotlinPackages.namespace {
    public protocol I1: KotlinRuntime.KotlinBase, ExportedKotlinPackages.namespace._I1 {
    }
    @objc(_I1)
    public protocol _I1 {
    }
    open class Bar: ExportedKotlinPackages.namespace.Foo {
        @_nonoverride
        open var voo: any KotlinCoroutineSupport.KotlinTypedFlow<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2> {
            get {
                return KotlinCoroutineSupport._KotlinTypedFlowImpl<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Bar_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
            }
        }
        public override init() {
            let __kt = namespace_Bar_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { namespace_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func foo() -> any KotlinCoroutineSupport.KotlinTypedFlow<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2> {
            return KotlinCoroutineSupport._KotlinTypedFlowImpl<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Bar_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
        }
    }
    open class Foo: KotlinRuntime.KotlinBase {
        open var voo: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            get {
                return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Foo_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
            }
        }
        public init() {
            let __kt = namespace_Foo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { namespace_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func foo() -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Foo_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
        }
    }
    open class MutableSharedFoo: ExportedKotlinPackages.namespace.SharedFoo {
        @_nonoverride
        open var voo: any KotlinCoroutineSupport.KotlinTypedMutableSharedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            get {
                return KotlinCoroutineSupport._KotlinTypedMutableSharedFlowImpl<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_MutableSharedFoo_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow)
            }
        }
        public override init() {
            let __kt = namespace_MutableSharedFoo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { namespace_MutableSharedFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func foo() -> any KotlinCoroutineSupport.KotlinTypedMutableSharedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            return KotlinCoroutineSupport._KotlinTypedMutableSharedFlowImpl<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_MutableSharedFoo_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow)
        }
    }
    open class MutableStateFoo: ExportedKotlinPackages.namespace.StateFoo {
        @_nonoverride
        open var voo: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            get {
                return KotlinCoroutineSupport._KotlinTypedMutableStateFlowImpl<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_MutableStateFoo_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow)
            }
        }
        public override init() {
            let __kt = namespace_MutableStateFoo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { namespace_MutableStateFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func foo() -> any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            return KotlinCoroutineSupport._KotlinTypedMutableStateFlowImpl<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_MutableStateFoo_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow)
        }
    }
    open class Nar: ExportedKotlinPackages.namespace.Foo {
        @_nonoverride
        open var voo: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Never> {
            get {
                return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Never>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Nar_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
            }
        }
        public override init() {
            let __kt = namespace_Nar_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { namespace_Nar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func foo() -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Never> {
            return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Never>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Nar_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
        }
    }
    open class SharedFoo: ExportedKotlinPackages.namespace.Foo {
        @_nonoverride
        open var voo: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            get {
                return KotlinCoroutineSupport._KotlinTypedSharedFlowImpl<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_SharedFoo_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow)
            }
        }
        public override init() {
            let __kt = namespace_SharedFoo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { namespace_SharedFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func foo() -> any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            return KotlinCoroutineSupport._KotlinTypedSharedFlowImpl<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_SharedFoo_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow)
        }
    }
    open class StateFoo: ExportedKotlinPackages.namespace.SharedFoo {
        @_nonoverride
        open var voo: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            get {
                return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_StateFoo_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow)
            }
        }
        public override init() {
            let __kt = namespace_StateFoo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { namespace_StateFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func foo() -> any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_StateFoo_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow)
        }
    }
    open class Zar: ExportedKotlinPackages.namespace.Foo {
        @_nonoverride
        open var voo: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>> {
            get {
                return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Zar_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
            }
        }
        public override init() {
            let __kt = namespace_Zar_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { namespace_Zar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func foo() -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>> {
            return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Zar_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
        }
    }
}
@_cdecl("namespace_Bar_foo__reverse_swift")
public func namespace_Bar_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.namespace.Bar.__createClassWrapper(externalRCRef: `self`)!
    let _result: any KotlinCoroutineSupport.KotlinTypedFlow<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2> = _self.foo()
    return _result.wrapped.__externalRCRef()
}

@_cdecl("namespace_Foo_foo__reverse_swift")
public func namespace_Foo_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.namespace.Foo.__createClassWrapper(externalRCRef: `self`)!
    let _result: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> = _self.foo()
    return _result.wrapped.__externalRCRef()
}

@_cdecl("namespace_MutableSharedFoo_foo__reverse_swift")
public func namespace_MutableSharedFoo_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.namespace.MutableSharedFoo.__createClassWrapper(externalRCRef: `self`)!
    let _result: any KotlinCoroutineSupport.KotlinTypedMutableSharedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> = _self.foo()
    return _result.wrapped.__externalRCRef()
}

@_cdecl("namespace_MutableStateFoo_foo__reverse_swift")
public func namespace_MutableStateFoo_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.namespace.MutableStateFoo.__createClassWrapper(externalRCRef: `self`)!
    let _result: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> = _self.foo()
    return _result.wrapped.__externalRCRef()
}

@_cdecl("namespace_Nar_foo__reverse_swift")
public func namespace_Nar_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.namespace.Nar.__createClassWrapper(externalRCRef: `self`)!
    let _result: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Never> = _self.foo()
    return _result.wrapped.__externalRCRef()
}

@_cdecl("namespace_SharedFoo_foo__reverse_swift")
public func namespace_SharedFoo_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.namespace.SharedFoo.__createClassWrapper(externalRCRef: `self`)!
    let _result: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> = _self.foo()
    return _result.wrapped.__externalRCRef()
}

@_cdecl("namespace_StateFoo_foo__reverse_swift")
public func namespace_StateFoo_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.namespace.StateFoo.__createClassWrapper(externalRCRef: `self`)!
    let _result: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> = _self.foo()
    return _result.wrapped.__externalRCRef()
}

@_cdecl("namespace_Zar_foo__reverse_swift")
public func namespace_Zar_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.namespace.Zar.__createClassWrapper(externalRCRef: `self`)!
    let _result: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>> = _self.foo()
    return _result.wrapped.__externalRCRef()
}
