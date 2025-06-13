@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public extension ExportedKotlinPackages.generation.generics.generics.A where Self : KotlinRuntimeSupport._KotlinBridged {
    public var foo: KotlinRuntime.KotlinBase? {
        get {
            return { switch generation_generics_generics_A_foo_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
        }
    }
}
public extension ExportedKotlinPackages.generation.generics.generics.B where Self : KotlinRuntimeSupport._KotlinBridged {
    public var foo: KotlinRuntime.KotlinBase? {
        get {
            return { switch generation_generics_generics_B_foo_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
        }
    }
}
public extension ExportedKotlinPackages.generation.generics.generics.Consumer where Self : KotlinRuntimeSupport._KotlinBridged {
    public func consume(
        item: KotlinRuntime.KotlinBase?
    ) -> Swift.Void {
        return generation_generics_generics_Consumer_consume__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), item.map { it in it.__externalRCRef() } ?? nil)
    }
}
public extension ExportedKotlinPackages.generation.generics.generics.ConsumerProducer where Self : KotlinRuntimeSupport._KotlinBridged {
}
public extension ExportedKotlinPackages.generation.generics.generics.Processor where Self : KotlinRuntimeSupport._KotlinBridged {
    public func process(
        input: KotlinRuntime.KotlinBase?
    ) -> KotlinRuntime.KotlinBase? {
        return { switch generation_generics_generics_Processor_process__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), input.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
}
public extension ExportedKotlinPackages.generation.generics.generics.Producer where Self : KotlinRuntimeSupport._KotlinBridged {
    public func produce() -> KotlinRuntime.KotlinBase? {
        return { switch generation_generics_generics_Producer_produce(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.generation.generics.generics.Producer where Wrapped : ExportedKotlinPackages.generation.generics.generics._Producer {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.generation.generics.generics.Consumer where Wrapped : ExportedKotlinPackages.generation.generics.generics._Consumer {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.generation.generics.generics.Processor where Wrapped : ExportedKotlinPackages.generation.generics.generics._Processor {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.generation.generics.generics.ConsumerProducer where Wrapped : ExportedKotlinPackages.generation.generics.generics._ConsumerProducer {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.generation.generics.generics.A where Wrapped : ExportedKotlinPackages.generation.generics.generics._A {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.generation.generics.generics.B where Wrapped : ExportedKotlinPackages.generation.generics.generics._B {
}
public extension ExportedKotlinPackages.generation.generics.generics {
    public protocol A: KotlinRuntime.KotlinBase {
        var foo: KotlinRuntime.KotlinBase? {
            get
        }
    }
    public protocol B: KotlinRuntime.KotlinBase {
        var foo: KotlinRuntime.KotlinBase? {
            get
        }
    }
    public protocol Consumer: KotlinRuntime.KotlinBase {
        func consume(
            item: KotlinRuntime.KotlinBase?
        ) -> Swift.Void
    }
    public protocol ConsumerProducer: KotlinRuntime.KotlinBase, ExportedKotlinPackages.generation.generics.generics.Consumer, ExportedKotlinPackages.generation.generics.generics.Producer {
    }
    public protocol Processor: KotlinRuntime.KotlinBase {
        func process(
            input: KotlinRuntime.KotlinBase?
        ) -> KotlinRuntime.KotlinBase?
    }
    public protocol Producer: KotlinRuntime.KotlinBase {
        func produce() -> KotlinRuntime.KotlinBase?
    }
    @objc(_A)
    protocol _A {
    }
    @objc(_B)
    protocol _B {
    }
    @objc(_Consumer)
    protocol _Consumer {
    }
    @objc(_ConsumerProducer)
    protocol _ConsumerProducer: ExportedKotlinPackages.generation.generics.generics._Consumer, ExportedKotlinPackages.generation.generics.generics._Producer {
    }
    @objc(_Processor)
    protocol _Processor {
    }
    @objc(_Producer)
    protocol _Producer {
    }
    public final class AnyConsumer: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public init() {
            if Self.self != ExportedKotlinPackages.generation.generics.generics.AnyConsumer.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.generation.generics.generics.AnyConsumer ") }
            let __kt = generation_generics_generics_AnyConsumer_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            generation_generics_generics_AnyConsumer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public func consume(
            item: KotlinRuntime.KotlinBase
        ) -> Swift.Void {
            return generation_generics_generics_AnyConsumer_consume__TypesOfArguments__KotlinRuntime_KotlinBase__(self.__externalRCRef(), item.__externalRCRef())
        }
    }
    open class Box: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final var t: KotlinRuntime.KotlinBase? {
            get {
                return { switch generation_generics_generics_Box_t_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
            }
        }
        package init(
            t: KotlinRuntime.KotlinBase?
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class CPImpl: ExportedKotlinPackages.generation.generics.generics.StringProducer {
        public override init() {
            if Self.self != ExportedKotlinPackages.generation.generics.generics.CPImpl.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.generation.generics.generics.CPImpl ") }
            let __kt = generation_generics_generics_CPImpl_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            generation_generics_generics_CPImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public func consume(
            item: Swift.String
        ) -> Swift.Void {
            return generation_generics_generics_CPImpl_consume__TypesOfArguments__Swift_String__(self.__externalRCRef(), item)
        }
    }
    public final class DefaultBox: ExportedKotlinPackages.generation.generics.generics.Box {
        public override init(
            t: KotlinRuntime.KotlinBase?
        ) {
            if Self.self != ExportedKotlinPackages.generation.generics.generics.DefaultBox.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.generation.generics.generics.DefaultBox ") }
            let __kt = generation_generics_generics_DefaultBox_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            generation_generics_generics_DefaultBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_KotlinRuntime_KotlinBase___(__kt, t.map { it in it.__externalRCRef() } ?? nil)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class Demo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var foo: Swift.Int32 {
            get {
                return generation_generics_generics_Demo_foo_get(self.__externalRCRef())
            }
        }
        public init() {
            if Self.self != ExportedKotlinPackages.generation.generics.generics.Demo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.generation.generics.generics.Demo ") }
            let __kt = generation_generics_generics_Demo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            generation_generics_generics_Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class IdentityProcessor: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public init() {
            if Self.self != ExportedKotlinPackages.generation.generics.generics.IdentityProcessor.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.generation.generics.generics.IdentityProcessor ") }
            let __kt = generation_generics_generics_IdentityProcessor_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            generation_generics_generics_IdentityProcessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public func process(
            input: KotlinRuntime.KotlinBase?
        ) -> KotlinRuntime.KotlinBase? {
            return { switch generation_generics_generics_IdentityProcessor_process__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), input.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
        }
    }
    public final class Pair: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var first: KotlinRuntime.KotlinBase? {
            get {
                return { switch generation_generics_generics_Pair_first_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
            }
        }
        public var second: KotlinRuntime.KotlinBase? {
            get {
                return { switch generation_generics_generics_Pair_second_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public init(
            first: KotlinRuntime.KotlinBase?,
            second: KotlinRuntime.KotlinBase?
        ) {
            if Self.self != ExportedKotlinPackages.generation.generics.generics.Pair.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.generation.generics.generics.Pair ") }
            let __kt = generation_generics_generics_Pair_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            generation_generics_generics_Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___(__kt, first.map { it in it.__externalRCRef() } ?? nil, second.map { it in it.__externalRCRef() } ?? nil)
        }
    }
    open class StringProducer: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public init() {
            if Self.self != ExportedKotlinPackages.generation.generics.generics.StringProducer.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.generation.generics.generics.StringProducer ") }
            let __kt = generation_generics_generics_StringProducer_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            generation_generics_generics_StringProducer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        open func produce() -> Swift.String {
            return generation_generics_generics_StringProducer_produce(self.__externalRCRef())
        }
    }
    public final class TripleBox: ExportedKotlinPackages.generation.generics.generics.Box {
        public init() {
            if Self.self != ExportedKotlinPackages.generation.generics.generics.TripleBox.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.generation.generics.generics.TripleBox ") }
            let __kt = generation_generics_generics_TripleBox_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            generation_generics_generics_TripleBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public static func createMap(
        pairs: [ExportedKotlinPackages.generation.generics.generics.Pair]
    ) -> [KotlinRuntime.KotlinBase?: KotlinRuntime.KotlinBase?] {
        return generation_generics_generics_createMap__TypesOfArguments__Swift_Array_ExportedKotlinPackages_generation_generics_generics_Pair___(pairs) as! Swift.Dictionary<Swift.Optional<KotlinRuntime.KotlinBase>,Swift.Optional<KotlinRuntime.KotlinBase>>
    }
    public static func customFilter(
        _ receiver: [KotlinRuntime.KotlinBase?],
        predicate: @escaping (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Bool
    ) -> [KotlinRuntime.KotlinBase?] {
        return generation_generics_generics_customFilter__TypesOfArguments__Swift_Array_Swift_Optional_KotlinRuntime_KotlinBase___U28Swift_Optional_KotlinRuntime_KotlinBase_U29202D_U20Swift_Bool__(receiver.map { it in it as NSObject? ?? NSNull() }, {
            let originalBlock = predicate
            return { arg0 in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()) }
        }()) as! Swift.Array<Swift.Optional<KotlinRuntime.KotlinBase>>
    }
    public static func foo(
        param1: KotlinRuntime.KotlinBase?,
        param2: KotlinRuntime.KotlinBase?
    ) -> Swift.Void {
        return generation_generics_generics_foo__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___(param1.map { it in it.__externalRCRef() } ?? nil, param2.map { it in it.__externalRCRef() } ?? nil)
    }
}
