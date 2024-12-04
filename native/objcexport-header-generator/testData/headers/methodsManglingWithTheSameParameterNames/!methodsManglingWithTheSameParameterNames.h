#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

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

@protocol Foo1Params
@required
- (void)barParam1:(BOOL)param1 __attribute__((swift_name("bar(param1:)")));
- (void)barParam1_:(int32_t)param1 __attribute__((swift_name("bar(param1_:)")));
- (void)barParam1__:(NSString *)param1 __attribute__((swift_name("bar(param1__:)")));
@end

@protocol Foo2Params
@required
- (void)barParam1:(BOOL)param1 param2:(int32_t)param2 __attribute__((swift_name("bar(param1:param2:)")));
- (void)barParam1:(int32_t)param1 param2_:(NSString *)param2 __attribute__((swift_name("bar(param1:param2_:)")));
- (void)barParam1:(NSString *)param1 param2__:(int32_t)param2 __attribute__((swift_name("bar(param1:param2__:)")));
@end

@protocol Foo3Params
@required
- (void)barParam1:(BOOL)param1 param2:(NSString *)param2 param3:(int32_t)param3 __attribute__((swift_name("bar(param1:param2:param3:)")));
- (void)barParam1:(int32_t)param1 param2:(BOOL)param2 param3_:(NSString *)param3 __attribute__((swift_name("bar(param1:param2:param3_:)")));
- (void)barParam1:(NSString *)param1 param2:(int32_t)param2 param3__:(BOOL)param3 __attribute__((swift_name("bar(param1:param2:param3__:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
