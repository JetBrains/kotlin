#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Success<T>;

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

@interface Success<T> : Base
- (instancetype)initWithData:(T _Nullable)data __attribute__((swift_name("init(data:)"))) __attribute__((objc_designated_initializer));
@property (readonly) T _Nullable data __attribute__((swift_name("data")));
@end

__attribute__((objc_subclassing_restricted))
@interface Items : Success<NSString *>
- (instancetype)initWithItems:(NSString *)items __attribute__((swift_name("init(items:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithData:(id _Nullable)data __attribute__((swift_name("init(data:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
