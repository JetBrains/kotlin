@_exported import ExportedKotlinPackages
import KotlinRuntime
import KotlinRuntimeSupport
@_implementationOnly import KotlinBridges_KotlinStdlib

public protocol _ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry {
    func setValue(
        newValue: KotlinRuntime.KotlinBase?
    ) -> KotlinRuntime.KotlinBase?
}
@objc(__ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry)
protocol __ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry: KotlinStdlib.__ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
public protocol _ExportedKotlinPackages_kotlin_collections_Map_Entry: KotlinRuntime.KotlinBase {
    var key: KotlinRuntime.KotlinBase? {
        get
    }
    var value: KotlinRuntime.KotlinBase? {
        get
    }
}
@objc(__ExportedKotlinPackages_kotlin_collections_Map_Entry)
protocol __ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
public extension ExportedKotlinPackages.kotlin {
    public protocol Annotation: KotlinRuntime.KotlinBase {
    }
    @objc(_Annotation)
    protocol _Annotation {
    }
    public protocol CharSequence: KotlinRuntime.KotlinBase {
        var length: Swift.Int32 {
            get
        }
        func _get(
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit
        func subSequence(
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.CharSequence
        subscript(
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            get
        }
    }
    @objc(_CharSequence)
    protocol _CharSequence {
    }
    public final class Array: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var size: Swift.Int32 {
            get {
                return kotlin_Array_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> KotlinRuntime.KotlinBase? {
            return { switch kotlin_Array_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
        }
        public func _set(
            index: Swift.Int32,
            value: KotlinRuntime.KotlinBase?
        ) -> Swift.Void {
            return kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), index, value.map { it in it.__externalRCRef() } ?? nil)
        }
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_Array_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Optional<KotlinRuntime.KotlinBase>
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public subscript(
            index: Swift.Int32
        ) -> KotlinRuntime.KotlinBase? {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class ByteArray: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var size: Swift.Int32 {
            get {
                return kotlin_ByteArray_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Int8 {
            return kotlin_ByteArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Int8
        ) -> Swift.Void {
            return kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__(self.__externalRCRef(), index, value)
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.ByteIterator {
            return ExportedKotlinPackages.kotlin.collections.ByteIterator.__createClassWrapper(externalRCRef: kotlin_ByteArray_iterator(self.__externalRCRef()))
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int8
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Int8 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class IntArray: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
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
            return kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), index, value)
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
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
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
    public final class Boolean: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public static var shared: ExportedKotlinPackages.kotlin.Boolean.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Boolean.Companion.__createClassWrapper(externalRCRef: kotlin_Boolean_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public func _not() -> Swift.Bool {
            return kotlin_Boolean_not(self.__externalRCRef())
        }
        public static prefix func !(
            this: ExportedKotlinPackages.kotlin.Boolean
        ) -> Swift.Bool {
            this._not()
        }
        public func and(
            other: Swift.Bool
        ) -> Swift.Bool {
            return kotlin_Boolean_and__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.Bool
        ) -> Swift.Bool {
            return kotlin_Boolean_or__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.Bool
        ) -> Swift.Bool {
            return kotlin_Boolean_xor__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), other)
        }
        public func _compareTo(
            other: Swift.Bool
        ) -> Swift.Int32 {
            return kotlin_Boolean_compareTo__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Boolean,
            other: Swift.Bool
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Boolean,
            other: Swift.Bool
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Boolean,
            other: Swift.Bool
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Boolean,
            other: Swift.Bool
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func toString() -> Swift.String {
            return kotlin_Boolean_toString(self.__externalRCRef())
        }
        public func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_Boolean_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Boolean,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_Boolean_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class Char: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public var MIN_VALUE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var MIN_HIGH_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MIN_HIGH_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var MAX_HIGH_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MAX_HIGH_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var MIN_LOW_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MIN_LOW_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var MAX_LOW_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MAX_LOW_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var MIN_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MIN_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var MAX_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MAX_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public var MIN_SUPPLEMENTARY_CODE_POINT: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_MIN_SUPPLEMENTARY_CODE_POINT_get(self.__externalRCRef())
                }
            }
            public var MIN_CODE_POINT: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_MIN_CODE_POINT_get(self.__externalRCRef())
                }
            }
            public var MAX_CODE_POINT: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_MAX_CODE_POINT_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "Introduce your own constant with the value of `2`. Replacement: 2")
            public var MIN_RADIX: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_MIN_RADIX_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "Introduce your own constant with the value of `36. Replacement: 36")
            public var MAX_RADIX: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_MAX_RADIX_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Char.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Char.Companion.__createClassWrapper(externalRCRef: kotlin_Char_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Int32 {
            return kotlin_Char_compareTo__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func inc() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Char_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Char_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> ExportedKotlinPackages.kotlin.ranges.CharRange {
            return ExportedKotlinPackages.kotlin.ranges.CharRange.__createClassWrapper(externalRCRef: kotlin_Char_rangeTo__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> ExportedKotlinPackages.kotlin.ranges.CharRange {
            return ExportedKotlinPackages.kotlin.ranges.CharRange.__createClassWrapper(externalRCRef: kotlin_Char_rangeUntil__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), other))
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code.toByte()")
        public func toByte() -> Swift.Int8 {
            return kotlin_Char_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code.toShort()")
        public func toShort() -> Swift.Int16 {
            return kotlin_Char_toShort(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code")
        public func toInt() -> Swift.Int32 {
            return kotlin_Char_toInt(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code.toLong()")
        public func toLong() -> Swift.Int64 {
            return kotlin_Char_toLong(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code.toFloat()")
        public func toFloat() -> Swift.Float {
            return kotlin_Char_toFloat(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code.toDouble()")
        public func toDouble() -> Swift.Double {
            return kotlin_Char_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_Char_toString(self.__externalRCRef())
        }
        public func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_Char_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Char,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_Char_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Exception: ExportedKotlinPackages.kotlin.Throwable {
        public override init() {
            if Self.self != ExportedKotlinPackages.kotlin.Exception.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ") }
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Exception.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ") }
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Exception.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ") }
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Exception.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ") }
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class RuntimeException: ExportedKotlinPackages.kotlin.Exception {
        public override init() {
            if Self.self != ExportedKotlinPackages.kotlin.RuntimeException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ") }
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.RuntimeException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ") }
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.RuntimeException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ") }
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.RuntimeException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ") }
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class IllegalArgumentException: ExportedKotlinPackages.kotlin.RuntimeException {
        public override init() {
            if Self.self != ExportedKotlinPackages.kotlin.IllegalArgumentException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalArgumentException ") }
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.IllegalArgumentException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalArgumentException ") }
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.IllegalArgumentException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalArgumentException ") }
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.IllegalArgumentException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalArgumentException ") }
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class Byte: ExportedKotlinPackages.kotlin.Number {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public var MIN_VALUE: Swift.Int8 {
                get {
                    return kotlin_Byte_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Int8 {
                get {
                    return kotlin_Byte_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Byte_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Byte_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Byte.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Byte.Companion.__createClassWrapper(externalRCRef: kotlin_Byte_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func inc() -> Swift.Int8 {
            return kotlin_Byte_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int8 {
            return kotlin_Byte_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.Int8
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other))
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Byte_toChar(self.__externalRCRef())
        }
        public override func toShort() -> Swift.Int16 {
            return kotlin_Byte_toShort(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Byte_toInt(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Byte_toLong(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Byte_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Byte_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_Byte_toString(self.__externalRCRef())
        }
        public func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_Byte_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_Byte_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class Short: ExportedKotlinPackages.kotlin.Number {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public var MIN_VALUE: Swift.Int16 {
                get {
                    return kotlin_Short_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Int16 {
                get {
                    return kotlin_Short_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Short_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Short_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Short.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Short.Companion.__createClassWrapper(externalRCRef: kotlin_Short_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func inc() -> Swift.Int16 {
            return kotlin_Short_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int16 {
            return kotlin_Short_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.Int8
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Short_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Short_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Short_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Short_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other))
        }
        public override func toByte() -> Swift.Int8 {
            return kotlin_Short_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Short_toChar(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Short_toInt(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Short_toLong(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Short_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Short_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_Short_toString(self.__externalRCRef())
        }
        public func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_Short_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Short,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_Short_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class Int: ExportedKotlinPackages.kotlin.Number {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public var MIN_VALUE: Swift.Int32 {
                get {
                    return kotlin_Int_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Int32 {
                get {
                    return kotlin_Int_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Int_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Int_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Int.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Int.Companion.__createClassWrapper(externalRCRef: kotlin_Int_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func inc() -> Swift.Int32 {
            return kotlin_Int_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int32 {
            return kotlin_Int_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Int32 {
            return kotlin_Int_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Int
        ) -> Swift.Int32 {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Int32 {
            return kotlin_Int_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Int
        ) -> Swift.Int32 {
            this._unaryMinus()
        }
        public func rangeTo(
            other: Swift.Int8
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Int_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Int_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Int_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Int_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.ranges.IntRange {
            return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other))
        }
        public func shl(
            bitCount: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_shl__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func shr(
            bitCount: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_shr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func ushr(
            bitCount: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_ushr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func and(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_and__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_or__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_xor__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.Int32 {
            return kotlin_Int_inv(self.__externalRCRef())
        }
        public override func toByte() -> Swift.Int8 {
            return kotlin_Int_toByte(self.__externalRCRef())
        }
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Int_toChar(self.__externalRCRef())
        }
        public override func toShort() -> Swift.Int16 {
            return kotlin_Int_toShort(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Int_toLong(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Int_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Int_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_Int_toString(self.__externalRCRef())
        }
        public func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_Int_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Int,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_Int_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class Long: ExportedKotlinPackages.kotlin.Number {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public var MIN_VALUE: Swift.Int64 {
                get {
                    return kotlin_Long_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Int64 {
                get {
                    return kotlin_Long_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Long_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Long_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Long.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Long.Companion.__createClassWrapper(externalRCRef: kotlin_Long_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func inc() -> Swift.Int64 {
            return kotlin_Long_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int64 {
            return kotlin_Long_dec(self.__externalRCRef())
        }
        public func _unaryMinus() -> Swift.Int64 {
            return kotlin_Long_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Long
        ) -> Swift.Int64 {
            this._unaryMinus()
        }
        public func rangeTo(
            other: Swift.Int8
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Long_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Long_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Long_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other))
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Long_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
            return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other))
        }
        public func shl(
            bitCount: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_shl__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func shr(
            bitCount: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_shr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func ushr(
            bitCount: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_ushr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func and(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_and__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_or__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_xor__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.Int64 {
            return kotlin_Long_inv(self.__externalRCRef())
        }
        public override func toByte() -> Swift.Int8 {
            return kotlin_Long_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Long_toChar(self.__externalRCRef())
        }
        public override func toShort() -> Swift.Int16 {
            return kotlin_Long_toShort(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Long_toInt(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Long_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Long_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_Long_toString(self.__externalRCRef())
        }
        public func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_Long_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Long,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_Long_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class Float: ExportedKotlinPackages.kotlin.Number {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public var MIN_VALUE: Swift.Float {
                get {
                    return kotlin_Float_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Float {
                get {
                    return kotlin_Float_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var POSITIVE_INFINITY: Swift.Float {
                get {
                    return kotlin_Float_Companion_POSITIVE_INFINITY_get(self.__externalRCRef())
                }
            }
            public var NEGATIVE_INFINITY: Swift.Float {
                get {
                    return kotlin_Float_Companion_NEGATIVE_INFINITY_get(self.__externalRCRef())
                }
            }
            public var NaN: Swift.Float {
                get {
                    return kotlin_Float_Companion_NaN_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Float_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Float_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Float.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Float.Companion.__createClassWrapper(externalRCRef: kotlin_Float_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Float_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Float_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Float_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Float_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Float_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func inc() -> Swift.Float {
            return kotlin_Float_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Float {
            return kotlin_Float_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Float {
            return kotlin_Float_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Float
        ) -> Swift.Float {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Float {
            return kotlin_Float_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Float
        ) -> Swift.Float {
            this._unaryMinus()
        }
        @available(*, deprecated, message: "Unclear conversion. To achieve the same result convert to Int explicitly and then to Byte.. Replacement: toInt().toByte()")
        public override func toByte() -> Swift.Int8 {
            return kotlin_Float_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Float_toChar(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Unclear conversion. To achieve the same result convert to Int explicitly and then to Short.. Replacement: toInt().toShort()")
        public override func toShort() -> Swift.Int16 {
            return kotlin_Float_toShort(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Float_toInt(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Float_toLong(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Float_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_Float_toString(self.__externalRCRef())
        }
        public func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_Float_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Float,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_Float_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class Double: ExportedKotlinPackages.kotlin.Number {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public var MIN_VALUE: Swift.Double {
                get {
                    return kotlin_Double_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Double {
                get {
                    return kotlin_Double_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var POSITIVE_INFINITY: Swift.Double {
                get {
                    return kotlin_Double_Companion_POSITIVE_INFINITY_get(self.__externalRCRef())
                }
            }
            public var NEGATIVE_INFINITY: Swift.Double {
                get {
                    return kotlin_Double_Companion_NEGATIVE_INFINITY_get(self.__externalRCRef())
                }
            }
            public var NaN: Swift.Double {
                get {
                    return kotlin_Double_Companion_NaN_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Double_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Double_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Double.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Double.Companion.__createClassWrapper(externalRCRef: kotlin_Double_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func inc() -> Swift.Double {
            return kotlin_Double_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Double {
            return kotlin_Double_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Double {
            return kotlin_Double_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Double
        ) -> Swift.Double {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Double {
            return kotlin_Double_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Double
        ) -> Swift.Double {
            this._unaryMinus()
        }
        @available(*, deprecated, message: "Unclear conversion. To achieve the same result convert to Int explicitly and then to Byte.. Replacement: toInt().toByte()")
        public override func toByte() -> Swift.Int8 {
            return kotlin_Double_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Double_toChar(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Unclear conversion. To achieve the same result convert to Int explicitly and then to Short.. Replacement: toInt().toShort()")
        public override func toShort() -> Swift.Int16 {
            return kotlin_Double_toShort(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Double_toInt(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Double_toLong(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Double_toFloat(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_Double_toString(self.__externalRCRef())
        }
        public func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_Double_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Double,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_Double_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class String: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.CharSequence, ExportedKotlinPackages.kotlin._CharSequence, KotlinRuntimeSupport._KotlinBridged {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public static var shared: ExportedKotlinPackages.kotlin.String.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.String.Companion.__createClassWrapper(externalRCRef: kotlin_String_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public var length: Swift.Int32 {
            get {
                return kotlin_String_length_get(self.__externalRCRef())
            }
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_String_hashCode(self.__externalRCRef())
        }
        public func _plus(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.String {
            return kotlin_String_plus__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.String,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.String {
            this._plus(other: other)
        }
        public func toString() -> Swift.String {
            return kotlin_String_toString(self.__externalRCRef())
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_String_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func subSequence(
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.CharSequence {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_String_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), startIndex, endIndex)) as! any ExportedKotlinPackages.kotlin.CharSequence
        }
        public func _compareTo(
            other: Swift.String
        ) -> Swift.Int32 {
            return kotlin_String_compareTo__TypesOfArguments__Swift_String__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.String,
            other: Swift.String
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.String,
            other: Swift.String
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.String,
            other: Swift.String
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.String,
            other: Swift.String
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_String_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.String,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public init() {
            if Self.self != ExportedKotlinPackages.kotlin.String.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.String ") }
            let __kt = kotlin_String_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_String_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            get {
                _get(index: index)
            }
        }
    }
    open class Throwable: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        open var message: Swift.String? {
            get {
                return kotlin_Throwable_message_get(self.__externalRCRef())
            }
        }
        open var cause: ExportedKotlinPackages.kotlin.Throwable? {
            get {
                return { switch kotlin_Throwable_cause_get(self.__externalRCRef()) { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
            }
        }
        public final func getStackTrace() -> ExportedKotlinPackages.kotlin.Array {
            return ExportedKotlinPackages.kotlin.Array.__createClassWrapper(externalRCRef: kotlin_Throwable_getStackTrace(self.__externalRCRef()))
        }
        public final func printStackTrace() -> Swift.Void {
            return kotlin_Throwable_printStackTrace(self.__externalRCRef())
        }
        open func toString() -> Swift.String {
            return kotlin_Throwable_toString(self.__externalRCRef())
        }
        public init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Throwable.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ") }
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Throwable.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ") }
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.Throwable.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ") }
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public init() {
            if Self.self != ExportedKotlinPackages.kotlin.Throwable.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ") }
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Number: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        open func toDouble() -> Swift.Double {
            return kotlin_Number_toDouble(self.__externalRCRef())
        }
        open func toFloat() -> Swift.Float {
            return kotlin_Number_toFloat(self.__externalRCRef())
        }
        open func toLong() -> Swift.Int64 {
            return kotlin_Number_toLong(self.__externalRCRef())
        }
        open func toInt() -> Swift.Int32 {
            return kotlin_Number_toInt(self.__externalRCRef())
        }
        @available(*, deprecated, message: """
Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.
If you override toChar() function in your Number inheritor, it's recommended to gradually deprecate the overriding function and then remove it.
See https://youtrack.jetbrains.com/issue/KT-46465 for details about the migration. Replacement: this.toInt().toChar()
""")
        open func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Number_toChar(self.__externalRCRef())
        }
        open func toShort() -> Swift.Int16 {
            return kotlin_Number_toShort(self.__externalRCRef())
        }
        open func toByte() -> Swift.Int8 {
            return kotlin_Number_toByte(self.__externalRCRef())
        }
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}
public extension ExportedKotlinPackages.kotlin.collections {
    public protocol Iterable: KotlinRuntime.KotlinBase {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    @objc(_Iterable)
    protocol _Iterable {
    }
    public protocol Iterator: KotlinRuntime.KotlinBase {
        func next() -> KotlinRuntime.KotlinBase?
        func hasNext() -> Swift.Bool
    }
    @objc(_Iterator)
    protocol _Iterator {
    }
    public protocol MutableMap: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Map {
        typealias MutableEntry = KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry
        var keys: any ExportedKotlinPackages.kotlin.collections.MutableSet {
            get
        }
        var values: any ExportedKotlinPackages.kotlin.collections.MutableCollection {
            get
        }
        var entries: any ExportedKotlinPackages.kotlin.collections.MutableSet {
            get
        }
        func put(
            key: KotlinRuntime.KotlinBase?,
            value: KotlinRuntime.KotlinBase?
        ) -> KotlinRuntime.KotlinBase?
        func remove(
            key: KotlinRuntime.KotlinBase?
        ) -> KotlinRuntime.KotlinBase?
        func putAll(
            from: [KotlinRuntime.KotlinBase?: KotlinRuntime.KotlinBase?]
        ) -> Swift.Void
        func clear() -> Swift.Void
    }
    @objc(_MutableMap)
    protocol _MutableMap: ExportedKotlinPackages.kotlin.collections._Map {
    }
    public protocol MutableCollection: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.MutableIterable {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator
        func add(
            element: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool
        func remove(
            element: KotlinRuntime.KotlinBase?
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
    @objc(_MutableCollection)
    protocol _MutableCollection: ExportedKotlinPackages.kotlin.collections._Collection, ExportedKotlinPackages.kotlin.collections._MutableIterable {
    }
    public protocol Map: KotlinRuntime.KotlinBase {
        typealias Entry = KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry
        var size: Swift.Int32 {
            get
        }
        var keys: Swift.Set<Swift.Optional<KotlinRuntime.KotlinBase>> {
            get
        }
        var values: any ExportedKotlinPackages.kotlin.collections.Collection {
            get
        }
        var entries: Swift.Set<any KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry> {
            get
        }
        func isEmpty() -> Swift.Bool
        func containsKey(
            key: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool
        func containsValue(
            value: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool
        func _get(
            key: KotlinRuntime.KotlinBase?
        ) -> KotlinRuntime.KotlinBase?
        subscript(
            key: KotlinRuntime.KotlinBase?
        ) -> KotlinRuntime.KotlinBase? {
            get
        }
    }
    @objc(_Map)
    protocol _Map {
    }
    public protocol MutableSet: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Set, ExportedKotlinPackages.kotlin.collections.MutableCollection {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator
        func add(
            element: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool
        func remove(
            element: KotlinRuntime.KotlinBase?
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
    @objc(_MutableSet)
    protocol _MutableSet: ExportedKotlinPackages.kotlin.collections._Set, ExportedKotlinPackages.kotlin.collections._MutableCollection {
    }
    public protocol Collection: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable {
        var size: Swift.Int32 {
            get
        }
        func isEmpty() -> Swift.Bool
        func contains(
            element: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool
        func ~=(
            this: ExportedKotlinPackages.kotlin.collections.Collection,
            element: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
    }
    @objc(_Collection)
    protocol _Collection: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    public protocol MutableIterable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    @objc(_MutableIterable)
    protocol _MutableIterable: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    public protocol MutableIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator {
        func remove() -> Swift.Void
    }
    @objc(_MutableIterator)
    protocol _MutableIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
    public protocol Set: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection {
        var size: Swift.Int32 {
            get
        }
        func isEmpty() -> Swift.Bool
        func contains(
            element: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool
        func ~=(
            this: ExportedKotlinPackages.kotlin.collections.Set,
            element: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
    }
    @objc(_Set)
    protocol _Set: ExportedKotlinPackages.kotlin.collections._Collection {
    }
    open class ByteIterator: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final func next() -> Swift.Int8 {
            return kotlin_collections_ByteIterator_next(self.__externalRCRef())
        }
        open func nextByte() -> Swift.Int8 {
            return kotlin_collections_ByteIterator_nextByte(self.__externalRCRef())
        }
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class IntIterator: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final func next() -> Swift.Int32 {
            return kotlin_collections_IntIterator_next(self.__externalRCRef())
        }
        open func nextInt() -> Swift.Int32 {
            return kotlin_collections_IntIterator_nextInt(self.__externalRCRef())
        }
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class CharIterator: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final func next() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_collections_CharIterator_next(self.__externalRCRef())
        }
        open func nextChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_collections_CharIterator_nextChar(self.__externalRCRef())
        }
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class LongIterator: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final func next() -> Swift.Int64 {
            return kotlin_collections_LongIterator_next(self.__externalRCRef())
        }
        open func nextLong() -> Swift.Int64 {
            return kotlin_collections_LongIterator_nextLong(self.__externalRCRef())
        }
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}
public extension ExportedKotlinPackages.kotlin.Annotation where Self : KotlinRuntimeSupport._KotlinBridged {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.Annotation where Wrapped : ExportedKotlinPackages.kotlin._Annotation {
}
public extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : KotlinRuntimeSupport._KotlinBridged {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Iterable_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterable where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterable {
}
public extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : KotlinRuntimeSupport._KotlinBridged {
    public func next() -> KotlinRuntime.KotlinBase? {
        return { switch kotlin_collections_Iterator_next(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterator where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterator {
}
public extension ExportedKotlinPackages.kotlin.collections.MutableMap where Self : KotlinRuntimeSupport._KotlinBridged {
    public var keys: any ExportedKotlinPackages.kotlin.collections.MutableSet {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableMap_keys_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
        }
    }
    public var values: any ExportedKotlinPackages.kotlin.collections.MutableCollection {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableMap_values_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
        }
    }
    public var entries: any ExportedKotlinPackages.kotlin.collections.MutableSet {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableMap_entries_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
        }
    }
    public func put(
        key: KotlinRuntime.KotlinBase?,
        value: KotlinRuntime.KotlinBase?
    ) -> KotlinRuntime.KotlinBase? {
        return { switch kotlin_collections_MutableMap_put__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), key.map { it in it.__externalRCRef() } ?? nil, value.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
    public func remove(
        key: KotlinRuntime.KotlinBase?
    ) -> KotlinRuntime.KotlinBase? {
        return { switch kotlin_collections_MutableMap_remove__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), key.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
    public func putAll(
        from: [KotlinRuntime.KotlinBase?: KotlinRuntime.KotlinBase?]
    ) -> Swift.Void {
        return kotlin_collections_MutableMap_putAll__TypesOfArguments__Swift_Dictionary_Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase____(self.__externalRCRef(), Dictionary(uniqueKeysWithValues: from.map { key, value in (key as NSObject? ?? NSNull(), value as NSObject? ?? NSNull() )}))
    }
    public func clear() -> Swift.Void {
        return kotlin_collections_MutableMap_clear(self.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableMap where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableMap {
}
public extension KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry where Self : KotlinRuntimeSupport._KotlinBridged {
    public func setValue(
        newValue: KotlinRuntime.KotlinBase?
    ) -> KotlinRuntime.KotlinBase? {
        return { switch kotlin_collections_MutableMap_MutableEntry_setValue__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), newValue.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
}
extension KotlinRuntimeSupport._KotlinExistential: KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry where Wrapped : KotlinStdlib.__ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry {
}
public extension ExportedKotlinPackages.kotlin.ranges {
    public final class CharRange: ExportedKotlinPackages.kotlin.ranges.CharProgression {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public var EMPTY: ExportedKotlinPackages.kotlin.ranges.CharRange {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.CharRange.__createClassWrapper(externalRCRef: kotlin_ranges_CharRange_Companion_EMPTY_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.ranges.CharRange.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.CharRange.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_CharRange_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public var start: Swift.Unicode.UTF16.CodeUnit {
            get {
                return kotlin_ranges_CharRange_start_get(self.__externalRCRef())
            }
        }
        public var endInclusive: Swift.Unicode.UTF16.CodeUnit {
            get {
                return kotlin_ranges_CharRange_endInclusive_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "Can throw an exception when it's impossible to represent the value with Char type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
        public var endExclusive: Swift.Unicode.UTF16.CodeUnit {
            get {
                return kotlin_ranges_CharRange_endExclusive_get(self.__externalRCRef())
            }
        }
        public func contains(
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            return kotlin_ranges_CharRange_contains__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), value)
        }
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.ranges.CharRange,
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            this.contains(value: value)
        }
        public override func isEmpty() -> Swift.Bool {
            return kotlin_ranges_CharRange_isEmpty(self.__externalRCRef())
        }
        public override func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_ranges_CharRange_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.CharRange,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public override func hashCode() -> Swift.Int32 {
            return kotlin_ranges_CharRange_hashCode(self.__externalRCRef())
        }
        public override func toString() -> Swift.String {
            return kotlin_ranges_CharRange_toString(self.__externalRCRef())
        }
        public init(
            start: Swift.Unicode.UTF16.CodeUnit,
            endInclusive: Swift.Unicode.UTF16.CodeUnit
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.ranges.CharRange.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.ranges.CharRange ") }
            let __kt = kotlin_ranges_CharRange_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_ranges_CharRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Unicode_UTF16_CodeUnit_Swift_Unicode_UTF16_CodeUnit__(__kt, start, endInclusive)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class IntRange: ExportedKotlinPackages.kotlin.ranges.IntProgression {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public var EMPTY: ExportedKotlinPackages.kotlin.ranges.IntRange {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_ranges_IntRange_Companion_EMPTY_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.ranges.IntRange.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.IntRange.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_IntRange_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public var start: Swift.Int32 {
            get {
                return kotlin_ranges_IntRange_start_get(self.__externalRCRef())
            }
        }
        public var endInclusive: Swift.Int32 {
            get {
                return kotlin_ranges_IntRange_endInclusive_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "Can throw an exception when it's impossible to represent the value with Int type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
        public var endExclusive: Swift.Int32 {
            get {
                return kotlin_ranges_IntRange_endExclusive_get(self.__externalRCRef())
            }
        }
        public func contains(
            value: Swift.Int32
        ) -> Swift.Bool {
            return kotlin_ranges_IntRange_contains__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), value)
        }
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.ranges.IntRange,
            value: Swift.Int32
        ) -> Swift.Bool {
            this.contains(value: value)
        }
        public override func isEmpty() -> Swift.Bool {
            return kotlin_ranges_IntRange_isEmpty(self.__externalRCRef())
        }
        public override func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_ranges_IntRange_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.IntRange,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public override func hashCode() -> Swift.Int32 {
            return kotlin_ranges_IntRange_hashCode(self.__externalRCRef())
        }
        public override func toString() -> Swift.String {
            return kotlin_ranges_IntRange_toString(self.__externalRCRef())
        }
        public init(
            start: Swift.Int32,
            endInclusive: Swift.Int32
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.ranges.IntRange.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.ranges.IntRange ") }
            let __kt = kotlin_ranges_IntRange_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_ranges_IntRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(__kt, start, endInclusive)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class LongRange: ExportedKotlinPackages.kotlin.ranges.LongProgression {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public var EMPTY: ExportedKotlinPackages.kotlin.ranges.LongRange {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: kotlin_ranges_LongRange_Companion_EMPTY_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.ranges.LongRange.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.LongRange.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_LongRange_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public var start: Swift.Int64 {
            get {
                return kotlin_ranges_LongRange_start_get(self.__externalRCRef())
            }
        }
        public var endInclusive: Swift.Int64 {
            get {
                return kotlin_ranges_LongRange_endInclusive_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "Can throw an exception when it's impossible to represent the value with Long type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
        public var endExclusive: Swift.Int64 {
            get {
                return kotlin_ranges_LongRange_endExclusive_get(self.__externalRCRef())
            }
        }
        public func contains(
            value: Swift.Int64
        ) -> Swift.Bool {
            return kotlin_ranges_LongRange_contains__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), value)
        }
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.ranges.LongRange,
            value: Swift.Int64
        ) -> Swift.Bool {
            this.contains(value: value)
        }
        public override func isEmpty() -> Swift.Bool {
            return kotlin_ranges_LongRange_isEmpty(self.__externalRCRef())
        }
        public override func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_ranges_LongRange_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.LongRange,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public override func hashCode() -> Swift.Int32 {
            return kotlin_ranges_LongRange_hashCode(self.__externalRCRef())
        }
        public override func toString() -> Swift.String {
            return kotlin_ranges_LongRange_toString(self.__externalRCRef())
        }
        public init(
            start: Swift.Int64,
            endInclusive: Swift.Int64
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.ranges.LongRange.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.ranges.LongRange ") }
            let __kt = kotlin_ranges_LongRange_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_ranges_LongRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int64__(__kt, start, endInclusive)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class CharProgression: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public static var shared: ExportedKotlinPackages.kotlin.ranges.CharProgression.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.CharProgression.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_CharProgression_Companion_get())
                }
            }
            public func fromClosedRange(
                rangeStart: Swift.Unicode.UTF16.CodeUnit,
                rangeEnd: Swift.Unicode.UTF16.CodeUnit,
                step: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.ranges.CharProgression {
                return ExportedKotlinPackages.kotlin.ranges.CharProgression.__createClassWrapper(externalRCRef: kotlin_ranges_CharProgression_Companion_fromClosedRange__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit_Swift_Unicode_UTF16_CodeUnit_Swift_Int32__(self.__externalRCRef(), rangeStart, rangeEnd, step))
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public final var first: Swift.Unicode.UTF16.CodeUnit {
            get {
                return kotlin_ranges_CharProgression_first_get(self.__externalRCRef())
            }
        }
        public final var last: Swift.Unicode.UTF16.CodeUnit {
            get {
                return kotlin_ranges_CharProgression_last_get(self.__externalRCRef())
            }
        }
        public final var step: Swift.Int32 {
            get {
                return kotlin_ranges_CharProgression_step_get(self.__externalRCRef())
            }
        }
        open func iterator() -> ExportedKotlinPackages.kotlin.collections.CharIterator {
            return ExportedKotlinPackages.kotlin.collections.CharIterator.__createClassWrapper(externalRCRef: kotlin_ranges_CharProgression_iterator(self.__externalRCRef()))
        }
        open func isEmpty() -> Swift.Bool {
            return kotlin_ranges_CharProgression_isEmpty(self.__externalRCRef())
        }
        open func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_ranges_CharProgression_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.CharProgression,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        open func hashCode() -> Swift.Int32 {
            return kotlin_ranges_CharProgression_hashCode(self.__externalRCRef())
        }
        open func toString() -> Swift.String {
            return kotlin_ranges_CharProgression_toString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class IntProgression: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public static var shared: ExportedKotlinPackages.kotlin.ranges.IntProgression.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.IntProgression.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_IntProgression_Companion_get())
                }
            }
            public func fromClosedRange(
                rangeStart: Swift.Int32,
                rangeEnd: Swift.Int32,
                step: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.ranges.IntProgression {
                return ExportedKotlinPackages.kotlin.ranges.IntProgression.__createClassWrapper(externalRCRef: kotlin_ranges_IntProgression_Companion_fromClosedRange__TypesOfArguments__Swift_Int32_Swift_Int32_Swift_Int32__(self.__externalRCRef(), rangeStart, rangeEnd, step))
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public final var first: Swift.Int32 {
            get {
                return kotlin_ranges_IntProgression_first_get(self.__externalRCRef())
            }
        }
        public final var last: Swift.Int32 {
            get {
                return kotlin_ranges_IntProgression_last_get(self.__externalRCRef())
            }
        }
        public final var step: Swift.Int32 {
            get {
                return kotlin_ranges_IntProgression_step_get(self.__externalRCRef())
            }
        }
        open func iterator() -> ExportedKotlinPackages.kotlin.collections.IntIterator {
            return ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: kotlin_ranges_IntProgression_iterator(self.__externalRCRef()))
        }
        open func isEmpty() -> Swift.Bool {
            return kotlin_ranges_IntProgression_isEmpty(self.__externalRCRef())
        }
        open func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_ranges_IntProgression_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.IntProgression,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        open func hashCode() -> Swift.Int32 {
            return kotlin_ranges_IntProgression_hashCode(self.__externalRCRef())
        }
        open func toString() -> Swift.String {
            return kotlin_ranges_IntProgression_toString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class LongProgression: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public static var shared: ExportedKotlinPackages.kotlin.ranges.LongProgression.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.LongProgression.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_LongProgression_Companion_get())
                }
            }
            public func fromClosedRange(
                rangeStart: Swift.Int64,
                rangeEnd: Swift.Int64,
                step: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.ranges.LongProgression {
                return ExportedKotlinPackages.kotlin.ranges.LongProgression.__createClassWrapper(externalRCRef: kotlin_ranges_LongProgression_Companion_fromClosedRange__TypesOfArguments__Swift_Int64_Swift_Int64_Swift_Int64__(self.__externalRCRef(), rangeStart, rangeEnd, step))
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            private init() {
                fatalError()
            }
        }
        public final var first: Swift.Int64 {
            get {
                return kotlin_ranges_LongProgression_first_get(self.__externalRCRef())
            }
        }
        public final var last: Swift.Int64 {
            get {
                return kotlin_ranges_LongProgression_last_get(self.__externalRCRef())
            }
        }
        public final var step: Swift.Int64 {
            get {
                return kotlin_ranges_LongProgression_step_get(self.__externalRCRef())
            }
        }
        open func iterator() -> ExportedKotlinPackages.kotlin.collections.LongIterator {
            return ExportedKotlinPackages.kotlin.collections.LongIterator.__createClassWrapper(externalRCRef: kotlin_ranges_LongProgression_iterator(self.__externalRCRef()))
        }
        open func isEmpty() -> Swift.Bool {
            return kotlin_ranges_LongProgression_isEmpty(self.__externalRCRef())
        }
        open func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_ranges_LongProgression_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.LongProgression,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        open func hashCode() -> Swift.Int32 {
            return kotlin_ranges_LongProgression_hashCode(self.__externalRCRef())
        }
        open func toString() -> Swift.String {
            return kotlin_ranges_LongProgression_toString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}
public extension ExportedKotlinPackages.kotlin.CharSequence where Self : KotlinRuntimeSupport._KotlinBridged {
    public var length: Swift.Int32 {
        get {
            return kotlin_CharSequence_length_get(self.__externalRCRef())
        }
    }
    public func _get(
        index: Swift.Int32
    ) -> Swift.Unicode.UTF16.CodeUnit {
        return kotlin_CharSequence_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
    }
    public func subSequence(
        startIndex: Swift.Int32,
        endIndex: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.CharSequence {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_CharSequence_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), startIndex, endIndex)) as! any ExportedKotlinPackages.kotlin.CharSequence
    }
    public subscript(
        index: Swift.Int32
    ) -> Swift.Unicode.UTF16.CodeUnit {
        get {
            _get(index: index)
        }
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.CharSequence where Wrapped : ExportedKotlinPackages.kotlin._CharSequence {
}
public extension ExportedKotlinPackages.kotlin.collections.MutableCollection where Self : KotlinRuntimeSupport._KotlinBridged {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableCollection_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    public func add(
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func remove(
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
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
        return kotlin_collections_MutableCollection_clear(self.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableCollection where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableCollection {
}
public extension ExportedKotlinPackages.kotlin.collections.Map where Self : KotlinRuntimeSupport._KotlinBridged {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Map_size_get(self.__externalRCRef())
        }
    }
    public var keys: Swift.Set<Swift.Optional<KotlinRuntime.KotlinBase>> {
        get {
            return kotlin_collections_Map_keys_get(self.__externalRCRef()) as! Swift.Set<Swift.Optional<KotlinRuntime.KotlinBase>>
        }
    }
    public var values: any ExportedKotlinPackages.kotlin.collections.Collection {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Map_values_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Collection
        }
    }
    public var entries: Swift.Set<any KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry> {
        get {
            return kotlin_collections_Map_entries_get(self.__externalRCRef()) as! Swift.Set<any KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry>
        }
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Map_isEmpty(self.__externalRCRef())
    }
    public func containsKey(
        key: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return kotlin_collections_Map_containsKey__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), key.map { it in it.__externalRCRef() } ?? nil)
    }
    public func containsValue(
        value: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return kotlin_collections_Map_containsValue__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil)
    }
    public func _get(
        key: KotlinRuntime.KotlinBase?
    ) -> KotlinRuntime.KotlinBase? {
        return { switch kotlin_collections_Map_get__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), key.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
    public subscript(
        key: KotlinRuntime.KotlinBase?
    ) -> KotlinRuntime.KotlinBase? {
        get {
            _get(key: key)
        }
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Map where Wrapped : ExportedKotlinPackages.kotlin.collections._Map {
}
public extension ExportedKotlinPackages.kotlin.collections.MutableSet where Self : KotlinRuntimeSupport._KotlinBridged {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableSet_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    public func add(
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func remove(
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
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
        return kotlin_collections_MutableSet_clear(self.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableSet where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableSet {
}
public extension KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry where Self : KotlinRuntimeSupport._KotlinBridged {
    public var key: KotlinRuntime.KotlinBase? {
        get {
            return { switch kotlin_collections_Map_Entry_key_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
        }
    }
    public var value: KotlinRuntime.KotlinBase? {
        get {
            return { switch kotlin_collections_Map_Entry_value_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
        }
    }
}
extension KotlinRuntimeSupport._KotlinExistential: KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry where Wrapped : KotlinStdlib.__ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
public extension ExportedKotlinPackages.kotlin.collections.Collection where Self : KotlinRuntimeSupport._KotlinBridged {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Collection_size_get(self.__externalRCRef())
        }
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Collection_isEmpty(self.__externalRCRef())
    }
    public func contains(
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public static func ~=(
        this: ExportedKotlinPackages.kotlin.collections.Collection,
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Collection_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Collection where Wrapped : ExportedKotlinPackages.kotlin.collections._Collection {
}
public extension ExportedKotlinPackages.kotlin.collections.MutableIterable where Self : KotlinRuntimeSupport._KotlinBridged {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableIterable_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterable where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterable {
}
public extension ExportedKotlinPackages.kotlin.collections.MutableIterator where Self : KotlinRuntimeSupport._KotlinBridged {
    public func remove() -> Swift.Void {
        return kotlin_collections_MutableIterator_remove(self.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterator where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterator {
}
public extension ExportedKotlinPackages.kotlin.collections.Set where Self : KotlinRuntimeSupport._KotlinBridged {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Set_size_get(self.__externalRCRef())
        }
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Set_isEmpty(self.__externalRCRef())
    }
    public func contains(
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public static func ~=(
        this: ExportedKotlinPackages.kotlin.collections.Set,
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Set_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Set where Wrapped : ExportedKotlinPackages.kotlin.collections._Set {
}
