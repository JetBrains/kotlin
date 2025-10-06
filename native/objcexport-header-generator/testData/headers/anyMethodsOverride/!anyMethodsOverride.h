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
@interface Mix : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)equals;
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (void)equalsParam:(int32_t)param __attribute__((swift_name("equals(param:)")));
- (NSUInteger)hash;
- (int32_t)hashCodeParam:(int32_t)param __attribute__((swift_name("hashCode(param:)")));
- (NSString *)description;
- (NSString *)toStringParam:(int32_t)param __attribute__((swift_name("toString(param:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface NoParams : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)equals;
@end

__attribute__((objc_subclassing_restricted))
@interface Override : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash;
- (NSString *)description;
@end

__attribute__((objc_subclassing_restricted))
@interface WithParams : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)equalsParam:(int32_t)param __attribute__((swift_name("equals(param:)")));
- (int32_t)hashCodeParam:(int32_t)param __attribute__((swift_name("hashCode(param:)")));
- (NSString *)toStringParam:(int32_t)param __attribute__((swift_name("toString(param:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
