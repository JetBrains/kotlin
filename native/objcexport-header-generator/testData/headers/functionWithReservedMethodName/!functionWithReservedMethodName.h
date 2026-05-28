#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class SharedNULL<T>;

@protocol SharedNO;

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
__attribute__((swift_name("Bar")))
@interface SharedBar : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * Documented [YES] method.
 */
- (void)YES_:(SharedNULL<id<SharedNO>> *)receiver __attribute__((swift_name("YES(_:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Foo")))
@interface SharedFoo : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * Documented [DEBUG] method.
 */
- (void)DEBUG_ __attribute__((swift_name("DEBUG()")));

/**
 * Documented [N] method.
 */
- (void)NULL_:(int32_t)ULL DEBUG_:(int64_t)DEBUG_ __attribute__((swift_name("N(ULL:DEBUG:)")));

/**
 * Documented [NO] method.
 */
- (void)NO_ __attribute__((swift_name("NO()")));

/**
 * Documented [NO] method.
 */
- (void)NOParam1:(int32_t)param1 __attribute__((swift_name("NO(param1:)")));

/**
 * Documented [NULL] method.
 */
- (void)NULL_ __attribute__((swift_name("NULL()")));

/**
 * Documented [YES] method.
 */
- (void)YES_ __attribute__((swift_name("YES()")));

/**
 * Documented [autorelease] method.
 */
- (void)autorelease_ __attribute__((swift_name("autorelease()")));

/**
 * Documented [class] method.
 */
- (void)class_ __attribute__((swift_name("class()")));

/**
 * Documented [release] method.
 */
- (void)release_ __attribute__((swift_name("release()")));

/**
 * Documented [retain] method.
 */
- (void)retain_ __attribute__((swift_name("retain()")));

/**
 * Documented [superclass] method.
 */
- (void)superclass_ __attribute__((swift_name("superclass()")));
@end

__attribute__((swift_name("NO")))
@protocol SharedNO
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NULL")))
@interface SharedNULL<T> : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
