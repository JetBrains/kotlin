#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Bar, Foo;

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

@interface Foo : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
@interface Bar : Foo
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

@interface Bar (Extensions)
- (void)extFun;
- (void)extFunP0:(BOOL)p0 p1:(NSString *)p1 __attribute__((swift_name("extFun(p0:p1:)")));
@property (readonly) int32_t propVal;
@property int32_t propVar;
@end

@interface Foo (Extensions)
- (void)extFun_;
- (void)extFunP0:(NSString *)p0 p1_:(BOOL)p1 __attribute__((swift_name("extFun(p0:p1_:)")));
@property (readonly) int32_t propVal_;
@property int32_t propVar_;
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
