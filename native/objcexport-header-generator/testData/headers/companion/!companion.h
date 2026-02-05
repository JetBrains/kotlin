#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Foo1Companion, Foo2Companion, Foo3Companion, Foo4NamedCompanion, Foo5CompanionInObjC;

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
@interface Foo1 : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) Foo1Companion *companion;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Foo1.Companion")))
@interface Foo1Companion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Foo1Companion *shared;
- (void)publicFoo;
@property (readonly) int32_t constant;
@property (readonly) Foo2Companion *refToFoo2;
@property (readonly) Foo1Companion *refToItself;
@end

__attribute__((objc_subclassing_restricted))
@interface Foo2 : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) Foo2Companion *companion;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Foo2.Companion")))
@interface Foo2Companion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Foo2Companion *shared;
- (void)publicFoo;
@property (readonly) int32_t constant;
@property (readonly) Foo1Companion *refToFoo1;
@property (readonly) Foo2Companion *refToItself;
@end

__attribute__((objc_subclassing_restricted))
@interface Foo3 : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) Foo3Companion *companion;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Foo3.Companion")))
@interface Foo3Companion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Foo3Companion *shared;
@end

__attribute__((objc_subclassing_restricted))
@interface Foo4 : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) Foo4NamedCompanion *companion;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Foo4.NamedCompanion")))
@interface Foo4NamedCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)namedCompanion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Foo4NamedCompanion *shared;
@end

__attribute__((objc_subclassing_restricted))
@interface Foo5 : Base
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) Foo5CompanionInObjC *companion;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Foo5.CompanionInSwift")))
@interface Foo5CompanionInObjC : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companionInObjC __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Foo5CompanionInObjC *shared;
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
