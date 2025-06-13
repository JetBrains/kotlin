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
            fatalError()
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
            fatalError()
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
        public func toString() -> Swift.String {
            return kotlin_Boolean_toString(self.__externalRCRef())
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
        public func toString() -> Swift.String {
            return kotlin_String_toString(self.__externalRCRef())
        }
        public func subSequence(
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.CharSequence {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_String_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), startIndex, endIndex)) as! any ExportedKotlinPackages.kotlin.CharSequence
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
    }
    @objc(_Iterable)
    protocol _Iterable {
    }
    public protocol Iterator: KotlinRuntime.KotlinBase {
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
    }
    @objc(_Map)
    protocol _Map {
    }
    public protocol MutableSet: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Set, ExportedKotlinPackages.kotlin.collections.MutableCollection {
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
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
    }
    @objc(_Collection)
    protocol _Collection: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    public protocol MutableIterable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable {
    }
    @objc(_MutableIterable)
    protocol _MutableIterable: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    public protocol Set: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection {
        var size: Swift.Int32 {
            get
        }
        func isEmpty() -> Swift.Bool
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
    }
    @objc(_Set)
    protocol _Set: ExportedKotlinPackages.kotlin.collections._Collection {
    }
}
public extension ExportedKotlinPackages.kotlin.Annotation where Self : KotlinRuntimeSupport._KotlinBridged {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.Annotation where Wrapped : ExportedKotlinPackages.kotlin._Annotation {
}
public extension ExportedKotlinPackages.kotlin.collections {
    open class ByteIterator: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    }
    open class IntIterator: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
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
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Map where Wrapped : ExportedKotlinPackages.kotlin.collections._Map {
}
public extension ExportedKotlinPackages.kotlin.collections.MutableSet where Self : KotlinRuntimeSupport._KotlinBridged {
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
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Collection where Wrapped : ExportedKotlinPackages.kotlin.collections._Collection {
}
public extension ExportedKotlinPackages.kotlin.collections.MutableIterable where Self : KotlinRuntimeSupport._KotlinBridged {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterable where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterable {
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
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Set where Wrapped : ExportedKotlinPackages.kotlin.collections._Set {
}
