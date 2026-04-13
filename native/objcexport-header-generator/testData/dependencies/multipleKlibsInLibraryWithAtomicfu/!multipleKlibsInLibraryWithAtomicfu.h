#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class AtomicfuAtomicBoolean, KotlinArray<T>, KotlinEnum<E>, KotlinEnumCompanion, KotlinKTypeProjection, KotlinKTypeProjectionCompanion, KotlinKVariance;

@protocol KotlinComparable, KotlinIterator, KotlinKAnnotatedElement, KotlinKCallable, KotlinKClassifier, KotlinKProperty, KotlinKType;

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

__attribute__((objc_subclassing_restricted))
@interface FooKt : Base
@property (class, readonly) AtomicfuAtomicBoolean *flag __attribute__((swift_name("flag")));
@end

__attribute__((objc_subclassing_restricted))
@interface AtomicfuAtomicBoolean : Base
- (BOOL)compareAndSetExpect:(BOOL)expect update:(BOOL)update __attribute__((swift_name("compareAndSet(expect:update:)")));
- (BOOL)getAndSetValue:(BOOL)value __attribute__((swift_name("getAndSet(value:)")));
- (BOOL)getValueThisRef:(id _Nullable)thisRef property:(id<KotlinKProperty>)property __attribute__((swift_name("getValue(thisRef:property:)")));
- (void)lazySetValue:(BOOL)value __attribute__((swift_name("lazySet(value:)")));
- (void)setValueThisRef:(id _Nullable)thisRef property:(id<KotlinKProperty>)property value:(BOOL)value __attribute__((swift_name("setValue(thisRef:property:value:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property BOOL value __attribute__((swift_name("value")));
@end

@protocol KotlinKAnnotatedElement
@required
@end

@protocol KotlinKCallable <KotlinKAnnotatedElement>
@required
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) id<KotlinKType> returnType __attribute__((swift_name("returnType")));
@end

@protocol KotlinKProperty <KotlinKCallable>
@required
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


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
@protocol KotlinKClassifier
@required
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
- (KotlinKTypeProjection *)contravariantType:(id<KotlinKType>)type __attribute__((swift_name("contravariant(type:)")));
- (KotlinKTypeProjection *)covariantType:(id<KotlinKType>)type __attribute__((swift_name("covariant(type:)")));
- (KotlinKTypeProjection *)invariantType:(id<KotlinKType>)type __attribute__((swift_name("invariant(type:)")));
@property (readonly) KotlinKTypeProjection *STAR __attribute__((swift_name("STAR")));
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

@protocol KotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
