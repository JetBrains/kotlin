#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Foo;

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
@interface Foo : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (Foo *)days:(double)receiver __attribute__((swift_name("days(_:)")));
- (Foo *)days_:(int32_t)receiver __attribute__((swift_name("days(__:)")));
- (Foo *)days__:(int64_t)receiver __attribute__((swift_name("days(___:)")));
- (Foo *)days:(double)receiver p:(NSString *)p __attribute__((swift_name("days(_:p:)")));
- (Foo *)days:(int32_t)receiver p_:(NSString *)p __attribute__((swift_name("days(_:p_:)")));
- (Foo *)days:(int64_t)receiver p__:(NSString *)p __attribute__((swift_name("days(_:p__:)")));
- (Foo *)hours:(double)receiver __attribute__((swift_name("hours(_:)")));
- (Foo *)hours_:(int32_t)receiver __attribute__((swift_name("hours(__:)")));
- (Foo *)hours__:(int64_t)receiver __attribute__((swift_name("hours(___:)")));
- (Foo *)microseconds:(double)receiver __attribute__((swift_name("microseconds(_:)")));
- (Foo *)microseconds_:(int32_t)receiver __attribute__((swift_name("microseconds(__:)")));
- (Foo *)microseconds__:(int64_t)receiver __attribute__((swift_name("microseconds(___:)")));
- (Foo *)milliseconds:(double)receiver __attribute__((swift_name("milliseconds(_:)")));
- (Foo *)milliseconds_:(int32_t)receiver __attribute__((swift_name("milliseconds(__:)")));
- (Foo *)milliseconds__:(int64_t)receiver __attribute__((swift_name("milliseconds(___:)")));
- (Foo *)minutes:(double)receiver __attribute__((swift_name("minutes(_:)")));
- (Foo *)minutes_:(int32_t)receiver __attribute__((swift_name("minutes(__:)")));
- (Foo *)minutes__:(int64_t)receiver __attribute__((swift_name("minutes(___:)")));
- (Foo *)nanoseconds:(double)receiver __attribute__((swift_name("nanoseconds(_:)")));
- (Foo *)nanoseconds_:(int32_t)receiver __attribute__((swift_name("nanoseconds(__:)")));
- (Foo *)nanoseconds__:(int64_t)receiver __attribute__((swift_name("nanoseconds(___:)")));
- (Foo *)seconds:(double)receiver __attribute__((swift_name("seconds(_:)")));
- (Foo *)seconds_:(int32_t)receiver __attribute__((swift_name("seconds(__:)")));
- (Foo *)seconds__:(int64_t)receiver __attribute__((swift_name("seconds(___:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
