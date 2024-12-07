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
@interface Constructor : Base
@end

__attribute__((objc_subclassing_restricted))
@interface ConstructorDouble : Base
- (instancetype)initWithA:(int32_t)a __attribute__((swift_name("init(a:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithA_:(int32_t)a __attribute__((swift_name("init(a_:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithA:(int32_t)a b:(int32_t)b __attribute__((swift_name("init(a:b:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithA:(int32_t)a b:(int32_t)b c:(int32_t)c __attribute__((swift_name("init(a:b:c:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
@interface ConstructorFunction : Base
- (instancetype)initWithFoo:(void (^)(void))foo __attribute__((swift_name("init(foo:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
@interface ConstructorParam0 : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
@interface ConstructorParam1 : Base
- (instancetype)initWithA:(int32_t)a __attribute__((swift_name("init(a:)"))) __attribute__((objc_designated_initializer));
@property (readonly) int32_t a __attribute__((swift_name("a")));
@end

__attribute__((objc_subclassing_restricted))
@interface ConstructorParam2 : Base
- (instancetype)initWithA:(int32_t)a b:(int32_t)b __attribute__((swift_name("init(a:b:)"))) __attribute__((objc_designated_initializer));
@property (readonly) int32_t a __attribute__((swift_name("a")));
@property (readonly) int32_t b __attribute__((swift_name("b")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
