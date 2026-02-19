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
@interface FooKt : Base
+ (float)floatVar:(float)receiver __attribute__((swift_name("floatVar(_:)")));
+ (int32_t)intVal:(int32_t)receiver __attribute__((swift_name("intVal(_:)")));
+ (void)setFloatVar:(float)receiver value:(float)value __attribute__((swift_name("setFloatVar(_:value:)")));
+ (void)stringFoo:(NSString *)receiver __attribute__((swift_name("stringFoo(_:)")));
+ (void)stringListFoo:(NSArray<NSString *> *)receiver __attribute__((swift_name("stringListFoo(_:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
