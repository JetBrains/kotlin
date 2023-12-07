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
 * `Summary class` [KDocExport]. // EXPORT_KDOC
 *
 * @property xyzzy Doc for property xyzzy // EXPORT_KDOC
 * @property zzz See below. // EXPORT_KDOC
 */
__attribute__((objc_subclassing_restricted))
@interface KDocExport : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/** Non-primary ctor KDoc: // EXPORT_KDOC */
- (instancetype)initWithName:(NSString *)name __attribute__((swift_name("init(name:)"))) __attribute__((objc_designated_initializer));

/** @property xyzzy KDoc for foo? // EXPORT_KDOC */
@property (readonly) NSString *foo __attribute__((swift_name("foo")));

/**
 * @param xyzzy is documented. // EXPORT_KDOC
 *
 * This is multi-line KDoc. See a blank line above. // EXPORT_KDOC
 */
@property (readonly) NSString *xyzzy __attribute__((swift_name("xyzzy")));

/** @property foo KDoc for yxxyz? // EXPORT_KDOC */
@property int32_t yxxyz __attribute__((swift_name("yxxyz")));
@end

@interface SomeClassWithProperty : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * Kdoc for a property // EXPORT_KDOC
 */
@property (readonly) SomeClassWithProperty *heavyFormattedKDocFoo __attribute__((swift_name("heavyFormattedKDocFoo")));
@end

__attribute__((objc_subclassing_restricted))
@interface FooKt : Base

/**
 * Useless function [whatever] // EXPORT_KDOC
 *
 * This kdoc has some additional formatting. // EXPORT_KDOC
 * @param a keep intact and return // EXPORT_KDOC
 * @return value of [a] // EXPORT_KDOC
 * Check for additional comment (note) below // EXPORT_KDOC
 */
+ (NSString *)whateverA:(NSString *)a __attribute__((swift_name("whatever(a:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
