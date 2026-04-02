@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinCoroutineSupport
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
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterator where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterator {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext where Wrapped : ExportedKotlinPackages.kotlin.coroutines._CoroutineContext {
}
extension KotlinRuntimeSupport._KotlinExistential: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key where Wrapped : KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
extension KotlinRuntimeSupport._KotlinExistential: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element where Wrapped : KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
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
    public protocol Iterator: KotlinRuntime.KotlinBase {
        func hasNext() -> Swift.Bool
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    }
    @objc(_Iterator)
    package protocol _Iterator {
    }
}
extension ExportedKotlinPackages.kotlin.coroutines {
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
    @objc(_CoroutineContext)
    package protocol _CoroutineContext {
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
