#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@protocol A, B, C;

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

@protocol A
@required
- (id)funA __attribute__((swift_name("funA()")));
- (id)funAForOverride __attribute__((swift_name("funAForOverride()")));
@property (readonly) int32_t propertyA __attribute__((swift_name("propertyA")));
@property (readonly) int32_t propertyAForOverride __attribute__((swift_name("propertyAForOverride")));
@end

@protocol B <A>
@required
- (void)funB __attribute__((swift_name("funB()")));
@property (readonly) BOOL propertyB __attribute__((swift_name("propertyB")));
@end

@protocol C <B>
@required
@end

@protocol D <C>
@required
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
