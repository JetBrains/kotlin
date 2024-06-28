#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class SimpleDataClass;

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
@interface SimpleDataClass : Base
- (instancetype)initWithIntValue:(int32_t)intValue intVar:(int32_t)intVar nullableDefaultStringValue:(NSString * _Nullable)nullableDefaultStringValue __attribute__((swift_name("init(intValue:intVar:nullableDefaultStringValue:)"))) __attribute__((objc_designated_initializer));
- (SimpleDataClass *)doCopyIntValue:(int32_t)intValue intVar:(int32_t)intVar nullableDefaultStringValue:(NSString * _Nullable)nullableDefaultStringValue __attribute__((swift_name("doCopy(intValue:intVar:nullableDefaultStringValue:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t intValue __attribute__((swift_name("intValue")));
@property int32_t intVar __attribute__((swift_name("intVar")));
@property (readonly) NSString * _Nullable nullableDefaultStringValue __attribute__((swift_name("nullableDefaultStringValue")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
