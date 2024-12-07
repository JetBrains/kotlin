#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class KotlinNothing;

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

@protocol NothingCases
@required
- (void)nonNullExtension:(KotlinNothing *)receiver __attribute__((swift_name("nonNullExtension(_:)")));
- (void)nonNullMethod __attribute__((swift_name("nonNullMethod()")));
- (void)nonNullParamNothing:(KotlinNothing *)nothing __attribute__((swift_name("nonNullParam(nothing:)")));
- (void)nullExtension:(KotlinNothing *)receiver __attribute__((swift_name("nullExtension(_:)")));
- (KotlinNothing * _Nullable)nullMethod __attribute__((swift_name("nullMethod()")));
- (void)nullParamNothing:(KotlinNothing * _Nullable)nothing __attribute__((swift_name("nullParam(nothing:)")));
@property (readonly) KotlinNothing *nonNullVal __attribute__((swift_name("nonNullVal")));
@property KotlinNothing *nonNullVar __attribute__((swift_name("nonNullVar")));
@property (readonly) KotlinNothing * _Nullable nullVal __attribute__((swift_name("nullVal")));
@property KotlinNothing * _Nullable nullVar __attribute__((swift_name("nullVar")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinNothing : Base
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
