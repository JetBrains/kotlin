#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class KotlinByteArray, SerializersModule, KotlinArray<T>, KotlinThrowable, KotlinException, KotlinRuntimeException, KotlinIllegalArgumentException, SerializationException, AbstractPolymorphicSerializer<T>, LongAsStringSerializer, SerialKind, PolymorphicKind, PolymorphicKindOPEN, PolymorphicKindSEALED, PrimitiveKind, PrimitiveKindBOOLEAN, PrimitiveKindBYTE, PrimitiveKindCHAR, PrimitiveKindDOUBLE, PrimitiveKindFLOAT, PrimitiveKindINT, PrimitiveKindLONG, PrimitiveKindSHORT, PrimitiveKindSTRING, SerialKindCONTEXTUAL, SerialKindENUM, StructureKind, StructureKindCLASS, StructureKindLIST, StructureKindMAP, StructureKindOBJECT, KotlinNothing, CompositeDecoderCompanion, AbstractCollectionSerializer<Element, Collection, Builder>, TaggedDecoder<Tag>, TaggedEncoder<Tag>, KotlinBooleanCompanion, KotlinByteCompanion, KotlinCharCompanion, KotlinDoubleCompanion, KotlinFloatCompanion, KotlinIntCompanion, KotlinLongCompanion, KotlinShortCompanion, KotlinStringCompanion, KotlinUByteCompanion, KotlinUIntCompanion, KotlinULongCompanion, KotlinUShortCompanion, KotlinUnit, KotlinDurationCompanion, KotlinDurationUnit, ClassSerialDescriptorBuilder, PolymorphicModuleBuilder<__contravariant Base_>, SerializersModuleBuilder, KotlinIntArray, KotlinByteIterator, KotlinEnumCompanion, KotlinEnum<E>, KotlinKTypeProjection, KotlinIntIterator, KotlinKVariance, KotlinKTypeProjectionCompanion;

@protocol DeserializationStrategy, SerializationStrategy, SerialFormat, Encoder, SerialDescriptor, Decoder, KSerializer, KotlinKClass, CompositeDecoder, KotlinAnnotation, CompositeEncoder, KotlinIterator, KotlinMapEntry, SerializersModuleCollector, KotlinKType, BinaryFormat, StringFormat, KotlinKDeclarationContainer, KotlinKAnnotatedElement, KotlinKClassifier, KotlinComparable;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

@protocol SerialFormat
@required
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

@protocol BinaryFormat <SerialFormat>
@required
- (id _Nullable)decodeFromByteArrayDeserializer:(id<DeserializationStrategy>)deserializer bytes:(KotlinByteArray *)bytes __attribute__((swift_name("decodeFromByteArray(deserializer:bytes:)")));
- (KotlinByteArray *)encodeToByteArraySerializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeToByteArray(serializer:value:)")));
@end

@protocol SerializationStrategy
@required
- (void)serializeEncoder:(id<Encoder>)encoder value:(id _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

@protocol DeserializationStrategy
@required
- (id _Nullable)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

@protocol KSerializer <SerializationStrategy, DeserializationStrategy>
@required
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((objc_subclassing_restricted))
@interface ContextualSerializer<T> : Base <KSerializer>
- (instancetype)initWithSerializableClass:(id<KotlinKClass>)serializableClass __attribute__((swift_name("init(serializableClass:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithSerializableClass:(id<KotlinKClass>)serializableClass fallbackSerializer:(id<KSerializer> _Nullable)fallbackSerializer typeArgumentsSerializers:(KotlinArray<id<KSerializer>> *)typeArgumentsSerializers __attribute__((swift_name("init(serializableClass:fallbackSerializer:typeArgumentsSerializers:)"))) __attribute__((objc_designated_initializer));
- (T)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(T)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

@interface KotlinThrowable : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
- (KotlinArray<NSString *> *)getStackTrace __attribute__((swift_name("getStackTrace()")));
- (void)printStackTrace __attribute__((swift_name("printStackTrace()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) KotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (NSError *)asError __attribute__((swift_name("asError()")));
@end

@interface KotlinException : KotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

@interface KotlinRuntimeException : KotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

@interface KotlinIllegalArgumentException : KotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

@interface SerializationException : KotlinIllegalArgumentException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((objc_subclassing_restricted))
@interface MissingFieldException : SerializationException
- (instancetype)initWithMissingField:(NSString *)missingField serialName:(NSString *)serialName __attribute__((swift_name("init(missingField:serialName:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMissingFields:(NSArray<NSString *> *)missingFields serialName:(NSString *)serialName __attribute__((swift_name("init(missingFields:serialName:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMissingFields:(NSArray<NSString *> *)missingFields message:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(missingFields:message:cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (readonly) NSArray<NSString *> *missingFields __attribute__((swift_name("missingFields")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface AbstractPolymorphicSerializer<T> : Base <KSerializer>
- (T)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<DeserializationStrategy> _Nullable)findPolymorphicSerializerOrNullDecoder:(id<CompositeDecoder>)decoder klassName:(NSString * _Nullable)klassName __attribute__((swift_name("findPolymorphicSerializerOrNull(decoder:klassName:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<SerializationStrategy> _Nullable)findPolymorphicSerializerOrNullEncoder:(id<Encoder>)encoder value:(T)value __attribute__((swift_name("findPolymorphicSerializerOrNull(encoder:value:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(T)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<KotlinKClass> baseClass __attribute__((swift_name("baseClass")));
@end

__attribute__((objc_subclassing_restricted))
@interface PolymorphicSerializer<T> : AbstractPolymorphicSerializer<T>
- (instancetype)initWithBaseClass:(id<KotlinKClass>)baseClass __attribute__((swift_name("init(baseClass:)"))) __attribute__((objc_designated_initializer));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<KotlinKClass> baseClass __attribute__((swift_name("baseClass")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
__attribute__((objc_subclassing_restricted))
@interface SealedClassSerializer<T> : AbstractPolymorphicSerializer<T>
- (instancetype)initWithSerialName:(NSString *)serialName baseClass:(id<KotlinKClass>)baseClass subclasses:(KotlinArray<id<KotlinKClass>> *)subclasses subclassSerializers:(KotlinArray<id<KSerializer>> *)subclassSerializers __attribute__((swift_name("init(serialName:baseClass:subclasses:subclassSerializers:)"))) __attribute__((objc_designated_initializer));
- (id<DeserializationStrategy> _Nullable)findPolymorphicSerializerOrNullDecoder:(id<CompositeDecoder>)decoder klassName:(NSString * _Nullable)klassName __attribute__((swift_name("findPolymorphicSerializerOrNull(decoder:klassName:)")));
- (id<SerializationStrategy> _Nullable)findPolymorphicSerializerOrNullEncoder:(id<Encoder>)encoder value:(T)value __attribute__((swift_name("findPolymorphicSerializerOrNull(encoder:value:)")));
@property (readonly) id<KotlinKClass> baseClass __attribute__((swift_name("baseClass")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

@protocol StringFormat <SerialFormat>
@required
- (id _Nullable)decodeFromStringDeserializer:(id<DeserializationStrategy>)deserializer string:(NSString *)string __attribute__((swift_name("decodeFromString(deserializer:string:)")));
- (NSString *)encodeToStringSerializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeToString(serializer:value:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface LongAsStringSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)longAsStringSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LongAsStringSerializer *shared __attribute__((swift_name("shared")));
- (Long *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(Long *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface ClassSerialDescriptorBuilder : Base
- (void)elementElementName:(NSString *)elementName descriptor:(id<SerialDescriptor>)descriptor annotations:(NSArray<id<KotlinAnnotation>> *)annotations isOptional:(BOOL)isOptional __attribute__((swift_name("element(elementName:descriptor:annotations:isOptional:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property NSArray<id<KotlinAnnotation>> *annotations __attribute__((swift_name("annotations")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property BOOL isNullable __attribute__((swift_name("isNullable"))) __attribute__((unavailable("isNullable inside buildSerialDescriptor is deprecated. Please use SerialDescriptor.nullable extension on a builder result.")));
@property (readonly) NSString *serialName __attribute__((swift_name("serialName")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@interface SerialKind : Base
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@interface PolymorphicKind : SerialKind
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PolymorphicKind.OPEN")))
@interface PolymorphicKindOPEN : PolymorphicKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)oPEN __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PolymorphicKindOPEN *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PolymorphicKind.SEALED")))
@interface PolymorphicKindSEALED : PolymorphicKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sEALED __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PolymorphicKindSEALED *shared __attribute__((swift_name("shared")));
@end

@interface PrimitiveKind : SerialKind
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.BOOLEAN")))
@interface PrimitiveKindBOOLEAN : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)bOOLEAN __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindBOOLEAN *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.BYTE")))
@interface PrimitiveKindBYTE : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)bYTE __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindBYTE *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.CHAR")))
@interface PrimitiveKindCHAR : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cHAR __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindCHAR *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.DOUBLE")))
@interface PrimitiveKindDOUBLE : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dOUBLE __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindDOUBLE *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.FLOAT")))
@interface PrimitiveKindFLOAT : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)fLOAT __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindFLOAT *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.INT")))
@interface PrimitiveKindINT : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)iNT __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindINT *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.LONG")))
@interface PrimitiveKindLONG : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lONG __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindLONG *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.SHORT")))
@interface PrimitiveKindSHORT : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sHORT __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindSHORT *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.STRING")))
@interface PrimitiveKindSTRING : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sTRING __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindSTRING *shared __attribute__((swift_name("shared")));
@end

@protocol SerialDescriptor
@required

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (NSArray<id<KotlinAnnotation>> *)getElementAnnotationsIndex:(int32_t)index __attribute__((swift_name("getElementAnnotations(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SerialDescriptor>)getElementDescriptorIndex:(int32_t)index __attribute__((swift_name("getElementDescriptor(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (int32_t)getElementIndexName:(NSString *)name __attribute__((swift_name("getElementIndex(name:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (NSString *)getElementNameIndex:(int32_t)index __attribute__((swift_name("getElementName(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)isElementOptionalIndex:(int32_t)index __attribute__((swift_name("isElementOptional(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) NSArray<id<KotlinAnnotation>> *annotations __attribute__((swift_name("annotations")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) int32_t elementsCount __attribute__((swift_name("elementsCount")));
@property (readonly) BOOL isInline __attribute__((swift_name("isInline")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) BOOL isNullable __attribute__((swift_name("isNullable")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) SerialKind *kind __attribute__((swift_name("kind")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) NSString *serialName __attribute__((swift_name("serialName")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SerialKind.CONTEXTUAL")))
@interface SerialKindCONTEXTUAL : SerialKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cONTEXTUAL __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SerialKindCONTEXTUAL *shared __attribute__((swift_name("shared")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SerialKind.ENUM")))
@interface SerialKindENUM : SerialKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)eNUM __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SerialKindENUM *shared __attribute__((swift_name("shared")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@interface StructureKind : SerialKind
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StructureKind.CLASS")))
@interface StructureKindCLASS : StructureKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cLASS __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) StructureKindCLASS *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StructureKind.LIST")))
@interface StructureKindLIST : StructureKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lIST __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) StructureKindLIST *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StructureKind.MAP")))
@interface StructureKindMAP : StructureKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)mAP __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) StructureKindMAP *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StructureKind.OBJECT")))
@interface StructureKindOBJECT : StructureKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)oBJECT __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) StructureKindOBJECT *shared __attribute__((swift_name("shared")));
@end

@protocol Decoder
@required
- (id<CompositeDecoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (int32_t)decodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (id<Decoder>)decodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (KotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableValueDeserializer:(id<DeserializationStrategy>)deserializer __attribute__((swift_name("decodeNullableSerializableValue(deserializer:)")));
- (id _Nullable)decodeSerializableValueDeserializer:(id<DeserializationStrategy>)deserializer __attribute__((swift_name("decodeSerializableValue(deserializer:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

@protocol CompositeDecoder
@required
- (BOOL)decodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (int32_t)decodeCollectionSizeDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("decodeCollectionSize(descriptor:)")));
- (double)decodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeElementIndexDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("decodeElementIndex(descriptor:)")));
- (float)decodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<Decoder>)decodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeSequentially __attribute__((swift_name("decodeSequentially()")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (int16_t)decodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@interface AbstractDecoder : Base <Decoder, CompositeDecoder>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<CompositeDecoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (BOOL)decodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (int8_t)decodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (unichar)decodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (double)decodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (float)decodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<Decoder>)decodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (id<Decoder>)decodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int32_t)decodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));
- (int64_t)decodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));
- (KotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (id _Nullable)decodeSerializableValueDeserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableValue(deserializer:previousValue:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (int16_t)decodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
- (NSString *)decodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));
- (id)decodeValue __attribute__((swift_name("decodeValue()")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@end

@protocol Encoder
@required
- (id<CompositeEncoder>)beginCollectionDescriptor:(id<SerialDescriptor>)descriptor collectionSize:(int32_t)collectionSize __attribute__((swift_name("beginCollection(descriptor:collectionSize:)")));
- (id<CompositeEncoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (id<Encoder>)encodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNotNullMark __attribute__((swift_name("encodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNull __attribute__((swift_name("encodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableValueSerializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableValue(serializer:value:)")));
- (void)encodeSerializableValueSerializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableValue(serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

@protocol CompositeEncoder
@required
- (void)encodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (void)encodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<Encoder>)encodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)shouldEncodeElementDefaultDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("shouldEncodeElementDefault(descriptor:index:)")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@interface AbstractEncoder : Base <Encoder, CompositeEncoder>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<CompositeEncoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (BOOL)encodeElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeElement(descriptor:index:)")));
- (void)encodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (void)encodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<Encoder>)encodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (id<Encoder>)encodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));
- (void)encodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));
- (void)encodeNull __attribute__((swift_name("encodeNull()")));
- (void)encodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
- (void)encodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));
- (void)encodeValueValue:(id)value __attribute__((swift_name("encodeValue(value:)")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@protocol ChunkedDecoder
@required

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)decodeStringChunkedConsumeChunk:(void (^)(NSString *))consumeChunk __attribute__((swift_name("decodeStringChunked(consumeChunk:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CompositeDecoderCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) CompositeDecoderCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t DECODE_DONE __attribute__((swift_name("DECODE_DONE")));
@property (readonly) int32_t UNKNOWN_NAME __attribute__((swift_name("UNKNOWN_NAME")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface AbstractCollectionSerializer<Element, Collection, Builder> : Base <KSerializer>

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Builder _Nullable)builder __attribute__((swift_name("builder()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int32_t)builderSize:(Builder _Nullable)receiver __attribute__((swift_name("builderSize(_:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)checkCapacity:(Builder _Nullable)receiver size:(int32_t)size __attribute__((swift_name("checkCapacity(_:size:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id<KotlinIterator>)collectionIterator:(Collection _Nullable)receiver __attribute__((swift_name("collectionIterator(_:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int32_t)collectionSize:(Collection _Nullable)receiver __attribute__((swift_name("collectionSize(_:)")));
- (Collection _Nullable)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (Collection _Nullable)mergeDecoder:(id<Decoder>)decoder previous:(Collection _Nullable)previous __attribute__((swift_name("merge(decoder:previous:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)readAllDecoder:(id<CompositeDecoder>)decoder builder:(Builder _Nullable)builder startIndex:(int32_t)startIndex size:(int32_t)size __attribute__((swift_name("readAll(decoder:builder:startIndex:size:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)readElementDecoder:(id<CompositeDecoder>)decoder index:(int32_t)index builder:(Builder _Nullable)builder checkIndex:(BOOL)checkIndex __attribute__((swift_name("readElement(decoder:index:builder:checkIndex:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(Collection _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Builder _Nullable)toBuilder:(Collection _Nullable)receiver __attribute__((swift_name("toBuilder(_:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Collection _Nullable)toResult:(Builder _Nullable)receiver __attribute__((swift_name("toResult(_:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ElementMarker : Base
- (instancetype)initWithDescriptor:(id<SerialDescriptor>)descriptor readIfAbsent:(Boolean *(^)(id<SerialDescriptor>, Int *))readIfAbsent __attribute__((swift_name("init(descriptor:readIfAbsent:)"))) __attribute__((objc_designated_initializer));
- (void)markIndex:(int32_t)index __attribute__((swift_name("mark(index:)")));
- (int32_t)nextUnmarkedIndex __attribute__((swift_name("nextUnmarkedIndex()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@protocol GeneratedSerializer <KSerializer>
@required
- (KotlinArray<id<KSerializer>> *)childSerializers __attribute__((swift_name("childSerializers()")));
- (KotlinArray<id<KSerializer>> *)typeParametersSerializers __attribute__((swift_name("typeParametersSerializers()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface MapLikeSerializer<Key, Value, Collection, Builder> : AbstractCollectionSerializer<id<KotlinMapEntry>, Collection, MutableDictionary<id, id> *>

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)insertKeyValuePair:(MutableDictionary<id, id> *)receiver index:(int32_t)index key:(Key _Nullable)key value:(Value _Nullable)value __attribute__((swift_name("insertKeyValuePair(_:index:key:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)readAllDecoder:(id<CompositeDecoder>)decoder builder:(MutableDictionary<id, id> *)builder startIndex:(int32_t)startIndex size:(int32_t)size __attribute__((swift_name("readAll(decoder:builder:startIndex:size:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)readElementDecoder:(id<CompositeDecoder>)decoder index:(int32_t)index builder:(MutableDictionary<id, id> *)builder checkIndex:(BOOL)checkIndex __attribute__((swift_name("readElement(decoder:index:builder:checkIndex:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(Collection _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@property (readonly) id<KSerializer> keySerializer __attribute__((swift_name("keySerializer")));
@property (readonly) id<KSerializer> valueSerializer __attribute__((swift_name("valueSerializer")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface TaggedDecoder<Tag> : Base <Decoder, CompositeDecoder>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<CompositeDecoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)doCopyTagsToOther:(TaggedDecoder<Tag> *)other __attribute__((swift_name("doCopyTagsTo(other:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (BOOL)decodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (int8_t)decodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (unichar)decodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (double)decodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (float)decodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<Decoder>)decodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (id<Decoder>)decodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int32_t)decodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));
- (int64_t)decodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));
- (KotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id _Nullable)decodeSerializableValueDeserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableValue(deserializer:previousValue:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (int16_t)decodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
- (NSString *)decodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (BOOL)decodeTaggedBooleanTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedBoolean(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int8_t)decodeTaggedByteTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedByte(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (unichar)decodeTaggedCharTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedChar(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (double)decodeTaggedDoubleTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedDouble(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int32_t)decodeTaggedEnumTag:(Tag _Nullable)tag enumDescriptor:(id<SerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeTaggedEnum(tag:enumDescriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (float)decodeTaggedFloatTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedFloat(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id<Decoder>)decodeTaggedInlineTag:(Tag _Nullable)tag inlineDescriptor:(id<SerialDescriptor>)inlineDescriptor __attribute__((swift_name("decodeTaggedInline(tag:inlineDescriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int32_t)decodeTaggedIntTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedInt(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int64_t)decodeTaggedLongTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedLong(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (BOOL)decodeTaggedNotNullMarkTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedNotNullMark(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (KotlinNothing * _Nullable)decodeTaggedNullTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedNull(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int16_t)decodeTaggedShortTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedShort(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)decodeTaggedStringTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedString(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id)decodeTaggedValueTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedValue(tag:)")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Tag _Nullable)getTag:(id<SerialDescriptor>)receiver index:(int32_t)index __attribute__((swift_name("getTag(_:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Tag _Nullable)popTag __attribute__((swift_name("popTag()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)pushTagName:(Tag _Nullable)name __attribute__((swift_name("pushTag(name:)")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) Tag _Nullable currentTag __attribute__((swift_name("currentTag")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) Tag _Nullable currentTagOrNull __attribute__((swift_name("currentTagOrNull")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface NamedValueDecoder : TaggedDecoder<NSString *>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)composeNameParentName:(NSString *)parentName childName:(NSString *)childName __attribute__((swift_name("composeName(parentName:childName:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)elementNameDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("elementName(descriptor:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)getTag:(id<SerialDescriptor>)receiver index:(int32_t)index __attribute__((swift_name("getTag(_:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)nestedNestedName:(NSString *)nestedName __attribute__((swift_name("nested(nestedName:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface TaggedEncoder<Tag> : Base <Encoder, CompositeEncoder>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<CompositeEncoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (void)encodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (void)encodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<Encoder>)encodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (id<Encoder>)encodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));
- (void)encodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));
- (void)encodeNotNullMark __attribute__((swift_name("encodeNotNullMark()")));
- (void)encodeNull __attribute__((swift_name("encodeNull()")));
- (void)encodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
- (void)encodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedBooleanTag:(Tag _Nullable)tag value:(BOOL)value __attribute__((swift_name("encodeTaggedBoolean(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedByteTag:(Tag _Nullable)tag value:(int8_t)value __attribute__((swift_name("encodeTaggedByte(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedCharTag:(Tag _Nullable)tag value:(unichar)value __attribute__((swift_name("encodeTaggedChar(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedDoubleTag:(Tag _Nullable)tag value:(double)value __attribute__((swift_name("encodeTaggedDouble(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedEnumTag:(Tag _Nullable)tag enumDescriptor:(id<SerialDescriptor>)enumDescriptor ordinal:(int32_t)ordinal __attribute__((swift_name("encodeTaggedEnum(tag:enumDescriptor:ordinal:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedFloatTag:(Tag _Nullable)tag value:(float)value __attribute__((swift_name("encodeTaggedFloat(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id<Encoder>)encodeTaggedInlineTag:(Tag _Nullable)tag inlineDescriptor:(id<SerialDescriptor>)inlineDescriptor __attribute__((swift_name("encodeTaggedInline(tag:inlineDescriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedIntTag:(Tag _Nullable)tag value:(int32_t)value __attribute__((swift_name("encodeTaggedInt(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedLongTag:(Tag _Nullable)tag value:(int64_t)value __attribute__((swift_name("encodeTaggedLong(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedNonNullMarkTag:(Tag _Nullable)tag __attribute__((swift_name("encodeTaggedNonNullMark(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedNullTag:(Tag _Nullable)tag __attribute__((swift_name("encodeTaggedNull(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedShortTag:(Tag _Nullable)tag value:(int16_t)value __attribute__((swift_name("encodeTaggedShort(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedStringTag:(Tag _Nullable)tag value:(NSString *)value __attribute__((swift_name("encodeTaggedString(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedValueTag:(Tag _Nullable)tag value:(id)value __attribute__((swift_name("encodeTaggedValue(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)endEncodeDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endEncode(descriptor:)")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Tag _Nullable)getTag:(id<SerialDescriptor>)receiver index:(int32_t)index __attribute__((swift_name("getTag(_:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Tag _Nullable)popTag __attribute__((swift_name("popTag()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)pushTagName:(Tag _Nullable)name __attribute__((swift_name("pushTag(name:)")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) Tag _Nullable currentTag __attribute__((swift_name("currentTag")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) Tag _Nullable currentTagOrNull __attribute__((swift_name("currentTagOrNull")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface NamedValueEncoder : TaggedEncoder<NSString *>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)composeNameParentName:(NSString *)parentName childName:(NSString *)childName __attribute__((swift_name("composeName(parentName:childName:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)elementNameDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("elementName(descriptor:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)getTag:(id<SerialDescriptor>)receiver index:(int32_t)index __attribute__((swift_name("getTag(_:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)nestedNestedName:(NSString *)nestedName __attribute__((swift_name("nested(nestedName:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface PolymorphicModuleBuilder<__contravariant Base_> : Base
- (void)defaultDefaultSerializerProvider:(id<DeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultSerializerProvider __attribute__((swift_name("default(defaultSerializerProvider:)"))) __attribute__((deprecated("Deprecated in favor of function with more precise name: defaultDeserializer")));
- (void)defaultDeserializerDefaultDeserializerProvider:(id<DeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("defaultDeserializer(defaultDeserializerProvider:)")));
- (void)subclassSubclass:(id<KotlinKClass>)subclass serializer:(id<KSerializer>)serializer __attribute__((swift_name("subclass(subclass:serializer:)")));
@end

@interface SerializersModule : Base

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)dumpToCollector:(id<SerializersModuleCollector>)collector __attribute__((swift_name("dumpTo(collector:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<KSerializer> _Nullable)getContextualKClass:(id<KotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<KSerializer>> *)typeArgumentsSerializers __attribute__((swift_name("getContextual(kClass:typeArgumentsSerializers:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SerializationStrategy> _Nullable)getPolymorphicBaseClass:(id<KotlinKClass>)baseClass value:(id)value __attribute__((swift_name("getPolymorphic(baseClass:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<DeserializationStrategy> _Nullable)getPolymorphicBaseClass:(id<KotlinKClass>)baseClass serializedClassName:(NSString * _Nullable)serializedClassName __attribute__((swift_name("getPolymorphic(baseClass:serializedClassName:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@protocol SerializersModuleCollector
@required
- (void)contextualKClass:(id<KotlinKClass>)kClass provider:(id<KSerializer> (^)(NSArray<id<KSerializer>> *))provider __attribute__((swift_name("contextual(kClass:provider:)")));
- (void)contextualKClass:(id<KotlinKClass>)kClass serializer:(id<KSerializer>)serializer __attribute__((swift_name("contextual(kClass:serializer:)")));
- (void)polymorphicBaseClass:(id<KotlinKClass>)baseClass actualClass:(id<KotlinKClass>)actualClass actualSerializer:(id<KSerializer>)actualSerializer __attribute__((swift_name("polymorphic(baseClass:actualClass:actualSerializer:)")));
- (void)polymorphicDefaultBaseClass:(id<KotlinKClass>)baseClass defaultDeserializerProvider:(id<DeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefault(baseClass:defaultDeserializerProvider:)"))) __attribute__((deprecated("Deprecated in favor of function with more precise name: polymorphicDefaultDeserializer")));
- (void)polymorphicDefaultDeserializerBaseClass:(id<KotlinKClass>)baseClass defaultDeserializerProvider:(id<DeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefaultDeserializer(baseClass:defaultDeserializerProvider:)")));
- (void)polymorphicDefaultSerializerBaseClass:(id<KotlinKClass>)baseClass defaultSerializerProvider:(id<SerializationStrategy> _Nullable (^)(id))defaultSerializerProvider __attribute__((swift_name("polymorphicDefaultSerializer(baseClass:defaultSerializerProvider:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerializersModuleBuilder : Base <SerializersModuleCollector>
- (void)contextualKClass:(id<KotlinKClass>)kClass provider:(id<KSerializer> (^)(NSArray<id<KSerializer>> *))provider __attribute__((swift_name("contextual(kClass:provider:)")));
- (void)contextualKClass:(id<KotlinKClass>)kClass serializer:(id<KSerializer>)serializer __attribute__((swift_name("contextual(kClass:serializer:)")));
- (void)includeModule:(SerializersModule *)module __attribute__((swift_name("include(module:)")));
- (void)polymorphicBaseClass:(id<KotlinKClass>)baseClass actualClass:(id<KotlinKClass>)actualClass actualSerializer:(id<KSerializer>)actualSerializer __attribute__((swift_name("polymorphic(baseClass:actualClass:actualSerializer:)")));
- (void)polymorphicDefaultDeserializerBaseClass:(id<KotlinKClass>)baseClass defaultDeserializerProvider:(id<DeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefaultDeserializer(baseClass:defaultDeserializerProvider:)")));
- (void)polymorphicDefaultSerializerBaseClass:(id<KotlinKClass>)baseClass defaultSerializerProvider:(id<SerializationStrategy> _Nullable (^)(id))defaultSerializerProvider __attribute__((swift_name("polymorphicDefaultSerializer(baseClass:defaultSerializerProvider:)")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinBoolean.Companion")))
@interface KotlinBooleanCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinBooleanCompanion *shared __attribute__((swift_name("shared")));
@end

@interface KotlinBooleanCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinByte.Companion")))
@interface KotlinByteCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinByteCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int8_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) int8_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinByteCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinChar.Companion")))
@interface KotlinCharCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinCharCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
@property (readonly) int32_t MAX_CODE_POINT __attribute__((swift_name("MAX_CODE_POINT")));
@property (readonly) unichar MAX_HIGH_SURROGATE __attribute__((swift_name("MAX_HIGH_SURROGATE")));
@property (readonly) unichar MAX_LOW_SURROGATE __attribute__((swift_name("MAX_LOW_SURROGATE")));

/**
 * @note annotations
 *   kotlin.DeprecatedSinceKotlin(warningSince="1.9")
*/
@property (readonly) int32_t MAX_RADIX __attribute__((swift_name("MAX_RADIX"))) __attribute__((deprecated("Introduce your own constant with the value of `36")));
@property (readonly) unichar MAX_SURROGATE __attribute__((swift_name("MAX_SURROGATE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) unichar MAX_VALUE __attribute__((swift_name("MAX_VALUE")));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
@property (readonly) int32_t MIN_CODE_POINT __attribute__((swift_name("MIN_CODE_POINT")));
@property (readonly) unichar MIN_HIGH_SURROGATE __attribute__((swift_name("MIN_HIGH_SURROGATE")));
@property (readonly) unichar MIN_LOW_SURROGATE __attribute__((swift_name("MIN_LOW_SURROGATE")));

/**
 * @note annotations
 *   kotlin.DeprecatedSinceKotlin(warningSince="1.9")
*/
@property (readonly) int32_t MIN_RADIX __attribute__((swift_name("MIN_RADIX"))) __attribute__((deprecated("Introduce your own constant with the value of `2`")));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
@property (readonly) int32_t MIN_SUPPLEMENTARY_CODE_POINT __attribute__((swift_name("MIN_SUPPLEMENTARY_CODE_POINT")));
@property (readonly) unichar MIN_SURROGATE __attribute__((swift_name("MIN_SURROGATE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) unichar MIN_VALUE __attribute__((swift_name("MIN_VALUE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinCharCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinDouble.Companion")))
@interface KotlinDoubleCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinDoubleCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) double MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) double MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) double NEGATIVE_INFINITY __attribute__((swift_name("NEGATIVE_INFINITY")));
@property (readonly) double NaN __attribute__((swift_name("NaN")));
@property (readonly) double POSITIVE_INFINITY __attribute__((swift_name("POSITIVE_INFINITY")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinDoubleCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinFloat.Companion")))
@interface KotlinFloatCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinFloatCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) float MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) float MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) float NEGATIVE_INFINITY __attribute__((swift_name("NEGATIVE_INFINITY")));
@property (readonly) float NaN __attribute__((swift_name("NaN")));
@property (readonly) float POSITIVE_INFINITY __attribute__((swift_name("POSITIVE_INFINITY")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinFloatCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinInt.Companion")))
@interface KotlinIntCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinIntCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) int32_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinIntCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinLong.Companion")))
@interface KotlinLongCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinLongCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int64_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) int64_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinLongCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinShort.Companion")))
@interface KotlinShortCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinShortCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int16_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) int16_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinShortCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinString.Companion")))
@interface KotlinStringCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinStringCompanion *shared __attribute__((swift_name("shared")));
@end

@interface KotlinStringCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinUByte.Companion")))
@interface KotlinUByteCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinUByteCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) uint8_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) uint8_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinUByteCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinUInt.Companion")))
@interface KotlinUIntCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinUIntCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) uint32_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) uint32_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinUIntCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinULong.Companion")))
@interface KotlinULongCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinULongCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) uint64_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) uint64_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinULongCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinUShort.Companion")))
@interface KotlinUShortCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinUShortCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) uint16_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) uint16_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

@interface KotlinUShortCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinUnit : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unit __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinUnit *shared __attribute__((swift_name("shared")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

@interface KotlinUnit (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinDuration.Companion")))
@interface KotlinDurationCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinDurationCompanion *shared __attribute__((swift_name("shared")));
- (int64_t)days:(double)receiver __attribute__((swift_name("days(_:)")));
- (int64_t)days_:(int32_t)receiver __attribute__((swift_name("days(__:)")));
- (int64_t)days__:(int64_t)receiver __attribute__((swift_name("days(___:)")));
- (int64_t)hours:(double)receiver __attribute__((swift_name("hours(_:)")));
- (int64_t)hours_:(int32_t)receiver __attribute__((swift_name("hours(__:)")));
- (int64_t)hours__:(int64_t)receiver __attribute__((swift_name("hours(___:)")));
- (int64_t)microseconds:(double)receiver __attribute__((swift_name("microseconds(_:)")));
- (int64_t)microseconds_:(int32_t)receiver __attribute__((swift_name("microseconds(__:)")));
- (int64_t)microseconds__:(int64_t)receiver __attribute__((swift_name("microseconds(___:)")));
- (int64_t)milliseconds:(double)receiver __attribute__((swift_name("milliseconds(_:)")));
- (int64_t)milliseconds_:(int32_t)receiver __attribute__((swift_name("milliseconds(__:)")));
- (int64_t)milliseconds__:(int64_t)receiver __attribute__((swift_name("milliseconds(___:)")));
- (int64_t)minutes:(double)receiver __attribute__((swift_name("minutes(_:)")));
- (int64_t)minutes_:(int32_t)receiver __attribute__((swift_name("minutes(__:)")));
- (int64_t)minutes__:(int64_t)receiver __attribute__((swift_name("minutes(___:)")));
- (int64_t)nanoseconds:(double)receiver __attribute__((swift_name("nanoseconds(_:)")));
- (int64_t)nanoseconds_:(int32_t)receiver __attribute__((swift_name("nanoseconds(__:)")));
- (int64_t)nanoseconds__:(int64_t)receiver __attribute__((swift_name("nanoseconds(___:)")));
- (int64_t)seconds:(double)receiver __attribute__((swift_name("seconds(_:)")));
- (int64_t)seconds_:(int32_t)receiver __attribute__((swift_name("seconds(__:)")));
- (int64_t)seconds__:(int64_t)receiver __attribute__((swift_name("seconds(___:)")));

/**
 * @note annotations
 *   kotlin.time.ExperimentalTime
*/
- (double)convertValue:(double)value sourceUnit:(KotlinDurationUnit *)sourceUnit targetUnit:(KotlinDurationUnit *)targetUnit __attribute__((swift_name("convert(value:sourceUnit:targetUnit:)")));
- (int64_t)parseValue:(NSString *)value __attribute__((swift_name("parse(value:)")));
- (int64_t)parseIsoStringValue:(NSString *)value __attribute__((swift_name("parseIsoString(value:)")));
- (id _Nullable)parseIsoStringOrNullValue:(NSString *)value __attribute__((swift_name("parseIsoStringOrNull(value:)")));
- (id _Nullable)parseOrNullValue:(NSString *)value __attribute__((swift_name("parseOrNull(value:)")));
@property (readonly) int64_t INFINITE __attribute__((swift_name("INFINITE")));
@property (readonly) int64_t ZERO __attribute__((swift_name("ZERO")));
@end

@interface KotlinDurationCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface ClassSerialDescriptorBuilder (Extensions)
- (void)elementElementName:(NSString *)elementName annotations:(NSArray<id<KotlinAnnotation>> *)annotations isOptional:(BOOL)isOptional __attribute__((swift_name("element(elementName:annotations:isOptional:)")));
@end

@interface AbstractPolymorphicSerializer (Extensions)

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<DeserializationStrategy>)findPolymorphicSerializerDecoder:(id<CompositeDecoder>)decoder klassName:(NSString * _Nullable)klassName __attribute__((swift_name("findPolymorphicSerializer(decoder:klassName:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<SerializationStrategy>)findPolymorphicSerializerEncoder:(id<Encoder>)encoder value:(id)value __attribute__((swift_name("findPolymorphicSerializer(encoder:value:)")));
@end

@interface PolymorphicModuleBuilder (Extensions)
- (void)subclassClazz:(id<KotlinKClass>)clazz __attribute__((swift_name("subclass(clazz:)")));
- (void)subclassSerializer:(id<KSerializer>)serializer __attribute__((swift_name("subclass(serializer:)")));
@end

@interface SerializersModule (Extensions)

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SerialDescriptor> _Nullable)getContextualDescriptorDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("getContextualDescriptor(descriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (NSArray<id<SerialDescriptor>> *)getPolymorphicDescriptorsDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("getPolymorphicDescriptors(descriptor:)")));
- (SerializersModule *)overwriteWithOther:(SerializersModule *)other __attribute__((swift_name("overwriteWith(other:)")));
- (SerializersModule *)plusOther:(SerializersModule *)other __attribute__((swift_name("plus(other:)")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
- (id<KSerializer>)serializerType:(id<KotlinKType>)type __attribute__((swift_name("serializer(type:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<KSerializer>)serializerKClass:(id<KotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<KSerializer>> *)typeArgumentsSerializers isNullable:(BOOL)isNullable __attribute__((swift_name("serializer(kClass:typeArgumentsSerializers:isNullable:)")));
- (id<KSerializer> _Nullable)serializerOrNullType:(id<KotlinKType>)type __attribute__((swift_name("serializerOrNull(type:)")));
@end

@interface SerializersModuleBuilder (Extensions)
- (void)contextualSerializer:(id<KSerializer>)serializer __attribute__((swift_name("contextual(serializer:)")));
- (void)polymorphicBaseClass:(id<KotlinKClass>)baseClass baseSerializer:(id<KSerializer> _Nullable)baseSerializer builderAction:(void (^)(PolymorphicModuleBuilder<id> *))builderAction __attribute__((swift_name("polymorphic(baseClass:baseSerializer:builderAction:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface BuiltinSerializersKt : Base
+ (id<KSerializer>)nullable:(id<KSerializer>)receiver __attribute__((swift_name("nullable(_:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<KSerializer>)ArraySerializerElementSerializer:(id<KSerializer>)elementSerializer __attribute__((swift_name("ArraySerializer(elementSerializer:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<KSerializer>)ArraySerializerKClass:(id<KotlinKClass>)kClass elementSerializer:(id<KSerializer>)elementSerializer __attribute__((swift_name("ArraySerializer(kClass:elementSerializer:)")));
+ (id<KSerializer>)BooleanArraySerializer __attribute__((swift_name("BooleanArraySerializer()")));
+ (id<KSerializer>)ByteArraySerializer __attribute__((swift_name("ByteArraySerializer()")));
+ (id<KSerializer>)CharArraySerializer __attribute__((swift_name("CharArraySerializer()")));
+ (id<KSerializer>)DoubleArraySerializer __attribute__((swift_name("DoubleArraySerializer()")));
+ (id<KSerializer>)FloatArraySerializer __attribute__((swift_name("FloatArraySerializer()")));
+ (id<KSerializer>)IntArraySerializer __attribute__((swift_name("IntArraySerializer()")));
+ (id<KSerializer>)ListSerializerElementSerializer:(id<KSerializer>)elementSerializer __attribute__((swift_name("ListSerializer(elementSerializer:)")));
+ (id<KSerializer>)LongArraySerializer __attribute__((swift_name("LongArraySerializer()")));
+ (id<KSerializer>)MapEntrySerializerKeySerializer:(id<KSerializer>)keySerializer valueSerializer:(id<KSerializer>)valueSerializer __attribute__((swift_name("MapEntrySerializer(keySerializer:valueSerializer:)")));
+ (id<KSerializer>)MapSerializerKeySerializer:(id<KSerializer>)keySerializer valueSerializer:(id<KSerializer>)valueSerializer __attribute__((swift_name("MapSerializer(keySerializer:valueSerializer:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<KSerializer>)NothingSerializer __attribute__((swift_name("NothingSerializer()")));
+ (id<KSerializer>)PairSerializerKeySerializer:(id<KSerializer>)keySerializer valueSerializer:(id<KSerializer>)valueSerializer __attribute__((swift_name("PairSerializer(keySerializer:valueSerializer:)")));
+ (id<KSerializer>)SetSerializerElementSerializer:(id<KSerializer>)elementSerializer __attribute__((swift_name("SetSerializer(elementSerializer:)")));
+ (id<KSerializer>)ShortArraySerializer __attribute__((swift_name("ShortArraySerializer()")));
+ (id<KSerializer>)TripleSerializerASerializer:(id<KSerializer>)aSerializer bSerializer:(id<KSerializer>)bSerializer cSerializer:(id<KSerializer>)cSerializer __attribute__((swift_name("TripleSerializer(aSerializer:bSerializer:cSerializer:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
 *   kotlin.ExperimentalUnsignedTypes
*/
+ (id<KSerializer>)UByteArraySerializer __attribute__((swift_name("UByteArraySerializer()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
 *   kotlin.ExperimentalUnsignedTypes
*/
+ (id<KSerializer>)UIntArraySerializer __attribute__((swift_name("UIntArraySerializer()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
 *   kotlin.ExperimentalUnsignedTypes
*/
+ (id<KSerializer>)ULongArraySerializer __attribute__((swift_name("ULongArraySerializer()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
 *   kotlin.ExperimentalUnsignedTypes
*/
+ (id<KSerializer>)UShortArraySerializer __attribute__((swift_name("UShortArraySerializer()")));
@end

__attribute__((objc_subclassing_restricted))
@interface ContextAwareKt : Base
+ (id<KotlinKClass> _Nullable)capturedKClass:(id<SerialDescriptor>)receiver __attribute__((swift_name("capturedKClass(_:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DecodingKt : Base
+ (id _Nullable)decodeStructure:(id<Decoder>)receiver descriptor:(id<SerialDescriptor>)descriptor block:(id _Nullable (^)(id<CompositeDecoder>))block __attribute__((swift_name("decodeStructure(_:descriptor:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface EncodingKt : Base
+ (void)encodeCollection:(id<Encoder>)receiver descriptor:(id<SerialDescriptor>)descriptor collectionSize:(int32_t)collectionSize block:(void (^)(id<CompositeEncoder>))block __attribute__((swift_name("encodeCollection(_:descriptor:collectionSize:block:)")));
+ (void)encodeCollection:(id<Encoder>)receiver descriptor:(id<SerialDescriptor>)descriptor collection:(id)collection block:(void (^)(id<CompositeEncoder>, Int *, id _Nullable))block __attribute__((swift_name("encodeCollection(_:descriptor:collection:block:)")));
+ (void)encodeStructure:(id<Encoder>)receiver descriptor:(id<SerialDescriptor>)descriptor block:(void (^)(id<CompositeEncoder>))block __attribute__((swift_name("encodeStructure(_:descriptor:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface InlineClassDescriptorKt : Base

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (id<SerialDescriptor>)InlinePrimitiveDescriptorName:(NSString *)name primitiveSerializer:(id<KSerializer>)primitiveSerializer __attribute__((swift_name("InlinePrimitiveDescriptor(name:primitiveSerializer:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface JsonInternalDependenciesKt : Base
+ (NSSet<NSString *> *)jsonCachedSerialNames:(id<SerialDescriptor>)receiver __attribute__((swift_name("jsonCachedSerialNames(_:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface PluginExceptionsKt : Base

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (void)throwArrayMissingFieldExceptionSeenArray:(KotlinIntArray *)seenArray goldenMaskArray:(KotlinIntArray *)goldenMaskArray descriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("throwArrayMissingFieldException(seenArray:goldenMaskArray:descriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (void)throwMissingFieldExceptionSeen:(int32_t)seen goldenMask:(int32_t)goldenMask descriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("throwMissingFieldException(seen:goldenMask:descriptor:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerialDescriptorKt : Base
+ (id)elementDescriptors:(id<SerialDescriptor>)receiver __attribute__((swift_name("elementDescriptors(_:)")));
+ (id)elementNames:(id<SerialDescriptor>)receiver __attribute__((swift_name("elementNames(_:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerialDescriptorsKt : Base
+ (id<SerialDescriptor>)nullable:(id<SerialDescriptor>)receiver __attribute__((swift_name("nullable(_:)")));
+ (id<SerialDescriptor>)PrimitiveSerialDescriptorSerialName:(NSString *)serialName kind:(PrimitiveKind *)kind __attribute__((swift_name("PrimitiveSerialDescriptor(serialName:kind:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)SerialDescriptorSerialName:(NSString *)serialName original:(id<SerialDescriptor>)original __attribute__((swift_name("SerialDescriptor(serialName:original:)")));
+ (id<SerialDescriptor>)buildClassSerialDescriptorSerialName:(NSString *)serialName typeParameters:(KotlinArray<id<SerialDescriptor>> *)typeParameters builderAction:(void (^)(ClassSerialDescriptorBuilder *))builderAction __attribute__((swift_name("buildClassSerialDescriptor(serialName:typeParameters:builderAction:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (id<SerialDescriptor>)buildSerialDescriptorSerialName:(NSString *)serialName kind:(SerialKind *)kind typeParameters:(KotlinArray<id<SerialDescriptor>> *)typeParameters builder:(void (^)(ClassSerialDescriptorBuilder *))builder __attribute__((swift_name("buildSerialDescriptor(serialName:kind:typeParameters:builder:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)listSerialDescriptor __attribute__((swift_name("listSerialDescriptor()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)listSerialDescriptorElementDescriptor:(id<SerialDescriptor>)elementDescriptor __attribute__((swift_name("listSerialDescriptor(elementDescriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)mapSerialDescriptor __attribute__((swift_name("mapSerialDescriptor()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)mapSerialDescriptorKeyDescriptor:(id<SerialDescriptor>)keyDescriptor valueDescriptor:(id<SerialDescriptor>)valueDescriptor __attribute__((swift_name("mapSerialDescriptor(keyDescriptor:valueDescriptor:)")));
+ (id<SerialDescriptor>)serialDescriptor __attribute__((swift_name("serialDescriptor()")));
+ (id<SerialDescriptor>)serialDescriptorType:(id<KotlinKType>)type __attribute__((swift_name("serialDescriptor(type:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)setSerialDescriptor __attribute__((swift_name("setSerialDescriptor()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)setSerialDescriptorElementDescriptor:(id<SerialDescriptor>)elementDescriptor __attribute__((swift_name("setSerialDescriptor(elementDescriptor:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerialFormatKt : Base
+ (id _Nullable)decodeFromByteArray:(id<BinaryFormat>)receiver bytes:(KotlinByteArray *)bytes __attribute__((swift_name("decodeFromByteArray(_:bytes:)")));
+ (id _Nullable)decodeFromHexString:(id<BinaryFormat>)receiver hex:(NSString *)hex __attribute__((swift_name("decodeFromHexString(_:hex:)")));
+ (id _Nullable)decodeFromHexString:(id<BinaryFormat>)receiver deserializer:(id<DeserializationStrategy>)deserializer hex:(NSString *)hex __attribute__((swift_name("decodeFromHexString(_:deserializer:hex:)")));
+ (id _Nullable)decodeFromString:(id<StringFormat>)receiver string:(NSString *)string __attribute__((swift_name("decodeFromString(_:string:)")));
+ (KotlinByteArray *)encodeToByteArray:(id<BinaryFormat>)receiver value:(id _Nullable)value __attribute__((swift_name("encodeToByteArray(_:value:)")));
+ (NSString *)encodeToHexString:(id<BinaryFormat>)receiver value:(id _Nullable)value __attribute__((swift_name("encodeToHexString(_:value:)")));
+ (NSString *)encodeToHexString:(id<BinaryFormat>)receiver serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeToHexString(_:serializer:value:)")));
+ (NSString *)encodeToString:(id<StringFormat>)receiver value:(id _Nullable)value __attribute__((swift_name("encodeToString(_:value:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerializersKt : Base
+ (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (id<KSerializer>)serializer:(id<KotlinKClass>)receiver __attribute__((swift_name("serializer(_:)")));
+ (id<KSerializer>)serializerType:(id<KotlinKType>)type __attribute__((swift_name("serializer(type:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<KSerializer>)serializerKClass:(id<KotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<KSerializer>> *)typeArgumentsSerializers isNullable:(BOOL)isNullable __attribute__((swift_name("serializer(kClass:typeArgumentsSerializers:isNullable:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (id<KSerializer> _Nullable)serializerOrNull:(id<KotlinKClass>)receiver __attribute__((swift_name("serializerOrNull(_:)")));
+ (id<KSerializer> _Nullable)serializerOrNullType:(id<KotlinKType>)type __attribute__((swift_name("serializerOrNull(type:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerializersModuleKt : Base
@property (class, readonly) SerializersModule *EmptySerializersModule __attribute__((swift_name("EmptySerializersModule"))) __attribute__((deprecated("Deprecated in the favour of 'EmptySerializersModule()'")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerializersModuleBuildersKt : Base
+ (SerializersModule *)EmptySerializersModule __attribute__((swift_name("EmptySerializersModule()")));
+ (SerializersModule *)SerializersModuleBuilderAction:(void (^)(SerializersModuleBuilder *))builderAction __attribute__((swift_name("SerializersModule(builderAction:)")));
+ (SerializersModule *)serializersModuleOfSerializer:(id<KSerializer>)serializer __attribute__((swift_name("serializersModuleOf(serializer:)")));
+ (SerializersModule *)serializersModuleOfKClass:(id<KotlinKClass>)kClass serializer:(id<KSerializer>)serializer __attribute__((swift_name("serializersModuleOf(kClass:serializer:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinByteArray : Base
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(Byte *(^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int8_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (KotlinByteIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int8_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

@protocol KotlinKDeclarationContainer
@required
@end

@protocol KotlinKAnnotatedElement
@required
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
@protocol KotlinKClassifier
@required
@end

@protocol KotlinKClass <KotlinKDeclarationContainer, KotlinKAnnotatedElement, KotlinKClassifier>
@required

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
- (BOOL)isInstanceValue:(id _Nullable)value __attribute__((swift_name("isInstance(value:)")));
@property (readonly) NSString * _Nullable qualifiedName __attribute__((swift_name("qualifiedName")));
@property (readonly) NSString * _Nullable simpleName __attribute__((swift_name("simpleName")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinArray<T> : Base
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<KotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

@protocol KotlinAnnotation
@required
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinNothing : Base
@end

@protocol KotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

@protocol KotlinMapEntry
@required
@property (readonly) id _Nullable key __attribute__((swift_name("key")));
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end

@protocol KotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

@interface KotlinEnum<E> : Base <KotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) KotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.6")
*/
__attribute__((objc_subclassing_restricted))
@interface KotlinDurationUnit : KotlinEnum<KotlinDurationUnit *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KotlinDurationUnit *nanoseconds __attribute__((swift_name("nanoseconds")));
@property (class, readonly) KotlinDurationUnit *microseconds __attribute__((swift_name("microseconds")));
@property (class, readonly) KotlinDurationUnit *milliseconds __attribute__((swift_name("milliseconds")));
@property (class, readonly) KotlinDurationUnit *seconds __attribute__((swift_name("seconds")));
@property (class, readonly) KotlinDurationUnit *minutes __attribute__((swift_name("minutes")));
@property (class, readonly) KotlinDurationUnit *hours __attribute__((swift_name("hours")));
@property (class, readonly) KotlinDurationUnit *days __attribute__((swift_name("days")));
+ (KotlinArray<KotlinDurationUnit *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KotlinDurationUnit *> *entries __attribute__((swift_name("entries")));
@end

@protocol KotlinKType
@required

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
@property (readonly) NSArray<KotlinKTypeProjection *> *arguments __attribute__((swift_name("arguments")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
@property (readonly) id<KotlinKClassifier> _Nullable classifier __attribute__((swift_name("classifier")));
@property (readonly) BOOL isMarkedNullable __attribute__((swift_name("isMarkedNullable")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinIntArray : Base
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(Int *(^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int32_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (KotlinIntIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int32_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

@interface KotlinByteIterator : Base <KotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (Byte *)next __attribute__((swift_name("next()")));
- (int8_t)nextByte __attribute__((swift_name("nextByte()")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinEnumCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((objc_subclassing_restricted))
@interface KotlinKTypeProjection : Base
- (instancetype)initWithVariance:(KotlinKVariance * _Nullable)variance type:(id<KotlinKType> _Nullable)type __attribute__((swift_name("init(variance:type:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) KotlinKTypeProjectionCompanion *companion __attribute__((swift_name("companion")));
- (KotlinKTypeProjection *)doCopyVariance:(KotlinKVariance * _Nullable)variance type:(id<KotlinKType> _Nullable)type __attribute__((swift_name("doCopy(variance:type:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<KotlinKType> _Nullable type __attribute__((swift_name("type")));
@property (readonly) KotlinKVariance * _Nullable variance __attribute__((swift_name("variance")));
@end

@interface KotlinIntIterator : Base <KotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (Int *)next __attribute__((swift_name("next()")));
- (int32_t)nextInt __attribute__((swift_name("nextInt()")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((objc_subclassing_restricted))
@interface KotlinKVariance : KotlinEnum<KotlinKVariance *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KotlinKVariance *invariant __attribute__((swift_name("invariant")));
@property (class, readonly) KotlinKVariance *in __attribute__((swift_name("in")));
@property (class, readonly) KotlinKVariance *out __attribute__((swift_name("out")));
+ (KotlinArray<KotlinKVariance *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KotlinKVariance *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinKTypeProjection.Companion")))
@interface KotlinKTypeProjectionCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinKTypeProjectionCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (KotlinKTypeProjection *)contravariantType:(id<KotlinKType>)type __attribute__((swift_name("contravariant(type:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (KotlinKTypeProjection *)covariantType:(id<KotlinKType>)type __attribute__((swift_name("covariant(type:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (KotlinKTypeProjection *)invariantType:(id<KotlinKType>)type __attribute__((swift_name("invariant(type:)")));
@property (readonly) KotlinKTypeProjection *STAR __attribute__((swift_name("STAR")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
