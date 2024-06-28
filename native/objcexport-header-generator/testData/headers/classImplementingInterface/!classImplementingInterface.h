#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@protocol Foo;

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

@protocol Foo
@required
- (id)someMethod __attribute__((swift_name("someMethod()")));
- (id)someMethodWithCovariantOverwrite __attribute__((swift_name("someMethodWithCovariantOverwrite()")));
@property (readonly) int32_t someProperty __attribute__((swift_name("someProperty")));
@end

__attribute__((objc_subclassing_restricted))
@interface Bar : Base <Foo>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id)someMethod __attribute__((swift_name("someMethod()")));
- (NSString *)someMethodWithCovariantOverwrite __attribute__((swift_name("someMethodWithCovariantOverwrite()")));
@property (readonly) int32_t someProperty __attribute__((swift_name("someProperty")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
