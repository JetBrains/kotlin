#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Point;

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

@protocol Marker
@required
@end

__attribute__((objc_subclassing_restricted))
@interface Point : Base
- (instancetype)initWithX:(int32_t)x y:(int32_t)y __attribute__((swift_name("init(x:y:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t x __attribute__((swift_name("x")));
@property (readonly) int32_t y __attribute__((swift_name("y")));
@end

__attribute__((objc_subclassing_restricted))
@interface FooKt : Base
+ (int32_t)doubled:(int32_t)receiver __attribute__((swift_name("doubled(_:)")));
+ (int32_t)doubledInline:(int32_t)receiver __attribute__((swift_name("doubledInline(_:)")));
+ (NSArray<id> *)listMeters:(NSArray<id> *)meters __attribute__((swift_name("list(meters:)")));
+ (NSArray<id> *)listInlineMeters:(NSArray<id> *)meters __attribute__((swift_name("listInline(meters:)")));
+ (int32_t)mapMeters:(int32_t)meters transform:(id (^)(id))transform __attribute__((swift_name("map(meters:transform:)")));
+ (int32_t)mapInlineMeters:(int32_t)meters transform:(id (^)(id))transform __attribute__((swift_name("mapInline(meters:transform:)")));
+ (id _Nullable)nullableMeters:(id _Nullable)meters __attribute__((swift_name("nullable(meters:)")));
+ (id _Nullable)nullableInlineMeters:(id _Nullable)meters __attribute__((swift_name("nullableInline(meters:)")));
+ (int32_t)sumPoint:(Point *)point __attribute__((swift_name("sum(point:)")));
+ (int32_t)tagTagged:(int32_t)tagged __attribute__((swift_name("tag(tagged:)")));
+ (int32_t)tagInlineTagged:(int32_t)tagged __attribute__((swift_name("tagInline(tagged:)")));
+ (int32_t)twiceMeters:(int32_t)meters __attribute__((swift_name("twice(meters:)")));
+ (int32_t)twiceInlineMeters:(int32_t)meters __attribute__((swift_name("twiceInline(meters:)")));
@property (class, readonly) int32_t zero __attribute__((swift_name("zero")));
@property (class, readonly) int32_t zeroInline __attribute__((swift_name("zeroInline")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
