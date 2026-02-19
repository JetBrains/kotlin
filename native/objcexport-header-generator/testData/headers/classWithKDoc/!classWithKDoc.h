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


/**
 * This class [Foo] is documented.
 */
__attribute__((objc_subclassing_restricted))
@interface Foo : Base

/**
 * This class [Foo] is documented.
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * This class [Foo] is documented.
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * This member function is documented
 */
- (void)someMemberFunction __attribute__((swift_name("someMemberFunction()")));

/**
 * This member property is documented.
 * It will return the 'The Answer to the Ultimate Question of Life, The Universe, and Everything'
 */
@property (readonly) int32_t someMemberProperty __attribute__((swift_name("someMemberProperty")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
