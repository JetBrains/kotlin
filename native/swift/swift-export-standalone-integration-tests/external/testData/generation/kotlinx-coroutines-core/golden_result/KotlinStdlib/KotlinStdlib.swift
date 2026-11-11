import KotlinRuntime
@_exported import ExportedKotlinPackages
import KotlinRuntimeSupport
@_exported import KotlinCoroutineSupport
@_implementationOnly import KotlinBridges_KotlinStdlib

public protocol _ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>: KotlinRuntime.KotlinBase, KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
    associatedtype E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
}
@objc(_KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key)
public protocol __ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
public protocol ___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol _ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    var key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
        get
    }
    func _get<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
    ) -> E?
    func fold<R: KotlinRuntimeSupport._KotlinBridgeable>(
        initial: R,
        operation: @escaping (R, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> R
    ) -> R
    func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
}
@objc(_KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)
public protocol __ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element: ExportedKotlinPackages.kotlin.coroutines._CoroutineContext {
}
public protocol ___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.coroutines.__CoroutineContext {
}
public final class _ExportedKotlinPackages_kotlin_Result_Companion: KotlinRuntime.KotlinBase {
    public static var shared: KotlinStdlib._ExportedKotlinPackages_kotlin_Result_Companion {
        get {
            return KotlinStdlib._ExportedKotlinPackages_kotlin_Result_Companion.__createClassWrapper(externalRCRef: kotlin_Result_Companion_get())
        }
    }
    @inlinable
    public func success<T: KotlinRuntimeSupport._KotlinBridgeable>(
        value: T
    ) -> ExportedKotlinPackages.kotlin.Result<T> {
        return ExportedKotlinPackages.kotlin.Result<T>.__fromKotlinPointer(_cbridge_kotlin_Result_Companion_success__TypesOfArguments__T__(self.__externalRCRef(), value.__retainedToKotlinPointer()))
    }
    @inlinable
    public func failure<T: KotlinRuntimeSupport._KotlinBridgeable>(
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> ExportedKotlinPackages.kotlin.Result<T> {
        return ExportedKotlinPackages.kotlin.Result<T>.__fromKotlinPointer(_cbridge_kotlin_Result_Companion_failure__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), exception.__externalRCRef()))
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private override init() {
        fatalError()
    }
}
public final class _ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
    public typealias E = KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlin.coroutines._ContinuationInterceptor>
    public static var shared: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key {
        get {
            return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key.__createClassWrapper(externalRCRef: kotlin_coroutines_ContinuationInterceptor_Key_get())
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private override init() {
        fatalError()
    }
}
public final class _ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
extension ExportedKotlinPackages.kotlin {
    public protocol Comparable<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin._Comparable {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func _compareTo(
            other: T
        ) -> Swift.Int32
    }
    @objc(_ExportedKotlinPackages_kotlin_Comparable)
    public protocol _Comparable {
    }
    public protocol __Comparable: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class Array<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var size: Swift.Int32 {
            get {
                return _cbridge_kotlin_Array_size_get(self.__externalRCRef())
            }
        }
        @inlinable
        public func _get(
            index: Swift.Int32
        ) -> T {
            return T.__fromKotlinPointer(_cbridge_kotlin_Array_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index))
        }
        @inlinable
        public func _set(
            index: Swift.Int32,
            value: T
        ) -> Swift.Void {
            return { _cbridge_kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(self.__externalRCRef(), index, value.__retainedToKotlinPointer()); return () }()
        }
        @inlinable
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T> {
            return { let _ptr = _cbridge_kotlin_Array_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> T
        ) {
            fatalError()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public subscript(
            index: Swift.Int32
        ) -> T {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class IntArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_IntArray_size_get(self.__externalRCRef())
            }
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
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int64
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
    open class Throwable: KotlinRuntime.KotlinBase, Swift.Error {
        open var message: Swift.String? {
            get {
                if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                    return kotlin_Throwable_message_get(self.__externalRCRef())
                } else {
                    return kotlin_Throwable_message_get_direct(self.__externalRCRef())
                }
            }
        }
        open var cause: ExportedKotlinPackages.kotlin.Throwable? {
            get {
                if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                    return { switch kotlin_Throwable_cause_get(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                } else {
                    return { switch kotlin_Throwable_cause_get_direct(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                }
            }
        }
        @_spi(kotlin$experimental$ExperimentalNativeApi)
        public final func getStackTrace() -> ExportedKotlinPackages.kotlin.Array<Swift.String> {
            return ExportedKotlinPackages.kotlin.Array<Swift.String>.__fromKotlinPointer(kotlin_Throwable_getStackTrace(self.__externalRCRef()))
        }
        public final func printStackTrace() -> Swift.Void {
            return { kotlin_Throwable_printStackTrace(self.__externalRCRef()); return () }()
        }
        public init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                 __kt = kotlin_Throwable_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                 __kt = kotlin_Throwable_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        public init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                 __kt = kotlin_Throwable_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                 __kt = kotlin_Throwable_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class Exception: ExportedKotlinPackages.kotlin.Throwable {
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Exception.self {
                 __kt = kotlin_Exception_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Exception.self {
                 __kt = kotlin_Exception_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Exception.self {
                 __kt = kotlin_Exception_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Exception.self {
                 __kt = kotlin_Exception_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class RuntimeException: ExportedKotlinPackages.kotlin.Exception {
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self {
                 __kt = kotlin_RuntimeException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self {
                 __kt = kotlin_RuntimeException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self {
                 __kt = kotlin_RuntimeException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self {
                 __kt = kotlin_RuntimeException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class IllegalStateException: ExportedKotlinPackages.kotlin.RuntimeException {
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.IllegalStateException.self {
                 __kt = kotlin_IllegalStateException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.IllegalStateException.self {
                 __kt = kotlin_IllegalStateException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.IllegalStateException.self {
                 __kt = kotlin_IllegalStateException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.IllegalStateException.self {
                 __kt = kotlin_IllegalStateException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class NoSuchElementException: ExportedKotlinPackages.kotlin.RuntimeException {
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.NoSuchElementException.self {
                 __kt = kotlin_NoSuchElementException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_NoSuchElementException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.NoSuchElementException.self {
                 __kt = kotlin_NoSuchElementException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_NoSuchElementException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Result<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias Companion = KotlinStdlib._ExportedKotlinPackages_kotlin_Result_Companion
        @inlinable
        public var isSuccess: Swift.Bool {
            get {
                return _cbridge_kotlin_Result_isSuccess_get(self.__externalRCRef())
            }
        }
        @inlinable
        public var isFailure: Swift.Bool {
            get {
                return _cbridge_kotlin_Result_isFailure_get(self.__externalRCRef())
            }
        }
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @inlinable
        public func getOrNull() -> T? {
            return Swift.Optional<T>.__fromKotlinPointer(_cbridge_kotlin_Result_getOrNull(self.__externalRCRef()))
        }
        @inlinable
        public func exceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? {
            return { switch _cbridge_kotlin_Result_exceptionOrNull(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyComparable<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
}
extension ExportedKotlinPackages.kotlin.collections {
    public protocol Collection<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections._Collection where Self.T == E {
        associatedtype E: KotlinRuntimeSupport._KotlinBridgeable
        var size: Swift.Int32 {
            get
        }
        func isEmpty() -> Swift.Bool
        func contains(
            element: E
        ) -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E>
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Collection)
    public protocol _Collection: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    public protocol __Collection: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterable {
    }
    public protocol MutableCollection<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.MutableIterable, ExportedKotlinPackages.kotlin.collections._MutableCollection where Self.T == E {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<E>
        @discardableResult
        func add(
            element: E
        ) -> Swift.Bool
        @discardableResult
        func remove(
            element: E
        ) -> Swift.Bool
        @discardableResult
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func clear() -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableCollection)
    public protocol _MutableCollection: ExportedKotlinPackages.kotlin.collections._Collection, ExportedKotlinPackages.kotlin.collections._MutableIterable {
    }
    public protocol __MutableCollection: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection, ExportedKotlinPackages.kotlin.collections.__MutableIterable {
    }
    public protocol Iterable<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterable {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T>
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Iterable)
    public protocol _Iterable {
    }
    public protocol __Iterable: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol MutableIterable<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections._MutableIterable {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<T>
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableIterable)
    public protocol _MutableIterable: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    public protocol __MutableIterable: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterable {
    }
    public protocol Iterator<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterator {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func next() -> T
        func hasNext() -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Iterator)
    public protocol _Iterator {
    }
    public protocol __Iterator: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol List<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections._List {
        var size: Swift.Int32 {
            get
        }
        func isEmpty() -> Swift.Bool
        func contains(
            element: E
        ) -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E>
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func _get(
            index: Swift.Int32
        ) -> E
        func indexOf(
            element: E
        ) -> Swift.Int32
        func lastIndexOf(
            element: E
        ) -> Swift.Int32
        func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator<E>
        func listIterator(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator<E>
        func subList(
            fromIndex: Swift.Int32,
            toIndex: Swift.Int32
        ) -> [E]
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_List)
    public protocol _List: ExportedKotlinPackages.kotlin.collections._Collection {
    }
    public protocol __List: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection {
    }
    public protocol MutableList<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections._MutableList {
        @discardableResult
        func add(
            element: E
        ) -> Swift.Bool
        func add(
            index: Swift.Int32,
            element: E
        ) -> Swift.Void
        @discardableResult
        func remove(
            element: E
        ) -> Swift.Bool
        @discardableResult
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func addAll(
            index: Swift.Int32,
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func clear() -> Swift.Void
        @discardableResult
        func _set(
            index: Swift.Int32,
            element: E
        ) -> E
        @discardableResult
        func removeAt(
            index: Swift.Int32
        ) -> E
        func listIterator() -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E>
        func listIterator(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E>
        func subList(
            fromIndex: Swift.Int32,
            toIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.MutableList<E>
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableList)
    public protocol _MutableList: ExportedKotlinPackages.kotlin.collections._List, ExportedKotlinPackages.kotlin.collections._MutableCollection {
    }
    public protocol __MutableList: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__List, ExportedKotlinPackages.kotlin.collections.__MutableCollection {
    }
    public protocol Set<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections._Set {
        var size: Swift.Int32 {
            get
        }
        func isEmpty() -> Swift.Bool
        func contains(
            element: E
        ) -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E>
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Set)
    public protocol _Set: ExportedKotlinPackages.kotlin.collections._Collection {
    }
    public protocol __Set: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection {
    }
    public protocol MutableSet<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Set, ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections._MutableSet where Self.E : Swift.Hashable {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<E>
        @discardableResult
        func add(
            element: E
        ) -> Swift.Bool
        @discardableResult
        func remove(
            element: E
        ) -> Swift.Bool
        @discardableResult
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func clear() -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableSet)
    public protocol _MutableSet: ExportedKotlinPackages.kotlin.collections._Set, ExportedKotlinPackages.kotlin.collections._MutableCollection {
    }
    public protocol __MutableSet: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Set, ExportedKotlinPackages.kotlin.collections.__MutableCollection {
    }
    public protocol MutableIterator<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections._MutableIterator {
        func remove() -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableIterator)
    public protocol _MutableIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
    public protocol __MutableIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterator {
    }
    public protocol ListIterator<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections._ListIterator {
        func next() -> T
        func hasNext() -> Swift.Bool
        func hasPrevious() -> Swift.Bool
        func previous() -> T
        func nextIndex() -> Swift.Int32
        func previousIndex() -> Swift.Int32
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_ListIterator)
    public protocol _ListIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
    public protocol __ListIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterator {
    }
    public protocol MutableListIterator<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.ListIterator, ExportedKotlinPackages.kotlin.collections.MutableIterator, ExportedKotlinPackages.kotlin.collections._MutableListIterator {
        func next() -> T
        func hasNext() -> Swift.Bool
        func remove() -> Swift.Void
        func set(
            element: T
        ) -> Swift.Void
        func add(
            element: T
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableListIterator)
    public protocol _MutableListIterator: ExportedKotlinPackages.kotlin.collections._ListIterator, ExportedKotlinPackages.kotlin.collections._MutableIterator {
    }
    public protocol __MutableListIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__ListIterator, ExportedKotlinPackages.kotlin.collections.__MutableIterator {
    }
    public final class AnyCollection<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.__Collection, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<E>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyMutableCollection<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections.__MutableCollection, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyIterable<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections.__Iterable, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyMutableIterable<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableIterable, ExportedKotlinPackages.kotlin.collections.__MutableIterable, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyIterator<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyList<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.kotlin.collections.__List, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<E>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyMutableList<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableList, ExportedKotlinPackages.kotlin.collections.__MutableList, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnySet<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Set, ExportedKotlinPackages.kotlin.collections.__Set, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<E>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyMutableSet<E: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableSet, ExportedKotlinPackages.kotlin.collections.__MutableSet, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class IndexedValue<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var index: Swift.Int32 {
            get {
                return _cbridge_kotlin_collections_IndexedValue_index_get(self.__externalRCRef())
            }
        }
        @inlinable
        public var value: T {
            get {
                return T.__fromKotlinPointer(_cbridge_kotlin_collections_IndexedValue_value_get(self.__externalRCRef()))
            }
        }
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @inlinable
        public func copy(
            index: Swift.Int32,
            value: T
        ) -> ExportedKotlinPackages.kotlin.collections.IndexedValue<T> {
            return ExportedKotlinPackages.kotlin.collections.IndexedValue<T>.__fromKotlinPointer(_cbridge_kotlin_collections_IndexedValue_copy__TypesOfArguments__Swift_Int32_T__(self.__externalRCRef(), index, value.__retainedToKotlinPointer()))
        }
        public init(
            index: Swift.Int32,
            value: T
        ) {
            let __kt = kotlin_collections_IndexedValue_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
            { kotlin_collections_IndexedValue_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_T__(__kt, index, value.__retainedToKotlinPointer()); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyMutableIterator<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableIterator, ExportedKotlinPackages.kotlin.collections.__MutableIterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyListIterator<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.ListIterator, ExportedKotlinPackages.kotlin.collections.__ListIterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyMutableListIterator<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableListIterator, ExportedKotlinPackages.kotlin.collections.__MutableListIterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class IntIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Int32
        public final func next() -> Swift.Int32 {
            return kotlin_collections_IntIterator_next(self.__externalRCRef())
        }
        open func nextInt() -> Swift.Int32 {
            if Self.self == ExportedKotlinPackages.kotlin.collections.IntIterator.self {
                return kotlin_collections_IntIterator_nextInt(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.IntIterator.nextInt': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.IntIterator.self, "ExportedKotlinPackages.kotlin.collections.IntIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_IntIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class LongIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Int64
        public final func next() -> Swift.Int64 {
            return kotlin_collections_LongIterator_next(self.__externalRCRef())
        }
        open func nextLong() -> Swift.Int64 {
            if Self.self == ExportedKotlinPackages.kotlin.collections.LongIterator.self {
                return kotlin_collections_LongIterator_nextLong(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.LongIterator.nextLong': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.LongIterator.self, "ExportedKotlinPackages.kotlin.collections.LongIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_LongIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlin.coroutines {
    public protocol Continuation<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.coroutines._Continuation {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            get
        }
        func resumeWith(
            result: ExportedKotlinPackages.kotlin.Result<T>
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlin_coroutines_Continuation)
    public protocol _Continuation {
    }
    public protocol __Continuation: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol ContinuationInterceptor: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element, ExportedKotlinPackages.kotlin.coroutines._ContinuationInterceptor {
        func interceptContinuation<T: KotlinRuntimeSupport._KotlinBridgeable>(
            continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation<T>
        ) -> any ExportedKotlinPackages.kotlin.coroutines.Continuation<T>
        func releaseInterceptedContinuation(
            continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
        ) -> Swift.Void
        func _get<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
            key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
        ) -> E?
        func minusKey(
            key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
        ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    @objc(_ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor)
    public protocol _ContinuationInterceptor: KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    }
    public protocol __ContinuationInterceptor: KotlinRuntimeSupport._KotlinBridgeable, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    }
    public protocol CoroutineContext: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.coroutines._CoroutineContext {
        func _get<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
            key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
        ) -> E?
        func fold<R: KotlinRuntimeSupport._KotlinBridgeable>(
            initial: R,
            operation: @escaping (R, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> R
        ) -> R
        func _plus(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        func minusKey(
            key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
        ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    @objc(_ExportedKotlinPackages_kotlin_coroutines_CoroutineContext)
    public protocol _CoroutineContext {
    }
    public protocol __CoroutineContext: KotlinRuntimeSupport._KotlinBridgeable {
    }
    @available(*, deprecated, message: "Polymorphic coroutine context keys are error-prone, difficult to implement correctly, and can encourage users to depend on implementation details. Prefer retrieving the element by its base key and casting it explicitly when needed or introducing a dedicated extension property.") @_spi(kotlin$ExperimentalStdlibApi) @_documentation(visibility: internal)
    public protocol _AbstractCoroutineContextKey_ReverseDispatch<B, E>: KotlinRuntime.KotlinBase {
        associatedtype B: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
        associatedtype E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
    }
    public final class AnyContinuation<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.coroutines.Continuation, ExportedKotlinPackages.kotlin.coroutines.__Continuation, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    open class AbstractCoroutineContextElement: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
        open var key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
            get {
                if Self.self == ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement.self {
                    return { let _ptr = kotlin_coroutines_AbstractCoroutineContextElement_key_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
                } else {
                    return { let _ptr = kotlin_coroutines_AbstractCoroutineContextElement_key_get_direct(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
                }
            }
        }
        public init(
            key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
        ) {
            precondition(Self.self != ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement.self, "ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_AbstractCoroutineContextElement_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(__kt, key.__externalRCRef()); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, deprecated, message: "Polymorphic coroutine context keys are error-prone, difficult to implement correctly, and can encourage users to depend on implementation details. Prefer retrieving the element by its base key and casting it explicitly when needed or introducing a dedicated extension property.") @_spi(kotlin$ExperimentalStdlibApi)
    open class AbstractCoroutineContextKey<B: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element, E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>: ExportedKotlinPackages.kotlin.coroutines._AbstractCoroutineContextKey_Base, KotlinRuntimeSupport._KotlinOpenGenericBridgeable, ExportedKotlinPackages.kotlin.coroutines._AbstractCoroutineContextKey_ReverseDispatch, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
        @_spi(kotlin$ExperimentalStdlibApi)
        public init(
            baseKey: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<B>,
            safeCast: @escaping (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> E?
        ) {
            precondition(Self.self != ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextKey<B,E>.self, "ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextKey is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_AbstractCoroutineContextKey_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_B__U28anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_E___(__kt, baseKey.__externalRCRef(), Unmanaged.passRetained(({ (_p0: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: Swift.Optional<E> = (safeCast)(_p0); return _res.__retainedToKotlinPointer() } as (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, deprecated, message: "Polymorphic coroutine context keys are error-prone, difficult to implement correctly, and can encourage users to depend on implementation details. Prefer retrieving the element by its base key and casting it explicitly when needed or introducing a dedicated extension property.") @_spi(kotlin$ExperimentalStdlibApi) @_documentation(visibility: internal)
    open class _AbstractCoroutineContextKey_Base: KotlinRuntime.KotlinBase {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlin.coroutines.cancellation {
    open class CancellationException: ExportedKotlinPackages.kotlin.IllegalStateException {
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.self {
                 __kt = kotlin_coroutines_cancellation_CancellationException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.self {
                 __kt = kotlin_coroutines_cancellation_CancellationException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.self {
                 __kt = kotlin_coroutines_cancellation_CancellationException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.self {
                 __kt = kotlin_coroutines_cancellation_CancellationException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlin.sequences {
    public protocol Sequence<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.sequences._Sequence {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T>
    }
    @objc(_ExportedKotlinPackages_kotlin_sequences_Sequence)
    public protocol _Sequence {
    }
    public protocol __Sequence: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class AnySequence<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.sequences.Sequence, ExportedKotlinPackages.kotlin.sequences.__Sequence, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
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
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
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
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer {
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
    }
    public final class Duration: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = ExportedKotlinPackages.kotlin.time.Duration
        public final class Companion: KotlinRuntime.KotlinBase {
            public var ZERO: ExportedKotlinPackages.kotlin.time.Duration {
                get {
                    return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_ZERO_get(self.__externalRCRef()))
                }
            }
            public var INFINITE: ExportedKotlinPackages.kotlin.time.Duration {
                get {
                    return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_INFINITE_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.time.Duration.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.time.Duration.Companion.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_get())
                }
            }
            @_spi(kotlin$time$ExperimentalTime)
            public func convert(
                value: Swift.Double,
                sourceUnit: ExportedKotlinPackages.kotlin.time.DurationUnit,
                targetUnit: ExportedKotlinPackages.kotlin.time.DurationUnit
            ) -> Swift.Double {
                return kotlin_time_Duration_Companion_convert__TypesOfArguments__Swift_Double_ExportedKotlinPackages_kotlin_time_DurationUnit_ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), value, sourceUnit.__externalRCRef(), targetUnit.__externalRCRef())
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
            public func parseOrNull(
                value: Swift.String
            ) -> ExportedKotlinPackages.kotlin.time.Duration? {
                return { switch kotlin_time_Duration_Companion_parseOrNull__TypesOfArguments__Swift_String__(self.__externalRCRef(), value) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: res); } }()
            }
            public func parseIsoStringOrNull(
                value: Swift.String
            ) -> ExportedKotlinPackages.kotlin.time.Duration? {
                return { switch kotlin_time_Duration_Companion_parseIsoStringOrNull__TypesOfArguments__Swift_String__(self.__externalRCRef(), value) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: res); } }()
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
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
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
        public var inWholeMinutes: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeMinutes_get(self.__externalRCRef())
            }
        }
        public var inWholeSeconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeSeconds_get(self.__externalRCRef())
            }
        }
        public var inWholeMilliseconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeMilliseconds_get(self.__externalRCRef())
            }
        }
        public var inWholeMicroseconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeMicroseconds_get(self.__externalRCRef())
            }
        }
        public var inWholeNanoseconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeNanoseconds_get(self.__externalRCRef())
            }
        }
        public func _unaryMinus() -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_unaryMinus(self.__externalRCRef()))
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._unaryMinus()
        }
        public func _plus(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_plus__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._plus(other: other)
        }
        public func _minus(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_minus__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._minus(other: other)
        }
        public func _times(
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), scale))
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._times(scale: scale)
        }
        public func _times(
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), scale))
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._times(scale: scale)
        }
        public func _div(
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), scale))
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._div(scale: scale)
        }
        public func _div(
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), scale))
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._div(scale: scale)
        }
        public func _div(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Double {
            return kotlin_time_Duration_div__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef())
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func isNegative() -> Swift.Bool {
            return kotlin_time_Duration_isNegative(self.__externalRCRef())
        }
        public func isPositive() -> Swift.Bool {
            return kotlin_time_Duration_isPositive(self.__externalRCRef())
        }
        public func isInfinite() -> Swift.Bool {
            return kotlin_time_Duration_isInfinite(self.__externalRCRef())
        }
        public func isFinite() -> Swift.Bool {
            return kotlin_time_Duration_isFinite(self.__externalRCRef())
        }
        public func _compareTo(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Int32 {
            return kotlin_time_Duration_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef())
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
        public func toComponents<T: KotlinRuntimeSupport._KotlinBridgeable>(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32, Swift.Int32) -> T
        ) -> T {
            return T.__fromKotlinPointer(kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20T__(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Int64, _p1: Swift.Int32, _p2: Swift.Int32, _p3: Swift.Int32, _p4: Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (action)(_p0, _p1, _p2, _p3, _p4); return _res.__retainedToKotlinPointer() } as (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
        }
        public func toComponents<T: KotlinRuntimeSupport._KotlinBridgeable>(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32) -> T
        ) -> T {
            return T.__fromKotlinPointer(kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20T__(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Int64, _p1: Swift.Int32, _p2: Swift.Int32, _p3: Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (action)(_p0, _p1, _p2, _p3); return _res.__retainedToKotlinPointer() } as (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
        }
        public func toComponents<T: KotlinRuntimeSupport._KotlinBridgeable>(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32) -> T
        ) -> T {
            return T.__fromKotlinPointer(kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32U29202D_U20T__(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Int64, _p1: Swift.Int32, _p2: Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (action)(_p0, _p1, _p2); return _res.__retainedToKotlinPointer() } as (Swift.Int64, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
        }
        public func toComponents<T: KotlinRuntimeSupport._KotlinBridgeable>(
            action: @escaping (Swift.Int64, Swift.Int32) -> T
        ) -> T {
            return T.__fromKotlinPointer(kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32U29202D_U20T__(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Int64, _p1: Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (action)(_p0, _p1); return _res.__retainedToKotlinPointer() } as (Swift.Int64, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
        }
        public func toDouble(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit
        ) -> Swift.Double {
            return kotlin_time_Duration_toDouble__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), unit.__externalRCRef())
        }
        public func toLong(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit
        ) -> Swift.Int64 {
            return kotlin_time_Duration_toLong__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), unit.__externalRCRef())
        }
        public func toInt(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit
        ) -> Swift.Int32 {
            return kotlin_time_Duration_toInt__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), unit.__externalRCRef())
        }
        public func toString(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit,
            decimals: Swift.Int32
        ) -> Swift.String {
            return kotlin_time_Duration_toString__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit_Swift_Int32__(self.__externalRCRef(), unit.__externalRCRef(), decimals)
        }
        public func toIsoString() -> Swift.String {
            return kotlin_time_Duration_toIsoString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Collection where Self : ExportedKotlinPackages.kotlin.collections.__Collection {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Collection_size_get(self.__externalRCRef())
        }
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Collection_isEmpty(self.__externalRCRef())
    }
    public func contains(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_Collection_contains__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public static func ~=(
        this: Self,
        element: E
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E> {
        return { let _ptr = kotlin_collections_Collection_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Collection {
}
extension ExportedKotlinPackages.kotlin.collections.Collection {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableCollection where Self : ExportedKotlinPackages.kotlin.collections.__MutableCollection {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<E> {
        return { let _ptr = kotlin_collections_MutableCollection_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    @discardableResult
    public func add(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_add__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    @discardableResult
    public func remove(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_remove__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    @discardableResult
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    @discardableResult
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    @discardableResult
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableCollection_clear(self.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableCollection {
}
extension ExportedKotlinPackages.kotlin.collections.MutableCollection {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : ExportedKotlinPackages.kotlin.collections.__Iterable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T> {
        return { let _ptr = kotlin_collections_Iterable_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterable {
}
extension ExportedKotlinPackages.kotlin.collections.Iterable {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableIterable where Self : ExportedKotlinPackages.kotlin.collections.__MutableIterable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<T> {
        return { let _ptr = kotlin_collections_MutableIterable_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableIterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableIterator<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableIterable {
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterable {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : ExportedKotlinPackages.kotlin.collections.__Iterator {
    public func next() -> T {
        return T.__fromKotlinPointer(kotlin_collections_Iterator_next(self.__externalRCRef()))
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterator {
}
extension ExportedKotlinPackages.kotlin.collections.Iterator {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.List where Self : ExportedKotlinPackages.kotlin.collections.__List {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_List_size_get(self.__externalRCRef())
        }
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_List_isEmpty(self.__externalRCRef())
    }
    public func contains(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_List_contains__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public static func ~=(
        this: Self,
        element: E
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E> {
        return { let _ptr = kotlin_collections_List_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func _get(
        index: Swift.Int32
    ) -> E {
        return E.__fromKotlinPointer(kotlin_collections_List_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index))
    }
    public func indexOf(
        element: E
    ) -> Swift.Int32 {
        return kotlin_collections_List_indexOf__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public func lastIndexOf(
        element: E
    ) -> Swift.Int32 {
        return kotlin_collections_List_lastIndexOf__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator<E> {
        return { let _ptr = kotlin_collections_List_listIterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.ListIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyListIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator<E> {
        return { let _ptr = kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.ListIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyListIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public func subList(
        fromIndex: Swift.Int32,
        toIndex: Swift.Int32
    ) -> [E] {
        return Swift.Array<E>.__fromKotlinPointer(kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex))
    }
    public subscript(
        index: Swift.Int32
    ) -> E {
        get {
            _get(index: index)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._List {
}
extension ExportedKotlinPackages.kotlin.collections.List {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableList where Self : ExportedKotlinPackages.kotlin.collections.__MutableList {
    @discardableResult
    public func add(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_add__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public func add(
        index: Swift.Int32,
        element: E
    ) -> Swift.Void {
        return { kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_E__(self.__externalRCRef(), index, element.__retainedToKotlinPointer()); return () }()
    }
    @discardableResult
    public func remove(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_remove__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    @discardableResult
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    @discardableResult
    public func addAll(
        index: Swift.Int32,
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), index, elements.__externalRCRef())
    }
    @discardableResult
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    @discardableResult
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableList_clear(self.__externalRCRef()); return () }()
    }
    @discardableResult
    public func _set(
        index: Swift.Int32,
        element: E
    ) -> E {
        return E.__fromKotlinPointer(kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_E__(self.__externalRCRef(), index, element.__retainedToKotlinPointer()))
    }
    @discardableResult
    public func removeAt(
        index: Swift.Int32
    ) -> E {
        return E.__fromKotlinPointer(kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index))
    }
    public func listIterator() -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E> {
        return { let _ptr = kotlin_collections_MutableList_listIterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableListIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E> {
        return { let _ptr = kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableListIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public func subList(
        fromIndex: Swift.Int32,
        toIndex: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.MutableList<E> {
        return { let _ptr = kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableList<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableList<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableList {
}
extension ExportedKotlinPackages.kotlin.collections.MutableList {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Set where Self : ExportedKotlinPackages.kotlin.collections.__Set {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Set_size_get(self.__externalRCRef())
        }
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Set_isEmpty(self.__externalRCRef())
    }
    public func contains(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_Set_contains__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public static func ~=(
        this: Self,
        element: E
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E> {
        return { let _ptr = kotlin_collections_Set_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Set {
}
extension ExportedKotlinPackages.kotlin.collections.Set {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableSet where Self : ExportedKotlinPackages.kotlin.collections.__MutableSet {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<E> {
        return { let _ptr = kotlin_collections_MutableSet_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    @discardableResult
    public func add(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_add__TypesOfArguments__E_Swift_Hashable__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    @discardableResult
    public func remove(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_remove__TypesOfArguments__E_Swift_Hashable__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    @discardableResult
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    @discardableResult
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    @discardableResult
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableSet_clear(self.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableSet {
}
extension ExportedKotlinPackages.kotlin.collections.MutableSet {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.coroutines.Continuation where Self : ExportedKotlinPackages.kotlin.coroutines.__Continuation {
    public var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_Continuation_context_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        }
    }
    public func resumeWith(
        result: ExportedKotlinPackages.kotlin.Result<T>
    ) -> Swift.Void {
        return { kotlin_coroutines_Continuation_resumeWith__TypesOfArguments__ExportedKotlinPackages_kotlin_Result_T___(self.__externalRCRef(), result.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.coroutines._Continuation {
}
extension ExportedKotlinPackages.kotlin.coroutines.Continuation {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor where Self : ExportedKotlinPackages.kotlin.coroutines.__ContinuationInterceptor {
    public func interceptContinuation<T: KotlinRuntimeSupport._KotlinBridgeable>(
        continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation<T>
    ) -> any ExportedKotlinPackages.kotlin.coroutines.Continuation<T> {
        return { let _ptr = kotlin_coroutines_ContinuationInterceptor_interceptContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation_T___(self.__externalRCRef(), continuation.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.coroutines.Continuation<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.coroutines.AnyContinuation<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public func releaseInterceptedContinuation(
        continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
    ) -> Swift.Void {
        return { kotlin_coroutines_ContinuationInterceptor_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__(self.__externalRCRef(), continuation.__externalRCRef()); return () }()
    }
    public func _get<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
    ) -> E? {
        return Swift.Optional<E>.__fromKotlinPointer(kotlin_coroutines_ContinuationInterceptor_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E___(self.__externalRCRef(), key.__externalRCRef()))
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public subscript<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
    ) -> E? {
        get {
            _get(key: key)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.coroutines._ContinuationInterceptor {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor, ExportedKotlinPackages.kotlin.coroutines.__ContinuationInterceptor where Wrapped : ExportedKotlinPackages.kotlin.coroutines._ContinuationInterceptor {
}
extension ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor {
    public typealias Key = KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key
    public func releaseInterceptedContinuation(
        continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
    ) -> Swift.Void {
        return { kotlin_coroutines_ContinuationInterceptor_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation___direct(self.__externalRCRef(), continuation.__externalRCRef()); return () }()
    }
    public func _get<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
    ) -> E? {
        return Swift.Optional<E>.__fromKotlinPointer(kotlin_coroutines_ContinuationInterceptor_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E____direct(self.__externalRCRef(), key.__externalRCRef()))
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key___direct(self.__externalRCRef(), key.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext where Self : ExportedKotlinPackages.kotlin.coroutines.__CoroutineContext {
    public func _get<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
    ) -> E? {
        return Swift.Optional<E>.__fromKotlinPointer(kotlin_coroutines_CoroutineContext_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E___(self.__externalRCRef(), key.__externalRCRef()))
    }
    public func fold<R: KotlinRuntimeSupport._KotlinBridgeable>(
        initial: R,
        operation: @escaping (R, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> R
    ) -> R {
        return R.__fromKotlinPointer(kotlin_coroutines_CoroutineContext_fold__TypesOfArguments__R_U28R_U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20R__(self.__externalRCRef(), initial.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = (operation)(R.__fromKotlinPointer(_p0), _p1); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    public func _plus(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_plus__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(self.__externalRCRef(), context.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public static func +(
        this: Self,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        this._plus(context: context)
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public subscript<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
    ) -> E? {
        get {
            _get(key: key)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.coroutines._CoroutineContext {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, ExportedKotlinPackages.kotlin.coroutines.__CoroutineContext where Wrapped : ExportedKotlinPackages.kotlin.coroutines._CoroutineContext {
}
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
    public typealias Key<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element> = KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
    public typealias Element = KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
    public func _plus(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_plus__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext___direct(self.__externalRCRef(), context.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.sequences.Sequence where Self : ExportedKotlinPackages.kotlin.sequences.__Sequence {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T> {
        return { let _ptr = kotlin_sequences_Sequence_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.sequences._Sequence {
}
extension ExportedKotlinPackages.kotlin.sequences.Sequence {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key where Self : KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
@_documentation(visibility: internal)
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element where Self : KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    public var key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
        get {
            return { let _ptr = kotlin_coroutines_CoroutineContext_Element_key_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
    }
    public func _get<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
    ) -> E? {
        return Swift.Optional<E>.__fromKotlinPointer(kotlin_coroutines_CoroutineContext_Element_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E___(self.__externalRCRef(), key.__externalRCRef()))
    }
    public func fold<R: KotlinRuntimeSupport._KotlinBridgeable>(
        initial: R,
        operation: @escaping (R, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> R
    ) -> R {
        return R.__fromKotlinPointer(kotlin_coroutines_CoroutineContext_Element_fold__TypesOfArguments__R_U28R_U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20R__(self.__externalRCRef(), initial.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = (operation)(R.__fromKotlinPointer(_p0), _p1); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_Element_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public subscript<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
    ) -> E? {
        get {
            _get(key: key)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element where Wrapped : KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
}
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    public func _get<E: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<E>
    ) -> E? {
        return Swift.Optional<E>.__fromKotlinPointer(kotlin_coroutines_CoroutineContext_Element_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E____direct(self.__externalRCRef(), key.__externalRCRef()))
    }
    public func fold<R: KotlinRuntimeSupport._KotlinBridgeable>(
        initial: R,
        operation: @escaping (R, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> R
    ) -> R {
        return R.__fromKotlinPointer(kotlin_coroutines_CoroutineContext_Element_fold__TypesOfArguments__R_U28R_U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20R___direct(self.__externalRCRef(), initial.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = (operation)(R.__fromKotlinPointer(_p0), _p1); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_Element_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key___direct(self.__externalRCRef(), key.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.Comparable where Self : ExportedKotlinPackages.kotlin.__Comparable {
    public func _compareTo(
        other: T
    ) -> Swift.Int32 {
        return kotlin_Comparable_compareTo__TypesOfArguments__T__(self.__externalRCRef(), other.__retainedToKotlinPointer())
    }
    public static func <(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) < 0
    }
    public static func <=(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) <= 0
    }
    public static func >(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) > 0
    }
    public static func >=(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) >= 0
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin._Comparable {
}
extension ExportedKotlinPackages.kotlin.Comparable {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableIterator where Self : ExportedKotlinPackages.kotlin.collections.__MutableIterator {
    public func remove() -> Swift.Void {
        return { kotlin_collections_MutableIterator_remove(self.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableIterator {
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterator {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.ListIterator where Self : ExportedKotlinPackages.kotlin.collections.__ListIterator {
    public func next() -> T {
        return T.__fromKotlinPointer(kotlin_collections_ListIterator_next(self.__externalRCRef()))
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasNext(self.__externalRCRef())
    }
    public func hasPrevious() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasPrevious(self.__externalRCRef())
    }
    public func previous() -> T {
        return T.__fromKotlinPointer(kotlin_collections_ListIterator_previous(self.__externalRCRef()))
    }
    public func nextIndex() -> Swift.Int32 {
        return kotlin_collections_ListIterator_nextIndex(self.__externalRCRef())
    }
    public func previousIndex() -> Swift.Int32 {
        return kotlin_collections_ListIterator_previousIndex(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._ListIterator {
}
extension ExportedKotlinPackages.kotlin.collections.ListIterator {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableListIterator where Self : ExportedKotlinPackages.kotlin.collections.__MutableListIterator {
    public func next() -> T {
        return T.__fromKotlinPointer(kotlin_collections_MutableListIterator_next(self.__externalRCRef()))
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_MutableListIterator_hasNext(self.__externalRCRef())
    }
    public func remove() -> Swift.Void {
        return { kotlin_collections_MutableListIterator_remove(self.__externalRCRef()); return () }()
    }
    public func set(
        element: T
    ) -> Swift.Void {
        return { kotlin_collections_MutableListIterator_set__TypesOfArguments__T__(self.__externalRCRef(), element.__retainedToKotlinPointer()); return () }()
    }
    public func add(
        element: T
    ) -> Swift.Void {
        return { kotlin_collections_MutableListIterator_add__TypesOfArguments__T__(self.__externalRCRef(), element.__retainedToKotlinPointer()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableListIterator {
}
extension ExportedKotlinPackages.kotlin.collections.MutableListIterator {
}
@_cdecl("KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32_Swift_Int32_Swift_Int32__")
package func KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32_Swift_Int32_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int64, _ _2: Swift.Int32, _ _3: Swift.Int32, _ _4: Swift.Int32, _ _5: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2, _3, _4, _5)
    return _result
}

@_cdecl("KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32_Swift_Int32__")
package func KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int64, _ _2: Swift.Int32, _ _3: Swift.Int32, _ _4: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2, _3, _4)
    return _result
}

@_cdecl("KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32__")
package func KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int64, _ _2: Swift.Int32, _ _3: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int64, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2, _3)
    return _result
}

@_cdecl("KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32__")
package func KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int64, _ _2: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int64, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2)
    return _result
}

@_cdecl("KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element__")
package func KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _2, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)
    return _result
}

@_cdecl("KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element__")
package func KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _1, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)
    return _result
}

@_cdecl("kotlin_Comparable_compareTo__TypesOfArguments__T____reverse_swift")
package func kotlin_Comparable_compareTo__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ other: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.Comparable)
    func _open<__Self: ExportedKotlinPackages.kotlin.Comparable>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self._compareTo(other: __Self.T.__fromKotlinPointer(other))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_Throwable_cause_get__reverse_swift")
package func kotlin_Throwable_cause_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = _self.cause
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_Throwable_message_get__reverse_swift")
package func kotlin_Throwable_message_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String? {
    let _self = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Optional<Swift.String> = _self.message
    return _result ?? nil
}

@_cdecl("kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_contains__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_Collection_contains__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.contains(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_isEmpty__reverse_swift")
package func kotlin_collections_Collection_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_iterator__reverse_swift")
package func kotlin_collections_Collection_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_size_get__reverse_swift")
package func kotlin_collections_Collection_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.size
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_IntIterator_nextInt__reverse_swift")
package func kotlin_collections_IntIterator_nextInt__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int32 = _self.nextInt()
    return _result
}

@_cdecl("kotlin_collections_Iterable_iterator__reverse_swift")
package func kotlin_collections_Iterable_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterable)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterable>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.T> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Iterator_hasNext__reverse_swift")
package func kotlin_collections_Iterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.hasNext()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Iterator_next__reverse_swift")
package func kotlin_collections_Iterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.next()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_hasNext__reverse_swift")
package func kotlin_collections_ListIterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.hasNext()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_hasPrevious__reverse_swift")
package func kotlin_collections_ListIterator_hasPrevious__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.hasPrevious()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_nextIndex__reverse_swift")
package func kotlin_collections_ListIterator_nextIndex__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.nextIndex()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_next__reverse_swift")
package func kotlin_collections_ListIterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.next()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_previousIndex__reverse_swift")
package func kotlin_collections_ListIterator_previousIndex__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.previousIndex()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_previous__reverse_swift")
package func kotlin_collections_ListIterator_previous__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.previous()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_contains__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_List_contains__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.contains(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.E = _self._get(index: index)
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_indexOf__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_List_indexOf__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.indexOf(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_isEmpty__reverse_swift")
package func kotlin_collections_List_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_iterator__reverse_swift")
package func kotlin_collections_List_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_lastIndexOf__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_List_lastIndexOf__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.lastIndexOf(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.ListIterator<__Self.E> = _self.listIterator(index: index)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_listIterator__reverse_swift")
package func kotlin_collections_List_listIterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.ListIterator<__Self.E> = _self.listIterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_size_get__reverse_swift")
package func kotlin_collections_List_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.size
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
package func kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ fromIndex: Swift.Int32, _ toIndex: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Array<__Self.E> = _self.subList(fromIndex: fromIndex, toIndex: toIndex)
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_LongIterator_nextLong__reverse_swift")
package func kotlin_collections_LongIterator_nextLong__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int64 {
    let _self = ExportedKotlinPackages.kotlin.collections.LongIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int64 = _self.nextLong()
    return _result
}

@_cdecl("kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.addAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_add__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_MutableCollection_add__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.add(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_clear__reverse_swift")
package func kotlin_collections_MutableCollection_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.clear()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_iterator__reverse_swift")
package func kotlin_collections_MutableCollection_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.removeAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_remove__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_MutableCollection_remove__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.remove(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.retainAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableIterable_iterator__reverse_swift")
package func kotlin_collections_MutableIterable_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableIterable)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableIterable>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator<__Self.T> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableIterator_remove__reverse_swift")
package func kotlin_collections_MutableIterator_remove__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.remove()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableListIterator_add__TypesOfArguments__T____reverse_swift")
package func kotlin_collections_MutableListIterator_add__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.add(element: __Self.T.__fromKotlinPointer(element))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableListIterator_hasNext__reverse_swift")
package func kotlin_collections_MutableListIterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.hasNext()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableListIterator_next__reverse_swift")
package func kotlin_collections_MutableListIterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableListIterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.next()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableListIterator_remove__reverse_swift")
package func kotlin_collections_MutableListIterator_remove__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.remove()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableListIterator_set__TypesOfArguments__T____reverse_swift")
package func kotlin_collections_MutableListIterator_set__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.set(element: __Self.T.__fromKotlinPointer(element))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.addAll(index: index, elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.addAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_add__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_MutableList_add__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.add(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_E____reverse_swift")
package func kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.add(index: index, element: __Self.E.__fromKotlinPointer(element))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_clear__reverse_swift")
package func kotlin_collections_MutableList_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.clear()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableListIterator<__Self.E> = _self.listIterator(index: index)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_listIterator__reverse_swift")
package func kotlin_collections_MutableList_listIterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableListIterator<__Self.E> = _self.listIterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.removeAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.E = _self.removeAt(index: index)
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_remove__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_MutableList_remove__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.remove(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.retainAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_E____reverse_swift")
package func kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.E = _self._set(index: index, element: __Self.E.__fromKotlinPointer(element))
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ fromIndex: Swift.Int32, _ toIndex: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableList<__Self.E> = _self.subList(fromIndex: fromIndex, toIndex: toIndex)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.addAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_add__TypesOfArguments__E_Swift_Hashable____reverse_swift")
package func kotlin_collections_MutableSet_add__TypesOfArguments__E_Swift_Hashable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.add(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_clear__reverse_swift")
package func kotlin_collections_MutableSet_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.clear()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_iterator__reverse_swift")
package func kotlin_collections_MutableSet_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.removeAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_remove__TypesOfArguments__E_Swift_Hashable____reverse_swift")
package func kotlin_collections_MutableSet_remove__TypesOfArguments__E_Swift_Hashable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.remove(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.retainAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Set)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Set>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Set_contains__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_Set_contains__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Set)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Set>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.contains(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Set_isEmpty__reverse_swift")
package func kotlin_collections_Set_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Set)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Set>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Set_iterator__reverse_swift")
package func kotlin_collections_Set_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Set)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Set>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Set_size_get__reverse_swift")
package func kotlin_collections_Set_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Set)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Set>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.size
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_coroutines_AbstractCoroutineContextElement_key_get__reverse_swift")
package func kotlin_coroutines_AbstractCoroutineContextElement_key_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement.__createClassWrapper(externalRCRef: `self`)
    let _result: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key = _self.key
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_ContinuationInterceptor_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E_____reverse_swift")
package func kotlin_coroutines_ContinuationInterceptor_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor
    let _result: Swift.Optional<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>> = _self._get(key: { let _ptr = key; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlin_coroutines_ContinuationInterceptor_interceptContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation_T_____reverse_swift")
package func kotlin_coroutines_ContinuationInterceptor_interceptContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor
    let _result: any ExportedKotlinPackages.kotlin.coroutines.Continuation<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> = _self.interceptContinuation(continuation: { let _ptr = continuation; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.coroutines.Continuation<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.coroutines.AnyContinuation<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift")
package func kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor
    let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.minusKey(key: { let _ptr = key; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_ContinuationInterceptor_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation____reverse_swift")
package func kotlin_coroutines_ContinuationInterceptor_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor
    let _result: Swift.Void = _self.releaseInterceptedContinuation(continuation: { let _ptr = continuation; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.coroutines.Continuation) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.coroutines.AnyContinuation<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    return { _result; return true }()
}

@_cdecl("kotlin_coroutines_Continuation_context_get__reverse_swift")
package func kotlin_coroutines_Continuation_context_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.coroutines.Continuation)
    func _open<__Self: ExportedKotlinPackages.kotlin.coroutines.Continuation>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.context
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_coroutines_Continuation_resumeWith__TypesOfArguments__ExportedKotlinPackages_kotlin_Result_T_____reverse_swift")
package func kotlin_coroutines_Continuation_resumeWith__TypesOfArguments__ExportedKotlinPackages_kotlin_Result_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ result: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.coroutines.Continuation)
    func _open<__Self: ExportedKotlinPackages.kotlin.coroutines.Continuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.resumeWith(result: ExportedKotlinPackages.kotlin.Result<__Self.T>.__fromKotlinPointer(result))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_coroutines_CoroutineContext_Element_fold__TypesOfArguments__R_U28R_U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20R____reverse_swift")
package func kotlin_coroutines_CoroutineContext_Element_fold__TypesOfArguments__R_U28R_U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20R____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ initial: Swift.UnsafeMutableRawPointer?, _ operation: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
    let _result: Swift.Optional<KotlinRuntimeSupport.KotlinAny> = _self.fold(initial: Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(initial), operation: {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: operation, options: .asBestFittingWrapper)
    return { _1, _2 in return Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(KotlinStdlib_internal_functional_type_caller_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E_generic_Function2_kotlin_Any___kotlin_coroutines_CoroutineContext_Element__kotlin_Any____TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element__(pointerToBlock.__externalRCRef(), _1.__retainedToKotlinPointer(), _2.__externalRCRef())) }
}())
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlin_coroutines_CoroutineContext_Element_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E_____reverse_swift")
package func kotlin_coroutines_CoroutineContext_Element_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
    let _result: Swift.Optional<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>> = _self._get(key: { let _ptr = key; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlin_coroutines_CoroutineContext_Element_key_get__reverse_swift")
package func kotlin_coroutines_CoroutineContext_Element_key_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
    let _result: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key = _self.key
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_CoroutineContext_Element_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift")
package func kotlin_coroutines_CoroutineContext_Element_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
    let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.minusKey(key: { let _ptr = key; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_CoroutineContext_fold__TypesOfArguments__R_U28R_U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20R____reverse_swift")
package func kotlin_coroutines_CoroutineContext_fold__TypesOfArguments__R_U28R_U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20R____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ initial: Swift.UnsafeMutableRawPointer?, _ operation: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    let _result: Swift.Optional<KotlinRuntimeSupport.KotlinAny> = _self.fold(initial: Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(initial), operation: {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: operation, options: .asBestFittingWrapper)
    return { _1, _2 in return Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(KotlinStdlib_internal_functional_type_caller_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E_generic_Function2_kotlin_Any___kotlin_coroutines_CoroutineContext_Element__kotlin_Any____TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element__(pointerToBlock.__externalRCRef(), _1.__retainedToKotlinPointer(), _2.__externalRCRef())) }
}())
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlin_coroutines_CoroutineContext_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E_____reverse_swift")
package func kotlin_coroutines_CoroutineContext_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    let _result: Swift.Optional<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>> = _self._get(key: { let _ptr = key; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlin_coroutines_CoroutineContext_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift")
package func kotlin_coroutines_CoroutineContext_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.minusKey(key: { let _ptr = key; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_CoroutineContext_plus__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext____reverse_swift")
package func kotlin_coroutines_CoroutineContext_plus__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self._plus(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext)
    return _result.__externalRCRef()
}

@_cdecl("kotlin_sequences_Sequence_iterator__reverse_swift")
package func kotlin_sequences_Sequence_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.sequences.Sequence)
    func _open<__Self: ExportedKotlinPackages.kotlin.sequences.Sequence>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.T> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_get__TypesOfArguments__Swift_Int32__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Array_get__TypesOfArguments__Swift_Int32__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_iterator(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlin_Array_iterator(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.Int32, _ _p2: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    return kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(_p0, _p1, _p2)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_size_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    return kotlin_Array_size_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Result_Companion_failure__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlin_Result_Companion_failure__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Result_Companion_success__TypesOfArguments__T__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    return kotlin_Result_Companion_success__TypesOfArguments__T__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Result_exceptionOrNull(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Result_exceptionOrNull(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Result_getOrNull(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Result_getOrNull(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Result_isFailure_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    return kotlin_Result_isFailure_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Result_isSuccess_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    return kotlin_Result_isSuccess_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_collections_IndexedValue_copy__TypesOfArguments__Swift_Int32_T__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.Int32, _ _p2: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    return kotlin_collections_IndexedValue_copy__TypesOfArguments__Swift_Int32_T__(_p0, _p1, _p2)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_collections_IndexedValue_index_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    return kotlin_collections_IndexedValue_index_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_collections_IndexedValue_value_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_collections_IndexedValue_value_get(_p0)
}
