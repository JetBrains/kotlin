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
- (void)bar0Cb:(void (^)(Int *result))cb __attribute__((swift_name("bar0(cb:)")));
- (void)bar1Cb:(void (^)(Int *a))cb __attribute__((swift_name("bar1(cb:)")));
- (void)bar2Cb:(void (^)(Int *a, Int *b, NSString *c))cb __attribute__((swift_name("bar2(cb:)")));
- (void)bar3Cb:(void (^)(Int *int_, Int *int__, Double *double_))cb __attribute__((swift_name("bar3(cb:)")));
@end

@interface Foo (Extensions)
- (void)bar4Cb:(void (^)(Int *int_, NSString *message))cb __attribute__((swift_name("bar4(cb:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
