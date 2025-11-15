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
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (Foo *)__init __attribute__((objc_method_family(none)));
- (Foo *)__initOtherX:(int32_t)x __attribute__((swift_name("__initOther(x:)"))) __attribute__((objc_method_family(none)));
- (Foo *)__initWithX:(int32_t)x __attribute__((swift_name("__initWith(x:)"))) __attribute__((objc_method_family(none)));
- (Foo *)__initializer;
- (Foo *)_init __attribute__((objc_method_family(none)));
- (Foo *)_initOtherX:(int32_t)x __attribute__((swift_name("_initOther(x:)"))) __attribute__((objc_method_family(none)));
- (Foo *)_initWithX:(int32_t)x __attribute__((swift_name("_initWith(x:)"))) __attribute__((objc_method_family(none)));
- (Foo *)_initializer;
- (void)alloc __attribute__((objc_method_family(none)));
- (void)allocWithX:(int32_t)x __attribute__((swift_name("allocWith(x:)"))) __attribute__((objc_method_family(none)));
- (void)allocation;
- (Foo *)copy __attribute__((objc_method_family(none)));
- (Foo *)copyWithX:(int32_t)x __attribute__((swift_name("copyWith(x:)"))) __attribute__((objc_method_family(none)));
- (Foo *)copymachine;
- (Foo *)doInit;
- (Foo *)initOtherX:(int32_t)x __attribute__((swift_name("initOther(x:)"))) __attribute__((objc_method_family(none)));
- (Foo *)doInitWithX:(int32_t)x __attribute__((swift_name("doInitWith(x:)")));
- (Foo *)initializer;
- (Foo *)mutableCopy __attribute__((objc_method_family(none)));
- (Foo *)mutableCopyWithX:(int32_t)x __attribute__((swift_name("mutableCopyWith(x:)"))) __attribute__((objc_method_family(none)));
- (Foo *)mutableCopymachine;
- (Foo *)new __attribute__((objc_method_family(none)));
- (Foo *)newWithX:(int32_t)x __attribute__((swift_name("newWith(x:)"))) __attribute__((objc_method_family(none)));
- (Foo *)newer;
@property (getter=_init_) int32_t _init;
- (int32_t)_init_ __attribute__((objc_method_family(none)));
@property (getter=doInit_) int32_t init;
@property int32_t initOther;
- (int32_t)initOther __attribute__((objc_method_family(none)));
@property (getter=doInitWith) int32_t initWith;
@property (getter=initializer_) int32_t initializer;
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
