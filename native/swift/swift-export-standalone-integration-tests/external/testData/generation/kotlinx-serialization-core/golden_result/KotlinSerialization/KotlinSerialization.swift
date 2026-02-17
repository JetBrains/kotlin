@_exported import ExportedKotlinPackages
import KotlinRuntimeSupport
import KotlinRuntime
import KotlinStdlib
@_implementationOnly import KotlinBridges_KotlinSerialization

public typealias `internal` = ExportedKotlinPackages.kotlinx.serialization.`internal`
public typealias modules = ExportedKotlinPackages.kotlinx.serialization.modules
public typealias descriptors = ExportedKotlinPackages.kotlinx.serialization.descriptors
public typealias builtins = ExportedKotlinPackages.kotlinx.serialization.builtins
public typealias encoding = ExportedKotlinPackages.kotlinx.serialization.encoding
public typealias BinaryFormat = ExportedKotlinPackages.kotlinx.serialization.BinaryFormat
package typealias _BinaryFormat = ExportedKotlinPackages.kotlinx.serialization._BinaryFormat
public typealias ContextualSerializer = ExportedKotlinPackages.kotlinx.serialization.ContextualSerializer
public typealias DeserializationStrategy = ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy
package typealias _DeserializationStrategy = ExportedKotlinPackages.kotlinx.serialization._DeserializationStrategy
public typealias KSerializer = ExportedKotlinPackages.kotlinx.serialization.KSerializer
package typealias _KSerializer = ExportedKotlinPackages.kotlinx.serialization._KSerializer
public typealias MissingFieldException = ExportedKotlinPackages.kotlinx.serialization.MissingFieldException
public typealias PolymorphicSerializer = ExportedKotlinPackages.kotlinx.serialization.PolymorphicSerializer
public typealias SealedClassSerializer = ExportedKotlinPackages.kotlinx.serialization.SealedClassSerializer
public typealias SerialFormat = ExportedKotlinPackages.kotlinx.serialization.SerialFormat
package typealias _SerialFormat = ExportedKotlinPackages.kotlinx.serialization._SerialFormat
public typealias SerializationException = ExportedKotlinPackages.kotlinx.serialization.SerializationException
public typealias SerializationStrategy = ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy
package typealias _SerializationStrategy = ExportedKotlinPackages.kotlinx.serialization._SerializationStrategy
public typealias StringFormat = ExportedKotlinPackages.kotlinx.serialization.StringFormat
package typealias _StringFormat = ExportedKotlinPackages.kotlinx.serialization._StringFormat
public final class _ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Companion: KotlinRuntime.KotlinBase {
    public var DECODE_DONE: Swift.Int32 {
        get {
            return kotlinx_serialization_encoding_CompositeDecoder_Companion_DECODE_DONE_get(self.__externalRCRef())
        }
    }
    public var UNKNOWN_NAME: Swift.Int32 {
        get {
            return kotlinx_serialization_encoding_CompositeDecoder_Companion_UNKNOWN_NAME_get(self.__externalRCRef())
        }
    }
    public static var shared: KotlinSerialization._ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Companion {
        get {
            return KotlinSerialization._ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Companion.__createClassWrapper(externalRCRef: kotlinx_serialization_encoding_CompositeDecoder_Companion_get())
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
public func serializer(
    kClass: Swift.Never,
    typeArgumentsSerializers: [any ExportedKotlinPackages.kotlinx.serialization.KSerializer],
    isNullable: Swift.Bool
) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
    ExportedKotlinPackages.kotlinx.serialization.serializer(kClass: kClass, typeArgumentsSerializers: typeArgumentsSerializers, isNullable: isNullable)
}
public func serializer(
    type: Swift.Never
) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
    ExportedKotlinPackages.kotlinx.serialization.serializer(type: type)
}
public func serializer(
    _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
    kClass: Swift.Never,
    typeArgumentsSerializers: [any ExportedKotlinPackages.kotlinx.serialization.KSerializer],
    isNullable: Swift.Bool
) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
    ExportedKotlinPackages.kotlinx.serialization.serializer(receiver, kClass: kClass, typeArgumentsSerializers: typeArgumentsSerializers, isNullable: isNullable)
}
public func serializer(
    _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
    type: Swift.Never
) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
    ExportedKotlinPackages.kotlinx.serialization.serializer(receiver, type: type)
}
public func serializerOrNull(
    type: Swift.Never
) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer)? {
    ExportedKotlinPackages.kotlinx.serialization.serializerOrNull(type: type)
}
public func serializerOrNull(
    _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
    type: Swift.Never
) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer)? {
    ExportedKotlinPackages.kotlinx.serialization.serializerOrNull(receiver, type: type)
}
extension ExportedKotlinPackages.kotlinx.serialization {
    public protocol BinaryFormat: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.SerialFormat {
    }
    @objc(_BinaryFormat)
    package protocol _BinaryFormat: ExportedKotlinPackages.kotlinx.serialization._SerialFormat {
    }
    public protocol DeserializationStrategy: KotlinRuntime.KotlinBase {
        var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get
        }
        func deserialize(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    }
    @objc(_DeserializationStrategy)
    package protocol _DeserializationStrategy {
    }
    public protocol KSerializer: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy, ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy {
        var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get
        }
    }
    @objc(_KSerializer)
    package protocol _KSerializer: ExportedKotlinPackages.kotlinx.serialization._SerializationStrategy, ExportedKotlinPackages.kotlinx.serialization._DeserializationStrategy {
    }
    public protocol SerialFormat: KotlinRuntime.KotlinBase {
        var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get
        }
    }
    @objc(_SerialFormat)
    package protocol _SerialFormat {
    }
    public protocol SerializationStrategy: KotlinRuntime.KotlinBase {
        var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get
        }
        func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
    }
    @objc(_SerializationStrategy)
    package protocol _SerializationStrategy {
    }
    public protocol StringFormat: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.SerialFormat {
    }
    @objc(_StringFormat)
    package protocol _StringFormat: ExportedKotlinPackages.kotlinx.serialization._SerialFormat {
    }
    public final class ContextualSerializer: KotlinRuntime.KotlinBase {
        public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_ContextualSerializer_descriptor_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
            }
        }
        public func deserialize(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        ) -> any KotlinRuntimeSupport._KotlinBridgeable {
            return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: kotlinx_serialization_ContextualSerializer_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder__(self.__externalRCRef(), decoder.__externalRCRef()))
        }
        public func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: any KotlinRuntimeSupport._KotlinBridgeable
        ) -> Swift.Void {
            return kotlinx_serialization_ContextualSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), encoder.__externalRCRef(), value.__externalRCRef())
        }
        public init(
            serializableClass: Swift.Never
        ) {
            fatalError()
        }
        public init(
            serializableClass: Swift.Never,
            fallbackSerializer: (any ExportedKotlinPackages.kotlinx.serialization.KSerializer)?,
            typeArgumentsSerializers: ExportedKotlinPackages.kotlin.Array
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
    public final class MissingFieldException: ExportedKotlinPackages.kotlinx.serialization.SerializationException {
        public var missingFields: [Swift.String] {
            get {
                return kotlinx_serialization_MissingFieldException_missingFields_get(self.__externalRCRef()) as! Swift.Array<Swift.String>
            }
        }
        public init(
            missingFields: [Swift.String],
            serialName: Swift.String
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.serialization.MissingFieldException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.serialization.MissingFieldException ") }
            let __kt = kotlinx_serialization_MissingFieldException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlinx_serialization_MissingFieldException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_String__Swift_String__(__kt, missingFields, serialName)
        }
        public init(
            missingField: Swift.String,
            serialName: Swift.String
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.serialization.MissingFieldException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.serialization.MissingFieldException ") }
            let __kt = kotlinx_serialization_MissingFieldException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlinx_serialization_MissingFieldException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_String__(__kt, missingField, serialName)
        }
        public init(
            missingFields: [Swift.String],
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.serialization.MissingFieldException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.serialization.MissingFieldException ") }
            let __kt = kotlinx_serialization_MissingFieldException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlinx_serialization_MissingFieldException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_String__Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, missingFields, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class PolymorphicSerializer: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractPolymorphicSerializer {
        public override var baseClass: Swift.Never {
            get {
                fatalError()
            }
        }
        public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_PolymorphicSerializer_descriptor_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
            }
        }
        public func toString() -> Swift.String {
            return kotlinx_serialization_PolymorphicSerializer_toString(self.__externalRCRef())
        }
        public init(
            baseClass: Swift.Never
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
    public final class SealedClassSerializer: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractPolymorphicSerializer {
        public override var baseClass: Swift.Never {
            get {
                fatalError()
            }
        }
        public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_SealedClassSerializer_descriptor_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
            }
        }
        public override func findPolymorphicSerializerOrNull(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder,
            klassName: Swift.String?
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy)? {
            return { switch kotlinx_serialization_SealedClassSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Swift_Optional_Swift_String___(self.__externalRCRef(), decoder.__externalRCRef(), klassName ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy; } }()
        }
        public override func findPolymorphicSerializerOrNull(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: any KotlinRuntimeSupport._KotlinBridgeable
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy)? {
            return { switch kotlinx_serialization_SealedClassSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), encoder.__externalRCRef(), value.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy; } }()
        }
        public init(
            serialName: Swift.String,
            baseClass: Swift.Never,
            subclasses: ExportedKotlinPackages.kotlin.Array,
            subclassSerializers: ExportedKotlinPackages.kotlin.Array
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
    open class SerializationException: ExportedKotlinPackages.kotlin.IllegalArgumentException {
        public override init() {
            if Self.self != ExportedKotlinPackages.kotlinx.serialization.SerializationException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.serialization.SerializationException ") }
            let __kt = kotlinx_serialization_SerializationException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.serialization.SerializationException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.serialization.SerializationException ") }
            let __kt = kotlinx_serialization_SerializationException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.serialization.SerializationException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.serialization.SerializationException ") }
            let __kt = kotlinx_serialization_SerializationException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.serialization.SerializationException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.serialization.SerializationException ") }
            let __kt = kotlinx_serialization_SerializationException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public static func serializer(
        kClass: Swift.Never,
        typeArgumentsSerializers: [any ExportedKotlinPackages.kotlinx.serialization.KSerializer],
        isNullable: Swift.Bool
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        fatalError()
    }
    public static func serializer(
        type: Swift.Never
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        kClass: Swift.Never,
        typeArgumentsSerializers: [any ExportedKotlinPackages.kotlinx.serialization.KSerializer],
        isNullable: Swift.Bool
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        type: Swift.Never
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        fatalError()
    }
    public static func serializerOrNull(
        type: Swift.Never
    ) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer)? {
        fatalError()
    }
    public static func serializerOrNull(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        type: Swift.Never
    ) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer)? {
        fatalError()
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.`internal` {
    public protocol GeneratedSerializer: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        func childSerializers() -> ExportedKotlinPackages.kotlin.Array
        func typeParametersSerializers() -> ExportedKotlinPackages.kotlin.Array
    }
    @objc(_GeneratedSerializer)
    package protocol _GeneratedSerializer: ExportedKotlinPackages.kotlinx.serialization._KSerializer {
    }
    open class AbstractCollectionSerializer: KotlinRuntime.KotlinBase {
        open func deserialize(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_serialization_internal_AbstractCollectionSerializer_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder__(self.__externalRCRef(), decoder.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public final func merge(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder,
            previous: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_serialization_internal_AbstractCollectionSerializer_merge__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), decoder.__externalRCRef(), previous.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        open func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void {
            return kotlinx_serialization_internal_AbstractCollectionSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), encoder.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class AbstractPolymorphicSerializer: KotlinRuntime.KotlinBase {
        open var baseClass: Swift.Never {
            get {
                fatalError()
            }
        }
        public final func deserialize(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        ) -> any KotlinRuntimeSupport._KotlinBridgeable {
            return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: kotlinx_serialization_internal_AbstractPolymorphicSerializer_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder__(self.__externalRCRef(), decoder.__externalRCRef()))
        }
        open func findPolymorphicSerializerOrNull(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder,
            klassName: Swift.String?
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy)? {
            return { switch kotlinx_serialization_internal_AbstractPolymorphicSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Swift_Optional_Swift_String___(self.__externalRCRef(), decoder.__externalRCRef(), klassName ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy; } }()
        }
        open func findPolymorphicSerializerOrNull(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: any KotlinRuntimeSupport._KotlinBridgeable
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy)? {
            return { switch kotlinx_serialization_internal_AbstractPolymorphicSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), encoder.__externalRCRef(), value.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy; } }()
        }
        public final func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: any KotlinRuntimeSupport._KotlinBridgeable
        ) -> Swift.Void {
            return kotlinx_serialization_internal_AbstractPolymorphicSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), encoder.__externalRCRef(), value.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class ElementMarker: KotlinRuntime.KotlinBase {
        public func mark(
            index: Swift.Int32
        ) -> Swift.Void {
            return kotlinx_serialization_internal_ElementMarker_mark__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func nextUnmarkedIndex() -> Swift.Int32 {
            return kotlinx_serialization_internal_ElementMarker_nextUnmarkedIndex(self.__externalRCRef())
        }
        public init(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            readIfAbsent: @escaping (any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, Swift.Int32) -> Swift.Bool
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.serialization.`internal`.ElementMarker.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.serialization.`internal`.ElementMarker ") }
            let __kt = kotlinx_serialization_internal_ElementMarker_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlinx_serialization_internal_ElementMarker_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_U28anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_U20Swift_Int32U29202D_U20Swift_Bool__(__kt, descriptor.__externalRCRef(), {
                let originalBlock = readIfAbsent
                return { arg0, arg1 in return originalBlock(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, arg1) }
            }())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class MapLikeSerializer: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer {
        open var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_MapLikeSerializer_descriptor_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
            }
        }
        public final var keySerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_MapLikeSerializer_keySerializer_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
            }
        }
        public final var valueSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_MapLikeSerializer_valueSerializer_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
            }
        }
        open override func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void {
            return kotlinx_serialization_internal_MapLikeSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), encoder.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class NamedValueDecoder: ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedDecoder {
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class NamedValueEncoder: ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder {
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class TaggedDecoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder, ExportedKotlinPackages.kotlinx.serialization.encoding._Decoder, ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder, ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeDecoder {
        open var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get {
                return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_serializersModule_get(self.__externalRCRef()))
            }
        }
        open func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
        }
        public final func decodeBoolean() -> Swift.Bool {
            return kotlinx_serialization_internal_TaggedDecoder_decodeBoolean(self.__externalRCRef())
        }
        public final func decodeBooleanElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Bool {
            return kotlinx_serialization_internal_TaggedDecoder_decodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        public final func decodeByte() -> Swift.Int8 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeByte(self.__externalRCRef())
        }
        public final func decodeByteElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int8 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        public final func decodeChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlinx_serialization_internal_TaggedDecoder_decodeChar(self.__externalRCRef())
        }
        public final func decodeCharElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlinx_serialization_internal_TaggedDecoder_decodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        public final func decodeDouble() -> Swift.Double {
            return kotlinx_serialization_internal_TaggedDecoder_decodeDouble(self.__externalRCRef())
        }
        public final func decodeDoubleElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Double {
            return kotlinx_serialization_internal_TaggedDecoder_decodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        public final func decodeEnum(
            enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Int32 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), enumDescriptor.__externalRCRef())
        }
        public final func decodeFloat() -> Swift.Float {
            return kotlinx_serialization_internal_TaggedDecoder_decodeFloat(self.__externalRCRef())
        }
        public final func decodeFloatElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Float {
            return kotlinx_serialization_internal_TaggedDecoder_decodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        }
        public final func decodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        }
        public final func decodeInt() -> Swift.Int32 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeInt(self.__externalRCRef())
        }
        public final func decodeIntElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int32 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        public final func decodeLong() -> Swift.Int64 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeLong(self.__externalRCRef())
        }
        public final func decodeLongElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int64 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeNotNullMark() -> Swift.Bool {
            return kotlinx_serialization_internal_TaggedDecoder_decodeNotNullMark(self.__externalRCRef())
        }
        public final func decodeNull() -> Swift.Never? {
            return { kotlinx_serialization_internal_TaggedDecoder_decodeNull(self.__externalRCRef()); return nil; }()
        }
        public final func decodeShort() -> Swift.Int16 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeShort(self.__externalRCRef())
        }
        public final func decodeShortElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int16 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        public final func decodeString() -> Swift.String {
            return kotlinx_serialization_internal_TaggedDecoder_decodeString(self.__externalRCRef())
        }
        public final func decodeStringElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.String {
            return kotlinx_serialization_internal_TaggedDecoder_decodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())
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
    open class TaggedEncoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder, ExportedKotlinPackages.kotlinx.serialization.encoding._Encoder, ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder, ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeEncoder {
        open var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get {
                return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_serializersModule_get(self.__externalRCRef()))
            }
        }
        open func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
        }
        public final func encodeBoolean(
            value: Swift.Bool
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeBoolean__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), value)
        }
        public final func encodeBooleanElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Bool
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Bool__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        public final func encodeByte(
            value: Swift.Int8
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeByte__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), value)
        }
        public final func encodeByteElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int8
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int8__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        public final func encodeChar(
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), value)
        }
        public final func encodeCharElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        public final func encodeDouble(
            value: Swift.Double
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeDouble__TypesOfArguments__Swift_Double__(self.__externalRCRef(), value)
        }
        public final func encodeDoubleElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Double
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Double__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        public final func encodeEnum(
            enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), enumDescriptor.__externalRCRef(), index)
        }
        public final func encodeFloat(
            value: Swift.Float
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeFloat__TypesOfArguments__Swift_Float__(self.__externalRCRef(), value)
        }
        public final func encodeFloatElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Float
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Float__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
        }
        public final func encodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_encodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
        }
        public final func encodeInt(
            value: Swift.Int32
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeInt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), value)
        }
        public final func encodeIntElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int32
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        public final func encodeLong(
            value: Swift.Int64
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeLong__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), value)
        }
        public final func encodeLongElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int64
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int64__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeNotNullMark() -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeNotNullMark(self.__externalRCRef())
        }
        open func encodeNull() -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeNull(self.__externalRCRef())
        }
        public final func encodeShort(
            value: Swift.Int16
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeShort__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), value)
        }
        public final func encodeShortElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int16
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int16__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        public final func encodeString(
            value: Swift.String
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeString__TypesOfArguments__Swift_String__(self.__externalRCRef(), value)
        }
        public final func encodeStringElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.String
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_encodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_String__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        public final func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void {
            return kotlinx_serialization_internal_TaggedEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())
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
    public static func throwArrayMissingFieldException(
        seenArray: ExportedKotlinPackages.kotlin.IntArray,
        goldenMaskArray: ExportedKotlinPackages.kotlin.IntArray,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Void {
        return kotlinx_serialization_internal_throwArrayMissingFieldException__TypesOfArguments__ExportedKotlinPackages_kotlin_IntArray_ExportedKotlinPackages_kotlin_IntArray_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(seenArray.__externalRCRef(), goldenMaskArray.__externalRCRef(), descriptor.__externalRCRef())
    }
    public static func throwMissingFieldException(
        seen: Swift.Int32,
        goldenMask: Swift.Int32,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Void {
        return kotlinx_serialization_internal_throwMissingFieldException__TypesOfArguments__Swift_Int32_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(seen, goldenMask, descriptor.__externalRCRef())
    }
    public static func jsonCachedSerialNames(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Set<Swift.String> {
        return kotlinx_serialization_internal_jsonCachedSerialNames__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef()) as! Swift.Set<Swift.String>
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.modules {
    public protocol SerializersModuleCollector: KotlinRuntime.KotlinBase {
    }
    @objc(_SerializersModuleCollector)
    package protocol _SerializersModuleCollector {
    }
    public final class PolymorphicModuleBuilder: KotlinRuntime.KotlinBase {
        @available(*, deprecated, message: "Deprecated in favor of function with more precise name: defaultDeserializer. Replacement: defaultDeserializer(defaultSerializerProvider)")
        public func `default`(
            defaultSerializerProvider: @escaping (Swift.String?) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy)?
        ) -> Swift.Void {
            return kotlinx_serialization_modules_PolymorphicModuleBuilder_default__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy___(self.__externalRCRef(), {
                let originalBlock = defaultSerializerProvider
                return { arg0 in return originalBlock(arg0).map { it in it.__externalRCRef() } ?? nil }
            }())
        }
        public func defaultDeserializer(
            defaultDeserializerProvider: @escaping (Swift.String?) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy)?
        ) -> Swift.Void {
            return kotlinx_serialization_modules_PolymorphicModuleBuilder_defaultDeserializer__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy___(self.__externalRCRef(), {
                let originalBlock = defaultDeserializerProvider
                return { arg0 in return originalBlock(arg0).map { it in it.__externalRCRef() } ?? nil }
            }())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class SerializersModule: KotlinRuntime.KotlinBase {
        open func dumpTo(
            collector: any ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector
        ) -> Swift.Void {
            return kotlinx_serialization_modules_SerializersModule_dumpTo__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModuleCollector__(self.__externalRCRef(), collector.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class SerializersModuleBuilder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector, ExportedKotlinPackages.kotlinx.serialization.modules._SerializersModuleCollector {
        public func include(
            module: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule
        ) -> Swift.Void {
            return kotlinx_serialization_modules_SerializersModuleBuilder_include__TypesOfArguments__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule__(self.__externalRCRef(), module.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    @available(*, deprecated, message: "Deprecated in the favour of 'EmptySerializersModule()'. Replacement: EmptySerializersModule()")
    public static var EmptySerializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_modules_EmptySerializersModule_get())
        }
    }
    public static func EmptySerializersModule() -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_modules_EmptySerializersModule())
    }
    public static func serializersModule(
        builderAction: @escaping (ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleBuilder) -> Swift.Void
    ) -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_modules_SerializersModule__TypesOfArguments__U28ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModuleBuilderU29202D_U20Swift_Void__({
            let originalBlock = builderAction
            return { arg0 in return originalBlock(ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleBuilder.__createClassWrapper(externalRCRef: arg0)) }
        }()))
    }
    public static func overwriteWith(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        other: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule
    ) -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_modules_overwriteWith__TypesOfArguments__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule__(receiver.__externalRCRef(), other.__externalRCRef()))
    }
    public static func plus(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        other: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule
    ) -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_modules_plus__TypesOfArguments__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule__(receiver.__externalRCRef(), other.__externalRCRef()))
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.descriptors {
    public protocol SerialDescriptor: KotlinRuntime.KotlinBase {
        var annotations: [any ExportedKotlinPackages.kotlin.Annotation] {
            get
        }
        var elementsCount: Swift.Int32 {
            get
        }
        var isInline: Swift.Bool {
            get
        }
        var isNullable: Swift.Bool {
            get
        }
        var kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
            get
        }
        var serialName: Swift.String {
            get
        }
        func getElementAnnotations(
            index: Swift.Int32
        ) -> [any ExportedKotlinPackages.kotlin.Annotation]
        func getElementDescriptor(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        func getElementIndex(
            name: Swift.String
        ) -> Swift.Int32
        func getElementName(
            index: Swift.Int32
        ) -> Swift.String
        func isElementOptional(
            index: Swift.Int32
        ) -> Swift.Bool
    }
    @objc(_SerialDescriptor)
    package protocol _SerialDescriptor {
    }
    public final class ClassSerialDescriptorBuilder: KotlinRuntime.KotlinBase {
        public var annotations: [any ExportedKotlinPackages.kotlin.Annotation] {
            get {
                return kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder_annotations_get(self.__externalRCRef()) as! Swift.Array<any ExportedKotlinPackages.kotlin.Annotation>
            }
            set {
                return kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder_annotations_set__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlin_Annotation___(self.__externalRCRef(), newValue)
            }
        }
        @available(*, unavailable, message: "isNullable inside buildSerialDescriptor is deprecated. Please use SerialDescriptor.nullable extension on a builder result.")
        public var isNullable: Swift.Bool {
            get {
                return kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder_isNullable_get(self.__externalRCRef())
            }
            set {
                return kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder_isNullable_set__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), newValue)
            }
        }
        public var serialName: Swift.String {
            get {
                return kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder_serialName_get(self.__externalRCRef())
            }
        }
        public func element(
            elementName: Swift.String,
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            annotations: [any ExportedKotlinPackages.kotlin.Annotation],
            isOptional: Swift.Bool
        ) -> Swift.Void {
            return kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder_element__TypesOfArguments__Swift_String_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Array_anyU20ExportedKotlinPackages_kotlin_Annotation__Swift_Bool__(self.__externalRCRef(), elementName, descriptor.__externalRCRef(), annotations, isOptional)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class PolymorphicKind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        public final class OPEN: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.OPEN {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.OPEN.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PolymorphicKind_OPEN_get())
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
        public final class SEALED: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.SEALED {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.SEALED.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PolymorphicKind_SEALED_get())
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
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class PrimitiveKind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        public final class BOOLEAN: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_BOOLEAN_get())
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
        public final class BYTE: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BYTE {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BYTE.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_BYTE_get())
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
        public final class CHAR: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.CHAR {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.CHAR.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_CHAR_get())
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
        public final class DOUBLE: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_DOUBLE_get())
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
        public final class FLOAT: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.FLOAT {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.FLOAT.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_FLOAT_get())
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
        public final class INT: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.INT {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.INT.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_INT_get())
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
        public final class LONG: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.LONG {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.LONG.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_LONG_get())
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
        public final class SHORT: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.SHORT {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.SHORT.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_SHORT_get())
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
        public final class STRING: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.STRING {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.STRING.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_STRING_get())
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
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class SerialKind: KotlinRuntime.KotlinBase {
        public final class CONTEXTUAL: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.CONTEXTUAL {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.CONTEXTUAL.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_SerialKind_CONTEXTUAL_get())
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
        public final class ENUM: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.ENUM {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.ENUM.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_SerialKind_ENUM_get())
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
        open func hashCode() -> Swift.Int32 {
            return kotlinx_serialization_descriptors_SerialKind_hashCode(self.__externalRCRef())
        }
        open func toString() -> Swift.String {
            return kotlinx_serialization_descriptors_SerialKind_toString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class StructureKind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        public final class CLASS: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.CLASS {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.CLASS.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_StructureKind_CLASS_get())
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
        public final class LIST: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.LIST {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.LIST.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_StructureKind_LIST_get())
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
        public final class MAP: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.MAP {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.MAP.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_StructureKind_MAP_get())
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
        public final class OBJECT: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.OBJECT {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.OBJECT.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_StructureKind_OBJECT_get())
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
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public static func getCapturedKClass(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Never {
        fatalError()
    }
    public static func getElementDescriptors(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlin.collections.Iterable {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_elementDescriptors_get__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterable
    }
    public static func getElementNames(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlin.collections.Iterable {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_elementNames_get__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterable
    }
    public static func getNullable(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_nullable_get__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func PrimitiveSerialDescriptor(
        serialName: Swift.String,
        kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveSerialDescriptor__TypesOfArguments__Swift_String_ExportedKotlinPackages_kotlinx_serialization_descriptors_PrimitiveKind__(serialName, kind.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func serialDescriptor(
        serialName: Swift.String,
        original: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_SerialDescriptor__TypesOfArguments__Swift_String_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(serialName, original.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func buildClassSerialDescriptor(
        serialName: Swift.String,
        typeParameters: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor...,
        builderAction: @escaping (ExportedKotlinPackages.kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder) -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_buildClassSerialDescriptor__TypesOfArguments__Swift_String_Swift_Array_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__Vararg__U28ExportedKotlinPackages_kotlinx_serialization_descriptors_ClassSerialDescriptorBuilderU29202D_U20Swift_Void__(serialName, typeParameters, {
            let originalBlock = builderAction
            return { arg0 in return originalBlock(ExportedKotlinPackages.kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder.__createClassWrapper(externalRCRef: arg0)) }
        }())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func buildSerialDescriptor(
        serialName: Swift.String,
        kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind,
        typeParameters: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor...,
        builder: @escaping (ExportedKotlinPackages.kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder) -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_buildSerialDescriptor__TypesOfArguments__Swift_String_ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialKind_Swift_Array_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__Vararg__U28ExportedKotlinPackages_kotlinx_serialization_descriptors_ClassSerialDescriptorBuilderU29202D_U20Swift_Void__(serialName, kind.__externalRCRef(), typeParameters, {
            let originalBlock = builder
            return { arg0 in return originalBlock(ExportedKotlinPackages.kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder.__createClassWrapper(externalRCRef: arg0)) }
        }())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func listSerialDescriptor(
        elementDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_listSerialDescriptor__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(elementDescriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func mapSerialDescriptor(
        keyDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        valueDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_mapSerialDescriptor__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(keyDescriptor.__externalRCRef(), valueDescriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func serialDescriptor(
        type: Swift.Never
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        fatalError()
    }
    public static func setSerialDescriptor(
        elementDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_setSerialDescriptor__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(elementDescriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func getContextualDescriptor(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> (any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)? {
        return { switch kotlinx_serialization_descriptors_getContextualDescriptor__TypesOfArguments__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef(), descriptor.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor; } }()
    }
    public static func getPolymorphicDescriptors(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> [any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor] {
        return kotlinx_serialization_descriptors_getPolymorphicDescriptors__TypesOfArguments__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef(), descriptor.__externalRCRef()) as! Swift.Array<any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor>
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.builtins {
    public final class LongAsStringSerializer: KotlinRuntime.KotlinBase {
        public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_LongAsStringSerializer_descriptor_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
            }
        }
        public static var shared: ExportedKotlinPackages.kotlinx.serialization.builtins.LongAsStringSerializer {
            get {
                return ExportedKotlinPackages.kotlinx.serialization.builtins.LongAsStringSerializer.__createClassWrapper(externalRCRef: kotlinx_serialization_builtins_LongAsStringSerializer_get())
            }
        }
        public func deserialize(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        ) -> Swift.Int64 {
            return kotlinx_serialization_builtins_LongAsStringSerializer_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder__(self.__externalRCRef(), decoder.__externalRCRef())
        }
        public func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: Swift.Int64
        ) -> Swift.Void {
            return kotlinx_serialization_builtins_LongAsStringSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Swift_Int64__(self.__externalRCRef(), encoder.__externalRCRef(), value)
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
    public static func getNullable(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_nullable_get__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func BooleanArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_BooleanArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func ByteArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_ByteArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func CharArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_CharArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func DoubleArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_DoubleArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func FloatArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_FloatArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func IntArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_IntArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func LongArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_LongArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func NothingSerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_NothingSerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func ShortArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_ShortArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func UByteArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_UByteArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func UIntArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_UIntArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func ULongArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_ULongArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func UShortArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_UShortArraySerializer()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Boolean.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_serializer__TypesOfArguments__ExportedKotlinPackages_kotlin_Boolean_Companion__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Byte.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_serializer__TypesOfArguments__ExportedKotlinPackages_kotlin_Byte_Companion__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Char.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_serializer__TypesOfArguments__ExportedKotlinPackages_kotlin_Char_Companion__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Double.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_serializer__TypesOfArguments__ExportedKotlinPackages_kotlin_Double_Companion__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Float.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_serializer__TypesOfArguments__ExportedKotlinPackages_kotlin_Float_Companion__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Int.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_serializer__TypesOfArguments__ExportedKotlinPackages_kotlin_Int_Companion__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Long.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_serializer__TypesOfArguments__ExportedKotlinPackages_kotlin_Long_Companion__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Short.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_serializer__TypesOfArguments__ExportedKotlinPackages_kotlin_Short_Companion__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.String.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_serializer__TypesOfArguments__ExportedKotlinPackages_kotlin_String_Companion__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.UByte.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.UInt.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.ULong.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.UShort.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        fatalError()
    }
    public static func serializer(
        _ receiver: Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_serializer__TypesOfArguments__Swift_Void__()) as! any ExportedKotlinPackages.kotlinx.serialization.KSerializer
    }
    public static func serializer(
        _ receiver: Swift.Never
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer {
        fatalError()
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding {
    public protocol ChunkedDecoder: KotlinRuntime.KotlinBase {
        func decodeStringChunked(
            consumeChunk: @escaping (Swift.String) -> Swift.Void
        ) -> Swift.Void
    }
    @objc(_ChunkedDecoder)
    package protocol _ChunkedDecoder {
    }
    public protocol CompositeDecoder: KotlinRuntime.KotlinBase {
        var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get
        }
        func decodeBooleanElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Bool
        func decodeByteElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int8
        func decodeCharElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit
        func decodeCollectionSize(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Int32
        func decodeDoubleElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Double
        func decodeElementIndex(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Int32
        func decodeFloatElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Float
        func decodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        func decodeIntElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int32
        func decodeLongElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int64
        func decodeSequentially() -> Swift.Bool
        func decodeShortElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int16
        func decodeStringElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.String
        func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void
    }
    @objc(_CompositeDecoder)
    package protocol _CompositeDecoder {
    }
    public protocol CompositeEncoder: KotlinRuntime.KotlinBase {
        var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get
        }
        func encodeBooleanElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Bool
        ) -> Swift.Void
        func encodeByteElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int8
        ) -> Swift.Void
        func encodeCharElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void
        func encodeDoubleElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Double
        ) -> Swift.Void
        func encodeFloatElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Float
        ) -> Swift.Void
        func encodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
        func encodeIntElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int32
        ) -> Swift.Void
        func encodeLongElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int64
        ) -> Swift.Void
        func encodeShortElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int16
        ) -> Swift.Void
        func encodeStringElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.String
        ) -> Swift.Void
        func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void
        func shouldEncodeElementDefault(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Bool
    }
    @objc(_CompositeEncoder)
    package protocol _CompositeEncoder {
    }
    public protocol Decoder: KotlinRuntime.KotlinBase {
        var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get
        }
        func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
        func decodeBoolean() -> Swift.Bool
        func decodeByte() -> Swift.Int8
        func decodeChar() -> Swift.Unicode.UTF16.CodeUnit
        func decodeDouble() -> Swift.Double
        func decodeEnum(
            enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Int32
        func decodeFloat() -> Swift.Float
        func decodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        func decodeInt() -> Swift.Int32
        func decodeLong() -> Swift.Int64
        func decodeNotNullMark() -> Swift.Bool
        func decodeNull() -> Swift.Never?
        func decodeShort() -> Swift.Int16
        func decodeString() -> Swift.String
    }
    @objc(_Decoder)
    package protocol _Decoder {
    }
    public protocol Encoder: KotlinRuntime.KotlinBase {
        var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get
        }
        func beginCollection(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            collectionSize: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
        func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
        func encodeBoolean(
            value: Swift.Bool
        ) -> Swift.Void
        func encodeByte(
            value: Swift.Int8
        ) -> Swift.Void
        func encodeChar(
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void
        func encodeDouble(
            value: Swift.Double
        ) -> Swift.Void
        func encodeEnum(
            enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Void
        func encodeFloat(
            value: Swift.Float
        ) -> Swift.Void
        func encodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
        func encodeInt(
            value: Swift.Int32
        ) -> Swift.Void
        func encodeLong(
            value: Swift.Int64
        ) -> Swift.Void
        func encodeNotNullMark() -> Swift.Void
        func encodeNull() -> Swift.Void
        func encodeShort(
            value: Swift.Int16
        ) -> Swift.Void
        func encodeString(
            value: Swift.String
        ) -> Swift.Void
    }
    @objc(_Encoder)
    package protocol _Encoder {
    }
    open class AbstractDecoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder, ExportedKotlinPackages.kotlinx.serialization.encoding._Decoder, ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder, ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeDecoder {
        open func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
        }
        open func decodeBoolean() -> Swift.Bool {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeBoolean(self.__externalRCRef())
        }
        public final func decodeBooleanElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Bool {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeByte() -> Swift.Int8 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeByte(self.__externalRCRef())
        }
        public final func decodeByteElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int8 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeChar(self.__externalRCRef())
        }
        public final func decodeCharElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeDouble() -> Swift.Double {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeDouble(self.__externalRCRef())
        }
        public final func decodeDoubleElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Double {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeEnum(
            enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Int32 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), enumDescriptor.__externalRCRef())
        }
        open func decodeFloat() -> Swift.Float {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeFloat(self.__externalRCRef())
        }
        public final func decodeFloatElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Float {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        }
        open func decodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        }
        open func decodeInt() -> Swift.Int32 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeInt(self.__externalRCRef())
        }
        public final func decodeIntElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int32 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeLong() -> Swift.Int64 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeLong(self.__externalRCRef())
        }
        public final func decodeLongElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int64 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeNotNullMark() -> Swift.Bool {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeNotNullMark(self.__externalRCRef())
        }
        open func decodeNull() -> Swift.Never? {
            return { kotlinx_serialization_encoding_AbstractDecoder_decodeNull(self.__externalRCRef()); return nil; }()
        }
        open func decodeShort() -> Swift.Int16 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeShort(self.__externalRCRef())
        }
        public final func decodeShortElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int16 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeString() -> Swift.String {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeString(self.__externalRCRef())
        }
        public final func decodeStringElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.String {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func decodeValue() -> any KotlinRuntimeSupport._KotlinBridgeable {
            return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeValue(self.__externalRCRef()))
        }
        open func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())
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
    open class AbstractEncoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder, ExportedKotlinPackages.kotlinx.serialization.encoding._Encoder, ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder, ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeEncoder {
        open func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
        }
        open func encodeBoolean(
            value: Swift.Bool
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeBoolean__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), value)
        }
        public final func encodeBooleanElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Bool
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Bool__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeByte(
            value: Swift.Int8
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeByte__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), value)
        }
        public final func encodeByteElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int8
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int8__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeChar(
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), value)
        }
        public final func encodeCharElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeDouble(
            value: Swift.Double
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeDouble__TypesOfArguments__Swift_Double__(self.__externalRCRef(), value)
        }
        public final func encodeDoubleElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Double
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Double__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Bool {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        open func encodeEnum(
            enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), enumDescriptor.__externalRCRef(), index)
        }
        open func encodeFloat(
            value: Swift.Float
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeFloat__TypesOfArguments__Swift_Float__(self.__externalRCRef(), value)
        }
        public final func encodeFloatElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Float
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Float__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
        }
        public final func encodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_encodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
        }
        open func encodeInt(
            value: Swift.Int32
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeInt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), value)
        }
        public final func encodeIntElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int32
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeLong(
            value: Swift.Int64
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeLong__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), value)
        }
        public final func encodeLongElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int64
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int64__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeNull() -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeNull(self.__externalRCRef())
        }
        open func encodeShort(
            value: Swift.Int16
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeShort__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), value)
        }
        public final func encodeShortElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int16
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int16__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeString(
            value: Swift.String
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeString__TypesOfArguments__Swift_String__(self.__externalRCRef(), value)
        }
        public final func encodeStringElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.String
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_String__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
        }
        open func encodeValue(
            value: any KotlinRuntimeSupport._KotlinBridgeable
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeValue__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), value.__externalRCRef())
        }
        open func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())
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
    public static func decodeStructure(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        block: @escaping (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_serialization_encoding_decodeStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_U28anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoderU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), descriptor.__externalRCRef(), {
            let originalBlock = block
            return { arg0 in return originalBlock(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder).map { it in it.__externalRCRef() } ?? nil }
        }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public static func encodeCollection(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        collectionSize: Swift.Int32,
        block: @escaping (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder) -> Swift.Void
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_encodeCollection__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_U28anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeEncoderU29202D_U20Swift_Void__(receiver.__externalRCRef(), descriptor.__externalRCRef(), collectionSize, {
            let originalBlock = block
            return { arg0 in return originalBlock(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder) }
        }())
    }
    public static func encodeStructure(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        block: @escaping (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder) -> Swift.Void
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_encodeStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_U28anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeEncoderU29202D_U20Swift_Void__(receiver.__externalRCRef(), descriptor.__externalRCRef(), {
            let originalBlock = block
            return { arg0 in return originalBlock(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder) }
        }())
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.BinaryFormat where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.BinaryFormat where Wrapped : ExportedKotlinPackages.kotlinx.serialization._BinaryFormat {
}
extension ExportedKotlinPackages.kotlinx.serialization.BinaryFormat {
}
extension ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_DeserializationStrategy_descriptor_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        }
    }
    public func deserialize(
        decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_serialization_DeserializationStrategy_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder__(self.__externalRCRef(), decoder.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy where Wrapped : ExportedKotlinPackages.kotlinx.serialization._DeserializationStrategy {
}
extension ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy {
}
extension ExportedKotlinPackages.kotlinx.serialization.KSerializer where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_KSerializer_descriptor_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        }
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.KSerializer where Wrapped : ExportedKotlinPackages.kotlinx.serialization._KSerializer {
}
extension ExportedKotlinPackages.kotlinx.serialization.KSerializer {
}
extension ExportedKotlinPackages.kotlinx.serialization.SerialFormat where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_SerialFormat_serializersModule_get(self.__externalRCRef()))
        }
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.SerialFormat where Wrapped : ExportedKotlinPackages.kotlinx.serialization._SerialFormat {
}
extension ExportedKotlinPackages.kotlinx.serialization.SerialFormat {
}
extension ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_SerializationStrategy_descriptor_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        }
    }
    public func serialize(
        encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return kotlinx_serialization_SerializationStrategy_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), encoder.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil)
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy where Wrapped : ExportedKotlinPackages.kotlinx.serialization._SerializationStrategy {
}
extension ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy {
}
extension ExportedKotlinPackages.kotlinx.serialization.StringFormat where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.StringFormat where Wrapped : ExportedKotlinPackages.kotlinx.serialization._StringFormat {
}
extension ExportedKotlinPackages.kotlinx.serialization.StringFormat {
}
extension ExportedKotlinPackages.kotlinx.serialization.`internal`.GeneratedSerializer where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func childSerializers() -> ExportedKotlinPackages.kotlin.Array {
        return ExportedKotlinPackages.kotlin.Array.__createClassWrapper(externalRCRef: kotlinx_serialization_internal_GeneratedSerializer_childSerializers(self.__externalRCRef()))
    }
    public func typeParametersSerializers() -> ExportedKotlinPackages.kotlin.Array {
        return ExportedKotlinPackages.kotlin.Array.__createClassWrapper(externalRCRef: kotlinx_serialization_internal_GeneratedSerializer_typeParametersSerializers(self.__externalRCRef()))
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.`internal`.GeneratedSerializer where Wrapped : ExportedKotlinPackages.kotlinx.serialization.`internal`._GeneratedSerializer {
}
extension ExportedKotlinPackages.kotlinx.serialization.`internal`.GeneratedSerializer {
}
extension ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector where Wrapped : ExportedKotlinPackages.kotlinx.serialization.modules._SerializersModuleCollector {
}
extension ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector {
}
extension ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var annotations: [any ExportedKotlinPackages.kotlin.Annotation] {
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_annotations_get(self.__externalRCRef()) as! Swift.Array<any ExportedKotlinPackages.kotlin.Annotation>
        }
    }
    public var elementsCount: Swift.Int32 {
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_elementsCount_get(self.__externalRCRef())
        }
    }
    public var isInline: Swift.Bool {
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_isInline_get(self.__externalRCRef())
        }
    }
    public var isNullable: Swift.Bool {
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_isNullable_get(self.__externalRCRef())
        }
    }
    public var kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_SerialDescriptor_kind_get(self.__externalRCRef()))
        }
    }
    public var serialName: Swift.String {
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_serialName_get(self.__externalRCRef())
        }
    }
    public func getElementAnnotations(
        index: Swift.Int32
    ) -> [any ExportedKotlinPackages.kotlin.Annotation] {
        return kotlinx_serialization_descriptors_SerialDescriptor_getElementAnnotations__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) as! Swift.Array<any ExportedKotlinPackages.kotlin.Annotation>
    }
    public func getElementDescriptor(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_SerialDescriptor_getElementDescriptor__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public func getElementIndex(
        name: Swift.String
    ) -> Swift.Int32 {
        return kotlinx_serialization_descriptors_SerialDescriptor_getElementIndex__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
    }
    public func getElementName(
        index: Swift.Int32
    ) -> Swift.String {
        return kotlinx_serialization_descriptors_SerialDescriptor_getElementName__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
    }
    public func isElementOptional(
        index: Swift.Int32
    ) -> Swift.Bool {
        return kotlinx_serialization_descriptors_SerialDescriptor_isElementOptional__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor where Wrapped : ExportedKotlinPackages.kotlinx.serialization.descriptors._SerialDescriptor {
}
extension ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.ChunkedDecoder where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func decodeStringChunked(
        consumeChunk: @escaping (Swift.String) -> Swift.Void
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_ChunkedDecoder_decodeStringChunked__TypesOfArguments__U28Swift_StringU29202D_U20Swift_Void__(self.__externalRCRef(), {
            let originalBlock = consumeChunk
            return { arg0 in return originalBlock(arg0) }
        }())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.encoding.ChunkedDecoder where Wrapped : ExportedKotlinPackages.kotlinx.serialization.encoding._ChunkedDecoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.ChunkedDecoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_encoding_CompositeDecoder_serializersModule_get(self.__externalRCRef()))
        }
    }
    public func decodeBooleanElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Bool {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
    public func decodeByteElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Int8 {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
    public func decodeCharElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Unicode.UTF16.CodeUnit {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
    public func decodeCollectionSize(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Int32 {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeCollectionSize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())
    }
    public func decodeDoubleElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Double {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
    public func decodeElementIndex(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Int32 {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeElementIndex__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())
    }
    public func decodeFloatElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Float {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
    public func decodeInlineElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_CompositeDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    }
    public func decodeIntElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Int32 {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
    public func decodeLongElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Int64 {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
    public func decodeSequentially() -> Swift.Bool {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeSequentially(self.__externalRCRef())
    }
    public func decodeShortElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Int16 {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
    public func decodeStringElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.String {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
    public func endStructure(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder where Wrapped : ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeDecoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder {
    typealias Companion = KotlinSerialization._ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Companion
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_encoding_CompositeEncoder_serializersModule_get(self.__externalRCRef()))
        }
    }
    public func encodeBooleanElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Bool
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeEncoder_encodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Bool__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
    }
    public func encodeByteElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Int8
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeEncoder_encodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int8__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
    }
    public func encodeCharElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Unicode.UTF16.CodeUnit
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeEncoder_encodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
    }
    public func encodeDoubleElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Double
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeEncoder_encodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Double__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
    }
    public func encodeFloatElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Float
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeEncoder_encodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Float__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
    }
    public func encodeInlineElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_CompositeEncoder_encodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    }
    public func encodeIntElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Int32
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeEncoder_encodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
    }
    public func encodeLongElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Int64
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeEncoder_encodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int64__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
    }
    public func encodeShortElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Int16
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeEncoder_encodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int16__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
    }
    public func encodeStringElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.String
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeEncoder_encodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_String__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value)
    }
    public func endStructure(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_CompositeEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())
    }
    public func shouldEncodeElementDefault(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Bool {
        return kotlinx_serialization_encoding_CompositeEncoder_shouldEncodeElementDefault__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder where Wrapped : ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeEncoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_encoding_Decoder_serializersModule_get(self.__externalRCRef()))
        }
    }
    public func beginStructure(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Decoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    }
    public func decodeBoolean() -> Swift.Bool {
        return kotlinx_serialization_encoding_Decoder_decodeBoolean(self.__externalRCRef())
    }
    public func decodeByte() -> Swift.Int8 {
        return kotlinx_serialization_encoding_Decoder_decodeByte(self.__externalRCRef())
    }
    public func decodeChar() -> Swift.Unicode.UTF16.CodeUnit {
        return kotlinx_serialization_encoding_Decoder_decodeChar(self.__externalRCRef())
    }
    public func decodeDouble() -> Swift.Double {
        return kotlinx_serialization_encoding_Decoder_decodeDouble(self.__externalRCRef())
    }
    public func decodeEnum(
        enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Int32 {
        return kotlinx_serialization_encoding_Decoder_decodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), enumDescriptor.__externalRCRef())
    }
    public func decodeFloat() -> Swift.Float {
        return kotlinx_serialization_encoding_Decoder_decodeFloat(self.__externalRCRef())
    }
    public func decodeInline(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Decoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    }
    public func decodeInt() -> Swift.Int32 {
        return kotlinx_serialization_encoding_Decoder_decodeInt(self.__externalRCRef())
    }
    public func decodeLong() -> Swift.Int64 {
        return kotlinx_serialization_encoding_Decoder_decodeLong(self.__externalRCRef())
    }
    public func decodeNotNullMark() -> Swift.Bool {
        return kotlinx_serialization_encoding_Decoder_decodeNotNullMark(self.__externalRCRef())
    }
    public func decodeNull() -> Swift.Never? {
        return { kotlinx_serialization_encoding_Decoder_decodeNull(self.__externalRCRef()); return nil; }()
    }
    public func decodeShort() -> Swift.Int16 {
        return kotlinx_serialization_encoding_Decoder_decodeShort(self.__externalRCRef())
    }
    public func decodeString() -> Swift.String {
        return kotlinx_serialization_encoding_Decoder_decodeString(self.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder where Wrapped : ExportedKotlinPackages.kotlinx.serialization.encoding._Decoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_encoding_Encoder_serializersModule_get(self.__externalRCRef()))
        }
    }
    public func beginCollection(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        collectionSize: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Encoder_beginCollection__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), collectionSize)) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    }
    public func beginStructure(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Encoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    }
    public func encodeBoolean(
        value: Swift.Bool
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeBoolean__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), value)
    }
    public func encodeByte(
        value: Swift.Int8
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeByte__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), value)
    }
    public func encodeChar(
        value: Swift.Unicode.UTF16.CodeUnit
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), value)
    }
    public func encodeDouble(
        value: Swift.Double
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeDouble__TypesOfArguments__Swift_Double__(self.__externalRCRef(), value)
    }
    public func encodeEnum(
        enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), enumDescriptor.__externalRCRef(), index)
    }
    public func encodeFloat(
        value: Swift.Float
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeFloat__TypesOfArguments__Swift_Float__(self.__externalRCRef(), value)
    }
    public func encodeInline(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Encoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    }
    public func encodeInt(
        value: Swift.Int32
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeInt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), value)
    }
    public func encodeLong(
        value: Swift.Int64
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeLong__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), value)
    }
    public func encodeNotNullMark() -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeNotNullMark(self.__externalRCRef())
    }
    public func encodeNull() -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeNull(self.__externalRCRef())
    }
    public func encodeShort(
        value: Swift.Int16
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeShort__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), value)
    }
    public func encodeString(
        value: Swift.String
    ) -> Swift.Void {
        return kotlinx_serialization_encoding_Encoder_encodeString__TypesOfArguments__Swift_String__(self.__externalRCRef(), value)
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder where Wrapped : ExportedKotlinPackages.kotlinx.serialization.encoding._Encoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
}
