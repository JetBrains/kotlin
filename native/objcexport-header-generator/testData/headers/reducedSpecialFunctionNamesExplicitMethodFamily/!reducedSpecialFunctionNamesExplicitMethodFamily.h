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
- (int32_t)__initSomethingElse __attribute__((swift_name("__initSomethingElse()"))) __attribute__((objc_method_family(none)));
- (int32_t)alloc __attribute__((swift_name("alloc()"))) __attribute__((objc_method_family(none)));
@property (readonly) int32_t _newBaz __attribute__((swift_name("_newBaz")));
- (int32_t)_newBaz __attribute__((swift_name("_newBaz()"))) __attribute__((objc_method_family(none)));
@property (readonly) int32_t copySomething __attribute__((swift_name("copySomething")));
- (int32_t)copySomething __attribute__((swift_name("copySomething()"))) __attribute__((objc_method_family(none)));
@property (readonly) int32_t copyWithSomething __attribute__((swift_name("copyWithSomething")));
- (int32_t)copyWithSomething __attribute__((swift_name("copyWithSomething()"))) __attribute__((objc_method_family(none)));
@property (getter=doInitWithNothing) int32_t initWithNothing __attribute__((swift_name("initWithNothing")));
@property (readonly, getter=doInitWithSomething) int32_t initWithSomething __attribute__((swift_name("initWithSomething")));
@property (readonly) int32_t mutableCopy __attribute__((swift_name("mutableCopy")));
- (int32_t)mutableCopy __attribute__((swift_name("mutableCopy()"))) __attribute__((objc_method_family(none)));
@property (readonly) int32_t newBar __attribute__((swift_name("newBar")));
- (int32_t)newBar __attribute__((swift_name("newBar()"))) __attribute__((objc_method_family(none)));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
