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

__attribute__((objc_subclassing_restricted))
@interface Foo : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)foo __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Foo *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *NO_ __attribute__((swift_name("NO")));
@property (setter=setDEBUG:) BOOL DEBUG_ __attribute__((swift_name("DEBUG")));
@property (readonly) NSString *NULL_ __attribute__((swift_name("NULL")));
@end

__attribute__((objc_subclassing_restricted))
@interface FooKt : Base
@property (class, readonly) NSString *YES_ __attribute__((swift_name("YES")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
