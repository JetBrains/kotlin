@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public typealias BFun = (any main.B) -> Swift.Void
public typealias BoxFun<T: KotlinRuntimeSupport._KotlinBridgeable> = () -> main.Box<T>
public typealias BoxFunIn<T: KotlinRuntimeSupport._KotlinBridgeable> = (main.Box<T>) -> Swift.Int32
public protocol A<T>: KotlinRuntime.KotlinBase, main._A {
    associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
    var foo: T {
        get
    }
}
public protocol AFactory<T>: KotlinRuntime.KotlinBase, main._AFactory where T.T == KotlinRuntimeSupport.KotlinUnit {
    associatedtype T: main.A
    func create() -> T
}
public protocol B<T>: KotlinRuntime.KotlinBase, main._B {
    associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
    var foo: T {
        get
    }
}
public protocol Consumer<T>: KotlinRuntime.KotlinBase, main._Consumer {
    associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
    func consume(
        item: T
    ) -> Swift.Void
}
public protocol ConsumerProducer<T>: KotlinRuntime.KotlinBase, main.Consumer, main.Producer, main._ConsumerProducer {
}
public protocol Foo: KotlinRuntime.KotlinBase, main._Foo {
}
public protocol Processor<T, R>: KotlinRuntime.KotlinBase, main._Processor {
    associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
    associatedtype R: KotlinRuntimeSupport._KotlinBridgeable
    func process(
        input: T
    ) -> R
}
public protocol Producer<T>: KotlinRuntime.KotlinBase, main._Producer {
    associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
    func produce() -> T
}
@objc(_main_A)
public protocol _A {
}
@objc(_main_AFactory)
public protocol _AFactory {
}
@objc(_main_B)
public protocol _B {
}
@_documentation(visibility: internal)
public protocol _Bar_ReverseDispatch<A, B>: KotlinRuntime.KotlinBase {
    associatedtype A: main.Foo
    associatedtype B: main.Foo
}
@_documentation(visibility: internal)
public protocol _Box_ReverseDispatch<T>: KotlinRuntime.KotlinBase {
    associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
}
@objc(_main_Consumer)
public protocol _Consumer {
}
@objc(_main_ConsumerProducer)
public protocol _ConsumerProducer: main._Consumer, main._Producer {
}
@objc(_main_Foo)
public protocol _Foo {
}
@objc(_main_Processor)
public protocol _Processor {
}
@objc(_main_Producer)
public protocol _Producer {
}
public protocol __A: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __AFactory: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __B: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __Consumer: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __ConsumerProducer: KotlinRuntimeSupport._KotlinBridgeable, main.__Consumer, main.__Producer {
}
public protocol __Foo: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __Processor: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __Producer: KotlinRuntimeSupport._KotlinBridgeable {
}
public final class AnyA<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, main.A, main.__A, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class AnyAFactory<T: main.A>: KotlinRuntime.KotlinBase, main.AFactory, main.__AFactory, KotlinRuntimeSupport._KotlinCustomPointerBridgeable where T.T == KotlinRuntimeSupport.KotlinUnit {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class AnyB<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, main.B, main.__B, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class AnyConsumer: KotlinRuntime.KotlinBase, main.Consumer, main.__Consumer {
    public typealias T = KotlinRuntimeSupport.KotlinAny
    public override init() {
        let __kt = __root___AnyConsumer_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___AnyConsumer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func consume(
        item: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> Swift.Void {
        return { AnyConsumer_consume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), (item).__retainedToKotlinPointer()!); return () }()
    }
}
public final class AnyConsumerProducer<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, main.ConsumerProducer, main.__ConsumerProducer, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class AnyProcessor<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, main.Processor, main.__Processor, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class AnyProducer<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, main.Producer, main.__Producer, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
    @inlinable
    public convenience init(
        _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
    ) {
        self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class ArrayBox: KotlinRuntime.KotlinBase {
    public var ints: ExportedKotlinPackages.kotlin.Array<Swift.Int32> {
        get {
            return ExportedKotlinPackages.kotlin.Array<Swift.Int32>.__fromKotlinPointer(ArrayBox_ints_get(self.__externalRCRef()))
        }
    }
    public override init() {
        let __kt = __root___ArrayBox_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ArrayBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
open class Bar<A: main.Foo, B: main.Foo>: main._Bar_Base, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, main._Bar_ReverseDispatch {
    public init() {
        precondition(Self.self != main.Bar<A,B>.self, "main.Bar is an abstract class and cannot be instantiated directly")
        let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
open class Box<T: KotlinRuntimeSupport._KotlinBridgeable>: main._Box_Base, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, main._Box_ReverseDispatch {
    @inlinable
    public final var t: T {
        get {
            return T.__fromKotlinPointer(_cbridge_Box_t_get(self.__externalRCRef()))
        }
    }
    public init(
        t: T
    ) {
        precondition(Self.self != main.Box<T>.self, "main.Box is an abstract class and cannot be instantiated directly")
        let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Box_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T__(__kt, t.__retainedToKotlinPointer()); return () }()
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class CPImpl: main.StringProducer, main.ConsumerProducer, main.__ConsumerProducer {
    public override init() {
        let __kt = __root___CPImpl_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___CPImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func consume(
        item: Swift.String
    ) -> Swift.Void {
        return { CPImpl_consume__TypesOfArguments__Swift_String__(self.__externalRCRef(), item); return () }()
    }
}
public final class DefaultBox<T: KotlinRuntimeSupport._KotlinBridgeable>: main.Box<T> {
    public override init(
        t: T
    ) {
        let __kt = __root___DefaultBox_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
        { __root___DefaultBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T__(__kt, t.__retainedToKotlinPointer()); return () }()
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class Demo: KotlinRuntime.KotlinBase, main.A, main.__A {
    public typealias T = Swift.Int32
    public var foo: Swift.Int32 {
        get {
            return Demo_foo_get(self.__externalRCRef())
        }
    }
    public override init() {
        let __kt = __root___Demo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class FunctionalBox: main.Box<KotlinRuntimeSupport.KotlinAny> {
    public init() {
        let __kt = __root___FunctionalBox_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___FunctionalBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class GenericWithComparableUpperBound<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @inlinable
    public var t: T {
        get {
            return T.__fromKotlinPointer(_cbridge_GenericWithComparableUpperBound_t_get(self.__externalRCRef()))
        }
    }
    public init(
        t: T
    ) {
        let __kt = __root___GenericWithComparableUpperBound_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
        { __root___GenericWithComparableUpperBound_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T__(__kt, t.__retainedToKotlinPointer()); return () }()
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class Holder<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @inlinable
    public var xs: ExportedKotlinPackages.kotlin.Array<T> {
        get {
            return ExportedKotlinPackages.kotlin.Array<T>.__fromKotlinPointer(_cbridge_Holder_xs_get(self.__externalRCRef()))
        }
    }
    public init(
        xs: ExportedKotlinPackages.kotlin.Array<T>
    ) {
        let __kt = __root___Holder_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
        { __root___Holder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Array_T___(__kt, xs.__externalRCRef()); return () }()
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    @inlinable
    public func headOrNull() -> T? {
        return Swift.Optional<T>.__fromKotlinPointer(_cbridge_Holder_headOrNull(self.__externalRCRef()))
    }
}
public final class HolderConstrained<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @inlinable
    public var xs: ExportedKotlinPackages.kotlin.Array<T> {
        get {
            return ExportedKotlinPackages.kotlin.Array<T>.__fromKotlinPointer(_cbridge_HolderConstrained_xs_get(self.__externalRCRef()))
        }
    }
    public init(
        xs: ExportedKotlinPackages.kotlin.Array<T>
    ) {
        let __kt = __root___HolderConstrained_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
        { __root___HolderConstrained_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Array_T___(__kt, xs.__externalRCRef()); return () }()
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class IdentityProcessor<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, main.Processor, main.__Processor {
    public typealias R = T
    public override init() {
        let __kt = __root___IdentityProcessor_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
        { __root___IdentityProcessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func process(
        input: T
    ) -> T {
        return T.__fromKotlinPointer(IdentityProcessor_process__TypesOfArguments__T__(self.__externalRCRef(), input.__retainedToKotlinPointer()))
    }
}
public final class MultipleUpperBounds<T: main.Producer & main.Consumer>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable where T.T == Swift.String {
    public override init() {
        let __kt = __root___MultipleUpperBounds_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
        { __root___MultipleUpperBounds_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class Pair<K: KotlinRuntimeSupport._KotlinBridgeable, V: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @inlinable
    public var first: K {
        get {
            return K.__fromKotlinPointer(_cbridge_Pair_first_get(self.__externalRCRef()))
        }
    }
    @inlinable
    public var second: V {
        get {
            return V.__fromKotlinPointer(_cbridge_Pair_second_get(self.__externalRCRef()))
        }
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public init(
        first: K,
        second: V
    ) {
        let __kt = __root___Pair_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
        { __root___Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_K_V__(__kt, first.__retainedToKotlinPointer(), second.__retainedToKotlinPointer()); return () }()
    }
}
open class StringProducer: KotlinRuntime.KotlinBase, main.Producer, main.__Producer {
    public typealias T = Swift.String
    public override init() {
         let __kt: Swift.UnsafeMutableRawPointer!
         if Self.self == main.StringProducer.self {
             __kt = __root___StringProducer_init_allocate()
         } else {
             __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
         }
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___StringProducer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func produce() -> Swift.String {
        if Self.self == main.StringProducer.self {
            return StringProducer_produce(self.__externalRCRef())
        } else {
            return StringProducer_produce_direct(self.__externalRCRef())
        }
    }
}
public final class TripleBox: main.Box<main.Box<main.Box<Swift.Int32>>> {
    public init() {
        let __kt = __root___TripleBox_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___TripleBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class _AnyConsumer<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, main.Consumer, main.__Consumer, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
    @inlinable
    public convenience init(
        _ projection: KotlinRuntimeSupport.KotlinContravariant<T>
    ) {
        self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@_documentation(visibility: internal)
open class _Bar_Base: KotlinRuntime.KotlinBase {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@_documentation(visibility: internal)
open class _Box_Base: KotlinRuntime.KotlinBase {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public var bar: main.Holder<Swift.String> {
    get {
        return main.Holder<Swift.String>.__fromKotlinPointer(__root___bar_get())
    }
}
public var baz: main.Holder<Swift.String> {
    get {
        return main.Holder<Swift.String>.__fromKotlinPointer(__root___baz_get())
    }
    set {
        return { __root___baz_set__TypesOfArguments__main_Holder_Swift_String___(newValue.__externalRCRef()); return () }()
    }
}
@inlinable
public func a<T: KotlinRuntimeSupport._KotlinBridgeable>() -> any main.A<T> {
    return { let _ptr = _cbridge___root___A(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any main.A<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return main.AnyA<T>.__createTypedWrapper(externalRCRef: _ptr) }()
}
@inlinable
public func bar<T: KotlinRuntimeSupport._KotlinBridgeable>(
    param1: T?,
    param2: T
) -> T {
    return T.__fromKotlinPointer(_cbridge___root___bar__TypesOfArguments__Swift_Optional_T__T__(param1.__retainedToKotlinPointer(), param2.__retainedToKotlinPointer()))
}
@inlinable
public func createMap<K: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable, V: KotlinRuntimeSupport._KotlinBridgeable>(
    pairs: [main.Pair<K,V>]
) -> [K: V] {
    return Swift.Dictionary<K,V>.__fromKotlinPointer(_cbridge___root___createMap__TypesOfArguments__Swift_Array_main_Pair_K_V____(pairs.__retainedToKotlinPointer()))
}
public func customFilter<T: KotlinRuntimeSupport._KotlinBridgeable>(
    _ receiver: [T],
    predicate: @escaping (T) -> Swift.Bool
) -> [T] {
    return Swift.Array<T>.__fromKotlinPointer(__root___customFilter__TypesOfArgumentsE__Swift_Array_T__U28TU29202D_U20Swift_Bool__(receiver.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool in return (predicate)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool) as AnyObject).toOpaque()))
}
@inlinable
public func foo<T: KotlinRuntimeSupport._KotlinBridgeable>(
    param1: T,
    param2: T?
) -> T {
    return T.__fromKotlinPointer(_cbridge___root___foo__TypesOfArguments__T_Swift_Optional_T___(param1.__retainedToKotlinPointer(), param2.__retainedToKotlinPointer()))
}
public func getExtProp(
    _ receiver: main.Holder<Swift.String>
) -> Swift.Int32 {
    return __root___extProp_get__TypesOfArgumentsE__main_Holder_Swift_String___(receiver.__externalRCRef())
}
public func produceBox(
    box: @escaping (main.Box<Swift.String>) -> Swift.Void
) -> Swift.Void {
    return { __root___produceBox__TypesOfArguments__U28main_Box_Swift_String_U29202D_U20Swift_Void__(Unmanaged.passRetained((box as (main.Box<Swift.String>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func produceBoxStar(
    box: @escaping (main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) -> Swift.Void
) -> Swift.Void {
    return { __root___produceBoxStar__TypesOfArguments__U28main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny__U29202D_U20Swift_Void__(Unmanaged.passRetained((box as (main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func produceBoxUpperBound(
    box: @escaping (main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) -> Swift.Void
) -> Swift.Void {
    return { __root___produceBoxUpperBound__TypesOfArguments__U28main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny__U29202D_U20Swift_Void__(Unmanaged.passRetained((box as (main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func produceCtxBox(
    box: @escaping (main.Box<Swift.String>) -> Swift.Void
) -> Swift.Void {
    return { __root___produceCtxBox__TypesOfArguments__U28main_Box_Swift_String_U29202D_U20Swift_Void__(Unmanaged.passRetained((box as (main.Box<Swift.String>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func produceExtBox(
    box: @escaping (main.Box<Swift.String>) -> Swift.Void
) -> Swift.Void {
    return { __root___produceExtBox__TypesOfArguments__U28main_Box_Swift_String_U29202D_U20Swift_Void__(Unmanaged.passRetained((box as (main.Box<Swift.String>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func returnBFun() -> main.BFun {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___returnBFun(), options: .asBestFittingWrapper)
        return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20main_B__(pointerToBlock.__externalRCRef(), _1.__externalRCRef()); return () }() }
    }()
}
public func returnBoxFun() -> main.BoxFun<Swift.String> {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___returnBoxFun(), options: .asBestFittingWrapper)
        return { return main.Box<Swift.String>.__fromKotlinPointer(main_internal_functional_type_caller_mainU2EBoxU3CSwiftU2EStringU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef())) }
    }()
}
public func returnBoxFunIn() -> main.BoxFunIn<Swift.String> {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___returnBoxFunIn(), options: .asBestFittingWrapper)
        return { _1 in return main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___(pointerToBlock.__externalRCRef(), _1.__externalRCRef()) }
    }()
}
@inlinable
public func returnGenericConstraintToGeneric<A: KotlinRuntimeSupport._KotlinBridgeable, B: KotlinRuntimeSupport._KotlinBridgeable>(
    arg1: A,
    arg2: B
) -> A {
    return A.__fromKotlinPointer(_cbridge___root___returnGenericConstraintToGeneric__TypesOfArguments__A_B__(arg1.__retainedToKotlinPointer(), arg2.__retainedToKotlinPointer()))
}
@inlinable
public func returnSomeBoxForArg<A: KotlinRuntimeSupport._KotlinBridgeable, B: main.Box<A>>(
    arg: A
) -> B {
    return B.__fromKotlinPointer(_cbridge___root___returnSomeBoxForArg__TypesOfArguments__A__(arg.__retainedToKotlinPointer()))
}
public func takeAndReturn(
    a: ExportedKotlinPackages.kotlin.Array<Swift.String>
) -> ExportedKotlinPackages.kotlin.Array<Swift.String> {
    return ExportedKotlinPackages.kotlin.Array<Swift.String>.__fromKotlinPointer(__root___takeAndReturn__TypesOfArguments__ExportedKotlinPackages_kotlin_Array_Swift_String___(a.__externalRCRef()))
}
public func takeBox(
    box: main.Box<Swift.String>
) -> Swift.Void {
    return { __root___takeBox__TypesOfArguments__main_Box_Swift_String___(box.__externalRCRef()); return () }()
}
public func takeBoxClosure(
    box: @escaping () -> main.Box<Swift.String>
) -> Swift.Void {
    return { __root___takeBoxClosure__TypesOfArguments__U2829202D_U20main_Box_Swift_String___(Unmanaged.passRetained((box as () -> main.Box<Swift.String>) as AnyObject).toOpaque()); return () }()
}
@inlinable
public func takeBoxStarProjection<_T0: KotlinRuntimeSupport._KotlinBridgeable>(
    box: main.Box<_T0>
) -> Swift.Void {
    return { _cbridge___root___takeBoxStarProjection__TypesOfArguments__main_Box__T0___(box.__externalRCRef()); return () }()
}
public func takeBoxUpperBound(
    box: main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>
) -> Swift.Void {
    return { __root___takeBoxUpperBound__TypesOfArguments__main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(box.__externalRCRef()); return () }()
}
public func takeBoxUpperBoundClosure(
    box: @escaping () -> main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>
) -> Swift.Void {
    return { __root___takeBoxUpperBoundClosure__TypesOfArguments__U2829202D_U20main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(Unmanaged.passRetained((box as () -> main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) as AnyObject).toOpaque()); return () }()
}
@_documentation(visibility: internal)
extension main.A where Self : main.__A {
    public var foo: T {
        get {
            return T.__fromKotlinPointer(A_foo_get(self.__externalRCRef()))
        }
    }
}
extension main.A {
}
@_documentation(visibility: internal)
extension main.AFactory where Self : main.__AFactory {
    public func create() -> T {
        return T.__fromKotlinPointer(AFactory_create(self.__externalRCRef()))
    }
}
extension main.AFactory {
}
extension Swift.Array where Element : KotlinRuntimeSupport._KotlinBridgeable {
    public func customFilter(
        predicate: @escaping (Element) -> Swift.Bool
    ) -> [Element] {
        let receiver = self
        return main.customFilter(receiver, predicate: predicate)
    }
}
@_documentation(visibility: internal)
extension main.B where Self : main.__B {
    public var foo: T {
        get {
            return T.__fromKotlinPointer(B_foo_get(self.__externalRCRef()))
        }
    }
}
extension main.B {
}
@_documentation(visibility: internal)
extension main.Consumer where Self : main.__Consumer {
    public func consume(
        item: T
    ) -> Swift.Void {
        return { Consumer_consume__TypesOfArguments__T__(self.__externalRCRef(), item.__retainedToKotlinPointer()); return () }()
    }
}
extension main.Consumer {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
}
@_documentation(visibility: internal)
extension main.ConsumerProducer where Self : main.__ConsumerProducer {
}
extension main.ConsumerProducer {
}
@_documentation(visibility: internal)
extension main.Foo where Self : main.__Foo {
}
extension main.Foo {
}
extension main.Holder<Swift.String> {
    public var extProp: Swift.Int32 {
        get {
            let receiver = self
            return main.getExtProp(receiver)
        }
    }
}
@_documentation(visibility: internal)
extension main.Processor where Self : main.__Processor {
    public func process(
        input: T
    ) -> R {
        return R.__fromKotlinPointer(Processor_process__TypesOfArguments__T__(self.__externalRCRef(), input.__retainedToKotlinPointer()))
    }
}
extension main.Processor {
}
@_documentation(visibility: internal)
extension main.Producer where Self : main.__Producer {
    public func produce() -> T {
        return T.__fromKotlinPointer(Producer_produce(self.__externalRCRef()))
    }
}
extension main.Producer {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: main.Foo, main.__Foo where Wrapped : main._Foo {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Producer {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Consumer {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Processor {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._ConsumerProducer {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._A {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._B {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._AFactory {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Foo {
}
@_cdecl("AFactory_create__reverse_swift")
package func AFactory_create__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any main.AFactory)
    func _open<__Self: main.AFactory>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.create()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("A_foo_get__reverse_swift")
package func A_foo_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any main.A)
    func _open<__Self: main.A>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.foo
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("B_foo_get__reverse_swift")
package func B_foo_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any main.B)
    func _open<__Self: main.B>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.foo
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("Consumer_consume__TypesOfArguments__T____reverse_swift")
package func Consumer_consume__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ item: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any main.Consumer)
    func _open<__Self: main.Consumer>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.consume(item: __Self.T.__fromKotlinPointer(item))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("Processor_process__TypesOfArguments__T____reverse_swift")
package func Processor_process__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ input: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any main.Processor)
    func _open<__Self: main.Processor>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.R = _self.process(input: __Self.T.__fromKotlinPointer(input))
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("Producer_produce__reverse_swift")
package func Producer_produce__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any main.Producer)
    func _open<__Self: main.Producer>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.produce()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("StringProducer_produce__reverse_swift")
package func StringProducer_produce__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = main.StringProducer.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.String = _self.produce()
    return _result
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func main_internal_functional_type_callee_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _result: Swift.Bool = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool)(_1)
    return _result
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) -> Swift.Void)(main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(_1))
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (main.Box<Swift.String>) -> Swift.Void)(main.Box<Swift.String>.__fromKotlinPointer(_1))
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_ctx1_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___")
package func main_internal_functional_type_callee_ctx1_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ ctx0: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (main.Box<Swift.String>) -> Swift.Void)((main.Box<Swift.String>.__fromKotlinPointer(ctx0)))
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_mainU2EBoxU3CSwiftU2EOptionalU3CKotlinRuntimeSupportU2EKotlinAnyU3E3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func main_internal_functional_type_callee_mainU2EBoxU3CSwiftU2EOptionalU3CKotlinRuntimeSupportU2EKotlinAnyU3E3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _result: main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> main.Box<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>)()
    return _result.__externalRCRef()
}

@_cdecl("main_internal_functional_type_callee_mainU2EBoxU3CSwiftU2EStringU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func main_internal_functional_type_callee_mainU2EBoxU3CSwiftU2EStringU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _result: main.Box<Swift.String> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> main.Box<Swift.String>)()
    return _result.__externalRCRef()
}

@usableFromInline
@discardableResult
internal func _cbridge_Box_t_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return Box_t_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_GenericWithComparableUpperBound_t_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return GenericWithComparableUpperBound_t_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_HolderConstrained_xs_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return HolderConstrained_xs_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_Holder_headOrNull(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return Holder_headOrNull(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_Holder_xs_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return Holder_xs_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_Pair_first_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return Pair_first_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_Pair_second_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return Pair_second_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge___root___A() -> Swift.UnsafeMutableRawPointer {
    return __root___A()
}

@usableFromInline
@discardableResult
internal func _cbridge___root___bar__TypesOfArguments__Swift_Optional_T__T__(_ _p0: Swift.UnsafeMutableRawPointer?, _ _p1: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    return __root___bar__TypesOfArguments__Swift_Optional_T__T__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge___root___createMap__TypesOfArguments__Swift_Array_main_Pair_K_V____(_ _p0: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    return __root___createMap__TypesOfArguments__Swift_Array_main_Pair_K_V____(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge___root___foo__TypesOfArguments__T_Swift_Optional_T___(_ _p0: Swift.UnsafeMutableRawPointer?, _ _p1: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    return __root___foo__TypesOfArguments__T_Swift_Optional_T___(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge___root___returnGenericConstraintToGeneric__TypesOfArguments__A_B__(_ _p0: Swift.UnsafeMutableRawPointer?, _ _p1: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    return __root___returnGenericConstraintToGeneric__TypesOfArguments__A_B__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge___root___returnSomeBoxForArg__TypesOfArguments__A__(_ _p0: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    return __root___returnSomeBoxForArg__TypesOfArguments__A__(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge___root___takeBoxStarProjection__TypesOfArguments__main_Box__T0___(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    return __root___takeBoxStarProjection__TypesOfArguments__main_Box__T0___(_p0)
}
