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

__attribute__((objc_subclassing_restricted))
@interface Foo : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)a __attribute__((swift_name("a()")));
- (void)aP0:(int32_t)p0 __attribute__((swift_name("a(p0:)")));
- (void)aP0:(int32_t)p0 p1:(int32_t)p1 __attribute__((swift_name("a(p0:p1:)")));
- (void)b __attribute__((swift_name("b()")));
- (void)bP0:(int32_t)p0 __attribute__((swift_name("b(p0:)")));
- (void)bP0:(int32_t)p0 p1:(int32_t)p1 __attribute__((swift_name("b(p0:p1:)")));
- (void)c __attribute__((swift_name("c()")));
- (int32_t)d __attribute__((swift_name("d()")));
@property (readonly) int32_t pA __attribute__((swift_name("pA")));
@property (readonly) int32_t pB __attribute__((swift_name("pB")));
@property (readonly) int32_t pC __attribute__((swift_name("pC")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
