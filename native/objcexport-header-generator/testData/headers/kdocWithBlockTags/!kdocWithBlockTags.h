#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class SomeClassWithProperty;

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
 * `Summary class` [KDocExport].
 *
 * @property xyzzy Doc for property xyzzy
 * @property zzz See below.
 */
__attribute__((objc_subclassing_restricted))
@interface KDocExport : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/** Non-primary ctor KDoc:*/
- (instancetype)initWithName:(NSString *)name __attribute__((swift_name("init(name:)"))) __attribute__((objc_designated_initializer));

/** @property xyzzy KDoc for foo?*/
@property (readonly) NSString *foo __attribute__((swift_name("foo")));

/**
 * @param xyzzy is documented.
 *
 * This is multi-line KDoc. See a blank line above.
 */
@property (readonly) NSString *xyzzy __attribute__((swift_name("xyzzy")));

/** @property foo KDoc for yxxyz?*/
@property int32_t yxxyz __attribute__((swift_name("yxxyz")));
@end

@interface SomeClassWithProperty : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * Kdoc for a property
 */
@property (readonly) SomeClassWithProperty *heavyFormattedKDocFoo __attribute__((swift_name("heavyFormattedKDocFoo")));
@end

__attribute__((objc_subclassing_restricted))
@interface FooKt : Base

/**
 * Useless function [whatever]
 *
 * This kdoc has some additional formatting.
 * @param a keep intact and return
 * @return value of [a]
 * Check for additional comment (note) below
 */
+ (NSString *)whateverA:(NSString *)a __attribute__((swift_name("whatever(a:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END