#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class DocumentedEnum, KotlinArray<T>, KotlinEnum<E>, KotlinEnumCompanion;

@protocol DocumentedInterface, KotlinComparable, KotlinIterator;

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


/**
 * KDoc of a class from a dependency.
 */
__attribute__((objc_subclassing_restricted))
@interface DocumentedClass : Base

/**
 * KDoc of a class from a dependency.
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * KDoc of a class from a dependency.
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * KDoc of a member function from a dependency.
 */
- (void)documentedFunction __attribute__((swift_name("documentedFunction()")));

/**
 * KDoc of a member property from a dependency.
 */
@property (readonly) int32_t documentedProperty __attribute__((swift_name("documentedProperty")));
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

__attribute__((objc_subclassing_restricted))
@interface DocumentedEnum : KotlinEnum<DocumentedEnum *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * KDoc of an enum entry from a dependency.
 */
@property (class, readonly) DocumentedEnum *entry __attribute__((swift_name("entry")));
+ (KotlinArray<DocumentedEnum *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<DocumentedEnum *> *entries __attribute__((swift_name("entries")));
@end

@protocol DocumentedInterface
@required
@end

__attribute__((objc_subclassing_restricted))
@interface DocumentedKt : Base

/**
 * KDoc of a getter from a dependency.
 */
+ (int32_t)documentedExtension:(id<DocumentedInterface>)receiver __attribute__((swift_name("documentedExtension(_:)")));

/**
 * KDoc of a setter from a dependency.
 */
+ (void)setDocumentedExtension:(id<DocumentedInterface>)receiver value:(int32_t)value __attribute__((swift_name("setDocumentedExtension(_:value:)")));
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
