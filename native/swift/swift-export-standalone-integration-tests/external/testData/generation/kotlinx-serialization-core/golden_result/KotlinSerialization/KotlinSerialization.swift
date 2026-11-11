@_exported import ExportedKotlinPackages
import KotlinRuntime
import KotlinRuntimeSupport
@_spi(kotlin$ExperimentalUnsignedTypes) import KotlinStdlib
@_implementationOnly import KotlinBridges_KotlinSerialization

public typealias modules = ExportedKotlinPackages.kotlinx.serialization.modules
public typealias encoding = ExportedKotlinPackages.kotlinx.serialization.encoding
public typealias `internal` = ExportedKotlinPackages.kotlinx.serialization.`internal`
public typealias builtins = ExportedKotlinPackages.kotlinx.serialization.builtins
public typealias descriptors = ExportedKotlinPackages.kotlinx.serialization.descriptors
public typealias BinaryFormat = ExportedKotlinPackages.kotlinx.serialization.BinaryFormat
public typealias _BinaryFormat = ExportedKotlinPackages.kotlinx.serialization._BinaryFormat
public typealias __BinaryFormat = ExportedKotlinPackages.kotlinx.serialization.__BinaryFormat
@_spi(kotlinx$serialization$ExperimentalSerializationApi)
public typealias ContextualSerializer<T: KotlinRuntimeSupport._KotlinBridgeable> = ExportedKotlinPackages.kotlinx.serialization.ContextualSerializer<T>
public typealias DeserializationStrategy = ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy
public typealias AnyDeserializationStrategy<T: KotlinRuntimeSupport._KotlinBridgeable> = ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<T>
public typealias _DeserializationStrategy = ExportedKotlinPackages.kotlinx.serialization._DeserializationStrategy
public typealias __DeserializationStrategy = ExportedKotlinPackages.kotlinx.serialization.__DeserializationStrategy
public typealias KSerializer = ExportedKotlinPackages.kotlinx.serialization.KSerializer
public typealias AnyKSerializer<T: KotlinRuntimeSupport._KotlinBridgeable> = ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<T>
public typealias _KSerializer = ExportedKotlinPackages.kotlinx.serialization._KSerializer
public typealias __KSerializer = ExportedKotlinPackages.kotlinx.serialization.__KSerializer
@_spi(kotlinx$serialization$ExperimentalSerializationApi)
public typealias MissingFieldException = ExportedKotlinPackages.kotlinx.serialization.MissingFieldException
@_spi(kotlinx$serialization$InternalSerializationApi)
public typealias PolymorphicSerializer<T: KotlinRuntimeSupport._KotlinBridgeable> = ExportedKotlinPackages.kotlinx.serialization.PolymorphicSerializer<T>
@_spi(kotlinx$serialization$InternalSerializationApi)
public typealias SealedClassSerializer<T: KotlinRuntimeSupport._KotlinBridgeable> = ExportedKotlinPackages.kotlinx.serialization.SealedClassSerializer<T>
public typealias SerialFormat = ExportedKotlinPackages.kotlinx.serialization.SerialFormat
public typealias _SerialFormat = ExportedKotlinPackages.kotlinx.serialization._SerialFormat
public typealias __SerialFormat = ExportedKotlinPackages.kotlinx.serialization.__SerialFormat
public typealias SerializationException = ExportedKotlinPackages.kotlinx.serialization.SerializationException
public typealias SerializationStrategy = ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy
public typealias AnySerializationStrategy<T: KotlinRuntimeSupport._KotlinBridgeable> = ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<T>
public typealias _SerializationStrategy = ExportedKotlinPackages.kotlinx.serialization._SerializationStrategy
public typealias __SerializationStrategy = ExportedKotlinPackages.kotlinx.serialization.__SerializationStrategy
public typealias StringFormat = ExportedKotlinPackages.kotlinx.serialization.StringFormat
public typealias _StringFormat = ExportedKotlinPackages.kotlinx.serialization._StringFormat
public typealias __StringFormat = ExportedKotlinPackages.kotlinx.serialization.__StringFormat
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
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private override init() {
        fatalError()
    }
}
@_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
public func serializer(
    kClass: Swift.Never,
    typeArgumentsSerializers: [ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>],
    isNullable: Swift.Bool
) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func serializer(
    type: Swift.Never
) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
    fatalError()
}
@_spi(kotlinx$serialization$InternalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
public func serializer<T: KotlinRuntimeSupport._KotlinBridgeable>(
    _ receiver: Swift.Never
) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T> {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func serializerOrNull(
    type: Swift.Never
) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>)? {
    fatalError()
}
@_spi(kotlinx$serialization$InternalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
public func serializerOrNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
    _ receiver: Swift.Never
) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>)? {
    fatalError()
}
extension ExportedKotlinPackages.kotlinx.serialization {
    public protocol BinaryFormat: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.SerialFormat, ExportedKotlinPackages.kotlinx.serialization._BinaryFormat {
        func decodeFromByteArray<T: KotlinRuntimeSupport._KotlinBridgeable>(
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
            bytes: ExportedKotlinPackages.kotlin.ByteArray
        ) -> T
        func encodeToByteArray<T: KotlinRuntimeSupport._KotlinBridgeable>(
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T
        ) -> ExportedKotlinPackages.kotlin.ByteArray
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_BinaryFormat)
    public protocol _BinaryFormat: ExportedKotlinPackages.kotlinx.serialization._SerialFormat {
    }
    public protocol __BinaryFormat: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.serialization.__SerialFormat {
    }
    public protocol DeserializationStrategy<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization._DeserializationStrategy {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get
        }
        func deserialize(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        ) -> T
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy)
    public protocol _DeserializationStrategy {
    }
    public protocol __DeserializationStrategy: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol KSerializer<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy, ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy, ExportedKotlinPackages.kotlinx.serialization._KSerializer {
        var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get
        }
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_KSerializer)
    public protocol _KSerializer: ExportedKotlinPackages.kotlinx.serialization._SerializationStrategy, ExportedKotlinPackages.kotlinx.serialization._DeserializationStrategy {
    }
    public protocol __KSerializer: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.serialization.__SerializationStrategy, ExportedKotlinPackages.kotlinx.serialization.__DeserializationStrategy {
    }
    public protocol SerialFormat: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization._SerialFormat {
        var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get
        }
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_SerialFormat)
    public protocol _SerialFormat {
    }
    public protocol __SerialFormat: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol SerializationStrategy<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization._SerializationStrategy {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get
        }
        func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: T
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy)
    public protocol _SerializationStrategy {
    }
    public protocol __SerializationStrategy: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol StringFormat: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.SerialFormat, ExportedKotlinPackages.kotlinx.serialization._StringFormat {
        func decodeFromString<T: KotlinRuntimeSupport._KotlinBridgeable>(
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
            string: Swift.String
        ) -> T
        func encodeToString<T: KotlinRuntimeSupport._KotlinBridgeable>(
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T
        ) -> Swift.String
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_StringFormat)
    public protocol _StringFormat: ExportedKotlinPackages.kotlinx.serialization._SerialFormat {
    }
    public protocol __StringFormat: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.serialization.__SerialFormat {
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public final class ContextualSerializer<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, ExportedKotlinPackages.kotlinx.serialization.KSerializer, ExportedKotlinPackages.kotlinx.serialization.__KSerializer {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_ContextualSerializer_descriptor_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public func deserialize(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        ) -> T {
            return T.__fromKotlinPointer(kotlinx_serialization_ContextualSerializer_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder__(self.__externalRCRef(), decoder.__externalRCRef()))
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: T
        ) -> Swift.Void {
            return { kotlinx_serialization_ContextualSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T__(self.__externalRCRef(), encoder.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public init(
            serializableClass: Swift.Never
        ) {
            fatalError()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public init(
            serializableClass: Swift.Never,
            fallbackSerializer: (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>)?,
            typeArgumentsSerializers: ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>>
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
    }
    public final class AnyDeserializationStrategy<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy, ExportedKotlinPackages.kotlinx.serialization.__DeserializationStrategy, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
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
    public final class AnyKSerializer<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.KSerializer, ExportedKotlinPackages.kotlinx.serialization.__KSerializer, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public final class MissingFieldException: ExportedKotlinPackages.kotlinx.serialization.SerializationException {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public var missingFields: [Swift.String] {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get {
                return kotlinx_serialization_MissingFieldException_missingFields_get(self.__externalRCRef()) as! Swift.Array<Swift.String>
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public init(
            missingFields: [Swift.String],
            serialName: Swift.String
        ) {
            let __kt = kotlinx_serialization_MissingFieldException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_MissingFieldException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_String__Swift_String__(__kt, missingFields, serialName); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public init(
            missingField: Swift.String,
            serialName: Swift.String
        ) {
            let __kt = kotlinx_serialization_MissingFieldException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_MissingFieldException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_String__(__kt, missingField, serialName); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public init(
            missingFields: [Swift.String],
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlinx_serialization_MissingFieldException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_MissingFieldException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_String__Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, missingFields, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public final class PolymorphicSerializer<T: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractPolymorphicSerializer<T> {
        @_spi(kotlinx$serialization$InternalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public var baseClass: Swift.Never {
            @_spi(kotlinx$serialization$InternalSerializationApi)
            get {
                fatalError()
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            @_spi(kotlinx$serialization$InternalSerializationApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_PolymorphicSerializer_descriptor_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public init(
            baseClass: Swift.Never
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
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public final class SealedClassSerializer<T: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractPolymorphicSerializer<T> {
        @_spi(kotlinx$serialization$InternalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public var baseClass: Swift.Never {
            @_spi(kotlinx$serialization$InternalSerializationApi)
            get {
                fatalError()
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            @_spi(kotlinx$serialization$InternalSerializationApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_SealedClassSerializer_descriptor_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public override func findPolymorphicSerializerOrNull(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder,
            klassName: Swift.String?
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>)? {
            return { () -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>)? in guard let res = kotlinx_serialization_SealedClassSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Swift_Optional_Swift_String___(self.__externalRCRef(), decoder.__externalRCRef(), klassName ?? nil) else { return nil }; return { let _ptr = res; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<T>.__createTypedWrapper(externalRCRef: _ptr) }() }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public override func findPolymorphicSerializerOrNull(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: T
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>)? {
            return { () -> (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>)? in guard let res = kotlinx_serialization_SealedClassSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T__(self.__externalRCRef(), encoder.__externalRCRef(), value.__retainedToKotlinPointer()) else { return nil }; return { let _ptr = res; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<T>.__createTypedWrapper(externalRCRef: _ptr) }() }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public init(
            serialName: Swift.String,
            baseClass: Swift.Never,
            subclasses: Swift.Never,
            subclassSerializers: ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<T>>
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
    }
    open class SerializationException: ExportedKotlinPackages.kotlin.IllegalArgumentException {
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlinx.serialization.SerializationException.self {
                 __kt = kotlinx_serialization_SerializationException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlinx.serialization.SerializationException.self {
                 __kt = kotlinx_serialization_SerializationException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlinx.serialization.SerializationException.self {
                 __kt = kotlinx_serialization_SerializationException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlinx.serialization.SerializationException.self {
                 __kt = kotlinx_serialization_SerializationException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnySerializationStrategy<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy, ExportedKotlinPackages.kotlinx.serialization.__SerializationStrategy, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
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
    @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func serializer(
        kClass: Swift.Never,
        typeArgumentsSerializers: [ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>],
        isNullable: Swift.Bool
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
        fatalError()
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func serializer(
        type: Swift.Never
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
        fatalError()
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func serializer<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: Swift.Never
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T> {
        fatalError()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        kClass: Swift.Never,
        typeArgumentsSerializers: [ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>],
        isNullable: Swift.Bool
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
        fatalError()
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        type: Swift.Never
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
        fatalError()
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func serializerOrNull(
        type: Swift.Never
    ) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>)? {
        fatalError()
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func serializerOrNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: Swift.Never
    ) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>)? {
        fatalError()
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func serializerOrNull(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        type: Swift.Never
    ) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>)? {
        fatalError()
    }
    public static func decodeFromHexString<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.BinaryFormat,
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
        hex: Swift.String
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_serialization_decodeFromHexString__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_BinaryFormat_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_String__(receiver.__externalRCRef(), deserializer.__externalRCRef(), hex))
    }
    public static func encodeToHexString<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.BinaryFormat,
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T
    ) -> Swift.String {
        return kotlinx_serialization_encodeToHexString__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_BinaryFormat_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T__(receiver.__externalRCRef(), serializer.__externalRCRef(), value.__retainedToKotlinPointer())
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public static func findPolymorphicSerializer<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractPolymorphicSerializer<T>,
        decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder,
        klassName: Swift.String?
    ) -> any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T> {
        return { let _ptr = kotlinx_serialization_findPolymorphicSerializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_serialization_U60internalU60_AbstractPolymorphicSerializer_T__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Swift_Optional_Swift_String___(receiver.__externalRCRef(), decoder.__externalRCRef(), klassName ?? nil); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<T>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public static func findPolymorphicSerializer<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractPolymorphicSerializer<T>,
        encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
        value: T
    ) -> any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T> {
        return { let _ptr = kotlinx_serialization_findPolymorphicSerializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_serialization_U60internalU60_AbstractPolymorphicSerializer_T__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T__(receiver.__externalRCRef(), encoder.__externalRCRef(), value.__retainedToKotlinPointer()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<T>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.modules {
    public enum SerializersModule_SealedType: KotlinRuntimeSupport.SealedType {
        case unknown(ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule_SealedType.Unknown)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule
            ) {
                self.value = value
            }
        }
        public var value: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get {
                switch self {
                case let .unknown(type): type.value
                }
            }
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public protocol SerializersModuleCollector: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.modules._SerializersModuleCollector {
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModuleCollector)
    public protocol _SerializersModuleCollector {
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public protocol __SerializersModuleCollector: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class PolymorphicModuleBuilder<Base: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<Base> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @available(*, deprecated, message: "Deprecated in favor of function with more precise name: defaultDeserializer. Replacement: defaultDeserializer(defaultSerializerProvider)")
        public func `default`(
            defaultSerializerProvider: @escaping (Swift.String?) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Base>)?
        ) -> Swift.Void {
            return { kotlinx_serialization_modules_PolymorphicModuleBuilder_default__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Base____(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.String>) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: Swift.Optional<any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Base>> = (defaultSerializerProvider)(_p0); return { if let it = _res { return KotlinRuntimeSupport._cloneBridgeableRef((it).__externalRCRef()) } else { return nil } }() } as (Swift.Optional<Swift.String>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
        }
        public func defaultDeserializer(
            defaultDeserializerProvider: @escaping (Swift.String?) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Base>)?
        ) -> Swift.Void {
            return { kotlinx_serialization_modules_PolymorphicModuleBuilder_defaultDeserializer__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Base____(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.String>) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: Swift.Optional<any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Base>> = (defaultDeserializerProvider)(_p0); return { if let it = _res { return KotlinRuntimeSupport._cloneBridgeableRef((it).__externalRCRef()) } else { return nil } }() } as (Swift.Optional<Swift.String>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
        }
        @available(*, unavailable, message: "Declaration uses unsupported types")
        public func subclass<T: KotlinRuntimeSupport._KotlinBridgeable>(
            subclass: Swift.Never,
            serializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>
        ) -> Swift.Void {
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
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<Base>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    open class SerializersModule: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func dumpTo(
            collector: any ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.self {
                return { kotlinx_serialization_modules_SerializersModule_dumpTo__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModuleCollector__(self.__externalRCRef(), collector.__externalRCRef()); return () }()
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.dumpTo': a Swift subclass must override it and must not call super.")
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        open func getContextual<T: KotlinRuntimeSupport._KotlinBridgeable>(
            kClass: Swift.Never,
            typeArgumentsSerializers: [ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>]
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>)? {
            fatalError()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        open func getPolymorphic<T: KotlinRuntimeSupport._KotlinBridgeable>(
            baseClass: Swift.Never,
            value: T
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>)? {
            fatalError()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        open func getPolymorphic<T: KotlinRuntimeSupport._KotlinBridgeable>(
            baseClass: Swift.Never,
            serializedClassName: Swift.String?
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>)? {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule_SealedType {
            switch self {
            default: .unknown(.init(self))
            }
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public final class SerializersModuleBuilder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector, ExportedKotlinPackages.kotlinx.serialization.modules.__SerializersModuleCollector {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public func contextual(
            kClass: Swift.Never,
            provider: @escaping ([ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>]) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer
        ) -> Swift.Void {
            fatalError()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public func contextual<T: KotlinRuntimeSupport._KotlinBridgeable>(
            kClass: Swift.Never,
            serializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>
        ) -> Swift.Void {
            fatalError()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public func include(
            module: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule
        ) -> Swift.Void {
            return { kotlinx_serialization_modules_SerializersModuleBuilder_include__TypesOfArguments__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule__(self.__externalRCRef(), module.__externalRCRef()); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public func polymorphic<Sub: KotlinRuntimeSupport._KotlinBridgeable>(
            baseClass: Swift.Never,
            actualClass: Swift.Never,
            actualSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Sub>
        ) -> Swift.Void {
            fatalError()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public func polymorphicDefaultDeserializer<Base: KotlinRuntimeSupport._KotlinBridgeable>(
            baseClass: Swift.Never,
            defaultDeserializerProvider: @escaping (Swift.String?) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Base>)?
        ) -> Swift.Void {
            fatalError()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public func polymorphicDefaultSerializer<Base: KotlinRuntimeSupport._KotlinBridgeable>(
            baseClass: Swift.Never,
            defaultSerializerProvider: @escaping (Base) -> (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Base>)?
        ) -> Swift.Void {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, deprecated, message: "Deprecated in the favour of 'EmptySerializersModule()'. Replacement: EmptySerializersModule()")
    public static var EmptySerializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_modules_EmptySerializersModule_get())
        }
    }
    public static func emptySerializersModule() -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_modules_EmptySerializersModule())
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func serializersModule(
        builderAction: @escaping (ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleBuilder) -> Swift.Void
    ) -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_modules_SerializersModule__TypesOfArguments__U28ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModuleBuilderU29202D_U20Swift_Void__(Unmanaged.passRetained((builderAction as (ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleBuilder) -> Swift.Void) as AnyObject).toOpaque()))
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func serializersModuleOf<T: KotlinRuntimeSupport._KotlinBridgeable>(
        kClass: Swift.Never,
        serializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>
    ) -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        fatalError()
    }
    public static func overwriteWith(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        other: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule
    ) -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_modules_overwriteWith__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule__(receiver.__externalRCRef(), other.__externalRCRef()))
    }
    public static func plus(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        other: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule
    ) -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_modules_plus__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule__(receiver.__externalRCRef(), other.__externalRCRef()))
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func polymorphic<Base: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleBuilder,
        baseClass: Swift.Never,
        baseSerializer: (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Base>)?,
        builderAction: @escaping (ExportedKotlinPackages.kotlinx.serialization.modules.PolymorphicModuleBuilder<Base>) -> Swift.Void
    ) -> Swift.Void {
        fatalError()
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding {
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public protocol ChunkedDecoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding._ChunkedDecoder {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func decodeStringChunked(
            consumeChunk: @escaping (Swift.String) -> Swift.Void
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_encoding_ChunkedDecoder)
    public protocol _ChunkedDecoder {
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public protocol __ChunkedDecoder: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol CompositeDecoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeDecoder {
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
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func decodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<T>>,
            previousValue: T?
        ) -> T?
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func decodeSequentially() -> Swift.Bool
        func decodeSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
            previousValue: T?
        ) -> T
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
    @objc(_ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder)
    public protocol _CompositeDecoder {
    }
    public protocol __CompositeDecoder: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol CompositeEncoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeEncoder {
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
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func encodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T?
        ) -> Swift.Void
        func encodeSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T
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
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func shouldEncodeElementDefault(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeEncoder)
    public protocol _CompositeEncoder {
    }
    public protocol __CompositeEncoder: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol Decoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding._Decoder {
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
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func decodeNotNullMark() -> Swift.Bool
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func decodeNull() -> Swift.Never?
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func decodeNullableSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<T>>
        ) -> T?
        func decodeSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>
        ) -> T
        func decodeShort() -> Swift.Int16
        func decodeString() -> Swift.String
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder)
    public protocol _Decoder {
    }
    public protocol __Decoder: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol Encoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding._Encoder {
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
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func encodeNotNullMark() -> Swift.Void
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func encodeNull() -> Swift.Void
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func encodeNullableSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T?
        ) -> Swift.Void
        func encodeSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T
        ) -> Swift.Void
        func encodeShort(
            value: Swift.Int16
        ) -> Swift.Void
        func encodeString(
            value: Swift.String
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder)
    public protocol _Encoder {
    }
    public protocol __Encoder: KotlinRuntimeSupport._KotlinBridgeable {
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    open class AbstractDecoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__Decoder, ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__CompositeDecoder {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                    return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_serializersModule_get(self.__externalRCRef()))
                } else {
                    fatalError("Cannot invoke the inherited implementation of abstract property 'ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.serializersModule': a Swift subclass must override it and must not call super.")
                }
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
            } else {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeBoolean() -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeBoolean(self.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeBoolean_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func decodeBooleanElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Bool {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeByte() -> Swift.Int8 {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeByte(self.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeByte_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func decodeByteElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int8 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeChar() -> Swift.Unicode.UTF16.CodeUnit {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeChar(self.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeChar_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func decodeCharElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeDouble() -> Swift.Double {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeDouble(self.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeDouble_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func decodeDoubleElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Double {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeEnum(
            enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Int32 {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), enumDescriptor.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), enumDescriptor.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeFloat() -> Swift.Float {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeFloat(self.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeFloat_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func decodeFloatElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Float {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
            } else {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
            } else {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32___direct(self.__externalRCRef(), descriptor.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeInt() -> Swift.Int32 {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeInt(self.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeInt_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func decodeIntElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int32 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeLong() -> Swift.Int64 {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeLong(self.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeLong_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func decodeLongElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int64 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeNotNullMark() -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeNotNullMark(self.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeNotNullMark_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeNull() -> Swift.Never? {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return { kotlinx_serialization_encoding_AbstractDecoder_decodeNull(self.__externalRCRef()); return nil }()
            } else {
                return { kotlinx_serialization_encoding_AbstractDecoder_decodeNull_direct(self.__externalRCRef()); return nil }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func decodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<T>>,
            previousValue: T?
        ) -> T? {
            return Swift.Optional<T>.__fromKotlinPointer(kotlinx_serialization_encoding_AbstractDecoder_decodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Swift_Optional_T___Swift_Optional_T___(self.__externalRCRef(), descriptor.__externalRCRef(), index, deserializer.__externalRCRef(), previousValue.__retainedToKotlinPointer()))
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
            previousValue: T?
        ) -> T {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return T.__fromKotlinPointer(kotlinx_serialization_encoding_AbstractDecoder_decodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T___(self.__externalRCRef(), descriptor.__externalRCRef(), index, deserializer.__externalRCRef(), previousValue.__retainedToKotlinPointer()))
            } else {
                return T.__fromKotlinPointer(kotlinx_serialization_encoding_AbstractDecoder_decodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T____direct(self.__externalRCRef(), descriptor.__externalRCRef(), index, deserializer.__externalRCRef(), previousValue.__retainedToKotlinPointer()))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
            previousValue: T?
        ) -> T {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return T.__fromKotlinPointer(kotlinx_serialization_encoding_AbstractDecoder_decodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T___(self.__externalRCRef(), deserializer.__externalRCRef(), previousValue.__retainedToKotlinPointer()))
            } else {
                return T.__fromKotlinPointer(kotlinx_serialization_encoding_AbstractDecoder_decodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T____direct(self.__externalRCRef(), deserializer.__externalRCRef(), previousValue.__retainedToKotlinPointer()))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeShort() -> Swift.Int16 {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeShort(self.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeShort_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func decodeShortElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int16 {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeString() -> Swift.String {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeString(self.__externalRCRef())
            } else {
                return kotlinx_serialization_encoding_AbstractDecoder_decodeString_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func decodeStringElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.String {
            return kotlinx_serialization_encoding_AbstractDecoder_decodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func decodeValue() -> any KotlinRuntimeSupport._KotlinBridgeable {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeValue(self.__externalRCRef()))
            } else {
                return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeValue_direct(self.__externalRCRef()))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self {
                return { kotlinx_serialization_encoding_AbstractDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.self, "ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_encoding_AbstractDecoder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    open class AbstractEncoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__Encoder, ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__CompositeEncoder {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                    return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_serializersModule_get(self.__externalRCRef()))
                } else {
                    fatalError("Cannot invoke the inherited implementation of abstract property 'ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.serializersModule': a Swift subclass must override it and must not call super.")
                }
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
            } else {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeBoolean(
            value: Swift.Bool
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeBoolean__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), value); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeBoolean__TypesOfArguments__Swift_Bool___direct(self.__externalRCRef(), value); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func encodeBooleanElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Bool
        ) -> Swift.Void {
            return { kotlinx_serialization_encoding_AbstractEncoder_encodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Bool__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeByte(
            value: Swift.Int8
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeByte__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), value); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeByte__TypesOfArguments__Swift_Int8___direct(self.__externalRCRef(), value); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func encodeByteElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int8
        ) -> Swift.Void {
            return { kotlinx_serialization_encoding_AbstractEncoder_encodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int8__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeChar(
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), value); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit___direct(self.__externalRCRef(), value); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func encodeCharElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void {
            return { kotlinx_serialization_encoding_AbstractEncoder_encodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeDouble(
            value: Swift.Double
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeDouble__TypesOfArguments__Swift_Double__(self.__externalRCRef(), value); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeDouble__TypesOfArguments__Swift_Double___direct(self.__externalRCRef(), value); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func encodeDoubleElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Double
        ) -> Swift.Void {
            return { kotlinx_serialization_encoding_AbstractEncoder_encodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Double__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return kotlinx_serialization_encoding_AbstractEncoder_encodeElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
            } else {
                return kotlinx_serialization_encoding_AbstractEncoder_encodeElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32___direct(self.__externalRCRef(), descriptor.__externalRCRef(), index)
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeEnum(
            enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), enumDescriptor.__externalRCRef(), index); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32___direct(self.__externalRCRef(), enumDescriptor.__externalRCRef(), index); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeFloat(
            value: Swift.Float
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeFloat__TypesOfArguments__Swift_Float__(self.__externalRCRef(), value); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeFloat__TypesOfArguments__Swift_Float___direct(self.__externalRCRef(), value); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func encodeFloatElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Float
        ) -> Swift.Void {
            return { kotlinx_serialization_encoding_AbstractEncoder_encodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Float__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
            } else {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func encodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_encodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeInt(
            value: Swift.Int32
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeInt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), value); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeInt__TypesOfArguments__Swift_Int32___direct(self.__externalRCRef(), value); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func encodeIntElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int32
        ) -> Swift.Void {
            return { kotlinx_serialization_encoding_AbstractEncoder_encodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeLong(
            value: Swift.Int64
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeLong__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), value); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeLong__TypesOfArguments__Swift_Int64___direct(self.__externalRCRef(), value); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func encodeLongElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int64
        ) -> Swift.Void {
            return { kotlinx_serialization_encoding_AbstractEncoder_encodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int64__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeNull() -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeNull(self.__externalRCRef()); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeNull_direct(self.__externalRCRef()); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T?
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T___(self.__externalRCRef(), descriptor.__externalRCRef(), index, serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T____direct(self.__externalRCRef(), descriptor.__externalRCRef(), index, serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T__(self.__externalRCRef(), descriptor.__externalRCRef(), index, serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T___direct(self.__externalRCRef(), descriptor.__externalRCRef(), index, serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeShort(
            value: Swift.Int16
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeShort__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), value); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeShort__TypesOfArguments__Swift_Int16___direct(self.__externalRCRef(), value); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func encodeShortElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int16
        ) -> Swift.Void {
            return { kotlinx_serialization_encoding_AbstractEncoder_encodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int16__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeString(
            value: Swift.String
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeString__TypesOfArguments__Swift_String__(self.__externalRCRef(), value); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeString__TypesOfArguments__Swift_String___direct(self.__externalRCRef(), value); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final func encodeStringElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.String
        ) -> Swift.Void {
            return { kotlinx_serialization_encoding_AbstractEncoder_encodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_String__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func encodeValue(
            value: any KotlinRuntimeSupport._KotlinBridgeable
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeValue__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), (value).__retainedToKotlinPointer()!); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_encodeValue__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable___direct(self.__externalRCRef(), (value).__retainedToKotlinPointer()!); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self {
                return { kotlinx_serialization_encoding_AbstractEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()); return () }()
            } else {
                return { kotlinx_serialization_encoding_AbstractEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.self, "ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_encoding_AbstractEncoder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public static func decodeStructure<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        block: @escaping (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder) -> T
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_serialization_encoding_decodeStructure__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_U28anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoderU29202D_U20T__(receiver.__externalRCRef(), descriptor.__externalRCRef(), Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    public static func encodeCollection(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        collectionSize: Swift.Int32,
        block: @escaping (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder) -> Swift.Void
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_encodeCollection__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_U28anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeEncoderU29202D_U20Swift_Void__(receiver.__externalRCRef(), descriptor.__externalRCRef(), collectionSize, Unmanaged.passRetained((block as (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder) -> Swift.Void) as AnyObject).toOpaque()); return () }()
    }
    public static func encodeCollection<E: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        collection: any ExportedKotlinPackages.kotlin.collections.Collection<E>,
        block: @escaping (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder, Swift.Int32, E) -> Swift.Void
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_encodeCollection__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_anyU20ExportedKotlinPackages_kotlin_collections_Collection_E__U28anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeEncoder_U20Swift_Int32_U20EU29202D_U20Swift_Void__(receiver.__externalRCRef(), descriptor.__externalRCRef(), collection.__externalRCRef(), Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder, _p1: Swift.Int32, _p2: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (block)(_p0, _p1, E.__fromKotlinPointer(_p2)) } as (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder, Swift.Int32, Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
    }
    public static func encodeStructure(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        block: @escaping (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder) -> Swift.Void
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_encodeStructure__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_U28anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeEncoderU29202D_U20Swift_Void__(receiver.__externalRCRef(), descriptor.__externalRCRef(), Unmanaged.passRetained((block as (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder) -> Swift.Void) as AnyObject).toOpaque()); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.`internal` {
    @_spi(kotlinx$serialization$InternalSerializationApi) @frozen
    public enum AbstractCollectionSerializer_SealedType<Element: KotlinRuntimeSupport._KotlinBridgeable, Collection: KotlinRuntimeSupport._KotlinBridgeable, Builder: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntimeSupport.SealedType {
        @_spi(kotlinx$serialization$InternalSerializationApi)
        case mapLikeSerializer(ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer_SealedType<KotlinRuntimeSupport.KotlinAny,KotlinRuntimeSupport.KotlinAny,Collection,Builder>)
        case unknown(ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer_SealedType<Element,Collection,Builder>.Unknown)
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer<Element,Collection,Builder>
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer<Element,Collection,Builder>
            ) {
                self.value = value
            }
        }
        public var value: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer<Element,Collection,Builder> {
            get {
                switch self {
                case let .mapLikeSerializer(type): ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer<Element,Collection,Builder>.__createTypedWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(type.value.__externalRCRef()))
                case let .unknown(type): type.value
                }
            }
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @frozen
    public enum MapLikeSerializer_SealedType<Key: KotlinRuntimeSupport._KotlinBridgeable, Value: KotlinRuntimeSupport._KotlinBridgeable, Collection: KotlinRuntimeSupport._KotlinBridgeable, Builder: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntimeSupport.SealedType {
        case unknown(ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer_SealedType<Key,Value,Collection,Builder>.Unknown)
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer<Key,Value,Collection,Builder>
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer<Key,Value,Collection,Builder>
            ) {
                self.value = value
            }
        }
        public var value: ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer<Key,Value,Collection,Builder> {
            get {
                switch self {
                case let .unknown(type): type.value
                }
            }
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @_documentation(visibility: internal)
    public protocol _AbstractCollectionSerializer_ReverseDispatch<Element, Collection, Builder>: KotlinRuntime.KotlinBase {
        associatedtype Element: KotlinRuntimeSupport._KotlinBridgeable
        associatedtype Collection: KotlinRuntimeSupport._KotlinBridgeable
        associatedtype Builder: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func deserialize(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        ) -> Collection
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: Collection
        ) -> Swift.Void
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @_documentation(visibility: internal)
    public protocol _AbstractPolymorphicSerializer_ReverseDispatch<T>: KotlinRuntime.KotlinBase {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func findPolymorphicSerializerOrNull(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder,
            klassName: Swift.String?
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>)?
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func findPolymorphicSerializerOrNull(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: T
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>)?
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public protocol GeneratedSerializer<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.KSerializer, ExportedKotlinPackages.kotlinx.serialization.`internal`._GeneratedSerializer {
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func childSerializers() -> ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>>
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func typeParametersSerializers() -> ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>>
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_internal_GeneratedSerializer)
    public protocol _GeneratedSerializer: ExportedKotlinPackages.kotlinx.serialization._KSerializer {
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public protocol __GeneratedSerializer: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.serialization.__KSerializer {
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @_documentation(visibility: internal)
    public protocol _MapLikeSerializer_ReverseDispatch<Key, Value, Collection, Builder>: KotlinRuntime.KotlinBase {
        associatedtype Key: KotlinRuntimeSupport._KotlinBridgeable
        associatedtype Value: KotlinRuntimeSupport._KotlinBridgeable
        associatedtype Collection: KotlinRuntimeSupport._KotlinBridgeable
        associatedtype Builder: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$serialization$InternalSerializationApi)
        var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: Collection
        ) -> Swift.Void
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @_documentation(visibility: internal)
    public protocol _TaggedDecoder_ReverseDispatch<Tag>: KotlinRuntime.KotlinBase {
        associatedtype Tag: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$serialization$InternalSerializationApi)
        var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func decodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
        func decodeNotNullMark() -> Swift.Bool
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @_documentation(visibility: internal)
    public protocol _TaggedEncoder_ReverseDispatch<Tag>: KotlinRuntime.KotlinBase {
        associatedtype Tag: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$serialization$InternalSerializationApi)
        var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func encodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
        func encodeNotNullMark() -> Swift.Void
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
        func encodeNull() -> Swift.Void
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
        func encodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T?
        ) -> Swift.Void
        @_spi(kotlinx$serialization$InternalSerializationApi)
        func encodeSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T
        ) -> Swift.Void
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    open class AbstractCollectionSerializer<Element: KotlinRuntimeSupport._KotlinBridgeable, Collection: KotlinRuntimeSupport._KotlinBridgeable, Builder: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractCollectionSerializer_Base, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractCollectionSerializer_ReverseDispatch, ExportedKotlinPackages.kotlinx.serialization.KSerializer, ExportedKotlinPackages.kotlinx.serialization.__KSerializer {
        public typealias T = Collection
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func deserialize(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        ) -> Collection {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer<Element,Collection,Builder>.self {
                return Collection.__fromKotlinPointer(kotlinx_serialization_internal_AbstractCollectionSerializer_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder__(self.__externalRCRef(), decoder.__externalRCRef()))
            } else {
                return Collection.__fromKotlinPointer(kotlinx_serialization_internal_AbstractCollectionSerializer_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder___direct(self.__externalRCRef(), decoder.__externalRCRef()))
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func merge(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder,
            previous: Collection?
        ) -> Collection {
            return Collection.__fromKotlinPointer(kotlinx_serialization_internal_AbstractCollectionSerializer_merge__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder_Swift_Optional_Collection___(self.__externalRCRef(), decoder.__externalRCRef(), previous.__retainedToKotlinPointer()))
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: Collection
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer<Element,Collection,Builder>.self {
                return { kotlinx_serialization_internal_AbstractCollectionSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Collection__(self.__externalRCRef(), encoder.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer.serialize': a Swift subclass must override it and must not call super.")
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer_SealedType<Element,Collection,Builder> {
            switch kotlinx_serialization_internal_AbstractCollectionSerializer_sealedType_tag(self.__externalRCRef()) {
            case 0: .mapLikeSerializer(ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer<KotlinRuntimeSupport.KotlinAny,KotlinRuntimeSupport.KotlinAny,Collection,Builder>.__fromKotlinPointer(KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef())).sealedType())
            default: .unknown(.init(self))
            }
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @_documentation(visibility: internal)
    open class _AbstractCollectionSerializer_Base: KotlinRuntime.KotlinBase {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    open class AbstractPolymorphicSerializer<T: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractPolymorphicSerializer_Base, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractPolymorphicSerializer_ReverseDispatch, ExportedKotlinPackages.kotlinx.serialization.KSerializer, ExportedKotlinPackages.kotlinx.serialization.__KSerializer {
        @_spi(kotlinx$serialization$InternalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        open var baseClass: Swift.Never {
            @_spi(kotlinx$serialization$InternalSerializationApi)
            get {
                fatalError()
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func deserialize(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        ) -> T {
            return T.__fromKotlinPointer(kotlinx_serialization_internal_AbstractPolymorphicSerializer_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder__(self.__externalRCRef(), decoder.__externalRCRef()))
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func findPolymorphicSerializerOrNull(
            decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder,
            klassName: Swift.String?
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>)? {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractPolymorphicSerializer<T>.self {
                return { () -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>)? in guard let res = kotlinx_serialization_internal_AbstractPolymorphicSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Swift_Optional_Swift_String___(self.__externalRCRef(), decoder.__externalRCRef(), klassName ?? nil) else { return nil }; return { let _ptr = res; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<T>.__createTypedWrapper(externalRCRef: _ptr) }() }()
            } else {
                return { () -> (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>)? in guard let res = kotlinx_serialization_internal_AbstractPolymorphicSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Swift_Optional_Swift_String____direct(self.__externalRCRef(), decoder.__externalRCRef(), klassName ?? nil) else { return nil }; return { let _ptr = res; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<T>.__createTypedWrapper(externalRCRef: _ptr) }() }()
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func findPolymorphicSerializerOrNull(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: T
        ) -> (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>)? {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractPolymorphicSerializer<T>.self {
                return { () -> (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>)? in guard let res = kotlinx_serialization_internal_AbstractPolymorphicSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T__(self.__externalRCRef(), encoder.__externalRCRef(), value.__retainedToKotlinPointer()) else { return nil }; return { let _ptr = res; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<T>.__createTypedWrapper(externalRCRef: _ptr) }() }()
            } else {
                return { () -> (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>)? in guard let res = kotlinx_serialization_internal_AbstractPolymorphicSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T___direct(self.__externalRCRef(), encoder.__externalRCRef(), value.__retainedToKotlinPointer()) else { return nil }; return { let _ptr = res; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<T>.__createTypedWrapper(externalRCRef: _ptr) }() }()
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: T
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_AbstractPolymorphicSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T__(self.__externalRCRef(), encoder.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @_documentation(visibility: internal)
    open class _AbstractPolymorphicSerializer_Base: KotlinRuntime.KotlinBase {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public final class AnyGeneratedSerializer<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.`internal`.GeneratedSerializer, ExportedKotlinPackages.kotlinx.serialization.`internal`.__GeneratedSerializer, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    open class MapLikeSerializer<Key: KotlinRuntimeSupport._KotlinBridgeable, Value: KotlinRuntimeSupport._KotlinBridgeable, Collection: KotlinRuntimeSupport._KotlinBridgeable, Builder: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<Key,Value>,Collection,Builder>, ExportedKotlinPackages.kotlinx.serialization.`internal`._MapLikeSerializer_ReverseDispatch {
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            @_spi(kotlinx$serialization$InternalSerializationApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer<Key,Value,Collection,Builder>.self {
                    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_MapLikeSerializer_descriptor_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
                } else {
                    fatalError("Cannot invoke the inherited implementation of abstract property 'ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer.descriptor': a Swift subclass must override it and must not call super.")
                }
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final var keySerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Key> {
            @_spi(kotlinx$serialization$InternalSerializationApi)
            get {
                return { let _ptr = kotlinx_serialization_internal_MapLikeSerializer_keySerializer_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Key>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Key>.__createTypedWrapper(externalRCRef: _ptr) }()
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final var valueSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Value> {
            @_spi(kotlinx$serialization$InternalSerializationApi)
            get {
                return { let _ptr = kotlinx_serialization_internal_MapLikeSerializer_valueSerializer_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Value>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Value>.__createTypedWrapper(externalRCRef: _ptr) }()
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open override func serialize(
            encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
            value: Collection
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer<Key,Value,Collection,Builder>.self {
                return { kotlinx_serialization_internal_MapLikeSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Collection__(self.__externalRCRef(), encoder.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            } else {
                return { kotlinx_serialization_internal_MapLikeSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Collection___direct(self.__externalRCRef(), encoder.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer_SealedType<Key,Value,Collection,Builder> {
            switch 0 {
            default: .unknown(.init(self))
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi) @_disfavoredOverload
        public final override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractCollectionSerializer_SealedType<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<Key,Value>,Collection,Builder> {
            .mapLikeSerializer(ExportedKotlinPackages.kotlinx.serialization.`internal`.MapLikeSerializer<KotlinRuntimeSupport.KotlinAny,KotlinRuntimeSupport.KotlinAny,Collection,Builder>.__fromKotlinPointer(KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef())).sealedType())
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    open class NamedValueDecoder: ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedDecoder<Swift.String> {
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.serialization.`internal`.NamedValueDecoder.self, "ExportedKotlinPackages.kotlinx.serialization.`internal`.NamedValueDecoder is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_internal_NamedValueDecoder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    open class NamedValueEncoder: ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder<Swift.String> {
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.serialization.`internal`.NamedValueEncoder.self, "ExportedKotlinPackages.kotlinx.serialization.`internal`.NamedValueEncoder is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_internal_NamedValueEncoder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    open class TaggedDecoder<Tag: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_Base, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch, ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__Decoder, ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__CompositeDecoder {
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            @_spi(kotlinx$serialization$InternalSerializationApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedDecoder<Tag>.self {
                    return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_serializersModule_get(self.__externalRCRef()))
                } else {
                    return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_serializersModule_get_direct(self.__externalRCRef()))
                }
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedDecoder<Tag>.self {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
            } else {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeBoolean() -> Swift.Bool {
            return kotlinx_serialization_internal_TaggedDecoder_decodeBoolean(self.__externalRCRef())
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeBooleanElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Bool {
            return kotlinx_serialization_internal_TaggedDecoder_decodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeByte() -> Swift.Int8 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeByte(self.__externalRCRef())
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeByteElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int8 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlinx_serialization_internal_TaggedDecoder_decodeChar(self.__externalRCRef())
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeCharElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlinx_serialization_internal_TaggedDecoder_decodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeDouble() -> Swift.Double {
            return kotlinx_serialization_internal_TaggedDecoder_decodeDouble(self.__externalRCRef())
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeDoubleElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Double {
            return kotlinx_serialization_internal_TaggedDecoder_decodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeEnum(
            enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Int32 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), enumDescriptor.__externalRCRef())
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeFloat() -> Swift.Float {
            return kotlinx_serialization_internal_TaggedDecoder_decodeFloat(self.__externalRCRef())
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeFloatElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Float {
            return kotlinx_serialization_internal_TaggedDecoder_decodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func decodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedDecoder<Tag>.self {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
            } else {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeInt() -> Swift.Int32 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeInt(self.__externalRCRef())
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeIntElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int32 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeLong() -> Swift.Int64 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeLong(self.__externalRCRef())
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeLongElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int64 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
        open func decodeNotNullMark() -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedDecoder<Tag>.self {
                return kotlinx_serialization_internal_TaggedDecoder_decodeNotNullMark(self.__externalRCRef())
            } else {
                return kotlinx_serialization_internal_TaggedDecoder_decodeNotNullMark_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeNull() -> Swift.Never? {
            return { kotlinx_serialization_internal_TaggedDecoder_decodeNull(self.__externalRCRef()); return nil }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<T>>,
            previousValue: T?
        ) -> T? {
            return Swift.Optional<T>.__fromKotlinPointer(kotlinx_serialization_internal_TaggedDecoder_decodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Swift_Optional_T___Swift_Optional_T___(self.__externalRCRef(), descriptor.__externalRCRef(), index, deserializer.__externalRCRef(), previousValue.__retainedToKotlinPointer()))
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
            previousValue: T?
        ) -> T {
            return T.__fromKotlinPointer(kotlinx_serialization_internal_TaggedDecoder_decodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T___(self.__externalRCRef(), descriptor.__externalRCRef(), index, deserializer.__externalRCRef(), previousValue.__retainedToKotlinPointer()))
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeShort() -> Swift.Int16 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeShort(self.__externalRCRef())
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeShortElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Int16 {
            return kotlinx_serialization_internal_TaggedDecoder_decodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeString() -> Swift.String {
            return kotlinx_serialization_internal_TaggedDecoder_decodeString(self.__externalRCRef())
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func decodeStringElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.String {
            return kotlinx_serialization_internal_TaggedDecoder_decodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedDecoder<Tag>.self {
                return { kotlinx_serialization_internal_TaggedDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()); return () }()
            } else {
                return { kotlinx_serialization_internal_TaggedDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()); return () }()
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedDecoder<Tag>.self, "ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedDecoder is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_internal_TaggedDecoder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @_documentation(visibility: internal)
    open class _TaggedDecoder_Base: KotlinRuntime.KotlinBase {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    open class TaggedEncoder<Tag: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_Base, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch, ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__Encoder, ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__CompositeEncoder {
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            @_spi(kotlinx$serialization$InternalSerializationApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder<Tag>.self {
                    return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_serializersModule_get(self.__externalRCRef()))
                } else {
                    return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_serializersModule_get_direct(self.__externalRCRef()))
                }
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder<Tag>.self {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
            } else {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeBoolean(
            value: Swift.Bool
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeBoolean__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeBooleanElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Bool
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Bool__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeByte(
            value: Swift.Int8
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeByte__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeByteElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int8
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int8__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeChar(
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeCharElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeDouble(
            value: Swift.Double
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeDouble__TypesOfArguments__Swift_Double__(self.__externalRCRef(), value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeDoubleElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Double
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Double__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeEnum(
            enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), enumDescriptor.__externalRCRef(), index); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeFloat(
            value: Swift.Float
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeFloat__TypesOfArguments__Swift_Float__(self.__externalRCRef(), value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeFloatElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Float
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Float__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func encodeInline(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder<Tag>.self {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
            } else {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_TaggedEncoder_encodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeInt(
            value: Swift.Int32
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeInt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeIntElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int32
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeLong(
            value: Swift.Int64
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeLong__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeLongElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int64
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int64__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
        open func encodeNotNullMark() -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder<Tag>.self {
                return { kotlinx_serialization_internal_TaggedEncoder_encodeNotNullMark(self.__externalRCRef()); return () }()
            } else {
                return { kotlinx_serialization_internal_TaggedEncoder_encodeNotNullMark_direct(self.__externalRCRef()); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
        open func encodeNull() -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder<Tag>.self {
                return { kotlinx_serialization_internal_TaggedEncoder_encodeNull(self.__externalRCRef()); return () }()
            } else {
                return { kotlinx_serialization_internal_TaggedEncoder_encodeNull_direct(self.__externalRCRef()); return () }()
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
        open func encodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T?
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder<Tag>.self {
                return { kotlinx_serialization_internal_TaggedEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T___(self.__externalRCRef(), descriptor.__externalRCRef(), index, serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            } else {
                return { kotlinx_serialization_internal_TaggedEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T____direct(self.__externalRCRef(), descriptor.__externalRCRef(), index, serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        open func encodeSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
            value: T
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder<Tag>.self {
                return { kotlinx_serialization_internal_TaggedEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T__(self.__externalRCRef(), descriptor.__externalRCRef(), index, serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            } else {
                return { kotlinx_serialization_internal_TaggedEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T___direct(self.__externalRCRef(), descriptor.__externalRCRef(), index, serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
            }
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeShort(
            value: Swift.Int16
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeShort__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeShortElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.Int16
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int16__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeString(
            value: Swift.String
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeString__TypesOfArguments__Swift_String__(self.__externalRCRef(), value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func encodeStringElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            value: Swift.String
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_encodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_String__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public final func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void {
            return { kotlinx_serialization_internal_TaggedEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()); return () }()
        }
        @_spi(kotlinx$serialization$InternalSerializationApi)
        public init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder<Tag>.self, "ExportedKotlinPackages.kotlinx.serialization.`internal`.TaggedEncoder is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_serialization_internal_TaggedEncoder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi) @_documentation(visibility: internal)
    open class _TaggedEncoder_Base: KotlinRuntime.KotlinBase {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public static func InlinePrimitiveDescriptor<T: KotlinRuntimeSupport._KotlinBridgeable>(
        name: Swift.String,
        primitiveSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_internal_InlinePrimitiveDescriptor__TypesOfArguments__Swift_String_anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_T___(name, primitiveSerializer.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public static func throwArrayMissingFieldException(
        seenArray: ExportedKotlinPackages.kotlin.IntArray,
        goldenMaskArray: ExportedKotlinPackages.kotlin.IntArray,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Void {
        return { kotlinx_serialization_internal_throwArrayMissingFieldException__TypesOfArguments__ExportedKotlinPackages_kotlin_IntArray_ExportedKotlinPackages_kotlin_IntArray_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(seenArray.__externalRCRef(), goldenMaskArray.__externalRCRef(), descriptor.__externalRCRef()); return () }()
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public static func throwMissingFieldException(
        seen: Swift.Int32,
        goldenMask: Swift.Int32,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Void {
        return { kotlinx_serialization_internal_throwMissingFieldException__TypesOfArguments__Swift_Int32_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(seen, goldenMask, descriptor.__externalRCRef()); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.builtins {
    public final class LongAsStringSerializer: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.KSerializer, ExportedKotlinPackages.kotlinx.serialization.__KSerializer {
        public typealias T = Swift.Int64
        public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_builtins_LongAsStringSerializer_descriptor_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
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
            return { kotlinx_serialization_builtins_LongAsStringSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Swift_Int64__(self.__externalRCRef(), encoder.__externalRCRef(), value); return () }()
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
    @inlinable
    public static func getNullable<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<T>> {
        return { let _ptr = _cbridge_kotlinx_serialization_builtins_nullable_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_T___(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<T>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<T>>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func ArraySerializer<E: KotlinRuntimeSupport._KotlinBridgeable>(
        kClass: Swift.Never,
        elementSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<E>
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.Array<E>> {
        fatalError()
    }
    public static func BooleanArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.BooleanArray> {
        return { let _ptr = kotlinx_serialization_builtins_BooleanArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.BooleanArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.BooleanArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func ByteArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.ByteArray> {
        return { let _ptr = kotlinx_serialization_builtins_ByteArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.ByteArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.ByteArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func CharArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.CharArray> {
        return { let _ptr = kotlinx_serialization_builtins_CharArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.CharArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.CharArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func DoubleArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.DoubleArray> {
        return { let _ptr = kotlinx_serialization_builtins_DoubleArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.DoubleArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.DoubleArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func FloatArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.FloatArray> {
        return { let _ptr = kotlinx_serialization_builtins_FloatArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.FloatArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.FloatArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func IntArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.IntArray> {
        return { let _ptr = kotlinx_serialization_builtins_IntArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.IntArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.IntArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @inlinable
    public static func ListSerializer<T: KotlinRuntimeSupport._KotlinBridgeable>(
        elementSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Array<T>> {
        return { let _ptr = _cbridge_kotlinx_serialization_builtins_ListSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_T___(elementSerializer.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Array<T>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Array<T>>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func LongArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.LongArray> {
        return { let _ptr = kotlinx_serialization_builtins_LongArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.LongArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.LongArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @inlinable
    public static func MapEntrySerializer<K: KotlinRuntimeSupport._KotlinBridgeable, V: KotlinRuntimeSupport._KotlinBridgeable>(
        keySerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<K>,
        valueSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<V>
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K,V>> {
        return { let _ptr = _cbridge_kotlinx_serialization_builtins_MapEntrySerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_K__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_V___(keySerializer.__externalRCRef(), valueSerializer.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K,V>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K,V>>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @inlinable
    public static func MapSerializer<K: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable, V: KotlinRuntimeSupport._KotlinBridgeable>(
        keySerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<K>,
        valueSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<V>
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Dictionary<K,V>> {
        return { let _ptr = _cbridge_kotlinx_serialization_builtins_MapSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_K__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_V___(keySerializer.__externalRCRef(), valueSerializer.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Dictionary<K,V>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Dictionary<K,V>>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func NothingSerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Never> {
        return { let _ptr = kotlinx_serialization_builtins_NothingSerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Never>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Never>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @inlinable
    public static func PairSerializer<K: KotlinRuntimeSupport._KotlinBridgeable, V: KotlinRuntimeSupport._KotlinBridgeable>(
        keySerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<K>,
        valueSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<V>
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.Pair<K,V>> {
        return { let _ptr = _cbridge_kotlinx_serialization_builtins_PairSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_K__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_V___(keySerializer.__externalRCRef(), valueSerializer.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.Pair<K,V>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.Pair<K,V>>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @inlinable
    public static func SetSerializer<T: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable>(
        elementSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<T>
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Set<T>> {
        return { let _ptr = _cbridge_kotlinx_serialization_builtins_SetSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_T___(elementSerializer.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Set<T>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Set<T>>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func ShortArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.ShortArray> {
        return { let _ptr = kotlinx_serialization_builtins_ShortArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.ShortArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.ShortArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @inlinable
    public static func TripleSerializer<A: KotlinRuntimeSupport._KotlinBridgeable, B: KotlinRuntimeSupport._KotlinBridgeable, C: KotlinRuntimeSupport._KotlinBridgeable>(
        aSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<A>,
        bSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<B>,
        cSerializer: any ExportedKotlinPackages.kotlinx.serialization.KSerializer<C>
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.Triple<A,B,C>> {
        return { let _ptr = _cbridge_kotlinx_serialization_builtins_TripleSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_A__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_B__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_C___(aSerializer.__externalRCRef(), bSerializer.__externalRCRef(), cSerializer.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.Triple<A,B,C>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.Triple<A,B,C>>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @_spi(kotlin$ExperimentalUnsignedTypes) @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func UByteArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.UByteArray> {
        return { let _ptr = kotlinx_serialization_builtins_UByteArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.UByteArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.UByteArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @_spi(kotlin$ExperimentalUnsignedTypes) @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func UIntArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.UIntArray> {
        return { let _ptr = kotlinx_serialization_builtins_UIntArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.UIntArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.UIntArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @_spi(kotlin$ExperimentalUnsignedTypes) @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func ULongArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.ULongArray> {
        return { let _ptr = kotlinx_serialization_builtins_ULongArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.ULongArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.ULongArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @_spi(kotlin$ExperimentalUnsignedTypes) @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func UShortArraySerializer() -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.UShortArray> {
        return { let _ptr = kotlinx_serialization_builtins_UShortArraySerializer(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.UShortArray>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.UShortArray>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Boolean.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Bool> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_Boolean_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Bool>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Bool>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Byte.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Int8> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_Byte_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Int8>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Int8>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Char.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Unicode.UTF16.CodeUnit> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_Char_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Unicode.UTF16.CodeUnit>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Unicode.UTF16.CodeUnit>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Double.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Double> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_Double_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Double>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Double>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Float.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Float> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_Float_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Float>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Float>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Int.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Int32> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_Int_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Int32>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Int32>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Long.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Int64> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_Long_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Int64>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Int64>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Short.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Int16> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_Short_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Int16>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Int16>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.String.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.String> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_String_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.String>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.String>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.UByte.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.UInt8> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_UByte_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.UInt8>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.UInt8>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.UInt.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.UInt32> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_UInt_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.UInt32>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.UInt32>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.ULong.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.UInt64> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_ULong_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.UInt64>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.UInt64>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.UShort.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.UInt16> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_UShort_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.UInt16>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.UInt16>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<KotlinRuntimeSupport.KotlinUnit> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__Swift_Void__({ receiver; return true }()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<KotlinRuntimeSupport.KotlinUnit>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<KotlinRuntimeSupport.KotlinUnit>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.time.Duration.Companion
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.time.Duration> {
        return { let _ptr = kotlinx_serialization_builtins_serializer__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_time_Duration_Companion__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<ExportedKotlinPackages.kotlin.time.Duration>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<ExportedKotlinPackages.kotlin.time.Duration>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.descriptors {
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public enum PolymorphicKind_SealedType: KotlinRuntimeSupport.SealedType {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case `open`(ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.OPEN_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case sealed(ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.SEALED_SealedType)
        public var value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind {
            get {
                switch self {
                case let .`open`(type): type.value
                case let .sealed(type): type.value
                }
            }
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public enum PrimitiveKind_SealedType: KotlinRuntimeSupport.SealedType {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case boolean(ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case byte(ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BYTE_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case char(ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.CHAR_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case double(ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case float(ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.FLOAT_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case int(ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.INT_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case long(ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.LONG_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case short(ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.SHORT_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case string(ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.STRING_SealedType)
        public var value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            get {
                switch self {
                case let .boolean(type): type.value
                case let .byte(type): type.value
                case let .char(type): type.value
                case let .double(type): type.value
                case let .float(type): type.value
                case let .int(type): type.value
                case let .long(type): type.value
                case let .short(type): type.value
                case let .string(type): type.value
                }
            }
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public enum SerialKind_SealedType: KotlinRuntimeSupport.SealedType {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case polymorphicKind(ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case primitiveKind(ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case contextual(ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.CONTEXTUAL_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case `enum`(ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.ENUM_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case structureKind(ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind_SealedType)
        public var value: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
            get {
                switch self {
                case let .polymorphicKind(type): type.value
                case let .primitiveKind(type): type.value
                case let .contextual(type): type.value
                case let .`enum`(type): type.value
                case let .structureKind(type): type.value
                }
            }
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public enum StructureKind_SealedType: KotlinRuntimeSupport.SealedType {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case `class`(ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.CLASS_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case list(ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.LIST_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case map(ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.MAP_SealedType)
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        case object(ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.OBJECT_SealedType)
        public var value: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            get {
                switch self {
                case let .`class`(type): type.value
                case let .list(type): type.value
                case let .map(type): type.value
                case let .object(type): type.value
                }
            }
        }
    }
    public protocol SerialDescriptor: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.descriptors._SerialDescriptor {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        var annotations: [any ExportedKotlinPackages.kotlin.Annotation] {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        var elementsCount: Swift.Int32 {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get
        }
        var isInline: Swift.Bool {
            get
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        var isNullable: Swift.Bool {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        var kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        var serialName: Swift.String {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func getElementAnnotations(
            index: Swift.Int32
        ) -> [any ExportedKotlinPackages.kotlin.Annotation]
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func getElementDescriptor(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func getElementIndex(
            name: Swift.String
        ) -> Swift.Int32
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func getElementName(
            index: Swift.Int32
        ) -> Swift.String
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        func isElementOptional(
            index: Swift.Int32
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor)
    public protocol _SerialDescriptor {
    }
    public protocol __SerialDescriptor: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class ClassSerialDescriptorBuilder: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public var annotations: [any ExportedKotlinPackages.kotlin.Annotation] {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get {
                return kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder_annotations_get(self.__externalRCRef()) as! Swift.Array<any ExportedKotlinPackages.kotlin.Annotation>
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            set {
                return { kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder_annotations_set__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlin_Annotation___(self.__externalRCRef(), newValue); return () }()
            }
        }
        @available(*, unavailable, message: "isNullable inside buildSerialDescriptor is deprecated. Please use SerialDescriptor.nullable extension on a builder result.") @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public var isNullable: Swift.Bool {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            get {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            set {
                fatalError()
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
            return { kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder_element__TypesOfArguments__Swift_String_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Array_anyU20ExportedKotlinPackages_kotlin_Annotation__Swift_Bool__(self.__externalRCRef(), elementName, descriptor.__externalRCRef(), annotations, isOptional); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    open class PolymorphicKind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class OPEN: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.OPEN {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.OPEN.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PolymorphicKind_OPEN_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind_SealedType {
                .`open`(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class SEALED: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.SEALED {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.SEALED.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PolymorphicKind_SEALED_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind_SealedType {
                .sealed(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct OPEN_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.OPEN
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.OPEN
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct SEALED_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.SEALED
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.SEALED
            ) {
                self.value = value
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind_SealedType {
            switch self {
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.OPEN: .`open`(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.SEALED: .sealed(.init(value))
            default: fatalError("missing sealedType for \(self)")
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_disfavoredOverload
        public final override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind_SealedType {
            .polymorphicKind(sealedType())
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    open class PrimitiveKind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class BOOLEAN: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_BOOLEAN_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType {
                .boolean(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class BYTE: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BYTE {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BYTE.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_BYTE_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType {
                .byte(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class CHAR: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.CHAR {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.CHAR.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_CHAR_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType {
                .char(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class DOUBLE: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_DOUBLE_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType {
                .double(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class FLOAT: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.FLOAT {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.FLOAT.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_FLOAT_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType {
                .float(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class INT: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.INT {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.INT.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_INT_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType {
                .int(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class LONG: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.LONG {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.LONG.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_LONG_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType {
                .long(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class SHORT: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.SHORT {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.SHORT.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_SHORT_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType {
                .short(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class STRING: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.STRING {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.STRING.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_STRING_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType {
                .string(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct BOOLEAN_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct BYTE_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BYTE
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BYTE
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct CHAR_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.CHAR
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.CHAR
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct DOUBLE_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct FLOAT_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.FLOAT
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.FLOAT
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct INT_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.INT
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.INT
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct LONG_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.LONG
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.LONG
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct SHORT_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.SHORT
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.SHORT
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct STRING_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.STRING
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.STRING
            ) {
                self.value = value
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind_SealedType {
            switch self {
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN: .boolean(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BYTE: .byte(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.CHAR: .char(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE: .double(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.FLOAT: .float(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.INT: .int(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.LONG: .long(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.SHORT: .short(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.STRING: .string(.init(value))
            default: fatalError("missing sealedType for \(self)")
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_disfavoredOverload
        public final override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind_SealedType {
            .primitiveKind(sealedType())
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    open class SerialKind: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class CONTEXTUAL: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.CONTEXTUAL {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.CONTEXTUAL.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_SerialKind_CONTEXTUAL_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind_SealedType {
                .contextual(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class ENUM: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.ENUM {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.ENUM.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_SerialKind_ENUM_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind_SealedType {
                .`enum`(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct CONTEXTUAL_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.CONTEXTUAL
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.CONTEXTUAL
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct ENUM_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.ENUM
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.ENUM
            ) {
                self.value = value
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind_SealedType {
            switch self {
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind: .polymorphicKind(value.sealedType())
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind: .primitiveKind(value.sealedType())
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.CONTEXTUAL: .contextual(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.ENUM: .`enum`(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind: .structureKind(value.sealedType())
            default: fatalError("missing sealedType for \(self)")
            }
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    open class StructureKind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class CLASS: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.CLASS {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.CLASS.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_StructureKind_CLASS_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind_SealedType {
                .`class`(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class LIST: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.LIST {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.LIST.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_StructureKind_LIST_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind_SealedType {
                .list(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class MAP: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.MAP {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.MAP.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_StructureKind_MAP_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind_SealedType {
                .map(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public final class OBJECT: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.OBJECT {
                @_spi(kotlinx$serialization$ExperimentalSerializationApi)
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.OBJECT.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_StructureKind_OBJECT_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
            @_spi(kotlinx$serialization$ExperimentalSerializationApi)
            public override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind_SealedType {
                .object(.init(self))
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct CLASS_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.CLASS
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.CLASS
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct LIST_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.LIST
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.LIST
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct MAP_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.MAP
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.MAP
            ) {
                self.value = value
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        public struct OBJECT_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.OBJECT
            init(
                _ value: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.OBJECT
            ) {
                self.value = value
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        open func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind_SealedType {
            switch self {
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.CLASS: .`class`(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.LIST: .list(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.MAP: .map(.init(value))
            case let value as ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.OBJECT: .object(.init(value))
            default: fatalError("missing sealedType for \(self)")
            }
        }
        @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_disfavoredOverload
        public final override func sealedType() -> ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind_SealedType {
            .structureKind(sealedType())
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func getCapturedKClass(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Never {
        fatalError()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func getElementDescriptors(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlin.collections.Iterable<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.serialization.descriptors._SerialDescriptor>> {
        return { let _ptr = kotlinx_serialization_descriptors_elementDescriptors_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterable<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.serialization.descriptors._SerialDescriptor>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterable<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.serialization.descriptors._SerialDescriptor>>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func getElementNames(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlin.collections.Iterable<Swift.String> {
        return { let _ptr = kotlinx_serialization_descriptors_elementNames_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterable<Swift.String>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterable<Swift.String>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public static func getNullable(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_nullable_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func primitiveSerialDescriptor(
        serialName: Swift.String,
        kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_PrimitiveSerialDescriptor__TypesOfArguments__Swift_String_ExportedKotlinPackages_kotlinx_serialization_descriptors_PrimitiveKind__(serialName, kind.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func serialDescriptor(
        serialName: Swift.String,
        original: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_SerialDescriptor__TypesOfArguments__Swift_String_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(serialName, original.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func buildClassSerialDescriptor(
        serialName: Swift.String,
        typeParameters: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor...,
        builderAction: @escaping (ExportedKotlinPackages.kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder) -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_buildClassSerialDescriptor__TypesOfArguments__Swift_String_Swift_Array_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__Vararg__U28ExportedKotlinPackages_kotlinx_serialization_descriptors_ClassSerialDescriptorBuilderU29202D_U20Swift_Void__(serialName, typeParameters, Unmanaged.passRetained((builderAction as (ExportedKotlinPackages.kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder) -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi) @_spi(kotlinx$serialization$InternalSerializationApi)
    public static func buildSerialDescriptor(
        serialName: Swift.String,
        kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind,
        typeParameters: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor...,
        builder: @escaping (ExportedKotlinPackages.kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder) -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_buildSerialDescriptor__TypesOfArguments__Swift_String_ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialKind_Swift_Array_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__Vararg__U28ExportedKotlinPackages_kotlinx_serialization_descriptors_ClassSerialDescriptorBuilderU29202D_U20Swift_Void__(serialName, kind.__externalRCRef(), typeParameters, Unmanaged.passRetained((builder as (ExportedKotlinPackages.kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder) -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func listSerialDescriptor(
        elementDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_listSerialDescriptor__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(elementDescriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func mapSerialDescriptor(
        keyDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        valueDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_mapSerialDescriptor__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(keyDescriptor.__externalRCRef(), valueDescriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func serialDescriptor(
        type: Swift.Never
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        fatalError()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func setSerialDescriptor(
        elementDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_setSerialDescriptor__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(elementDescriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func getContextualDescriptor(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> (any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)? {
        return { switch kotlinx_serialization_descriptors_getContextualDescriptor__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef(), descriptor.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor; } }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public static func getPolymorphicDescriptors(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> [any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor] {
        return kotlinx_serialization_descriptors_getPolymorphicDescriptors__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef(), descriptor.__externalRCRef()) as! Swift.Array<any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor>
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
    @_spi(kotlinx$serialization$ExperimentalSerializationApi) @available(*, unavailable, message: "Declaration uses unsupported types")
    public func serializer(
        kClass: Swift.Never,
        typeArgumentsSerializers: [ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>],
        isNullable: Swift.Bool
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
        fatalError()
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public func serializer(
        type: Swift.Never
    ) -> any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
        fatalError()
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public func serializerOrNull(
        type: Swift.Never
    ) -> (any ExportedKotlinPackages.kotlinx.serialization.KSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>)? {
        fatalError()
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.BinaryFormat {
    public func decodeFromHexString<T: KotlinRuntimeSupport._KotlinBridgeable>(
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
        hex: Swift.String
    ) -> T {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.serialization.decodeFromHexString(receiver, deserializer: deserializer, hex: hex)
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.BinaryFormat {
    public func encodeToHexString<T: KotlinRuntimeSupport._KotlinBridgeable>(
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T
    ) -> Swift.String {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.serialization.encodeToHexString(receiver, serializer: serializer, value: value)
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractPolymorphicSerializer {
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public func findPolymorphicSerializer(
        decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder,
        klassName: Swift.String?
    ) -> any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T> {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.serialization.findPolymorphicSerializer(receiver, decoder: decoder, klassName: klassName)
    }
}
extension ExportedKotlinPackages.kotlinx.serialization.`internal`.AbstractPolymorphicSerializer {
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public func findPolymorphicSerializer(
        encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
        value: T
    ) -> any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T> {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.serialization.findPolymorphicSerializer(receiver, encoder: encoder, value: value)
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.BinaryFormat where Self : ExportedKotlinPackages.kotlinx.serialization.__BinaryFormat {
    public func decodeFromByteArray<T: KotlinRuntimeSupport._KotlinBridgeable>(
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
        bytes: ExportedKotlinPackages.kotlin.ByteArray
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_serialization_BinaryFormat_decodeFromByteArray__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__ExportedKotlinPackages_kotlin_ByteArray__(self.__externalRCRef(), deserializer.__externalRCRef(), bytes.__externalRCRef()))
    }
    public func encodeToByteArray<T: KotlinRuntimeSupport._KotlinBridgeable>(
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T
    ) -> ExportedKotlinPackages.kotlin.ByteArray {
        return ExportedKotlinPackages.kotlin.ByteArray.__createClassWrapper(externalRCRef: kotlinx_serialization_BinaryFormat_encodeToByteArray__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T__(self.__externalRCRef(), serializer.__externalRCRef(), value.__retainedToKotlinPointer()))
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization._BinaryFormat {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.BinaryFormat, ExportedKotlinPackages.kotlinx.serialization.__BinaryFormat where Wrapped : ExportedKotlinPackages.kotlinx.serialization._BinaryFormat {
}
extension ExportedKotlinPackages.kotlinx.serialization.BinaryFormat {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy where Self : ExportedKotlinPackages.kotlinx.serialization.__DeserializationStrategy {
    public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_DeserializationStrategy_descriptor_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        }
    }
    public func deserialize(
        decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_serialization_DeserializationStrategy_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder__(self.__externalRCRef(), decoder.__externalRCRef()))
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization._DeserializationStrategy {
}
extension ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.KSerializer where Self : ExportedKotlinPackages.kotlinx.serialization.__KSerializer {
    public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_KSerializer_descriptor_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization._KSerializer {
}
extension ExportedKotlinPackages.kotlinx.serialization.KSerializer {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.SerialFormat where Self : ExportedKotlinPackages.kotlinx.serialization.__SerialFormat {
    public var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_SerialFormat_serializersModule_get(self.__externalRCRef()))
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization._SerialFormat {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.SerialFormat, ExportedKotlinPackages.kotlinx.serialization.__SerialFormat where Wrapped : ExportedKotlinPackages.kotlinx.serialization._SerialFormat {
}
extension ExportedKotlinPackages.kotlinx.serialization.SerialFormat {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy where Self : ExportedKotlinPackages.kotlinx.serialization.__SerializationStrategy {
    public var descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_SerializationStrategy_descriptor_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        }
    }
    public func serialize(
        encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
        value: T
    ) -> Swift.Void {
        return { kotlinx_serialization_SerializationStrategy_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T__(self.__externalRCRef(), encoder.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization._SerializationStrategy {
}
extension ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.StringFormat where Self : ExportedKotlinPackages.kotlinx.serialization.__StringFormat {
    public func decodeFromString<T: KotlinRuntimeSupport._KotlinBridgeable>(
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
        string: Swift.String
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_serialization_StringFormat_decodeFromString__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_String__(self.__externalRCRef(), deserializer.__externalRCRef(), string))
    }
    public func encodeToString<T: KotlinRuntimeSupport._KotlinBridgeable>(
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T
    ) -> Swift.String {
        return kotlinx_serialization_StringFormat_encodeToString__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T__(self.__externalRCRef(), serializer.__externalRCRef(), value.__retainedToKotlinPointer())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization._StringFormat {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.StringFormat, ExportedKotlinPackages.kotlinx.serialization.__StringFormat where Wrapped : ExportedKotlinPackages.kotlinx.serialization._StringFormat {
}
extension ExportedKotlinPackages.kotlinx.serialization.StringFormat {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor where Self : ExportedKotlinPackages.kotlinx.serialization.descriptors.__SerialDescriptor {
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public var annotations: [any ExportedKotlinPackages.kotlin.Annotation] {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_annotations_get(self.__externalRCRef()) as! Swift.Array<any ExportedKotlinPackages.kotlin.Annotation>
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public var elementsCount: Swift.Int32 {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_elementsCount_get(self.__externalRCRef())
        }
    }
    public var isInline: Swift.Bool {
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_isInline_get(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public var isNullable: Swift.Bool {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_isNullable_get(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public var kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        get {
            return ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.__createClassWrapper(externalRCRef: kotlinx_serialization_descriptors_SerialDescriptor_kind_get(self.__externalRCRef()))
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public var serialName: Swift.String {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_serialName_get(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func getElementAnnotations(
        index: Swift.Int32
    ) -> [any ExportedKotlinPackages.kotlin.Annotation] {
        return kotlinx_serialization_descriptors_SerialDescriptor_getElementAnnotations__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) as! Swift.Array<any ExportedKotlinPackages.kotlin.Annotation>
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func getElementDescriptor(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_descriptors_SerialDescriptor_getElementDescriptor__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func getElementIndex(
        name: Swift.String
    ) -> Swift.Int32 {
        return kotlinx_serialization_descriptors_SerialDescriptor_getElementIndex__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func getElementName(
        index: Swift.Int32
    ) -> Swift.String {
        return kotlinx_serialization_descriptors_SerialDescriptor_getElementName__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func isElementOptional(
        index: Swift.Int32
    ) -> Swift.Bool {
        return kotlinx_serialization_descriptors_SerialDescriptor_isElementOptional__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization.descriptors._SerialDescriptor {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, ExportedKotlinPackages.kotlinx.serialization.descriptors.__SerialDescriptor where Wrapped : ExportedKotlinPackages.kotlinx.serialization.descriptors._SerialDescriptor {
}
extension ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public var annotations: [any ExportedKotlinPackages.kotlin.Annotation] {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_annotations_get_direct(self.__externalRCRef()) as! Swift.Array<any ExportedKotlinPackages.kotlin.Annotation>
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public var elementsCount: Swift.Int32 {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        get {
            fatalError("'elementsCount' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    public var isInline: Swift.Bool {
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_isInline_get_direct(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public var isNullable: Swift.Bool {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        get {
            return kotlinx_serialization_descriptors_SerialDescriptor_isNullable_get_direct(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public var kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        get {
            fatalError("'kind' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public var serialName: Swift.String {
        @_spi(kotlinx$serialization$ExperimentalSerializationApi)
        get {
            fatalError("'serialName' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func getElementAnnotations(
        index: Swift.Int32
    ) -> [any ExportedKotlinPackages.kotlin.Annotation] {
        fatalError("'getElementAnnotations' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func getElementDescriptor(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        fatalError("'getElementDescriptor' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func getElementIndex(
        name: Swift.String
    ) -> Swift.Int32 {
        fatalError("'getElementIndex' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func getElementName(
        index: Swift.Int32
    ) -> Swift.String {
        fatalError("'getElementName' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func isElementOptional(
        index: Swift.Int32
    ) -> Swift.Bool {
        fatalError("'isElementOptional' is an @_spi requirement that must be implemented by Swift conformers")
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.encoding.ChunkedDecoder where Self : ExportedKotlinPackages.kotlinx.serialization.encoding.__ChunkedDecoder {
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeStringChunked(
        consumeChunk: @escaping (Swift.String) -> Swift.Void
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_ChunkedDecoder_decodeStringChunked__TypesOfArguments__U28Swift_StringU29202D_U20Swift_Void__(self.__externalRCRef(), Unmanaged.passRetained((consumeChunk as (Swift.String) -> Swift.Void) as AnyObject).toOpaque()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization.encoding._ChunkedDecoder {
}
@_spi(kotlinx$serialization$ExperimentalSerializationApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.encoding.ChunkedDecoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__ChunkedDecoder where Wrapped : ExportedKotlinPackages.kotlinx.serialization.encoding._ChunkedDecoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.ChunkedDecoder {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder where Self : ExportedKotlinPackages.kotlinx.serialization.encoding.__CompositeDecoder {
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
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_CompositeDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
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
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<T>>,
        previousValue: T?
    ) -> T? {
        return Swift.Optional<T>.__fromKotlinPointer(kotlinx_serialization_encoding_CompositeDecoder_decodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Swift_Optional_T___Swift_Optional_T___(self.__externalRCRef(), descriptor.__externalRCRef(), index, deserializer.__externalRCRef(), previousValue.__retainedToKotlinPointer()))
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeSequentially() -> Swift.Bool {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeSequentially(self.__externalRCRef())
    }
    public func decodeSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>,
        previousValue: T?
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_serialization_encoding_CompositeDecoder_decodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T___(self.__externalRCRef(), descriptor.__externalRCRef(), index, deserializer.__externalRCRef(), previousValue.__retainedToKotlinPointer()))
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
        return { kotlinx_serialization_encoding_CompositeDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeDecoder {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__CompositeDecoder where Wrapped : ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeDecoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder {
    public typealias Companion = KotlinSerialization._ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Companion
    public func decodeCollectionSize(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Int32 {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeCollectionSize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor___direct(self.__externalRCRef(), descriptor.__externalRCRef())
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<T>>,
        previousValue: T?
    ) -> T? {
        fatalError("'decodeNullableSerializableElement' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeSequentially() -> Swift.Bool {
        return kotlinx_serialization_encoding_CompositeDecoder_decodeSequentially_direct(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder where Self : ExportedKotlinPackages.kotlinx.serialization.encoding.__CompositeEncoder {
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
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Bool__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
    }
    public func encodeByteElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Int8
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int8__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
    }
    public func encodeCharElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Unicode.UTF16.CodeUnit
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
    }
    public func encodeDoubleElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Double
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Double__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
    }
    public func encodeFloatElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Float
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Float__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
    }
    public func encodeInlineElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_CompositeEncoder_encodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    }
    public func encodeIntElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Int32
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
    }
    public func encodeLongElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Int64
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int64__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func encodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T?
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T___(self.__externalRCRef(), descriptor.__externalRCRef(), index, serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
    }
    public func encodeSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T__(self.__externalRCRef(), descriptor.__externalRCRef(), index, serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
    }
    public func encodeShortElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.Int16
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int16__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
    }
    public func encodeStringElement(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        value: Swift.String
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_encodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_String__(self.__externalRCRef(), descriptor.__externalRCRef(), index, value); return () }()
    }
    public func endStructure(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_CompositeEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()); return () }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func shouldEncodeElementDefault(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Bool {
        return kotlinx_serialization_encoding_CompositeEncoder_shouldEncodeElementDefault__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeEncoder {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__CompositeEncoder where Wrapped : ExportedKotlinPackages.kotlinx.serialization.encoding._CompositeEncoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func encodeNullableSerializableElement<T: KotlinRuntimeSupport._KotlinBridgeable>(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32,
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T?
    ) -> Swift.Void {
        fatalError("'encodeNullableSerializableElement' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func shouldEncodeElementDefault(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Bool {
        return kotlinx_serialization_encoding_CompositeEncoder_shouldEncodeElementDefault__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32___direct(self.__externalRCRef(), descriptor.__externalRCRef(), index)
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder where Self : ExportedKotlinPackages.kotlinx.serialization.encoding.__Decoder {
    public var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_encoding_Decoder_serializersModule_get(self.__externalRCRef()))
        }
    }
    public func beginStructure(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Decoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
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
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Decoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    }
    public func decodeInt() -> Swift.Int32 {
        return kotlinx_serialization_encoding_Decoder_decodeInt(self.__externalRCRef())
    }
    public func decodeLong() -> Swift.Int64 {
        return kotlinx_serialization_encoding_Decoder_decodeLong(self.__externalRCRef())
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeNotNullMark() -> Swift.Bool {
        return kotlinx_serialization_encoding_Decoder_decodeNotNullMark(self.__externalRCRef())
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeNull() -> Swift.Never? {
        return { kotlinx_serialization_encoding_Decoder_decodeNull(self.__externalRCRef()); return nil }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeNullableSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<T>>
    ) -> T? {
        return Swift.Optional<T>.__fromKotlinPointer(kotlinx_serialization_encoding_Decoder_decodeNullableSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Swift_Optional_T____(self.__externalRCRef(), deserializer.__externalRCRef()))
    }
    public func decodeSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_serialization_encoding_Decoder_decodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T___(self.__externalRCRef(), deserializer.__externalRCRef()))
    }
    public func decodeShort() -> Swift.Int16 {
        return kotlinx_serialization_encoding_Decoder_decodeShort(self.__externalRCRef())
    }
    public func decodeString() -> Swift.String {
        return kotlinx_serialization_encoding_Decoder_decodeString(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization.encoding._Decoder {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__Decoder where Wrapped : ExportedKotlinPackages.kotlinx.serialization.encoding._Decoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeNotNullMark() -> Swift.Bool {
        fatalError("'decodeNotNullMark' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeNull() -> Swift.Never? {
        fatalError("'decodeNull' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func decodeNullableSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<T>>
    ) -> T? {
        return Swift.Optional<T>.__fromKotlinPointer(kotlinx_serialization_encoding_Decoder_decodeNullableSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Swift_Optional_T_____direct(self.__externalRCRef(), deserializer.__externalRCRef()))
    }
    public func decodeSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
        deserializer: any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<T>
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_serialization_encoding_Decoder_decodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T____direct(self.__externalRCRef(), deserializer.__externalRCRef()))
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder where Self : ExportedKotlinPackages.kotlinx.serialization.encoding.__Encoder {
    public var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: kotlinx_serialization_encoding_Encoder_serializersModule_get(self.__externalRCRef()))
        }
    }
    public func beginCollection(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        collectionSize: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Encoder_beginCollection__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), collectionSize), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    }
    public func beginStructure(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Encoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    }
    public func encodeBoolean(
        value: Swift.Bool
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeBoolean__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), value); return () }()
    }
    public func encodeByte(
        value: Swift.Int8
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeByte__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), value); return () }()
    }
    public func encodeChar(
        value: Swift.Unicode.UTF16.CodeUnit
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), value); return () }()
    }
    public func encodeDouble(
        value: Swift.Double
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeDouble__TypesOfArguments__Swift_Double__(self.__externalRCRef(), value); return () }()
    }
    public func encodeEnum(
        enumDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        index: Swift.Int32
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), enumDescriptor.__externalRCRef(), index); return () }()
    }
    public func encodeFloat(
        value: Swift.Float
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeFloat__TypesOfArguments__Swift_Float__(self.__externalRCRef(), value); return () }()
    }
    public func encodeInline(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Encoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    }
    public func encodeInt(
        value: Swift.Int32
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeInt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), value); return () }()
    }
    public func encodeLong(
        value: Swift.Int64
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeLong__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), value); return () }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func encodeNotNullMark() -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeNotNullMark(self.__externalRCRef()); return () }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func encodeNull() -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeNull(self.__externalRCRef()); return () }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func encodeNullableSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T?
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeNullableSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T___(self.__externalRCRef(), serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
    }
    public func encodeSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T__(self.__externalRCRef(), serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
    }
    public func encodeShort(
        value: Swift.Int16
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeShort__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), value); return () }()
    }
    public func encodeString(
        value: Swift.String
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeString__TypesOfArguments__Swift_String__(self.__externalRCRef(), value); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization.encoding._Encoder {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder, ExportedKotlinPackages.kotlinx.serialization.encoding.__Encoder where Wrapped : ExportedKotlinPackages.kotlinx.serialization.encoding._Encoder {
}
extension ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
    public func beginCollection(
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        collectionSize: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_serialization_encoding_Encoder_beginCollection__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32___direct(self.__externalRCRef(), descriptor.__externalRCRef(), collectionSize), conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func encodeNotNullMark() -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeNotNullMark_direct(self.__externalRCRef()); return () }()
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func encodeNull() -> Swift.Void {
        fatalError("'encodeNull' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$serialization$ExperimentalSerializationApi)
    public func encodeNullableSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T?
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeNullableSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T____direct(self.__externalRCRef(), serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
    }
    public func encodeSerializableValue<T: KotlinRuntimeSupport._KotlinBridgeable>(
        serializer: any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<T>,
        value: T
    ) -> Swift.Void {
        return { kotlinx_serialization_encoding_Encoder_encodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T___direct(self.__externalRCRef(), serializer.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.`internal`.GeneratedSerializer where Self : ExportedKotlinPackages.kotlinx.serialization.`internal`.__GeneratedSerializer {
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public func childSerializers() -> ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>> {
        return ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>>.__fromKotlinPointer(kotlinx_serialization_internal_GeneratedSerializer_childSerializers(self.__externalRCRef()))
    }
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public func typeParametersSerializers() -> ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>> {
        return ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>>.__fromKotlinPointer(kotlinx_serialization_internal_GeneratedSerializer_typeParametersSerializers(self.__externalRCRef()))
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization.`internal`._GeneratedSerializer {
}
extension ExportedKotlinPackages.kotlinx.serialization.`internal`.GeneratedSerializer {
    @_spi(kotlinx$serialization$InternalSerializationApi)
    public func typeParametersSerializers() -> ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>> {
        return ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>>.__fromKotlinPointer(kotlinx_serialization_internal_GeneratedSerializer_typeParametersSerializers_direct(self.__externalRCRef()))
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector where Self : ExportedKotlinPackages.kotlinx.serialization.modules.__SerializersModuleCollector {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.serialization.modules._SerializersModuleCollector {
}
@_spi(kotlinx$serialization$ExperimentalSerializationApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector, ExportedKotlinPackages.kotlinx.serialization.modules.__SerializersModuleCollector where Wrapped : ExportedKotlinPackages.kotlinx.serialization.modules._SerializersModuleCollector {
}
extension ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector {
}
@_cdecl("KotlinSerialization_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___")
package func KotlinSerialization_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.String?) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.String>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1)
    return _result
}

@_cdecl("KotlinSerialization_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder__")
package func KotlinSerialization_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _1, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder)
    return _result
}

@_cdecl("KotlinSerialization_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder__")
package func KotlinSerialization_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlinx_serialization_descriptors_ClassSerialDescriptorBuilder__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (ExportedKotlinPackages.kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder) -> Swift.Void)(ExportedKotlinPackages.kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder.__createClassWrapper(externalRCRef: _1))
    return { _result; return true }()
}

@_cdecl("KotlinSerialization_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModuleBuilder__")
package func KotlinSerialization_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModuleBuilder__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleBuilder) -> Swift.Void)(ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleBuilder.__createClassWrapper(externalRCRef: _1))
    return { _result; return true }()
}

@_cdecl("KotlinSerialization_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
package func KotlinSerialization_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.String) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.String) -> Swift.Void)(_1)
    return { _result; return true }()
}

@_cdecl("KotlinSerialization_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeEncoder_Swift_Int32_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinSerialization_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeEncoder_Swift_Int32_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer, _ _2: Swift.Int32, _ _3: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder, Swift.Int32, Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void)(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _1, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder, _2, _3)
    return { _result; return true }()
}

@_cdecl("KotlinSerialization_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeEncoder__")
package func KotlinSerialization_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeEncoder__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder) -> Swift.Void)(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _1, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_BinaryFormat_decodeFromByteArray__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__ExportedKotlinPackages_kotlin_ByteArray____reverse_swift")
package func kotlinx_serialization_BinaryFormat_decodeFromByteArray__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__ExportedKotlinPackages_kotlin_ByteArray____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ deserializer: Swift.UnsafeMutableRawPointer, _ bytes: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.BinaryFormat.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.BinaryFormat
    let _result: Swift.Optional<KotlinRuntimeSupport.KotlinAny> = _self.decodeFromByteArray(deserializer: { let _ptr = deserializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), bytes: ExportedKotlinPackages.kotlin.ByteArray.__createClassWrapper(externalRCRef: bytes))
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlinx_serialization_BinaryFormat_encodeToByteArray__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift")
package func kotlinx_serialization_BinaryFormat_encodeToByteArray__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ serializer: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.BinaryFormat.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.BinaryFormat
    let _result: ExportedKotlinPackages.kotlin.ByteArray = _self.encodeToByteArray(serializer: { let _ptr = serializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), value: Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(value))
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_DeserializationStrategy_descriptor_get__reverse_swift")
package func kotlinx_serialization_DeserializationStrategy_descriptor_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor = _self.descriptor
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_DeserializationStrategy_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder____reverse_swift")
package func kotlinx_serialization_DeserializationStrategy_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ decoder: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.deserialize(decoder: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: decoder, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder)
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_KSerializer_descriptor_get__reverse_swift")
package func kotlinx_serialization_KSerializer_descriptor_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.KSerializer)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.KSerializer>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor = _self.descriptor
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_SerialFormat_serializersModule_get__reverse_swift")
package func kotlinx_serialization_SerialFormat_serializersModule_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.SerialFormat.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.SerialFormat
    let _result: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule = _self.serializersModule
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_SerializationStrategy_descriptor_get__reverse_swift")
package func kotlinx_serialization_SerializationStrategy_descriptor_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor = _self.descriptor
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_SerializationStrategy_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T____reverse_swift")
package func kotlinx_serialization_SerializationStrategy_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ encoder: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.serialize(encoder: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: encoder, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder, value: __Self.T.__fromKotlinPointer(value))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_StringFormat_decodeFromString__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_String____reverse_swift")
package func kotlinx_serialization_StringFormat_decodeFromString__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ deserializer: Swift.UnsafeMutableRawPointer, _ string: Swift.String) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.StringFormat.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.StringFormat
    let _result: Swift.Optional<KotlinRuntimeSupport.KotlinAny> = _self.decodeFromString(deserializer: { let _ptr = deserializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), string: string)
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlinx_serialization_StringFormat_encodeToString__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift")
package func kotlinx_serialization_StringFormat_encodeToString__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ serializer: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.StringFormat.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.StringFormat
    let _result: Swift.String = _self.encodeToString(serializer: { let _ptr = serializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), value: Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(value))
    return _result
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_annotations_get__reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_annotations_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Any {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: Swift.Array<any ExportedKotlinPackages.kotlin.Annotation> = _self.annotations
    return _result
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_elementsCount_get__reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_elementsCount_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: Swift.Int32 = _self.elementsCount
    return _result
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_getElementAnnotations__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_getElementAnnotations__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Any {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: Swift.Array<any ExportedKotlinPackages.kotlin.Annotation> = _self.getElementAnnotations(index: index)
    return _result
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_getElementDescriptor__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_getElementDescriptor__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor = _self.getElementDescriptor(index: index)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_getElementIndex__TypesOfArguments__Swift_String____reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_getElementIndex__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: Swift.Int32 = _self.getElementIndex(name: name)
    return _result
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_getElementName__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_getElementName__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: Swift.String = _self.getElementName(index: index)
    return _result
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_isElementOptional__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_isElementOptional__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: Swift.Bool = _self.isElementOptional(index: index)
    return _result
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_isInline_get__reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_isInline_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: Swift.Bool = _self.isInline
    return _result
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_isNullable_get__reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_isNullable_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: Swift.Bool = _self.isNullable
    return _result
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_kind_get__reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_kind_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind = _self.kind
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_descriptors_SerialDescriptor_serialName_get__reverse_swift")
package func kotlinx_serialization_descriptors_SerialDescriptor_serialName_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    let _result: Swift.String = _self.serialName
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder = _self.beginStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeBoolean__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeBoolean__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.decodeBoolean()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeByte__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeByte__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int8 {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int8 = _self.decodeByte()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeChar__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeChar__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UInt16 {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Unicode.UTF16.CodeUnit = _self.decodeChar()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeDouble__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeDouble__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Double {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Double = _self.decodeDouble()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ enumDescriptor: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int32 = _self.decodeEnum(enumDescriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: enumDescriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeFloat__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeFloat__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Float {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Float = _self.decodeFloat()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder = _self.decodeInlineElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder = _self.decodeInline(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeInt__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeInt__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int32 = _self.decodeInt()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeLong__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeLong__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int64 {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int64 = _self.decodeLong()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeNotNullMark__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeNotNullMark__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.decodeNotNullMark()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeNull__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeNull__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Optional<Swift.Never> = _self.decodeNull()
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T_____reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ deserializer: Swift.UnsafeMutableRawPointer, _ previousValue: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Optional<KotlinRuntimeSupport.KotlinAny> = _self.decodeSerializableElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, deserializer: { let _ptr = deserializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), previousValue: Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(previousValue))
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T_____reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ deserializer: Swift.UnsafeMutableRawPointer, _ previousValue: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Optional<KotlinRuntimeSupport.KotlinAny> = _self.decodeSerializableValue(deserializer: { let _ptr = deserializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), previousValue: Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(previousValue))
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeShort__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeShort__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int16 {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int16 = _self.decodeShort()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeString__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeString__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.String = _self.decodeString()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_decodeValue__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_decodeValue__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: any KotlinRuntimeSupport._KotlinBridgeable = _self.decodeValue()
    return (_result).__retainedToKotlinPointer()!
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.endStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractDecoder_serializersModule_get__reverse_swift")
package func kotlinx_serialization_encoding_AbstractDecoder_serializersModule_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractDecoder.__createClassWrapper(externalRCRef: `self`)
    let _result: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule = _self.serializersModule
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder = _self.beginStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeBoolean__TypesOfArguments__Swift_Bool____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeBoolean__TypesOfArguments__Swift_Bool____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Bool) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeBoolean(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeByte__TypesOfArguments__Swift_Int8____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeByte__TypesOfArguments__Swift_Int8____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Int8) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeByte(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UInt16) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeChar(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeDouble__TypesOfArguments__Swift_Double____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeDouble__TypesOfArguments__Swift_Double____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Double) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeDouble(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.encodeElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ enumDescriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeEnum(enumDescriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: enumDescriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeFloat__TypesOfArguments__Swift_Float____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeFloat__TypesOfArguments__Swift_Float____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Float) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeFloat(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder = _self.encodeInline(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeInt__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeInt__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Int32) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeInt(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeLong__TypesOfArguments__Swift_Int64____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeLong__TypesOfArguments__Swift_Int64____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Int64) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeLong(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeNull__reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeNull__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeNull()
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T_____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ serializer: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeNullableSerializableElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, serializer: { let _ptr = serializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), value: Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(value))
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ serializer: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeSerializableElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, serializer: { let _ptr = serializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), value: Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(value))
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeShort__TypesOfArguments__Swift_Int16____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeShort__TypesOfArguments__Swift_Int16____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Int16) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeShort(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeString__TypesOfArguments__Swift_String____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeString__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.String) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeString(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_encodeValue__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_encodeValue__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.encodeValue(value: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: value))
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.endStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_AbstractEncoder_serializersModule_get__reverse_swift")
package func kotlinx_serialization_encoding_AbstractEncoder_serializersModule_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.serialization.encoding.AbstractEncoder.__createClassWrapper(externalRCRef: `self`)
    let _result: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule = _self.serializersModule
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_ChunkedDecoder_decodeStringChunked__TypesOfArguments__U28Swift_StringU29202D_U20Swift_Void____reverse_swift")
package func kotlinx_serialization_encoding_ChunkedDecoder_decodeStringChunked__TypesOfArguments__U28Swift_StringU29202D_U20Swift_Void____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ consumeChunk: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.ChunkedDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.ChunkedDecoder
    let _result: Swift.Void = _self.decodeStringChunked(consumeChunk: {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: consumeChunk, options: .asBestFittingWrapper)
    return { _1 in return { KotlinSerialization_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(pointerToBlock.__externalRCRef(), _1); return () }() }
}())
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Bool = _self.decodeBooleanElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Int8 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Int8 = _self.decodeByteElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UInt16 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Unicode.UTF16.CodeUnit = _self.decodeCharElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeCollectionSize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeCollectionSize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Int32 = _self.decodeCollectionSize(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Double {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Double = _self.decodeDoubleElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeElementIndex__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeElementIndex__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Int32 = _self.decodeElementIndex(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Float {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Float = _self.decodeFloatElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder = _self.decodeInlineElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Int32 = _self.decodeIntElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Int64 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Int64 = _self.decodeLongElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Swift_Optional_T___Swift_Optional_T_____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Swift_Optional_T___Swift_Optional_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ deserializer: Swift.UnsafeMutableRawPointer, _ previousValue: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> = _self.decodeNullableSerializableElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, deserializer: { let _ptr = deserializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>>.__createTypedWrapper(externalRCRef: _ptr) }(), previousValue: Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(previousValue))
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeSequentially__reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeSequentially__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Bool = _self.decodeSequentially()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T_____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T__Swift_Optional_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ deserializer: Swift.UnsafeMutableRawPointer, _ previousValue: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Optional<KotlinRuntimeSupport.KotlinAny> = _self.decodeSerializableElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, deserializer: { let _ptr = deserializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), previousValue: Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(previousValue))
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Int16 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Int16 = _self.decodeShortElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_decodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_decodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.String = _self.decodeStringElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: Swift.Void = _self.endStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeDecoder_serializersModule_get__reverse_swift")
package func kotlinx_serialization_encoding_CompositeDecoder_serializersModule_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
    let _result: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule = _self.serializersModule
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Bool____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeBooleanElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Bool____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ value: Swift.Bool) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeBooleanElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int8____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeByteElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int8____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ value: Swift.Int8) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeByteElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Unicode_UTF16_CodeUnit____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeCharElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Unicode_UTF16_CodeUnit____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ value: Swift.UInt16) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeCharElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Double____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeDoubleElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Double____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ value: Swift.Double) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeDoubleElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Float____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeFloatElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Float____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ value: Swift.Float) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeFloatElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder = _self.encodeInlineElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeIntElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ value: Swift.Int32) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeIntElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int64____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeLongElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int64____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ value: Swift.Int64) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeLongElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T_____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ serializer: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeNullableSerializableElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, serializer: { let _ptr = serializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), value: Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(value))
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ serializer: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeSerializableElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, serializer: { let _ptr = serializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), value: Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(value))
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int16____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeShortElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_Int16____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ value: Swift.Int16) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeShortElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_encodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_String____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_encodeStringElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ value: Swift.String) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.encodeStringElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Void = _self.endStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_serializersModule_get__reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_serializersModule_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule = _self.serializersModule
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_CompositeEncoder_shouldEncodeElementDefault__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_CompositeEncoder_shouldEncodeElementDefault__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
    let _result: Swift.Bool = _self.shouldEncodeElementDefault(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_Decoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder = _self.beginStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeBoolean__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeBoolean__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Bool = _self.decodeBoolean()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeByte__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeByte__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int8 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Int8 = _self.decodeByte()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeChar__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeChar__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UInt16 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Unicode.UTF16.CodeUnit = _self.decodeChar()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeDouble__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeDouble__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Double {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Double = _self.decodeDouble()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ enumDescriptor: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Int32 = _self.decodeEnum(enumDescriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: enumDescriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeFloat__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeFloat__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Float {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Float = _self.decodeFloat()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder = _self.decodeInline(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeInt__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeInt__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Int32 = _self.decodeInt()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeLong__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeLong__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int64 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Int64 = _self.decodeLong()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeNotNullMark__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeNotNullMark__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Bool = _self.decodeNotNullMark()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeNull__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeNull__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Optional<Swift.Never> = _self.decodeNull()
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeNullableSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Swift_Optional_T______reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeNullableSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_Swift_Optional_T______reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ deserializer: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> = _self.decodeNullableSerializableValue(deserializer: { let _ptr = deserializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>>.__createTypedWrapper(externalRCRef: _ptr) }())
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T_____reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_DeserializationStrategy_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ deserializer: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Optional<KotlinRuntimeSupport.KotlinAny> = _self.decodeSerializableValue(deserializer: { let _ptr = deserializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnyDeserializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }())
    return _result.__retainedToKotlinPointer()
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeShort__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeShort__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int16 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.Int16 = _self.decodeShort()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_decodeString__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_decodeString__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: Swift.String = _self.decodeString()
    return _result
}

@_cdecl("kotlinx_serialization_encoding_Decoder_serializersModule_get__reverse_swift")
package func kotlinx_serialization_encoding_Decoder_serializersModule_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
    let _result: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule = _self.serializersModule
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_beginCollection__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_beginCollection__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ collectionSize: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder = _self.beginCollection(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, collectionSize: collectionSize)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder = _self.beginStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeBoolean__TypesOfArguments__Swift_Bool____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeBoolean__TypesOfArguments__Swift_Bool____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Bool) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeBoolean(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeByte__TypesOfArguments__Swift_Int8____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeByte__TypesOfArguments__Swift_Int8____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Int8) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeByte(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeChar__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UInt16) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeChar(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeDouble__TypesOfArguments__Swift_Double____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeDouble__TypesOfArguments__Swift_Double____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Double) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeDouble(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeEnum__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ enumDescriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeEnum(enumDescriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: enumDescriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeFloat__TypesOfArguments__Swift_Float____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeFloat__TypesOfArguments__Swift_Float____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Float) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeFloat(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder = _self.encodeInline(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeInt__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeInt__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Int32) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeInt(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeLong__TypesOfArguments__Swift_Int64____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeLong__TypesOfArguments__Swift_Int64____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Int64) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeLong(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeNotNullMark__reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeNotNullMark__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeNotNullMark()
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeNull__reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeNull__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeNull()
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeNullableSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T_____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeNullableSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ serializer: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeNullableSerializableValue(serializer: { let _ptr = serializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), value: Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(value))
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeSerializableValue__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ serializer: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeSerializableValue(serializer: { let _ptr = serializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), value: Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(value))
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeShort__TypesOfArguments__Swift_Int16____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeShort__TypesOfArguments__Swift_Int16____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.Int16) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeShort(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_encodeString__TypesOfArguments__Swift_String____reverse_swift")
package func kotlinx_serialization_encoding_Encoder_encodeString__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.String) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: Swift.Void = _self.encodeString(value: value)
    return { _result; return true }()
}

@_cdecl("kotlinx_serialization_encoding_Encoder_serializersModule_get__reverse_swift")
package func kotlinx_serialization_encoding_Encoder_serializersModule_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
    let _result: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule = _self.serializersModule
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_serialization_internal_AbstractCollectionSerializer_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder____reverse_swift")
package func kotlinx_serialization_internal_AbstractCollectionSerializer_deserialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Decoder____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ decoder: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractCollectionSerializer_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractCollectionSerializer_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.Collection = _self.deserialize(decoder: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: decoder, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder)
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_AbstractCollectionSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Collection____reverse_swift")
package func kotlinx_serialization_internal_AbstractCollectionSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ encoder: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractCollectionSerializer_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractCollectionSerializer_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.serialize(encoder: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: encoder, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder, value: __Self.Collection.__fromKotlinPointer(value))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_AbstractPolymorphicSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Swift_Optional_Swift_String_____reverse_swift")
package func kotlinx_serialization_internal_AbstractPolymorphicSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_CompositeDecoder_Swift_Optional_Swift_String_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ decoder: Swift.UnsafeMutableRawPointer, _ klassName: Swift.String?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractPolymorphicSerializer_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractPolymorphicSerializer_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<any ExportedKotlinPackages.kotlinx.serialization.DeserializationStrategy<__Self.T>> = _self.findPolymorphicSerializerOrNull(decoder: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: decoder, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder, klassName: klassName)
        return { if let it = _result { return it.__externalRCRef() } else { return nil } }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_AbstractPolymorphicSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T____reverse_swift")
package func kotlinx_serialization_internal_AbstractPolymorphicSerializer_findPolymorphicSerializerOrNull__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ encoder: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractPolymorphicSerializer_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._AbstractPolymorphicSerializer_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<__Self.T>> = _self.findPolymorphicSerializerOrNull(encoder: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: encoder, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder, value: __Self.T.__fromKotlinPointer(value))
        return { if let it = _result { return it.__externalRCRef() } else { return nil } }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_GeneratedSerializer_childSerializers__reverse_swift")
package func kotlinx_serialization_internal_GeneratedSerializer_childSerializers__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`.GeneratedSerializer)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`.GeneratedSerializer>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>> = _self.childSerializers()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_GeneratedSerializer_typeParametersSerializers__reverse_swift")
package func kotlinx_serialization_internal_GeneratedSerializer_typeParametersSerializers__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`.GeneratedSerializer)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`.GeneratedSerializer>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: ExportedKotlinPackages.kotlin.Array<ExportedKotlinPackages.kotlinx.serialization.AnyKSerializer<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>> = _self.typeParametersSerializers()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_MapLikeSerializer_descriptor_get__reverse_swift")
package func kotlinx_serialization_internal_MapLikeSerializer_descriptor_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._MapLikeSerializer_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._MapLikeSerializer_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor = _self.descriptor
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_MapLikeSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Collection____reverse_swift")
package func kotlinx_serialization_internal_MapLikeSerializer_serialize__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_encoding_Encoder_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ encoder: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._MapLikeSerializer_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._MapLikeSerializer_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.serialize(encoder: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: encoder, conformsTo: ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder, value: __Self.Collection.__fromKotlinPointer(value))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_internal_TaggedDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder = _self.beginStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_internal_TaggedDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder = _self.decodeInline(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedDecoder_decodeNotNullMark__reverse_swift")
package func kotlinx_serialization_internal_TaggedDecoder_decodeNotNullMark__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.decodeNotNullMark()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_internal_TaggedDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.endStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedDecoder_serializersModule_get__reverse_swift")
package func kotlinx_serialization_internal_TaggedDecoder_serializersModule_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedDecoder_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule = _self.serializersModule
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_internal_TaggedEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder = _self.beginStructure(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift")
package func kotlinx_serialization_internal_TaggedEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder = _self.encodeInline(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedEncoder_encodeNotNullMark__reverse_swift")
package func kotlinx_serialization_internal_TaggedEncoder_encodeNotNullMark__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.encodeNotNullMark()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedEncoder_encodeNull__reverse_swift")
package func kotlinx_serialization_internal_TaggedEncoder_encodeNull__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.encodeNull()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T_____reverse_swift")
package func kotlinx_serialization_internal_TaggedEncoder_encodeNullableSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__Swift_Optional_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ serializer: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.encodeNullableSerializableElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, serializer: { let _ptr = serializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), value: Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(value))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift")
package func kotlinx_serialization_internal_TaggedEncoder_encodeSerializableElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32_anyU20ExportedKotlinPackages_kotlinx_serialization_SerializationStrategy_T__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ descriptor: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ serializer: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.encodeSerializableElement(descriptor: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: descriptor, conformsTo: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, index: index, serializer: { let _ptr = serializer; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.serialization.SerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.serialization.AnySerializationStrategy<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__createTypedWrapper(externalRCRef: _ptr) }(), value: Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(value))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_internal_TaggedEncoder_serializersModule_get__reverse_swift")
package func kotlinx_serialization_internal_TaggedEncoder_serializersModule_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.serialization.`internal`._TaggedEncoder_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule = _self.serializersModule
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_serialization_modules_SerializersModule_dumpTo__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModuleCollector____reverse_swift")
package func kotlinx_serialization_modules_SerializersModule_dumpTo__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModuleCollector____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ collector: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.dumpTo(collector: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: collector, conformsTo: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector)
    return { _result; return true }()
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_serialization_builtins_ListSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_T___(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlinx_serialization_builtins_ListSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_T___(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_serialization_builtins_MapEntrySerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_K__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_V___(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlinx_serialization_builtins_MapEntrySerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_K__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_V___(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_serialization_builtins_MapSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_K__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_V___(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlinx_serialization_builtins_MapSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_K__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_V___(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_serialization_builtins_PairSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_K__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_V___(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlinx_serialization_builtins_PairSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_K__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_V___(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_serialization_builtins_SetSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_T___(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlinx_serialization_builtins_SetSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_T___(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_serialization_builtins_TripleSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_A__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_B__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_C___(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer, _ _p2: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlinx_serialization_builtins_TripleSerializer__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_A__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_B__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_C___(_p0, _p1, _p2)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_serialization_builtins_nullable_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_T___(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlinx_serialization_builtins_nullable_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_serialization_KSerializer_T___(_p0)
}
