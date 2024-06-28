#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class KotlinEnumCompanion, KotlinEnum<E>, ClassDiscriminatorMode, KotlinArray<T>, DecodeSequenceMode, StringFormat, JsonDefault, DeserializationStrategy<T0>, JsonElement, SerializationStrategy<T0>, JsonConfiguration, SerializersModule, Json, KSerializer<T0>, Decoder, Encoder, SerialDescriptor, JsonElementCompanion, JsonNamingStrategyBuiltins, JsonPrimitiveCompanion, JsonPrimitive, JsonNull, KotlinCharArray, JsonArrayBuilder, KotlinNothing, JsonObjectBuilder, JsonBuilder, KotlinCharIterator;

@protocol KotlinComparable, JsonNamingStrategy, KotlinKClass, InternalJsonReader, KotlinSequence, InternalJsonWriter, KotlinIterator, KotlinKDeclarationContainer, KotlinKAnnotatedElement, KotlinKClassifier;

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
@property (class, readonly, getter=companion) KotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
@interface ClassDiscriminatorMode : KotlinEnum<ClassDiscriminatorMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) ClassDiscriminatorMode *none __attribute__((swift_name("none")));
@property (class, readonly) ClassDiscriminatorMode *allJsonObjects __attribute__((swift_name("allJsonObjects")));
@property (class, readonly) ClassDiscriminatorMode *polymorphic __attribute__((swift_name("polymorphic")));
+ (KotlinArray<ClassDiscriminatorMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<ClassDiscriminatorMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
@interface DecodeSequenceMode : KotlinEnum<DecodeSequenceMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) DecodeSequenceMode *whitespaceSeparated __attribute__((swift_name("whitespaceSeparated")));
@property (class, readonly) DecodeSequenceMode *arrayWrapped __attribute__((swift_name("arrayWrapped")));
@property (class, readonly) DecodeSequenceMode *autoDetect __attribute__((swift_name("autoDetect")));
+ (KotlinArray<DecodeSequenceMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<DecodeSequenceMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
@interface StringFormat : Base
@end

@interface Json : StringFormat
@property (class, readonly, getter=companion) JsonDefault *companion __attribute__((swift_name("companion")));
- (id _Nullable)decodeFromJsonElementDeserializer:(DeserializationStrategy<id> *)deserializer element:(JsonElement *)element __attribute__((swift_name("decodeFromJsonElement(deserializer:element:)")));
- (id _Nullable)decodeFromStringString:(NSString *)string __attribute__((swift_name("decodeFromString(string:)")));
- (id _Nullable)decodeFromStringDeserializer:(DeserializationStrategy<id> *)deserializer string:(NSString *)string __attribute__((swift_name("decodeFromString(deserializer:string:)")));
- (JsonElement *)encodeToJsonElementSerializer:(SerializationStrategy<id> *)serializer value:(id _Nullable)value __attribute__((swift_name("encodeToJsonElement(serializer:value:)")));
- (NSString *)encodeToStringSerializer:(SerializationStrategy<id> *)serializer value:(id _Nullable)value __attribute__((swift_name("encodeToString(serializer:value:)")));
- (JsonElement *)parseToJsonElementString:(NSString *)string __attribute__((swift_name("parseToJsonElement(string:)")));
@property (readonly) JsonConfiguration *configuration __attribute__((swift_name("configuration")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Json.Default")))
@interface JsonDefault : Json
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)default_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) JsonDefault *shared __attribute__((swift_name("shared")));
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
@property BOOL allowSpecialFloatingPointValues __attribute__((swift_name("allowSpecialFloatingPointValues")));
@property BOOL allowStructuredMapKeys __attribute__((swift_name("allowStructuredMapKeys")));
@property BOOL allowTrailingComma __attribute__((swift_name("allowTrailingComma")));
@property NSString *classDiscriminator __attribute__((swift_name("classDiscriminator")));
@property ClassDiscriminatorMode *classDiscriminatorMode __attribute__((swift_name("classDiscriminatorMode")));
@property BOOL coerceInputValues __attribute__((swift_name("coerceInputValues")));
@property BOOL decodeEnumsCaseInsensitive __attribute__((swift_name("decodeEnumsCaseInsensitive")));
@property BOOL encodeDefaults __attribute__((swift_name("encodeDefaults")));
@property BOOL explicitNulls __attribute__((swift_name("explicitNulls")));
@property BOOL ignoreUnknownKeys __attribute__((swift_name("ignoreUnknownKeys")));
@property BOOL isLenient __attribute__((swift_name("isLenient")));
@property id<JsonNamingStrategy> _Nullable namingStrategy __attribute__((swift_name("namingStrategy")));
@property BOOL prettyPrint __attribute__((swift_name("prettyPrint")));
@property NSString *prettyPrintIndent __attribute__((swift_name("prettyPrintIndent")));
@property SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@property BOOL useAlternativeNames __attribute__((swift_name("useAlternativeNames")));
@property BOOL useArrayPolymorphism __attribute__((swift_name("useArrayPolymorphism")));
@end

__attribute__((objc_subclassing_restricted))
@interface JsonConfiguration : Base
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL allowSpecialFloatingPointValues __attribute__((swift_name("allowSpecialFloatingPointValues")));
@property (readonly) BOOL allowStructuredMapKeys __attribute__((swift_name("allowStructuredMapKeys")));
@property (readonly) BOOL allowTrailingComma __attribute__((swift_name("allowTrailingComma")));
@property (readonly) NSString *classDiscriminator __attribute__((swift_name("classDiscriminator")));
@property ClassDiscriminatorMode *classDiscriminatorMode __attribute__((swift_name("classDiscriminatorMode")));
@property (readonly) BOOL coerceInputValues __attribute__((swift_name("coerceInputValues")));
@property (readonly) BOOL decodeEnumsCaseInsensitive __attribute__((swift_name("decodeEnumsCaseInsensitive")));
@property (readonly) BOOL encodeDefaults __attribute__((swift_name("encodeDefaults")));
@property (readonly) BOOL explicitNulls __attribute__((swift_name("explicitNulls")));
@property (readonly) BOOL ignoreUnknownKeys __attribute__((swift_name("ignoreUnknownKeys")));
@property (readonly) BOOL isLenient __attribute__((swift_name("isLenient")));
@property (readonly) id<JsonNamingStrategy> _Nullable namingStrategy __attribute__((swift_name("namingStrategy")));
@property (readonly) BOOL prettyPrint __attribute__((swift_name("prettyPrint")));
@property (readonly) NSString *prettyPrintIndent __attribute__((swift_name("prettyPrintIndent")));
@property (readonly) BOOL useAlternativeNames __attribute__((swift_name("useAlternativeNames")));
@property (readonly) BOOL useArrayPolymorphism __attribute__((swift_name("useArrayPolymorphism")));
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
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

@protocol JsonDecoder
@required
- (JsonElement *)decodeJsonElement __attribute__((swift_name("decodeJsonElement()")));
@property (readonly) Json *json __attribute__((swift_name("json")));
@end

@interface JsonElement : Base
@property (class, readonly, getter=companion) JsonElementCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("JsonElement.Companion")))
@interface JsonElementCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) JsonElementCompanion *shared __attribute__((swift_name("shared")));
- (KSerializer<JsonElement *> *)serializer __attribute__((swift_name("serializer()")));
@end

@protocol JsonEncoder
@required
- (void)encodeJsonElementElement:(JsonElement *)element __attribute__((swift_name("encodeJsonElement(element:)")));
@property (readonly) Json *json __attribute__((swift_name("json")));
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
@property (class, readonly, getter=shared) JsonNamingStrategyBuiltins *shared __attribute__((swift_name("shared")));
@property (readonly) id<JsonNamingStrategy> KebabCase __attribute__((swift_name("KebabCase")));
@property (readonly) id<JsonNamingStrategy> SnakeCase __attribute__((swift_name("SnakeCase")));
@end

@interface JsonPrimitive : JsonElement
@property (class, readonly, getter=companion) JsonPrimitiveCompanion *companion __attribute__((swift_name("companion")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *content __attribute__((swift_name("content")));
@property (readonly) BOOL isString __attribute__((swift_name("isString")));
@end

__attribute__((objc_subclassing_restricted))
@interface JsonNull : JsonPrimitive
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)jsonNull __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) JsonNull *shared __attribute__((swift_name("shared")));
- (KSerializer<JsonNull *> *)serializer __attribute__((swift_name("serializer()")));
- (KSerializer<id> *)serializerTypeParamsSerializers:(KotlinArray<KSerializer<id> *> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@property (readonly) NSString *content __attribute__((swift_name("content")));
@property (readonly) BOOL isString __attribute__((swift_name("isString")));
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
@property (class, readonly, getter=shared) JsonPrimitiveCompanion *shared __attribute__((swift_name("shared")));
- (KSerializer<JsonPrimitive *> *)serializer __attribute__((swift_name("serializer()")));
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
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
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
@property (readonly) NSArray<JsonElement *> *jsonArray __attribute__((swift_name("jsonArray")));
@property (readonly) JsonNull *jsonNull __attribute__((swift_name("jsonNull")));
@property (readonly) NSDictionary<NSString *, JsonElement *> *jsonObject __attribute__((swift_name("jsonObject")));
@property (readonly) JsonPrimitive *jsonPrimitive __attribute__((swift_name("jsonPrimitive")));
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
@property (readonly) BOOL boolean __attribute__((swift_name("boolean")));
@property (readonly) Boolean * _Nullable booleanOrNull __attribute__((swift_name("booleanOrNull")));
@property (readonly) NSString * _Nullable contentOrNull __attribute__((swift_name("contentOrNull")));
@property (readonly, getter=double) double double_ __attribute__((swift_name("double_")));
@property (readonly) Double * _Nullable doubleOrNull __attribute__((swift_name("doubleOrNull")));
@property (readonly, getter=float) float float_ __attribute__((swift_name("float_")));
@property (readonly) Float * _Nullable floatOrNull __attribute__((swift_name("floatOrNull")));
@property (readonly, getter=int) int32_t int_ __attribute__((swift_name("int_")));
@property (readonly) Int * _Nullable intOrNull __attribute__((swift_name("intOrNull")));
@property (readonly, getter=long) int64_t long_ __attribute__((swift_name("long_")));
@property (readonly) Long * _Nullable longOrNull __attribute__((swift_name("longOrNull")));
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
@property (class, readonly, getter=shared) KotlinEnumCompanion *shared __attribute__((swift_name("shared")));
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
@property (readonly) NSString * _Nullable qualifiedName __attribute__((swift_name("qualifiedName")));
@property (readonly) NSString * _Nullable simpleName __attribute__((swift_name("simpleName")));
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
- (KotlinCharIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(unichar)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinNothing : Base
@end

@protocol KotlinSequence
@required
- (id<KotlinIterator>)iterator __attribute__((swift_name("iterator()")));
@end

@protocol KotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

@interface KotlinCharIterator : Base <KotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id)next __attribute__((swift_name("next()")));
- (unichar)nextChar __attribute__((swift_name("nextChar()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
