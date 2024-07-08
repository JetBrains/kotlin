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
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithI:(int32_t)i __attribute__((swift_name("init(i:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
@interface ExplicitCallable : Base
- (int32_t)entryPoint __attribute__((swift_name("entryPoint()")));
- (int32_t)entryPointI:(int32_t)i __attribute__((swift_name("entryPoint(i:)")));
@property (readonly, getter=entryPoint_) int32_t entryPoint __attribute__((swift_name("entryPoint")));
@end

__attribute__((objc_subclassing_restricted))
@interface ExplicitFunction : Base
- (int32_t)entryPoint __attribute__((swift_name("entryPoint()")));
- (int32_t)entryPointI:(int32_t)i __attribute__((swift_name("entryPoint(i:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ExplicitProperty : Base
@property (readonly) int32_t entryPoint __attribute__((swift_name("entryPoint")));
@end

__attribute__((objc_subclassing_restricted))
@interface NoEntryPoints : Base
@end

__attribute__((objc_subclassing_restricted))
@interface WildcardCallable : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithI:(int32_t)i __attribute__((swift_name("init(i:)"))) __attribute__((objc_designated_initializer));
- (int32_t)entryPoint1 __attribute__((swift_name("entryPoint1()")));
- (int32_t)entryPoint1I:(int32_t)i __attribute__((swift_name("entryPoint1(i:)")));
- (int32_t)entryPoint2 __attribute__((swift_name("entryPoint2()")));
@property (getter=entryPoint1_) int32_t entryPoint1 __attribute__((swift_name("entryPoint1")));
@property (getter=entryPoint2_) int32_t entryPoint2 __attribute__((swift_name("entryPoint2")));
@end

__attribute__((objc_subclassing_restricted))
@interface WildcardFunction : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithI:(int32_t)i __attribute__((swift_name("init(i:)"))) __attribute__((objc_designated_initializer));
- (int32_t)entryPoint1 __attribute__((swift_name("entryPoint1()")));
- (int32_t)entryPoint1I:(int32_t)i __attribute__((swift_name("entryPoint1(i:)")));
- (int32_t)entryPoint2 __attribute__((swift_name("entryPoint2()")));
@end

__attribute__((objc_subclassing_restricted))
@interface WildcardProperty : Base
@property (readonly) int32_t entryPoint1 __attribute__((swift_name("entryPoint1")));
@property (readonly) int32_t entryPoint2 __attribute__((swift_name("entryPoint2")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
