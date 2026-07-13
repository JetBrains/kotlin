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
+ (void)barYES:(id _Nullable)YES_ a:(id _Nullable)a b:(int64_t)b __attribute__((swift_name("bar(YES:a:b:)")));
+ (void)barNO:(id _Nullable)NO_ a:(int32_t)a b:(id _Nullable)b __attribute__((swift_name("bar(NO:a:b:)")));
+ (void)fooNULL:(unichar)NULL_ a:(int32_t)a DEBUG_:(int64_t)DEBUG_ __attribute__((swift_name("foo(NULL:a:DEBUG:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
