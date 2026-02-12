@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_flow_overrides
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinxCoroutinesCore

public protocol _ExportedKotlinPackages_namespace_I1_I2: KotlinRuntime.KotlinBase, ExportedKotlinPackages.namespace.I1 {
}
@objc(__ExportedKotlinPackages_namespace_I1_I2)
package protocol __ExportedKotlinPackages_namespace_I1_I2: ExportedKotlinPackages.namespace._I1 {
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
extension ExportedKotlinPackages.namespace {
    public protocol I1: KotlinRuntime.KotlinBase {
    }
    @objc(_I1)
    package protocol _I1 {
    }
    open class Bar: ExportedKotlinPackages.namespace.Foo {
        @_nonoverride
        open var voo: KotlinCoroutineSupport._KotlinTypedFlow<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2> {
            get {
                return KotlinCoroutineSupport._KotlinTypedFlow<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Bar_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
            }
        }
        public override init() {
            if Self.self != ExportedKotlinPackages.namespace.Bar.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.namespace.Bar ") }
            let __kt = namespace_Bar_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            namespace_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        open func foo() -> KotlinCoroutineSupport._KotlinTypedFlow<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2> {
            return KotlinCoroutineSupport._KotlinTypedFlow<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Bar_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
        }
    }
    open class Foo: KotlinRuntime.KotlinBase {
        open var voo: KotlinCoroutineSupport._KotlinTypedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            get {
                return KotlinCoroutineSupport._KotlinTypedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Foo_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
            }
        }
        public init() {
            if Self.self != ExportedKotlinPackages.namespace.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.namespace.Foo ") }
            let __kt = namespace_Foo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            namespace_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        open func foo() -> KotlinCoroutineSupport._KotlinTypedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>> {
            return KotlinCoroutineSupport._KotlinTypedFlow<Swift.Optional<any ExportedKotlinPackages.namespace.I1>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Foo_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
        }
    }
    open class Nar: ExportedKotlinPackages.namespace.Foo {
        @_nonoverride
        open var voo: KotlinCoroutineSupport._KotlinTypedFlow<Swift.Never> {
            get {
                return KotlinCoroutineSupport._KotlinTypedFlow<Swift.Never>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Nar_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
            }
        }
        public override init() {
            if Self.self != ExportedKotlinPackages.namespace.Nar.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.namespace.Nar ") }
            let __kt = namespace_Nar_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            namespace_Nar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        open func foo() -> KotlinCoroutineSupport._KotlinTypedFlow<Swift.Never> {
            return KotlinCoroutineSupport._KotlinTypedFlow<Swift.Never>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Nar_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
        }
    }
    open class StateFoo: ExportedKotlinPackages.namespace.Foo {
        @_nonoverride
        open var voo: any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_StateFoo_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow
            }
        }
        public override init() {
            if Self.self != ExportedKotlinPackages.namespace.StateFoo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.namespace.StateFoo ") }
            let __kt = namespace_StateFoo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            namespace_StateFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        open func foo() -> any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_StateFoo_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow
        }
    }
    open class Zar: ExportedKotlinPackages.namespace.Foo {
        @_nonoverride
        open var voo: KotlinCoroutineSupport._KotlinTypedFlow<Swift.Optional<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>> {
            get {
                return KotlinCoroutineSupport._KotlinTypedFlow<Swift.Optional<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Zar_voo_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
            }
        }
        public override init() {
            if Self.self != ExportedKotlinPackages.namespace.Zar.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.namespace.Zar ") }
            let __kt = namespace_Zar_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            namespace_Zar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        open func foo() -> KotlinCoroutineSupport._KotlinTypedFlow<Swift.Optional<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>> {
            return KotlinCoroutineSupport._KotlinTypedFlow<Swift.Optional<any flow_overrides._ExportedKotlinPackages_namespace_I1_I2>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: namespace_Zar_foo(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
        }
    }
}
