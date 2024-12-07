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
- (instancetype)initWithParam:(int32_t)param __attribute__((swift_name("init(param:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("message: error-constructor")));
- (instancetype)initWithParamA:(int32_t)paramA paramB:(int32_t)paramB __attribute__((swift_name("init(paramA:paramB:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("message: warning-constructor")));
- (void)fooError __attribute__((swift_name("fooError()"))) __attribute__((unavailable("message: error")));
- (void)fooWarning __attribute__((swift_name("fooWarning()"))) __attribute__((deprecated("message: warning")));
@property int32_t varError __attribute__((swift_name("varError"))) __attribute__((unavailable("message: warning-property")));
@property int32_t varWarning __attribute__((swift_name("varWarning"))) __attribute__((deprecated("message: warning-property")));
@end

__attribute__((objc_subclassing_restricted))
@interface FooError : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("message: error-class")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
@interface FooWarning : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("message: warning-class")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
