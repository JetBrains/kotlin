#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Bar, KotlinArray<T>, KotlinEnum<E>, KotlinEnumCompanion, ObjCNameBaz, ObjCNameFoo, ObjCNameFooBar;

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

typedef NS_ENUM(int32_t, ObjCEnumBar) {
  ObjCEnumBarAlpha NS_SWIFT_NAME(alpha) = 0,
  ObjCEnumBarTheCopy NS_SWIFT_NAME(theCopy) = 1,
  ObjCEnumBarBarFoo NS_SWIFT_NAME(barFoo) = 2,
} NS_SWIFT_NAME(ObjCEnumBar);


__attribute__((objc_subclassing_restricted))
@interface Bar : KotlinEnum<Bar *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (readonly) ObjCEnumBar nsEnum;
@property (class, readonly) Bar *alpha __attribute__((swift_name("alpha")));
@property (class, readonly) Bar *theCopy __attribute__((swift_name("theCopy")));
@property (class, readonly) Bar *barFoo __attribute__((swift_name("barFoo")));
+ (KotlinArray<Bar *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<Bar *> *entries __attribute__((swift_name("entries")));
@end

typedef NS_ENUM(int32_t, ObjCNameBazNSEnum) {
  ObjCNameBazNSEnumAlpha NS_SWIFT_NAME(alpha) = 0,
  ObjCNameBazNSEnumTheCopy NS_SWIFT_NAME(theCopy) = 1,
  ObjCNameBazNSEnumBarFoo NS_SWIFT_NAME(barFoo) = 2,
} NS_SWIFT_NAME(ObjCNameBazNSEnum);


__attribute__((objc_subclassing_restricted))
@interface ObjCNameBaz : KotlinEnum<ObjCNameBaz *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (readonly) ObjCNameBazNSEnum nsEnum;
@property (class, readonly) ObjCNameBaz *alpha __attribute__((swift_name("alpha")));
@property (class, readonly) ObjCNameBaz *theCopy __attribute__((swift_name("theCopy")));
@property (class, readonly) ObjCNameBaz *barFoo __attribute__((swift_name("barFoo")));
+ (KotlinArray<ObjCNameBaz *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<ObjCNameBaz *> *entries __attribute__((swift_name("entries")));
@end

typedef NS_ENUM(int32_t, ObjCEnumFoo) {
  ObjCEnumFooAlpha NS_SWIFT_NAME(alpha) = 0,
  ObjCEnumFooTheCopy NS_SWIFT_NAME(theCopy) = 1,
  ObjCEnumFooBarFoo NS_SWIFT_NAME(barFoo) = 2,
} NS_SWIFT_NAME(ObjCEnumFoo);


__attribute__((objc_subclassing_restricted))
@interface ObjCNameFoo : KotlinEnum<ObjCNameFoo *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (readonly) ObjCEnumFoo nsEnum;
@property (class, readonly) ObjCNameFoo *alpha __attribute__((swift_name("alpha")));
@property (class, readonly) ObjCNameFoo *theCopy __attribute__((swift_name("theCopy")));
@property (class, readonly) ObjCNameFoo *barFoo __attribute__((swift_name("barFoo")));
+ (KotlinArray<ObjCNameFoo *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<ObjCNameFoo *> *entries __attribute__((swift_name("entries")));
@end

typedef NS_ENUM(int32_t, ObjCEnumFooBar) {
  ObjCEnumFooBarAlpha NS_SWIFT_NAME(alpha) = 0,
  ObjCEnumFooBarTheCopy NS_SWIFT_NAME(theCopy) = 1,
  ObjCEnumFooBarBarFoo NS_SWIFT_NAME(barFoo) = 2,
} NS_SWIFT_NAME(SwiftEnumFooBar);


__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SwiftNameFooBar")))
@interface ObjCNameFooBar : KotlinEnum<ObjCNameFooBar *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (readonly) ObjCEnumFooBar nsEnum;
@property (class, readonly) ObjCNameFooBar *alpha __attribute__((swift_name("alpha")));
@property (class, readonly) ObjCNameFooBar *theCopy __attribute__((swift_name("theCopy")));
@property (class, readonly) ObjCNameFooBar *barFoo __attribute__((swift_name("barFoo")));
+ (KotlinArray<ObjCNameFooBar *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<ObjCNameFooBar *> *entries __attribute__((swift_name("entries")));
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
