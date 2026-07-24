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
- (void)NO_ __attribute__((swift_name("NO()")));
- (void)NOX:(int32_t)x y:(int64_t)y z:(double)z __attribute__((swift_name("NO(x:y:z:)")));
- (void)NOX __attribute__((swift_name("NOX()")));
- (void)YES_ __attribute__((swift_name("YES()")));
- (void)YES__ __attribute__((swift_name("YES_()")));
@property (setter=setNULL:) int32_t NULL_ __attribute__((swift_name("NULL")));
@property (readonly) int32_t NULL__ __attribute__((swift_name("NULL_")));
@end

__attribute__((objc_subclassing_restricted))
@interface FooKt : Base
+ (void)DEBUG_:(int32_t)BUG __attribute__((swift_name("DE(BUG:)")));
+ (void)DEBUG_ __attribute__((swift_name("DEBUG()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
