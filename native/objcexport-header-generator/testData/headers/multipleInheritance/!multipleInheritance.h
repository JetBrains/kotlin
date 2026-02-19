#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class A<T>, B<T>;

@protocol RootA, RootB;

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


/**
 * inheritance chain: A > B > C
 */
@interface A<T> : Base

/**
 * inheritance chain: A > B > C
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * inheritance chain: A > B > C
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (T _Nullable)foo __attribute__((swift_name("foo()")));
@end

@interface B<T> : A<Int *>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (Int *)foo __attribute__((swift_name("foo()")));
@end

@interface C : B<Int *>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (Int *)foo __attribute__((swift_name("foo()")));
@end


/**
 * inheritance tree:
 * RootA  RootB
 *     \ /
 *    Tree
 */
@protocol RootA
@required
- (id _Nullable)fooA __attribute__((swift_name("fooA()")));
@end

@protocol RootB
@required
- (id _Nullable)fooB __attribute__((swift_name("fooB()")));
@end

__attribute__((objc_subclassing_restricted))
@interface Tree : Base <RootA, RootB>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (Int *)fooA __attribute__((swift_name("fooA()")));
- (NSString *)fooB __attribute__((swift_name("fooB()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
