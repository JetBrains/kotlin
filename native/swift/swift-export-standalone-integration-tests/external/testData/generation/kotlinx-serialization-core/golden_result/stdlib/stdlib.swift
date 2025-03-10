@_exported import ExportedKotlinPackages
import KotlinRuntimeSupport
import KotlinRuntime
@_implementationOnly import KotlinBridges_stdlib

public extension ExportedKotlinPackages.kotlin {
    public protocol Annotation: KotlinRuntime.KotlinBase {
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
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
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
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class Exception: ExportedKotlinPackages.kotlin.Throwable {
        public override init() {
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlin_Exception_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_Exception_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class RuntimeException: ExportedKotlinPackages.kotlin.Exception {
        public override init() {
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlin_RuntimeException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class IllegalArgumentException: ExportedKotlinPackages.kotlin.RuntimeException {
        public override init() {
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlin_IllegalArgumentException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
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
                return { switch kotlin_Throwable_cause_get(self.__externalRCRef()) { case 0: .none; case let res: ExportedKotlinPackages.kotlin.Throwable(__externalRCRef: res); } }()
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
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        public init(
            message: Swift.String?
        ) {
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UInt_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        public override init() {
            let __kt = kotlin_Throwable_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
public extension ExportedKotlinPackages.kotlin.Annotation where Self : KotlinRuntimeSupport._KotlinBridged {
}
public extension ExportedKotlinPackages.kotlin.reflect {
    public protocol KType: KotlinRuntime.KotlinBase {
        var classifier: (any ExportedKotlinPackages.kotlin.reflect.KClassifier)? {
            get
        }
        var arguments: [ExportedKotlinPackages.kotlin.reflect.KTypeProjection] {
            get
        }
        var isMarkedNullable: Swift.Bool {
            get
        }
    }
}
public extension ExportedKotlinPackages.kotlin.reflect.KType where Self : KotlinRuntimeSupport._KotlinBridged {
    public var classifier: (any ExportedKotlinPackages.kotlin.reflect.KClassifier)? {
        get {
            return { switch kotlin_reflect_KType_classifier_get(self.__externalRCRef()) { case 0: .none; case let res: KotlinRuntime.KotlinBase(__externalRCRef: res) as! any ExportedKotlinPackages.kotlin.reflect.KClassifier; } }()
        }
    }
    public var arguments: [ExportedKotlinPackages.kotlin.reflect.KTypeProjection] {
        get {
            return kotlin_reflect_KType_arguments_get(self.__externalRCRef()) as! Swift.Array<ExportedKotlinPackages.kotlin.reflect.KTypeProjection>
        }
    }
    public var isMarkedNullable: Swift.Bool {
        get {
            return kotlin_reflect_KType_isMarkedNullable_get(self.__externalRCRef())
        }
    }
}
