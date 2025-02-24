@_exported import ExportedKotlinPackages
import KotlinRuntimeSupport
import KotlinRuntime
import stdlib
@_implementationOnly import KotlinBridges_KotlinSerialization

public typealias `internal` = ExportedKotlinPackages.kotlinx.serialization.internal
public typealias modules = ExportedKotlinPackages.kotlinx.serialization.modules
public typealias descriptors = ExportedKotlinPackages.kotlinx.serialization.descriptors
public typealias builtins = ExportedKotlinPackages.kotlinx.serialization.builtins
public typealias encoding = ExportedKotlinPackages.kotlinx.serialization.encoding
public typealias BinaryFormat = ExportedKotlinPackages.kotlinx.serialization.BinaryFormat
public typealias MissingFieldException = ExportedKotlinPackages.kotlinx.serialization.MissingFieldException
public typealias SerialFormat = ExportedKotlinPackages.kotlinx.serialization.SerialFormat
public typealias SerializationException = ExportedKotlinPackages.kotlinx.serialization.SerializationException
public typealias StringFormat = ExportedKotlinPackages.kotlinx.serialization.StringFormat
public func serializer(
    kClass: Swift.Never,
    typeArgumentsSerializers: [Swift.Never],
    isNullable: Swift.Bool
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.serializer(kClass: kClass, typeArgumentsSerializers: typeArgumentsSerializers, isNullable: isNullable)
}
public func serializer(
    type: any ExportedKotlinPackages.kotlin.reflect.KType
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.serializer(type: type)
}
public func serializer(
    _ receiver: Swift.Never
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.serializer(receiver)
}
public func serializer(
    _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
    kClass: Swift.Never,
    typeArgumentsSerializers: [Swift.Never],
    isNullable: Swift.Bool
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.serializer(receiver, kClass: kClass, typeArgumentsSerializers: typeArgumentsSerializers, isNullable: isNullable)
}
public func serializer(
    _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
    type: any ExportedKotlinPackages.kotlin.reflect.KType
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.serializer(receiver, type: type)
}
public func serializerOrNull(
    type: any ExportedKotlinPackages.kotlin.reflect.KType
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.serializerOrNull(type: type)
}
public func serializerOrNull(
    _ receiver: Swift.Never
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.serializerOrNull(receiver)
}
public func serializerOrNull(
    _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
    type: any ExportedKotlinPackages.kotlin.reflect.KType
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.serializerOrNull(receiver, type: type)
}
public func decodeFromHexString(
    _ receiver: any ExportedKotlinPackages.kotlinx.serialization.BinaryFormat,
    deserializer: Swift.Never,
    hex: Swift.String
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.decodeFromHexString(receiver, deserializer: deserializer, hex: hex)
}
public func encodeToHexString(
    _ receiver: any ExportedKotlinPackages.kotlinx.serialization.BinaryFormat,
    serializer: Swift.Never,
    value: Swift.Never
) -> Swift.String {
    ExportedKotlinPackages.kotlinx.serialization.encodeToHexString(receiver, serializer: serializer, value: value)
}
public func findPolymorphicSerializer(
    _ receiver: Swift.Never,
    decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder,
    klassName: Swift.String?
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.findPolymorphicSerializer(receiver, decoder: decoder, klassName: klassName)
}
public func findPolymorphicSerializer(
    _ receiver: Swift.Never,
    encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
    value: Swift.Never
) -> Swift.Never {
    ExportedKotlinPackages.kotlinx.serialization.findPolymorphicSerializer(receiver, encoder: encoder, value: value)
}
public extension ExportedKotlinPackages.kotlinx.serialization {
    public protocol BinaryFormat: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.SerialFormat {
        func decodeFromByteArray(
            deserializer: Swift.Never,
            bytes: ExportedKotlinPackages.kotlin.ByteArray
        ) -> Swift.Never
        func encodeToByteArray(
            serializer: Swift.Never,
            value: Swift.Never
        ) -> ExportedKotlinPackages.kotlin.ByteArray
    }
    public protocol SerialFormat: KotlinRuntime.KotlinBase {
        var serializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
            get
        }
    }
    public protocol StringFormat: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.SerialFormat {
        func decodeFromString(
            deserializer: Swift.Never,
            string: Swift.String
        ) -> Swift.Never
        func encodeToString(
            serializer: Swift.Never,
            value: Swift.Never
        ) -> Swift.String
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
            let __kt = kotlinx_serialization_MissingFieldException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlinx_serialization_MissingFieldException_init_initialize__TypesOfArguments__Swift_UInt_Swift_Array_Swift_String__Swift_String__(__kt, missingFields, serialName)
        }
        public init(
            missingField: Swift.String,
            serialName: Swift.String
        ) {
            let __kt = kotlinx_serialization_MissingFieldException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlinx_serialization_MissingFieldException_init_initialize__TypesOfArguments__Swift_UInt_Swift_String_Swift_String__(__kt, missingField, serialName)
        }
        public init(
            missingFields: [Swift.String],
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlinx_serialization_MissingFieldException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlinx_serialization_MissingFieldException_init_initialize__TypesOfArguments__Swift_UInt_Swift_Array_Swift_String__Swift_String_opt__ExportedKotlinPackages_kotlin_Throwable_opt___(__kt, missingFields, message ?? nil, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class SerializationException: ExportedKotlinPackages.kotlin.IllegalArgumentException {
        public override init() {
            let __kt = kotlinx_serialization_SerializationException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        public override init(
            message: Swift.String?
        ) {
            let __kt = kotlinx_serialization_SerializationException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UInt_Swift_String_opt___(__kt, message ?? nil)
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlinx_serialization_SerializationException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UInt_Swift_String_opt__ExportedKotlinPackages_kotlin_Throwable_opt___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
            let __kt = kotlinx_serialization_SerializationException_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlinx_serialization_SerializationException_init_initialize__TypesOfArguments__Swift_UInt_ExportedKotlinPackages_kotlin_Throwable_opt___(__kt, cause.map { it in it.__externalRCRef() } ?? 0)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static func serializer(
        kClass: Swift.Never,
        typeArgumentsSerializers: [Swift.Never],
        isNullable: Swift.Bool
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        type: any ExportedKotlinPackages.kotlin.reflect.KType
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        kClass: Swift.Never,
        typeArgumentsSerializers: [Swift.Never],
        isNullable: Swift.Bool
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        type: any ExportedKotlinPackages.kotlin.reflect.KType
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializerOrNull(
        type: any ExportedKotlinPackages.kotlin.reflect.KType
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializerOrNull(
        _ receiver: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializerOrNull(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        type: any ExportedKotlinPackages.kotlin.reflect.KType
    ) -> Swift.Never {
        fatalError()
    }
    public static func decodeFromHexString(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.BinaryFormat,
        deserializer: Swift.Never,
        hex: Swift.String
    ) -> Swift.Never {
        fatalError()
    }
    public static func encodeToHexString(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.BinaryFormat,
        serializer: Swift.Never,
        value: Swift.Never
    ) -> Swift.String {
        fatalError()
    }
    public static func findPolymorphicSerializer(
        _ receiver: Swift.Never,
        decoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder,
        klassName: Swift.String?
    ) -> Swift.Never {
        fatalError()
    }
    public static func findPolymorphicSerializer(
        _ receiver: Swift.Never,
        encoder: any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder,
        value: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
}
public extension ExportedKotlinPackages.kotlinx.serialization.internal {
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
            readIfAbsent: @escaping @convention(block) (any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, Swift.Int32) -> Swift.Bool
        ) {
            let __kt = kotlinx_serialization_internal_ElementMarker_init_allocate()
            super.init(__externalRCRef: __kt)
            kotlinx_serialization_internal_ElementMarker_init_initialize__TypesOfArguments__Swift_UInt_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_U28anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_U20Swift_Int32U29202D_U20Swift_Bool__(__kt, descriptor.__externalRCRef(), {
                let originalBlock = readIfAbsent
                return { arg0, arg1 in return originalBlock(KotlinRuntime.KotlinBase(__externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor, arg1) }
            }())
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static func InlinePrimitiveDescriptor(
        name: Swift.String,
        primitiveSerializer: Swift.Never
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        fatalError()
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
public extension ExportedKotlinPackages.kotlinx.serialization.modules {
    public protocol SerializersModuleCollector: KotlinRuntime.KotlinBase {
        func contextual(
            kClass: Swift.Never,
            provider: @escaping @convention(block) (Swift.Array<Swift.Never>) -> Swift.Never
        ) -> Swift.Void
        func contextual(
            kClass: Swift.Never,
            serializer: Swift.Never
        ) -> Swift.Void
        func polymorphic(
            baseClass: Swift.Never,
            actualClass: Swift.Never,
            actualSerializer: Swift.Never
        ) -> Swift.Void
        @available(*, deprecated, message: "Deprecated in favor of function with more precise name: polymorphicDefaultDeserializer. Replacement: polymorphicDefaultDeserializer(baseClass, defaultDeserializerProvider)")
        func polymorphicDefault(
            baseClass: Swift.Never,
            defaultDeserializerProvider: @escaping @convention(block) (Swift.Optional<Swift.String>) -> Swift.Never
        ) -> Swift.Void
        func polymorphicDefaultDeserializer(
            baseClass: Swift.Never,
            defaultDeserializerProvider: @escaping @convention(block) (Swift.Optional<Swift.String>) -> Swift.Never
        ) -> Swift.Void
        func polymorphicDefaultSerializer(
            baseClass: Swift.Never,
            defaultSerializerProvider: @escaping @convention(block) (Swift.Never) -> Swift.Never
        ) -> Swift.Void
    }
    open class SerializersModule: KotlinRuntime.KotlinBase {
        open func dumpTo(
            collector: any ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector
        ) -> Swift.Void {
            return kotlinx_serialization_modules_SerializersModule_dumpTo__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModuleCollector__(self.__externalRCRef(), collector.__externalRCRef())
        }
        open func getContextual(
            kClass: Swift.Never,
            typeArgumentsSerializers: [Swift.Never]
        ) -> Swift.Never {
            fatalError()
        }
        open func getPolymorphic(
            baseClass: Swift.Never,
            value: Swift.Never
        ) -> Swift.Never {
            fatalError()
        }
        open func getPolymorphic(
            baseClass: Swift.Never,
            serializedClassName: Swift.String?
        ) -> Swift.Never {
            fatalError()
        }
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class SerializersModuleBuilder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModuleCollector {
        public func contextual(
            kClass: Swift.Never,
            provider: @escaping @convention(block) (Swift.Array<Swift.Never>) -> Swift.Never
        ) -> Swift.Void {
            fatalError()
        }
        public func contextual(
            kClass: Swift.Never,
            serializer: Swift.Never
        ) -> Swift.Void {
            fatalError()
        }
        public func include(
            module: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule
        ) -> Swift.Void {
            return kotlinx_serialization_modules_SerializersModuleBuilder_include__TypesOfArguments__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule__(self.__externalRCRef(), module.__externalRCRef())
        }
        public func polymorphic(
            baseClass: Swift.Never,
            actualClass: Swift.Never,
            actualSerializer: Swift.Never
        ) -> Swift.Void {
            fatalError()
        }
        public func polymorphicDefaultDeserializer(
            baseClass: Swift.Never,
            defaultDeserializerProvider: @escaping @convention(block) (Swift.Optional<Swift.String>) -> Swift.Never
        ) -> Swift.Void {
            fatalError()
        }
        public func polymorphicDefaultSerializer(
            baseClass: Swift.Never,
            defaultSerializerProvider: @escaping @convention(block) (Swift.Never) -> Swift.Never
        ) -> Swift.Void {
            fatalError()
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    @available(*, deprecated, message: "Deprecated in the favour of 'EmptySerializersModule()'. Replacement: EmptySerializersModule()")
    public static var EmptySerializersModule: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        get {
            return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule(__externalRCRef: kotlinx_serialization_modules_EmptySerializersModule_get())
        }
    }
    public static func EmptySerializersModule() -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule(__externalRCRef: kotlinx_serialization_modules_EmptySerializersModule())
    }
    public static func serializersModuleOf(
        kClass: Swift.Never,
        serializer: Swift.Never
    ) -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        fatalError()
    }
    public static func overwriteWith(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        other: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule
    ) -> ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule {
        return ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule(__externalRCRef: kotlinx_serialization_modules_overwriteWith__TypesOfArguments__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule__(receiver.__externalRCRef(), other.__externalRCRef()))
    }
}
public extension ExportedKotlinPackages.kotlinx.serialization.descriptors {
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
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class PolymorphicKind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        public final class OPEN: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.OPEN {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.OPEN(__externalRCRef: kotlinx_serialization_descriptors_PolymorphicKind_OPEN_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class SEALED: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.SEALED {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PolymorphicKind.SEALED(__externalRCRef: kotlinx_serialization_descriptors_PolymorphicKind_SEALED_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class PrimitiveKind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        public final class BOOLEAN: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN(__externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_BOOLEAN_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class BYTE: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BYTE {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.BYTE(__externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_BYTE_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class CHAR: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.CHAR {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.CHAR(__externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_CHAR_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class DOUBLE: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE(__externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_DOUBLE_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class FLOAT: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.FLOAT {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.FLOAT(__externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_FLOAT_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class INT: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.INT {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.INT(__externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_INT_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class LONG: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.LONG {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.LONG(__externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_LONG_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class SHORT: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.SHORT {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.SHORT(__externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_SHORT_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class STRING: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.STRING {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind.STRING(__externalRCRef: kotlinx_serialization_descriptors_PrimitiveKind_STRING_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class SerialKind: KotlinRuntime.KotlinBase {
        public final class CONTEXTUAL: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.CONTEXTUAL {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.CONTEXTUAL(__externalRCRef: kotlinx_serialization_descriptors_SerialKind_CONTEXTUAL_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class ENUM: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.ENUM {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind.ENUM(__externalRCRef: kotlinx_serialization_descriptors_SerialKind_ENUM_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        open func hashCode() -> Swift.Int32 {
            return kotlinx_serialization_descriptors_SerialKind_hashCode(self.__externalRCRef())
        }
        open func toString() -> Swift.String {
            return kotlinx_serialization_descriptors_SerialKind_toString(self.__externalRCRef())
        }
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class StructureKind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind {
        public final class CLASS: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.CLASS {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.CLASS(__externalRCRef: kotlinx_serialization_descriptors_StructureKind_CLASS_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class LIST: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.LIST {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.LIST(__externalRCRef: kotlinx_serialization_descriptors_StructureKind_LIST_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class MAP: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.MAP {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.MAP(__externalRCRef: kotlinx_serialization_descriptors_StructureKind_MAP_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        public final class OBJECT: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind {
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.OBJECT {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.descriptors.StructureKind.OBJECT(__externalRCRef: kotlinx_serialization_descriptors_StructureKind_OBJECT_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static func getCapturedKClass(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Never {
        fatalError()
    }
    public static func getElementDescriptors(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Never {
        fatalError()
    }
    public static func getElementNames(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> Swift.Never {
        fatalError()
    }
    public static func getNullable(
        _ receiver: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_descriptors_nullable_get__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func PrimitiveSerialDescriptor(
        serialName: Swift.String,
        kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.PrimitiveKind
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_descriptors_PrimitiveSerialDescriptor__TypesOfArguments__Swift_String_ExportedKotlinPackages_kotlinx_serialization_descriptors_PrimitiveKind__(serialName, kind.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func serialDescriptor(
        serialName: Swift.String,
        original: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_descriptors_SerialDescriptor__TypesOfArguments__Swift_String_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(serialName, original.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func buildClassSerialDescriptor(
        serialName: Swift.String,
        typeParameters: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        builderAction: @escaping @convention(block) () -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_descriptors_buildClassSerialDescriptor__TypesOfArguments__Swift_String_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_U2829202D_U20Swift_Void__(serialName, typeParameters.__externalRCRef(), {
            let originalBlock = builderAction
            return { return originalBlock() }
        }())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func buildSerialDescriptor(
        serialName: Swift.String,
        kind: ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialKind,
        typeParameters: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        builder: @escaping @convention(block) () -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_descriptors_buildSerialDescriptor__TypesOfArguments__Swift_String_ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialKind_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_U2829202D_U20Swift_Void__(serialName, kind.__externalRCRef(), typeParameters.__externalRCRef(), {
            let originalBlock = builder
            return { return originalBlock() }
        }())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func listSerialDescriptor(
        elementDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_descriptors_listSerialDescriptor__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(elementDescriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func mapSerialDescriptor(
        keyDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
        valueDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_descriptors_mapSerialDescriptor__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(keyDescriptor.__externalRCRef(), valueDescriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func serialDescriptor(
        type: any ExportedKotlinPackages.kotlin.reflect.KType
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_descriptors_serialDescriptor__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_reflect_KType__(type.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func setSerialDescriptor(
        elementDescriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor {
        return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_descriptors_setSerialDescriptor__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(elementDescriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    }
    public static func getContextualDescriptor(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> (any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor)? {
        return switch kotlinx_serialization_descriptors_getContextualDescriptor__TypesOfArguments__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef(), descriptor.__externalRCRef()) { case 0: .none; case let res: KotlinRuntime.KotlinBase(__externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor; }
    }
    public static func getPolymorphicDescriptors(
        _ receiver: ExportedKotlinPackages.kotlinx.serialization.modules.SerializersModule,
        descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
    ) -> [any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor] {
        return kotlinx_serialization_descriptors_getPolymorphicDescriptors__TypesOfArguments__ExportedKotlinPackages_kotlinx_serialization_modules_SerializersModule_anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(receiver.__externalRCRef(), descriptor.__externalRCRef()) as! Swift.Array<any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor>
    }
}
public extension ExportedKotlinPackages.kotlinx.serialization.builtins {
    public static func getNullable(
        _ receiver: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
    public static func ArraySerializer(
        kClass: Swift.Never,
        elementSerializer: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
    public static func BooleanArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func ByteArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func CharArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func DoubleArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func FloatArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func IntArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func ListSerializer(
        elementSerializer: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
    public static func LongArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func MapEntrySerializer(
        keySerializer: Swift.Never,
        valueSerializer: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
    public static func MapSerializer(
        keySerializer: Swift.Never,
        valueSerializer: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
    public static func NothingSerializer() -> Swift.Never {
        fatalError()
    }
    public static func PairSerializer(
        keySerializer: Swift.Never,
        valueSerializer: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
    public static func SetSerializer(
        elementSerializer: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
    public static func ShortArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func TripleSerializer(
        aSerializer: Swift.Never,
        bSerializer: Swift.Never,
        cSerializer: Swift.Never
    ) -> Swift.Never {
        fatalError()
    }
    public static func UByteArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func UIntArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func ULongArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func UShortArraySerializer() -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Boolean.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Byte.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Char.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Double.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Float.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Int.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Long.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.Short.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.String.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.UByte.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.UInt.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.ULong.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.UShort.Companion
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: Swift.Void
    ) -> Swift.Never {
        fatalError()
    }
    public static func serializer(
        _ receiver: ExportedKotlinPackages.kotlin.time.Duration.Companion
    ) -> Swift.Never {
        fatalError()
    }
}
public extension ExportedKotlinPackages.kotlinx.serialization.encoding {
    public protocol ChunkedDecoder: KotlinRuntime.KotlinBase {
        func decodeStringChunked(
            consumeChunk: @escaping @convention(block) (Swift.String) -> Swift.Void
        ) -> Swift.Void
    }
    public protocol CompositeDecoder: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
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
            public static var shared: ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Companion {
                get {
                    return ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder.Companion(__externalRCRef: kotlinx_serialization_encoding_CompositeDecoder_Companion_get())
                }
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            private override init() {
                fatalError()
            }
        }
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
        func decodeNullableSerializableElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            deserializer: Swift.Never,
            previousValue: Swift.Never
        ) -> Swift.Never
        func decodeSequentially() -> Swift.Bool
        func decodeSerializableElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            deserializer: Swift.Never,
            previousValue: Swift.Never
        ) -> Swift.Never
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
        func encodeNullableSerializableElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: Swift.Never,
            value: Swift.Never
        ) -> Swift.Void
        func encodeSerializableElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: Swift.Never,
            value: Swift.Never
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
        func decodeNullableSerializableValue(
            deserializer: Swift.Never
        ) -> Swift.Never
        func decodeSerializableValue(
            deserializer: Swift.Never
        ) -> Swift.Never
        func decodeShort() -> Swift.Int16
        func decodeString() -> Swift.String
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
        func encodeNullableSerializableValue(
            serializer: Swift.Never,
            value: Swift.Never
        ) -> Swift.Void
        func encodeSerializableValue(
            serializer: Swift.Never,
            value: Swift.Never
        ) -> Swift.Void
        func encodeShort(
            value: Swift.Int16
        ) -> Swift.Void
        func encodeString(
            value: Swift.String
        ) -> Swift.Void
    }
    open class AbstractDecoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder, ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder {
        open func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder {
            return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeDecoder
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
            return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
        }
        open func decodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder {
            return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Decoder
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
        public final func decodeNullableSerializableElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            deserializer: Swift.Never,
            previousValue: Swift.Never
        ) -> Swift.Never {
            fatalError()
        }
        open func decodeSerializableElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            deserializer: Swift.Never,
            previousValue: Swift.Never
        ) -> Swift.Never {
            fatalError()
        }
        open func decodeSerializableValue(
            deserializer: Swift.Never,
            previousValue: Swift.Never
        ) -> Swift.Never {
            fatalError()
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
        open func decodeValue() -> KotlinRuntime.KotlinBase {
            return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_encoding_AbstractDecoder_decodeValue(self.__externalRCRef()))
        }
        open func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractDecoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())
        }
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    open class AbstractEncoder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder, ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
        open func beginStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder {
            return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_beginStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.CompositeEncoder
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
            return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_encodeInline__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
        }
        public final func encodeInlineElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder {
            return KotlinRuntime.KotlinBase(__externalRCRef: kotlinx_serialization_encoding_AbstractEncoder_encodeInlineElement__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor_Swift_Int32__(self.__externalRCRef(), descriptor.__externalRCRef(), index)) as! any ExportedKotlinPackages.kotlinx.serialization.encoding.Encoder
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
        open func encodeNullableSerializableElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: Swift.Never,
            value: Swift.Never
        ) -> Swift.Void {
            fatalError()
        }
        open func encodeSerializableElement(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor,
            index: Swift.Int32,
            serializer: Swift.Never,
            value: Swift.Never
        ) -> Swift.Void {
            fatalError()
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
            value: KotlinRuntime.KotlinBase
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_encodeValue__TypesOfArguments__KotlinRuntime_KotlinBase__(self.__externalRCRef(), value.__externalRCRef())
        }
        open func endStructure(
            descriptor: any ExportedKotlinPackages.kotlinx.serialization.descriptors.SerialDescriptor
        ) -> Swift.Void {
            return kotlinx_serialization_encoding_AbstractEncoder_endStructure__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_serialization_descriptors_SerialDescriptor__(self.__externalRCRef(), descriptor.__externalRCRef())
        }
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
