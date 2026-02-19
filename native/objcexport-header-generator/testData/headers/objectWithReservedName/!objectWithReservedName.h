#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class alloc, autorelease, class, classFallbacksForKeyedArchiver, classForKeyedUnarchiver, debugDescription, description, hash, initialize, load, new, release, retain, superclass, useStoredAccessor, version;

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
@interface alloc : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)getAlloc __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) alloc *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface autorelease : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)autorelease_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) autorelease *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface class : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)class_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) class *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface classFallbacksForKeyedArchiver : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)classFallbacksForKeyedArchiver_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) classFallbacksForKeyedArchiver *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface classForKeyedUnarchiver : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)classForKeyedUnarchiver_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) classForKeyedUnarchiver *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface debugDescription : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)debugDescription_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) debugDescription *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface description : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)description_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) description *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface hash : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)hash_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) hash *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface initialize : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)initialize_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) initialize *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface load : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)load_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) load *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface new : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)getNew __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) new *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface release : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)release_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) release *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface retain : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)retain_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) retain *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface superclass : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)superclass_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) superclass *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface useStoredAccessor : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)useStoredAccessor_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) useStoredAccessor *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface version : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)version_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) version *shared __attribute__((swift_name("shared")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
