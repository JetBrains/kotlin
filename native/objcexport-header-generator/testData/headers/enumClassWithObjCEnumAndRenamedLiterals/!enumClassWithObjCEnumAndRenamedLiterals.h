#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Foo, KotlinArray<T>, KotlinEnum<E>, KotlinEnumCompanion;

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

typedef NS_ENUM(int32_t, FooNSEnum) {
  FooNSEnumAlphaBeta NS_SWIFT_NAME(alphaBeta) = 0,
  FooNSEnumAlpha NS_SWIFT_NAME(alpha) = 1,
  FooNSEnumTheCopy NS_SWIFT_NAME(theCopy) = 2,
  FooNSEnumObjCName1Renamed NS_SWIFT_NAME(objCName1Renamed) = 3,
  FooNSEnumObjcName2Renamed NS_SWIFT_NAME(objcName2Swift) = 4,
  FooNSEnumEntryName1Renamed NS_SWIFT_NAME(entryName1Renamed) = 5,
  FooNSEnumEntryName2Renamed NS_SWIFT_NAME(entryName2Swift) = 6,
  FooNSEnumCombination1Renamed NS_SWIFT_NAME(combination1Renamed) = 7,
  FooNSEnumCombination2Renamed NS_SWIFT_NAME(combination2Renamed) = 8,
} NS_SWIFT_NAME(FooNSEnum);


__attribute__((objc_subclassing_restricted))
@interface Foo : KotlinEnum<Foo *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (readonly) FooNSEnum nsEnum;
@property (class, readonly) Foo *alphaBeta __attribute__((swift_name("alphaBeta")));
@property (class, readonly) Foo *alpha __attribute__((swift_name("alpha")));
@property (class, readonly) Foo *theCopy __attribute__((swift_name("theCopy")));
@property (class, readonly) Foo *objCName1Renamed __attribute__((swift_name("objCName1Renamed")));
@property (class, readonly) Foo *objcName2Renamed __attribute__((swift_name("objcName2Swift")));
@property (class, readonly) Foo *entryName1Original __attribute__((swift_name("entryName1Original")));
@property (class, readonly) Foo *entryName2Swift __attribute__((swift_name("entryName2Swift")));
@property (class, readonly) Foo *combination1Bad __attribute__((swift_name("combination1Bad")));
@property (class, readonly) Foo *combination2BadObjC __attribute__((swift_name("combination2BadSwift")));
+ (KotlinArray<Foo *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<Foo *> *entries __attribute__((swift_name("entries")));
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
