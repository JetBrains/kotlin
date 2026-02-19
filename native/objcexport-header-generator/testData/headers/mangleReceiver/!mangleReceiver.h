#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@protocol Bar0, Bar1;

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

@protocol Bar0
@required
@end

@protocol Bar1
@required
@end

@protocol Foo
@required
- (void)invoke:(id<Bar0>)receiver block:(id _Nullable (^)(void))block __attribute__((swift_name("invoke(_:block:)")));
- (void)invoke:(id<Bar1>)receiver block_:(id _Nullable (^)(id _Nullable))block __attribute__((swift_name("invoke(_:block_:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
