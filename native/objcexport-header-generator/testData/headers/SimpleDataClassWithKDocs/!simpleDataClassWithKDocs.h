#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Foo;

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
- (instancetype)initWithText:(NSString *)text number:(int32_t)number __attribute__((swift_name("init(text:number:)"))) __attribute__((objc_designated_initializer));

/**
 * This class [Foo] is documented.
 */
- (Foo *)doCopyText:(NSString *)text number:(int32_t)number __attribute__((swift_name("doCopy(text:number:)")));

/**
 * This class [Foo] is documented.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * This class [Foo] is documented.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * This class [Foo] is documented.
 */
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * Basic integer field
 */
@property (readonly) int32_t number __attribute__((swift_name("number")));

/**
 * Basic text field
 */
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
