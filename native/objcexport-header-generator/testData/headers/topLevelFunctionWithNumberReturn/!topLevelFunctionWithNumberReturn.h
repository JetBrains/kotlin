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
@interface FooKt : Base
+ (int8_t)myByte __attribute__((swift_name("myByte()")));
+ (double)myDouble __attribute__((swift_name("myDouble()")));
+ (float)myFloat __attribute__((swift_name("myFloat()")));
+ (int32_t)myInt __attribute__((swift_name("myInt()")));
+ (int64_t)myLong __attribute__((swift_name("myLong()")));
+ (int16_t)myShort __attribute__((swift_name("myShort()")));
+ (uint8_t)myUByte __attribute__((swift_name("myUByte()")));
+ (uint32_t)myUInt __attribute__((swift_name("myUInt()")));
+ (uint64_t)myULong __attribute__((swift_name("myULong()")));
+ (uint16_t)myUShort __attribute__((swift_name("myUShort()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
