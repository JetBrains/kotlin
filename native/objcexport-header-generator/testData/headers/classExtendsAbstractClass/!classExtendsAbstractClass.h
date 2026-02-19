#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class A;

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

@interface A : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)a0 __attribute__((swift_name("a0()")));
- (void)a1I:(int32_t)i __attribute__((swift_name("a1(i:)")));
- (void)a2B:(BOOL)b a:(id)a __attribute__((swift_name("a2(b:a:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface B : A
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)a0 __attribute__((swift_name("a0()")));
- (void)a1I:(int32_t)i __attribute__((swift_name("a1(i:)")));
- (void)a2B:(BOOL)b a:(id)a __attribute__((swift_name("a2(b:a:)")));
- (void)b0 __attribute__((swift_name("b0()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
