@_exported import ExportedKotlinPackages
import KotlinRuntime
import KotlinRuntimeSupport
@_exported import KotlinCoroutineSupport
@_implementationOnly import KotlinBridges_KotlinStdlib

public protocol _ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key: KotlinRuntime.KotlinBase, KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
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
    func _get(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)?
    func fold(
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
}
@objc(_KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)
public protocol __ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element: ExportedKotlinPackages.kotlin.coroutines._CoroutineContext {
}
public protocol ___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.coroutines.__CoroutineContext {
}
public final class _ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key: KotlinRuntime.KotlinBase {
    public static var shared: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key {
        get {
            return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_Key.__createClassWrapper(externalRCRef: kotlin_coroutines_ContinuationInterceptor_Key_get())
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private init() {
        fatalError()
    }
}
extension ExportedKotlinPackages.kotlin {
    public final class Array: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_Array_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_Array_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func _set(
            index: Swift.Int32,
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void {
            return { kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, value.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_Array_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
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
        public final func getStackTrace() -> ExportedKotlinPackages.kotlin.Array {
            return ExportedKotlinPackages.kotlin.Array.__createClassWrapper(externalRCRef: kotlin_Throwable_getStackTrace(self.__externalRCRef()))
        }
        public final func printStackTrace() -> Swift.Void {
            return { kotlin_Throwable_printStackTrace(self.__externalRCRef()); return () }()
        }
        open func toString() -> Swift.String {
            if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                return kotlin_Throwable_toString(self.__externalRCRef())
            } else {
                return kotlin_Throwable_toString_direct(self.__externalRCRef())
            }
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
        public init() {
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Result: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlin.Result.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Result.Companion.__createClassWrapper(externalRCRef: kotlin_Result_Companion_get())
                }
            }
            public func success(
                value: (any KotlinRuntimeSupport._KotlinBridgeable)?
            ) -> ExportedKotlinPackages.kotlin.Result {
                return ExportedKotlinPackages.kotlin.Result.__createClassWrapper(externalRCRef: kotlin_Result_Companion_success__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil))
            }
            public func failure(
                exception: ExportedKotlinPackages.kotlin.Throwable
            ) -> ExportedKotlinPackages.kotlin.Result {
                return ExportedKotlinPackages.kotlin.Result.__createClassWrapper(externalRCRef: kotlin_Result_Companion_failure__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), exception.__externalRCRef()))
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
        }
        public var isSuccess: Swift.Bool {
            get {
                return kotlin_Result_isSuccess_get(self.__externalRCRef())
            }
        }
        public var isFailure: Swift.Bool {
            get {
                return kotlin_Result_isFailure_get(self.__externalRCRef())
            }
        }
        public func getOrNull() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_Result_getOrNull(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func exceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? {
            return { switch kotlin_Result_exceptionOrNull(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
        }
        public func toString() -> Swift.String {
            return kotlin_Result_toString(self.__externalRCRef())
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_Result_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Result,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_Result_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlin.collections {
    public protocol MutableCollection: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.MutableIterable, ExportedKotlinPackages.kotlin.collections._MutableCollection {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator
        func add(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func remove(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func clear() -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableCollection)
    public protocol _MutableCollection: ExportedKotlinPackages.kotlin.collections._Collection, ExportedKotlinPackages.kotlin.collections._MutableIterable {
    }
    public protocol __MutableCollection: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection, ExportedKotlinPackages.kotlin.collections.__MutableIterable {
    }
    public protocol Iterable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterable {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Iterable)
    public protocol _Iterable {
    }
    public protocol __Iterable: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol Iterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterator {
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func hasNext() -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Iterator)
    public protocol _Iterator {
    }
    public protocol __Iterator: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol MutableList: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections._MutableList {
        func add(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func add(
            index: Swift.Int32,
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
        func remove(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func addAll(
            index: Swift.Int32,
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func clear() -> Swift.Void
        func _set(
            index: Swift.Int32,
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func removeAt(
            index: Swift.Int32
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func listIterator() -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator
        func listIterator(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator
        func subList(
            fromIndex: Swift.Int32,
            toIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.MutableList
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableList)
    public protocol _MutableList: ExportedKotlinPackages.kotlin.collections._List, ExportedKotlinPackages.kotlin.collections._MutableCollection {
    }
    public protocol __MutableList: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__List, ExportedKotlinPackages.kotlin.collections.__MutableCollection {
    }
    public protocol MutableSet: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Set, ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections._MutableSet {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator
        func add(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func remove(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func clear() -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableSet)
    public protocol _MutableSet: ExportedKotlinPackages.kotlin.collections._Set, ExportedKotlinPackages.kotlin.collections._MutableCollection {
    }
    public protocol __MutableSet: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Set, ExportedKotlinPackages.kotlin.collections.__MutableCollection {
    }
    public protocol Collection: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections._Collection {
        var size: Swift.Int32 {
            get
        }
        func isEmpty() -> Swift.Bool
        func contains(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Collection)
    public protocol _Collection: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    public protocol __Collection: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterable {
    }
    public protocol MutableIterable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections._MutableIterable {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableIterable)
    public protocol _MutableIterable: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    public protocol __MutableIterable: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterable {
    }
    public protocol MutableIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections._MutableIterator {
        func remove() -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableIterator)
    public protocol _MutableIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
    public protocol __MutableIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterator {
    }
    public protocol MutableListIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.ListIterator, ExportedKotlinPackages.kotlin.collections.MutableIterator, ExportedKotlinPackages.kotlin.collections._MutableListIterator {
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func hasNext() -> Swift.Bool
        func remove() -> Swift.Void
        func set(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
        func add(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableListIterator)
    public protocol _MutableListIterator: ExportedKotlinPackages.kotlin.collections._ListIterator, ExportedKotlinPackages.kotlin.collections._MutableIterator {
    }
    public protocol __MutableListIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__ListIterator, ExportedKotlinPackages.kotlin.collections.__MutableIterator {
    }
    public protocol List: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections._List {
        var size: Swift.Int32 {
            get
        }
        func isEmpty() -> Swift.Bool
        func contains(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func _get(
            index: Swift.Int32
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func indexOf(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Int32
        func lastIndexOf(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Int32
        func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator
        func listIterator(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator
        func subList(
            fromIndex: Swift.Int32,
            toIndex: Swift.Int32
        ) -> [(any KotlinRuntimeSupport._KotlinBridgeable)?]
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_List)
    public protocol _List: ExportedKotlinPackages.kotlin.collections._Collection {
    }
    public protocol __List: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection {
    }
    public protocol Set: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections._Set {
        var size: Swift.Int32 {
            get
        }
        func isEmpty() -> Swift.Bool
        func contains(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Set)
    public protocol _Set: ExportedKotlinPackages.kotlin.collections._Collection {
    }
    public protocol __Set: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection {
    }
    public protocol ListIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections._ListIterator {
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func hasNext() -> Swift.Bool
        func hasPrevious() -> Swift.Bool
        func previous() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func nextIndex() -> Swift.Int32
        func previousIndex() -> Swift.Int32
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_ListIterator)
    public protocol _ListIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
    public protocol __ListIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterator {
    }
    public final class IndexedValue: KotlinRuntime.KotlinBase {
        public var index: Swift.Int32 {
            get {
                return kotlin_collections_IndexedValue_index_get(self.__externalRCRef())
            }
        }
        public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            get {
                return { switch kotlin_collections_IndexedValue_value_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
            }
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
        public static func ==(
            this: ExportedKotlinPackages.kotlin.collections.IndexedValue,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_collections_IndexedValue_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_collections_IndexedValue_toString(self.__externalRCRef())
        }
        public init(
            index: Swift.Int32,
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) {
            let __kt = kotlin_collections_IndexedValue_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_IndexedValue_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt, index, value.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class IntIterator: KotlinRuntime.KotlinBase {
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
        public init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.IntIterator.self, "ExportedKotlinPackages.kotlin.collections.IntIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_IntIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class LongIterator: KotlinRuntime.KotlinBase {
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
        public init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.LongIterator.self, "ExportedKotlinPackages.kotlin.collections.LongIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_LongIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlin.coroutines {
    public protocol Continuation: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.coroutines._Continuation {
        var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            get
        }
        func resumeWith(
            result: ExportedKotlinPackages.kotlin.Result
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlin_coroutines_Continuation)
    public protocol _Continuation {
    }
    public protocol __Continuation: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol ContinuationInterceptor: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element, ExportedKotlinPackages.kotlin.coroutines._ContinuationInterceptor {
        func interceptContinuation(
            continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
        ) -> any ExportedKotlinPackages.kotlin.coroutines.Continuation
        func releaseInterceptedContinuation(
            continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
        ) -> Swift.Void
        func _get(
            key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
        ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)?
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
        func _get(
            key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
        ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)?
        func fold(
            initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
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
    open class AbstractCoroutineContextElement: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
        open var key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
            get {
                if Self.self == ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement.self {
                    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_AbstractCoroutineContextElement_key_get(self.__externalRCRef()), conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
                } else {
                    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_AbstractCoroutineContextElement_key_get_direct(self.__externalRCRef()), conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, deprecated, message: "Polymorphic coroutine context keys are error-prone, difficult to implement correctly, and can encourage users to depend on implementation details. Prefer retrieving the element by its base key and casting it explicitly when needed or introducing a dedicated extension property.") @_spi(kotlin$ExperimentalStdlibApi)
    open class AbstractCoroutineContextKey: KotlinRuntime.KotlinBase {
        @_spi(kotlin$ExperimentalStdlibApi)
        public init(
            baseKey: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key,
            safeCast: @escaping (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)?
        ) {
            precondition(Self.self != ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextKey.self, "ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextKey is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_coroutines_AbstractCoroutineContextKey_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key_U28anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element___(__kt, baseKey.__externalRCRef(), {
                let originalBlock: (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element> = safeCast
                return { (arg0: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
                    let _result = originalBlock(_arg0)
                    return _result.map { it in it.__externalRCRef() } ?? nil
                }
            }()); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlin.sequences {
    public protocol Sequence: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.sequences._Sequence {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    @objc(_ExportedKotlinPackages_kotlin_sequences_Sequence)
    public protocol _Sequence {
    }
    public protocol __Sequence: KotlinRuntimeSupport._KotlinBridgeable {
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
    public final class Duration: KotlinRuntime.KotlinBase {
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
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
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
        public func toComponents(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32, Swift.Int32) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), {
                let originalBlock: (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32, Swift.Int32) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = action
                return { (arg0: Swift.Int64, arg1: Swift.Int32, arg2: Swift.Int32, arg3: Swift.Int32, arg4: Swift.Int32) in
                    let _arg0: Swift.Int64 = arg0
                    let _arg1: Swift.Int32 = arg1
                    let _arg2: Swift.Int32 = arg2
                    let _arg3: Swift.Int32 = arg3
                    let _arg4: Swift.Int32 = arg4
                    let _result = originalBlock(_arg0, _arg1, _arg2, _arg3, _arg4)
                    return _result.map { it in it.__externalRCRef() } ?? nil
                }
            }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func toComponents(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), {
                let originalBlock: (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = action
                return { (arg0: Swift.Int64, arg1: Swift.Int32, arg2: Swift.Int32, arg3: Swift.Int32) in
                    let _arg0: Swift.Int64 = arg0
                    let _arg1: Swift.Int32 = arg1
                    let _arg2: Swift.Int32 = arg2
                    let _arg3: Swift.Int32 = arg3
                    let _result = originalBlock(_arg0, _arg1, _arg2, _arg3)
                    return _result.map { it in it.__externalRCRef() } ?? nil
                }
            }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func toComponents(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), {
                let originalBlock: (Swift.Int64, Swift.Int32, Swift.Int32) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = action
                return { (arg0: Swift.Int64, arg1: Swift.Int32, arg2: Swift.Int32) in
                    let _arg0: Swift.Int64 = arg0
                    let _arg1: Swift.Int32 = arg1
                    let _arg2: Swift.Int32 = arg2
                    let _result = originalBlock(_arg0, _arg1, _arg2)
                    return _result.map { it in it.__externalRCRef() } ?? nil
                }
            }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func toComponents(
            action: @escaping (Swift.Int64, Swift.Int32) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), {
                let originalBlock: (Swift.Int64, Swift.Int32) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = action
                return { (arg0: Swift.Int64, arg1: Swift.Int32) in
                    let _arg0: Swift.Int64 = arg0
                    let _arg1: Swift.Int32 = arg1
                    let _result = originalBlock(_arg0, _arg1)
                    return _result.map { it in it.__externalRCRef() } ?? nil
                }
            }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
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
        public func toString() -> Swift.String {
            return kotlin_time_Duration_toString(self.__externalRCRef())
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
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_time_Duration_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_time_Duration_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableCollection where Self : ExportedKotlinPackages.kotlin.collections.__MutableCollection {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableCollection_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    public func add(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func remove(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableCollection_clear(self.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableCollection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections.__MutableCollection where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableCollection {
}
extension ExportedKotlinPackages.kotlin.collections.MutableCollection {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : ExportedKotlinPackages.kotlin.collections.__Iterable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Iterable_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections.__Iterable where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterable {
}
extension ExportedKotlinPackages.kotlin.collections.Iterable {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : ExportedKotlinPackages.kotlin.collections.__Iterator {
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_Iterator_next(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterator {
}
extension ExportedKotlinPackages.kotlin.collections.Iterator {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableList where Self : ExportedKotlinPackages.kotlin.collections.__MutableList {
    public func add(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func add(
        index: Swift.Int32,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, element.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public func remove(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func addAll(
        index: Swift.Int32,
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), index, elements.__externalRCRef())
    }
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableList_clear(self.__externalRCRef()); return () }()
    }
    public func _set(
        index: Swift.Int32,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, element.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func removeAt(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func listIterator() -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableList_listIterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    }
    public func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    }
    public func subList(
        fromIndex: Swift.Int32,
        toIndex: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.MutableList {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableList {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableList, ExportedKotlinPackages.kotlin.collections.__MutableList where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableList {
}
extension ExportedKotlinPackages.kotlin.collections.MutableList {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableSet where Self : ExportedKotlinPackages.kotlin.collections.__MutableSet {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableSet_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    public func add(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func remove(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableSet_clear(self.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableSet {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableSet, ExportedKotlinPackages.kotlin.collections.__MutableSet where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableSet {
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
        result: ExportedKotlinPackages.kotlin.Result
    ) -> Swift.Void {
        return { kotlin_coroutines_Continuation_resumeWith__TypesOfArguments__ExportedKotlinPackages_kotlin_Result__(self.__externalRCRef(), result.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.coroutines._Continuation {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.coroutines.Continuation, ExportedKotlinPackages.kotlin.coroutines.__Continuation where Wrapped : ExportedKotlinPackages.kotlin.coroutines._Continuation {
}
extension ExportedKotlinPackages.kotlin.coroutines.Continuation {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor where Self : ExportedKotlinPackages.kotlin.coroutines.__ContinuationInterceptor {
    public func interceptContinuation(
        continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
    ) -> any ExportedKotlinPackages.kotlin.coroutines.Continuation {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_ContinuationInterceptor_interceptContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__(self.__externalRCRef(), continuation.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.Continuation.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.Continuation
    }
    public func releaseInterceptedContinuation(
        continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
    ) -> Swift.Void {
        return { kotlin_coroutines_ContinuationInterceptor_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__(self.__externalRCRef(), continuation.__externalRCRef()); return () }()
    }
    public func _get(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)? {
        return { switch kotlin_coroutines_ContinuationInterceptor_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element; } }()
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public subscript(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)? {
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
    public func _get(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)? {
        return { switch kotlin_coroutines_ContinuationInterceptor_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key___direct(self.__externalRCRef(), key.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element; } }()
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key___direct(self.__externalRCRef(), key.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext where Self : ExportedKotlinPackages.kotlin.coroutines.__CoroutineContext {
    public func _get(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)? {
        return { switch kotlin_coroutines_CoroutineContext_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element; } }()
    }
    public func fold(
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_coroutines_CoroutineContext_fold__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg1, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
                let _result = originalBlock(_arg0, _arg1)
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
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
    public subscript(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)? {
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
    public typealias Key = KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    public typealias Element = KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
    public func _plus(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_plus__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext___direct(self.__externalRCRef(), context.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.sequences.Sequence where Self : ExportedKotlinPackages.kotlin.sequences.__Sequence {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_sequences_Sequence_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.sequences._Sequence {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.sequences.Sequence, ExportedKotlinPackages.kotlin.sequences.__Sequence where Wrapped : ExportedKotlinPackages.kotlin.sequences._Sequence {
}
extension ExportedKotlinPackages.kotlin.sequences.Sequence {
}
@_documentation(visibility: internal)
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key where Self : KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key where Wrapped : KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
}
@_documentation(visibility: internal)
extension KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element where Self : KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    public var key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_Element_key_get(self.__externalRCRef()), conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
        }
    }
    public func _get(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)? {
        return { switch kotlin_coroutines_CoroutineContext_Element_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element; } }()
    }
    public func fold(
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_coroutines_CoroutineContext_Element_fold__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg1, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
                let _result = originalBlock(_arg0, _arg1)
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_Element_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self.__externalRCRef(), key.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public subscript(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)? {
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
    public func _get(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element)? {
        return { switch kotlin_coroutines_CoroutineContext_Element_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key___direct(self.__externalRCRef(), key.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element; } }()
    }
    public func fold(
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_coroutines_CoroutineContext_Element_fold__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct(self.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg1, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
                let _result = originalBlock(_arg0, _arg1)
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func minusKey(
        key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_coroutines_CoroutineContext_Element_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key___direct(self.__externalRCRef(), key.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
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
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public static func ~=(
        this: Self,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Collection_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Collection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.__Collection where Wrapped : ExportedKotlinPackages.kotlin.collections._Collection {
}
extension ExportedKotlinPackages.kotlin.collections.Collection {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableIterable where Self : ExportedKotlinPackages.kotlin.collections.__MutableIterable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableIterable_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableIterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterable, ExportedKotlinPackages.kotlin.collections.__MutableIterable where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterable {
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterable {
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
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterator, ExportedKotlinPackages.kotlin.collections.__MutableIterator where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterator {
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterator {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableListIterator where Self : ExportedKotlinPackages.kotlin.collections.__MutableListIterator {
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_MutableListIterator_next(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_MutableListIterator_hasNext(self.__externalRCRef())
    }
    public func remove() -> Swift.Void {
        return { kotlin_collections_MutableListIterator_remove(self.__externalRCRef()); return () }()
    }
    public func set(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlin_collections_MutableListIterator_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public func add(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlin_collections_MutableListIterator_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableListIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableListIterator, ExportedKotlinPackages.kotlin.collections.__MutableListIterator where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableListIterator {
}
extension ExportedKotlinPackages.kotlin.collections.MutableListIterator {
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
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public static func ~=(
        this: Self,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_List_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func _get(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_List_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func indexOf(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        return kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func lastIndexOf(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        return kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_List_listIterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    }
    public func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    }
    public func subList(
        fromIndex: Swift.Int32,
        toIndex: Swift.Int32
    ) -> [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        return kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex) as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    }
    public subscript(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            _get(index: index)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._List {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.kotlin.collections.__List where Wrapped : ExportedKotlinPackages.kotlin.collections._List {
}
extension ExportedKotlinPackages.kotlin.collections.List {
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
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public static func ~=(
        this: Self,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Set_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Set {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Set, ExportedKotlinPackages.kotlin.collections.__Set where Wrapped : ExportedKotlinPackages.kotlin.collections._Set {
}
extension ExportedKotlinPackages.kotlin.collections.Set {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.ListIterator where Self : ExportedKotlinPackages.kotlin.collections.__ListIterator {
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_ListIterator_next(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasNext(self.__externalRCRef())
    }
    public func hasPrevious() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasPrevious(self.__externalRCRef())
    }
    public func previous() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_ListIterator_previous(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
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
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.ListIterator, ExportedKotlinPackages.kotlin.collections.__ListIterator where Wrapped : ExportedKotlinPackages.kotlin.collections._ListIterator {
}
extension ExportedKotlinPackages.kotlin.collections.ListIterator {
}
@_cdecl("kotlin_Throwable_cause_get__reverse_swift")
package func kotlin_Throwable_cause_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = _self.cause
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_Throwable_message_get__reverse_swift")
package func kotlin_Throwable_message_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String? {
    let _self = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<Swift.String> = _self.message
    return _result ?? nil
}

@_cdecl("kotlin_Throwable_toString__reverse_swift")
package func kotlin_Throwable_toString__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.toString()
    return _result
}

@_cdecl("kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection
    let _result: Swift.Bool = _self.containsAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection
    let _result: Swift.Bool = _self.contains(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_Collection_isEmpty__reverse_swift")
package func kotlin_collections_Collection_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@_cdecl("kotlin_collections_Collection_iterator__reverse_swift")
package func kotlin_collections_Collection_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection
    let _result: any ExportedKotlinPackages.kotlin.collections.Iterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_Collection_size_get__reverse_swift")
package func kotlin_collections_Collection_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection
    let _result: Swift.Int32 = _self.size
    return _result
}

@_cdecl("kotlin_collections_IntIterator_nextInt__reverse_swift")
package func kotlin_collections_IntIterator_nextInt__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.nextInt()
    return _result
}

@_cdecl("kotlin_collections_Iterable_iterator__reverse_swift")
package func kotlin_collections_Iterable_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Iterable.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterable
    let _result: any ExportedKotlinPackages.kotlin.collections.Iterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_Iterator_hasNext__reverse_swift")
package func kotlin_collections_Iterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    let _result: Swift.Bool = _self.hasNext()
    return _result
}

@_cdecl("kotlin_collections_Iterator_next__reverse_swift")
package func kotlin_collections_Iterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.next()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_ListIterator_hasNext__reverse_swift")
package func kotlin_collections_ListIterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Bool = _self.hasNext()
    return _result
}

@_cdecl("kotlin_collections_ListIterator_hasPrevious__reverse_swift")
package func kotlin_collections_ListIterator_hasPrevious__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Bool = _self.hasPrevious()
    return _result
}

@_cdecl("kotlin_collections_ListIterator_nextIndex__reverse_swift")
package func kotlin_collections_ListIterator_nextIndex__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Int32 = _self.nextIndex()
    return _result
}

@_cdecl("kotlin_collections_ListIterator_next__reverse_swift")
package func kotlin_collections_ListIterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.next()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_ListIterator_previousIndex__reverse_swift")
package func kotlin_collections_ListIterator_previousIndex__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Int32 = _self.previousIndex()
    return _result
}

@_cdecl("kotlin_collections_ListIterator_previous__reverse_swift")
package func kotlin_collections_ListIterator_previous__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.previous()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Bool = _self.containsAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Bool = _self.contains(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self._get(index: index)
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Int32 = _self.indexOf(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_List_isEmpty__reverse_swift")
package func kotlin_collections_List_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@_cdecl("kotlin_collections_List_iterator__reverse_swift")
package func kotlin_collections_List_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: any ExportedKotlinPackages.kotlin.collections.Iterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Int32 = _self.lastIndexOf(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: any ExportedKotlinPackages.kotlin.collections.ListIterator = _self.listIterator(index: index)
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_List_listIterator__reverse_swift")
package func kotlin_collections_List_listIterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: any ExportedKotlinPackages.kotlin.collections.ListIterator = _self.listIterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_List_size_get__reverse_swift")
package func kotlin_collections_List_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Int32 = _self.size
    return _result
}

@_cdecl("kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
package func kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ fromIndex: Swift.Int32, _ toIndex: Swift.Int32) -> Any {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = _self.subList(fromIndex: fromIndex, toIndex: toIndex)
    return _result.map { it in it as! NSObject? ?? NSNull() }
}

@_cdecl("kotlin_collections_LongIterator_nextLong__reverse_swift")
package func kotlin_collections_LongIterator_nextLong__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int64 {
    let _self = ExportedKotlinPackages.kotlin.collections.LongIterator.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int64 = _self.nextLong()
    return _result
}

@_cdecl("kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Bool = _self.addAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Bool = _self.add(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableCollection_clear__reverse_swift")
package func kotlin_collections_MutableCollection_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Void = _self.clear()
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableCollection_iterator__reverse_swift")
package func kotlin_collections_MutableCollection_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Bool = _self.removeAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Bool = _self.remove(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Bool = _self.retainAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableIterable_iterator__reverse_swift")
package func kotlin_collections_MutableIterable_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterable.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterable
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableIterator_remove__reverse_swift")
package func kotlin_collections_MutableIterator_remove__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    let _result: Swift.Void = _self.remove()
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableListIterator_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableListIterator_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    let _result: Swift.Void = _self.add(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableListIterator_hasNext__reverse_swift")
package func kotlin_collections_MutableListIterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    let _result: Swift.Bool = _self.hasNext()
    return _result
}

@_cdecl("kotlin_collections_MutableListIterator_next__reverse_swift")
package func kotlin_collections_MutableListIterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.next()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_MutableListIterator_remove__reverse_swift")
package func kotlin_collections_MutableListIterator_remove__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    let _result: Swift.Void = _self.remove()
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableListIterator_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableListIterator_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    let _result: Swift.Void = _self.set(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.addAll(index: index, elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.addAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Void = _self.add(index: index, element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableList_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableList_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.add(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableList_clear__reverse_swift")
package func kotlin_collections_MutableList_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Void = _self.clear()
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableListIterator = _self.listIterator(index: index)
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableList_listIterator__reverse_swift")
package func kotlin_collections_MutableList_listIterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableListIterator = _self.listIterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.removeAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.removeAt(index: index)
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_MutableList_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableList_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.remove(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.retainAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self._set(index: index, element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ fromIndex: Swift.Int32, _ toIndex: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableList = _self.subList(fromIndex: fromIndex, toIndex: toIndex)
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Bool = _self.addAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Bool = _self.add(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableSet_clear__reverse_swift")
package func kotlin_collections_MutableSet_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Void = _self.clear()
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableSet_iterator__reverse_swift")
package func kotlin_collections_MutableSet_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Bool = _self.removeAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Bool = _self.remove(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Bool = _self.retainAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Set.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Set
    let _result: Swift.Bool = _self.containsAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Set.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Set
    let _result: Swift.Bool = _self.contains(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_Set_isEmpty__reverse_swift")
package func kotlin_collections_Set_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Set.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Set
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@_cdecl("kotlin_collections_Set_iterator__reverse_swift")
package func kotlin_collections_Set_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Set.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Set
    let _result: any ExportedKotlinPackages.kotlin.collections.Iterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_Set_size_get__reverse_swift")
package func kotlin_collections_Set_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Set.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Set
    let _result: Swift.Int32 = _self.size
    return _result
}

@_cdecl("kotlin_coroutines_AbstractCoroutineContextElement_key_get__reverse_swift")
package func kotlin_coroutines_AbstractCoroutineContextElement_key_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement.__createClassWrapper(externalRCRef: `self`)!
    let _result: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key = _self.key
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_ContinuationInterceptor_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift")
package func kotlin_coroutines_ContinuationInterceptor_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor
    let _result: Swift.Optional<any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element> = _self._get(key: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: key, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key)
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_coroutines_ContinuationInterceptor_interceptContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation____reverse_swift")
package func kotlin_coroutines_ContinuationInterceptor_interceptContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor
    let _result: any ExportedKotlinPackages.kotlin.coroutines.Continuation = _self.interceptContinuation(continuation: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: continuation, conformsTo: ExportedKotlinPackages.kotlin.coroutines.Continuation.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.Continuation)
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift")
package func kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor
    let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.minusKey(key: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: key, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key)
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_ContinuationInterceptor_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation____reverse_swift")
package func kotlin_coroutines_ContinuationInterceptor_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor
    let _result: Swift.Void = _self.releaseInterceptedContinuation(continuation: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: continuation, conformsTo: ExportedKotlinPackages.kotlin.coroutines.Continuation.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.Continuation)
    return { _result; return true }()
}

@_cdecl("kotlin_coroutines_Continuation_context_get__reverse_swift")
package func kotlin_coroutines_Continuation_context_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.Continuation.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.Continuation
    let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.context
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_Continuation_resumeWith__TypesOfArguments__ExportedKotlinPackages_kotlin_Result____reverse_swift")
package func kotlin_coroutines_Continuation_resumeWith__TypesOfArguments__ExportedKotlinPackages_kotlin_Result____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ result: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.Continuation.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.Continuation
    let _result: Swift.Void = _self.resumeWith(result: ExportedKotlinPackages.kotlin.Result.__createClassWrapper(externalRCRef: result))
    return { _result; return true }()
}

@_cdecl("kotlin_coroutines_CoroutineContext_Element_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift")
package func kotlin_coroutines_CoroutineContext_Element_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element
    let _result: Swift.Optional<any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element> = _self._get(key: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: key, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key)
    return _result.map { it in it.__externalRCRef() } ?? nil
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
    let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.minusKey(key: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: key, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key)
    return _result.__externalRCRef()
}

@_cdecl("kotlin_coroutines_CoroutineContext_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift")
package func kotlin_coroutines_CoroutineContext_get__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    let _result: Swift.Optional<any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element> = _self._get(key: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: key, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key)
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_coroutines_CoroutineContext_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift")
package func kotlin_coroutines_CoroutineContext_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.minusKey(key: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: key, conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key)
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
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.sequences.Sequence.Type.self) as! any ExportedKotlinPackages.kotlin.sequences.Sequence
    let _result: any ExportedKotlinPackages.kotlin.collections.Iterator = _self.iterator()
    return _result.__externalRCRef()
}
