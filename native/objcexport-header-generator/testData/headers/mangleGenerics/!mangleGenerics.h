#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Foo<Base_>;

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

@interface Foo<Base_> : Base
- (instancetype)initWithBase:(Base_ _Nullable)base __attribute__((swift_name("init(base:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
@interface Bar<Base_> : Foo<Base_>
- (instancetype)initWithBaseA:(Base_ _Nullable)baseA baseB:(Base_ _Nullable)baseB __attribute__((swift_name("init(baseA:baseB:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithBase:(id _Nullable)base __attribute__((swift_name("init(base:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (Base_ _Nullable)propertyExtension:(Base_ _Nullable)receiver __attribute__((swift_name("propertyExtension(_:)")));
- (Base_ _Nullable)methodBase1:(Base_ _Nullable)base1 base2:(Base_ _Nullable)base2 base3:(Base_ _Nullable)base3 __attribute__((swift_name("method(base1:base2:base3:)")));
- (void)methodExtension:(Base_ _Nullable)receiver __attribute__((swift_name("methodExtension(_:)")));
@property (readonly) Base_ _Nullable property __attribute__((swift_name("property")));
@end

__attribute__((objc_subclassing_restricted))
@interface BooleanType<Boolean> : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
@interface MutableSetType<MutableSet_> : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
@interface NSCopyingType<NSCopying_> : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
@interface NSErrorType<NSError> : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
@interface NSObjectType<NSObject_> : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
@interface TType<T> : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
@interface doubleType<double_> : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
