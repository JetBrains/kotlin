#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class KotlinAbstractCoroutineContextElement, KotlinAbstractCoroutineContextKey<B, E>, Kotlinx_coroutines_coreCoroutineDispatcher, Kotlinx_coroutines_coreCoroutineDispatcherKey;

@protocol KotlinContinuation, KotlinContinuationInterceptor, KotlinCoroutineContext, KotlinCoroutineContextElement, KotlinCoroutineContextKey, Kotlinx_coroutines_coreRunnable;

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
@property (class, readonly) Kotlinx_coroutines_coreCoroutineDispatcherKey * _Nullable key;
@end

@protocol KotlinCoroutineContextKey
@required
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
 *   kotlin.ExperimentalStdlibApi
*/
@interface KotlinAbstractCoroutineContextKey<B, E> : Base <KotlinCoroutineContextKey>
- (instancetype)initWithBaseKey:(id<KotlinCoroutineContextKey>)baseKey safeCast:(E _Nullable (^)(id<KotlinCoroutineContextElement> element))safeCast __attribute__((swift_name("init(baseKey:safeCast:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.ExperimentalStdlibApi
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_coroutines_coreCoroutineDispatcher.Key")))
@interface Kotlinx_coroutines_coreCoroutineDispatcherKey : KotlinAbstractCoroutineContextKey<id<KotlinContinuationInterceptor>, Kotlinx_coroutines_coreCoroutineDispatcher *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithBaseKey:(id<KotlinCoroutineContextKey>)baseKey safeCast:(id<KotlinCoroutineContextElement> _Nullable (^)(id<KotlinCoroutineContextElement> element))safeCast __attribute__((swift_name("init(baseKey:safeCast:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Kotlinx_coroutines_coreCoroutineDispatcherKey *shared;
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@protocol KotlinCoroutineContext
@required
- (id _Nullable)foldInitial:(id _Nullable)initial operation:(id _Nullable (^)(id _Nullable, id<KotlinCoroutineContextElement>))operation __attribute__((swift_name("fold(initial:operation:)")));
- (id<KotlinCoroutineContextElement> _Nullable)getKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("get(key:)")));
- (id<KotlinCoroutineContext>)minusKeyKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("minusKey(key:)")));
- (id<KotlinCoroutineContext>)plusContext:(id<KotlinCoroutineContext>)context __attribute__((swift_name("plus(context:)")));
@end

@protocol KotlinCoroutineContextElement <KotlinCoroutineContext>
@required
@property (readonly) id<KotlinCoroutineContextKey> key;
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@protocol KotlinContinuationInterceptor <KotlinCoroutineContextElement>
@required
- (id<KotlinContinuation>)interceptContinuationContinuation:(id<KotlinContinuation>)continuation __attribute__((swift_name("interceptContinuation(continuation:)")));
- (void)releaseInterceptedContinuationContinuation:(id<KotlinContinuation>)continuation __attribute__((swift_name("releaseInterceptedContinuation(continuation:)")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@interface KotlinAbstractCoroutineContextElement : Base <KotlinCoroutineContextElement>
- (instancetype)initWithKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer));
@property (readonly) id<KotlinCoroutineContextKey> key;
@end

@interface Kotlinx_coroutines_coreCoroutineDispatcher : KotlinAbstractCoroutineContextElement <KotlinContinuationInterceptor>
- (instancetype)init __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) Kotlinx_coroutines_coreCoroutineDispatcherKey *companion;
- (void)dispatchContext:(id<KotlinCoroutineContext>)context block:(id<Kotlinx_coroutines_coreRunnable>)block __attribute__((swift_name("dispatch(context:block:)")));
- (void)dispatchYieldContext:(id<KotlinCoroutineContext>)context block:(id<Kotlinx_coroutines_coreRunnable>)block __attribute__((swift_name("dispatchYield(context:block:)")));
- (id<KotlinContinuation>)interceptContinuationContinuation:(id<KotlinContinuation>)continuation __attribute__((swift_name("interceptContinuation(continuation:)")));
- (BOOL)isDispatchNeededContext:(id<KotlinCoroutineContext>)context __attribute__((swift_name("isDispatchNeeded(context:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (Kotlinx_coroutines_coreCoroutineDispatcher *)limitedParallelismParallelism:(int32_t)parallelism __attribute__((swift_name("limitedParallelism(parallelism:)")));
- (Kotlinx_coroutines_coreCoroutineDispatcher *)plusOther:(Kotlinx_coroutines_coreCoroutineDispatcher *)other __attribute__((swift_name("plus(other:)"))) __attribute__((unavailable("Operator '+' on two CoroutineDispatcher objects is meaningless. CoroutineDispatcher is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The dispatcher to the right of `+` just replaces the dispatcher to the left.")));
- (void)releaseInterceptedContinuationContinuation:(id<KotlinContinuation>)continuation __attribute__((swift_name("releaseInterceptedContinuation(continuation:)")));
- (NSString *)description;
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@protocol KotlinContinuation
@required
- (void)resumeWithResult:(id _Nullable)result __attribute__((swift_name("resumeWith(result:)")));
@property (readonly) id<KotlinCoroutineContext> context;
@end

@protocol Kotlinx_coroutines_coreRunnable
@required
- (void)run;
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
