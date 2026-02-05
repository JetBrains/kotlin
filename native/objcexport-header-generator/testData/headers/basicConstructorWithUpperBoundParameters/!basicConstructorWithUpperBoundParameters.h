#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class ReturnA<A, B>, ReturnB<A, B>;

@protocol UpperBound, UpperBound1;

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

@interface ReturnA<A, B> : Base
- (instancetype)initWithReturnA:(A (^)(void))returnA __attribute__((swift_name("init(returnA:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
@interface FooReturnA : ReturnA<id<UpperBound1>, id<UpperBound1>>
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithReturnA:(id<UpperBound> (^)(void))returnA __attribute__((swift_name("init(returnA:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

@interface ReturnB<A, B> : Base
- (instancetype)initWithReturnB:(B (^)(void))returnB __attribute__((swift_name("init(returnB:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
@interface FooReturnB : ReturnB<id<UpperBound1>, id<UpperBound1>>
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithReturnB:(id<UpperBound> (^)(void))returnB __attribute__((swift_name("init(returnB:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

@protocol UpperBound
@required
@end

@protocol UpperBound1 <UpperBound>
@required
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
