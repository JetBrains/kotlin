@_exported import ExportedKotlinPackages
import KotlinRuntime
import KotlinRuntimeSupport
@_implementationOnly import KotlinBridges_KotlinStdlib

extension ExportedKotlinPackages.kotlin {
    public protocol Annotation: KotlinRuntime.KotlinBase {
    }
    @objc(_Annotation)
    package protocol _Annotation {
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
    }
    @objc(_CharSequence)
    package protocol _CharSequence {
    }
    public final class Array: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_Array_size_get(self.__externalRCRef())
            }
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
            return kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, value.map { it in it.__externalRCRef() } ?? nil)
        }
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_Array_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
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
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
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
    public final class Boolean: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
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
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_Boolean_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Boolean,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
    public final class Char: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
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
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Char_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Int32 {
            return kotlin_Char_minus__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Char_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            this._minus(other: other)
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
        public func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Char_toChar(self.__externalRCRef())
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
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_Char_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Char,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
        public final class Companion: KotlinRuntime.KotlinBase {
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
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Byte_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Byte_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Byte_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Byte_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Byte_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Byte_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func inc() -> Swift.Int8 {
            return kotlin_Byte_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int8 {
            return kotlin_Byte_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Int32 {
            return kotlin_Byte_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Byte
        ) -> Swift.Int32 {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Int32 {
            return kotlin_Byte_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Byte
        ) -> Swift.Int32 {
            this._unaryMinus()
        }
        public func rangeTo(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public override func toByte() -> Swift.Int8 {
            return kotlin_Byte_toByte(self.__externalRCRef())
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
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_Byte_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
        public final class Companion: KotlinRuntime.KotlinBase {
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
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
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
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Short_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Short_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Short_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Short_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Short_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Short_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Short_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Short_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Short_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Short_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Short_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Short_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Short_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Short_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Short_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func inc() -> Swift.Int16 {
            return kotlin_Short_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int16 {
            return kotlin_Short_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Int32 {
            return kotlin_Short_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Short
        ) -> Swift.Int32 {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Int32 {
            return kotlin_Short_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Short
        ) -> Swift.Int32 {
            this._unaryMinus()
        }
        public func rangeTo(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Short_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Short_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Short_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Short_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public override func toByte() -> Swift.Int8 {
            return kotlin_Short_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Short_toChar(self.__externalRCRef())
        }
        public override func toShort() -> Swift.Int16 {
            return kotlin_Short_toShort(self.__externalRCRef())
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
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_Short_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Short,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
        public final class Companion: KotlinRuntime.KotlinBase {
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
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
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
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._plus(other: other)
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
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Int_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Int_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Int_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._minus(other: other)
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
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Int_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Int_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Int_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._times(other: other)
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
        public func _times(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Int_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Int_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Int_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._div(other: other)
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
        public func _div(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Int_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Int_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Int_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._rem(other: other)
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
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Int_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Int_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Int_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Double {
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
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Int_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Int_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Int_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Int_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            let _result = kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
            return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_result) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
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
        public override func toInt() -> Swift.Int32 {
            return kotlin_Int_toInt(self.__externalRCRef())
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
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_Int_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Int,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
        public final class Companion: KotlinRuntime.KotlinBase {
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
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
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
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int8
        ) -> Swift.Int64 {
            return kotlin_Long_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Int64 {
            return kotlin_Long_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Int64 {
            this._plus(other: other)
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
        public func _plus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Long_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Long_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Int64 {
            return kotlin_Long_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Int64 {
            return kotlin_Long_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Int64 {
            this._minus(other: other)
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
        public func _minus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Long_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Long_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Int64 {
            return kotlin_Long_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Int64 {
            return kotlin_Long_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Int64 {
            this._times(other: other)
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
        public func _times(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Long_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Long_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Int64 {
            return kotlin_Long_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Int64 {
            return kotlin_Long_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Int64 {
            this._div(other: other)
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
        public func _div(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Long_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Long_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Int64 {
            return kotlin_Long_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Int64 {
            return kotlin_Long_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Int64 {
            this._rem(other: other)
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
        public func _rem(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Long_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Long_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func inc() -> Swift.Int64 {
            return kotlin_Long_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int64 {
            return kotlin_Long_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Int64 {
            return kotlin_Long_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Long
        ) -> Swift.Int64 {
            this._unaryPlus()
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
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Long_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Long_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Long_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Long_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            let _result = kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
            return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_result) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_result)
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
        public override func toLong() -> Swift.Int64 {
            return kotlin_Long_toLong(self.__externalRCRef())
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
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_Long_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Long,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
        public final class Companion: KotlinRuntime.KotlinBase {
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
            other: Swift.Int8
        ) -> Swift.Float {
            return kotlin_Float_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Float {
            return kotlin_Float_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Float {
            return kotlin_Float_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Float {
            return kotlin_Float_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Float {
            this._plus(other: other)
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
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Float_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Float {
            return kotlin_Float_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Float {
            return kotlin_Float_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Float {
            return kotlin_Float_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Float {
            return kotlin_Float_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Float {
            this._minus(other: other)
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
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Float_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Float {
            return kotlin_Float_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Float {
            return kotlin_Float_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Float {
            return kotlin_Float_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Float {
            return kotlin_Float_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Float {
            this._times(other: other)
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
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Float_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Float {
            return kotlin_Float_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Float {
            return kotlin_Float_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Float {
            return kotlin_Float_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Float {
            return kotlin_Float_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Float {
            this._div(other: other)
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
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Float_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Float {
            return kotlin_Float_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Float {
            return kotlin_Float_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Float {
            return kotlin_Float_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Float {
            return kotlin_Float_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Float {
            this._rem(other: other)
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
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Float_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Double {
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
        public override func toFloat() -> Swift.Float {
            return kotlin_Float_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Float_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_Float_toString(self.__externalRCRef())
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_Float_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Float,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
        public final class Companion: KotlinRuntime.KotlinBase {
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
            other: Swift.Int8
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Double {
            this._plus(other: other)
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
            other: Swift.Int8
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Double {
            this._minus(other: other)
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
            other: Swift.Int8
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Double {
            this._times(other: other)
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
            other: Swift.Int8
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Double {
            this._div(other: other)
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
            other: Swift.Int8
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Double {
            this._rem(other: other)
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
        public override func toDouble() -> Swift.Double {
            return kotlin_Double_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_Double_toString(self.__externalRCRef())
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_Double_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Double,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
    public final class String: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.CharSequence, ExportedKotlinPackages.kotlin._CharSequence {
        public final class Companion: KotlinRuntime.KotlinBase {
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
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.String {
            return kotlin_String_plus__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.String,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_String_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.String,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
    open class Throwable: KotlinRuntime.KotlinBase {
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
    public final class UByte: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
        }
        public func _compareTo(
            other: Swift.UInt8
        ) -> Swift.Int32 {
            return kotlin_UByte_compareTo__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt16
        ) -> Swift.Int32 {
            return kotlin_UByte_compareTo__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt32
        ) -> Swift.Int32 {
            return kotlin_UByte_compareTo__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt64
        ) -> Swift.Int32 {
            return kotlin_UByte_compareTo__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_plus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_plus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_plus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_plus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_minus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_minus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_minus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_minus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_times__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_times__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_times__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_times__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _div(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_div__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_div__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_div__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_div__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_rem__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_rem__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_rem__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_rem__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func floorDiv(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_floorDiv__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_floorDiv__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_floorDiv__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_floorDiv__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UByte_mod__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UByte_mod__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_mod__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_mod__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func inc() -> Swift.UInt8 {
            return kotlin_UByte_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.UInt8 {
            return kotlin_UByte_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.UInt8
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UByte_rangeTo__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.UInt8
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UByte_rangeUntil__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other))
        }
        public func and(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UByte_and__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UByte_or__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UByte_xor__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.UInt8 {
            return kotlin_UByte_inv(self.__externalRCRef())
        }
        public func toByte() -> Swift.Int8 {
            return kotlin_UByte_toByte(self.__externalRCRef())
        }
        public func toShort() -> Swift.Int16 {
            return kotlin_UByte_toShort(self.__externalRCRef())
        }
        public func toInt() -> Swift.Int32 {
            return kotlin_UByte_toInt(self.__externalRCRef())
        }
        public func toLong() -> Swift.Int64 {
            return kotlin_UByte_toLong(self.__externalRCRef())
        }
        public func toUByte() -> Swift.UInt8 {
            return kotlin_UByte_toUByte(self.__externalRCRef())
        }
        public func toUShort() -> Swift.UInt16 {
            return kotlin_UByte_toUShort(self.__externalRCRef())
        }
        public func toUInt() -> Swift.UInt32 {
            return kotlin_UByte_toUInt(self.__externalRCRef())
        }
        public func toULong() -> Swift.UInt64 {
            return kotlin_UByte_toULong(self.__externalRCRef())
        }
        public func toFloat() -> Swift.Float {
            return kotlin_UByte_toFloat(self.__externalRCRef())
        }
        public func toDouble() -> Swift.Double {
            return kotlin_UByte_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_UByte_toString(self.__externalRCRef())
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_UByte_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_UByte_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class UInt: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
        }
        public func _compareTo(
            other: Swift.UInt8
        ) -> Swift.Int32 {
            return kotlin_UInt_compareTo__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt16
        ) -> Swift.Int32 {
            return kotlin_UInt_compareTo__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt32
        ) -> Swift.Int32 {
            return kotlin_UInt_compareTo__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt64
        ) -> Swift.Int32 {
            return kotlin_UInt_compareTo__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_plus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_plus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_plus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_plus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_minus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_minus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_minus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_minus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_times__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_times__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_times__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_times__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _div(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_div__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_div__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_div__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_div__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_rem__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_rem__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_rem__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_rem__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func floorDiv(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_floorDiv__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_floorDiv__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_floorDiv__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_floorDiv__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UInt_mod__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UInt_mod__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_mod__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_mod__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func inc() -> Swift.UInt32 {
            return kotlin_UInt_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.UInt32 {
            return kotlin_UInt_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.UInt32
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UInt_rangeTo__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.UInt32
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UInt_rangeUntil__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other))
        }
        public func shl(
            bitCount: Swift.Int32
        ) -> Swift.UInt32 {
            return kotlin_UInt_shl__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func shr(
            bitCount: Swift.Int32
        ) -> Swift.UInt32 {
            return kotlin_UInt_shr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func and(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_and__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_or__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_xor__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.UInt32 {
            return kotlin_UInt_inv(self.__externalRCRef())
        }
        public func toByte() -> Swift.Int8 {
            return kotlin_UInt_toByte(self.__externalRCRef())
        }
        public func toShort() -> Swift.Int16 {
            return kotlin_UInt_toShort(self.__externalRCRef())
        }
        public func toInt() -> Swift.Int32 {
            return kotlin_UInt_toInt(self.__externalRCRef())
        }
        public func toLong() -> Swift.Int64 {
            return kotlin_UInt_toLong(self.__externalRCRef())
        }
        public func toUByte() -> Swift.UInt8 {
            return kotlin_UInt_toUByte(self.__externalRCRef())
        }
        public func toUShort() -> Swift.UInt16 {
            return kotlin_UInt_toUShort(self.__externalRCRef())
        }
        public func toUInt() -> Swift.UInt32 {
            return kotlin_UInt_toUInt(self.__externalRCRef())
        }
        public func toULong() -> Swift.UInt64 {
            return kotlin_UInt_toULong(self.__externalRCRef())
        }
        public func toFloat() -> Swift.Float {
            return kotlin_UInt_toFloat(self.__externalRCRef())
        }
        public func toDouble() -> Swift.Double {
            return kotlin_UInt_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_UInt_toString(self.__externalRCRef())
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_UInt_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_UInt_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class ULong: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
        }
        public func _compareTo(
            other: Swift.UInt8
        ) -> Swift.Int32 {
            return kotlin_ULong_compareTo__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt16
        ) -> Swift.Int32 {
            return kotlin_ULong_compareTo__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt32
        ) -> Swift.Int32 {
            return kotlin_ULong_compareTo__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt64
        ) -> Swift.Int32 {
            return kotlin_ULong_compareTo__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_plus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_plus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_plus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_plus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_minus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_minus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_minus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_minus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_times__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_times__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_times__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_times__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _div(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_div__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_div__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_div__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_div__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_rem__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_rem__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_rem__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_rem__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func floorDiv(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_floorDiv__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_floorDiv__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_floorDiv__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_floorDiv__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_ULong_mod__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_ULong_mod__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_ULong_mod__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_mod__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func inc() -> Swift.UInt64 {
            return kotlin_ULong_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.UInt64 {
            return kotlin_ULong_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.UInt64
        ) -> ExportedKotlinPackages.kotlin.ranges.ULongRange {
            return ExportedKotlinPackages.kotlin.ranges.ULongRange.__createClassWrapper(externalRCRef: kotlin_ULong_rangeTo__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.UInt64
        ) -> ExportedKotlinPackages.kotlin.ranges.ULongRange {
            return ExportedKotlinPackages.kotlin.ranges.ULongRange.__createClassWrapper(externalRCRef: kotlin_ULong_rangeUntil__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other))
        }
        public func shl(
            bitCount: Swift.Int32
        ) -> Swift.UInt64 {
            return kotlin_ULong_shl__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func shr(
            bitCount: Swift.Int32
        ) -> Swift.UInt64 {
            return kotlin_ULong_shr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func and(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_and__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_or__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_xor__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.UInt64 {
            return kotlin_ULong_inv(self.__externalRCRef())
        }
        public func toByte() -> Swift.Int8 {
            return kotlin_ULong_toByte(self.__externalRCRef())
        }
        public func toShort() -> Swift.Int16 {
            return kotlin_ULong_toShort(self.__externalRCRef())
        }
        public func toInt() -> Swift.Int32 {
            return kotlin_ULong_toInt(self.__externalRCRef())
        }
        public func toLong() -> Swift.Int64 {
            return kotlin_ULong_toLong(self.__externalRCRef())
        }
        public func toUByte() -> Swift.UInt8 {
            return kotlin_ULong_toUByte(self.__externalRCRef())
        }
        public func toUShort() -> Swift.UInt16 {
            return kotlin_ULong_toUShort(self.__externalRCRef())
        }
        public func toUInt() -> Swift.UInt32 {
            return kotlin_ULong_toUInt(self.__externalRCRef())
        }
        public func toULong() -> Swift.UInt64 {
            return kotlin_ULong_toULong(self.__externalRCRef())
        }
        public func toFloat() -> Swift.Float {
            return kotlin_ULong_toFloat(self.__externalRCRef())
        }
        public func toDouble() -> Swift.Double {
            return kotlin_ULong_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_ULong_toString(self.__externalRCRef())
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_ULong_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_ULong_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class UShort: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
        }
        public func _compareTo(
            other: Swift.UInt8
        ) -> Swift.Int32 {
            return kotlin_UShort_compareTo__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt16
        ) -> Swift.Int32 {
            return kotlin_UShort_compareTo__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt32
        ) -> Swift.Int32 {
            return kotlin_UShort_compareTo__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt64
        ) -> Swift.Int32 {
            return kotlin_UShort_compareTo__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_plus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_plus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_plus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_plus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_minus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_minus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_minus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_minus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_times__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_times__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_times__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_times__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _div(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_div__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_div__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_div__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_div__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_rem__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_rem__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_rem__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_rem__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func floorDiv(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_floorDiv__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_floorDiv__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_floorDiv__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_floorDiv__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UShort_mod__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UShort_mod__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_mod__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_mod__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func inc() -> Swift.UInt16 {
            return kotlin_UShort_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.UInt16 {
            return kotlin_UShort_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.UInt16
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UShort_rangeTo__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.UInt16
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UShort_rangeUntil__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other))
        }
        public func and(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UShort_and__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UShort_or__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UShort_xor__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.UInt16 {
            return kotlin_UShort_inv(self.__externalRCRef())
        }
        public func toByte() -> Swift.Int8 {
            return kotlin_UShort_toByte(self.__externalRCRef())
        }
        public func toShort() -> Swift.Int16 {
            return kotlin_UShort_toShort(self.__externalRCRef())
        }
        public func toInt() -> Swift.Int32 {
            return kotlin_UShort_toInt(self.__externalRCRef())
        }
        public func toLong() -> Swift.Int64 {
            return kotlin_UShort_toLong(self.__externalRCRef())
        }
        public func toUByte() -> Swift.UInt8 {
            return kotlin_UShort_toUByte(self.__externalRCRef())
        }
        public func toUShort() -> Swift.UInt16 {
            return kotlin_UShort_toUShort(self.__externalRCRef())
        }
        public func toUInt() -> Swift.UInt32 {
            return kotlin_UShort_toUInt(self.__externalRCRef())
        }
        public func toULong() -> Swift.UInt64 {
            return kotlin_UShort_toULong(self.__externalRCRef())
        }
        public func toFloat() -> Swift.Float {
            return kotlin_UShort_toFloat(self.__externalRCRef())
        }
        public func toDouble() -> Swift.Double {
            return kotlin_UShort_toDouble(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlin_UShort_toString(self.__externalRCRef())
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_UShort_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlin_UShort_hashCode(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Number: KotlinRuntime.KotlinBase {
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
extension ExportedKotlinPackages.kotlin.collections {
    public protocol Iterable: KotlinRuntime.KotlinBase {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    @objc(_Iterable)
    package protocol _Iterable {
    }
    public protocol Iterator: KotlinRuntime.KotlinBase {
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func hasNext() -> Swift.Bool
    }
    @objc(_Iterator)
    package protocol _Iterator {
    }
    open class IntIterator: KotlinRuntime.KotlinBase {
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
    open class CharIterator: KotlinRuntime.KotlinBase {
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
}
extension ExportedKotlinPackages.kotlin.Annotation where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.Annotation where Wrapped : ExportedKotlinPackages.kotlin._Annotation {
}
extension ExportedKotlinPackages.kotlin.Annotation {
}
extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Iterable_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterable where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterable {
}
extension ExportedKotlinPackages.kotlin.collections.Iterable {
}
extension ExportedKotlinPackages.kotlin.ranges {
    public final class CharRange: ExportedKotlinPackages.kotlin.ranges.CharProgression {
        public final class Companion: KotlinRuntime.KotlinBase {
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
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_ranges_CharRange_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.CharRange,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
    public final class UIntRange: ExportedKotlinPackages.kotlin.ranges.UIntProgression {
        public final class Companion: KotlinRuntime.KotlinBase {
            public var EMPTY: ExportedKotlinPackages.kotlin.ranges.UIntRange {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_ranges_UIntRange_Companion_EMPTY_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.ranges.UIntRange.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.UIntRange.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_UIntRange_Companion_get())
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
        public var start: Swift.UInt32 {
            get {
                return kotlin_ranges_UIntRange_start_get(self.__externalRCRef())
            }
        }
        public var endInclusive: Swift.UInt32 {
            get {
                return kotlin_ranges_UIntRange_endInclusive_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "Can throw an exception when it's impossible to represent the value with UInt type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
        public var endExclusive: Swift.UInt32 {
            get {
                return kotlin_ranges_UIntRange_endExclusive_get(self.__externalRCRef())
            }
        }
        public func contains(
            value: Swift.UInt32
        ) -> Swift.Bool {
            return kotlin_ranges_UIntRange_contains__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), value)
        }
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.ranges.UIntRange,
            value: Swift.UInt32
        ) -> Swift.Bool {
            this.contains(value: value)
        }
        public override func isEmpty() -> Swift.Bool {
            return kotlin_ranges_UIntRange_isEmpty(self.__externalRCRef())
        }
        public override func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_ranges_UIntRange_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.UIntRange,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public override func hashCode() -> Swift.Int32 {
            return kotlin_ranges_UIntRange_hashCode(self.__externalRCRef())
        }
        public override func toString() -> Swift.String {
            return kotlin_ranges_UIntRange_toString(self.__externalRCRef())
        }
        public init(
            start: Swift.UInt32,
            endInclusive: Swift.UInt32
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.ranges.UIntRange.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.ranges.UIntRange ") }
            let __kt = kotlin_ranges_UIntRange_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_ranges_UIntRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt32_Swift_UInt32__(__kt, start, endInclusive)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class ULongRange: ExportedKotlinPackages.kotlin.ranges.ULongProgression {
        public final class Companion: KotlinRuntime.KotlinBase {
            public var EMPTY: ExportedKotlinPackages.kotlin.ranges.ULongRange {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.ULongRange.__createClassWrapper(externalRCRef: kotlin_ranges_ULongRange_Companion_EMPTY_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.ranges.ULongRange.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.ULongRange.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_ULongRange_Companion_get())
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
        public var start: Swift.UInt64 {
            get {
                return kotlin_ranges_ULongRange_start_get(self.__externalRCRef())
            }
        }
        public var endInclusive: Swift.UInt64 {
            get {
                return kotlin_ranges_ULongRange_endInclusive_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "Can throw an exception when it's impossible to represent the value with ULong type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
        public var endExclusive: Swift.UInt64 {
            get {
                return kotlin_ranges_ULongRange_endExclusive_get(self.__externalRCRef())
            }
        }
        public func contains(
            value: Swift.UInt64
        ) -> Swift.Bool {
            return kotlin_ranges_ULongRange_contains__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), value)
        }
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.ranges.ULongRange,
            value: Swift.UInt64
        ) -> Swift.Bool {
            this.contains(value: value)
        }
        public override func isEmpty() -> Swift.Bool {
            return kotlin_ranges_ULongRange_isEmpty(self.__externalRCRef())
        }
        public override func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_ranges_ULongRange_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.ULongRange,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public override func hashCode() -> Swift.Int32 {
            return kotlin_ranges_ULongRange_hashCode(self.__externalRCRef())
        }
        public override func toString() -> Swift.String {
            return kotlin_ranges_ULongRange_toString(self.__externalRCRef())
        }
        public init(
            start: Swift.UInt64,
            endInclusive: Swift.UInt64
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.ranges.ULongRange.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.ranges.ULongRange ") }
            let __kt = kotlin_ranges_ULongRange_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_ranges_ULongRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt64_Swift_UInt64__(__kt, start, endInclusive)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class CharProgression: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
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
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_ranges_CharProgression_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.CharProgression,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
    open class UIntProgression: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlin.ranges.UIntProgression.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.UIntProgression.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_UIntProgression_Companion_get())
                }
            }
            public func fromClosedRange(
                rangeStart: Swift.UInt32,
                rangeEnd: Swift.UInt32,
                step: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.ranges.UIntProgression {
                return ExportedKotlinPackages.kotlin.ranges.UIntProgression.__createClassWrapper(externalRCRef: kotlin_ranges_UIntProgression_Companion_fromClosedRange__TypesOfArguments__Swift_UInt32_Swift_UInt32_Swift_Int32__(self.__externalRCRef(), rangeStart, rangeEnd, step))
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
        public final var first: Swift.UInt32 {
            get {
                return kotlin_ranges_UIntProgression_first_get(self.__externalRCRef())
            }
        }
        public final var last: Swift.UInt32 {
            get {
                return kotlin_ranges_UIntProgression_last_get(self.__externalRCRef())
            }
        }
        public final var step: Swift.Int32 {
            get {
                return kotlin_ranges_UIntProgression_step_get(self.__externalRCRef())
            }
        }
        public final func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_ranges_UIntProgression_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
        }
        open func isEmpty() -> Swift.Bool {
            return kotlin_ranges_UIntProgression_isEmpty(self.__externalRCRef())
        }
        open func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_ranges_UIntProgression_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.UIntProgression,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        open func hashCode() -> Swift.Int32 {
            return kotlin_ranges_UIntProgression_hashCode(self.__externalRCRef())
        }
        open func toString() -> Swift.String {
            return kotlin_ranges_UIntProgression_toString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class ULongProgression: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlin.ranges.ULongProgression.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.ULongProgression.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_ULongProgression_Companion_get())
                }
            }
            public func fromClosedRange(
                rangeStart: Swift.UInt64,
                rangeEnd: Swift.UInt64,
                step: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.ranges.ULongProgression {
                return ExportedKotlinPackages.kotlin.ranges.ULongProgression.__createClassWrapper(externalRCRef: kotlin_ranges_ULongProgression_Companion_fromClosedRange__TypesOfArguments__Swift_UInt64_Swift_UInt64_Swift_Int64__(self.__externalRCRef(), rangeStart, rangeEnd, step))
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
        public final var first: Swift.UInt64 {
            get {
                return kotlin_ranges_ULongProgression_first_get(self.__externalRCRef())
            }
        }
        public final var last: Swift.UInt64 {
            get {
                return kotlin_ranges_ULongProgression_last_get(self.__externalRCRef())
            }
        }
        public final var step: Swift.Int64 {
            get {
                return kotlin_ranges_ULongProgression_step_get(self.__externalRCRef())
            }
        }
        public final func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_ranges_ULongProgression_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
        }
        open func isEmpty() -> Swift.Bool {
            return kotlin_ranges_ULongProgression_isEmpty(self.__externalRCRef())
        }
        open func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_ranges_ULongProgression_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.ULongProgression,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        open func hashCode() -> Swift.Int32 {
            return kotlin_ranges_ULongProgression_hashCode(self.__externalRCRef())
        }
        open func toString() -> Swift.String {
            return kotlin_ranges_ULongProgression_toString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}
extension ExportedKotlinPackages.kotlin.CharSequence where Self : KotlinRuntimeSupport._KotlinBridgeable {
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
extension ExportedKotlinPackages.kotlin.CharSequence {
}
extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_Iterator_next(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterator where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterator {
}
extension ExportedKotlinPackages.kotlin.collections.Iterator {
}
