#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class KotlinThrowable, KotlinCancellationException, JobSupport, CoroutineStart, CoroutineDispatcher, KotlinAbstractCoroutineContextElement, CoroutineDispatcherKey, KotlinArray<T>, KotlinException, KotlinRuntimeException, KotlinAbstractCoroutineContextKey<B, E>, CoroutineExceptionHandlerKey, CoroutineNameKey, CoroutineName, KotlinEnumCompanion, KotlinEnum<E>, Dispatchers, MainCoroutineDispatcher, GlobalScope, JobKey, NonCancellable, NonDisposableHandle, KotlinIllegalStateException, TimeoutCancellationException, BufferOverflow, ChannelFactory, KotlinNoSuchElementException, SharingCommand, SharingStartedCompanion, ChannelFlow<T>, AtomicOp<__contravariant T>, OpDescriptor, LockFreeLinkedListNode, SynchronizedObject, ThreadSafeHeap<T>, KotlinUnit, KotlinIntIterator, KotlinIntArray, KotlinLongIterator, KotlinLongArray, KotlinIntProgressionCompanion, KotlinIntProgression, KotlinIntRangeCompanion, KotlinIntRange, KotlinLongProgressionCompanion, KotlinLongProgression, KotlinLongRangeCompanion, KotlinLongRange, KotlinNothing, CloseableCoroutineDispatcher;

@protocol ChildHandle, ChildJob, DisposableHandle, Job, KotlinSequence, SelectClause0, KotlinCoroutineContextKey, KotlinCoroutineContextElement, KotlinCoroutineContext, ParentJob, SelectClause1, KotlinContinuation, CoroutineScope, KotlinSuspendFunction1, KotlinContinuationInterceptor, Runnable, Deferred, KotlinComparable, CancellableContinuation, CopyableThrowable, ReceiveChannel, SelectClause2, SendChannel, ChannelIterator, BroadcastChannel, FlowCollector, Flow, StateFlow, SharedFlow, MutableSharedFlow, SharingStarted, FusibleFlow, ProducerScope, MainDispatcherFactory, KotlinSuspendFunction0, SelectInstance, SelectClause, KotlinIterator, KotlinIterable, KotlinClosedRange, KotlinOpenEndRange, Channel, KotlinSuspendFunction2, CompletableDeferred, CoroutineExceptionHandler, KotlinComparator, KotlinSuspendFunction3, CompletableJob, KotlinSuspendFunction4, KotlinSuspendFunction5, Mutex, SelectBuilder, Semaphore, MutableStateFlow, KotlinKClass, KotlinSuspendFunction6, KotlinFunction, KotlinKDeclarationContainer, KotlinKAnnotatedElement, KotlinKClassifier;

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
@property (readonly) id<KotlinCoroutineContextKey> key __attribute__((swift_name("key")));
@end

@protocol Job <KotlinCoroutineContextElement>
@required
- (id<ChildHandle>)attachChildChild:(id<ChildJob>)child __attribute__((swift_name("attachChild(child:)")));
- (void)cancel __attribute__((swift_name("cancel()")));
- (BOOL)cancelCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (void)cancelCause_:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause_:)")));
- (KotlinCancellationException *)getCancellationException __attribute__((swift_name("getCancellationException()")));
- (id<DisposableHandle>)invokeOnCompletionHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(handler:)")));
- (id<DisposableHandle>)invokeOnCompletionOnCancelling:(BOOL)onCancelling invokeImmediately:(BOOL)invokeImmediately handler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(onCancelling:invokeImmediately:handler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)joinWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("join(completionHandler:)")));
- (id<Job>)plusOther:(id<Job>)other __attribute__((swift_name("plus(other:)")));
- (BOOL)start __attribute__((swift_name("start()")));
@property (readonly) id<KotlinSequence> children __attribute__((swift_name("children")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive")));
@property (readonly) BOOL isCancelled __attribute__((swift_name("isCancelled")));
@property (readonly) BOOL isCompleted __attribute__((swift_name("isCompleted")));
@property (readonly) id<SelectClause0> onJoin __attribute__((swift_name("onJoin")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
@property (readonly) id<Job> _Nullable parent __attribute__((swift_name("parent")));
@end

@protocol ChildJob <Job>
@required
- (void)parentCancelledParentJob:(id<ParentJob>)parentJob __attribute__((swift_name("parentCancelled(parentJob:)")));
@end

@protocol ParentJob <Job>
@required
- (KotlinCancellationException *)getChildJobCancellationCause __attribute__((swift_name("getChildJobCancellationCause()")));
@end

@interface JobSupport : Base <Job, ChildJob, ParentJob>
- (instancetype)initWithActive:(BOOL)active __attribute__((swift_name("init(active:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)afterCompletionState:(id _Nullable)state __attribute__((swift_name("afterCompletion(state:)")));
- (id<ChildHandle>)attachChildChild:(id<ChildJob>)child __attribute__((swift_name("attachChild(child:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)awaitInternalWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("awaitInternal(completionHandler:)")));
- (BOOL)cancelCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (void)cancelCause_:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause_:)")));
- (BOOL)cancelCoroutineCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancelCoroutine(cause:)")));
- (void)cancelInternalCause:(KotlinThrowable *)cause __attribute__((swift_name("cancelInternal(cause:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)cancellationExceptionMessage __attribute__((swift_name("cancellationExceptionMessage()")));
- (BOOL)childCancelledCause:(KotlinThrowable *)cause __attribute__((swift_name("childCancelled(cause:)")));
- (KotlinCancellationException *)getCancellationException __attribute__((swift_name("getCancellationException()")));
- (KotlinCancellationException *)getChildJobCancellationCause __attribute__((swift_name("getChildJobCancellationCause()")));
- (KotlinThrowable * _Nullable)getCompletionExceptionOrNull __attribute__((swift_name("getCompletionExceptionOrNull()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (BOOL)handleJobExceptionException:(KotlinThrowable *)exception __attribute__((swift_name("handleJobException(exception:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)doInitParentJobParent:(id<Job> _Nullable)parent __attribute__((swift_name("doInitParentJob(parent:)")));
- (id<DisposableHandle>)invokeOnCompletionHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(handler:)")));
- (id<DisposableHandle>)invokeOnCompletionOnCancelling:(BOOL)onCancelling invokeImmediately:(BOOL)invokeImmediately handler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(onCancelling:invokeImmediately:handler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)joinWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("join(completionHandler:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)onCancellingCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("onCancelling(cause:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)onCompletionInternalState:(id _Nullable)state __attribute__((swift_name("onCompletionInternal(state:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)onStart __attribute__((swift_name("onStart()")));
- (void)parentCancelledParentJob:(id<ParentJob>)parentJob __attribute__((swift_name("parentCancelled(parentJob:)")));
- (BOOL)start __attribute__((swift_name("start()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (KotlinCancellationException *)toCancellationException:(KotlinThrowable *)receiver message:(NSString * _Nullable)message __attribute__((swift_name("toCancellationException(_:message:)")));
- (NSString *)toDebugString __attribute__((swift_name("toDebugString()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<KotlinSequence> children __attribute__((swift_name("children")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) KotlinThrowable * _Nullable completionCause __attribute__((swift_name("completionCause")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) BOOL completionCauseHandled __attribute__((swift_name("completionCauseHandled")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive")));
@property (readonly) BOOL isCancelled __attribute__((swift_name("isCancelled")));
@property (readonly) BOOL isCompleted __attribute__((swift_name("isCompleted")));
@property (readonly) BOOL isCompletedExceptionally __attribute__((swift_name("isCompletedExceptionally")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) BOOL isScopedCoroutine __attribute__((swift_name("isScopedCoroutine")));
@property (readonly) id<KotlinCoroutineContextKey> key __attribute__((swift_name("key")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) id<SelectClause1> onAwaitInternal __attribute__((swift_name("onAwaitInternal")));
@property (readonly) id<SelectClause0> onJoin __attribute__((swift_name("onJoin")));
@property (readonly) id<Job> _Nullable parent __attribute__((swift_name("parent")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@protocol KotlinContinuation
@required
- (void)resumeWithResult:(id _Nullable)result __attribute__((swift_name("resumeWith(result:)")));
@property (readonly) id<KotlinCoroutineContext> context __attribute__((swift_name("context")));
@end

@protocol CoroutineScope
@required
@property (readonly) id<KotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
@end

@interface AbstractCoroutine<__contravariant T> : JobSupport <Job, KotlinContinuation, CoroutineScope>
- (instancetype)initWithParentContext:(id<KotlinCoroutineContext>)parentContext initParentJob:(BOOL)initParentJob active:(BOOL)active __attribute__((swift_name("init(parentContext:initParentJob:active:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithActive:(BOOL)active __attribute__((swift_name("init(active:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)afterResumeState:(id _Nullable)state __attribute__((swift_name("afterResume(state:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)cancellationExceptionMessage __attribute__((swift_name("cancellationExceptionMessage()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)onCancelledCause:(KotlinThrowable *)cause handled:(BOOL)handled __attribute__((swift_name("onCancelled(cause:handled:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)onCompletedValue:(T _Nullable)value __attribute__((swift_name("onCompleted(value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)onCompletionInternalState:(id _Nullable)state __attribute__((swift_name("onCompletionInternal(state:)")));
- (void)resumeWithResult:(id _Nullable)result __attribute__((swift_name("resumeWith(result:)")));
- (void)startStart:(CoroutineStart *)start receiver:(id _Nullable)receiver block:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("start(start:receiver:block:)")));
@property (readonly) id<KotlinCoroutineContext> context __attribute__((swift_name("context")));
@property (readonly) id<KotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive")));
@end

@protocol CancellableContinuation <KotlinContinuation>
@required
- (BOOL)cancelCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (void)completeResumeToken:(id)token __attribute__((swift_name("completeResume(token:)")));
- (void)doInitCancellability __attribute__((swift_name("doInitCancellability()")));
- (void)invokeOnCancellationHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCancellation(handler:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)resumeValue:(id _Nullable)value onCancellation:(void (^ _Nullable)(KotlinThrowable *))onCancellation __attribute__((swift_name("resume(value:onCancellation:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)resumeUndispatched:(CoroutineDispatcher *)receiver value:(id _Nullable)value __attribute__((swift_name("resumeUndispatched(_:value:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)resumeUndispatchedWithException:(CoroutineDispatcher *)receiver exception:(KotlinThrowable *)exception __attribute__((swift_name("resumeUndispatchedWithException(_:exception:)")));
- (id _Nullable)tryResumeValue:(id _Nullable)value idempotent:(id _Nullable)idempotent __attribute__((swift_name("tryResume(value:idempotent:)")));
- (id _Nullable)tryResumeValue:(id _Nullable)value idempotent:(id _Nullable)idempotent onCancellation:(void (^ _Nullable)(KotlinThrowable *))onCancellation __attribute__((swift_name("tryResume(value:idempotent:onCancellation:)")));
- (id _Nullable)tryResumeWithExceptionException:(KotlinThrowable *)exception __attribute__((swift_name("tryResumeWithException(exception:)")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive")));
@property (readonly) BOOL isCancelled __attribute__((swift_name("isCancelled")));
@property (readonly) BOOL isCompleted __attribute__((swift_name("isCompleted")));
@end

@protocol DisposableHandle
@required
- (void)dispose __attribute__((swift_name("dispose()")));
@end

@protocol ChildHandle <DisposableHandle>
@required
- (BOOL)childCancelledCause:(KotlinThrowable *)cause __attribute__((swift_name("childCancelled(cause:)")));
@property (readonly) id<Job> _Nullable parent __attribute__((swift_name("parent")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@interface KotlinAbstractCoroutineContextElement : Base <KotlinCoroutineContextElement>
- (instancetype)initWithKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer));
@property (readonly) id<KotlinCoroutineContextKey> key __attribute__((swift_name("key")));
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

@interface CoroutineDispatcher : KotlinAbstractCoroutineContextElement <KotlinContinuationInterceptor>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) CoroutineDispatcherKey *companion __attribute__((swift_name("companion")));
- (void)dispatchContext:(id<KotlinCoroutineContext>)context block:(id<Runnable>)block __attribute__((swift_name("dispatch(context:block:)")));
- (void)dispatchYieldContext:(id<KotlinCoroutineContext>)context block:(id<Runnable>)block __attribute__((swift_name("dispatchYield(context:block:)")));
- (id<KotlinContinuation>)interceptContinuationContinuation:(id<KotlinContinuation>)continuation __attribute__((swift_name("interceptContinuation(continuation:)")));
- (BOOL)isDispatchNeededContext:(id<KotlinCoroutineContext>)context __attribute__((swift_name("isDispatchNeeded(context:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (CoroutineDispatcher *)limitedParallelismParallelism:(int32_t)parallelism __attribute__((swift_name("limitedParallelism(parallelism:)")));
- (CoroutineDispatcher *)plusOther_:(CoroutineDispatcher *)other __attribute__((swift_name("plus(other_:)")));
- (void)releaseInterceptedContinuationContinuation:(id<KotlinContinuation>)continuation __attribute__((swift_name("releaseInterceptedContinuation(continuation:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

@interface CloseableCoroutineDispatcher : CoroutineDispatcher
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)close __attribute__((swift_name("close()")));
@end

@protocol Deferred <Job>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)awaitWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("await(completionHandler:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (id _Nullable)getCompleted __attribute__((swift_name("getCompleted()")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (KotlinThrowable * _Nullable)getCompletionExceptionOrNull __attribute__((swift_name("getCompletionExceptionOrNull()")));
@property (readonly) id<SelectClause1> onAwait __attribute__((swift_name("onAwait")));
@end

@protocol CompletableDeferred <Deferred>
@required
- (BOOL)completeValue:(id _Nullable)value __attribute__((swift_name("complete(value:)")));
- (BOOL)completeExceptionallyException:(KotlinThrowable *)exception __attribute__((swift_name("completeExceptionally(exception:)")));
@end

@protocol CompletableJob <Job>
@required
- (BOOL)complete __attribute__((swift_name("complete()")));
- (BOOL)completeExceptionallyException:(KotlinThrowable *)exception __attribute__((swift_name("completeExceptionally(exception:)")));
@end

@interface KotlinThrowable : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
- (KotlinArray<NSString *> *)getStackTrace __attribute__((swift_name("getStackTrace()")));
- (void)printStackTrace __attribute__((swift_name("printStackTrace()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) KotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (NSError *)asError __attribute__((swift_name("asError()")));
@end

@interface KotlinException : KotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

@interface KotlinRuntimeException : KotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
@interface CompletionHandlerException : KotlinRuntimeException
- (instancetype)initWithMessage:(NSString *)message cause:(KotlinThrowable *)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
@protocol CopyableThrowable
@required
- (KotlinThrowable * _Nullable)createCopy __attribute__((swift_name("createCopy()")));
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
- (instancetype)initWithBaseKey:(id<KotlinCoroutineContextKey>)baseKey safeCast:(E _Nullable (^)(id<KotlinCoroutineContextElement>))safeCast __attribute__((swift_name("init(baseKey:safeCast:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.ExperimentalStdlibApi
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CoroutineDispatcher.Key")))
@interface CoroutineDispatcherKey : KotlinAbstractCoroutineContextKey<id<KotlinContinuationInterceptor>, CoroutineDispatcher *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithBaseKey:(id<KotlinCoroutineContextKey>)baseKey safeCast:(id<KotlinCoroutineContextElement> _Nullable (^)(id<KotlinCoroutineContextElement>))safeCast __attribute__((swift_name("init(baseKey:safeCast:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) CoroutineDispatcherKey *shared __attribute__((swift_name("shared")));
@end

@protocol CoroutineExceptionHandler <KotlinCoroutineContextElement>
@required
- (void)handleExceptionContext:(id<KotlinCoroutineContext>)context exception:(KotlinThrowable *)exception __attribute__((swift_name("handleException(context:exception:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CoroutineExceptionHandlerKey : Base <KotlinCoroutineContextKey>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) CoroutineExceptionHandlerKey *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface CoroutineName : KotlinAbstractCoroutineContextElement
- (instancetype)initWithName:(NSString *)name __attribute__((swift_name("init(name:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) CoroutineNameKey *companion __attribute__((swift_name("companion")));
- (CoroutineName *)doCopyName:(NSString *)name __attribute__((swift_name("doCopy(name:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CoroutineName.Key")))
@interface CoroutineNameKey : Base <KotlinCoroutineContextKey>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) CoroutineNameKey *shared __attribute__((swift_name("shared")));
@end

@protocol KotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

@interface KotlinEnum<E> : Base <KotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) KotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
@interface CoroutineStart : KotlinEnum<CoroutineStart *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) CoroutineStart *default_ __attribute__((swift_name("default_")));
@property (class, readonly) CoroutineStart *lazy __attribute__((swift_name("lazy")));
@property (class, readonly) CoroutineStart *atomic __attribute__((swift_name("atomic")));
@property (class, readonly) CoroutineStart *undispatched __attribute__((swift_name("undispatched")));
+ (KotlinArray<CoroutineStart *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<CoroutineStart *> *entries __attribute__((swift_name("entries")));
- (void)invokeBlock:(id<KotlinSuspendFunction1>)block receiver:(id _Nullable)receiver completion:(id<KotlinContinuation>)completion __attribute__((swift_name("invoke(block:receiver:completion:)")));
@property (readonly) BOOL isLazy __attribute__((swift_name("isLazy")));
@end

@protocol Delay
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)delayTime:(int64_t)time completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("delay(time:completionHandler:)")));
- (id<DisposableHandle>)invokeOnTimeoutTimeMillis:(int64_t)timeMillis block:(id<Runnable>)block context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("invokeOnTimeout(timeMillis:block:context:)")));
- (void)scheduleResumeAfterDelayTimeMillis:(int64_t)timeMillis continuation:(id<CancellableContinuation>)continuation __attribute__((swift_name("scheduleResumeAfterDelay(timeMillis:continuation:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface Dispatchers : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dispatchers __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Dispatchers *shared __attribute__((swift_name("shared")));
@property (readonly) CoroutineDispatcher *Default __attribute__((swift_name("Default")));
@property (readonly) MainCoroutineDispatcher *Main __attribute__((swift_name("Main")));
@property (readonly) CoroutineDispatcher *Unconfined __attribute__((swift_name("Unconfined")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.DelicateCoroutinesApi
*/
__attribute__((objc_subclassing_restricted))
@interface GlobalScope : Base <CoroutineScope>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)globalScope __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) GlobalScope *shared __attribute__((swift_name("shared")));
@property (readonly) id<KotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
@end

__attribute__((objc_subclassing_restricted))
@interface JobKey : Base <KotlinCoroutineContextKey>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) JobKey *shared __attribute__((swift_name("shared")));
@end

@interface MainCoroutineDispatcher : CoroutineDispatcher
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (CoroutineDispatcher *)limitedParallelismParallelism:(int32_t)parallelism __attribute__((swift_name("limitedParallelism(parallelism:)")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString * _Nullable)toStringInternalImpl __attribute__((swift_name("toStringInternalImpl()")));
@property (readonly) MainCoroutineDispatcher *immediate __attribute__((swift_name("immediate")));
@end

__attribute__((objc_subclassing_restricted))
@interface NonCancellable : KotlinAbstractCoroutineContextElement <Job>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)nonCancellable __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) NonCancellable *shared __attribute__((swift_name("shared")));
- (id<ChildHandle>)attachChildChild:(id<ChildJob>)child __attribute__((swift_name("attachChild(child:)")));
- (BOOL)cancelCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (void)cancelCause_:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause_:)")));
- (KotlinCancellationException *)getCancellationException __attribute__((swift_name("getCancellationException()")));
- (id<DisposableHandle>)invokeOnCompletionHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(handler:)")));
- (id<DisposableHandle>)invokeOnCompletionOnCancelling:(BOOL)onCancelling invokeImmediately:(BOOL)invokeImmediately handler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(onCancelling:invokeImmediately:handler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)joinWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("join(completionHandler:)")));
- (BOOL)start __attribute__((swift_name("start()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<KotlinSequence> children __attribute__((swift_name("children")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive")));
@property (readonly) BOOL isCancelled __attribute__((swift_name("isCancelled")));
@property (readonly) BOOL isCompleted __attribute__((swift_name("isCompleted")));
@property (readonly) id<SelectClause0> onJoin __attribute__((swift_name("onJoin")));
@property (readonly) id<Job> _Nullable parent __attribute__((swift_name("parent")));
@end

__attribute__((objc_subclassing_restricted))
@interface NonDisposableHandle : Base <DisposableHandle, ChildHandle>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)nonDisposableHandle __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) NonDisposableHandle *shared __attribute__((swift_name("shared")));
- (BOOL)childCancelledCause:(KotlinThrowable *)cause __attribute__((swift_name("childCancelled(cause:)")));
- (void)dispose __attribute__((swift_name("dispose()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<Job> _Nullable parent __attribute__((swift_name("parent")));
@end

@protocol Runnable
@required
- (void)run __attribute__((swift_name("run()")));
@end

@interface KotlinIllegalStateException : KotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
@interface KotlinCancellationException : KotlinIllegalStateException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
@interface TimeoutCancellationException : KotlinCancellationException <CopyableThrowable>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (TimeoutCancellationException *)createCopy __attribute__((swift_name("createCopy()")));
@end

@protocol SendChannel
@required
- (BOOL)closeCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("close(cause:)")));
- (void)invokeOnCloseHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnClose(handler:)")));
- (BOOL)offerElement:(id _Nullable)element __attribute__((swift_name("offer(element:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)sendElement:(id _Nullable)element completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("send(element:completionHandler:)")));
- (id _Nullable)trySendElement:(id _Nullable)element __attribute__((swift_name("trySend(element:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.DelicateCoroutinesApi
*/
@property (readonly) BOOL isClosedForSend __attribute__((swift_name("isClosedForSend")));
@property (readonly) id<SelectClause2> onSend __attribute__((swift_name("onSend")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
@protocol BroadcastChannel <SendChannel>
@required
- (BOOL)cancelCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (void)cancelCause_:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause_:)")));
- (id<ReceiveChannel>)openSubscription __attribute__((swift_name("openSubscription()")));
@end

__attribute__((objc_subclassing_restricted))
@interface BufferOverflow : KotlinEnum<BufferOverflow *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) BufferOverflow *suspend __attribute__((swift_name("suspend")));
@property (class, readonly) BufferOverflow *dropOldest __attribute__((swift_name("dropOldest")));
@property (class, readonly) BufferOverflow *dropLatest __attribute__((swift_name("dropLatest")));
+ (KotlinArray<BufferOverflow *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<BufferOverflow *> *entries __attribute__((swift_name("entries")));
@end

@protocol ReceiveChannel
@required
- (void)cancel __attribute__((swift_name("cancel()")));
- (BOOL)cancelCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (void)cancelCause_:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause_:)")));
- (id<ChannelIterator>)iterator __attribute__((swift_name("iterator()")));
- (id _Nullable)poll __attribute__((swift_name("poll()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)receiveWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("receive(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)receiveCatchingWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("receiveCatching(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)receiveOrNullWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("receiveOrNull(completionHandler:)")));
- (id _Nullable)tryReceive __attribute__((swift_name("tryReceive()")));

/**
 * @note annotations
 *   kotlinx.coroutines.DelicateCoroutinesApi
*/
@property (readonly) BOOL isClosedForReceive __attribute__((swift_name("isClosedForReceive")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) id<SelectClause1> onReceive __attribute__((swift_name("onReceive")));
@property (readonly) id<SelectClause1> onReceiveCatching __attribute__((swift_name("onReceiveCatching")));
@property (readonly) id<SelectClause1> onReceiveOrNull __attribute__((swift_name("onReceiveOrNull")));
@end

@protocol Channel <SendChannel, ReceiveChannel>
@required
@end

__attribute__((objc_subclassing_restricted))
@interface ChannelFactory : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)factory __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ChannelFactory *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t BUFFERED __attribute__((swift_name("BUFFERED")));
@property (readonly) int32_t CONFLATED __attribute__((swift_name("CONFLATED")));
@property (readonly) NSString *DEFAULT_BUFFER_PROPERTY_NAME __attribute__((swift_name("DEFAULT_BUFFER_PROPERTY_NAME")));
@property (readonly) int32_t RENDEZVOUS __attribute__((swift_name("RENDEZVOUS")));
@property (readonly) int32_t UNLIMITED __attribute__((swift_name("UNLIMITED")));
@end

@protocol ChannelIterator
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)hasNextWithCompletionHandler:(void (^)(Boolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("hasNext(completionHandler:)")));
- (id _Nullable)next __attribute__((swift_name("next()")));

/**
 * @note annotations
 *   kotlin.jvm.JvmName(name="next")
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)next0WithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("next0(completionHandler:)")));
@end

@interface KotlinNoSuchElementException : KotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

__attribute__((objc_subclassing_restricted))
@interface ClosedReceiveChannelException : KotlinNoSuchElementException
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@end

__attribute__((objc_subclassing_restricted))
@interface ClosedSendChannelException : KotlinIllegalStateException
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
__attribute__((objc_subclassing_restricted))
@interface ConflatedBroadcastChannel<E> : Base <BroadcastChannel>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithValue:(E _Nullable)value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
- (BOOL)cancelCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (void)cancelCause_:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause_:)")));
- (BOOL)closeCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("close(cause:)")));
- (void)invokeOnCloseHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnClose(handler:)")));
- (BOOL)offerElement:(E _Nullable)element __attribute__((swift_name("offer(element:)")));
- (id<ReceiveChannel>)openSubscription __attribute__((swift_name("openSubscription()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)sendElement:(E _Nullable)element completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("send(element:completionHandler:)")));
- (id _Nullable)trySendElement:(E _Nullable)element __attribute__((swift_name("trySend(element:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.DelicateCoroutinesApi
*/
@property (readonly) BOOL isClosedForSend __attribute__((swift_name("isClosedForSend")));
@property (readonly) id<SelectClause2> onSend __attribute__((swift_name("onSend")));
@property (readonly) E _Nullable value __attribute__((swift_name("value")));
@property (readonly) E _Nullable valueOrNull __attribute__((swift_name("valueOrNull")));
@end

@protocol ProducerScope <CoroutineScope, SendChannel>
@required
@property (readonly) id<SendChannel> channel __attribute__((swift_name("channel")));
@end

@protocol Flow
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<FlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
@interface AbstractFlow<T> : Base <Flow>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<FlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectSafelyCollector:(id<FlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collectSafely(collector:completionHandler:)")));
@end

@protocol FlowCollector
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(id _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
@end

@protocol SharedFlow <Flow>
@required
@property (readonly) NSArray<id> *replayCache __attribute__((swift_name("replayCache")));
@end

@protocol MutableSharedFlow <SharedFlow, FlowCollector>
@required

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)resetReplayCache __attribute__((swift_name("resetReplayCache()")));
- (BOOL)tryEmitValue:(id _Nullable)value __attribute__((swift_name("tryEmit(value:)")));
@property (readonly) id<StateFlow> subscriptionCount __attribute__((swift_name("subscriptionCount")));
@end

@protocol StateFlow <SharedFlow>
@required
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end

@protocol MutableStateFlow <StateFlow, MutableSharedFlow>
@required
- (void)setValue:(id _Nullable)value __attribute__((swift_name("setValue(_:)")));
- (BOOL)compareAndSetExpect:(id _Nullable)expect update:(id _Nullable)update __attribute__((swift_name("compareAndSet(expect:update:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SharingCommand : KotlinEnum<SharingCommand *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharingCommand *start __attribute__((swift_name("start")));
@property (class, readonly) SharingCommand *stop __attribute__((swift_name("stop")));
@property (class, readonly) SharingCommand *stopAndResetReplayCache __attribute__((swift_name("stopAndResetReplayCache")));
+ (KotlinArray<SharingCommand *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharingCommand *> *entries __attribute__((swift_name("entries")));
@end

@protocol SharingStarted
@required
- (id<Flow>)commandSubscriptionCount:(id<StateFlow>)subscriptionCount __attribute__((swift_name("command(subscriptionCount:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SharingStartedCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharingStartedCompanion *shared __attribute__((swift_name("shared")));
- (id<SharingStarted>)WhileSubscribedStopTimeoutMillis:(int64_t)stopTimeoutMillis replayExpirationMillis:(int64_t)replayExpirationMillis __attribute__((swift_name("WhileSubscribed(stopTimeoutMillis:replayExpirationMillis:)")));
@property (readonly) id<SharingStarted> Eagerly __attribute__((swift_name("Eagerly")));
@property (readonly) id<SharingStarted> Lazily __attribute__((swift_name("Lazily")));
@end

@protocol FusibleFlow <Flow>
@required
- (id<Flow>)fuseContext:(id<KotlinCoroutineContext>)context capacity:(int32_t)capacity onBufferOverflow:(BufferOverflow *)onBufferOverflow __attribute__((swift_name("fuse(context:capacity:onBufferOverflow:)")));
@end

@interface ChannelFlow<T> : Base <FusibleFlow>
- (instancetype)initWithContext:(id<KotlinCoroutineContext>)context capacity:(int32_t)capacity onBufferOverflow:(BufferOverflow *)onBufferOverflow __attribute__((swift_name("init(context:capacity:onBufferOverflow:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString * _Nullable)additionalToStringProps __attribute__((swift_name("additionalToStringProps()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<FlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)collectToScope:(id<ProducerScope>)scope completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collectTo(scope:completionHandler:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (ChannelFlow<T> *)createContext:(id<KotlinCoroutineContext>)context capacity:(int32_t)capacity onBufferOverflow:(BufferOverflow *)onBufferOverflow __attribute__((swift_name("create(context:capacity:onBufferOverflow:)")));
- (id<Flow> _Nullable)dropChannelOperators __attribute__((swift_name("dropChannelOperators()")));
- (id<Flow>)fuseContext:(id<KotlinCoroutineContext>)context capacity:(int32_t)capacity onBufferOverflow:(BufferOverflow *)onBufferOverflow __attribute__((swift_name("fuse(context:capacity:onBufferOverflow:)")));
- (id<ReceiveChannel>)produceImplScope:(id<CoroutineScope>)scope __attribute__((swift_name("produceImpl(scope:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t capacity __attribute__((swift_name("capacity")));
@property (readonly) id<KotlinCoroutineContext> context __attribute__((swift_name("context")));
@property (readonly) BufferOverflow *onBufferOverflow __attribute__((swift_name("onBufferOverflow")));
@end

__attribute__((objc_subclassing_restricted))
@interface SendingCollector<T> : Base <FlowCollector>
- (instancetype)initWithChannel:(id<SendChannel>)channel __attribute__((swift_name("init(channel:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(T _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
@end

@interface OpDescriptor : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id _Nullable)performAffected:(id _Nullable)affected __attribute__((swift_name("perform(affected:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) AtomicOp<id> * _Nullable atomicOp __attribute__((swift_name("atomicOp")));
@end

@interface AtomicOp<__contravariant T> : OpDescriptor
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)completeAffected:(T _Nullable)affected failure:(id _Nullable)failure __attribute__((swift_name("complete(affected:failure:)")));
- (id _Nullable)performAffected:(id _Nullable)affected __attribute__((swift_name("perform(affected:)")));
- (id _Nullable)prepareAffected:(T _Nullable)affected __attribute__((swift_name("prepare(affected:)")));
@property (readonly) AtomicOp<id> *atomicOp __attribute__((swift_name("atomicOp")));
@end

@interface LockFreeLinkedListNode : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)addLastNode:(LockFreeLinkedListNode *)node __attribute__((swift_name("addLast(node:)")));
- (BOOL)addLastIfNode:(LockFreeLinkedListNode *)node condition:(Boolean *(^)(void))condition __attribute__((swift_name("addLastIf(node:condition:)")));
- (BOOL)addOneIfEmptyNode:(LockFreeLinkedListNode *)node __attribute__((swift_name("addOneIfEmpty(node:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (LockFreeLinkedListNode * _Nullable)nextIfRemoved __attribute__((swift_name("nextIfRemoved()")));
- (BOOL)remove __attribute__((swift_name("remove()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL isRemoved __attribute__((swift_name("isRemoved")));
@property (readonly, getter=next_) id next __attribute__((swift_name("next")));
@property (readonly) LockFreeLinkedListNode *nextNode __attribute__((swift_name("nextNode")));
@property (readonly) LockFreeLinkedListNode *prevNode __attribute__((swift_name("prevNode")));
@end

@interface LockFreeLinkedListHead : LockFreeLinkedListNode
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)forEachBlock:(void (^)(LockFreeLinkedListNode *))block __attribute__((swift_name("forEach(block:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (LockFreeLinkedListNode * _Nullable)nextIfRemoved __attribute__((swift_name("nextIfRemoved()")));
- (BOOL)remove __attribute__((swift_name("remove()")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) BOOL isRemoved __attribute__((swift_name("isRemoved")));
@end

@protocol MainDispatcherFactory
@required
- (MainCoroutineDispatcher *)createDispatcherAllFactories:(NSArray<id<MainDispatcherFactory>> *)allFactories __attribute__((swift_name("createDispatcher(allFactories:)")));
- (NSString * _Nullable)hintOnError __attribute__((swift_name("hintOnError()")));
@property (readonly) int32_t loadPriority __attribute__((swift_name("loadPriority")));
@end

__attribute__((objc_subclassing_restricted))
@interface SynchronizedObject : Base
@end

@interface ThreadSafeHeap<T> : SynchronizedObject
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)addLastNode:(T)node __attribute__((swift_name("addLast(node:)")));
- (BOOL)addLastIfNode:(T)node cond:(Boolean *(^)(T _Nullable))cond __attribute__((swift_name("addLastIf(node:cond:)")));
- (T _Nullable)findPredicate:(Boolean *(^)(T))predicate __attribute__((swift_name("find(predicate:)")));
- (T _Nullable)peek __attribute__((swift_name("peek()")));
- (BOOL)removeNode:(T)node __attribute__((swift_name("remove(node:)")));
- (T _Nullable)removeFirstIfPredicate:(Boolean *(^)(T))predicate __attribute__((swift_name("removeFirstIf(predicate:)")));
- (T _Nullable)removeFirstOrNull __attribute__((swift_name("removeFirstOrNull()")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

@protocol ThreadSafeHeapNode
@required
@property ThreadSafeHeap<id> * _Nullable heap __attribute__((swift_name("heap")));
@property int32_t index __attribute__((swift_name("index")));
@end

@protocol SelectBuilder
@required
- (void)invoke:(id<SelectClause0>)receiver block:(id<KotlinSuspendFunction0>)block __attribute__((swift_name("invoke(_:block:)")));
- (void)invoke:(id<SelectClause1>)receiver block_:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("invoke(_:block_:)")));
- (void)invoke:(id<SelectClause2>)receiver block__:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("invoke(_:block__:)")));
- (void)invoke:(id<SelectClause2>)receiver param:(id _Nullable)param block:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("invoke(_:param:block:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)onTimeoutTimeMillis:(int64_t)timeMillis block:(id<KotlinSuspendFunction0>)block __attribute__((swift_name("onTimeout(timeMillis:block:)")));
@end

@protocol SelectClause
@required
@property (readonly) id clauseObject __attribute__((swift_name("clauseObject")));
@property (readonly) KotlinUnit *(^(^ _Nullable onCancellationConstructor)(id<SelectInstance>, id _Nullable, id _Nullable))(KotlinThrowable *) __attribute__((swift_name("onCancellationConstructor")));
@property (readonly) id _Nullable (^processResFunc)(id, id _Nullable, id _Nullable) __attribute__((swift_name("processResFunc")));
@property (readonly) void (^regFunc)(id, id<SelectInstance>, id _Nullable) __attribute__((swift_name("regFunc")));
@end

@protocol SelectClause0 <SelectClause>
@required
@end

@protocol SelectClause1 <SelectClause>
@required
@end

@protocol SelectClause2 <SelectClause>
@required
@end

@protocol SelectInstance
@required
- (void)disposeOnCompletionDisposableHandle:(id<DisposableHandle>)disposableHandle __attribute__((swift_name("disposeOnCompletion(disposableHandle:)")));
- (void)selectInRegistrationPhaseInternalResult:(id _Nullable)internalResult __attribute__((swift_name("selectInRegistrationPhase(internalResult:)")));
- (BOOL)trySelectClauseObject:(id)clauseObject result:(id _Nullable)result __attribute__((swift_name("trySelect(clauseObject:result:)")));
@property (readonly) id<KotlinCoroutineContext> context __attribute__((swift_name("context")));
@end

@protocol Mutex
@required
- (BOOL)holdsLockOwner:(id)owner __attribute__((swift_name("holdsLock(owner:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)lockOwner:(id _Nullable)owner completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("lock(owner:completionHandler:)")));
- (BOOL)tryLockOwner:(id _Nullable)owner __attribute__((swift_name("tryLock(owner:)")));
- (void)unlockOwner:(id _Nullable)owner __attribute__((swift_name("unlock(owner:)")));
@property (readonly) BOOL isLocked __attribute__((swift_name("isLocked")));
@property (readonly) id<SelectClause2> onLock __attribute__((swift_name("onLock")));
@end

@protocol Semaphore
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)acquireWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("acquire(completionHandler:)")));
- (void)release_ __attribute__((swift_name("release()")));
- (BOOL)tryAcquire __attribute__((swift_name("tryAcquire()")));
@property (readonly) int32_t availablePermits __attribute__((swift_name("availablePermits")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinArray<T> : Base
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<KotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

@interface KotlinArray (Extensions)
- (id<Flow>)asFlow __attribute__((swift_name("asFlow()")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinIntArray : Base
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(Int *(^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int32_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (KotlinIntIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int32_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

@interface KotlinIntArray (Extensions)
- (id<Flow>)asFlow __attribute__((swift_name("asFlow()")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinLongArray : Base
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(Long *(^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int64_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (KotlinLongIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int64_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

@interface KotlinLongArray (Extensions)
- (id<Flow>)asFlow __attribute__((swift_name("asFlow()")));
@end

@protocol KotlinIterable
@required
- (id<KotlinIterator>)iterator __attribute__((swift_name("iterator()")));
@end

@interface KotlinIntProgression : Base <KotlinIterable>
@property (class, readonly, getter=companion) KotlinIntProgressionCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty_ __attribute__((swift_name("isEmpty()")));
- (KotlinIntIterator *)iterator __attribute__((swift_name("iterator()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t first __attribute__((swift_name("first")));
@property (readonly) int32_t last __attribute__((swift_name("last")));
@property (readonly) int32_t step __attribute__((swift_name("step")));
@end

@protocol KotlinClosedRange
@required
- (BOOL)containsValue:(id)value __attribute__((swift_name("contains(value:)")));
- (BOOL)isEmpty_ __attribute__((swift_name("isEmpty()")));
@property (readonly) id endInclusive __attribute__((swift_name("endInclusive")));
@property (readonly, getter=start_) id start __attribute__((swift_name("start")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
@protocol KotlinOpenEndRange
@required
- (BOOL)containsValue_:(id)value __attribute__((swift_name("contains(value_:)")));
- (BOOL)isEmpty_ __attribute__((swift_name("isEmpty()")));
@property (readonly) id endExclusive __attribute__((swift_name("endExclusive")));
@property (readonly, getter=start_) id start __attribute__((swift_name("start")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinIntRange : KotlinIntProgression <KotlinClosedRange, KotlinOpenEndRange>
- (instancetype)initWithStart:(int32_t)start endInclusive:(int32_t)endInclusive __attribute__((swift_name("init(start:endInclusive:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) KotlinIntRangeCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)containsValue:(Int *)value __attribute__((swift_name("contains(value:)")));
- (BOOL)containsValue_:(Int *)value __attribute__((swift_name("contains(value_:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty_ __attribute__((swift_name("isEmpty()")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
@property (readonly) Int *endExclusive __attribute__((swift_name("endExclusive")));
@property (readonly) Int *endInclusive __attribute__((swift_name("endInclusive")));
@property (readonly, getter=start_) Int *start __attribute__((swift_name("start")));
@end

@interface KotlinIntRange (Extensions)
- (id<Flow>)asFlow __attribute__((swift_name("asFlow()")));
@end

@interface KotlinLongProgression : Base <KotlinIterable>
@property (class, readonly, getter=companion) KotlinLongProgressionCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty_ __attribute__((swift_name("isEmpty()")));
- (KotlinLongIterator *)iterator __attribute__((swift_name("iterator()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t first __attribute__((swift_name("first")));
@property (readonly) int64_t last __attribute__((swift_name("last")));
@property (readonly) int64_t step __attribute__((swift_name("step")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinLongRange : KotlinLongProgression <KotlinClosedRange, KotlinOpenEndRange>
- (instancetype)initWithStart:(int64_t)start endInclusive:(int64_t)endInclusive __attribute__((swift_name("init(start:endInclusive:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) KotlinLongRangeCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)containsValue:(Long *)value __attribute__((swift_name("contains(value:)")));
- (BOOL)containsValue_:(Long *)value __attribute__((swift_name("contains(value_:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty_ __attribute__((swift_name("isEmpty()")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
@property (readonly) Long *endExclusive __attribute__((swift_name("endExclusive")));
@property (readonly) Long *endInclusive __attribute__((swift_name("endInclusive")));
@property (readonly, getter=start_) Long *start __attribute__((swift_name("start")));
@end

@interface KotlinLongRange (Extensions)
- (id<Flow>)asFlow __attribute__((swift_name("asFlow()")));
@end

@interface CoroutineDispatcher (Extensions)

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeBlock:(id<KotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(block:completionHandler:)")));
@end

@interface Dispatchers (Extensions)
@property (readonly) CoroutineDispatcher *IO __attribute__((swift_name("IO")));
@end

@interface SharingStartedCompanion (Extensions)
- (id<SharingStarted>)WhileSubscribedStopTimeout:(int64_t)stopTimeout replayExpiration:(int64_t)replayExpiration __attribute__((swift_name("WhileSubscribed(stopTimeout:replayExpiration:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface AwaitKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)awaitAll:(id)receiver completionHandler:(void (^)(NSArray<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("awaitAll(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)awaitAllDeferreds:(KotlinArray<id<Deferred>> *)deferreds completionHandler:(void (^)(NSArray<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("awaitAll(deferreds:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)joinAll:(id)receiver completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("joinAll(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)joinAllJobs:(KotlinArray<id<Job>> *)jobs completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("joinAll(jobs:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface BroadcastKt : Base

/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id<BroadcastChannel>)broadcast:(id<ReceiveChannel>)receiver capacity:(int32_t)capacity start:(CoroutineStart *)start __attribute__((swift_name("broadcast(_:capacity:start:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id<BroadcastChannel>)broadcast:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context capacity:(int32_t)capacity start:(CoroutineStart *)start onCompletion:(void (^ _Nullable)(KotlinThrowable * _Nullable))onCompletion block:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("broadcast(_:context:capacity:start:onCompletion:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface BroadcastChannelKt : Base

/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id<BroadcastChannel>)BroadcastChannelCapacity:(int32_t)capacity __attribute__((swift_name("BroadcastChannel(capacity:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface Builders_commonKt : Base
+ (id<Deferred>)async:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context start:(CoroutineStart *)start block:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("async(_:context:start:block:)")));
+ (id<Job>)launch:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context start:(CoroutineStart *)start block:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("launch(_:context:start:block:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)withContextContext:(id<KotlinCoroutineContext>)context block:(id<KotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withContext(context:block:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface BuildersKt : Base
+ (id<Flow>)asFlow:(id _Nullable (^)(void))receiver __attribute__((swift_name("asFlow(_:)")));
+ (id<Flow>)asFlow_:(id)receiver __attribute__((swift_name("asFlow(__:)")));
+ (id<Flow>)asFlow__:(id<KotlinIterator>)receiver __attribute__((swift_name("asFlow(___:)")));
+ (id<Flow>)asFlow___:(id<KotlinSuspendFunction0>)receiver __attribute__((swift_name("asFlow(____:)")));
+ (id<Flow>)asFlow____:(id<KotlinSequence>)receiver __attribute__((swift_name("asFlow(_____:)")));
+ (id<Flow>)callbackFlowBlock:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("callbackFlow(block:)")));
+ (id<Flow>)channelFlowBlock:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("channelFlow(block:)")));
+ (id<Flow>)emptyFlow __attribute__((swift_name("emptyFlow()")));
+ (id<Flow>)flowBlock:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("flow(block:)")));
+ (id<Flow>)flowOfValue:(id _Nullable)value __attribute__((swift_name("flowOf(value:)")));
+ (id<Flow>)flowOfElements:(KotlinArray<id> *)elements __attribute__((swift_name("flowOf(elements:)")));
+ (id _Nullable)runBlockingContext:(id<KotlinCoroutineContext>)context block:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("runBlocking(context:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CancellableKt : Base
+ (void)startCoroutineCancellable:(id<KotlinSuspendFunction0>)receiver completion:(id<KotlinContinuation>)completion __attribute__((swift_name("startCoroutineCancellable(_:completion:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CancellableContinuationKt : Base
+ (void)disposeOnCancellation:(id<CancellableContinuation>)receiver handle:(id<DisposableHandle>)handle __attribute__((swift_name("disposeOnCancellation(_:handle:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)suspendCancellableCoroutineBlock:(void (^)(id<CancellableContinuation>))block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("suspendCancellableCoroutine(block:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ChannelKt : Base
+ (id<Channel>)ChannelCapacity:(int32_t)capacity __attribute__((swift_name("Channel(capacity:)")));
+ (id<Channel>)ChannelCapacity:(int32_t)capacity onBufferOverflow:(BufferOverflow *)onBufferOverflow onUndeliveredElement:(void (^ _Nullable)(id _Nullable))onUndeliveredElement __attribute__((swift_name("Channel(capacity:onBufferOverflow:onUndeliveredElement:)")));
+ (id _Nullable)getOrElse:(id _Nullable)receiver onFailure:(id _Nullable (^)(KotlinThrowable * _Nullable))onFailure __attribute__((swift_name("getOrElse(_:onFailure:)")));
+ (id _Nullable)onClosed:(id _Nullable)receiver action:(void (^)(KotlinThrowable * _Nullable))action __attribute__((swift_name("onClosed(_:action:)")));
+ (id _Nullable)onFailure:(id _Nullable)receiver action:(void (^)(KotlinThrowable * _Nullable))action __attribute__((swift_name("onFailure(_:action:)")));
+ (id _Nullable)onSuccess:(id _Nullable)receiver action:(void (^)(id _Nullable))action __attribute__((swift_name("onSuccess(_:action:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface Channels_commonKt : Base
+ (id _Nullable)consume:(id<ReceiveChannel>)receiver block:(id _Nullable (^)(id<ReceiveChannel>))block __attribute__((swift_name("consume(_:block:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)consumeEach:(id<ReceiveChannel>)receiver action:(void (^)(id _Nullable))action completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("consumeEach(_:action:completionHandler:)")));
+ (id<SelectClause1>)onReceiveOrNull:(id<ReceiveChannel>)receiver __attribute__((swift_name("onReceiveOrNull(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)receiveOrNull:(id<ReceiveChannel>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("receiveOrNull(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toList:(id<ReceiveChannel>)receiver completionHandler:(void (^)(NSArray<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toList(_:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ChannelsKt : Base
+ (id<Flow>)asFlow:(id<BroadcastChannel>)receiver __attribute__((swift_name("asFlow(_:)")));
+ (id<Flow>)consumeAsFlow:(id<ReceiveChannel>)receiver __attribute__((swift_name("consumeAsFlow(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)emitAll:(id<FlowCollector>)receiver channel:(id<ReceiveChannel>)channel completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emitAll(_:channel:completionHandler:)")));
+ (id<ReceiveChannel>)produceIn:(id<Flow>)receiver scope:(id<CoroutineScope>)scope __attribute__((swift_name("produceIn(_:scope:)")));
+ (id<Flow>)receiveAsFlow:(id<ReceiveChannel>)receiver __attribute__((swift_name("receiveAsFlow(_:)")));
+ (void)sendBlocking:(id<SendChannel>)receiver element:(id _Nullable)element __attribute__((swift_name("sendBlocking(_:element:)")));
+ (id _Nullable)trySendBlocking:(id<SendChannel>)receiver element:(id _Nullable)element __attribute__((swift_name("trySendBlocking(_:element:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CollectKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)collect:(id<Flow>)receiver completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)collect:(id<Flow>)receiver action:(id<KotlinSuspendFunction1>)action completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(_:action:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)collectIndexed:(id<Flow>)receiver action:(id<KotlinSuspendFunction2>)action completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collectIndexed(_:action:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)collectLatest:(id<Flow>)receiver action:(id<KotlinSuspendFunction1>)action completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collectLatest(_:action:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)emitAll:(id<FlowCollector>)receiver flow:(id<Flow>)flow completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emitAll(_:flow:completionHandler:)")));
+ (id<Job>)launchIn:(id<Flow>)receiver scope:(id<CoroutineScope>)scope __attribute__((swift_name("launchIn(_:scope:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CollectionKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toCollection:(id<Flow>)receiver destination:(id)destination completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toCollection(_:destination:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toList:(id<Flow>)receiver destination:(NSMutableArray<id> *)destination completionHandler:(void (^)(NSArray<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toList(_:destination:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toSet:(id<Flow>)receiver destination:(MutableSet<id> *)destination completionHandler:(void (^)(NSSet<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toSet(_:destination:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CompletableDeferredKt : Base
+ (id<CompletableDeferred>)CompletableDeferredValue:(id _Nullable)value __attribute__((swift_name("CompletableDeferred(value:)")));
+ (id<CompletableDeferred>)CompletableDeferredParent:(id<Job> _Nullable)parent __attribute__((swift_name("CompletableDeferred(parent:)")));
+ (BOOL)completeWith:(id<CompletableDeferred>)receiver result:(id _Nullable)result __attribute__((swift_name("completeWith(_:result:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ContextKt : Base
+ (id<Flow>)buffer:(id<Flow>)receiver capacity:(int32_t)capacity __attribute__((swift_name("buffer(_:capacity:)")));
+ (id<Flow>)buffer:(id<Flow>)receiver capacity:(int32_t)capacity onBufferOverflow:(BufferOverflow *)onBufferOverflow __attribute__((swift_name("buffer(_:capacity:onBufferOverflow:)")));
+ (id<Flow>)cancellable:(id<Flow>)receiver __attribute__((swift_name("cancellable(_:)")));
+ (id<Flow>)conflate:(id<Flow>)receiver __attribute__((swift_name("conflate(_:)")));
+ (id<Flow>)flowOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("flowOn(_:context:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CoroutineContextKt : Base
+ (id<KotlinCoroutineContext>)doNewCoroutineContext:(id<KotlinCoroutineContext>)receiver addedContext:(id<KotlinCoroutineContext>)addedContext __attribute__((swift_name("doNewCoroutineContext(_:addedContext:)")));
+ (id<KotlinCoroutineContext>)doNewCoroutineContext:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("doNewCoroutineContext(_:context:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CoroutineExceptionHandlerKt : Base
+ (id<CoroutineExceptionHandler>)CoroutineExceptionHandlerHandler:(void (^)(id<KotlinCoroutineContext>, KotlinThrowable *))handler __attribute__((swift_name("CoroutineExceptionHandler(handler:)")));
+ (void)handleCoroutineExceptionContext:(id<KotlinCoroutineContext>)context exception:(KotlinThrowable *)exception __attribute__((swift_name("handleCoroutineException(context:exception:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CoroutineScopeKt : Base
+ (BOOL)isActive:(id<CoroutineScope>)receiver __attribute__((swift_name("isActive(_:)")));
+ (id<CoroutineScope>)CoroutineScopeContext:(id<KotlinCoroutineContext>)context __attribute__((swift_name("CoroutineScope(context:)")));
+ (id<CoroutineScope>)MainScope __attribute__((swift_name("MainScope()")));
+ (void)cancel:(id<CoroutineScope>)receiver cause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(_:cause:)")));
+ (void)cancel:(id<CoroutineScope>)receiver message:(NSString *)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(_:message:cause:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)coroutineScopeBlock:(id<KotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("coroutineScope(block:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)currentCoroutineContextWithCompletionHandler:(void (^)(id<KotlinCoroutineContext> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("currentCoroutineContext(completionHandler:)")));
+ (void)ensureActive:(id<CoroutineScope>)receiver __attribute__((swift_name("ensureActive(_:)")));
+ (id<CoroutineScope>)plus:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("plus(_:context:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CountKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)count:(id<Flow>)receiver completionHandler:(void (^)(Int * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("count(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)count:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction1>)predicate completionHandler:(void (^)(Int * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("count(_:predicate:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DelayKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)awaitCancellationWithCompletionHandler:(void (^)(KotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("awaitCancellation(completionHandler:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.FlowPreview
*/
+ (id<Flow>)debounce:(id<Flow>)receiver timeoutMillis:(Long *(^)(id _Nullable))timeoutMillis __attribute__((swift_name("debounce(_:timeoutMillis:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.FlowPreview
 *   kotlin.jvm.JvmName(name="debounceDuration")
*/
+ (id<Flow>)debounce:(id<Flow>)receiver timeout:(id (^)(id _Nullable))timeout __attribute__((swift_name("debounce(_:timeout:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.FlowPreview
*/
+ (id<Flow>)debounce:(id<Flow>)receiver timeoutMillis_:(int64_t)timeoutMillis __attribute__((swift_name("debounce(_:timeoutMillis_:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.FlowPreview
*/
+ (id<Flow>)debounce:(id<Flow>)receiver timeout_:(int64_t)timeout __attribute__((swift_name("debounce(_:timeout_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)delayTimeMillis:(int64_t)timeMillis completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("delay(timeMillis:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)delayDuration:(int64_t)duration completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("delay(duration:completionHandler:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.FlowPreview
*/
+ (id<Flow>)sample:(id<Flow>)receiver periodMillis:(int64_t)periodMillis __attribute__((swift_name("sample(_:periodMillis:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.FlowPreview
*/
+ (id<Flow>)sample:(id<Flow>)receiver period:(int64_t)period __attribute__((swift_name("sample(_:period:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.FlowPreview
*/
+ (id<Flow>)timeout:(id<Flow>)receiver timeout:(int64_t)timeout __attribute__((swift_name("timeout(_:timeout:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DeprecatedKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)any:(id<ReceiveChannel>)receiver completionHandler:(void (^)(Boolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("any(_:completionHandler:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id _Nullable)consume:(id<BroadcastChannel>)receiver block:(id _Nullable (^)(id<ReceiveChannel>))block __attribute__((swift_name("consume(_:block:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)consumeEach:(id<BroadcastChannel>)receiver action:(void (^)(id _Nullable))action completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("consumeEach(_:action:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)count:(id<ReceiveChannel>)receiver completionHandler:(void (^)(Int * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("count(_:completionHandler:)")));
+ (id<ReceiveChannel>)distinct:(id<ReceiveChannel>)receiver __attribute__((swift_name("distinct(_:)")));
+ (id<ReceiveChannel>)drop:(id<ReceiveChannel>)receiver n:(int32_t)n context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("drop(_:n:context:)")));
+ (id<ReceiveChannel>)dropWhile:(id<ReceiveChannel>)receiver context:(id<KotlinCoroutineContext>)context predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("dropWhile(_:context:predicate:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)elementAt:(id<ReceiveChannel>)receiver index:(int32_t)index completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("elementAt(_:index:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)elementAtOrNull:(id<ReceiveChannel>)receiver index:(int32_t)index completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("elementAtOrNull(_:index:completionHandler:)")));
+ (id<ReceiveChannel>)filterIndexed:(id<ReceiveChannel>)receiver context:(id<KotlinCoroutineContext>)context predicate:(id<KotlinSuspendFunction2>)predicate __attribute__((swift_name("filterIndexed(_:context:predicate:)")));
+ (id<ReceiveChannel>)filterNot:(id<ReceiveChannel>)receiver context:(id<KotlinCoroutineContext>)context predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("filterNot(_:context:predicate:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)filterNotNullTo:(id<ReceiveChannel>)receiver destination:(id)destination completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("filterNotNullTo(_:destination:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)filterNotNullTo:(id<ReceiveChannel>)receiver destination:(id<SendChannel>)destination completionHandler_:(void (^)(id<SendChannel> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("filterNotNullTo(_:destination:completionHandler_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)first:(id<ReceiveChannel>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("first(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)firstOrNull:(id<ReceiveChannel>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("firstOrNull(_:completionHandler:)")));
+ (id<ReceiveChannel>)flatMap:(id<ReceiveChannel>)receiver context:(id<KotlinCoroutineContext>)context transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("flatMap(_:context:transform:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)indexOf:(id<ReceiveChannel>)receiver element:(id _Nullable)element completionHandler:(void (^)(Int * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("indexOf(_:element:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)last:(id<ReceiveChannel>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("last(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)lastIndexOf:(id<ReceiveChannel>)receiver element:(id _Nullable)element completionHandler:(void (^)(Int * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("lastIndexOf(_:element:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)lastOrNull:(id<ReceiveChannel>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("lastOrNull(_:completionHandler:)")));
+ (id<ReceiveChannel>)mapIndexedNotNull:(id<ReceiveChannel>)receiver context:(id<KotlinCoroutineContext>)context transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("mapIndexedNotNull(_:context:transform:)")));
+ (id<ReceiveChannel>)mapNotNull:(id<ReceiveChannel>)receiver context:(id<KotlinCoroutineContext>)context transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("mapNotNull(_:context:transform:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)maxWith:(id<ReceiveChannel>)receiver comparator:(id<KotlinComparator>)comparator completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("maxWith(_:comparator:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)minWith:(id<ReceiveChannel>)receiver comparator:(id<KotlinComparator>)comparator completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("minWith(_:comparator:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)none:(id<ReceiveChannel>)receiver completionHandler:(void (^)(Boolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("none(_:completionHandler:)")));
+ (id<ReceiveChannel>)requireNoNulls:(id<ReceiveChannel>)receiver __attribute__((swift_name("requireNoNulls(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)single:(id<ReceiveChannel>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("single(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)singleOrNull:(id<ReceiveChannel>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("singleOrNull(_:completionHandler:)")));
+ (id<ReceiveChannel>)take:(id<ReceiveChannel>)receiver n:(int32_t)n context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("take(_:n:context:)")));
+ (id<ReceiveChannel>)takeWhile:(id<ReceiveChannel>)receiver context:(id<KotlinCoroutineContext>)context predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("takeWhile(_:context:predicate:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toMap:(id<ReceiveChannel>)receiver completionHandler:(void (^)(NSDictionary<id, id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toMap(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toMutableList:(id<ReceiveChannel>)receiver completionHandler:(void (^)(NSMutableArray<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toMutableList(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toSet:(id<ReceiveChannel>)receiver completionHandler:(void (^)(NSSet<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toSet(_:completionHandler:)")));
+ (id<ReceiveChannel>)withIndex:(id<ReceiveChannel>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("withIndex(_:context:)")));
+ (id<ReceiveChannel>)zip:(id<ReceiveChannel>)receiver other:(id<ReceiveChannel>)other __attribute__((swift_name("zip(_:other:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DispatchedContinuationKt : Base
+ (void)resumeCancellableWith:(id<KotlinContinuation>)receiver result:(id _Nullable)result onCancellation:(void (^ _Nullable)(KotlinThrowable *))onCancellation __attribute__((swift_name("resumeCancellableWith(_:result:onCancellation:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DistinctKt : Base
+ (id<Flow>)distinctUntilChanged:(id<Flow>)receiver __attribute__((swift_name("distinctUntilChanged(_:)")));
+ (id<Flow>)distinctUntilChanged:(id<Flow>)receiver areEquivalent:(Boolean *(^)(id _Nullable, id _Nullable))areEquivalent __attribute__((swift_name("distinctUntilChanged(_:areEquivalent:)")));
+ (id<Flow>)distinctUntilChangedBy:(id<Flow>)receiver keySelector:(id _Nullable (^)(id _Nullable))keySelector __attribute__((swift_name("distinctUntilChangedBy(_:keySelector:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface EmittersKt : Base
+ (id<Flow>)onCompletion:(id<Flow>)receiver action:(id<KotlinSuspendFunction2>)action __attribute__((swift_name("onCompletion(_:action:)")));
+ (id<Flow>)onEmpty:(id<Flow>)receiver action:(id<KotlinSuspendFunction1>)action __attribute__((swift_name("onEmpty(_:action:)")));
+ (id<Flow>)onStart:(id<Flow>)receiver action:(id<KotlinSuspendFunction1>)action __attribute__((swift_name("onStart(_:action:)")));
+ (id<Flow>)transform:(id<Flow>)receiver transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("transform(_:transform:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ErrorsKt : Base
+ (id<Flow>)catch:(id<Flow>)receiver action:(id<KotlinSuspendFunction2>)action __attribute__((swift_name("catch(_:action:)")));
+ (id<Flow>)retry:(id<Flow>)receiver retries:(int64_t)retries predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("retry(_:retries:predicate:)")));
+ (id<Flow>)retryWhen:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction3>)predicate __attribute__((swift_name("retryWhen(_:predicate:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ExceptionsKt : Base
+ (KotlinCancellationException *)CancellationExceptionMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("CancellationException(message:cause:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface JobKt : Base
+ (BOOL)isActive:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("isActive(_:)")));
+ (id<Job>)job:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("job(_:)")));
+ (id<CompletableJob>)JobParent:(id<Job> _Nullable)parent __attribute__((swift_name("Job(parent:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmName(name="Job")
*/
+ (id<Job>)Job0Parent:(id<Job> _Nullable)parent __attribute__((swift_name("Job0(parent:)")));
+ (void)cancel:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("cancel(_:)")));
+ (BOOL)cancel:(id<KotlinCoroutineContext>)receiver cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(_:cause:)")));
+ (void)cancel:(id<KotlinCoroutineContext>)receiver cause_:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(_:cause_:)")));
+ (void)cancel:(id<Job>)receiver message:(NSString *)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(_:message:cause:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)cancelAndJoin:(id<Job>)receiver completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("cancelAndJoin(_:completionHandler:)")));
+ (void)cancelChildren:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("cancelChildren(_:)")));
+ (void)cancelChildren_:(id<Job>)receiver __attribute__((swift_name("cancelChildren(__:)")));
+ (void)cancelChildren:(id<KotlinCoroutineContext>)receiver cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancelChildren(_:cause:)")));
+ (void)cancelChildren:(id<KotlinCoroutineContext>)receiver cause_:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancelChildren(_:cause_:)")));
+ (void)cancelChildren:(id<Job>)receiver cause__:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancelChildren(_:cause__:)")));
+ (void)cancelChildren:(id<Job>)receiver cause___:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancelChildren(_:cause___:)")));
+ (void)ensureActive:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("ensureActive(_:)")));
+ (void)ensureActive_:(id<Job>)receiver __attribute__((swift_name("ensureActive(__:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface LimitKt : Base
+ (id<Flow>)drop:(id<Flow>)receiver count:(int32_t)count __attribute__((swift_name("drop(_:count:)")));
+ (id<Flow>)dropWhile:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("dropWhile(_:predicate:)")));
+ (id<Flow>)take:(id<Flow>)receiver count:(int32_t)count __attribute__((swift_name("take(_:count:)")));
+ (id<Flow>)takeWhile:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("takeWhile(_:predicate:)")));
+ (id<Flow>)transformWhile:(id<Flow>)receiver transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("transformWhile(_:transform:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface LintKt : Base
+ (id<KotlinCoroutineContext>)coroutineContext:(id<FlowCollector>)receiver __attribute__((swift_name("coroutineContext(_:)")));
+ (BOOL)isActive:(id<FlowCollector>)receiver __attribute__((swift_name("isActive(_:)")));
+ (void)cancel:(id<FlowCollector>)receiver cause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(_:cause:)")));
+ (id<Flow>)cancellable:(id<SharedFlow>)receiver __attribute__((swift_name("cancellable(_:)")));
+ (id<Flow>)catch:(id<SharedFlow>)receiver action:(id<KotlinSuspendFunction2>)action __attribute__((swift_name("catch(_:action:)")));
+ (id<Flow>)conflate:(id<StateFlow>)receiver __attribute__((swift_name("conflate(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)count:(id<SharedFlow>)receiver completionHandler:(void (^)(Int * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("count(_:completionHandler:)")));
+ (id<Flow>)distinctUntilChanged:(id<StateFlow>)receiver __attribute__((swift_name("distinctUntilChanged(_:)")));
+ (id<Flow>)flowOn:(id<SharedFlow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("flowOn(_:context:)")));
+ (id<Flow>)retry:(id<SharedFlow>)receiver retries:(int64_t)retries predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("retry(_:retries:predicate:)")));
+ (id<Flow>)retryWhen:(id<SharedFlow>)receiver predicate:(id<KotlinSuspendFunction3>)predicate __attribute__((swift_name("retryWhen(_:predicate:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toList:(id<SharedFlow>)receiver completionHandler:(void (^)(NSArray<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toList(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toList:(id<SharedFlow>)receiver destination:(NSMutableArray<id> *)destination completionHandler:(void (^)(KotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toList(_:destination:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toSet:(id<SharedFlow>)receiver completionHandler:(void (^)(NSSet<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toSet(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toSet:(id<SharedFlow>)receiver destination:(MutableSet<id> *)destination completionHandler:(void (^)(KotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toSet(_:destination:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface MergeKt : Base

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
+ (id<Flow>)flatMapConcat:(id<Flow>)receiver transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("flatMapConcat(_:transform:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
+ (id<Flow>)flatMapLatest:(id<Flow>)receiver transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("flatMapLatest(_:transform:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
+ (id<Flow>)flatMapMerge:(id<Flow>)receiver concurrency:(int32_t)concurrency transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("flatMapMerge(_:concurrency:transform:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
+ (id<Flow>)flattenConcat:(id<Flow>)receiver __attribute__((swift_name("flattenConcat(_:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
+ (id<Flow>)flattenMerge:(id<Flow>)receiver concurrency:(int32_t)concurrency __attribute__((swift_name("flattenMerge(_:concurrency:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
+ (id<Flow>)mapLatest:(id<Flow>)receiver transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("mapLatest(_:transform:)")));
+ (id<Flow>)merge:(id)receiver __attribute__((swift_name("merge(_:)")));
+ (id<Flow>)mergeFlows:(KotlinArray<id<Flow>> *)flows __attribute__((swift_name("merge(flows:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
+ (id<Flow>)transformLatest:(id<Flow>)receiver transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("transformLatest(_:transform:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.FlowPreview
*/
@property (class, readonly) int32_t DEFAULT_CONCURRENCY __attribute__((swift_name("DEFAULT_CONCURRENCY")));

/**
 * @note annotations
 *   kotlinx.coroutines.FlowPreview
*/
@property (class, readonly) NSString *DEFAULT_CONCURRENCY_PROPERTY_NAME __attribute__((swift_name("DEFAULT_CONCURRENCY_PROPERTY_NAME")));
@end

__attribute__((objc_subclassing_restricted))
@interface MigrationKt : Base
+ (id<Flow>)cache:(id<Flow>)receiver __attribute__((swift_name("cache(_:)")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("combineLatest(_:other:transform:)")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other other2:(id<Flow>)other2 transform:(id<KotlinSuspendFunction3>)transform __attribute__((swift_name("combineLatest(_:other:other2:transform:)")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other other2:(id<Flow>)other2 other3:(id<Flow>)other3 transform:(id<KotlinSuspendFunction4>)transform __attribute__((swift_name("combineLatest(_:other:other2:other3:transform:)")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other other2:(id<Flow>)other2 other3:(id<Flow>)other3 other4:(id<Flow>)other4 transform:(id<KotlinSuspendFunction5>)transform __attribute__((swift_name("combineLatest(_:other:other2:other3:other4:transform:)")));
+ (id<Flow>)compose:(id<Flow>)receiver transformer:(id<Flow> (^)(id<Flow>))transformer __attribute__((swift_name("compose(_:transformer:)")));
+ (id<Flow>)concatMap:(id<Flow>)receiver mapper:(id<Flow> (^)(id _Nullable))mapper __attribute__((swift_name("concatMap(_:mapper:)")));
+ (id<Flow>)concatWith:(id<Flow>)receiver value:(id _Nullable)value __attribute__((swift_name("concatWith(_:value:)")));
+ (id<Flow>)concatWith:(id<Flow>)receiver other:(id<Flow>)other __attribute__((swift_name("concatWith(_:other:)")));
+ (id<Flow>)delayEach:(id<Flow>)receiver timeMillis:(int64_t)timeMillis __attribute__((swift_name("delayEach(_:timeMillis:)")));
+ (id<Flow>)delayFlow:(id<Flow>)receiver timeMillis:(int64_t)timeMillis __attribute__((swift_name("delayFlow(_:timeMillis:)")));
+ (id<Flow>)flatMap:(id<Flow>)receiver mapper:(id<KotlinSuspendFunction1>)mapper __attribute__((swift_name("flatMap(_:mapper:)")));
+ (id<Flow>)flatten:(id<Flow>)receiver __attribute__((swift_name("flatten(_:)")));
+ (void)forEach:(id<Flow>)receiver action:(id<KotlinSuspendFunction1>)action __attribute__((swift_name("forEach(_:action:)")));
+ (id<Flow>)merge:(id<Flow>)receiver __attribute__((swift_name("merge(_:)")));
+ (id<Flow>)observeOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("observeOn(_:context:)")));
+ (id<Flow>)onErrorResume:(id<Flow>)receiver fallback:(id<Flow>)fallback __attribute__((swift_name("onErrorResume(_:fallback:)")));
+ (id<Flow>)onErrorResumeNext:(id<Flow>)receiver fallback:(id<Flow>)fallback __attribute__((swift_name("onErrorResumeNext(_:fallback:)")));
+ (id<Flow>)onErrorReturn:(id<Flow>)receiver fallback:(id _Nullable)fallback __attribute__((swift_name("onErrorReturn(_:fallback:)")));
+ (id<Flow>)onErrorReturn:(id<Flow>)receiver fallback:(id _Nullable)fallback predicate:(Boolean *(^)(KotlinThrowable *))predicate __attribute__((swift_name("onErrorReturn(_:fallback:predicate:)")));
+ (id<Flow>)publish:(id<Flow>)receiver __attribute__((swift_name("publish(_:)")));
+ (id<Flow>)publish:(id<Flow>)receiver bufferSize:(int32_t)bufferSize __attribute__((swift_name("publish(_:bufferSize:)")));
+ (id<Flow>)publishOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("publishOn(_:context:)")));
+ (id<Flow>)replay:(id<Flow>)receiver __attribute__((swift_name("replay(_:)")));
+ (id<Flow>)replay:(id<Flow>)receiver bufferSize:(int32_t)bufferSize __attribute__((swift_name("replay(_:bufferSize:)")));
+ (id<Flow>)scanFold:(id<Flow>)receiver initial:(id _Nullable)initial operation:(id<KotlinSuspendFunction2>)operation __attribute__((swift_name("scanFold(_:initial:operation:)")));
+ (id<Flow>)scanReduce:(id<Flow>)receiver operation:(id<KotlinSuspendFunction2>)operation __attribute__((swift_name("scanReduce(_:operation:)")));
+ (id<Flow>)skip:(id<Flow>)receiver count:(int32_t)count __attribute__((swift_name("skip(_:count:)")));
+ (id<Flow>)startWith:(id<Flow>)receiver value:(id _Nullable)value __attribute__((swift_name("startWith(_:value:)")));
+ (id<Flow>)startWith:(id<Flow>)receiver other:(id<Flow>)other __attribute__((swift_name("startWith(_:other:)")));
+ (void)subscribe:(id<Flow>)receiver __attribute__((swift_name("subscribe(_:)")));
+ (void)subscribe:(id<Flow>)receiver onEach:(id<KotlinSuspendFunction1>)onEach __attribute__((swift_name("subscribe(_:onEach:)")));
+ (void)subscribe:(id<Flow>)receiver onEach:(id<KotlinSuspendFunction1>)onEach onError:(id<KotlinSuspendFunction1>)onError __attribute__((swift_name("subscribe(_:onEach:onError:)")));
+ (id<Flow>)subscribeOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("subscribeOn(_:context:)")));
+ (id<Flow>)switchMap:(id<Flow>)receiver transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("switchMap(_:transform:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface MultithreadedDispatchers_commonKt : Base

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
 *   kotlinx.coroutines.DelicateCoroutinesApi
*/
+ (CloseableCoroutineDispatcher *)doNewSingleThreadContextName:(NSString *)name __attribute__((swift_name("doNewSingleThreadContext(name:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface MultithreadedDispatchersKt : Base
+ (CloseableCoroutineDispatcher *)doNewFixedThreadPoolContextNThreads:(int32_t)nThreads name:(NSString *)name __attribute__((swift_name("doNewFixedThreadPoolContext(nThreads:name:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface MutexKt : Base
+ (id<Mutex>)MutexLocked:(BOOL)locked __attribute__((swift_name("Mutex(locked:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)withLock:(id<Mutex>)receiver owner:(id _Nullable)owner action:(id _Nullable (^)(void))action completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withLock(_:owner:action:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface OnTimeoutKt : Base

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
+ (void)onTimeout:(id<SelectBuilder>)receiver timeMillis:(int64_t)timeMillis block:(id<KotlinSuspendFunction0>)block __attribute__((swift_name("onTimeout(_:timeMillis:block:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
+ (void)onTimeout:(id<SelectBuilder>)receiver timeout:(int64_t)timeout block:(id<KotlinSuspendFunction0>)block __attribute__((swift_name("onTimeout(_:timeout:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ProduceKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)awaitClose:(id<ProducerScope>)receiver block:(void (^)(void))block completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("awaitClose(_:block:completionHandler:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
+ (id<ReceiveChannel>)produce:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context capacity:(int32_t)capacity block:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("produce(_:context:capacity:block:)")));
+ (id<ReceiveChannel>)produce:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context capacity:(int32_t)capacity start:(CoroutineStart *)start onCompletion:(void (^ _Nullable)(KotlinThrowable * _Nullable))onCompletion block:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("produce(_:context:capacity:start:onCompletion:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ReduceKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)first:(id<Flow>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("first(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)first:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction1>)predicate completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("first(_:predicate:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)firstOrNull:(id<Flow>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("firstOrNull(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)firstOrNull:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction1>)predicate completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("firstOrNull(_:predicate:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)fold:(id<Flow>)receiver initial:(id _Nullable)initial operation:(id<KotlinSuspendFunction2>)operation completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("fold(_:initial:operation:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)last:(id<Flow>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("last(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)lastOrNull:(id<Flow>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("lastOrNull(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)reduce:(id<Flow>)receiver operation:(id<KotlinSuspendFunction2>)operation completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("reduce(_:operation:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)single:(id<Flow>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("single(_:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)singleOrNull:(id<Flow>)receiver completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("singleOrNull(_:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface RunnableKt : Base
+ (id<Runnable>)RunnableBlock:(void (^)(void))block __attribute__((swift_name("Runnable(block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SelectKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)selectBuilder:(void (^)(id<SelectBuilder>))builder completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("select(builder:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SelectUnbiasedKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)selectUnbiasedBuilder:(void (^)(id<SelectBuilder>))builder completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("selectUnbiased(builder:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SemaphoreKt : Base
+ (id<Semaphore>)SemaphorePermits:(int32_t)permits acquiredPermits:(int32_t)acquiredPermits __attribute__((swift_name("Semaphore(permits:acquiredPermits:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)withPermit:(id<Semaphore>)receiver action:(id _Nullable (^)(void))action completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withPermit(_:action:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ShareKt : Base
+ (id<SharedFlow>)asSharedFlow:(id<MutableSharedFlow>)receiver __attribute__((swift_name("asSharedFlow(_:)")));
+ (id<StateFlow>)asStateFlow:(id<MutableStateFlow>)receiver __attribute__((swift_name("asStateFlow(_:)")));
+ (id<SharedFlow>)onSubscription:(id<SharedFlow>)receiver action:(id<KotlinSuspendFunction1>)action __attribute__((swift_name("onSubscription(_:action:)")));
+ (id<SharedFlow>)shareIn:(id<Flow>)receiver scope:(id<CoroutineScope>)scope started:(id<SharingStarted>)started replay:(int32_t)replay __attribute__((swift_name("shareIn(_:scope:started:replay:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)stateIn:(id<Flow>)receiver scope:(id<CoroutineScope>)scope completionHandler:(void (^)(id<StateFlow> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("stateIn(_:scope:completionHandler:)")));
+ (id<StateFlow>)stateIn:(id<Flow>)receiver scope:(id<CoroutineScope>)scope started:(id<SharingStarted>)started initialValue:(id _Nullable)initialValue __attribute__((swift_name("stateIn(_:scope:started:initialValue:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SharedFlowKt : Base
+ (id<MutableSharedFlow>)MutableSharedFlowReplay:(int32_t)replay extraBufferCapacity:(int32_t)extraBufferCapacity onBufferOverflow:(BufferOverflow *)onBufferOverflow __attribute__((swift_name("MutableSharedFlow(replay:extraBufferCapacity:onBufferOverflow:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface StateFlowKt : Base
+ (id<MutableStateFlow>)MutableStateFlowValue:(id _Nullable)value __attribute__((swift_name("MutableStateFlow(value:)")));
+ (id _Nullable)getAndUpdate:(id<MutableStateFlow>)receiver function:(id _Nullable (^)(id _Nullable))function __attribute__((swift_name("getAndUpdate(_:function:)")));
+ (void)update:(id<MutableStateFlow>)receiver function:(id _Nullable (^)(id _Nullable))function __attribute__((swift_name("update(_:function:)")));
+ (id _Nullable)updateAndGet:(id<MutableStateFlow>)receiver function:(id _Nullable (^)(id _Nullable))function __attribute__((swift_name("updateAndGet(_:function:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SupervisorKt : Base
+ (id<CompletableJob>)SupervisorJobParent:(id<Job> _Nullable)parent __attribute__((swift_name("SupervisorJob(parent:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmName(name="SupervisorJob")
*/
+ (id<Job>)SupervisorJob0Parent:(id<Job> _Nullable)parent __attribute__((swift_name("SupervisorJob0(parent:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)supervisorScopeBlock:(id<KotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("supervisorScope(block:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface Synchronized_commonKt : Base
+ (id _Nullable)synchronizedLock:(SynchronizedObject *)lock block:(id _Nullable (^)(void))block __attribute__((swift_name("synchronized(lock:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SynchronizedKt : Base
+ (id _Nullable)synchronizedImplLock:(SynchronizedObject *)lock block:(id _Nullable (^)(void))block __attribute__((swift_name("synchronizedImpl(lock:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface TimeoutKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)withTimeoutTimeMillis:(int64_t)timeMillis block:(id<KotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withTimeout(timeMillis:block:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)withTimeoutTimeout:(int64_t)timeout block:(id<KotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withTimeout(timeout:block:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)withTimeoutOrNullTimeMillis:(int64_t)timeMillis block:(id<KotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withTimeoutOrNull(timeMillis:block:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)withTimeoutOrNullTimeout:(int64_t)timeout block:(id<KotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withTimeoutOrNull(timeout:block:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface TransformKt : Base
+ (id<Flow>)filter:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("filter(_:predicate:)")));
+ (id<Flow>)filterIsInstance:(id<Flow>)receiver __attribute__((swift_name("filterIsInstance(_:)")));
+ (id<Flow>)filterIsInstance:(id<Flow>)receiver klass:(id<KotlinKClass>)klass __attribute__((swift_name("filterIsInstance(_:klass:)")));
+ (id<Flow>)filterNot:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("filterNot(_:predicate:)")));
+ (id<Flow>)filterNotNull:(id<Flow>)receiver __attribute__((swift_name("filterNotNull(_:)")));
+ (id<Flow>)map:(id<Flow>)receiver transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("map(_:transform:)")));
+ (id<Flow>)mapNotNull:(id<Flow>)receiver transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("mapNotNull(_:transform:)")));
+ (id<Flow>)onEach:(id<Flow>)receiver action:(id<KotlinSuspendFunction1>)action __attribute__((swift_name("onEach(_:action:)")));
+ (id<Flow>)runningFold:(id<Flow>)receiver initial:(id _Nullable)initial operation:(id<KotlinSuspendFunction2>)operation __attribute__((swift_name("runningFold(_:initial:operation:)")));
+ (id<Flow>)runningReduce:(id<Flow>)receiver operation:(id<KotlinSuspendFunction2>)operation __attribute__((swift_name("runningReduce(_:operation:)")));
+ (id<Flow>)scan:(id<Flow>)receiver initial:(id _Nullable)initial operation:(id<KotlinSuspendFunction2>)operation __attribute__((swift_name("scan(_:initial:operation:)")));
+ (id<Flow>)withIndex:(id<Flow>)receiver __attribute__((swift_name("withIndex(_:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface WhileSelectKt : Base

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)whileSelectBuilder:(void (^)(id<SelectBuilder>))builder completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("whileSelect(builder:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface YieldKt : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)yieldWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("yield(completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ZipKt : Base
+ (id<Flow>)combineFlows:(KotlinArray<id<Flow>> *)flows transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("combine(flows:transform:)")));
+ (id<Flow>)combineFlows:(id)flows transform_:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("combine(flows:transform_:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmName(name="flowCombine")
*/
+ (id<Flow>)combine:(id<Flow>)receiver flow:(id<Flow>)flow transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("combine(_:flow:transform:)")));
+ (id<Flow>)combineFlow:(id<Flow>)flow flow2:(id<Flow>)flow2 transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("combine(flow:flow2:transform:)")));
+ (id<Flow>)combineFlow:(id<Flow>)flow flow2:(id<Flow>)flow2 flow3:(id<Flow>)flow3 transform:(id<KotlinSuspendFunction3>)transform __attribute__((swift_name("combine(flow:flow2:flow3:transform:)")));
+ (id<Flow>)combineFlow:(id<Flow>)flow flow2:(id<Flow>)flow2 flow3:(id<Flow>)flow3 flow4:(id<Flow>)flow4 transform:(id<KotlinSuspendFunction4>)transform __attribute__((swift_name("combine(flow:flow2:flow3:flow4:transform:)")));
+ (id<Flow>)combineFlow:(id<Flow>)flow flow2:(id<Flow>)flow2 flow3:(id<Flow>)flow3 flow4:(id<Flow>)flow4 flow5:(id<Flow>)flow5 transform:(id<KotlinSuspendFunction5>)transform __attribute__((swift_name("combine(flow:flow2:flow3:flow4:flow5:transform:)")));
+ (id<Flow>)combineTransformFlows:(KotlinArray<id<Flow>> *)flows transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("combineTransform(flows:transform:)")));
+ (id<Flow>)combineTransformFlows:(id)flows transform_:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("combineTransform(flows:transform_:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmName(name="flowCombineTransform")
*/
+ (id<Flow>)combineTransform:(id<Flow>)receiver flow:(id<Flow>)flow transform:(id<KotlinSuspendFunction3>)transform __attribute__((swift_name("combineTransform(_:flow:transform:)")));
+ (id<Flow>)combineTransformFlow:(id<Flow>)flow flow2:(id<Flow>)flow2 transform:(id<KotlinSuspendFunction3>)transform __attribute__((swift_name("combineTransform(flow:flow2:transform:)")));
+ (id<Flow>)combineTransformFlow:(id<Flow>)flow flow2:(id<Flow>)flow2 flow3:(id<Flow>)flow3 transform:(id<KotlinSuspendFunction4>)transform __attribute__((swift_name("combineTransform(flow:flow2:flow3:transform:)")));
+ (id<Flow>)combineTransformFlow:(id<Flow>)flow flow2:(id<Flow>)flow2 flow3:(id<Flow>)flow3 flow4:(id<Flow>)flow4 transform:(id<KotlinSuspendFunction5>)transform __attribute__((swift_name("combineTransform(flow:flow2:flow3:flow4:transform:)")));
+ (id<Flow>)combineTransformFlow:(id<Flow>)flow flow2:(id<Flow>)flow2 flow3:(id<Flow>)flow3 flow4:(id<Flow>)flow4 flow5:(id<Flow>)flow5 transform:(id<KotlinSuspendFunction6>)transform __attribute__((swift_name("combineTransform(flow:flow2:flow3:flow4:flow5:transform:)")));
+ (id<Flow>)zip:(id<Flow>)receiver other:(id<Flow>)other transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("zip(_:other:transform:)")));
@end

@protocol KotlinSequence
@required
- (id<KotlinIterator>)iterator __attribute__((swift_name("iterator()")));
@end

@protocol KotlinFunction
@required
@end

@protocol KotlinSuspendFunction1 <KotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinEnumCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

@protocol KotlinSuspendFunction0 <KotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinUnit : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unit __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinUnit *shared __attribute__((swift_name("shared")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

@protocol KotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

@interface KotlinIntIterator : Base <KotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (Int *)next __attribute__((swift_name("next()")));
- (int32_t)nextInt __attribute__((swift_name("nextInt()")));
@end

@interface KotlinLongIterator : Base <KotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (Long *)next __attribute__((swift_name("next()")));
- (int64_t)nextLong __attribute__((swift_name("nextLong()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinIntProgression.Companion")))
@interface KotlinIntProgressionCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinIntProgressionCompanion *shared __attribute__((swift_name("shared")));
- (KotlinIntProgression *)fromClosedRangeRangeStart:(int32_t)rangeStart rangeEnd:(int32_t)rangeEnd step:(int32_t)step __attribute__((swift_name("fromClosedRange(rangeStart:rangeEnd:step:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinIntRange.Companion")))
@interface KotlinIntRangeCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinIntRangeCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) KotlinIntRange *EMPTY __attribute__((swift_name("EMPTY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinLongProgression.Companion")))
@interface KotlinLongProgressionCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinLongProgressionCompanion *shared __attribute__((swift_name("shared")));
- (KotlinLongProgression *)fromClosedRangeRangeStart:(int64_t)rangeStart rangeEnd:(int64_t)rangeEnd step:(int64_t)step __attribute__((swift_name("fromClosedRange(rangeStart:rangeEnd:step:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinLongRange.Companion")))
@interface KotlinLongRangeCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinLongRangeCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) KotlinLongRange *EMPTY __attribute__((swift_name("EMPTY")));
@end

@protocol KotlinSuspendFunction2 <KotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 p2:(id _Nullable)p2 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:p2:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinNothing : Base
@end

@protocol KotlinComparator
@required
- (int32_t)compareA:(id _Nullable)a b:(id _Nullable)b __attribute__((swift_name("compare(a:b:)")));
@end

@protocol KotlinSuspendFunction3 <KotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 p2:(id _Nullable)p2 p3:(id _Nullable)p3 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:p2:p3:completionHandler:)")));
@end

@protocol KotlinSuspendFunction4 <KotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 p2:(id _Nullable)p2 p3:(id _Nullable)p3 p4:(id _Nullable)p4 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:p2:p3:p4:completionHandler:)")));
@end

@protocol KotlinSuspendFunction5 <KotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 p2:(id _Nullable)p2 p3:(id _Nullable)p3 p4:(id _Nullable)p4 p5:(id _Nullable)p5 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:p2:p3:p4:p5:completionHandler:)")));
@end

@protocol KotlinKDeclarationContainer
@required
@end

@protocol KotlinKAnnotatedElement
@required
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
@protocol KotlinKClassifier
@required
@end

@protocol KotlinKClass <KotlinKDeclarationContainer, KotlinKAnnotatedElement, KotlinKClassifier>
@required

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
- (BOOL)isInstanceValue:(id _Nullable)value __attribute__((swift_name("isInstance(value:)")));
@property (readonly) NSString * _Nullable qualifiedName __attribute__((swift_name("qualifiedName")));
@property (readonly) NSString * _Nullable simpleName __attribute__((swift_name("simpleName")));
@end

@protocol KotlinSuspendFunction6 <KotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 p2:(id _Nullable)p2 p3:(id _Nullable)p3 p4:(id _Nullable)p4 p5:(id _Nullable)p5 p6:(id _Nullable)p6 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:p2:p3:p4:p5:p6:completionHandler:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
