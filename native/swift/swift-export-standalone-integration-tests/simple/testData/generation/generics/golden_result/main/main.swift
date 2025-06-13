@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

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
public protocol ConsumerProducer: KotlinRuntime.KotlinBase, main.Consumer, main.Producer {
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
protocol _ConsumerProducer: main._Consumer, main._Producer {
}
@objc(_Processor)
protocol _Processor {
}
@objc(_Producer)
protocol _Producer {
}
public final class AnyConsumer: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public init() {
        if Self.self != main.AnyConsumer.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.AnyConsumer ") }
        let __kt = __root___AnyConsumer_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___AnyConsumer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
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
        return AnyConsumer_consume__TypesOfArguments__KotlinRuntime_KotlinBase__(self.__externalRCRef(), item.__externalRCRef())
    }
}
open class Box: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public final var t: KotlinRuntime.KotlinBase? {
        get {
            return { switch Box_t_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
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
public final class CPImpl: main.StringProducer {
    public override init() {
        if Self.self != main.CPImpl.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.CPImpl ") }
        let __kt = __root___CPImpl_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___CPImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
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
        return CPImpl_consume__TypesOfArguments__Swift_String__(self.__externalRCRef(), item)
    }
}
public final class DefaultBox: main.Box {
    public override init(
        t: KotlinRuntime.KotlinBase?
    ) {
        if Self.self != main.DefaultBox.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.DefaultBox ") }
        let __kt = __root___DefaultBox_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___DefaultBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_KotlinRuntime_KotlinBase___(__kt, t.map { it in it.__externalRCRef() } ?? nil)
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
            return Demo_foo_get(self.__externalRCRef())
        }
    }
    public init() {
        if Self.self != main.Demo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Demo ") }
        let __kt = __root___Demo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
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
        if Self.self != main.IdentityProcessor.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.IdentityProcessor ") }
        let __kt = __root___IdentityProcessor_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___IdentityProcessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
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
        return { switch IdentityProcessor_process__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), input.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
}
public final class Pair: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var first: KotlinRuntime.KotlinBase? {
        get {
            return { switch Pair_first_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
        }
    }
    public var second: KotlinRuntime.KotlinBase? {
        get {
            return { switch Pair_second_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
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
        if Self.self != main.Pair.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Pair ") }
        let __kt = __root___Pair_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___(__kt, first.map { it in it.__externalRCRef() } ?? nil, second.map { it in it.__externalRCRef() } ?? nil)
    }
}
open class StringProducer: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public init() {
        if Self.self != main.StringProducer.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.StringProducer ") }
        let __kt = __root___StringProducer_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___StringProducer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    open func produce() -> Swift.String {
        return StringProducer_produce(self.__externalRCRef())
    }
}
public final class TripleBox: main.Box {
    public init() {
        if Self.self != main.TripleBox.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.TripleBox ") }
        let __kt = __root___TripleBox_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___TripleBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public func createMap(
    pairs: [main.Pair]
) -> [KotlinRuntime.KotlinBase?: KotlinRuntime.KotlinBase?] {
    return __root___createMap__TypesOfArguments__Swift_Array_main_Pair___(pairs) as! Swift.Dictionary<Swift.Optional<KotlinRuntime.KotlinBase>,Swift.Optional<KotlinRuntime.KotlinBase>>
}
public func customFilter(
    _ receiver: [KotlinRuntime.KotlinBase?],
    predicate: @escaping (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Bool
) -> [KotlinRuntime.KotlinBase?] {
    return __root___customFilter__TypesOfArguments__Swift_Array_Swift_Optional_KotlinRuntime_KotlinBase___U28Swift_Optional_KotlinRuntime_KotlinBase_U29202D_U20Swift_Bool__(receiver.map { it in it as NSObject? ?? NSNull() }, {
        let originalBlock = predicate
        return { arg0 in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()) }
    }()) as! Swift.Array<Swift.Optional<KotlinRuntime.KotlinBase>>
}
public func foo(
    param1: KotlinRuntime.KotlinBase?,
    param2: KotlinRuntime.KotlinBase?
) -> Swift.Void {
    return __root___foo__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___(param1.map { it in it.__externalRCRef() } ?? nil, param2.map { it in it.__externalRCRef() } ?? nil)
}
public extension main.A where Self : KotlinRuntimeSupport._KotlinBridged {
    public var foo: KotlinRuntime.KotlinBase? {
        get {
            return { switch A_foo_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
        }
    }
}
public extension main.B where Self : KotlinRuntimeSupport._KotlinBridged {
    public var foo: KotlinRuntime.KotlinBase? {
        get {
            return { switch B_foo_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
        }
    }
}
public extension main.Consumer where Self : KotlinRuntimeSupport._KotlinBridged {
    public func consume(
        item: KotlinRuntime.KotlinBase?
    ) -> Swift.Void {
        return Consumer_consume__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), item.map { it in it.__externalRCRef() } ?? nil)
    }
}
public extension main.ConsumerProducer where Self : KotlinRuntimeSupport._KotlinBridged {
}
public extension main.Processor where Self : KotlinRuntimeSupport._KotlinBridged {
    public func process(
        input: KotlinRuntime.KotlinBase?
    ) -> KotlinRuntime.KotlinBase? {
        return { switch Processor_process__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), input.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
}
public extension main.Producer where Self : KotlinRuntimeSupport._KotlinBridged {
    public func produce() -> KotlinRuntime.KotlinBase? {
        return { switch Producer_produce(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
}
extension KotlinRuntimeSupport._KotlinExistential: main.Producer where Wrapped : main._Producer {
}
extension KotlinRuntimeSupport._KotlinExistential: main.Consumer where Wrapped : main._Consumer {
}
extension KotlinRuntimeSupport._KotlinExistential: main.Processor where Wrapped : main._Processor {
}
extension KotlinRuntimeSupport._KotlinExistential: main.ConsumerProducer where Wrapped : main._ConsumerProducer {
}
extension KotlinRuntimeSupport._KotlinExistential: main.A where Wrapped : main._A {
}
extension KotlinRuntimeSupport._KotlinExistential: main.B where Wrapped : main._B {
}
