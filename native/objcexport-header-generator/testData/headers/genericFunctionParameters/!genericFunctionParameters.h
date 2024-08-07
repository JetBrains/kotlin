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
@interface Foo<ClassParameter> : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (ClassParameter _Nullable)classParameterExtension:(ClassParameter _Nullable)receiver param:(ClassParameter _Nullable)param __attribute__((swift_name("classParameterExtension(_:param:)")));
- (NSArray<id> *)listExtensionWithClassParameter:(NSArray<id> *)receiver list:(NSArray<id> *)list __attribute__((swift_name("listExtensionWithClassParameter(_:list:)")));
- (NSArray<id> *)listExtensionWithMethodParameter:(NSArray<id> *)receiver list:(NSArray<id> *)list __attribute__((swift_name("listExtensionWithMethodParameter(_:list:)")));
- (id _Nullable)methodParameterExtension:(id _Nullable)receiver param:(id _Nullable)param __attribute__((swift_name("methodParameterExtension(_:param:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
