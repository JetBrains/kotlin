#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Clazz;

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
@interface Clazz : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)memberFun __attribute__((swift_name("memberFun()")));
@end

@interface Clazz (Extensions)
@property (readonly) int32_t extensionValA __attribute__((swift_name("extensionValA")));
@property (readonly) int32_t extensionValB __attribute__((swift_name("extensionValB")));
@property int32_t extensionVarA __attribute__((swift_name("extensionVarA")));
@property int32_t extensionVarB __attribute__((swift_name("extensionVarB")));
@end

__attribute__((objc_subclassing_restricted))
@interface FooKt : Base
@property (class, readonly) int32_t topLevelPropA __attribute__((swift_name("topLevelPropA")));
@property (class, readonly) int32_t topLevelPropB __attribute__((swift_name("topLevelPropB")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
