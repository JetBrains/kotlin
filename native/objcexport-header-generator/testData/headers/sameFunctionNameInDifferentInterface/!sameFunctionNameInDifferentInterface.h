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

@protocol Bar
@required
- (int32_t)someMethodWithDiffReturnType __attribute__((swift_name("someMethodWithDiffReturnType()")));
- (void)someMethodWithSameReturnType __attribute__((swift_name("someMethodWithSameReturnType()")));
@end

@protocol Foo
@required
- (NSString *)someMethodWithDiffReturnType_ __attribute__((swift_name("someMethodWithDiffReturnType_()")));
- (void)someMethodWithSameReturnType __attribute__((swift_name("someMethodWithSameReturnType()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END