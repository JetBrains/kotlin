#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class ClazzA, ClazzB;

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
@interface ClazzA : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)memberFun __attribute__((swift_name("memberFun()")));
@end

__attribute__((objc_subclassing_restricted))
@interface ClazzB : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)memberFun __attribute__((swift_name("memberFun()")));
@end

@interface ClazzA (Extensions)
- (void)extensionFunA1 __attribute__((swift_name("extensionFunA1()")));
- (void)extensionFunA2 __attribute__((swift_name("extensionFunA2()")));
@end

@interface ClazzB (Extensions)
- (void)extensionFunB1 __attribute__((swift_name("extensionFunB1()")));
- (void)extensionFunB2 __attribute__((swift_name("extensionFunB2()")));
@end

__attribute__((objc_subclassing_restricted))
@interface FooKt : Base
+ (void)topLevelFunA __attribute__((swift_name("topLevelFunA()")));
+ (void)topLevelFunB __attribute__((swift_name("topLevelFunB()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
