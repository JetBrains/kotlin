@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public protocol A: KotlinRuntime.KotlinBase {
    var foo: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get
    }
}
public protocol B: KotlinRuntime.KotlinBase {
    var foo: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get
    }
}
public protocol Consumer: KotlinRuntime.KotlinBase {
    func consume(
        item: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void
}
public protocol ConsumerProducer: KotlinRuntime.KotlinBase, main.Consumer, main.Producer {
}
public protocol Processor: KotlinRuntime.KotlinBase {
    func process(
        input: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
}
public protocol Producer: KotlinRuntime.KotlinBase {
    func produce() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
}
@objc(_A)
package protocol _A {
}
@objc(_B)
package protocol _B {
}
@objc(_Consumer)
package protocol _Consumer {
}
@objc(_ConsumerProducer)
package protocol _ConsumerProducer: main._Consumer, main._Producer {
}
@objc(_Processor)
package protocol _Processor {
}
@objc(_Producer)
package protocol _Producer {
}
public final class AnyConsumer: KotlinRuntime.KotlinBase {
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
        item: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> Swift.Void {
        return AnyConsumer_consume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), item.__externalRCRef())
    }
}
public final class ArrayBox: KotlinRuntime.KotlinBase {
    public var ints: ExportedKotlinPackages.kotlin.Array {
        get {
            return ExportedKotlinPackages.kotlin.Array.__createClassWrapper(externalRCRef: ArrayBox_ints_get(self.__externalRCRef()))
        }
    }
    public init() {
        if Self.self != main.ArrayBox.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.ArrayBox ") }
        let __kt = __root___ArrayBox_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___ArrayBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
open class Box: KotlinRuntime.KotlinBase {
    public final var t: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch Box_t_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
    package init(
        t: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
        t: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) {
        if Self.self != main.DefaultBox.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.DefaultBox ") }
        let __kt = __root___DefaultBox_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___DefaultBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt, t.map { it in it.__externalRCRef() } ?? nil)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class Demo: KotlinRuntime.KotlinBase {
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
public final class FunctionalBox: main.Box {
    public init() {
        if Self.self != main.FunctionalBox.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.FunctionalBox ") }
        let __kt = __root___FunctionalBox_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___FunctionalBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class GenericWithComparableUpperBound: KotlinRuntime.KotlinBase {
    public var t: any ExportedKotlinPackages.kotlin.Comparable {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: GenericWithComparableUpperBound_t_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.Comparable
        }
    }
    public init(
        t: any ExportedKotlinPackages.kotlin.Comparable
    ) {
        if Self.self != main.GenericWithComparableUpperBound.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.GenericWithComparableUpperBound ") }
        let __kt = __root___GenericWithComparableUpperBound_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___GenericWithComparableUpperBound_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_Comparable__(__kt, t.__externalRCRef())
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class Holder: KotlinRuntime.KotlinBase {
    public var xs: ExportedKotlinPackages.kotlin.Array {
        get {
            return ExportedKotlinPackages.kotlin.Array.__createClassWrapper(externalRCRef: Holder_xs_get(self.__externalRCRef()))
        }
    }
    public init(
        xs: ExportedKotlinPackages.kotlin.Array
    ) {
        if Self.self != main.Holder.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Holder ") }
        let __kt = __root___Holder_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Holder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Array__(__kt, xs.__externalRCRef())
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func headOrNull() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch Holder_headOrNull(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
public final class IdentityProcessor: KotlinRuntime.KotlinBase {
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
        input: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch IdentityProcessor_process__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), input.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
public final class Pair: KotlinRuntime.KotlinBase {
    public var first: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch Pair_first_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
    public var second: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch Pair_second_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public init(
        first: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        second: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) {
        if Self.self != main.Pair.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Pair ") }
        let __kt = __root___Pair_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt, first.map { it in it.__externalRCRef() } ?? nil, second.map { it in it.__externalRCRef() } ?? nil)
    }
}
open class StringProducer: KotlinRuntime.KotlinBase {
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
public func bar(
    param1: (any KotlinRuntimeSupport._KotlinBridgeable)?,
    param2: (any KotlinRuntimeSupport._KotlinBridgeable)?
) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return { switch __root___bar__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(param1.map { it in it.__externalRCRef() } ?? nil, param2.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
}
public func createMap(
    pairs: [main.Pair]
) -> [Swift.AnyHashable?: (any KotlinRuntimeSupport._KotlinBridgeable)?] {
    return __root___createMap__TypesOfArguments__Swift_Array_main_Pair___(pairs) as! Swift.Dictionary<Swift.Optional<Swift.AnyHashable>,Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
}
public func customFilter(
    _ receiver: [(any KotlinRuntimeSupport._KotlinBridgeable)?],
    predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Bool
) -> [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
    return __root___customFilter__TypesOfArguments__Swift_Array_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Bool__(receiver.map { it in it as! NSObject? ?? NSNull() }, {
        let originalBlock = predicate
        return { arg0 in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()) }
    }()) as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
}
public func foo(
    param1: (any KotlinRuntimeSupport._KotlinBridgeable)?,
    param2: (any KotlinRuntimeSupport._KotlinBridgeable)?
) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return { switch __root___foo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(param1.map { it in it.__externalRCRef() } ?? nil, param2.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
}
extension main.A where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var foo: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch A_foo_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
}
extension main.A {
}
extension main.B where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var foo: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch B_foo_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
}
extension main.B {
}
extension main.Consumer where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func consume(
        item: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return Consumer_consume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), item.map { it in it.__externalRCRef() } ?? nil)
    }
}
extension main.Consumer {
}
extension main.ConsumerProducer where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension main.ConsumerProducer {
}
extension main.Processor where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func process(
        input: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch Processor_process__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), input.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
extension main.Processor {
}
extension main.Producer where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func produce() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch Producer_produce(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
extension main.Producer {
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
