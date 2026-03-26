@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

public protocol _ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
    var key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
        get
    }
    func fold(
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
}
public protocol _ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key: KotlinRuntime.KotlinBase {
}
@objc(__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)
package protocol __ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element: ExportedKotlinPackages.kotlin.coroutines._CoroutineContext {
}
@objc(__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key)
package protocol __ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
public final class _ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key: KotlinRuntime.KotlinBase {
    public static var shared: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key {
        get {
            return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key.__createClassWrapper(externalRCRef: kotlin_coroutines_ContinuationInterceptor_Key_get())
        }
    }
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
extension ExportedKotlinPackages.kotlin.collections.Collection where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Collection_size_get(self.__externalRCRef())
        }
    }
    public func contains(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Collection_isEmpty(self.__externalRCRef())
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Collection_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public static func ~=(
        this: Self,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
}
extension ExportedKotlinPackages.kotlin.collections.Collection {
}
extension ExportedKotlinPackages.kotlin.coroutines.Continuation where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_Continuation_context_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        }
    }
}
extension ExportedKotlinPackages.kotlin.coroutines.Continuation {
}
extension ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public func releaseInterceptedContinuation(
        continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
    ) -> Swift.Void {
        return { kotlin_coroutines_ContinuationInterceptor_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__(self.__externalRCRef(), continuation.__externalRCRef()); return () }()
    }
}
extension ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor {
    typealias Key = KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key
}
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public static func +(
        this: Self,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        this._plus(context: context)
    }
    public func _plus(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_plus__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(self.__externalRCRef(), context.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public func fold(
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_coroutines_CoroutineContext_fold__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
            let originalBlock = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer) in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }(), KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg1) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element).map { it in it.__externalRCRef() } ?? nil }
        }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
}
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
    typealias Element = KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
    typealias Key = KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
}
extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Iterable_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
}
extension ExportedKotlinPackages.kotlin.collections.Iterable {
}
extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_Iterator_next(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.Iterator {
}
extension ExportedKotlinPackages.kotlin.collections.MutableCollection where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func add(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableCollection_clear(self.__externalRCRef()); return () }()
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableCollection_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    public func remove(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableCollection {
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterable where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableIterable_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterable {
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterator where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func remove() -> Swift.Void {
        return { kotlin_collections_MutableIterator_remove(self.__externalRCRef()); return () }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterator {
}
extension ExportedKotlinPackages.kotlin.sequences.Sequence where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_sequences_Sequence_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
}
extension ExportedKotlinPackages.kotlin.sequences.Sequence {
}
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_Element_key_get(self.__externalRCRef())) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
        }
    }
    public func fold(
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_coroutines_CoroutineContext_Element_fold__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
            let originalBlock = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer) in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }(), KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg1) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element).map { it in it.__externalRCRef() } ?? nil }
        }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_Element_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
}
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
}
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableCollection where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableCollection {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.coroutines.Continuation where Wrapped : ExportedKotlinPackages.kotlin.coroutines._Continuation {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor where Wrapped : ExportedKotlinPackages.kotlin.coroutines._ContinuationInterceptor {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext where Wrapped : ExportedKotlinPackages.kotlin.coroutines._CoroutineContext {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.sequences.Sequence where Wrapped : ExportedKotlinPackages.kotlin.sequences._Sequence {
}
extension KotlinRuntimeSupport._KotlinExistential: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key where Wrapped : KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
extension KotlinRuntimeSupport._KotlinExistential: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element where Wrapped : KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Collection where Wrapped : ExportedKotlinPackages.kotlin.collections._Collection {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterable where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterable {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterator where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterator {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterator where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterator {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterable where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterable {
}
extension ExportedKotlinPackages.kotlin.coroutines.cancellation {
    open class CancellationException: ExportedKotlinPackages.kotlin.IllegalStateException {
        public override init() {
            if Self.self != ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException ") }
            let __kt = kotlin_coroutines_cancellation_CancellationException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException ") }
            let __kt = kotlin_coroutines_cancellation_CancellationException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException ") }
            let __kt = kotlin_coroutines_cancellation_CancellationException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException ") }
            let __kt = kotlin_coroutines_cancellation_CancellationException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
    }
}
extension ExportedKotlinPackages.kotlin.collections {
    public protocol Collection: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable {
        var size: Swift.Int32 {
            get
        }
        func contains(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public protocol Iterable: KotlinRuntime.KotlinBase {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public protocol Iterator: KotlinRuntime.KotlinBase {
        func hasNext() -> Swift.Bool
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    }
    public protocol MutableCollection: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.MutableIterable {
        func add(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func clear() -> Swift.Void
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator
        func remove(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
    }
    public protocol MutableIterable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    public protocol MutableIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator {
        func remove() -> Swift.Void
    }
    @objc(_Collection)
    package protocol _Collection: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    @objc(_Iterable)
    package protocol _Iterable {
    }
    @objc(_Iterator)
    package protocol _Iterator {
    }
    @objc(_MutableCollection)
    package protocol _MutableCollection: ExportedKotlinPackages.kotlin.collections._Collection, ExportedKotlinPackages.kotlin.collections._MutableIterable {
    }
    @objc(_MutableIterable)
    package protocol _MutableIterable: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    @objc(_MutableIterator)
    package protocol _MutableIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
    public final class IndexedValue: KotlinRuntime.KotlinBase {
        public var index: Swift.Int32 {
            get {
                return kotlin_collections_IndexedValue_index_get(self.__externalRCRef())
            }
        }
        public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            get {
                return { switch kotlin_collections_IndexedValue_value_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            index: Swift.Int32,
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.collections.IndexedValue.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.collections.IndexedValue ") }
            let __kt = kotlin_collections_IndexedValue_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_IndexedValue_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt, index, value.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.collections.IndexedValue,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func copy(
            index: Swift.Int32,
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> ExportedKotlinPackages.kotlin.collections.IndexedValue {
            return ExportedKotlinPackages.kotlin.collections.IndexedValue.__createClassWrapper(externalRCRef: kotlin_collections_IndexedValue_copy__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, value.map { it in it.__externalRCRef() } ?? nil))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_collections_IndexedValue_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_collections_IndexedValue_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_collections_IndexedValue_toString(self.__externalRCRef())
        }
    }
    open class IntIterator: KotlinRuntime.KotlinBase {
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public final func next() -> Swift.Int32 {
            return kotlin_collections_IntIterator_next(self.__externalRCRef())
        }
        open func nextInt() -> Swift.Int32 {
            return kotlin_collections_IntIterator_nextInt(self.__externalRCRef())
        }
    }
    open class LongIterator: KotlinRuntime.KotlinBase {
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public final func next() -> Swift.Int64 {
            return kotlin_collections_LongIterator_next(self.__externalRCRef())
        }
        open func nextLong() -> Swift.Int64 {
            return kotlin_collections_LongIterator_nextLong(self.__externalRCRef())
        }
    }
}
extension ExportedKotlinPackages.kotlin.coroutines {
    public protocol Continuation: KotlinRuntime.KotlinBase {
        var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            get
        }
    }
    public protocol ContinuationInterceptor: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
        func minusKey(
            key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
        ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        func releaseInterceptedContinuation(
            continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
        ) -> Swift.Void
    }
    public protocol CoroutineContext: KotlinRuntime.KotlinBase {
        func _plus(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        func fold(
            initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func minusKey(
            key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
        ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    @objc(_Continuation)
    package protocol _Continuation {
    }
    @objc(_ContinuationInterceptor)
    package protocol _ContinuationInterceptor: KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    }
    @objc(_CoroutineContext)
    package protocol _CoroutineContext {
    }
    open class AbstractCoroutineContextElement: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element, KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
        open var key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_AbstractCoroutineContextElement_key_get(self.__externalRCRef())) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
            }
        }
        package init(
            key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, deprecated, message: "Polymorphic coroutine context keys are error-prone, difficult to implement correctly, and can encourage depending on implementation details. Prefer retrieving the element by its base key and casting it explicitly when needed or introducing a dedicated extension property.") @_spi(kotlin$ExperimentalStdlibApi)
    open class AbstractCoroutineContextKey: KotlinRuntime.KotlinBase {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlin$ExperimentalStdlibApi)
        package init(
            baseKey: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key,
            safeCast: @escaping (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)?
        ) {
            fatalError()
        }
    }
}
extension ExportedKotlinPackages.kotlin {
    public final class Array: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_Array_size_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) {
            fatalError()
        }
        public func _get(
            index: Swift.Int32
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_Array_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func _set(
            index: Swift.Int32,
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void {
            return { kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, value.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_Array_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
        }
        public subscript(
            index: Swift.Int32
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    open class Exception: ExportedKotlinPackages.kotlin.Throwable {
        public override init() {
            if Self.self != ExportedKotlinPackages.kotlin.Exception.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ") }
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Exception.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ") }
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Exception.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ") }
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Exception.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ") }
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
    }
    open class IllegalStateException: ExportedKotlinPackages.kotlin.RuntimeException {
        public override init() {
            if Self.self != ExportedKotlinPackages.kotlin.IllegalStateException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalStateException ") }
            let __kt = kotlin_IllegalStateException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.IllegalStateException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalStateException ") }
            let __kt = kotlin_IllegalStateException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.IllegalStateException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalStateException ") }
            let __kt = kotlin_IllegalStateException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.IllegalStateException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalStateException ") }
            let __kt = kotlin_IllegalStateException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
    }
    public final class IntArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_IntArray_size_get(self.__externalRCRef())
            }
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int32
        ) {
            fatalError()
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_IntArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Int32
        ) -> Swift.Void {
            return { kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.IntIterator {
            return ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: kotlin_IntArray_iterator(self.__externalRCRef()))
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Int32 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class LongArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_LongArray_size_get(self.__externalRCRef())
            }
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int64
        ) {
            fatalError()
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_LongArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Int64
        ) -> Swift.Void {
            return { kotlin_LongArray_set__TypesOfArguments__Swift_Int32_Swift_Int64__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.LongIterator {
            return ExportedKotlinPackages.kotlin.collections.LongIterator.__createClassWrapper(externalRCRef: kotlin_LongArray_iterator(self.__externalRCRef()))
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Int64 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    open class NoSuchElementException: ExportedKotlinPackages.kotlin.RuntimeException {
        public override init() {
            if Self.self != ExportedKotlinPackages.kotlin.NoSuchElementException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.NoSuchElementException ") }
            let __kt = kotlin_NoSuchElementException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_NoSuchElementException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.NoSuchElementException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.NoSuchElementException ") }
            let __kt = kotlin_NoSuchElementException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_NoSuchElementException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class RuntimeException: ExportedKotlinPackages.kotlin.Exception {
        public override init() {
            if Self.self != ExportedKotlinPackages.kotlin.RuntimeException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ") }
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.RuntimeException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ") }
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.RuntimeException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ") }
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.RuntimeException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ") }
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
    }
    open class Throwable: KotlinRuntime.KotlinBase {
        open var cause: ExportedKotlinPackages.kotlin.Throwable? {
            get {
                return { switch kotlin_Throwable_cause_get(self.__externalRCRef()) { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
            }
        }
        open var message: Swift.String? {
            get {
                return kotlin_Throwable_message_get(self.__externalRCRef())
            }
        }
        public init() {
            if Self.self != ExportedKotlinPackages.kotlin.Throwable.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ") }
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Throwable.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ") }
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Throwable.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ") }
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Throwable.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ") }
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        @_spi(kotlin$experimental$ExperimentalNativeApi)
        public final func getStackTrace() -> ExportedKotlinPackages.kotlin.Array {
            return ExportedKotlinPackages.kotlin.Array.__createClassWrapper(externalRCRef: kotlin_Throwable_getStackTrace(self.__externalRCRef()))
        }
        public final func printStackTrace() -> Swift.Void {
            return { kotlin_Throwable_printStackTrace(self.__externalRCRef()); return () }()
        }
        open func toString() -> Swift.String {
            return kotlin_Throwable_toString(self.__externalRCRef())
        }
    }
}
extension ExportedKotlinPackages.kotlin.sequences {
    public protocol Sequence: KotlinRuntime.KotlinBase {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    @objc(_Sequence)
    package protocol _Sequence {
    }
}
extension ExportedKotlinPackages.kotlin.time {
    public enum DurationUnit: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
        case NANOSECONDS
        case MICROSECONDS
        case MILLISECONDS
        case SECONDS
        case MINUTES
        case HOURS
        case DAYS
        public var description: Swift.String {
            get {
                switch self {
                case .NANOSECONDS: "NANOSECONDS"
                case .MICROSECONDS: "MICROSECONDS"
                case .MILLISECONDS: "MILLISECONDS"
                case .SECONDS: "SECONDS"
                case .MINUTES: "MINUTES"
                case .HOURS: "HOURS"
                case .DAYS: "DAYS"
                default: fatalError()
                }
            }
        }
        public var rawValue: Swift.Int32 {
            get {
                switch self {
                case .NANOSECONDS: 0
                case .MICROSECONDS: 1
                case .MILLISECONDS: 2
                case .SECONDS: 3
                case .MINUTES: 4
                case .HOURS: 5
                case .DAYS: 6
                default: fatalError()
                }
            }
        }
        public init?(
            _ description: Swift.String
        ) {
            switch description {
            case "NANOSECONDS": self = .NANOSECONDS
            case "MICROSECONDS": self = .MICROSECONDS
            case "MILLISECONDS": self = .MILLISECONDS
            case "SECONDS": self = .SECONDS
            case "MINUTES": self = .MINUTES
            case "HOURS": self = .HOURS
            case "DAYS": self = .DAYS
            default: return nil
            }
        }
        public init?(
            rawValue: Swift.Int32
        ) {
            guard 0..<7 ~= rawValue else { return nil }
            self = DurationUnit.allCases[Int(rawValue)]
        }
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            switch kotlin_time_DurationUnit_ordinal(__externalRCRefUnsafe) {
            case 0: self = .NANOSECONDS
            case 1: self = .MICROSECONDS
            case 2: self = .MILLISECONDS
            case 3: self = .SECONDS
            case 4: self = .MINUTES
            case 5: self = .HOURS
            case 6: self = .DAYS
            default: fatalError()
            }
        }
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
            return switch self {
            case .NANOSECONDS: kotlin_time_DurationUnit_NANOSECONDS()
            case .MICROSECONDS: kotlin_time_DurationUnit_MICROSECONDS()
            case .MILLISECONDS: kotlin_time_DurationUnit_MILLISECONDS()
            case .SECONDS: kotlin_time_DurationUnit_SECONDS()
            case .MINUTES: kotlin_time_DurationUnit_MINUTES()
            case .HOURS: kotlin_time_DurationUnit_HOURS()
            case .DAYS: kotlin_time_DurationUnit_DAYS()
            default: fatalError()
            }
        }
    }
    public final class Duration: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public var INFINITE: ExportedKotlinPackages.kotlin.time.Duration {
                get {
                    return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_INFINITE_get(self.__externalRCRef()))
                }
            }
            public var ZERO: ExportedKotlinPackages.kotlin.time.Duration {
                get {
                    return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_ZERO_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.time.Duration.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.time.Duration.Companion.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            @_spi(kotlin$time$ExperimentalTime)
            public func convert(
                value: Swift.Double,
                sourceUnit: ExportedKotlinPackages.kotlin.time.DurationUnit,
                targetUnit: ExportedKotlinPackages.kotlin.time.DurationUnit
            ) -> Swift.Double {
                return kotlin_time_Duration_Companion_convert__TypesOfArguments__Swift_Double_ExportedKotlinPackages_kotlin_time_DurationUnit_ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), value, sourceUnit.__externalRCRef(), targetUnit.__externalRCRef())
            }
            public func getDays(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getDays(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getDays(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getHours(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getHours(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getHours(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getMicroseconds(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getMicroseconds(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getMicroseconds(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getMilliseconds(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getMilliseconds(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getMilliseconds(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getMinutes(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getMinutes(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getMinutes(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getNanoseconds(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getNanoseconds(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getNanoseconds(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getSeconds(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getSeconds(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getSeconds(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func parse(
                value: Swift.String
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_parse__TypesOfArguments__Swift_String__(self.__externalRCRef(), value))
            }
            public func parseIsoString(
                value: Swift.String
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_parseIsoString__TypesOfArguments__Swift_String__(self.__externalRCRef(), value))
            }
            public func parseIsoStringOrNull(
                value: Swift.String
            ) -> ExportedKotlinPackages.kotlin.time.Duration? {
                return { switch kotlin_time_Duration_Companion_parseIsoStringOrNull__TypesOfArguments__Swift_String__(self.__externalRCRef(), value) { case nil: .none; case let res: ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: res); } }()
            }
            public func parseOrNull(
                value: Swift.String
            ) -> ExportedKotlinPackages.kotlin.time.Duration? {
                return { switch kotlin_time_Duration_Companion_parseOrNull__TypesOfArguments__Swift_String__(self.__externalRCRef(), value) { case nil: .none; case let res: ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: res); } }()
            }
        }
        public var absoluteValue: ExportedKotlinPackages.kotlin.time.Duration {
            get {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_absoluteValue_get(self.__externalRCRef()))
            }
        }
        public var inWholeDays: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeDays_get(self.__externalRCRef())
            }
        }
        public var inWholeHours: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeHours_get(self.__externalRCRef())
            }
        }
        public var inWholeMicroseconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeMicroseconds_get(self.__externalRCRef())
            }
        }
        public var inWholeMilliseconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeMilliseconds_get(self.__externalRCRef())
            }
        }
        public var inWholeMinutes: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeMinutes_get(self.__externalRCRef())
            }
        }
        public var inWholeNanoseconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeNanoseconds_get(self.__externalRCRef())
            }
        }
        public var inWholeSeconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeSeconds_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._times(scale: scale)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._times(scale: scale)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._plus(other: other)
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._unaryMinus()
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._minus(other: other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Double {
            this._div(other: other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._div(scale: scale)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._div(scale: scale)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Int32 {
            return kotlin_time_Duration_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef())
        }
        public func _div(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Double {
            return kotlin_time_Duration_div__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef())
        }
        public func _div(
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), scale))
        }
        public func _div(
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), scale))
        }
        public func _minus(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_minus__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func _plus(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_plus__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func _times(
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), scale))
        }
        public func _times(
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), scale))
        }
        public func _unaryMinus() -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_unaryMinus(self.__externalRCRef()))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_time_Duration_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_time_Duration_hashCode(self.__externalRCRef())
        }
        public func isFinite() -> Swift.Bool {
            return kotlin_time_Duration_isFinite(self.__externalRCRef())
        }
        public func isInfinite() -> Swift.Bool {
            return kotlin_time_Duration_isInfinite(self.__externalRCRef())
        }
        public func isNegative() -> Swift.Bool {
            return kotlin_time_Duration_isNegative(self.__externalRCRef())
        }
        public func isPositive() -> Swift.Bool {
            return kotlin_time_Duration_isPositive(self.__externalRCRef())
        }
        public func toComponents(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32, Swift.Int32) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), {
                let originalBlock = action
                return { (arg0: Swift.Int64, arg1: Swift.Int32, arg2: Swift.Int32, arg3: Swift.Int32, arg4: Swift.Int32) in return originalBlock(arg0, arg1, arg2, arg3, arg4).map { it in it.__externalRCRef() } ?? nil }
            }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func toComponents(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), {
                let originalBlock = action
                return { (arg0: Swift.Int64, arg1: Swift.Int32, arg2: Swift.Int32, arg3: Swift.Int32) in return originalBlock(arg0, arg1, arg2, arg3).map { it in it.__externalRCRef() } ?? nil }
            }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func toComponents(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), {
                let originalBlock = action
                return { (arg0: Swift.Int64, arg1: Swift.Int32, arg2: Swift.Int32) in return originalBlock(arg0, arg1, arg2).map { it in it.__externalRCRef() } ?? nil }
            }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func toComponents(
            action: @escaping (Swift.Int64, Swift.Int32) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), {
                let originalBlock = action
                return { (arg0: Swift.Int64, arg1: Swift.Int32) in return originalBlock(arg0, arg1).map { it in it.__externalRCRef() } ?? nil }
            }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func toDouble(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit
        ) -> Swift.Double {
            return kotlin_time_Duration_toDouble__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), unit.__externalRCRef())
        }
        public func toInt(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit
        ) -> Swift.Int32 {
            return kotlin_time_Duration_toInt__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), unit.__externalRCRef())
        }
        public func toIsoString() -> Swift.String {
            return kotlin_time_Duration_toIsoString(self.__externalRCRef())
        }
        public func toLong(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit
        ) -> Swift.Int64 {
            return kotlin_time_Duration_toLong__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), unit.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_time_Duration_toString(self.__externalRCRef())
        }
        public func toString(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit,
            decimals: Swift.Int32
        ) -> Swift.String {
            return kotlin_time_Duration_toString__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit_Swift_Int32__(self.__externalRCRef(), unit.__externalRCRef(), decimals)
        }
    }
}
