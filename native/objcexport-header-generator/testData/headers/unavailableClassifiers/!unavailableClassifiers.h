#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class AnnotatedEnum, AnnotatedObject, KotlinArray<T>, KotlinEnum<E>, KotlinEnumCompanion;

@protocol KotlinComparable, KotlinIterator;

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
@interface AnnotatedClass : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
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
@interface AnnotatedEnum : KotlinEnum<AnnotatedEnum *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (KotlinArray<AnnotatedEnum *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<AnnotatedEnum *> *entries __attribute__((swift_name("entries")));
@end

@protocol AnnotatedInterface
@required
@end

__attribute__((unavailable("can't be imported")))
@interface AnnotatedObjCObjectClass : NSObject
@end

__attribute__((unavailable("can't be imported")))
@interface AnnotatedObjCObjectEnum : NSObject
@end

__attribute__((unavailable("can't be imported")))
@protocol AnnotatedObjCObjectInterface
@required
@end

__attribute__((unavailable("can't be imported")))
@interface AnnotatedObjCObjectObject : NSObject
@end

__attribute__((objc_subclassing_restricted))
@interface AnnotatedObject : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)annotatedObject __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) AnnotatedObject *shared __attribute__((swift_name("shared")));
@end

__attribute__((unavailable("can't be imported")))
@interface ListClass : NSObject
@end

__attribute__((unavailable("can't be imported")))
@protocol ListInterface
@required
@end

__attribute__((unavailable("can't be imported")))
@protocol ListInterfaceExtension
@required
@end

__attribute__((unavailable("Kotlin subclass of Objective-C class can't be imported")))
@interface ObjCObjectClass : NSObject
@end

__attribute__((unavailable("Kotlin subclass of Objective-C class can't be imported")))
@protocol ObjCObjectInterface
@required
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
