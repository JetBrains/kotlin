@_exported import ExportedKotlinPackages
import KotlinRuntimeSupport
import KotlinRuntime
@_implementationOnly import KotlinBridges_KotlinStdlib

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
        func subSequence(
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.CharSequence
    }
    @objc(_CharSequence)
    protocol _CharSequence {
    }
    public final class ByteArray: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var size: Swift.Int32 {
            get {
                return kotlin_ByteArray_size_get(self.__externalRCRef())
            }
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
    }
    public final class IntArray: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
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
    }
    public final class Boolean: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    }
    public final class Char: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    }
    open class Exception: ExportedKotlinPackages.kotlin.Throwable {
        public override init() {
            precondition(Self.self == ExportedKotlinPackages.kotlin.Exception.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ")
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.Exception.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ")
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.Exception.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ")
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.Exception.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Exception ")
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
            precondition(Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ")
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ")
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ")
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.RuntimeException ")
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
            precondition(Self.self == ExportedKotlinPackages.kotlin.IllegalArgumentException.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalArgumentException ")
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.IllegalArgumentException.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalArgumentException ")
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.IllegalArgumentException.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalArgumentException ")
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.IllegalArgumentException.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.IllegalArgumentException ")
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
    public final class Byte: ExportedKotlinPackages.kotlin.Number {
    }
    public final class Short: ExportedKotlinPackages.kotlin.Number {
    }
    public final class Int: ExportedKotlinPackages.kotlin.Number {
    }
    public final class Long: ExportedKotlinPackages.kotlin.Number {
    }
    public final class Float: ExportedKotlinPackages.kotlin.Number {
    }
    public final class Double: ExportedKotlinPackages.kotlin.Number {
    }
    public final class String: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.CharSequence, ExportedKotlinPackages.kotlin._CharSequence, KotlinRuntimeSupport._KotlinBridged {
    }
    open class Throwable: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        open var message: Swift.String? {
            get {
                return kotlin_Throwable_message_get(self.__externalRCRef())
            }
        }
        open var cause: ExportedKotlinPackages.kotlin.Throwable? {
            get {
                return { switch kotlin_Throwable_cause_get(self.__externalRCRef()) { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__create(externalRCRef: res); } }()
            }
        }
        public final func getStackTrace() -> Swift.Never {
            fatalError()
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
            precondition(Self.self == ExportedKotlinPackages.kotlin.Throwable.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ")
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public init(
            message: Swift.String?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.Throwable.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ")
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            precondition(Self.self == ExportedKotlinPackages.kotlin.Throwable.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ")
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public init() {
            precondition(Self.self == ExportedKotlinPackages.kotlin.Throwable.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.Throwable ")
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
    public final class UByte: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    }
    public final class UInt: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    }
    public final class ULong: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    }
    public final class UShort: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    }
}
public extension ExportedKotlinPackages.kotlin.time {
    public final class Duration: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    }
}
public extension ExportedKotlinPackages.kotlin.Annotation where Self : KotlinRuntimeSupport._KotlinBridged {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.Annotation where Wrapped : ExportedKotlinPackages.kotlin._Annotation {
}
public extension ExportedKotlinPackages.kotlin.CharSequence where Self : KotlinRuntimeSupport._KotlinBridged {
    public var length: Swift.Int32 {
        get {
            return kotlin_CharSequence_length_get(self.__externalRCRef())
        }
    }
    public func subSequence(
        startIndex: Swift.Int32,
        endIndex: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.CharSequence {
        return KotlinRuntime.KotlinBase.__createExistential(externalRCRef: kotlin_CharSequence_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), startIndex, endIndex)) as! any ExportedKotlinPackages.kotlin.CharSequence
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.CharSequence where Wrapped : ExportedKotlinPackages.kotlin._CharSequence {
}
