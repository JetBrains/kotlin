#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class ClassDiscriminatorMode, DecodeSequenceMode, Decoder, DeserializationStrategy<T0>, Encoder, Json, JsonArrayBuilder, JsonBuilder, JsonConfiguration, JsonDefault, JsonElement, JsonElementCompanion, JsonNamingStrategyBuiltins, JsonNull, JsonObjectBuilder, JsonPrimitive, JsonPrimitiveCompanion, KSerializer<T0>, KotlinArray<T>, KotlinCharArray, KotlinCharIterator, KotlinEnum<E>, KotlinEnumCompanion, KotlinNothing, SerialDescriptor, SerializationStrategy<T0>, SerializersModule, StringFormat;

@protocol InternalJsonReader, InternalJsonWriter, JsonNamingStrategy, KotlinComparable, KotlinIterator, KotlinKAnnotatedElement, KotlinKClass, KotlinKClassifier, KotlinKDeclarationContainer, KotlinSequence;

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

@protocol KotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

@interface KotlinEnum<E> : Base <KotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) KotlinEnumCompanion *companion;
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash;
- (NSString *)description;
@property (readonly) NSString *name;
@property (readonly) int32_t ordinal;
@end

__attribute__((objc_subclassing_restricted))
@interface ClassDiscriminatorMode : KotlinEnum<ClassDiscriminatorMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) ClassDiscriminatorMode *none;
@property (class, readonly) ClassDiscriminatorMode *allJsonObjects;
@property (class, readonly) ClassDiscriminatorMode *polymorphic;
+ (KotlinArray<ClassDiscriminatorMode *> *)values;
@property (class, readonly) NSArray<ClassDiscriminatorMode *> *entries;
@end

__attribute__((objc_subclassing_restricted))
@interface DecodeSequenceMode : KotlinEnum<DecodeSequenceMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) DecodeSequenceMode *whitespaceSeparated;
@property (class, readonly) DecodeSequenceMode *arrayWrapped;
@property (class, readonly) DecodeSequenceMode *autoDetect;
+ (KotlinArray<DecodeSequenceMode *> *)values;
@property (class, readonly) NSArray<DecodeSequenceMode *> *entries;
@end

__attribute__((objc_subclassing_restricted))
@interface StringFormat : Base
@end

@interface Json : StringFormat
@property (class, readonly, getter=companion) JsonDefault *companion;
- (id _Nullable)decodeFromJsonElementDeserializer:(DeserializationStrategy<id> *)deserializer element:(JsonElement *)element __attribute__((swift_name("decodeFromJsonElement(deserializer:element:)")));
- (id _Nullable)decodeFromStringString:(NSString *)string __attribute__((swift_name("decodeFromString(string:)")));
- (id _Nullable)decodeFromStringDeserializer:(DeserializationStrategy<id> *)deserializer string:(NSString *)string __attribute__((swift_name("decodeFromString(deserializer:string:)")));
- (JsonElement *)encodeToJsonElementSerializer:(SerializationStrategy<id> *)serializer value:(id _Nullable)value __attribute__((swift_name("encodeToJsonElement(serializer:value:)")));
- (NSString *)encodeToStringSerializer:(SerializationStrategy<id> *)serializer value:(id _Nullable)value __attribute__((swift_name("encodeToString(serializer:value:)")));
- (JsonElement *)parseToJsonElementString:(NSString *)string __attribute__((swift_name("parseToJsonElement(string:)")));
@property (readonly) JsonConfiguration *configuration;
@property (readonly) SerializersModule *serializersModule;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Json.Default")))
@interface JsonDefault : Json
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)default_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) JsonDefault *shared;
@end

__attribute__((unavailable("can't be imported")))
@interface JsonArray : NSObject
@end

__attribute__((objc_subclassing_restricted))
@interface JsonArrayBuilder : Base
- (BOOL)addElement:(JsonElement *)element __attribute__((swift_name("add(element:)")));
- (BOOL)addAllElements:(id)elements __attribute__((swift_name("addAll(elements:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface JsonBuilder : Base
@property BOOL allowSpecialFloatingPointValues;
@property BOOL allowStructuredMapKeys;
@property BOOL allowTrailingComma;
@property NSString *classDiscriminator;
@property ClassDiscriminatorMode *classDiscriminatorMode;
@property BOOL coerceInputValues;
@property BOOL decodeEnumsCaseInsensitive;
@property BOOL encodeDefaults;
@property BOOL explicitNulls;
@property BOOL ignoreUnknownKeys;
@property BOOL isLenient;
@property id<JsonNamingStrategy> _Nullable namingStrategy;
@property BOOL prettyPrint;
@property NSString *prettyPrintIndent;
@property SerializersModule *serializersModule;
@property BOOL useAlternativeNames;
@property BOOL useArrayPolymorphism;
@end

__attribute__((objc_subclassing_restricted))
@interface JsonConfiguration : Base
- (NSString *)description;
@property (readonly) BOOL allowSpecialFloatingPointValues;
@property (readonly) BOOL allowStructuredMapKeys;
@property (readonly) BOOL allowTrailingComma;
@property (readonly) NSString *classDiscriminator;
@property ClassDiscriminatorMode *classDiscriminatorMode;
@property (readonly) BOOL coerceInputValues;
@property (readonly) BOOL decodeEnumsCaseInsensitive;
@property (readonly) BOOL encodeDefaults;
@property (readonly) BOOL explicitNulls;
@property (readonly) BOOL ignoreUnknownKeys;
@property (readonly) BOOL isLenient;
@property (readonly) id<JsonNamingStrategy> _Nullable namingStrategy;
@property (readonly) BOOL prettyPrint;
@property (readonly) NSString *prettyPrintIndent;
@property (readonly) BOOL useAlternativeNames;
@property (readonly) BOOL useArrayPolymorphism;
@end

__attribute__((objc_subclassing_restricted))
@interface KSerializer<T0> : Base
@end

@interface JsonContentPolymorphicSerializer<T> : KSerializer<T>
- (instancetype)initWithBaseClass:(id<KotlinKClass>)baseClass __attribute__((swift_name("init(baseClass:)"))) __attribute__((objc_designated_initializer));
- (T)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (DeserializationStrategy<T> *)selectDeserializerElement:(JsonElement *)element __attribute__((swift_name("selectDeserializer(element:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(T)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor;
@end

@protocol JsonDecoder
@required
- (JsonElement *)decodeJsonElement;
@property (readonly) Json *json;
@end

@interface JsonElement : Base
@property (class, readonly, getter=companion) JsonElementCompanion *companion;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("JsonElement.Companion")))
@interface JsonElementCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) JsonElementCompanion *shared;
- (KSerializer<JsonElement *> *)serializer;
@end

@protocol JsonEncoder
@required
- (void)encodeJsonElementElement:(JsonElement *)element __attribute__((swift_name("encodeJsonElement(element:)")));
@property (readonly) Json *json;
@end

@protocol JsonNamingStrategy
@required
- (NSString *)serialNameForJsonDescriptor:(SerialDescriptor *)descriptor elementIndex:(int32_t)elementIndex serialName:(NSString *)serialName __attribute__((swift_name("serialNameForJson(descriptor:elementIndex:serialName:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface JsonNamingStrategyBuiltins : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)builtins __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) JsonNamingStrategyBuiltins *shared;
@property (readonly) id<JsonNamingStrategy> KebabCase __attribute__((swift_name("KebabCase")));
@property (readonly) id<JsonNamingStrategy> SnakeCase __attribute__((swift_name("SnakeCase")));
@end

@interface JsonPrimitive : JsonElement
@property (class, readonly, getter=companion) JsonPrimitiveCompanion *companion;
- (NSString *)description;
@property (readonly) NSString *content;
@property (readonly) BOOL isString;
@end

__attribute__((objc_subclassing_restricted))
@interface JsonNull : JsonPrimitive
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)jsonNull __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) JsonNull *shared;
- (KSerializer<JsonNull *> *)serializer;
- (KSerializer<id> *)serializerTypeParamsSerializers:(KotlinArray<KSerializer<id> *> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@property (readonly) NSString *content;
@property (readonly) BOOL isString;
@end

__attribute__((unavailable("can't be imported")))
@interface JsonObject : NSObject
@end

__attribute__((objc_subclassing_restricted))
@interface JsonObjectBuilder : Base
- (JsonElement * _Nullable)putKey:(NSString *)key element:(JsonElement *)element __attribute__((swift_name("put(key:element:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("JsonPrimitive.Companion")))
@interface JsonPrimitiveCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) JsonPrimitiveCompanion *shared;
- (KSerializer<JsonPrimitive *> *)serializer;
@end

@interface JsonTransformingSerializer<T> : KSerializer<T>
- (instancetype)initWithTSerializer:(KSerializer<T> *)tSerializer __attribute__((swift_name("init(tSerializer:)"))) __attribute__((objc_designated_initializer));
- (T)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(T)value __attribute__((swift_name("serialize(encoder:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (JsonElement *)transformDeserializeElement:(JsonElement *)element __attribute__((swift_name("transformDeserialize(element:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (JsonElement *)transformSerializeElement:(JsonElement *)element __attribute__((swift_name("transformSerialize(element:)")));
@property (readonly) SerialDescriptor *descriptor;
@end

@protocol InternalJsonReader
@required
- (int32_t)readBuffer:(KotlinCharArray *)buffer bufferOffset:(int32_t)bufferOffset count:(int32_t)count __attribute__((swift_name("read(buffer:bufferOffset:count:)")));
@end

@protocol InternalJsonWriter
@required
- (void)release_ __attribute__((swift_name("release()")));
- (void)writeText:(NSString *)text __attribute__((swift_name("write(text:)")));
- (void)writeCharChar:(unichar)char_ __attribute__((swift_name("writeChar(char:)")));
- (void)writeLongValue:(int64_t)value __attribute__((swift_name("writeLong(value:)")));
- (void)writeQuotedText:(NSString *)text __attribute__((swift_name("writeQuoted(text:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface JsonKt : Base
+ (Json *)JsonFrom:(Json *)from builderAction:(void (^)(JsonBuilder *))builderAction __attribute__((swift_name("Json(from:builderAction:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface JsonElementKt : Base
+ (JsonPrimitive *)JsonPrimitiveValue:(Boolean * _Nullable)value __attribute__((swift_name("JsonPrimitive(value:)")));
+ (JsonNull *)JsonPrimitiveValue_:(KotlinNothing * _Nullable)value __attribute__((swift_name("JsonPrimitive(value_:)")));
+ (JsonPrimitive *)JsonPrimitiveValue__:(id _Nullable)value __attribute__((swift_name("JsonPrimitive(value__:)")));
+ (JsonPrimitive *)JsonPrimitiveValue___:(NSString * _Nullable)value __attribute__((swift_name("JsonPrimitive(value___:)")));
+ (JsonPrimitive *)JsonPrimitiveValue____:(uint8_t)value __attribute__((swift_name("JsonPrimitive(value____:)")));
+ (JsonPrimitive *)JsonPrimitiveValue_____:(uint32_t)value __attribute__((swift_name("JsonPrimitive(value_____:)")));
+ (JsonPrimitive *)JsonPrimitiveValue______:(uint64_t)value __attribute__((swift_name("JsonPrimitive(value______:)")));
+ (JsonPrimitive *)JsonPrimitiveValue_______:(uint16_t)value __attribute__((swift_name("JsonPrimitive(value_______:)")));
+ (JsonPrimitive *)JsonUnquotedLiteralValue:(NSString * _Nullable)value __attribute__((swift_name("JsonUnquotedLiteral(value:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface JsonElementBuildersKt : Base
+ (NSArray<JsonElement *> *)buildJsonArrayBuilderAction:(void (^)(JsonArrayBuilder *))builderAction __attribute__((swift_name("buildJsonArray(builderAction:)")));
+ (NSDictionary<NSString *, JsonElement *> *)buildJsonObjectBuilderAction:(void (^)(JsonObjectBuilder *))builderAction __attribute__((swift_name("buildJsonObject(builderAction:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface JsonStreamsKt : Base
+ (id _Nullable)decodeByReaderJson:(Json *)json deserializer:(DeserializationStrategy<id> *)deserializer reader:(id<InternalJsonReader>)reader __attribute__((swift_name("decodeByReader(json:deserializer:reader:)")));
+ (id<KotlinSequence>)decodeToSequenceByReaderJson:(Json *)json reader:(id<InternalJsonReader>)reader format:(DecodeSequenceMode *)format __attribute__((swift_name("decodeToSequenceByReader(json:reader:format:)")));
+ (id<KotlinSequence>)decodeToSequenceByReaderJson:(Json *)json reader:(id<InternalJsonReader>)reader deserializer:(DeserializationStrategy<id> *)deserializer format:(DecodeSequenceMode *)format __attribute__((swift_name("decodeToSequenceByReader(json:reader:deserializer:format:)")));
+ (void)encodeByWriterJson:(Json *)json writer:(id<InternalJsonWriter>)writer serializer:(SerializationStrategy<id> *)serializer value:(id _Nullable)value __attribute__((swift_name("encodeByWriter(json:writer:serializer:value:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface StreamingJsonDecoderKt : Base
+ (JsonElement *)decodeStringToJsonTreeJson:(Json *)json deserializer:(DeserializationStrategy<id> *)deserializer source:(NSString *)source __attribute__((swift_name("decodeStringToJsonTree(json:deserializer:source:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface TreeJsonDecoderKt : Base
+ (id _Nullable)readJsonJson:(Json *)json element:(JsonElement *)element deserializer:(DeserializationStrategy<id> *)deserializer __attribute__((swift_name("readJson(json:element:deserializer:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface TreeJsonEncoderKt : Base
+ (JsonElement *)writeJsonJson:(Json *)json value:(id _Nullable)value serializer:(SerializationStrategy<id> *)serializer __attribute__((swift_name("writeJson(json:value:serializer:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinEnumCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinEnumCompanion *shared;
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinArray<T> : Base
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<KotlinIterator>)iterator;
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size;
@end

__attribute__((objc_subclassing_restricted))
@interface DeserializationStrategy<T0> : Base
@end

__attribute__((objc_subclassing_restricted))
@interface SerializationStrategy<T0> : Base
@end

__attribute__((objc_subclassing_restricted))
@interface SerializersModule : Base
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
@property (readonly) NSString * _Nullable qualifiedName;
@property (readonly) NSString * _Nullable simpleName;
@end

__attribute__((objc_subclassing_restricted))
@interface Decoder : Base
@end

__attribute__((objc_subclassing_restricted))
@interface Encoder : Base
@end

__attribute__((objc_subclassing_restricted))
@interface SerialDescriptor : Base
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinCharArray : Base
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(id (^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (unichar)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (KotlinCharIterator *)iterator;
- (void)setIndex:(int32_t)index value:(unichar)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size;
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinNothing : Base
@end

@protocol KotlinSequence
@required
- (id<KotlinIterator>)iterator;
@end

@protocol KotlinIterator
@required
- (BOOL)hasNext;
- (id _Nullable)next;
@end

@interface KotlinCharIterator : Base <KotlinIterator>
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id)next;
- (unichar)nextChar;
@end

@interface Json (Extensions)
- (id _Nullable)decodeFromJsonElementJson:(JsonElement *)json __attribute__((swift_name("decodeFromJsonElement(json:)")));
- (JsonElement *)encodeToJsonElementValue:(id _Nullable)value __attribute__((swift_name("encodeToJsonElement(value:)")));
@end

@interface JsonArrayBuilder (Extensions)
- (BOOL)addValue:(Boolean * _Nullable)value __attribute__((swift_name("add(value:)")));
- (BOOL)addValue_:(KotlinNothing * _Nullable)value __attribute__((swift_name("add(value_:)")));
- (BOOL)addValue__:(id _Nullable)value __attribute__((swift_name("add(value__:)")));
- (BOOL)addValue___:(NSString * _Nullable)value __attribute__((swift_name("add(value___:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmName(name="addAllBooleans")
*/
- (BOOL)addAllValues:(id)values __attribute__((swift_name("addAll(values:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmName(name="addAllNumbers")
*/
- (BOOL)addAllValues_:(id)values __attribute__((swift_name("addAll(values_:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmName(name="addAllStrings")
*/
- (BOOL)addAllValues__:(id)values __attribute__((swift_name("addAll(values__:)")));
- (BOOL)addJsonArrayBuilderAction:(void (^)(JsonArrayBuilder *))builderAction __attribute__((swift_name("addJsonArray(builderAction:)")));
- (BOOL)addJsonObjectBuilderAction:(void (^)(JsonObjectBuilder *))builderAction __attribute__((swift_name("addJsonObject(builderAction:)")));
@end

@interface JsonElement (Extensions)
@property (readonly) NSArray<JsonElement *> *jsonArray;
@property (readonly) JsonNull *jsonNull;
@property (readonly) NSDictionary<NSString *, JsonElement *> *jsonObject;
@property (readonly) JsonPrimitive *jsonPrimitive;
@end

@interface JsonObjectBuilder (Extensions)
- (JsonElement * _Nullable)putKey:(NSString *)key value:(Boolean * _Nullable)value __attribute__((swift_name("put(key:value:)")));
- (JsonElement * _Nullable)putKey:(NSString *)key value_:(KotlinNothing * _Nullable)value __attribute__((swift_name("put(key:value_:)")));
- (JsonElement * _Nullable)putKey:(NSString *)key value__:(id _Nullable)value __attribute__((swift_name("put(key:value__:)")));
- (JsonElement * _Nullable)putKey:(NSString *)key value___:(NSString * _Nullable)value __attribute__((swift_name("put(key:value___:)")));
- (JsonElement * _Nullable)putJsonArrayKey:(NSString *)key builderAction:(void (^)(JsonArrayBuilder *))builderAction __attribute__((swift_name("putJsonArray(key:builderAction:)")));
- (JsonElement * _Nullable)putJsonObjectKey:(NSString *)key builderAction:(void (^)(JsonObjectBuilder *))builderAction __attribute__((swift_name("putJsonObject(key:builderAction:)")));
@end

@interface JsonPrimitive (Extensions)
@property (readonly) BOOL boolean;
@property (readonly) Boolean * _Nullable booleanOrNull;
@property (readonly) NSString * _Nullable contentOrNull;
@property (readonly, getter=double) double double_;
@property (readonly) Double * _Nullable doubleOrNull;
@property (readonly, getter=float) float float_;
@property (readonly) Float * _Nullable floatOrNull;
@property (readonly, getter=int) int32_t int_;
@property (readonly) Int * _Nullable intOrNull;
@property (readonly, getter=long) int64_t long_;
@property (readonly) Long * _Nullable longOrNull;
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
