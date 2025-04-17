#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class AbstractCollectionSerializer<Element, Collection, Builder>, AbstractPolymorphicSerializer<T>, AmPmMarker, AtomicArray<T>, AtomicBoolean, AtomicInt, AtomicLong, AtomicOp<__contravariant T>, AtomicRef<T>, BufferOverflow, BufferOverflow_, ChannelFactory, ChannelFlow<T>, Channel_Factory, ClassSerialDescriptorBuilder, ClockCompanion, ClockSystem, CloseableCoroutineDispatcher, CompositeDecoderCompanion, CoroutineDispatcher, CoroutineDispatcherKey, CoroutineDispatcher_Key, CoroutineExceptionHandlerKey, CoroutineExceptionHandler_Key, CoroutineName, CoroutineNameKey, CoroutineName_Key, CoroutineStart, CoroutineStart_, DateBasedDateTimeUnitSerializer, DatePeriod, DatePeriodCompanion, DatePeriodComponentSerializer, DatePeriodIso8601Serializer, DateTimeComponents, DateTimeComponentsCompanion, DateTimeComponentsFormats, DateTimeFormatCompanion, DateTimePeriod, DateTimePeriodCompanion, DateTimePeriodComponentSerializer, DateTimePeriodIso8601Serializer, DateTimeUnit, DateTimeUnitCompanion, DateTimeUnitDateBased, DateTimeUnitDateBasedCompanion, DateTimeUnitDayBased, DateTimeUnitDayBasedCompanion, DateTimeUnitMonthBased, DateTimeUnitMonthBasedCompanion, DateTimeUnitSerializer, DateTimeUnitTimeBased, DateTimeUnitTimeBasedCompanion, DayBasedDateTimeUnitSerializer, DayOfWeek, DayOfWeekNames, DayOfWeekNamesCompanion, DayOfWeekSerializer, Dispatchers, Dispatchers_, FixedOffsetTimeZone, FixedOffsetTimeZoneCompanion, FixedOffsetTimeZoneSerializer, GlobalScope, GlobalScope_, Instant, InstantCompanion, InstantComponentSerializer, InstantIso8601Serializer, JobKey, JobSupport, Job_Key, KotlinAbstractCoroutineContextElement, KotlinAbstractCoroutineContextKey<B, E>, KotlinArray<T>, KotlinAtomicReference<T>, KotlinBooleanCompanion, KotlinByteArray, KotlinByteCompanion, KotlinByteIterator, KotlinCancellationException, KotlinCharCompanion, KotlinDoubleCompanion, KotlinDurationCompanion, KotlinDurationUnit, KotlinEnum<E>, KotlinEnumCompanion, KotlinException, KotlinFloatCompanion, KotlinIllegalArgumentException, KotlinIllegalStateException, KotlinIntArray, KotlinIntCompanion, KotlinIntIterator, KotlinIntProgression, KotlinIntProgressionCompanion, KotlinIntRange, KotlinIntRangeCompanion, KotlinKTypeProjection, KotlinKTypeProjectionCompanion, KotlinKVariance, KotlinLongArray, KotlinLongCompanion, KotlinLongIterator, KotlinLongProgression, KotlinLongProgressionCompanion, KotlinLongRange, KotlinLongRangeCompanion, KotlinNoSuchElementException, KotlinNothing, KotlinRuntimeException, KotlinShortCompanion, KotlinStringCompanion, KotlinThrowable, KotlinUByteCompanion, KotlinUIntCompanion, KotlinULongCompanion, KotlinUShortCompanion, KotlinUnit, LocalDate, LocalDateCompanion, LocalDateComponentSerializer, LocalDateFormats, LocalDateIso8601Serializer, LocalDateTime, LocalDateTimeCompanion, LocalDateTimeComponentSerializer, LocalDateTimeFormats, LocalDateTimeIso8601Serializer, LocalTime, LocalTimeCompanion, LocalTimeComponentSerializer, LocalTimeFormats, LocalTimeIso8601Serializer, LockFreeLinkedListNode, LongAsStringSerializer, MainCoroutineDispatcher, Month, MonthBasedDateTimeUnitSerializer, MonthNames, MonthNamesCompanion, MonthSerializer, NSDate, NSDateComponents, NSTimeZone, NonCancellable, NonCancellable_, NonDisposableHandle, NonDisposableHandle_, OpDescriptor, Padding, PolymorphicKind, PolymorphicKindOPEN, PolymorphicKindSEALED, PolymorphicModuleBuilder<__contravariant Base_>, PrimitiveKind, PrimitiveKindBOOLEAN, PrimitiveKindBYTE, PrimitiveKindCHAR, PrimitiveKindDOUBLE, PrimitiveKindFLOAT, PrimitiveKindINT, PrimitiveKindLONG, PrimitiveKindSHORT, PrimitiveKindSTRING, SerialKind, SerialKindCONTEXTUAL, SerialKindENUM, SerializationException, SerializersModule, SerializersModuleBuilder, SharingCommand, SharingCommand_, SharingStartedCompanion, SharingStarted_Companion, StructureKind, StructureKindCLASS, StructureKindLIST, StructureKindMAP, StructureKindOBJECT, SynchronizedObject, SynchronizedObjectLockState, SynchronizedObjectStatus, TaggedDecoder<Tag>, TaggedEncoder<Tag>, ThreadSafeHeap<T>, TimeBasedDateTimeUnitSerializer, TimeZone, TimeZoneCompanion, TimeZoneSerializer, TimeoutCancellationException, TraceBase, TraceBaseNone, TraceFormat, UtcOffset, UtcOffsetCompanion, UtcOffsetFormats, UtcOffsetSerializer;

@protocol BinaryFormat, BroadcastChannel, CancellableContinuation, Channel, ChannelIterator, ChildHandle, ChildJob, Clock, CompletableDeferred, CompletableJob, CompositeDecoder, CompositeEncoder, CopyableThrowable, CoroutineExceptionHandler, CoroutineScope, DateTimeFormat, DateTimeFormatBuilder, DateTimeFormatBuilderWithDate, DateTimeFormatBuilderWithDateTime, DateTimeFormatBuilderWithDateTimeComponents, DateTimeFormatBuilderWithTime, DateTimeFormatBuilderWithUtcOffset, Decoder, Deferred, DeserializationStrategy, DisposableHandle, Encoder, Flow, FlowCollector, FusibleFlow, Job, KSerializer, KotlinAnnotation, KotlinAppendable, KotlinClosedRange, KotlinComparable, KotlinContinuation, KotlinContinuationInterceptor, KotlinCoroutineContext, KotlinCoroutineContextElement, KotlinCoroutineContextKey, KotlinFunction, KotlinIterable, KotlinIterator, KotlinKAnnotatedElement, KotlinKCallable, KotlinKClass, KotlinKClassifier, KotlinKDeclarationContainer, KotlinKProperty, KotlinKType, KotlinMapEntry, KotlinOpenEndRange, KotlinSequence, KotlinSuspendFunction0, KotlinSuspendFunction1, KotlinSuspendFunction2, KotlinSuspendFunction3, KotlinSuspendFunction4, KotlinSuspendFunction5, KotlinSuspendFunction6, KotlinTimeMark, KotlinTimeSource, KotlinTimeSourceWithComparableMarks, MainDispatcherFactory, MutableSharedFlow, MutableStateFlow, Mutex, ParentJob, ProducerScope, ReceiveChannel, Runnable, SelectBuilder, SelectClause, SelectClause0, SelectClause1, SelectClause2, SelectInstance, Semaphore, SendChannel, SerialDescriptor, SerialFormat, SerializationStrategy, SerializersModuleCollector, SharedFlow, SharingStarted, StateFlow, StringFormat;

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
@interface AtomicArray<T> : Base
- (AtomicRef<T> *)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
@interface AtomicBoolean : Base
- (BOOL)compareAndSetExpect:(BOOL)expect update:(BOOL)update __attribute__((swift_name("compareAndSet(expect:update:)")));
- (BOOL)getAndSetValue:(BOOL)value __attribute__((swift_name("getAndSet(value:)")));
- (BOOL)getValueThisRef:(id _Nullable)thisRef property:(id<KotlinKProperty>)property __attribute__((swift_name("getValue(thisRef:property:)")));
- (void)lazySetValue:(BOOL)value __attribute__((swift_name("lazySet(value:)")));
- (void)setValueThisRef:(id _Nullable)thisRef property:(id<KotlinKProperty>)property value:(BOOL)value __attribute__((swift_name("setValue(thisRef:property:value:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property BOOL value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
@interface AtomicBooleanArray : Base
- (instancetype)initWithSize:(int32_t)size __attribute__((swift_name("init(size:)"))) __attribute__((objc_designated_initializer));
- (AtomicBoolean *)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
@interface AtomicInt : Base
- (int32_t)addAndGetDelta:(int32_t)delta __attribute__((swift_name("addAndGet(delta:)")));
- (BOOL)compareAndSetExpect:(int32_t)expect update:(int32_t)update __attribute__((swift_name("compareAndSet(expect:update:)")));
- (int32_t)decrementAndGet __attribute__((swift_name("decrementAndGet()")));
- (int32_t)getAndAddDelta:(int32_t)delta __attribute__((swift_name("getAndAdd(delta:)")));
- (int32_t)getAndDecrement __attribute__((swift_name("getAndDecrement()")));
- (int32_t)getAndIncrement __attribute__((swift_name("getAndIncrement()")));
- (int32_t)getAndSetValue:(int32_t)value __attribute__((swift_name("getAndSet(value:)")));
- (int32_t)getValueThisRef:(id _Nullable)thisRef property:(id<KotlinKProperty>)property __attribute__((swift_name("getValue(thisRef:property:)")));
- (int32_t)incrementAndGet __attribute__((swift_name("incrementAndGet()")));
- (void)lazySetValue:(int32_t)value __attribute__((swift_name("lazySet(value:)")));
- (void)minusAssignDelta:(int32_t)delta __attribute__((swift_name("minusAssign(delta:)")));
- (void)plusAssignDelta:(int32_t)delta __attribute__((swift_name("plusAssign(delta:)")));
- (void)setValueThisRef:(id _Nullable)thisRef property:(id<KotlinKProperty>)property value:(int32_t)value __attribute__((swift_name("setValue(thisRef:property:value:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property int32_t value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
@interface AtomicIntArray : Base
- (instancetype)initWithSize:(int32_t)size __attribute__((swift_name("init(size:)"))) __attribute__((objc_designated_initializer));
- (AtomicInt *)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
@interface AtomicLong : Base
- (int64_t)addAndGetDelta:(int64_t)delta __attribute__((swift_name("addAndGet(delta:)")));
- (BOOL)compareAndSetExpect:(int64_t)expect update:(int64_t)update __attribute__((swift_name("compareAndSet(expect:update:)")));
- (int64_t)decrementAndGet __attribute__((swift_name("decrementAndGet()")));
- (int64_t)getAndAddDelta:(int64_t)delta __attribute__((swift_name("getAndAdd(delta:)")));
- (int64_t)getAndDecrement __attribute__((swift_name("getAndDecrement()")));
- (int64_t)getAndIncrement __attribute__((swift_name("getAndIncrement()")));
- (int64_t)getAndSetValue:(int64_t)value __attribute__((swift_name("getAndSet(value:)")));
- (int64_t)getValueThisRef:(id _Nullable)thisRef property:(id<KotlinKProperty>)property __attribute__((swift_name("getValue(thisRef:property:)")));
- (int64_t)incrementAndGet __attribute__((swift_name("incrementAndGet()")));
- (void)lazySetValue:(int64_t)value __attribute__((swift_name("lazySet(value:)")));
- (void)minusAssignDelta:(int64_t)delta __attribute__((swift_name("minusAssign(delta:)")));
- (void)plusAssignDelta:(int64_t)delta __attribute__((swift_name("plusAssign(delta:)")));
- (void)setValueThisRef:(id _Nullable)thisRef property:(id<KotlinKProperty>)property value:(int64_t)value __attribute__((swift_name("setValue(thisRef:property:value:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property int64_t value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
@interface AtomicLongArray : Base
- (instancetype)initWithSize:(int32_t)size __attribute__((swift_name("init(size:)"))) __attribute__((objc_designated_initializer));
- (AtomicLong *)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
@interface AtomicRef<T> : Base
- (BOOL)compareAndSetExpect:(T _Nullable)expect update:(T _Nullable)update __attribute__((swift_name("compareAndSet(expect:update:)")));
- (T _Nullable)getAndSetValue:(T _Nullable)value __attribute__((swift_name("getAndSet(value:)")));
- (T _Nullable)getValueThisRef:(id _Nullable)thisRef property:(id<KotlinKProperty>)property __attribute__((swift_name("getValue(thisRef:property:)")));
- (void)lazySetValue:(T _Nullable)value __attribute__((swift_name("lazySet(value:)")));
- (void)setValueThisRef:(id _Nullable)thisRef property:(id<KotlinKProperty>)property value:(T _Nullable)value __attribute__((swift_name("setValue(thisRef:property:value:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property T _Nullable value __attribute__((swift_name("value")));
@end

@interface TraceBase : Base
- (void)appendEvent:(id)event __attribute__((swift_name("append(event:)")));
- (void)appendEvent1:(id)event1 event2:(id)event2 __attribute__((swift_name("append(event1:event2:)")));
- (void)appendEvent1:(id)event1 event2:(id)event2 event3:(id)event3 __attribute__((swift_name("append(event1:event2:event3:)")));
- (void)appendEvent1:(id)event1 event2:(id)event2 event3:(id)event3 event4:(id)event4 __attribute__((swift_name("append(event1:event2:event3:event4:)")));
- (void)invokeEvent:(id (^)(void))event __attribute__((swift_name("invoke(event:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TraceBase.None")))
@interface TraceBaseNone : TraceBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)none __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) TraceBaseNone *shared __attribute__((swift_name("shared")));
@end

@interface TraceFormat : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)formatIndex:(int32_t)index event:(id)event __attribute__((swift_name("format(index:event:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface MutexPool : Base
- (instancetype)initWithCapacity:(int32_t)capacity __attribute__((swift_name("init(capacity:)"))) __attribute__((objc_designated_initializer));
- (void *)allocate __attribute__((swift_name("allocate()")));
- (void)releaseMutexNode:(void *)mutexNode __attribute__((swift_name("release(mutexNode:)")));
@end

@interface SynchronizedObject : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)lock __attribute__((swift_name("lock()")));
- (BOOL)tryLock __attribute__((swift_name("tryLock()")));
- (void)unlock __attribute__((swift_name("unlock()")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly, getter=lock_) KotlinAtomicReference<SynchronizedObjectLockState *> *lock __attribute__((swift_name("lock")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SynchronizedObject.LockState")))
@interface SynchronizedObjectLockState : Base
- (instancetype)initWithStatus:(SynchronizedObjectStatus *)status nestedLocks:(int32_t)nestedLocks waiters:(int32_t)waiters ownerThreadId:(void * _Nullable)ownerThreadId mutex:(void * _Nullable)mutex __attribute__((swift_name("init(status:nestedLocks:waiters:ownerThreadId:mutex:)"))) __attribute__((objc_designated_initializer));
@property (readonly) void * _Nullable mutex __attribute__((swift_name("mutex")));
@property (readonly) int32_t nestedLocks __attribute__((swift_name("nestedLocks")));
@property (readonly) void * _Nullable ownerThreadId __attribute__((swift_name("ownerThreadId")));
@property (readonly) SynchronizedObjectStatus *status __attribute__((swift_name("status")));
@property (readonly) int32_t waiters __attribute__((swift_name("waiters")));
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
__attribute__((swift_name("SynchronizedObject.Status")))
@interface SynchronizedObjectStatus : KotlinEnum<SynchronizedObjectStatus *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SynchronizedObjectStatus *unlocked __attribute__((swift_name("unlocked")));
@property (class, readonly) SynchronizedObjectStatus *thin __attribute__((swift_name("thin")));
@property (class, readonly) SynchronizedObjectStatus *fat __attribute__((swift_name("fat")));
+ (KotlinArray<SynchronizedObjectStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SynchronizedObjectStatus *> *entries __attribute__((swift_name("entries")));
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
@property (readonly) id<KotlinCoroutineContextKey> key __attribute__((swift_name("key")));
@end

@protocol Job <KotlinCoroutineContextElement>
@required
- (id<ChildHandle>)attachChildChild:(id<ChildJob>)child __attribute__((swift_name("attachChild(child:)")));
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (KotlinCancellationException *)getCancellationException __attribute__((swift_name("getCancellationException()")));
- (id<DisposableHandle>)invokeOnCompletionHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(handler:)")));
- (id<DisposableHandle>)invokeOnCompletionOnCancelling:(BOOL)onCancelling invokeImmediately:(BOOL)invokeImmediately handler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(onCancelling:invokeImmediately:handler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)joinWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("join(completionHandler:)")));
- (id<Job>)plusOther:(id<Job>)other __attribute__((swift_name("plus(other:)"))) __attribute__((unavailable("Operator '+' on two Job objects is meaningless. Job is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The job to the right of `+` just replaces the job the left of `+`.")));
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
- (instancetype)initWithActive:(BOOL)active __attribute__((swift_name("init(active:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("This is internal API and may be removed in the future releases")));

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
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
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

@interface AbstractCoroutine_<__contravariant T> : JobSupport <Job, KotlinContinuation, CoroutineScope>
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
- (void)startStart:(CoroutineStart *)start receiver:(id _Nullable)receiver block_:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("start(start:receiver:block_:)")));
@property (readonly) id<KotlinCoroutineContext> context __attribute__((swift_name("context")));
@property (readonly) id<KotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive")));
@end

@protocol CancellableContinuation <KotlinContinuation>
@required
- (BOOL)cancelCause_:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(cause_:)")));
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

@protocol CancellableContinuation_ <KotlinContinuation>
@required
- (BOOL)cancelCause_:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(cause_:)")));
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

@protocol ChildHandle_ <DisposableHandle>
@required
- (BOOL)childCancelledCause:(KotlinThrowable *)cause __attribute__((swift_name("childCancelled(cause:)")));
@property (readonly) id<Job> _Nullable parent __attribute__((swift_name("parent")));
@end

@protocol ChildJob_ <Job>
@required
- (void)parentCancelledParentJob:(id<ParentJob>)parentJob __attribute__((swift_name("parentCancelled(parentJob:)")));
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
- (CoroutineDispatcher *)plusOther_:(CoroutineDispatcher *)other __attribute__((swift_name("plus(other_:)"))) __attribute__((unavailable("Operator '+' on two CoroutineDispatcher objects is meaningless. CoroutineDispatcher is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The dispatcher to the right of `+` just replaces the dispatcher to the left.")));
- (void)releaseInterceptedContinuationContinuation:(id<KotlinContinuation>)continuation __attribute__((swift_name("releaseInterceptedContinuation(continuation:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

@interface CloseableCoroutineDispatcher : CoroutineDispatcher
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)close __attribute__((swift_name("close()")));
@end

@interface CloseableCoroutineDispatcher_ : CoroutineDispatcher
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

@protocol CompletableDeferred_ <Deferred>
@required
- (BOOL)completeValue:(id _Nullable)value __attribute__((swift_name("complete(value:)")));
- (BOOL)completeExceptionallyException:(KotlinThrowable *)exception __attribute__((swift_name("completeExceptionally(exception:)")));
@end

@protocol CompletableJob <Job>
@required
- (BOOL)complete __attribute__((swift_name("complete()")));
- (BOOL)completeExceptionallyException:(KotlinThrowable *)exception __attribute__((swift_name("completeExceptionally(exception:)")));
@end

@protocol CompletableJob_ <Job>
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

__attribute__((objc_subclassing_restricted))
@interface CompletionHandlerException_ : KotlinRuntimeException
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


/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
@protocol CopyableThrowable_
@required
- (KotlinThrowable * _Nullable)createCopy __attribute__((swift_name("createCopy()")));
@end

@interface CoroutineDispatcher_ : KotlinAbstractCoroutineContextElement <KotlinContinuationInterceptor>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) CoroutineDispatcher_Key *companion __attribute__((swift_name("companion")));
- (void)dispatchContext:(id<KotlinCoroutineContext>)context block:(id<Runnable>)block __attribute__((swift_name("dispatch(context:block:)")));
- (void)dispatchYieldContext:(id<KotlinCoroutineContext>)context block:(id<Runnable>)block __attribute__((swift_name("dispatchYield(context:block:)")));
- (id<KotlinContinuation>)interceptContinuationContinuation:(id<KotlinContinuation>)continuation __attribute__((swift_name("interceptContinuation(continuation:)")));
- (BOOL)isDispatchNeededContext:(id<KotlinCoroutineContext>)context __attribute__((swift_name("isDispatchNeeded(context:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (CoroutineDispatcher *)limitedParallelismParallelism:(int32_t)parallelism __attribute__((swift_name("limitedParallelism(parallelism:)")));
- (CoroutineDispatcher *)plusOther_:(CoroutineDispatcher *)other __attribute__((swift_name("plus(other_:)"))) __attribute__((unavailable("Operator '+' on two CoroutineDispatcher objects is meaningless. CoroutineDispatcher is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The dispatcher to the right of `+` just replaces the dispatcher to the left.")));
- (void)releaseInterceptedContinuationContinuation:(id<KotlinContinuation>)continuation __attribute__((swift_name("releaseInterceptedContinuation(continuation:)")));
- (NSString *)description __attribute__((swift_name("description()")));
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


/**
 * @note annotations
 *   kotlin.ExperimentalStdlibApi
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CoroutineDispatcher_.Key")))
@interface CoroutineDispatcher_Key : KotlinAbstractCoroutineContextKey<id<KotlinContinuationInterceptor>, CoroutineDispatcher *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithBaseKey:(id<KotlinCoroutineContextKey>)baseKey safeCast:(id<KotlinCoroutineContextElement> _Nullable (^)(id<KotlinCoroutineContextElement>))safeCast __attribute__((swift_name("init(baseKey:safeCast:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) CoroutineDispatcher_Key *shared __attribute__((swift_name("shared")));
@end

@protocol CoroutineExceptionHandler <KotlinCoroutineContextElement>
@required
- (void)handleExceptionContext:(id<KotlinCoroutineContext>)context exception:(KotlinThrowable *)exception __attribute__((swift_name("handleException(context:exception:)")));
@end

@protocol CoroutineExceptionHandler_ <KotlinCoroutineContextElement>
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
@interface CoroutineExceptionHandler_Key : Base <KotlinCoroutineContextKey>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) CoroutineExceptionHandler_Key *shared __attribute__((swift_name("shared")));
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
@interface CoroutineName_ : KotlinAbstractCoroutineContextElement
- (instancetype)initWithName:(NSString *)name __attribute__((swift_name("init(name:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) CoroutineName_Key *companion __attribute__((swift_name("companion")));
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

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CoroutineName_.Key")))
@interface CoroutineName_Key : Base <KotlinCoroutineContextKey>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) CoroutineName_Key *shared __attribute__((swift_name("shared")));
@end

@protocol CoroutineScope_
@required
@property (readonly) id<KotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
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

__attribute__((objc_subclassing_restricted))
@interface CoroutineStart_ : KotlinEnum<CoroutineStart *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) CoroutineStart_ *default_ __attribute__((swift_name("default_")));
@property (class, readonly) CoroutineStart_ *lazy __attribute__((swift_name("lazy")));
@property (class, readonly) CoroutineStart_ *atomic __attribute__((swift_name("atomic")));
@property (class, readonly) CoroutineStart_ *undispatched __attribute__((swift_name("undispatched")));
+ (KotlinArray<CoroutineStart_ *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<CoroutineStart_ *> *entries __attribute__((swift_name("entries")));
- (void)invokeBlock:(id<KotlinSuspendFunction1>)block receiver:(id _Nullable)receiver completion_:(id<KotlinContinuation>)completion __attribute__((swift_name("invoke(block:receiver:completion_:)")));
@property (readonly) BOOL isLazy __attribute__((swift_name("isLazy")));
@end

@protocol Deferred_ <Job>
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

@protocol Delay
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)delayTime:(int64_t)time completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("delay(time:completionHandler:)"))) __attribute__((unavailable("Deprecated without replacement as an internal method never intended for public use")));
- (id<DisposableHandle>)invokeOnTimeoutTimeMillis:(int64_t)timeMillis block:(id<Runnable>)block context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("invokeOnTimeout(timeMillis:block:context:)")));
- (void)scheduleResumeAfterDelayTimeMillis:(int64_t)timeMillis continuation:(id<CancellableContinuation>)continuation __attribute__((swift_name("scheduleResumeAfterDelay(timeMillis:continuation:)")));
@end

@protocol Delay_
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)delayTime:(int64_t)time completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("delay(time:completionHandler:)"))) __attribute__((unavailable("Deprecated without replacement as an internal method never intended for public use")));
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

__attribute__((objc_subclassing_restricted))
@interface Dispatchers_ : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dispatchers __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Dispatchers_ *shared __attribute__((swift_name("shared")));
@property (readonly) CoroutineDispatcher *Default __attribute__((swift_name("Default")));
@property (readonly) MainCoroutineDispatcher *Main __attribute__((swift_name("Main")));
@property (readonly) CoroutineDispatcher *Unconfined __attribute__((swift_name("Unconfined")));
@end

@protocol DisposableHandle_
@required
- (void)dispose __attribute__((swift_name("dispose()")));
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


/**
 * @note annotations
 *   kotlinx.coroutines.DelicateCoroutinesApi
*/
__attribute__((objc_subclassing_restricted))
@interface GlobalScope_ : Base <CoroutineScope>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)globalScope __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) GlobalScope_ *shared __attribute__((swift_name("shared")));
@property (readonly) id<KotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
@end

@protocol Job_ <KotlinCoroutineContextElement>
@required
- (id<ChildHandle>)attachChildChild:(id<ChildJob>)child __attribute__((swift_name("attachChild(child:)")));
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (KotlinCancellationException *)getCancellationException __attribute__((swift_name("getCancellationException()")));
- (id<DisposableHandle>)invokeOnCompletionHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(handler:)")));
- (id<DisposableHandle>)invokeOnCompletionOnCancelling:(BOOL)onCancelling invokeImmediately:(BOOL)invokeImmediately handler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(onCancelling:invokeImmediately:handler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)joinWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("join(completionHandler:)")));
- (id<Job>)plusOther:(id<Job>)other __attribute__((swift_name("plus(other:)"))) __attribute__((unavailable("Operator '+' on two Job objects is meaningless. Job is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The job to the right of `+` just replaces the job the left of `+`.")));
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

__attribute__((objc_subclassing_restricted))
@interface JobKey : Base <KotlinCoroutineContextKey>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) JobKey *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface Job_Key : Base <KotlinCoroutineContextKey>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Job_Key *shared __attribute__((swift_name("shared")));
@end

@interface JobSupport_ : Base <Job, ChildJob, ParentJob>
- (instancetype)initWithActive:(BOOL)active __attribute__((swift_name("init(active:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("This is internal API and may be removed in the future releases")));

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
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
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

@interface MainCoroutineDispatcher_ : CoroutineDispatcher
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
- (id<ChildHandle>)attachChildChild:(id<ChildJob>)child __attribute__((swift_name("attachChild(child:)"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (KotlinCancellationException *)getCancellationException __attribute__((swift_name("getCancellationException()"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (id<DisposableHandle>)invokeOnCompletionHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(handler:)"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (id<DisposableHandle>)invokeOnCompletionOnCancelling:(BOOL)onCancelling invokeImmediately:(BOOL)invokeImmediately handler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(onCancelling:invokeImmediately:handler:)"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)joinWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("join(completionHandler:)"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (BOOL)start __attribute__((swift_name("start()"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<KotlinSequence> children __attribute__((swift_name("children"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@property (readonly) BOOL isCancelled __attribute__((swift_name("isCancelled"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@property (readonly) BOOL isCompleted __attribute__((swift_name("isCompleted"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@property (readonly) id<SelectClause0> onJoin __attribute__((swift_name("onJoin"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@property (readonly) id<Job> _Nullable parent __attribute__((swift_name("parent"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@end

__attribute__((objc_subclassing_restricted))
@interface NonCancellable_ : KotlinAbstractCoroutineContextElement <Job>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithKey:(id<KotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)nonCancellable __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) NonCancellable_ *shared __attribute__((swift_name("shared")));
- (id<ChildHandle>)attachChildChild:(id<ChildJob>)child __attribute__((swift_name("attachChild(child:)"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (KotlinCancellationException *)getCancellationException __attribute__((swift_name("getCancellationException()"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (id<DisposableHandle>)invokeOnCompletionHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(handler:)"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (id<DisposableHandle>)invokeOnCompletionOnCancelling:(BOOL)onCancelling invokeImmediately:(BOOL)invokeImmediately handler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(onCancelling:invokeImmediately:handler:)"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)joinWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("join(completionHandler:)"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (BOOL)start __attribute__((swift_name("start()"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<KotlinSequence> children __attribute__((swift_name("children"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@property (readonly) BOOL isCancelled __attribute__((swift_name("isCancelled"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@property (readonly) BOOL isCompleted __attribute__((swift_name("isCompleted"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@property (readonly) id<SelectClause0> onJoin __attribute__((swift_name("onJoin"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
@property (readonly) id<Job> _Nullable parent __attribute__((swift_name("parent"))) __attribute__((deprecated("NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")));
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

__attribute__((objc_subclassing_restricted))
@interface NonDisposableHandle_ : Base <DisposableHandle, ChildHandle>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)nonDisposableHandle __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) NonDisposableHandle_ *shared __attribute__((swift_name("shared")));
- (BOOL)childCancelledCause:(KotlinThrowable *)cause __attribute__((swift_name("childCancelled(cause:)")));
- (void)dispose __attribute__((swift_name("dispose()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<Job> _Nullable parent __attribute__((swift_name("parent")));
@end

@protocol ParentJob_ <Job>
@required
- (KotlinCancellationException *)getChildJobCancellationCause __attribute__((swift_name("getChildJobCancellationCause()")));
@end

@protocol Runnable
@required
- (void)run __attribute__((swift_name("run()")));
@end

@protocol Runnable_
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

__attribute__((objc_subclassing_restricted))
@interface TimeoutCancellationException_ : KotlinCancellationException <CopyableThrowable>
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
- (BOOL)offerElement:(id _Nullable)element __attribute__((swift_name("offer(element:)"))) __attribute__((unavailable("Deprecated in the favour of 'trySend' method")));

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
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (id<ReceiveChannel>)openSubscription __attribute__((swift_name("openSubscription()")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
@protocol BroadcastChannel_ <SendChannel>
@required
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
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

__attribute__((objc_subclassing_restricted))
@interface BufferOverflow_ : KotlinEnum<BufferOverflow *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) BufferOverflow_ *suspend __attribute__((swift_name("suspend")));
@property (class, readonly) BufferOverflow_ *dropOldest __attribute__((swift_name("dropOldest")));
@property (class, readonly) BufferOverflow_ *dropLatest __attribute__((swift_name("dropLatest")));
+ (KotlinArray<BufferOverflow_ *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<BufferOverflow_ *> *entries __attribute__((swift_name("entries")));
@end

@protocol ReceiveChannel
@required
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (id<ChannelIterator>)iterator __attribute__((swift_name("iterator()")));
- (id _Nullable)poll __attribute__((swift_name("poll()"))) __attribute__((unavailable("Deprecated in the favour of 'tryReceive'. Please note that the provided replacement does not rethrow channel's close cause as 'poll' did, for the precise replacement please refer to the 'poll' documentation")));

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
- (void)receiveOrNullWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("receiveOrNull(completionHandler:)"))) __attribute__((unavailable("Deprecated in favor of 'receiveCatching'. Please note that the provided replacement does not rethrow channel's close cause as 'receiveOrNull' did, for the detailed replacement please refer to the 'receiveOrNull' documentation")));
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
@property (readonly) id<SelectClause1> onReceiveOrNull __attribute__((swift_name("onReceiveOrNull"))) __attribute__((unavailable("Deprecated in favor of onReceiveCatching extension")));
@end

@protocol Channel <SendChannel, ReceiveChannel>
@required
@end

@protocol Channel_ <SendChannel, ReceiveChannel>
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

__attribute__((objc_subclassing_restricted))
@interface Channel_Factory : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)factory __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) Channel_Factory *shared __attribute__((swift_name("shared")));
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
@end

@protocol ChannelIterator_
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)hasNextWithCompletionHandler:(void (^)(Boolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("hasNext(completionHandler:)")));
- (id _Nullable)next __attribute__((swift_name("next()")));
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
@interface ClosedReceiveChannelException_ : KotlinNoSuchElementException
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

__attribute__((objc_subclassing_restricted))
@interface ClosedSendChannelException_ : KotlinIllegalStateException
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
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("ConflatedBroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithValue:(E _Nullable)value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("ConflatedBroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (BOOL)closeCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("close(cause:)")));
- (void)invokeOnCloseHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnClose(handler:)")));
- (BOOL)offerElement:(E _Nullable)element __attribute__((swift_name("offer(element:)"))) __attribute__((unavailable("Deprecated in the favour of 'trySend' method")));
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


/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
__attribute__((objc_subclassing_restricted))
@interface ConflatedBroadcastChannel_<E> : Base <BroadcastChannel>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("ConflatedBroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithValue:(E _Nullable)value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("ConflatedBroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (BOOL)closeCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("close(cause:)")));
- (void)invokeOnCloseHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnClose(handler:)")));
- (BOOL)offerElement:(E _Nullable)element __attribute__((swift_name("offer(element:)"))) __attribute__((unavailable("Deprecated in the favour of 'trySend' method")));
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

@protocol ProducerScope_ <CoroutineScope, SendChannel>
@required
@property (readonly) id<SendChannel> channel __attribute__((swift_name("channel")));
@end

@protocol ReceiveChannel_
@required
- (void)cancelCause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));
- (id<ChannelIterator>)iterator __attribute__((swift_name("iterator()")));
- (id _Nullable)poll __attribute__((swift_name("poll()"))) __attribute__((unavailable("Deprecated in the favour of 'tryReceive'. Please note that the provided replacement does not rethrow channel's close cause as 'poll' did, for the precise replacement please refer to the 'poll' documentation")));

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
- (void)receiveOrNullWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("receiveOrNull(completionHandler:)"))) __attribute__((unavailable("Deprecated in favor of 'receiveCatching'. Please note that the provided replacement does not rethrow channel's close cause as 'receiveOrNull' did, for the detailed replacement please refer to the 'receiveOrNull' documentation")));
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
@property (readonly) id<SelectClause1> onReceiveOrNull __attribute__((swift_name("onReceiveOrNull"))) __attribute__((unavailable("Deprecated in favor of onReceiveCatching extension")));
@end

@protocol SendChannel_
@required
- (BOOL)closeCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("close(cause:)")));
- (void)invokeOnCloseHandler:(void (^)(KotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnClose(handler:)")));
- (BOOL)offerElement:(id _Nullable)element __attribute__((swift_name("offer(element:)"))) __attribute__((unavailable("Deprecated in the favour of 'trySend' method")));

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


/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
@interface AbstractFlow_<T> : Base <Flow>
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

@protocol Flow_
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<FlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end

@protocol FlowCollector
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(id _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
@end

@protocol FlowCollector_
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

@protocol MutableSharedFlow_ <SharedFlow, FlowCollector>
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

@protocol MutableStateFlow_ <StateFlow, MutableSharedFlow>
@required
- (void)setValue:(id _Nullable)value __attribute__((swift_name("setValue(_:)")));
- (BOOL)compareAndSetExpect:(id _Nullable)expect update:(id _Nullable)update __attribute__((swift_name("compareAndSet(expect:update:)")));
@end

@protocol SharedFlow_ <Flow>
@required
@property (readonly) NSArray<id> *replayCache __attribute__((swift_name("replayCache")));
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

__attribute__((objc_subclassing_restricted))
@interface SharingCommand_ : KotlinEnum<SharingCommand *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharingCommand_ *start __attribute__((swift_name("start")));
@property (class, readonly) SharingCommand_ *stop __attribute__((swift_name("stop")));
@property (class, readonly) SharingCommand_ *stopAndResetReplayCache __attribute__((swift_name("stopAndResetReplayCache")));
+ (KotlinArray<SharingCommand_ *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharingCommand_ *> *entries __attribute__((swift_name("entries")));
@end

@protocol SharingStarted
@required
- (id<Flow>)commandSubscriptionCount:(id<StateFlow>)subscriptionCount __attribute__((swift_name("command(subscriptionCount:)")));
@end

@protocol SharingStarted_
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

__attribute__((objc_subclassing_restricted))
@interface SharingStarted_Companion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharingStarted_Companion *shared __attribute__((swift_name("shared")));
- (id<SharingStarted>)WhileSubscribedStopTimeoutMillis:(int64_t)stopTimeoutMillis replayExpirationMillis:(int64_t)replayExpirationMillis __attribute__((swift_name("WhileSubscribed(stopTimeoutMillis:replayExpirationMillis:)")));
@property (readonly) id<SharingStarted> Eagerly __attribute__((swift_name("Eagerly")));
@property (readonly) id<SharingStarted> Lazily __attribute__((swift_name("Lazily")));
@end

@protocol StateFlow_ <SharedFlow>
@required
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
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

@interface ChannelFlow_<T> : Base <FusibleFlow>
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

@protocol FusibleFlow_ <Flow>
@required
- (id<Flow>)fuseContext:(id<KotlinCoroutineContext>)context capacity:(int32_t)capacity onBufferOverflow:(BufferOverflow *)onBufferOverflow __attribute__((swift_name("fuse(context:capacity:onBufferOverflow:)")));
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

__attribute__((objc_subclassing_restricted))
@interface SendingCollector_<T> : Base <FlowCollector>
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

@interface AtomicOp_<__contravariant T> : OpDescriptor
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

@interface LockFreeLinkedListHead_ : LockFreeLinkedListNode
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)forEachBlock_:(void (^)(LockFreeLinkedListNode *))block __attribute__((swift_name("forEach(block_:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (LockFreeLinkedListNode * _Nullable)nextIfRemoved __attribute__((swift_name("nextIfRemoved()")));
- (BOOL)remove __attribute__((swift_name("remove()")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) BOOL isRemoved __attribute__((swift_name("isRemoved")));
@end

@interface LockFreeLinkedListNode_ : Base
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

@protocol MainDispatcherFactory
@required
- (MainCoroutineDispatcher *)createDispatcherAllFactories:(NSArray<id<MainDispatcherFactory>> *)allFactories __attribute__((swift_name("createDispatcher(allFactories:)")));
- (NSString * _Nullable)hintOnError __attribute__((swift_name("hintOnError()")));
@property (readonly) int32_t loadPriority __attribute__((swift_name("loadPriority")));
@end

@protocol MainDispatcherFactory_
@required
- (MainCoroutineDispatcher *)createDispatcherAllFactories:(NSArray<id<MainDispatcherFactory>> *)allFactories __attribute__((swift_name("createDispatcher(allFactories:)")));
- (NSString * _Nullable)hintOnError __attribute__((swift_name("hintOnError()")));
@property (readonly) int32_t loadPriority __attribute__((swift_name("loadPriority")));
@end

@interface OpDescriptor_ : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id _Nullable)performAffected:(id _Nullable)affected __attribute__((swift_name("perform(affected:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) AtomicOp<id> * _Nullable atomicOp __attribute__((swift_name("atomicOp")));
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

@interface ThreadSafeHeap_<T> : SynchronizedObject
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

@protocol ThreadSafeHeapNode_
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
- (void)onTimeoutTimeMillis:(int64_t)timeMillis block:(id<KotlinSuspendFunction0>)block __attribute__((swift_name("onTimeout(timeMillis:block:)"))) __attribute__((unavailable("Replaced with the same extension function")));
@end

@protocol SelectBuilder_
@required
- (void)invoke:(id<SelectClause0>)receiver block:(id<KotlinSuspendFunction0>)block __attribute__((swift_name("invoke(_:block:)")));
- (void)invoke:(id<SelectClause1>)receiver block___:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("invoke(_:block___:)")));
- (void)invoke:(id<SelectClause2>)receiver block____:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("invoke(_:block____:)")));
- (void)invoke:(id<SelectClause2>)receiver param:(id _Nullable)param block_:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("invoke(_:param:block_:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)onTimeoutTimeMillis:(int64_t)timeMillis block:(id<KotlinSuspendFunction0>)block __attribute__((swift_name("onTimeout(timeMillis:block:)"))) __attribute__((unavailable("Replaced with the same extension function")));
@end

@protocol SelectClause
@required
@property (readonly) id clauseObject __attribute__((swift_name("clauseObject")));
@property (readonly) KotlinUnit *(^(^ _Nullable onCancellationConstructor)(id<SelectInstance>, id _Nullable, id _Nullable))(KotlinThrowable *) __attribute__((swift_name("onCancellationConstructor")));
@property (readonly) id _Nullable (^processResFunc)(id, id _Nullable, id _Nullable) __attribute__((swift_name("processResFunc")));
@property (readonly) void (^regFunc)(id, id<SelectInstance>, id _Nullable) __attribute__((swift_name("regFunc")));
@end

@protocol SelectClause_
@required
@property (readonly) id clauseObject __attribute__((swift_name("clauseObject")));
@property (readonly) KotlinUnit *(^(^ _Nullable onCancellationConstructor)(id<SelectInstance>, id _Nullable, id _Nullable))(KotlinThrowable *) __attribute__((swift_name("onCancellationConstructor")));
@property (readonly) id _Nullable (^processResFunc)(id, id _Nullable, id _Nullable) __attribute__((swift_name("processResFunc")));
@property (readonly) void (^regFunc)(id, id<SelectInstance>, id _Nullable) __attribute__((swift_name("regFunc")));
@end

@protocol SelectClause0 <SelectClause>
@required
@end

@protocol SelectClause0_ <SelectClause>
@required
@end

@protocol SelectClause1 <SelectClause>
@required
@end

@protocol SelectClause1_ <SelectClause>
@required
@end

@protocol SelectClause2 <SelectClause>
@required
@end

@protocol SelectClause2_ <SelectClause>
@required
@end

@protocol SelectInstance
@required
- (void)disposeOnCompletionDisposableHandle:(id<DisposableHandle>)disposableHandle __attribute__((swift_name("disposeOnCompletion(disposableHandle:)")));
- (void)selectInRegistrationPhaseInternalResult:(id _Nullable)internalResult __attribute__((swift_name("selectInRegistrationPhase(internalResult:)")));
- (BOOL)trySelectClauseObject:(id)clauseObject result:(id _Nullable)result __attribute__((swift_name("trySelect(clauseObject:result:)")));
@property (readonly) id<KotlinCoroutineContext> context __attribute__((swift_name("context")));
@end

@protocol SelectInstance_
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
@property (readonly) id<SelectClause2> onLock __attribute__((swift_name("onLock"))) __attribute__((deprecated("Mutex.onLock deprecated without replacement. For additional details please refer to #2794")));
@end

@protocol Mutex_
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
@property (readonly) id<SelectClause2> onLock __attribute__((swift_name("onLock"))) __attribute__((deprecated("Mutex.onLock deprecated without replacement. For additional details please refer to #2794")));
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

@protocol Semaphore_
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

@protocol Clock
@required
- (Instant *)now __attribute__((swift_name("now()")));
@end

__attribute__((objc_subclassing_restricted))
@interface ClockCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ClockCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
@interface ClockSystem : Base <Clock>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)system __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ClockSystem *shared __attribute__((swift_name("shared")));
- (Instant *)now __attribute__((swift_name("now()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/DateTimePeriodIso8601Serializer))
*/
@interface DateTimePeriod : Base
@property (class, readonly, getter=companion) DateTimePeriodCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t days __attribute__((swift_name("days")));
@property (readonly) int32_t hours __attribute__((swift_name("hours")));
@property (readonly) int32_t minutes __attribute__((swift_name("minutes")));
@property (readonly) int32_t months __attribute__((swift_name("months")));
@property (readonly) int32_t nanoseconds __attribute__((swift_name("nanoseconds")));
@property (readonly) int32_t seconds __attribute__((swift_name("seconds")));
@property (readonly) int32_t years __attribute__((swift_name("years")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/DatePeriodIso8601Serializer))
*/
__attribute__((objc_subclassing_restricted))
@interface DatePeriod : DateTimePeriod
- (instancetype)initWithYears:(int32_t)years months:(int32_t)months days:(int32_t)days __attribute__((swift_name("init(years:months:days:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) DatePeriodCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) int32_t days __attribute__((swift_name("days")));
@property (readonly) int32_t hours __attribute__((swift_name("hours")));
@property (readonly) int32_t minutes __attribute__((swift_name("minutes")));
@property (readonly) int32_t nanoseconds __attribute__((swift_name("nanoseconds")));
@property (readonly) int32_t seconds __attribute__((swift_name("seconds")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DatePeriod.Companion")))
@interface DatePeriodCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DatePeriodCompanion *shared __attribute__((swift_name("shared")));
- (DatePeriod *)parseText:(NSString *)text __attribute__((swift_name("parse(text:)")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimeArithmeticException : KotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString *)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable *)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString *)message cause:(KotlinThrowable *)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimePeriod.Companion")))
@interface DateTimePeriodCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimePeriodCompanion *shared __attribute__((swift_name("shared")));
- (DateTimePeriod *)parseText:(NSString *)text __attribute__((swift_name("parse(text:)")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/DateTimeUnitSerializer))
*/
@interface DateTimeUnit : Base
@property (class, readonly, getter=companion) DateTimeUnitCompanion *companion __attribute__((swift_name("companion")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)formatToStringValue:(int32_t)value unit:(NSString *)unit __attribute__((swift_name("formatToString(value:unit:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)formatToStringValue:(int64_t)value unit_:(NSString *)unit __attribute__((swift_name("formatToString(value:unit_:)")));
- (DateTimeUnit *)timesScalar:(int32_t)scalar __attribute__((swift_name("times(scalar:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeUnit.Companion")))
@interface DateTimeUnitCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimeUnitCompanion *shared __attribute__((swift_name("shared")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@property (readonly) DateTimeUnitMonthBased *CENTURY __attribute__((swift_name("CENTURY")));
@property (readonly) DateTimeUnitDayBased *DAY __attribute__((swift_name("DAY")));
@property (readonly) DateTimeUnitTimeBased *HOUR __attribute__((swift_name("HOUR")));
@property (readonly) DateTimeUnitTimeBased *MICROSECOND __attribute__((swift_name("MICROSECOND")));
@property (readonly) DateTimeUnitTimeBased *MILLISECOND __attribute__((swift_name("MILLISECOND")));
@property (readonly) DateTimeUnitTimeBased *MINUTE __attribute__((swift_name("MINUTE")));
@property (readonly) DateTimeUnitMonthBased *MONTH __attribute__((swift_name("MONTH")));
@property (readonly) DateTimeUnitTimeBased *NANOSECOND __attribute__((swift_name("NANOSECOND")));
@property (readonly) DateTimeUnitMonthBased *QUARTER __attribute__((swift_name("QUARTER")));
@property (readonly) DateTimeUnitTimeBased *SECOND __attribute__((swift_name("SECOND")));
@property (readonly) DateTimeUnitDayBased *WEEK __attribute__((swift_name("WEEK")));
@property (readonly) DateTimeUnitMonthBased *YEAR __attribute__((swift_name("YEAR")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/DateBasedDateTimeUnitSerializer))
*/
__attribute__((swift_name("DateTimeUnit.DateBased")))
@interface DateTimeUnitDateBased : DateTimeUnit
@property (class, readonly, getter=companion) DateTimeUnitDateBasedCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeUnit.DateBasedCompanion")))
@interface DateTimeUnitDateBasedCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimeUnitDateBasedCompanion *shared __attribute__((swift_name("shared")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/DayBasedDateTimeUnitSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeUnit.DayBased")))
@interface DateTimeUnitDayBased : DateTimeUnitDateBased
- (instancetype)initWithDays:(int32_t)days __attribute__((swift_name("init(days:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) DateTimeUnitDayBasedCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (DateTimeUnitDayBased *)timesScalar:(int32_t)scalar __attribute__((swift_name("times(scalar:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t days __attribute__((swift_name("days")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeUnit.DayBasedCompanion")))
@interface DateTimeUnitDayBasedCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimeUnitDayBasedCompanion *shared __attribute__((swift_name("shared")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/MonthBasedDateTimeUnitSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeUnit.MonthBased")))
@interface DateTimeUnitMonthBased : DateTimeUnitDateBased
- (instancetype)initWithMonths:(int32_t)months __attribute__((swift_name("init(months:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) DateTimeUnitMonthBasedCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (DateTimeUnitMonthBased *)timesScalar:(int32_t)scalar __attribute__((swift_name("times(scalar:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t months __attribute__((swift_name("months")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeUnit.MonthBasedCompanion")))
@interface DateTimeUnitMonthBasedCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimeUnitMonthBasedCompanion *shared __attribute__((swift_name("shared")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/TimeBasedDateTimeUnitSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeUnit.TimeBased")))
@interface DateTimeUnitTimeBased : DateTimeUnit
- (instancetype)initWithNanoseconds:(int64_t)nanoseconds __attribute__((swift_name("init(nanoseconds:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) DateTimeUnitTimeBasedCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (DateTimeUnitTimeBased *)timesScalar:(int32_t)scalar __attribute__((swift_name("times(scalar:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t duration __attribute__((swift_name("duration")));
@property (readonly) int64_t nanoseconds __attribute__((swift_name("nanoseconds")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeUnit.TimeBasedCompanion")))
@interface DateTimeUnitTimeBasedCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimeUnitTimeBasedCompanion *shared __attribute__((swift_name("shared")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
@interface DayOfWeek : KotlinEnum<DayOfWeek *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) DayOfWeek *monday __attribute__((swift_name("monday")));
@property (class, readonly) DayOfWeek *tuesday __attribute__((swift_name("tuesday")));
@property (class, readonly) DayOfWeek *wednesday __attribute__((swift_name("wednesday")));
@property (class, readonly) DayOfWeek *thursday __attribute__((swift_name("thursday")));
@property (class, readonly) DayOfWeek *friday __attribute__((swift_name("friday")));
@property (class, readonly) DayOfWeek *saturday __attribute__((swift_name("saturday")));
@property (class, readonly) DayOfWeek *sunday __attribute__((swift_name("sunday")));
+ (KotlinArray<DayOfWeek *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<DayOfWeek *> *entries __attribute__((swift_name("entries")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/TimeZoneSerializer))
*/
@interface TimeZone : Base
@property (class, readonly, getter=companion) TimeZoneCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (Instant *)toInstant:(LocalDateTime *)receiver __attribute__((swift_name("toInstant(_:)")));
- (LocalDateTime *)toLocalDateTime:(Instant *)receiver __attribute__((swift_name("toLocalDateTime(_:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/FixedOffsetTimeZoneSerializer))
*/
__attribute__((objc_subclassing_restricted))
@interface FixedOffsetTimeZone : TimeZone
- (instancetype)initWithOffset:(UtcOffset *)offset __attribute__((swift_name("init(offset:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) FixedOffsetTimeZoneCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) UtcOffset *offset __attribute__((swift_name("offset")));
@property (readonly) int32_t totalSeconds __attribute__((swift_name("totalSeconds"))) __attribute__((deprecated("Use offset.totalSeconds")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FixedOffsetTimeZone.Companion")))
@interface FixedOffsetTimeZoneCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) FixedOffsetTimeZoneCompanion *shared __attribute__((swift_name("shared")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinIllegalArgumentException : KotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
@interface IllegalTimeZoneException : KotlinIllegalArgumentException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString *)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable *)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString *)message cause:(KotlinThrowable *)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/InstantIso8601Serializer))
*/
__attribute__((objc_subclassing_restricted))
@interface Instant : Base <KotlinComparable>
@property (class, readonly, getter=companion) InstantCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(Instant *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (Instant *)minusDuration:(int64_t)duration __attribute__((swift_name("minus(duration:)")));
- (int64_t)minusOther:(Instant *)other __attribute__((swift_name("minus(other:)")));
- (Instant *)plusDuration:(int64_t)duration __attribute__((swift_name("plus(duration:)")));
- (int64_t)toEpochMilliseconds __attribute__((swift_name("toEpochMilliseconds()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t epochSeconds __attribute__((swift_name("epochSeconds")));
@property (readonly) int32_t nanosecondsOfSecond __attribute__((swift_name("nanosecondsOfSecond")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Instant.Companion")))
@interface InstantCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InstantCompanion *shared __attribute__((swift_name("shared")));
- (Instant *)fromEpochMillisecondsEpochMilliseconds:(int64_t)epochMilliseconds __attribute__((swift_name("fromEpochMilliseconds(epochMilliseconds:)")));
- (Instant *)fromEpochSecondsEpochSeconds:(int64_t)epochSeconds nanosecondAdjustment:(int32_t)nanosecondAdjustment __attribute__((swift_name("fromEpochSeconds(epochSeconds:nanosecondAdjustment:)")));
- (Instant *)fromEpochSecondsEpochSeconds:(int64_t)epochSeconds nanosecondAdjustment_:(int64_t)nanosecondAdjustment __attribute__((swift_name("fromEpochSeconds(epochSeconds:nanosecondAdjustment_:)")));
- (Instant *)now __attribute__((swift_name("now()"))) __attribute__((unavailable("Use Clock.System.now() instead")));
- (Instant *)parseInput:(id)input format:(id<DateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@property (readonly) Instant *DISTANT_FUTURE __attribute__((swift_name("DISTANT_FUTURE")));
@property (readonly) Instant *DISTANT_PAST __attribute__((swift_name("DISTANT_PAST")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/LocalDateIso8601Serializer))
*/
__attribute__((objc_subclassing_restricted))
@interface LocalDate : Base <KotlinComparable>
- (instancetype)initWithYear:(int32_t)year monthNumber:(int32_t)monthNumber dayOfMonth:(int32_t)dayOfMonth __attribute__((swift_name("init(year:monthNumber:dayOfMonth:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithYear:(int32_t)year month:(Month *)month dayOfMonth:(int32_t)dayOfMonth __attribute__((swift_name("init(year:month:dayOfMonth:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) LocalDateCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(LocalDate *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (int32_t)toEpochDays __attribute__((swift_name("toEpochDays()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t dayOfMonth __attribute__((swift_name("dayOfMonth")));
@property (readonly) DayOfWeek *dayOfWeek __attribute__((swift_name("dayOfWeek")));
@property (readonly) int32_t dayOfYear __attribute__((swift_name("dayOfYear")));
@property (readonly) Month *month __attribute__((swift_name("month")));
@property (readonly) int32_t monthNumber __attribute__((swift_name("monthNumber")));
@property (readonly) int32_t year __attribute__((swift_name("year")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LocalDate.Companion")))
@interface LocalDateCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateCompanion *shared __attribute__((swift_name("shared")));
- (id<DateTimeFormat>)FormatBlock:(void (^)(id<DateTimeFormatBuilderWithDate>))block __attribute__((swift_name("Format(block:)")));
- (LocalDate *)fromEpochDaysEpochDays:(int32_t)epochDays __attribute__((swift_name("fromEpochDays(epochDays:)")));
- (LocalDate *)parseInput:(id)input format:(id<DateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LocalDate.Formats")))
@interface LocalDateFormats : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)formats __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateFormats *shared __attribute__((swift_name("shared")));
@property (readonly) id<DateTimeFormat> ISO __attribute__((swift_name("ISO")));
@property (readonly) id<DateTimeFormat> ISO_BASIC __attribute__((swift_name("ISO_BASIC")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/LocalDateTimeIso8601Serializer))
*/
__attribute__((objc_subclassing_restricted))
@interface LocalDateTime : Base <KotlinComparable>
- (instancetype)initWithDate:(LocalDate *)date time:(LocalTime *)time __attribute__((swift_name("init(date:time:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithYear:(int32_t)year monthNumber:(int32_t)monthNumber dayOfMonth:(int32_t)dayOfMonth hour:(int32_t)hour minute:(int32_t)minute second:(int32_t)second nanosecond:(int32_t)nanosecond __attribute__((swift_name("init(year:monthNumber:dayOfMonth:hour:minute:second:nanosecond:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithYear:(int32_t)year month:(Month *)month dayOfMonth:(int32_t)dayOfMonth hour:(int32_t)hour minute:(int32_t)minute second:(int32_t)second nanosecond:(int32_t)nanosecond __attribute__((swift_name("init(year:month:dayOfMonth:hour:minute:second:nanosecond:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) LocalDateTimeCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(LocalDateTime *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) LocalDate *date __attribute__((swift_name("date")));
@property (readonly) int32_t dayOfMonth __attribute__((swift_name("dayOfMonth")));
@property (readonly) DayOfWeek *dayOfWeek __attribute__((swift_name("dayOfWeek")));
@property (readonly) int32_t dayOfYear __attribute__((swift_name("dayOfYear")));
@property (readonly) int32_t hour __attribute__((swift_name("hour")));
@property (readonly) int32_t minute __attribute__((swift_name("minute")));
@property (readonly) Month *month __attribute__((swift_name("month")));
@property (readonly) int32_t monthNumber __attribute__((swift_name("monthNumber")));
@property (readonly) int32_t nanosecond __attribute__((swift_name("nanosecond")));
@property (readonly) int32_t second __attribute__((swift_name("second")));
@property (readonly) LocalTime *time __attribute__((swift_name("time")));
@property (readonly) int32_t year __attribute__((swift_name("year")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LocalDateTime.Companion")))
@interface LocalDateTimeCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateTimeCompanion *shared __attribute__((swift_name("shared")));
- (id<DateTimeFormat>)FormatBuilder:(void (^)(id<DateTimeFormatBuilderWithDateTime>))builder __attribute__((swift_name("Format(builder:)")));
- (LocalDateTime *)parseInput:(id)input format:(id<DateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LocalDateTime.Formats")))
@interface LocalDateTimeFormats : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)formats __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateTimeFormats *shared __attribute__((swift_name("shared")));
@property (readonly) id<DateTimeFormat> ISO __attribute__((swift_name("ISO")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/LocalTimeIso8601Serializer))
*/
__attribute__((objc_subclassing_restricted))
@interface LocalTime : Base <KotlinComparable>
- (instancetype)initWithHour:(int32_t)hour minute:(int32_t)minute second:(int32_t)second nanosecond:(int32_t)nanosecond __attribute__((swift_name("init(hour:minute:second:nanosecond:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) LocalTimeCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(LocalTime *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (int32_t)toMillisecondOfDay __attribute__((swift_name("toMillisecondOfDay()")));
- (int64_t)toNanosecondOfDay __attribute__((swift_name("toNanosecondOfDay()")));
- (int32_t)toSecondOfDay __attribute__((swift_name("toSecondOfDay()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t hour __attribute__((swift_name("hour")));
@property (readonly) int32_t minute __attribute__((swift_name("minute")));
@property (readonly) int32_t nanosecond __attribute__((swift_name("nanosecond")));
@property (readonly) int32_t second __attribute__((swift_name("second")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LocalTime.Companion")))
@interface LocalTimeCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalTimeCompanion *shared __attribute__((swift_name("shared")));
- (id<DateTimeFormat>)FormatBuilder:(void (^)(id<DateTimeFormatBuilderWithTime>))builder __attribute__((swift_name("Format(builder:)")));
- (LocalTime *)fromMillisecondOfDayMillisecondOfDay:(int32_t)millisecondOfDay __attribute__((swift_name("fromMillisecondOfDay(millisecondOfDay:)")));
- (LocalTime *)fromNanosecondOfDayNanosecondOfDay:(int64_t)nanosecondOfDay __attribute__((swift_name("fromNanosecondOfDay(nanosecondOfDay:)")));
- (LocalTime *)fromSecondOfDaySecondOfDay:(int32_t)secondOfDay __attribute__((swift_name("fromSecondOfDay(secondOfDay:)")));
- (LocalTime *)parseInput:(id)input format:(id<DateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LocalTime.Formats")))
@interface LocalTimeFormats : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)formats __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalTimeFormats *shared __attribute__((swift_name("shared")));
@property (readonly) id<DateTimeFormat> ISO __attribute__((swift_name("ISO")));
@end

__attribute__((objc_subclassing_restricted))
@interface Month : KotlinEnum<Month *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) Month *january __attribute__((swift_name("january")));
@property (class, readonly) Month *february __attribute__((swift_name("february")));
@property (class, readonly) Month *march __attribute__((swift_name("march")));
@property (class, readonly) Month *april __attribute__((swift_name("april")));
@property (class, readonly) Month *may __attribute__((swift_name("may")));
@property (class, readonly) Month *june __attribute__((swift_name("june")));
@property (class, readonly) Month *july __attribute__((swift_name("july")));
@property (class, readonly) Month *august __attribute__((swift_name("august")));
@property (class, readonly) Month *september __attribute__((swift_name("september")));
@property (class, readonly) Month *october __attribute__((swift_name("october")));
@property (class, readonly) Month *november __attribute__((swift_name("november")));
@property (class, readonly) Month *december __attribute__((swift_name("december")));
+ (KotlinArray<Month *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<Month *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TimeZone.Companion")))
@interface TimeZoneCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) TimeZoneCompanion *shared __attribute__((swift_name("shared")));
- (TimeZone *)currentSystemDefault __attribute__((swift_name("currentSystemDefault()")));
- (TimeZone *)ofZoneId:(NSString *)zoneId __attribute__((swift_name("of(zoneId:)")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@property (readonly) FixedOffsetTimeZone *UTC __attribute__((swift_name("UTC")));
@property (readonly) NSSet<NSString *> *availableZoneIds __attribute__((swift_name("availableZoneIds")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/UtcOffsetSerializer))
*/
__attribute__((objc_subclassing_restricted))
@interface UtcOffset : Base
@property (class, readonly, getter=companion) UtcOffsetCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t totalSeconds __attribute__((swift_name("totalSeconds")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UtcOffset.Companion")))
@interface UtcOffsetCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) UtcOffsetCompanion *shared __attribute__((swift_name("shared")));
- (id<DateTimeFormat>)FormatBlock:(void (^)(id<DateTimeFormatBuilderWithUtcOffset>))block __attribute__((swift_name("Format(block:)")));
- (UtcOffset *)parseInput:(id)input format:(id<DateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@property (readonly) UtcOffset *ZERO __attribute__((swift_name("ZERO")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UtcOffset.Formats")))
@interface UtcOffsetFormats : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)formats __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) UtcOffsetFormats *shared __attribute__((swift_name("shared")));
@property (readonly) id<DateTimeFormat> FOUR_DIGITS __attribute__((swift_name("FOUR_DIGITS")));
@property (readonly) id<DateTimeFormat> ISO __attribute__((swift_name("ISO")));
@property (readonly) id<DateTimeFormat> ISO_BASIC __attribute__((swift_name("ISO_BASIC")));
@end

__attribute__((objc_subclassing_restricted))
@interface AmPmMarker : KotlinEnum<AmPmMarker *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) AmPmMarker *am __attribute__((swift_name("am")));
@property (class, readonly) AmPmMarker *pm __attribute__((swift_name("pm")));
+ (KotlinArray<AmPmMarker *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<AmPmMarker *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimeComponents : Base
@property (class, readonly, getter=companion) DateTimeComponentsCompanion *companion __attribute__((swift_name("companion")));
- (void)setDateLocalDate:(LocalDate *)localDate __attribute__((swift_name("setDate(localDate:)")));
- (void)setDateTimeLocalDateTime:(LocalDateTime *)localDateTime __attribute__((swift_name("setDateTime(localDateTime:)")));
- (void)setDateTimeOffsetInstant:(Instant *)instant utcOffset:(UtcOffset *)utcOffset __attribute__((swift_name("setDateTimeOffset(instant:utcOffset:)")));
- (void)setDateTimeOffsetLocalDateTime:(LocalDateTime *)localDateTime utcOffset:(UtcOffset *)utcOffset __attribute__((swift_name("setDateTimeOffset(localDateTime:utcOffset:)")));
- (void)setOffsetUtcOffset:(UtcOffset *)utcOffset __attribute__((swift_name("setOffset(utcOffset:)")));
- (void)setTimeLocalTime:(LocalTime *)localTime __attribute__((swift_name("setTime(localTime:)")));
- (Instant *)toInstantUsingOffset __attribute__((swift_name("toInstantUsingOffset()")));
- (LocalDate *)toLocalDate __attribute__((swift_name("toLocalDate()")));
- (LocalDateTime *)toLocalDateTime __attribute__((swift_name("toLocalDateTime()")));
- (LocalTime *)toLocalTime __attribute__((swift_name("toLocalTime()")));
- (UtcOffset *)toUtcOffset __attribute__((swift_name("toUtcOffset()")));
@property AmPmMarker * _Nullable amPm __attribute__((swift_name("amPm")));
@property Int * _Nullable dayOfMonth __attribute__((swift_name("dayOfMonth")));
@property DayOfWeek * _Nullable dayOfWeek __attribute__((swift_name("dayOfWeek")));
@property Int * _Nullable hour __attribute__((swift_name("hour")));
@property Int * _Nullable hourOfAmPm __attribute__((swift_name("hourOfAmPm")));
@property Int * _Nullable minute __attribute__((swift_name("minute")));
@property Month * _Nullable month __attribute__((swift_name("month")));
@property Int * _Nullable monthNumber __attribute__((swift_name("monthNumber")));
@property Int * _Nullable nanosecond __attribute__((swift_name("nanosecond")));
@property Int * _Nullable offsetHours __attribute__((swift_name("offsetHours")));
@property Boolean * _Nullable offsetIsNegative __attribute__((swift_name("offsetIsNegative")));
@property Int * _Nullable offsetMinutesOfHour __attribute__((swift_name("offsetMinutesOfHour")));
@property Int * _Nullable offsetSecondsOfMinute __attribute__((swift_name("offsetSecondsOfMinute")));
@property Int * _Nullable second __attribute__((swift_name("second")));
@property NSString * _Nullable timeZoneId __attribute__((swift_name("timeZoneId")));
@property Int * _Nullable year __attribute__((swift_name("year")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeComponents.Companion")))
@interface DateTimeComponentsCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimeComponentsCompanion *shared __attribute__((swift_name("shared")));
- (id<DateTimeFormat>)FormatBlock:(void (^)(id<DateTimeFormatBuilderWithDateTimeComponents>))block __attribute__((swift_name("Format(block:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeComponents.Formats")))
@interface DateTimeComponentsFormats : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)formats __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimeComponentsFormats *shared __attribute__((swift_name("shared")));
@property (readonly) id<DateTimeFormat> ISO_DATE_TIME_OFFSET __attribute__((swift_name("ISO_DATE_TIME_OFFSET")));
@property (readonly) id<DateTimeFormat> RFC_1123 __attribute__((swift_name("RFC_1123")));
@end

@protocol DateTimeFormat
@required
- (NSString *)formatValue:(id _Nullable)value __attribute__((swift_name("format(value:)")));
- (id<KotlinAppendable>)formatToAppendable:(id<KotlinAppendable>)appendable value:(id _Nullable)value __attribute__((swift_name("formatTo(appendable:value:)")));
- (id _Nullable)parseInput:(id)input __attribute__((swift_name("parse(input:)")));
- (id _Nullable)parseOrNullInput:(id)input __attribute__((swift_name("parseOrNull(input:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimeFormatCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimeFormatCompanion *shared __attribute__((swift_name("shared")));
- (NSString *)formatAsKotlinBuilderDslFormat:(id<DateTimeFormat>)format __attribute__((swift_name("formatAsKotlinBuilderDsl(format:)")));
@end

@protocol DateTimeFormatBuilder
@required
- (void)charsValue:(NSString *)value __attribute__((swift_name("chars(value:)")));
@end

@protocol DateTimeFormatBuilderWithDate <DateTimeFormatBuilder>
@required
- (void)dateFormat:(id<DateTimeFormat>)format __attribute__((swift_name("date(format:)")));
- (void)dayOfMonthPadding:(Padding *)padding __attribute__((swift_name("dayOfMonth(padding:)")));
- (void)dayOfWeekNames:(DayOfWeekNames *)names __attribute__((swift_name("dayOfWeek(names:)")));
- (void)monthNameNames:(MonthNames *)names __attribute__((swift_name("monthName(names:)")));
- (void)monthNumberPadding:(Padding *)padding __attribute__((swift_name("monthNumber(padding:)")));
- (void)yearPadding:(Padding *)padding __attribute__((swift_name("year(padding:)")));
- (void)yearTwoDigitsBaseYear:(int32_t)baseYear __attribute__((swift_name("yearTwoDigits(baseYear:)")));
@end

@protocol DateTimeFormatBuilderWithTime <DateTimeFormatBuilder>
@required
- (void)amPmHourPadding:(Padding *)padding __attribute__((swift_name("amPmHour(padding:)")));
- (void)amPmMarkerAm:(NSString *)am pm:(NSString *)pm __attribute__((swift_name("amPmMarker(am:pm:)")));
- (void)hourPadding:(Padding *)padding __attribute__((swift_name("hour(padding:)")));
- (void)minutePadding:(Padding *)padding __attribute__((swift_name("minute(padding:)")));
- (void)secondPadding:(Padding *)padding __attribute__((swift_name("second(padding:)")));
- (void)secondFractionFixedLength:(int32_t)fixedLength __attribute__((swift_name("secondFraction(fixedLength:)")));
- (void)secondFractionMinLength:(int32_t)minLength maxLength:(int32_t)maxLength __attribute__((swift_name("secondFraction(minLength:maxLength:)")));
- (void)timeFormat:(id<DateTimeFormat>)format __attribute__((swift_name("time(format:)")));
@end

@protocol DateTimeFormatBuilderWithDateTime <DateTimeFormatBuilderWithDate, DateTimeFormatBuilderWithTime>
@required
- (void)dateTimeFormat:(id<DateTimeFormat>)format __attribute__((swift_name("dateTime(format:)")));
@end

@protocol DateTimeFormatBuilderWithUtcOffset <DateTimeFormatBuilder>
@required
- (void)offsetFormat:(id<DateTimeFormat>)format __attribute__((swift_name("offset(format:)")));
- (void)offsetHoursPadding:(Padding *)padding __attribute__((swift_name("offsetHours(padding:)")));
- (void)offsetMinutesOfHourPadding:(Padding *)padding __attribute__((swift_name("offsetMinutesOfHour(padding:)")));
- (void)offsetSecondsOfMinutePadding:(Padding *)padding __attribute__((swift_name("offsetSecondsOfMinute(padding:)")));
@end

@protocol DateTimeFormatBuilderWithDateTimeComponents <DateTimeFormatBuilderWithDateTime, DateTimeFormatBuilderWithUtcOffset>
@required
- (void)dateTimeComponentsFormat:(id<DateTimeFormat>)format __attribute__((swift_name("dateTimeComponents(format:)")));
- (void)timeZoneId __attribute__((swift_name("timeZoneId()")));
@end

__attribute__((objc_subclassing_restricted))
@interface DayOfWeekNames : Base
- (instancetype)initWithNames:(NSArray<NSString *> *)names __attribute__((swift_name("init(names:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMonday:(NSString *)monday tuesday:(NSString *)tuesday wednesday:(NSString *)wednesday thursday:(NSString *)thursday friday:(NSString *)friday saturday:(NSString *)saturday sunday:(NSString *)sunday __attribute__((swift_name("init(monday:tuesday:wednesday:thursday:friday:saturday:sunday:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) DayOfWeekNamesCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *names __attribute__((swift_name("names")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DayOfWeekNames.Companion")))
@interface DayOfWeekNamesCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DayOfWeekNamesCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) DayOfWeekNames *ENGLISH_ABBREVIATED __attribute__((swift_name("ENGLISH_ABBREVIATED")));
@property (readonly) DayOfWeekNames *ENGLISH_FULL __attribute__((swift_name("ENGLISH_FULL")));
@end

__attribute__((objc_subclassing_restricted))
@interface MonthNames : Base
- (instancetype)initWithNames:(NSArray<NSString *> *)names __attribute__((swift_name("init(names:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithJanuary:(NSString *)january february:(NSString *)february march:(NSString *)march april:(NSString *)april may:(NSString *)may june:(NSString *)june july:(NSString *)july august:(NSString *)august september:(NSString *)september october:(NSString *)october november:(NSString *)november december:(NSString *)december __attribute__((swift_name("init(january:february:march:april:may:june:july:august:september:october:november:december:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) MonthNamesCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *names __attribute__((swift_name("names")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MonthNames.Companion")))
@interface MonthNamesCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) MonthNamesCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) MonthNames *ENGLISH_ABBREVIATED __attribute__((swift_name("ENGLISH_ABBREVIATED")));
@property (readonly) MonthNames *ENGLISH_FULL __attribute__((swift_name("ENGLISH_FULL")));
@end

__attribute__((objc_subclassing_restricted))
@interface Padding : KotlinEnum<Padding *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) Padding *none __attribute__((swift_name("none")));
@property (class, readonly) Padding *zero __attribute__((swift_name("zero")));
@property (class, readonly) Padding *space __attribute__((swift_name("space")));
+ (KotlinArray<Padding *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<Padding *> *entries __attribute__((swift_name("entries")));
@end

@protocol SerializationStrategy
@required
- (void)serializeEncoder:(id<Encoder>)encoder value:(id _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

@protocol DeserializationStrategy
@required
- (id _Nullable)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

@protocol KSerializer <SerializationStrategy, DeserializationStrategy>
@required
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface AbstractPolymorphicSerializer<T> : Base <KSerializer>
- (T)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<DeserializationStrategy> _Nullable)findPolymorphicSerializerOrNullDecoder:(id<CompositeDecoder>)decoder klassName:(NSString * _Nullable)klassName __attribute__((swift_name("findPolymorphicSerializerOrNull(decoder:klassName:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<SerializationStrategy> _Nullable)findPolymorphicSerializerOrNullEncoder:(id<Encoder>)encoder value:(T)value __attribute__((swift_name("findPolymorphicSerializerOrNull(encoder:value:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(T)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<KotlinKClass> baseClass __attribute__((swift_name("baseClass")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateBasedDateTimeUnitSerializer : AbstractPolymorphicSerializer<DateTimeUnitDateBased *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dateBasedDateTimeUnitSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateBasedDateTimeUnitSerializer *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<DeserializationStrategy> _Nullable)findPolymorphicSerializerOrNullDecoder:(id<CompositeDecoder>)decoder klassName:(NSString * _Nullable)klassName __attribute__((swift_name("findPolymorphicSerializerOrNull(decoder:klassName:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<SerializationStrategy> _Nullable)findPolymorphicSerializerOrNullEncoder:(id<Encoder>)encoder value:(DateTimeUnitDateBased *)value __attribute__((swift_name("findPolymorphicSerializerOrNull(encoder:value:)")));
@property (readonly) id<KotlinKClass> baseClass __attribute__((swift_name("baseClass")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DatePeriodComponentSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)datePeriodComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DatePeriodComponentSerializer *shared __attribute__((swift_name("shared")));
- (DatePeriod *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(DatePeriod *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DatePeriodIso8601Serializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)datePeriodIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DatePeriodIso8601Serializer *shared __attribute__((swift_name("shared")));
- (DatePeriod *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(DatePeriod *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimePeriodComponentSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dateTimePeriodComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimePeriodComponentSerializer *shared __attribute__((swift_name("shared")));
- (DateTimePeriod *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(DateTimePeriod *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimePeriodIso8601Serializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dateTimePeriodIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimePeriodIso8601Serializer *shared __attribute__((swift_name("shared")));
- (DateTimePeriod *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(DateTimePeriod *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimeUnitSerializer : AbstractPolymorphicSerializer<DateTimeUnit *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dateTimeUnitSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimeUnitSerializer *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<DeserializationStrategy> _Nullable)findPolymorphicSerializerOrNullDecoder:(id<CompositeDecoder>)decoder klassName:(NSString * _Nullable)klassName __attribute__((swift_name("findPolymorphicSerializerOrNull(decoder:klassName:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<SerializationStrategy> _Nullable)findPolymorphicSerializerOrNullEncoder:(id<Encoder>)encoder value:(DateTimeUnit *)value __attribute__((swift_name("findPolymorphicSerializerOrNull(encoder:value:)")));
@property (readonly) id<KotlinKClass> baseClass __attribute__((swift_name("baseClass")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DayBasedDateTimeUnitSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dayBasedDateTimeUnitSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DayBasedDateTimeUnitSerializer *shared __attribute__((swift_name("shared")));
- (DateTimeUnitDayBased *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(DateTimeUnitDayBased *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DayOfWeekSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dayOfWeekSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DayOfWeekSerializer *shared __attribute__((swift_name("shared")));
- (DayOfWeek *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(DayOfWeek *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface FixedOffsetTimeZoneSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)fixedOffsetTimeZoneSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) FixedOffsetTimeZoneSerializer *shared __attribute__((swift_name("shared")));
- (FixedOffsetTimeZone *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(FixedOffsetTimeZone *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface InstantComponentSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)instantComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InstantComponentSerializer *shared __attribute__((swift_name("shared")));
- (Instant *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(Instant *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface InstantIso8601Serializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)instantIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InstantIso8601Serializer *shared __attribute__((swift_name("shared")));
- (Instant *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(Instant *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalDateComponentSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localDateComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateComponentSerializer *shared __attribute__((swift_name("shared")));
- (LocalDate *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(LocalDate *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalDateIso8601Serializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localDateIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateIso8601Serializer *shared __attribute__((swift_name("shared")));
- (LocalDate *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(LocalDate *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalDateTimeComponentSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localDateTimeComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateTimeComponentSerializer *shared __attribute__((swift_name("shared")));
- (LocalDateTime *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(LocalDateTime *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalDateTimeIso8601Serializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localDateTimeIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateTimeIso8601Serializer *shared __attribute__((swift_name("shared")));
- (LocalDateTime *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(LocalDateTime *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalTimeComponentSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localTimeComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalTimeComponentSerializer *shared __attribute__((swift_name("shared")));
- (LocalTime *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(LocalTime *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalTimeIso8601Serializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localTimeIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalTimeIso8601Serializer *shared __attribute__((swift_name("shared")));
- (LocalTime *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(LocalTime *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface MonthBasedDateTimeUnitSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)monthBasedDateTimeUnitSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) MonthBasedDateTimeUnitSerializer *shared __attribute__((swift_name("shared")));
- (DateTimeUnitMonthBased *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(DateTimeUnitMonthBased *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface MonthSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)monthSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) MonthSerializer *shared __attribute__((swift_name("shared")));
- (Month *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(Month *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface TimeBasedDateTimeUnitSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)timeBasedDateTimeUnitSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) TimeBasedDateTimeUnitSerializer *shared __attribute__((swift_name("shared")));
- (DateTimeUnitTimeBased *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(DateTimeUnitTimeBased *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface TimeZoneSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)timeZoneSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) TimeZoneSerializer *shared __attribute__((swift_name("shared")));
- (TimeZone *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(TimeZone *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface UtcOffsetSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)utcOffsetSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) UtcOffsetSerializer *shared __attribute__((swift_name("shared")));
- (UtcOffset *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(UtcOffset *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

@protocol SerialFormat
@required
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

@protocol BinaryFormat <SerialFormat>
@required
- (id _Nullable)decodeFromByteArrayDeserializer:(id<DeserializationStrategy>)deserializer bytes:(KotlinByteArray *)bytes __attribute__((swift_name("decodeFromByteArray(deserializer:bytes:)")));
- (KotlinByteArray *)encodeToByteArraySerializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeToByteArray(serializer:value:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((objc_subclassing_restricted))
@interface ContextualSerializer<T> : Base <KSerializer>
- (instancetype)initWithSerializableClass:(id<KotlinKClass>)serializableClass __attribute__((swift_name("init(serializableClass:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithSerializableClass:(id<KotlinKClass>)serializableClass fallbackSerializer:(id<KSerializer> _Nullable)fallbackSerializer typeArgumentsSerializers:(KotlinArray<id<KSerializer>> *)typeArgumentsSerializers __attribute__((swift_name("init(serializableClass:fallbackSerializer:typeArgumentsSerializers:)"))) __attribute__((objc_designated_initializer));
- (T)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(T)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

@interface SerializationException : KotlinIllegalArgumentException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((objc_subclassing_restricted))
@interface MissingFieldException : SerializationException
- (instancetype)initWithMissingField:(NSString *)missingField serialName:(NSString *)serialName __attribute__((swift_name("init(missingField:serialName:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMissingFields:(NSArray<NSString *> *)missingFields serialName:(NSString *)serialName __attribute__((swift_name("init(missingFields:serialName:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMissingFields:(NSArray<NSString *> *)missingFields message:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(missingFields:message:cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (readonly) NSArray<NSString *> *missingFields __attribute__((swift_name("missingFields")));
@end

__attribute__((objc_subclassing_restricted))
@interface PolymorphicSerializer<T> : AbstractPolymorphicSerializer<T>
- (instancetype)initWithBaseClass:(id<KotlinKClass>)baseClass __attribute__((swift_name("init(baseClass:)"))) __attribute__((objc_designated_initializer));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<KotlinKClass> baseClass __attribute__((swift_name("baseClass")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
__attribute__((objc_subclassing_restricted))
@interface SealedClassSerializer<T> : AbstractPolymorphicSerializer<T>
- (instancetype)initWithSerialName:(NSString *)serialName baseClass:(id<KotlinKClass>)baseClass subclasses:(KotlinArray<id<KotlinKClass>> *)subclasses subclassSerializers:(KotlinArray<id<KSerializer>> *)subclassSerializers __attribute__((swift_name("init(serialName:baseClass:subclasses:subclassSerializers:)"))) __attribute__((objc_designated_initializer));
- (id<DeserializationStrategy> _Nullable)findPolymorphicSerializerOrNullDecoder:(id<CompositeDecoder>)decoder klassName:(NSString * _Nullable)klassName __attribute__((swift_name("findPolymorphicSerializerOrNull(decoder:klassName:)")));
- (id<SerializationStrategy> _Nullable)findPolymorphicSerializerOrNullEncoder:(id<Encoder>)encoder value:(T)value __attribute__((swift_name("findPolymorphicSerializerOrNull(encoder:value:)")));
@property (readonly) id<KotlinKClass> baseClass __attribute__((swift_name("baseClass")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

@protocol StringFormat <SerialFormat>
@required
- (id _Nullable)decodeFromStringDeserializer:(id<DeserializationStrategy>)deserializer string:(NSString *)string __attribute__((swift_name("decodeFromString(deserializer:string:)")));
- (NSString *)encodeToStringSerializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeToString(serializer:value:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface LongAsStringSerializer : Base <KSerializer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)longAsStringSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LongAsStringSerializer *shared __attribute__((swift_name("shared")));
- (Long *)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(Long *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface ClassSerialDescriptorBuilder : Base
- (void)elementElementName:(NSString *)elementName descriptor:(id<SerialDescriptor>)descriptor annotations:(NSArray<id<KotlinAnnotation>> *)annotations isOptional:(BOOL)isOptional __attribute__((swift_name("element(elementName:descriptor:annotations:isOptional:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property NSArray<id<KotlinAnnotation>> *annotations __attribute__((swift_name("annotations")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property BOOL isNullable __attribute__((swift_name("isNullable"))) __attribute__((unavailable("isNullable inside buildSerialDescriptor is deprecated. Please use SerialDescriptor.nullable extension on a builder result.")));
@property (readonly) NSString *serialName __attribute__((swift_name("serialName")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@interface SerialKind : Base
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@interface PolymorphicKind : SerialKind
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PolymorphicKind.OPEN")))
@interface PolymorphicKindOPEN : PolymorphicKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)oPEN __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PolymorphicKindOPEN *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PolymorphicKind.SEALED")))
@interface PolymorphicKindSEALED : PolymorphicKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sEALED __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PolymorphicKindSEALED *shared __attribute__((swift_name("shared")));
@end

@interface PrimitiveKind : SerialKind
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.BOOLEAN")))
@interface PrimitiveKindBOOLEAN : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)bOOLEAN __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindBOOLEAN *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.BYTE")))
@interface PrimitiveKindBYTE : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)bYTE __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindBYTE *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.CHAR")))
@interface PrimitiveKindCHAR : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cHAR __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindCHAR *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.DOUBLE")))
@interface PrimitiveKindDOUBLE : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dOUBLE __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindDOUBLE *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.FLOAT")))
@interface PrimitiveKindFLOAT : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)fLOAT __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindFLOAT *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.INT")))
@interface PrimitiveKindINT : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)iNT __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindINT *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.LONG")))
@interface PrimitiveKindLONG : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lONG __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindLONG *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.SHORT")))
@interface PrimitiveKindSHORT : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sHORT __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindSHORT *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PrimitiveKind.STRING")))
@interface PrimitiveKindSTRING : PrimitiveKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sTRING __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PrimitiveKindSTRING *shared __attribute__((swift_name("shared")));
@end

@protocol SerialDescriptor
@required

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (NSArray<id<KotlinAnnotation>> *)getElementAnnotationsIndex:(int32_t)index __attribute__((swift_name("getElementAnnotations(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SerialDescriptor>)getElementDescriptorIndex:(int32_t)index __attribute__((swift_name("getElementDescriptor(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (int32_t)getElementIndexName:(NSString *)name __attribute__((swift_name("getElementIndex(name:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (NSString *)getElementNameIndex:(int32_t)index __attribute__((swift_name("getElementName(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)isElementOptionalIndex:(int32_t)index __attribute__((swift_name("isElementOptional(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) NSArray<id<KotlinAnnotation>> *annotations __attribute__((swift_name("annotations")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) int32_t elementsCount __attribute__((swift_name("elementsCount")));
@property (readonly) BOOL isInline __attribute__((swift_name("isInline")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) BOOL isNullable __attribute__((swift_name("isNullable")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) SerialKind *kind __attribute__((swift_name("kind")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) NSString *serialName __attribute__((swift_name("serialName")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SerialKind.CONTEXTUAL")))
@interface SerialKindCONTEXTUAL : SerialKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cONTEXTUAL __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SerialKindCONTEXTUAL *shared __attribute__((swift_name("shared")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SerialKind.ENUM")))
@interface SerialKindENUM : SerialKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)eNUM __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SerialKindENUM *shared __attribute__((swift_name("shared")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@interface StructureKind : SerialKind
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StructureKind.CLASS")))
@interface StructureKindCLASS : StructureKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cLASS __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) StructureKindCLASS *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StructureKind.LIST")))
@interface StructureKindLIST : StructureKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lIST __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) StructureKindLIST *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StructureKind.MAP")))
@interface StructureKindMAP : StructureKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)mAP __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) StructureKindMAP *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StructureKind.OBJECT")))
@interface StructureKindOBJECT : StructureKind
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)oBJECT __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) StructureKindOBJECT *shared __attribute__((swift_name("shared")));
@end

@protocol Decoder
@required
- (id<CompositeDecoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (int32_t)decodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (id<Decoder>)decodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (KotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableValueDeserializer:(id<DeserializationStrategy>)deserializer __attribute__((swift_name("decodeNullableSerializableValue(deserializer:)")));
- (id _Nullable)decodeSerializableValueDeserializer:(id<DeserializationStrategy>)deserializer __attribute__((swift_name("decodeSerializableValue(deserializer:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

@protocol CompositeDecoder
@required
- (BOOL)decodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (int32_t)decodeCollectionSizeDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("decodeCollectionSize(descriptor:)")));
- (double)decodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeElementIndexDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("decodeElementIndex(descriptor:)")));
- (float)decodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<Decoder>)decodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeSequentially __attribute__((swift_name("decodeSequentially()")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (int16_t)decodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@interface AbstractDecoder : Base <Decoder, CompositeDecoder>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<CompositeDecoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (BOOL)decodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (int8_t)decodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (unichar)decodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (double)decodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (float)decodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<Decoder>)decodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (id<Decoder>)decodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int32_t)decodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));
- (int64_t)decodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));
- (KotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (id _Nullable)decodeSerializableValueDeserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableValue(deserializer:previousValue:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (int16_t)decodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
- (NSString *)decodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));
- (id)decodeValue __attribute__((swift_name("decodeValue()")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@end

@protocol Encoder
@required
- (id<CompositeEncoder>)beginCollectionDescriptor:(id<SerialDescriptor>)descriptor collectionSize:(int32_t)collectionSize __attribute__((swift_name("beginCollection(descriptor:collectionSize:)")));
- (id<CompositeEncoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (id<Encoder>)encodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNotNullMark __attribute__((swift_name("encodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNull __attribute__((swift_name("encodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableValueSerializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableValue(serializer:value:)")));
- (void)encodeSerializableValueSerializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableValue(serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

@protocol CompositeEncoder
@required
- (void)encodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (void)encodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<Encoder>)encodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)shouldEncodeElementDefaultDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("shouldEncodeElementDefault(descriptor:index:)")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@interface AbstractEncoder : Base <Encoder, CompositeEncoder>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<CompositeEncoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (BOOL)encodeElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeElement(descriptor:index:)")));
- (void)encodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (void)encodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<Encoder>)encodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (id<Encoder>)encodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));
- (void)encodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));
- (void)encodeNull __attribute__((swift_name("encodeNull()")));
- (void)encodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
- (void)encodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));
- (void)encodeValueValue:(id)value __attribute__((swift_name("encodeValue(value:)")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@protocol ChunkedDecoder
@required

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)decodeStringChunkedConsumeChunk:(void (^)(NSString *))consumeChunk __attribute__((swift_name("decodeStringChunked(consumeChunk:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CompositeDecoderCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) CompositeDecoderCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t DECODE_DONE __attribute__((swift_name("DECODE_DONE")));
@property (readonly) int32_t UNKNOWN_NAME __attribute__((swift_name("UNKNOWN_NAME")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface AbstractCollectionSerializer<Element, Collection, Builder> : Base <KSerializer>

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Builder _Nullable)builder __attribute__((swift_name("builder()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int32_t)builderSize:(Builder _Nullable)receiver __attribute__((swift_name("builderSize(_:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)checkCapacity:(Builder _Nullable)receiver size:(int32_t)size __attribute__((swift_name("checkCapacity(_:size:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id<KotlinIterator>)collectionIterator:(Collection _Nullable)receiver __attribute__((swift_name("collectionIterator(_:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int32_t)collectionSize:(Collection _Nullable)receiver __attribute__((swift_name("collectionSize(_:)")));
- (Collection _Nullable)deserializeDecoder:(id<Decoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (Collection _Nullable)mergeDecoder:(id<Decoder>)decoder previous:(Collection _Nullable)previous __attribute__((swift_name("merge(decoder:previous:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)readAllDecoder:(id<CompositeDecoder>)decoder builder:(Builder _Nullable)builder startIndex:(int32_t)startIndex size:(int32_t)size __attribute__((swift_name("readAll(decoder:builder:startIndex:size:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)readElementDecoder:(id<CompositeDecoder>)decoder index:(int32_t)index builder:(Builder _Nullable)builder checkIndex:(BOOL)checkIndex __attribute__((swift_name("readElement(decoder:index:builder:checkIndex:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(Collection _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Builder _Nullable)toBuilder:(Collection _Nullable)receiver __attribute__((swift_name("toBuilder(_:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Collection _Nullable)toResult:(Builder _Nullable)receiver __attribute__((swift_name("toResult(_:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ElementMarker : Base
- (instancetype)initWithDescriptor:(id<SerialDescriptor>)descriptor readIfAbsent:(Boolean *(^)(id<SerialDescriptor>, Int *))readIfAbsent __attribute__((swift_name("init(descriptor:readIfAbsent:)"))) __attribute__((objc_designated_initializer));
- (void)markIndex:(int32_t)index __attribute__((swift_name("mark(index:)")));
- (int32_t)nextUnmarkedIndex __attribute__((swift_name("nextUnmarkedIndex()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@protocol GeneratedSerializer <KSerializer>
@required
- (KotlinArray<id<KSerializer>> *)childSerializers __attribute__((swift_name("childSerializers()")));
- (KotlinArray<id<KSerializer>> *)typeParametersSerializers __attribute__((swift_name("typeParametersSerializers()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface MapLikeSerializer<Key, Value, Collection, Builder> : AbstractCollectionSerializer<id<KotlinMapEntry>, Collection, MutableDictionary<id, id> *>

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)insertKeyValuePair:(MutableDictionary<id, id> *)receiver index:(int32_t)index key:(Key _Nullable)key value:(Value _Nullable)value __attribute__((swift_name("insertKeyValuePair(_:index:key:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)readAllDecoder:(id<CompositeDecoder>)decoder builder:(MutableDictionary<id, id> *)builder startIndex:(int32_t)startIndex size:(int32_t)size __attribute__((swift_name("readAll(decoder:builder:startIndex:size:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)readElementDecoder:(id<CompositeDecoder>)decoder index:(int32_t)index builder:(MutableDictionary<id, id> *)builder checkIndex:(BOOL)checkIndex __attribute__((swift_name("readElement(decoder:index:builder:checkIndex:)")));
- (void)serializeEncoder:(id<Encoder>)encoder value:(Collection _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@property (readonly) id<KSerializer> keySerializer __attribute__((swift_name("keySerializer")));
@property (readonly) id<KSerializer> valueSerializer __attribute__((swift_name("valueSerializer")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface TaggedDecoder<Tag> : Base <Decoder, CompositeDecoder>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<CompositeDecoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)doCopyTagsToOther:(TaggedDecoder<Tag> *)other __attribute__((swift_name("doCopyTagsTo(other:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (BOOL)decodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (int8_t)decodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (unichar)decodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (double)decodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (float)decodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<Decoder>)decodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (id<Decoder>)decodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int32_t)decodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));
- (int64_t)decodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));
- (KotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id _Nullable)decodeSerializableValueDeserializer:(id<DeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableValue(deserializer:previousValue:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (int16_t)decodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
- (NSString *)decodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (BOOL)decodeTaggedBooleanTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedBoolean(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int8_t)decodeTaggedByteTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedByte(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (unichar)decodeTaggedCharTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedChar(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (double)decodeTaggedDoubleTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedDouble(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int32_t)decodeTaggedEnumTag:(Tag _Nullable)tag enumDescriptor:(id<SerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeTaggedEnum(tag:enumDescriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (float)decodeTaggedFloatTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedFloat(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id<Decoder>)decodeTaggedInlineTag:(Tag _Nullable)tag inlineDescriptor:(id<SerialDescriptor>)inlineDescriptor __attribute__((swift_name("decodeTaggedInline(tag:inlineDescriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int32_t)decodeTaggedIntTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedInt(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int64_t)decodeTaggedLongTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedLong(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (BOOL)decodeTaggedNotNullMarkTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedNotNullMark(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (KotlinNothing * _Nullable)decodeTaggedNullTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedNull(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (int16_t)decodeTaggedShortTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedShort(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)decodeTaggedStringTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedString(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id)decodeTaggedValueTag:(Tag _Nullable)tag __attribute__((swift_name("decodeTaggedValue(tag:)")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Tag _Nullable)getTag:(id<SerialDescriptor>)receiver index:(int32_t)index __attribute__((swift_name("getTag(_:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Tag _Nullable)popTag __attribute__((swift_name("popTag()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)pushTagName:(Tag _Nullable)name __attribute__((swift_name("pushTag(name:)")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) Tag _Nullable currentTag __attribute__((swift_name("currentTag")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) Tag _Nullable currentTagOrNull __attribute__((swift_name("currentTagOrNull")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface NamedValueDecoder : TaggedDecoder<NSString *>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)composeNameParentName:(NSString *)parentName childName:(NSString *)childName __attribute__((swift_name("composeName(parentName:childName:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)elementNameDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("elementName(descriptor:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)getTag:(id<SerialDescriptor>)receiver index:(int32_t)index __attribute__((swift_name("getTag(_:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)nestedNestedName:(NSString *)nestedName __attribute__((swift_name("nested(nestedName:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface TaggedEncoder<Tag> : Base <Encoder, CompositeEncoder>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<CompositeEncoder>)beginStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeBooleanElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeByteElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeCharElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeDoubleElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (void)encodeEnumEnumDescriptor:(id<SerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (void)encodeFloatElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<Encoder>)encodeInlineDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (id<Encoder>)encodeInlineElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeIntElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));
- (void)encodeLongElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));
- (void)encodeNotNullMark __attribute__((swift_name("encodeNotNullMark()")));
- (void)encodeNull __attribute__((swift_name("encodeNull()")));
- (void)encodeNullableSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeShortElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
- (void)encodeStringElementDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedBooleanTag:(Tag _Nullable)tag value:(BOOL)value __attribute__((swift_name("encodeTaggedBoolean(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedByteTag:(Tag _Nullable)tag value:(int8_t)value __attribute__((swift_name("encodeTaggedByte(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedCharTag:(Tag _Nullable)tag value:(unichar)value __attribute__((swift_name("encodeTaggedChar(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedDoubleTag:(Tag _Nullable)tag value:(double)value __attribute__((swift_name("encodeTaggedDouble(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedEnumTag:(Tag _Nullable)tag enumDescriptor:(id<SerialDescriptor>)enumDescriptor ordinal:(int32_t)ordinal __attribute__((swift_name("encodeTaggedEnum(tag:enumDescriptor:ordinal:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedFloatTag:(Tag _Nullable)tag value:(float)value __attribute__((swift_name("encodeTaggedFloat(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id<Encoder>)encodeTaggedInlineTag:(Tag _Nullable)tag inlineDescriptor:(id<SerialDescriptor>)inlineDescriptor __attribute__((swift_name("encodeTaggedInline(tag:inlineDescriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedIntTag:(Tag _Nullable)tag value:(int32_t)value __attribute__((swift_name("encodeTaggedInt(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedLongTag:(Tag _Nullable)tag value:(int64_t)value __attribute__((swift_name("encodeTaggedLong(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedNonNullMarkTag:(Tag _Nullable)tag __attribute__((swift_name("encodeTaggedNonNullMark(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedNullTag:(Tag _Nullable)tag __attribute__((swift_name("encodeTaggedNull(tag:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedShortTag:(Tag _Nullable)tag value:(int16_t)value __attribute__((swift_name("encodeTaggedShort(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedStringTag:(Tag _Nullable)tag value:(NSString *)value __attribute__((swift_name("encodeTaggedString(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)encodeTaggedValueTag:(Tag _Nullable)tag value:(id)value __attribute__((swift_name("encodeTaggedValue(tag:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)endEncodeDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endEncode(descriptor:)")));
- (void)endStructureDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Tag _Nullable)getTag:(id<SerialDescriptor>)receiver index:(int32_t)index __attribute__((swift_name("getTag(_:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (Tag _Nullable)popTag __attribute__((swift_name("popTag()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)pushTagName:(Tag _Nullable)name __attribute__((swift_name("pushTag(name:)")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) Tag _Nullable currentTag __attribute__((swift_name("currentTag")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) Tag _Nullable currentTagOrNull __attribute__((swift_name("currentTagOrNull")));
@property (readonly) SerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
@interface NamedValueEncoder : TaggedEncoder<NSString *>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)composeNameParentName:(NSString *)parentName childName:(NSString *)childName __attribute__((swift_name("composeName(parentName:childName:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)elementNameDescriptor:(id<SerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("elementName(descriptor:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)getTag:(id<SerialDescriptor>)receiver index:(int32_t)index __attribute__((swift_name("getTag(_:index:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)nestedNestedName:(NSString *)nestedName __attribute__((swift_name("nested(nestedName:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface PolymorphicModuleBuilder<__contravariant Base_> : Base
- (void)defaultDefaultSerializerProvider:(id<DeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultSerializerProvider __attribute__((swift_name("default(defaultSerializerProvider:)"))) __attribute__((deprecated("Deprecated in favor of function with more precise name: defaultDeserializer")));
- (void)defaultDeserializerDefaultDeserializerProvider:(id<DeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("defaultDeserializer(defaultDeserializerProvider:)")));
- (void)subclassSubclass:(id<KotlinKClass>)subclass serializer:(id<KSerializer>)serializer __attribute__((swift_name("subclass(subclass:serializer:)")));
@end

@interface SerializersModule : Base

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)dumpToCollector:(id<SerializersModuleCollector>)collector __attribute__((swift_name("dumpTo(collector:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<KSerializer> _Nullable)getContextualKClass:(id<KotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<KSerializer>> *)typeArgumentsSerializers __attribute__((swift_name("getContextual(kClass:typeArgumentsSerializers:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SerializationStrategy> _Nullable)getPolymorphicBaseClass:(id<KotlinKClass>)baseClass value:(id)value __attribute__((swift_name("getPolymorphic(baseClass:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<DeserializationStrategy> _Nullable)getPolymorphicBaseClass:(id<KotlinKClass>)baseClass serializedClassName:(NSString * _Nullable)serializedClassName __attribute__((swift_name("getPolymorphic(baseClass:serializedClassName:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@protocol SerializersModuleCollector
@required
- (void)contextualKClass:(id<KotlinKClass>)kClass provider:(id<KSerializer> (^)(NSArray<id<KSerializer>> *))provider __attribute__((swift_name("contextual(kClass:provider:)")));
- (void)contextualKClass:(id<KotlinKClass>)kClass serializer:(id<KSerializer>)serializer __attribute__((swift_name("contextual(kClass:serializer:)")));
- (void)polymorphicBaseClass:(id<KotlinKClass>)baseClass actualClass:(id<KotlinKClass>)actualClass actualSerializer:(id<KSerializer>)actualSerializer __attribute__((swift_name("polymorphic(baseClass:actualClass:actualSerializer:)")));
- (void)polymorphicDefaultBaseClass:(id<KotlinKClass>)baseClass defaultDeserializerProvider:(id<DeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefault(baseClass:defaultDeserializerProvider:)"))) __attribute__((deprecated("Deprecated in favor of function with more precise name: polymorphicDefaultDeserializer")));
- (void)polymorphicDefaultDeserializerBaseClass:(id<KotlinKClass>)baseClass defaultDeserializerProvider:(id<DeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefaultDeserializer(baseClass:defaultDeserializerProvider:)")));
- (void)polymorphicDefaultSerializerBaseClass:(id<KotlinKClass>)baseClass defaultSerializerProvider:(id<SerializationStrategy> _Nullable (^)(id))defaultSerializerProvider __attribute__((swift_name("polymorphicDefaultSerializer(baseClass:defaultSerializerProvider:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerializersModuleBuilder : Base <SerializersModuleCollector>
- (void)contextualKClass:(id<KotlinKClass>)kClass provider:(id<KSerializer> (^)(NSArray<id<KSerializer>> *))provider __attribute__((swift_name("contextual(kClass:provider:)")));
- (void)contextualKClass:(id<KotlinKClass>)kClass serializer:(id<KSerializer>)serializer __attribute__((swift_name("contextual(kClass:serializer:)")));
- (void)includeModule:(SerializersModule *)module __attribute__((swift_name("include(module:)")));
- (void)polymorphicBaseClass:(id<KotlinKClass>)baseClass actualClass:(id<KotlinKClass>)actualClass actualSerializer:(id<KSerializer>)actualSerializer __attribute__((swift_name("polymorphic(baseClass:actualClass:actualSerializer:)")));
- (void)polymorphicDefaultDeserializerBaseClass:(id<KotlinKClass>)baseClass defaultDeserializerProvider:(id<DeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefaultDeserializer(baseClass:defaultDeserializerProvider:)")));
- (void)polymorphicDefaultSerializerBaseClass:(id<KotlinKClass>)baseClass defaultSerializerProvider:(id<SerializationStrategy> _Nullable (^)(id))defaultSerializerProvider __attribute__((swift_name("polymorphicDefaultSerializer(baseClass:defaultSerializerProvider:)")));
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


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinBoolean.Companion")))
@interface KotlinBooleanCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinBooleanCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinByte.Companion")))
@interface KotlinByteCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinByteCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int8_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) int8_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinChar.Companion")))
@interface KotlinCharCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinCharCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
@property (readonly) int32_t MAX_CODE_POINT __attribute__((swift_name("MAX_CODE_POINT")));
@property (readonly) unichar MAX_HIGH_SURROGATE __attribute__((swift_name("MAX_HIGH_SURROGATE")));
@property (readonly) unichar MAX_LOW_SURROGATE __attribute__((swift_name("MAX_LOW_SURROGATE")));

/**
 * @note annotations
 *   kotlin.DeprecatedSinceKotlin(warningSince="1.9", errorSince="2.1")
*/
@property (readonly) int32_t MAX_RADIX __attribute__((swift_name("MAX_RADIX"))) __attribute__((unavailable("Introduce your own constant with the value of `36")));
@property (readonly) unichar MAX_SURROGATE __attribute__((swift_name("MAX_SURROGATE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) unichar MAX_VALUE __attribute__((swift_name("MAX_VALUE")));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
@property (readonly) int32_t MIN_CODE_POINT __attribute__((swift_name("MIN_CODE_POINT")));
@property (readonly) unichar MIN_HIGH_SURROGATE __attribute__((swift_name("MIN_HIGH_SURROGATE")));
@property (readonly) unichar MIN_LOW_SURROGATE __attribute__((swift_name("MIN_LOW_SURROGATE")));

/**
 * @note annotations
 *   kotlin.DeprecatedSinceKotlin(warningSince="1.9", errorSince="2.1")
*/
@property (readonly) int32_t MIN_RADIX __attribute__((swift_name("MIN_RADIX"))) __attribute__((unavailable("Introduce your own constant with the value of `2`")));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
@property (readonly) int32_t MIN_SUPPLEMENTARY_CODE_POINT __attribute__((swift_name("MIN_SUPPLEMENTARY_CODE_POINT")));
@property (readonly) unichar MIN_SURROGATE __attribute__((swift_name("MIN_SURROGATE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) unichar MIN_VALUE __attribute__((swift_name("MIN_VALUE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinDouble.Companion")))
@interface KotlinDoubleCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinDoubleCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) double MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) double MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) double NEGATIVE_INFINITY __attribute__((swift_name("NEGATIVE_INFINITY")));
@property (readonly) double NaN __attribute__((swift_name("NaN")));
@property (readonly) double POSITIVE_INFINITY __attribute__((swift_name("POSITIVE_INFINITY")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinFloat.Companion")))
@interface KotlinFloatCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinFloatCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) float MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) float MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) float NEGATIVE_INFINITY __attribute__((swift_name("NEGATIVE_INFINITY")));
@property (readonly) float NaN __attribute__((swift_name("NaN")));
@property (readonly) float POSITIVE_INFINITY __attribute__((swift_name("POSITIVE_INFINITY")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinInt.Companion")))
@interface KotlinIntCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinIntCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) int32_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
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

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinLong.Companion")))
@interface KotlinLongCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinLongCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int64_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) int64_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
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

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinShort.Companion")))
@interface KotlinShortCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinShortCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int16_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) int16_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinString.Companion")))
@interface KotlinStringCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinStringCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinUByte.Companion")))
@interface KotlinUByteCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinUByteCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) uint8_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) uint8_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinUInt.Companion")))
@interface KotlinUIntCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinUIntCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) uint32_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) uint32_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinULong.Companion")))
@interface KotlinULongCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinULongCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) uint64_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) uint64_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinUShort.Companion")))
@interface KotlinUShortCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinUShortCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) uint16_t MAX_VALUE __attribute__((swift_name("MAX_VALUE")));
@property (readonly) uint16_t MIN_VALUE __attribute__((swift_name("MIN_VALUE")));
@property (readonly) int32_t SIZE_BITS __attribute__((swift_name("SIZE_BITS")));
@property (readonly) int32_t SIZE_BYTES __attribute__((swift_name("SIZE_BYTES")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinUnit : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unit __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinUnit *shared __attribute__((swift_name("shared")));
- (NSString *)description __attribute__((swift_name("description()")));
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
@property (readonly) Int *endExclusive __attribute__((swift_name("endExclusive"))) __attribute__((deprecated("Can throw an exception when it's impossible to represent the value with Int type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")));
@property (readonly) Int *endInclusive __attribute__((swift_name("endInclusive")));
@property (readonly, getter=start_) Int *start __attribute__((swift_name("start")));
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
@property (readonly) Long *endExclusive __attribute__((swift_name("endExclusive"))) __attribute__((deprecated("Can throw an exception when it's impossible to represent the value with Long type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")));
@property (readonly) Long *endInclusive __attribute__((swift_name("endInclusive")));
@property (readonly, getter=start_) Long *start __attribute__((swift_name("start")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinDuration.Companion")))
@interface KotlinDurationCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinDurationCompanion *shared __attribute__((swift_name("shared")));
- (int64_t)days:(double)receiver __attribute__((swift_name("days(_:)")));
- (int64_t)days_:(int32_t)receiver __attribute__((swift_name("days(__:)")));
- (int64_t)days__:(int64_t)receiver __attribute__((swift_name("days(___:)")));
- (int64_t)hours:(double)receiver __attribute__((swift_name("hours(_:)")));
- (int64_t)hours_:(int32_t)receiver __attribute__((swift_name("hours(__:)")));
- (int64_t)hours__:(int64_t)receiver __attribute__((swift_name("hours(___:)")));
- (int64_t)microseconds:(double)receiver __attribute__((swift_name("microseconds(_:)")));
- (int64_t)microseconds_:(int32_t)receiver __attribute__((swift_name("microseconds(__:)")));
- (int64_t)microseconds__:(int64_t)receiver __attribute__((swift_name("microseconds(___:)")));
- (int64_t)milliseconds:(double)receiver __attribute__((swift_name("milliseconds(_:)")));
- (int64_t)milliseconds_:(int32_t)receiver __attribute__((swift_name("milliseconds(__:)")));
- (int64_t)milliseconds__:(int64_t)receiver __attribute__((swift_name("milliseconds(___:)")));
- (int64_t)minutes:(double)receiver __attribute__((swift_name("minutes(_:)")));
- (int64_t)minutes_:(int32_t)receiver __attribute__((swift_name("minutes(__:)")));
- (int64_t)minutes__:(int64_t)receiver __attribute__((swift_name("minutes(___:)")));
- (int64_t)nanoseconds:(double)receiver __attribute__((swift_name("nanoseconds(_:)")));
- (int64_t)nanoseconds_:(int32_t)receiver __attribute__((swift_name("nanoseconds(__:)")));
- (int64_t)nanoseconds__:(int64_t)receiver __attribute__((swift_name("nanoseconds(___:)")));
- (int64_t)seconds:(double)receiver __attribute__((swift_name("seconds(_:)")));
- (int64_t)seconds_:(int32_t)receiver __attribute__((swift_name("seconds(__:)")));
- (int64_t)seconds__:(int64_t)receiver __attribute__((swift_name("seconds(___:)")));

/**
 * @note annotations
 *   kotlin.time.ExperimentalTime
*/
- (double)convertValue:(double)value sourceUnit:(KotlinDurationUnit *)sourceUnit targetUnit:(KotlinDurationUnit *)targetUnit __attribute__((swift_name("convert(value:sourceUnit:targetUnit:)")));
- (int64_t)parseValue:(NSString *)value __attribute__((swift_name("parse(value:)")));
- (int64_t)parseIsoStringValue:(NSString *)value __attribute__((swift_name("parseIsoString(value:)")));
- (id _Nullable)parseIsoStringOrNullValue:(NSString *)value __attribute__((swift_name("parseIsoStringOrNull(value:)")));
- (id _Nullable)parseOrNullValue:(NSString *)value __attribute__((swift_name("parseOrNull(value:)")));
@property (readonly) int64_t INFINITE __attribute__((swift_name("INFINITE")));
@property (readonly) int64_t ZERO __attribute__((swift_name("ZERO")));
@end

__attribute__((objc_subclassing_restricted))
@interface NSDate : Base
@end

__attribute__((objc_subclassing_restricted))
@interface NSTimeZone : Base
@end

__attribute__((objc_subclassing_restricted))
@interface AtomicFU_commonKt : Base
+ (AtomicArray<id> *)atomicArrayOfNullsSize:(int32_t)size __attribute__((swift_name("atomicArrayOfNulls(size:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface AtomicFUKt : Base
+ (AtomicRef<id> *)atomicInitial:(id _Nullable)initial __attribute__((swift_name("atomic(initial:)")));
+ (AtomicBoolean *)atomicInitial_:(BOOL)initial __attribute__((swift_name("atomic(initial_:)")));
+ (AtomicInt *)atomicInitial__:(int32_t)initial __attribute__((swift_name("atomic(initial__:)")));
+ (AtomicLong *)atomicInitial___:(int64_t)initial __attribute__((swift_name("atomic(initial___:)")));
+ (AtomicRef<id> *)atomicInitial:(id _Nullable)initial trace:(TraceBase *)trace __attribute__((swift_name("atomic(initial:trace:)")));
+ (AtomicBoolean *)atomicInitial:(BOOL)initial trace_:(TraceBase *)trace __attribute__((swift_name("atomic(initial:trace_:)")));
+ (AtomicInt *)atomicInitial:(int32_t)initial trace__:(TraceBase *)trace __attribute__((swift_name("atomic(initial:trace__:)")));
+ (AtomicLong *)atomicInitial:(int64_t)initial trace___:(TraceBase *)trace __attribute__((swift_name("atomic(initial:trace___:)")));
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
@interface AwaitKt_ : Base

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
+ (id<BroadcastChannel>)broadcast:(id<ReceiveChannel>)receiver capacity:(int32_t)capacity start:(CoroutineStart *)start __attribute__((swift_name("broadcast(_:capacity:start:)"))) __attribute__((deprecated("BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));

/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id<BroadcastChannel>)broadcast:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context capacity:(int32_t)capacity start:(CoroutineStart *)start onCompletion:(void (^ _Nullable)(KotlinThrowable * _Nullable))onCompletion block:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("broadcast(_:context:capacity:start:onCompletion:block:)"))) __attribute__((deprecated("BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));
@end

__attribute__((objc_subclassing_restricted))
@interface BroadcastKt_ : Base

/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id<BroadcastChannel>)broadcast:(id<ReceiveChannel>)receiver capacity:(int32_t)capacity start:(CoroutineStart *)start __attribute__((swift_name("broadcast(_:capacity:start:)"))) __attribute__((deprecated("BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));

/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id<BroadcastChannel>)broadcast:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context capacity:(int32_t)capacity start:(CoroutineStart *)start onCompletion:(void (^ _Nullable)(KotlinThrowable * _Nullable))onCompletion block:(id<KotlinSuspendFunction1>)block __attribute__((swift_name("broadcast(_:context:capacity:start:onCompletion:block:)"))) __attribute__((deprecated("BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));
@end

__attribute__((objc_subclassing_restricted))
@interface BroadcastChannelKt : Base

/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id<BroadcastChannel>)BroadcastChannelCapacity:(int32_t)capacity __attribute__((swift_name("BroadcastChannel(capacity:)"))) __attribute__((deprecated("BroadcastChannel is deprecated in the favour of SharedFlow and StateFlow, and is no longer supported")));
@end

__attribute__((objc_subclassing_restricted))
@interface BroadcastChannelKt_ : Base

/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id<BroadcastChannel>)BroadcastChannelCapacity:(int32_t)capacity __attribute__((swift_name("BroadcastChannel(capacity:)"))) __attribute__((deprecated("BroadcastChannel is deprecated in the favour of SharedFlow and StateFlow, and is no longer supported")));
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
@interface Builders_commonKt_ : Base
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
@interface BuildersKt_ : Base
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
@interface BuiltinSerializersKt : Base
+ (id<KSerializer>)nullable:(id<KSerializer>)receiver __attribute__((swift_name("nullable(_:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<KSerializer>)ArraySerializerElementSerializer:(id<KSerializer>)elementSerializer __attribute__((swift_name("ArraySerializer(elementSerializer:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<KSerializer>)ArraySerializerKClass:(id<KotlinKClass>)kClass elementSerializer:(id<KSerializer>)elementSerializer __attribute__((swift_name("ArraySerializer(kClass:elementSerializer:)")));
+ (id<KSerializer>)BooleanArraySerializer __attribute__((swift_name("BooleanArraySerializer()")));
+ (id<KSerializer>)ByteArraySerializer __attribute__((swift_name("ByteArraySerializer()")));
+ (id<KSerializer>)CharArraySerializer __attribute__((swift_name("CharArraySerializer()")));
+ (id<KSerializer>)DoubleArraySerializer __attribute__((swift_name("DoubleArraySerializer()")));
+ (id<KSerializer>)FloatArraySerializer __attribute__((swift_name("FloatArraySerializer()")));
+ (id<KSerializer>)IntArraySerializer __attribute__((swift_name("IntArraySerializer()")));
+ (id<KSerializer>)ListSerializerElementSerializer:(id<KSerializer>)elementSerializer __attribute__((swift_name("ListSerializer(elementSerializer:)")));
+ (id<KSerializer>)LongArraySerializer __attribute__((swift_name("LongArraySerializer()")));
+ (id<KSerializer>)MapEntrySerializerKeySerializer:(id<KSerializer>)keySerializer valueSerializer:(id<KSerializer>)valueSerializer __attribute__((swift_name("MapEntrySerializer(keySerializer:valueSerializer:)")));
+ (id<KSerializer>)MapSerializerKeySerializer:(id<KSerializer>)keySerializer valueSerializer:(id<KSerializer>)valueSerializer __attribute__((swift_name("MapSerializer(keySerializer:valueSerializer:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<KSerializer>)NothingSerializer __attribute__((swift_name("NothingSerializer()")));
+ (id<KSerializer>)PairSerializerKeySerializer:(id<KSerializer>)keySerializer valueSerializer:(id<KSerializer>)valueSerializer __attribute__((swift_name("PairSerializer(keySerializer:valueSerializer:)")));
+ (id<KSerializer>)SetSerializerElementSerializer:(id<KSerializer>)elementSerializer __attribute__((swift_name("SetSerializer(elementSerializer:)")));
+ (id<KSerializer>)ShortArraySerializer __attribute__((swift_name("ShortArraySerializer()")));
+ (id<KSerializer>)TripleSerializerASerializer:(id<KSerializer>)aSerializer bSerializer:(id<KSerializer>)bSerializer cSerializer:(id<KSerializer>)cSerializer __attribute__((swift_name("TripleSerializer(aSerializer:bSerializer:cSerializer:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
 *   kotlin.ExperimentalUnsignedTypes
*/
+ (id<KSerializer>)UByteArraySerializer __attribute__((swift_name("UByteArraySerializer()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
 *   kotlin.ExperimentalUnsignedTypes
*/
+ (id<KSerializer>)UIntArraySerializer __attribute__((swift_name("UIntArraySerializer()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
 *   kotlin.ExperimentalUnsignedTypes
*/
+ (id<KSerializer>)ULongArraySerializer __attribute__((swift_name("ULongArraySerializer()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
 *   kotlin.ExperimentalUnsignedTypes
*/
+ (id<KSerializer>)UShortArraySerializer __attribute__((swift_name("UShortArraySerializer()")));
@end

__attribute__((objc_subclassing_restricted))
@interface CancellableKt : Base
+ (void)startCoroutineCancellable:(id<KotlinSuspendFunction0>)receiver completion:(id<KotlinContinuation>)completion __attribute__((swift_name("startCoroutineCancellable(_:completion:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CancellableKt_ : Base
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
@interface CancellableContinuationKt_ : Base
+ (void)disposeOnCancellation:(id<CancellableContinuation>)receiver handle:(id<DisposableHandle>)handle __attribute__((swift_name("disposeOnCancellation(_:handle:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)suspendCancellableCoroutineBlock:(void (^)(id<CancellableContinuation>))block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("suspendCancellableCoroutine(block:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ChannelKt : Base
+ (id<Channel>)ChannelCapacity:(int32_t)capacity onBufferOverflow:(BufferOverflow *)onBufferOverflow onUndeliveredElement:(void (^ _Nullable)(id _Nullable))onUndeliveredElement __attribute__((swift_name("Channel(capacity:onBufferOverflow:onUndeliveredElement:)")));
+ (id _Nullable)getOrElse:(id _Nullable)receiver onFailure:(id _Nullable (^)(KotlinThrowable * _Nullable))onFailure __attribute__((swift_name("getOrElse(_:onFailure:)")));
+ (id _Nullable)onClosed:(id _Nullable)receiver action:(void (^)(KotlinThrowable * _Nullable))action __attribute__((swift_name("onClosed(_:action:)")));
+ (id _Nullable)onFailure:(id _Nullable)receiver action:(void (^)(KotlinThrowable * _Nullable))action __attribute__((swift_name("onFailure(_:action:)")));
+ (id _Nullable)onSuccess:(id _Nullable)receiver action:(void (^)(id _Nullable))action __attribute__((swift_name("onSuccess(_:action:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ChannelKt_ : Base
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

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toList:(id<ReceiveChannel>)receiver completionHandler:(void (^)(NSArray<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toList(_:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface Channels_commonKt_ : Base
+ (id _Nullable)consume:(id<ReceiveChannel>)receiver block:(id _Nullable (^)(id<ReceiveChannel>))block __attribute__((swift_name("consume(_:block:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)consumeEach:(id<ReceiveChannel>)receiver action:(void (^)(id _Nullable))action completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("consumeEach(_:action:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toList:(id<ReceiveChannel>)receiver completionHandler:(void (^)(NSArray<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toList(_:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ChannelsKt : Base
+ (id<Flow>)asFlow:(id<BroadcastChannel>)receiver __attribute__((swift_name("asFlow(_:)"))) __attribute__((unavailable("'BroadcastChannel' is obsolete and all corresponding operators are deprecated in the favour of StateFlow and SharedFlow")));
+ (id<Flow>)consumeAsFlow:(id<ReceiveChannel>)receiver __attribute__((swift_name("consumeAsFlow(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)emitAll:(id<FlowCollector>)receiver channel:(id<ReceiveChannel>)channel completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emitAll(_:channel:completionHandler:)")));
+ (id<ReceiveChannel>)produceIn:(id<Flow>)receiver scope:(id<CoroutineScope>)scope __attribute__((swift_name("produceIn(_:scope:)")));
+ (id<Flow>)receiveAsFlow:(id<ReceiveChannel>)receiver __attribute__((swift_name("receiveAsFlow(_:)")));
+ (id _Nullable)trySendBlocking:(id<SendChannel>)receiver element:(id _Nullable)element __attribute__((swift_name("trySendBlocking(_:element:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ChannelsKt_ : Base
+ (id<Flow>)asFlow:(id<BroadcastChannel>)receiver __attribute__((swift_name("asFlow(_:)"))) __attribute__((unavailable("'BroadcastChannel' is obsolete and all corresponding operators are deprecated in the favour of StateFlow and SharedFlow")));
+ (id<Flow>)consumeAsFlow:(id<ReceiveChannel>)receiver __attribute__((swift_name("consumeAsFlow(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)emitAll:(id<FlowCollector>)receiver channel:(id<ReceiveChannel>)channel completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emitAll(_:channel:completionHandler:)")));
+ (id<ReceiveChannel>)produceIn:(id<Flow>)receiver scope:(id<CoroutineScope>)scope __attribute__((swift_name("produceIn(_:scope:)")));
+ (id<Flow>)receiveAsFlow:(id<ReceiveChannel>)receiver __attribute__((swift_name("receiveAsFlow(_:)")));
+ (id _Nullable)trySendBlocking:(id<SendChannel>)receiver element:(id _Nullable)element __attribute__((swift_name("trySendBlocking(_:element:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ClockKt : Base

/**
 * @note annotations
 *   kotlin.time.ExperimentalTime
*/
+ (id<KotlinTimeSourceWithComparableMarks>)asTimeSource:(id<Clock>)receiver __attribute__((swift_name("asTimeSource(_:)")));
+ (LocalDate *)todayAt:(id<Clock>)receiver timeZone:(TimeZone *)timeZone __attribute__((swift_name("todayAt(_:timeZone:)"))) __attribute__((deprecated("Use Clock.todayIn instead")));
+ (LocalDate *)todayIn:(id<Clock>)receiver timeZone:(TimeZone *)timeZone __attribute__((swift_name("todayIn(_:timeZone:)")));
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
@interface CollectKt_ : Base

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)collect:(id<Flow>)receiver completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(_:completionHandler:)")));

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
@interface CollectionKt_ : Base

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
@interface CompletableDeferredKt_ : Base
+ (id<CompletableDeferred>)CompletableDeferredValue:(id _Nullable)value __attribute__((swift_name("CompletableDeferred(value:)")));
+ (id<CompletableDeferred>)CompletableDeferredParent:(id<Job> _Nullable)parent __attribute__((swift_name("CompletableDeferred(parent:)")));
+ (BOOL)completeWith:(id<CompletableDeferred>)receiver result:(id _Nullable)result __attribute__((swift_name("completeWith(_:result:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ContextKt : Base
+ (id<Flow>)buffer:(id<Flow>)receiver capacity:(int32_t)capacity onBufferOverflow:(BufferOverflow *)onBufferOverflow __attribute__((swift_name("buffer(_:capacity:onBufferOverflow:)")));
+ (id<Flow>)cancellable:(id<Flow>)receiver __attribute__((swift_name("cancellable(_:)")));
+ (id<Flow>)conflate:(id<Flow>)receiver __attribute__((swift_name("conflate(_:)")));
+ (id<Flow>)flowOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("flowOn(_:context:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ContextKt_ : Base
+ (id<Flow>)buffer:(id<Flow>)receiver capacity:(int32_t)capacity onBufferOverflow:(BufferOverflow *)onBufferOverflow __attribute__((swift_name("buffer(_:capacity:onBufferOverflow:)")));
+ (id<Flow>)cancellable:(id<Flow>)receiver __attribute__((swift_name("cancellable(_:)")));
+ (id<Flow>)conflate:(id<Flow>)receiver __attribute__((swift_name("conflate(_:)")));
+ (id<Flow>)flowOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("flowOn(_:context:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ContextAwareKt : Base
+ (id<KotlinKClass> _Nullable)capturedKClass:(id<SerialDescriptor>)receiver __attribute__((swift_name("capturedKClass(_:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CoroutineContextKt : Base
+ (id<KotlinCoroutineContext>)doNewCoroutineContext:(id<KotlinCoroutineContext>)receiver addedContext:(id<KotlinCoroutineContext>)addedContext __attribute__((swift_name("doNewCoroutineContext(_:addedContext:)")));
+ (id<KotlinCoroutineContext>)doNewCoroutineContext:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("doNewCoroutineContext(_:context:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CoroutineContextKt_ : Base
+ (id<KotlinCoroutineContext>)doNewCoroutineContext:(id<KotlinCoroutineContext>)receiver addedContext:(id<KotlinCoroutineContext>)addedContext __attribute__((swift_name("doNewCoroutineContext(_:addedContext:)")));
+ (id<KotlinCoroutineContext>)doNewCoroutineContext:(id<CoroutineScope>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("doNewCoroutineContext(_:context:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CoroutineExceptionHandlerKt : Base
+ (id<CoroutineExceptionHandler>)CoroutineExceptionHandlerHandler:(void (^)(id<KotlinCoroutineContext>, KotlinThrowable *))handler __attribute__((swift_name("CoroutineExceptionHandler(handler:)")));
+ (void)handleCoroutineExceptionContext:(id<KotlinCoroutineContext>)context exception:(KotlinThrowable *)exception __attribute__((swift_name("handleCoroutineException(context:exception:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface CoroutineExceptionHandlerKt_ : Base
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
@interface CoroutineScopeKt_ : Base
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
@interface CountKt_ : Base

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
@interface DateTimeComponentsKt : Base
+ (NSString *)format:(id<DateTimeFormat>)receiver block:(void (^)(DateTimeComponents *))block __attribute__((swift_name("format(_:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimeFormatBuilderKt : Base
+ (void)alternativeParsing:(id<DateTimeFormatBuilder>)receiver alternativeFormats:(KotlinArray<KotlinUnit *(^)(id<DateTimeFormatBuilder>)> *)alternativeFormats primaryFormat:(void (^)(id<DateTimeFormatBuilder>))primaryFormat __attribute__((swift_name("alternativeParsing(_:alternativeFormats:primaryFormat:)")));
+ (void)char:(id<DateTimeFormatBuilder>)receiver value:(unichar)value __attribute__((swift_name("char(_:value:)")));
+ (void)optional:(id<DateTimeFormatBuilder>)receiver ifZero:(NSString *)ifZero format:(void (^)(id<DateTimeFormatBuilder>))format __attribute__((swift_name("optional(_:ifZero:format:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimePeriodKt : Base
+ (DateTimePeriod *)DateTimePeriodYears:(int32_t)years months:(int32_t)months days:(int32_t)days hours:(int32_t)hours minutes:(int32_t)minutes seconds:(int32_t)seconds nanoseconds:(int64_t)nanoseconds __attribute__((swift_name("DateTimePeriod(years:months:days:hours:minutes:seconds:nanoseconds:)")));
+ (DatePeriod *)toDatePeriod:(NSString *)receiver __attribute__((swift_name("toDatePeriod(_:)"))) __attribute__((deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339")));
+ (DateTimePeriod *)toDateTimePeriod:(NSString *)receiver __attribute__((swift_name("toDateTimePeriod(_:)"))) __attribute__((deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339")));
+ (DateTimePeriod *)toDateTimePeriod_:(int64_t)receiver __attribute__((swift_name("toDateTimePeriod(__:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DayOfWeekKt : Base
+ (DayOfWeek *)DayOfWeekIsoDayNumber:(int32_t)isoDayNumber __attribute__((swift_name("DayOfWeek(isoDayNumber:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DecodingKt : Base
+ (id _Nullable)decodeStructure:(id<Decoder>)receiver descriptor:(id<SerialDescriptor>)descriptor block:(id _Nullable (^)(id<CompositeDecoder>))block __attribute__((swift_name("decodeStructure(_:descriptor:block:)")));
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
@interface DelayKt_ : Base

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
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id _Nullable)consume:(id<BroadcastChannel>)receiver block:(id _Nullable (^)(id<ReceiveChannel>))block __attribute__((swift_name("consume(_:block:)"))) __attribute__((unavailable("BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)consumeEach:(id<BroadcastChannel>)receiver action:(void (^)(id _Nullable))action completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("consumeEach(_:action:completionHandler:)"))) __attribute__((unavailable("BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));
@end

__attribute__((objc_subclassing_restricted))
@interface DeprecatedKt_ : Base

/**
 * @note annotations
 *   kotlinx.coroutines.ObsoleteCoroutinesApi
*/
+ (id _Nullable)consume:(id<BroadcastChannel>)receiver block:(id _Nullable (^)(id<ReceiveChannel>))block __attribute__((swift_name("consume(_:block:)"))) __attribute__((unavailable("BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)consumeEach:(id<BroadcastChannel>)receiver action:(void (^)(id _Nullable))action completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("consumeEach(_:action:completionHandler:)"))) __attribute__((unavailable("BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")));
@end

__attribute__((objc_subclassing_restricted))
@interface DispatchedContinuationKt : Base
+ (void)resumeCancellableWith:(id<KotlinContinuation>)receiver result:(id _Nullable)result onCancellation:(void (^ _Nullable)(KotlinThrowable *))onCancellation __attribute__((swift_name("resumeCancellableWith(_:result:onCancellation:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DispatchedContinuationKt_ : Base
+ (void)resumeCancellableWith:(id<KotlinContinuation>)receiver result:(id _Nullable)result onCancellation:(void (^ _Nullable)(KotlinThrowable *))onCancellation __attribute__((swift_name("resumeCancellableWith(_:result:onCancellation:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DistinctKt : Base
+ (id<Flow>)distinctUntilChanged:(id<Flow>)receiver __attribute__((swift_name("distinctUntilChanged(_:)")));
+ (id<Flow>)distinctUntilChanged:(id<Flow>)receiver areEquivalent:(Boolean *(^)(id _Nullable, id _Nullable))areEquivalent __attribute__((swift_name("distinctUntilChanged(_:areEquivalent:)")));
+ (id<Flow>)distinctUntilChangedBy:(id<Flow>)receiver keySelector:(id _Nullable (^)(id _Nullable))keySelector __attribute__((swift_name("distinctUntilChangedBy(_:keySelector:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DistinctKt_ : Base
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
@interface EmittersKt_ : Base
+ (id<Flow>)onCompletion:(id<Flow>)receiver action:(id<KotlinSuspendFunction2>)action __attribute__((swift_name("onCompletion(_:action:)")));
+ (id<Flow>)onEmpty:(id<Flow>)receiver action:(id<KotlinSuspendFunction1>)action __attribute__((swift_name("onEmpty(_:action:)")));
+ (id<Flow>)onStart:(id<Flow>)receiver action:(id<KotlinSuspendFunction1>)action __attribute__((swift_name("onStart(_:action:)")));
+ (id<Flow>)transform:(id<Flow>)receiver transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("transform(_:transform:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface EncodingKt : Base
+ (void)encodeCollection:(id<Encoder>)receiver descriptor:(id<SerialDescriptor>)descriptor collectionSize:(int32_t)collectionSize block:(void (^)(id<CompositeEncoder>))block __attribute__((swift_name("encodeCollection(_:descriptor:collectionSize:block:)")));
+ (void)encodeCollection:(id<Encoder>)receiver descriptor:(id<SerialDescriptor>)descriptor collection:(id)collection block:(void (^)(id<CompositeEncoder>, Int *, id _Nullable))block __attribute__((swift_name("encodeCollection(_:descriptor:collection:block:)")));
+ (void)encodeStructure:(id<Encoder>)receiver descriptor:(id<SerialDescriptor>)descriptor block:(void (^)(id<CompositeEncoder>))block __attribute__((swift_name("encodeStructure(_:descriptor:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ErrorsKt : Base
+ (id<Flow>)catch:(id<Flow>)receiver action:(id<KotlinSuspendFunction2>)action __attribute__((swift_name("catch(_:action:)")));
+ (id<Flow>)retry:(id<Flow>)receiver retries:(int64_t)retries predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("retry(_:retries:predicate:)")));
+ (id<Flow>)retryWhen:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction3>)predicate __attribute__((swift_name("retryWhen(_:predicate:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ErrorsKt_ : Base
+ (id<Flow>)catch:(id<Flow>)receiver action:(id<KotlinSuspendFunction2>)action __attribute__((swift_name("catch(_:action:)")));
+ (id<Flow>)retry:(id<Flow>)receiver retries:(int64_t)retries predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("retry(_:retries:predicate:)")));
+ (id<Flow>)retryWhen:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction3>)predicate __attribute__((swift_name("retryWhen(_:predicate:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ExceptionsKt : Base
+ (KotlinCancellationException *)CancellationExceptionMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("CancellationException(message:cause:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface ExceptionsKt_ : Base
+ (KotlinCancellationException *)CancellationExceptionMessage:(NSString * _Nullable)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("CancellationException(message:cause:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface InlineClassDescriptorKt : Base

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (id<SerialDescriptor>)InlinePrimitiveDescriptorName:(NSString *)name primitiveSerializer:(id<KSerializer>)primitiveSerializer __attribute__((swift_name("InlinePrimitiveDescriptor(name:primitiveSerializer:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface InstantKt : Base
+ (Instant *)toInstant:(NSString *)receiver __attribute__((swift_name("toInstant(_:)"))) __attribute__((deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339")));
@end

__attribute__((objc_subclassing_restricted))
@interface JobKt : Base
+ (BOOL)isActive:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("isActive(_:)")));
+ (id<Job>)job:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("job(_:)")));
+ (id<CompletableJob>)JobParent:(id<Job> _Nullable)parent __attribute__((swift_name("Job(parent:)")));
+ (void)cancel:(id<KotlinCoroutineContext>)receiver cause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(_:cause:)")));
+ (void)cancel:(id<Job>)receiver message:(NSString *)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(_:message:cause:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)cancelAndJoin:(id<Job>)receiver completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("cancelAndJoin(_:completionHandler:)")));
+ (void)cancelChildren:(id<KotlinCoroutineContext>)receiver cause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancelChildren(_:cause:)")));
+ (void)cancelChildren:(id<Job>)receiver cause_:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancelChildren(_:cause_:)")));
+ (void)ensureActive:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("ensureActive(_:)")));
+ (void)ensureActive_:(id<Job>)receiver __attribute__((swift_name("ensureActive(__:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface JobKt_ : Base
+ (BOOL)isActive:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("isActive(_:)")));
+ (id<Job>)job:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("job(_:)")));
+ (id<CompletableJob>)JobParent:(id<Job> _Nullable)parent __attribute__((swift_name("Job(parent:)")));
+ (void)cancel:(id<KotlinCoroutineContext>)receiver cause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(_:cause:)")));
+ (void)cancel:(id<Job>)receiver message:(NSString *)message cause:(KotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(_:message:cause:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)cancelAndJoin:(id<Job>)receiver completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("cancelAndJoin(_:completionHandler:)")));
+ (void)cancelChildren:(id<KotlinCoroutineContext>)receiver cause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancelChildren(_:cause:)")));
+ (void)cancelChildren:(id<Job>)receiver cause_:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancelChildren(_:cause_:)")));
+ (void)ensureActive:(id<KotlinCoroutineContext>)receiver __attribute__((swift_name("ensureActive(_:)")));
+ (void)ensureActive_:(id<Job>)receiver __attribute__((swift_name("ensureActive(__:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface JsonInternalDependenciesKt : Base
+ (NSSet<NSString *> *)jsonCachedSerialNames:(id<SerialDescriptor>)receiver __attribute__((swift_name("jsonCachedSerialNames(_:)")));
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
@interface LimitKt_ : Base
+ (id<Flow>)drop:(id<Flow>)receiver count:(int32_t)count __attribute__((swift_name("drop(_:count:)")));
+ (id<Flow>)dropWhile:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("dropWhile(_:predicate:)")));
+ (id<Flow>)take:(id<Flow>)receiver count:(int32_t)count __attribute__((swift_name("take(_:count:)")));
+ (id<Flow>)takeWhile:(id<Flow>)receiver predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("takeWhile(_:predicate:)")));
+ (id<Flow>)transformWhile:(id<Flow>)receiver transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("transformWhile(_:transform:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface LintKt : Base
+ (id<KotlinCoroutineContext>)coroutineContext:(id<FlowCollector>)receiver __attribute__((swift_name("coroutineContext(_:)"))) __attribute__((unavailable("coroutineContext is resolved into the property of outer CoroutineScope which is likely to be an error.Use currentCoroutineContext() instead or specify the receiver of coroutineContext explicitly")));
+ (BOOL)isActive:(id<FlowCollector>)receiver __attribute__((swift_name("isActive(_:)"))) __attribute__((unavailable("isActive is resolved into the extension of outer CoroutineScope which is likely to be an error.Use currentCoroutineContext().isActive or cancellable() operator instead or specify the receiver of isActive explicitly. Additionally, flow {} builder emissions are cancellable by default.")));
+ (void)cancel:(id<FlowCollector>)receiver cause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(_:cause:)"))) __attribute__((unavailable("cancel() is resolved into the extension of outer CoroutineScope which is likely to be an error.Use currentCoroutineContext().cancel() instead or specify the receiver of cancel() explicitly")));
+ (id<Flow>)cancellable:(id<SharedFlow>)receiver __attribute__((swift_name("cancellable(_:)"))) __attribute__((unavailable("Applying 'cancellable' to a SharedFlow has no effect. See the SharedFlow documentation on Operator Fusion.")));
+ (id<Flow>)catch:(id<SharedFlow>)receiver action:(id<KotlinSuspendFunction2>)action __attribute__((swift_name("catch(_:action:)"))) __attribute__((deprecated("SharedFlow never completes, so this operator typically has not effect, it can only catch exceptions from 'onSubscribe' operator")));
+ (id<Flow>)conflate:(id<StateFlow>)receiver __attribute__((swift_name("conflate(_:)"))) __attribute__((unavailable("Applying 'conflate' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)count:(id<SharedFlow>)receiver completionHandler:(void (^)(Int * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("count(_:completionHandler:)"))) __attribute__((deprecated("SharedFlow never completes, so this terminal operation never completes.")));
+ (id<Flow>)distinctUntilChanged:(id<StateFlow>)receiver __attribute__((swift_name("distinctUntilChanged(_:)"))) __attribute__((unavailable("Applying 'distinctUntilChanged' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.")));
+ (id<Flow>)flowOn:(id<SharedFlow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("flowOn(_:context:)"))) __attribute__((unavailable("Applying 'flowOn' to SharedFlow has no effect. See the SharedFlow documentation on Operator Fusion.")));
+ (id<Flow>)retry:(id<SharedFlow>)receiver retries:(int64_t)retries predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("retry(_:retries:predicate:)"))) __attribute__((deprecated("SharedFlow never completes, so this operator has no effect.")));
+ (id<Flow>)retryWhen:(id<SharedFlow>)receiver predicate:(id<KotlinSuspendFunction3>)predicate __attribute__((swift_name("retryWhen(_:predicate:)"))) __attribute__((deprecated("SharedFlow never completes, so this operator has no effect.")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toList:(id<SharedFlow>)receiver completionHandler:(void (^)(NSArray<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toList(_:completionHandler:)"))) __attribute__((deprecated("SharedFlow never completes, so this terminal operation never completes.")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toList:(id<SharedFlow>)receiver destination:(NSMutableArray<id> *)destination completionHandler:(void (^)(KotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toList(_:destination:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toSet:(id<SharedFlow>)receiver completionHandler:(void (^)(NSSet<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toSet(_:completionHandler:)"))) __attribute__((deprecated("SharedFlow never completes, so this terminal operation never completes.")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toSet:(id<SharedFlow>)receiver destination:(MutableSet<id> *)destination completionHandler:(void (^)(KotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toSet(_:destination:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface LintKt_ : Base
+ (id<KotlinCoroutineContext>)coroutineContext:(id<FlowCollector>)receiver __attribute__((swift_name("coroutineContext(_:)"))) __attribute__((unavailable("coroutineContext is resolved into the property of outer CoroutineScope which is likely to be an error.Use currentCoroutineContext() instead or specify the receiver of coroutineContext explicitly")));
+ (BOOL)isActive:(id<FlowCollector>)receiver __attribute__((swift_name("isActive(_:)"))) __attribute__((unavailable("isActive is resolved into the extension of outer CoroutineScope which is likely to be an error.Use currentCoroutineContext().isActive or cancellable() operator instead or specify the receiver of isActive explicitly. Additionally, flow {} builder emissions are cancellable by default.")));
+ (void)cancel:(id<FlowCollector>)receiver cause:(KotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(_:cause:)"))) __attribute__((unavailable("cancel() is resolved into the extension of outer CoroutineScope which is likely to be an error.Use currentCoroutineContext().cancel() instead or specify the receiver of cancel() explicitly")));
+ (id<Flow>)cancellable:(id<SharedFlow>)receiver __attribute__((swift_name("cancellable(_:)"))) __attribute__((unavailable("Applying 'cancellable' to a SharedFlow has no effect. See the SharedFlow documentation on Operator Fusion.")));
+ (id<Flow>)catch:(id<SharedFlow>)receiver action:(id<KotlinSuspendFunction2>)action __attribute__((swift_name("catch(_:action:)"))) __attribute__((deprecated("SharedFlow never completes, so this operator typically has not effect, it can only catch exceptions from 'onSubscribe' operator")));
+ (id<Flow>)conflate:(id<StateFlow>)receiver __attribute__((swift_name("conflate(_:)"))) __attribute__((unavailable("Applying 'conflate' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)count:(id<SharedFlow>)receiver completionHandler:(void (^)(Int * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("count(_:completionHandler:)"))) __attribute__((deprecated("SharedFlow never completes, so this terminal operation never completes.")));
+ (id<Flow>)distinctUntilChanged:(id<StateFlow>)receiver __attribute__((swift_name("distinctUntilChanged(_:)"))) __attribute__((unavailable("Applying 'distinctUntilChanged' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.")));
+ (id<Flow>)flowOn:(id<SharedFlow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("flowOn(_:context:)"))) __attribute__((unavailable("Applying 'flowOn' to SharedFlow has no effect. See the SharedFlow documentation on Operator Fusion.")));
+ (id<Flow>)retry:(id<SharedFlow>)receiver retries:(int64_t)retries predicate:(id<KotlinSuspendFunction1>)predicate __attribute__((swift_name("retry(_:retries:predicate:)"))) __attribute__((deprecated("SharedFlow never completes, so this operator has no effect.")));
+ (id<Flow>)retryWhen:(id<SharedFlow>)receiver predicate:(id<KotlinSuspendFunction3>)predicate __attribute__((swift_name("retryWhen(_:predicate:)"))) __attribute__((deprecated("SharedFlow never completes, so this operator has no effect.")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toList:(id<SharedFlow>)receiver completionHandler:(void (^)(NSArray<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toList(_:completionHandler:)"))) __attribute__((deprecated("SharedFlow never completes, so this terminal operation never completes.")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toList:(id<SharedFlow>)receiver destination:(NSMutableArray<id> *)destination completionHandler:(void (^)(KotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toList(_:destination:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toSet:(id<SharedFlow>)receiver completionHandler:(void (^)(NSSet<id> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toSet(_:completionHandler:)"))) __attribute__((deprecated("SharedFlow never completes, so this terminal operation never completes.")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)toSet:(id<SharedFlow>)receiver destination:(MutableSet<id> *)destination completionHandler:(void (^)(KotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("toSet(_:destination:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalDateKt : Base
+ (LocalDate *)toLocalDate:(NSString *)receiver __attribute__((swift_name("toLocalDate(_:)"))) __attribute__((deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalDateTimeKt : Base
+ (LocalDateTime *)toLocalDateTime:(NSString *)receiver __attribute__((swift_name("toLocalDateTime(_:)"))) __attribute__((deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalTimeKt : Base
+ (LocalTime *)toLocalTime:(NSString *)receiver __attribute__((swift_name("toLocalTime(_:)"))) __attribute__((deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339")));
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
@interface MergeKt_ : Base

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
+ (id<Flow>)cache:(id<Flow>)receiver __attribute__((swift_name("cache(_:)"))) __attribute__((unavailable("Flow analogue of 'cache()' is 'shareIn' with unlimited replay and 'started = SharingStared.Lazily' argument'")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("combineLatest(_:other:transform:)"))) __attribute__((unavailable("Flow analogue of 'combineLatest' is 'combine'")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other other2:(id<Flow>)other2 transform:(id<KotlinSuspendFunction3>)transform __attribute__((swift_name("combineLatest(_:other:other2:transform:)"))) __attribute__((unavailable("Flow analogue of 'combineLatest' is 'combine'")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other other2:(id<Flow>)other2 other3:(id<Flow>)other3 transform:(id<KotlinSuspendFunction4>)transform __attribute__((swift_name("combineLatest(_:other:other2:other3:transform:)"))) __attribute__((unavailable("Flow analogue of 'combineLatest' is 'combine'")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other other2:(id<Flow>)other2 other3:(id<Flow>)other3 other4:(id<Flow>)other4 transform:(id<KotlinSuspendFunction5>)transform __attribute__((swift_name("combineLatest(_:other:other2:other3:other4:transform:)"))) __attribute__((unavailable("Flow analogue of 'combineLatest' is 'combine'")));
+ (id<Flow>)compose:(id<Flow>)receiver transformer:(id<Flow> (^)(id<Flow>))transformer __attribute__((swift_name("compose(_:transformer:)"))) __attribute__((unavailable("Flow analogue of 'compose' is 'let'")));
+ (id<Flow>)concatMap:(id<Flow>)receiver mapper:(id<Flow> (^)(id _Nullable))mapper __attribute__((swift_name("concatMap(_:mapper:)"))) __attribute__((unavailable("Flow analogue of 'concatMap' is 'flatMapConcat'")));
+ (id<Flow>)concatWith:(id<Flow>)receiver value:(id _Nullable)value __attribute__((swift_name("concatWith(_:value:)"))) __attribute__((unavailable("Flow analogue of 'concatWith' is 'onCompletion'. Use 'onCompletion { emit(value) }'")));
+ (id<Flow>)concatWith:(id<Flow>)receiver other:(id<Flow>)other __attribute__((swift_name("concatWith(_:other:)"))) __attribute__((unavailable("Flow analogue of 'concatWith' is 'onCompletion'. Use 'onCompletion { if (it == null) emitAll(other) }'")));
+ (id<Flow>)delayEach:(id<Flow>)receiver timeMillis:(int64_t)timeMillis __attribute__((swift_name("delayEach(_:timeMillis:)"))) __attribute__((unavailable("Use 'onEach { delay(timeMillis) }'")));
+ (id<Flow>)delayFlow:(id<Flow>)receiver timeMillis:(int64_t)timeMillis __attribute__((swift_name("delayFlow(_:timeMillis:)"))) __attribute__((unavailable("Use 'onStart { delay(timeMillis) }'")));
+ (id<Flow>)flatMap:(id<Flow>)receiver mapper:(id<KotlinSuspendFunction1>)mapper __attribute__((swift_name("flatMap(_:mapper:)"))) __attribute__((unavailable("Flow analogue is 'flatMapConcat'")));
+ (id<Flow>)flatten:(id<Flow>)receiver __attribute__((swift_name("flatten(_:)"))) __attribute__((unavailable("Flow analogue of 'flatten' is 'flattenConcat'")));
+ (void)forEach:(id<Flow>)receiver action:(id<KotlinSuspendFunction1>)action __attribute__((swift_name("forEach(_:action:)"))) __attribute__((unavailable("Flow analogue of 'forEach' is 'collect'")));
+ (id<Flow>)merge:(id<Flow>)receiver __attribute__((swift_name("merge(_:)"))) __attribute__((unavailable("Flow analogue of 'merge' is 'flattenConcat'")));
+ (id<Flow>)observeOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("observeOn(_:context:)"))) __attribute__((unavailable("Collect flow in the desired context instead")));
+ (id<Flow>)onErrorResume:(id<Flow>)receiver fallback:(id<Flow>)fallback __attribute__((swift_name("onErrorResume(_:fallback:)"))) __attribute__((unavailable("Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emitAll(fallback) }'")));
+ (id<Flow>)onErrorResumeNext:(id<Flow>)receiver fallback:(id<Flow>)fallback __attribute__((swift_name("onErrorResumeNext(_:fallback:)"))) __attribute__((unavailable("Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emitAll(fallback) }'")));
+ (id<Flow>)onErrorReturn:(id<Flow>)receiver fallback:(id _Nullable)fallback __attribute__((swift_name("onErrorReturn(_:fallback:)"))) __attribute__((unavailable("Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emit(fallback) }'")));
+ (id<Flow>)onErrorReturn:(id<Flow>)receiver fallback:(id _Nullable)fallback predicate:(Boolean *(^)(KotlinThrowable *))predicate __attribute__((swift_name("onErrorReturn(_:fallback:predicate:)"))) __attribute__((unavailable("Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { e -> if (predicate(e)) emit(fallback) else throw e }'")));
+ (id<Flow>)publish:(id<Flow>)receiver __attribute__((swift_name("publish(_:)"))) __attribute__((unavailable("Flow analogue of 'publish()' is 'shareIn'. \npublish().connect() is the default strategy (no extra call is needed), \npublish().autoConnect() translates to 'started = SharingStared.Lazily' argument, \npublish().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.")));
+ (id<Flow>)publish:(id<Flow>)receiver bufferSize:(int32_t)bufferSize __attribute__((swift_name("publish(_:bufferSize:)"))) __attribute__((unavailable("Flow analogue of 'publish(bufferSize)' is 'buffer' followed by 'shareIn'. \npublish().connect() is the default strategy (no extra call is needed), \npublish().autoConnect() translates to 'started = SharingStared.Lazily' argument, \npublish().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.")));
+ (id<Flow>)publishOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("publishOn(_:context:)"))) __attribute__((unavailable("Collect flow in the desired context instead")));
+ (id<Flow>)replay:(id<Flow>)receiver __attribute__((swift_name("replay(_:)"))) __attribute__((unavailable("Flow analogue of 'replay()' is 'shareIn' with unlimited replay. \nreplay().connect() is the default strategy (no extra call is needed), \nreplay().autoConnect() translates to 'started = SharingStared.Lazily' argument, \nreplay().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.")));
+ (id<Flow>)replay:(id<Flow>)receiver bufferSize:(int32_t)bufferSize __attribute__((swift_name("replay(_:bufferSize:)"))) __attribute__((unavailable("Flow analogue of 'replay(bufferSize)' is 'shareIn' with the specified replay parameter. \nreplay().connect() is the default strategy (no extra call is needed), \nreplay().autoConnect() translates to 'started = SharingStared.Lazily' argument, \nreplay().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.")));
+ (id<Flow>)scanFold:(id<Flow>)receiver initial:(id _Nullable)initial operation:(id<KotlinSuspendFunction2>)operation __attribute__((swift_name("scanFold(_:initial:operation:)"))) __attribute__((unavailable("Flow has less verbose 'scan' shortcut")));
+ (id<Flow>)scanReduce:(id<Flow>)receiver operation:(id<KotlinSuspendFunction2>)operation __attribute__((swift_name("scanReduce(_:operation:)"))) __attribute__((unavailable("'scanReduce' was renamed to 'runningReduce' to be consistent with Kotlin standard library")));
+ (id<Flow>)skip:(id<Flow>)receiver count:(int32_t)count __attribute__((swift_name("skip(_:count:)"))) __attribute__((unavailable("Flow analogue of 'skip' is 'drop'")));
+ (id<Flow>)startWith:(id<Flow>)receiver value:(id _Nullable)value __attribute__((swift_name("startWith(_:value:)"))) __attribute__((unavailable("Flow analogue of 'startWith' is 'onStart'. Use 'onStart { emit(value) }'")));
+ (id<Flow>)startWith:(id<Flow>)receiver other:(id<Flow>)other __attribute__((swift_name("startWith(_:other:)"))) __attribute__((unavailable("Flow analogue of 'startWith' is 'onStart'. Use 'onStart { emitAll(other) }'")));
+ (void)subscribe:(id<Flow>)receiver __attribute__((swift_name("subscribe(_:)"))) __attribute__((unavailable("Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")));
+ (void)subscribe:(id<Flow>)receiver onEach:(id<KotlinSuspendFunction1>)onEach __attribute__((swift_name("subscribe(_:onEach:)"))) __attribute__((unavailable("Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")));
+ (void)subscribe:(id<Flow>)receiver onEach:(id<KotlinSuspendFunction1>)onEach onError:(id<KotlinSuspendFunction1>)onError __attribute__((swift_name("subscribe(_:onEach:onError:)"))) __attribute__((unavailable("Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")));
+ (id<Flow>)subscribeOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("subscribeOn(_:context:)"))) __attribute__((unavailable("Use 'flowOn' instead")));
+ (id<Flow>)switchMap:(id<Flow>)receiver transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("switchMap(_:transform:)"))) __attribute__((unavailable("Flow analogues of 'switchMap' are 'transformLatest', 'flatMapLatest' and 'mapLatest'")));
@end

__attribute__((objc_subclassing_restricted))
@interface MigrationKt_ : Base
+ (id<Flow>)cache:(id<Flow>)receiver __attribute__((swift_name("cache(_:)"))) __attribute__((unavailable("Flow analogue of 'cache()' is 'shareIn' with unlimited replay and 'started = SharingStared.Lazily' argument'")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other transform:(id<KotlinSuspendFunction2>)transform __attribute__((swift_name("combineLatest(_:other:transform:)"))) __attribute__((unavailable("Flow analogue of 'combineLatest' is 'combine'")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other other2:(id<Flow>)other2 transform:(id<KotlinSuspendFunction3>)transform __attribute__((swift_name("combineLatest(_:other:other2:transform:)"))) __attribute__((unavailable("Flow analogue of 'combineLatest' is 'combine'")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other other2:(id<Flow>)other2 other3:(id<Flow>)other3 transform:(id<KotlinSuspendFunction4>)transform __attribute__((swift_name("combineLatest(_:other:other2:other3:transform:)"))) __attribute__((unavailable("Flow analogue of 'combineLatest' is 'combine'")));
+ (id<Flow>)combineLatest:(id<Flow>)receiver other:(id<Flow>)other other2:(id<Flow>)other2 other3:(id<Flow>)other3 other4:(id<Flow>)other4 transform:(id<KotlinSuspendFunction5>)transform __attribute__((swift_name("combineLatest(_:other:other2:other3:other4:transform:)"))) __attribute__((unavailable("Flow analogue of 'combineLatest' is 'combine'")));
+ (id<Flow>)compose:(id<Flow>)receiver transformer:(id<Flow> (^)(id<Flow>))transformer __attribute__((swift_name("compose(_:transformer:)"))) __attribute__((unavailable("Flow analogue of 'compose' is 'let'")));
+ (id<Flow>)concatMap:(id<Flow>)receiver mapper:(id<Flow> (^)(id _Nullable))mapper __attribute__((swift_name("concatMap(_:mapper:)"))) __attribute__((unavailable("Flow analogue of 'concatMap' is 'flatMapConcat'")));
+ (id<Flow>)concatWith:(id<Flow>)receiver value:(id _Nullable)value __attribute__((swift_name("concatWith(_:value:)"))) __attribute__((unavailable("Flow analogue of 'concatWith' is 'onCompletion'. Use 'onCompletion { emit(value) }'")));
+ (id<Flow>)concatWith:(id<Flow>)receiver other:(id<Flow>)other __attribute__((swift_name("concatWith(_:other:)"))) __attribute__((unavailable("Flow analogue of 'concatWith' is 'onCompletion'. Use 'onCompletion { if (it == null) emitAll(other) }'")));
+ (id<Flow>)delayEach:(id<Flow>)receiver timeMillis:(int64_t)timeMillis __attribute__((swift_name("delayEach(_:timeMillis:)"))) __attribute__((unavailable("Use 'onEach { delay(timeMillis) }'")));
+ (id<Flow>)delayFlow:(id<Flow>)receiver timeMillis:(int64_t)timeMillis __attribute__((swift_name("delayFlow(_:timeMillis:)"))) __attribute__((unavailable("Use 'onStart { delay(timeMillis) }'")));
+ (id<Flow>)flatMap:(id<Flow>)receiver mapper:(id<KotlinSuspendFunction1>)mapper __attribute__((swift_name("flatMap(_:mapper:)"))) __attribute__((unavailable("Flow analogue is 'flatMapConcat'")));
+ (id<Flow>)flatten:(id<Flow>)receiver __attribute__((swift_name("flatten(_:)"))) __attribute__((unavailable("Flow analogue of 'flatten' is 'flattenConcat'")));
+ (void)forEach:(id<Flow>)receiver action:(id<KotlinSuspendFunction1>)action __attribute__((swift_name("forEach(_:action:)"))) __attribute__((unavailable("Flow analogue of 'forEach' is 'collect'")));
+ (id<Flow>)merge:(id<Flow>)receiver __attribute__((swift_name("merge(_:)"))) __attribute__((unavailable("Flow analogue of 'merge' is 'flattenConcat'")));
+ (id<Flow>)observeOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("observeOn(_:context:)"))) __attribute__((unavailable("Collect flow in the desired context instead")));
+ (id<Flow>)onErrorResume:(id<Flow>)receiver fallback:(id<Flow>)fallback __attribute__((swift_name("onErrorResume(_:fallback:)"))) __attribute__((unavailable("Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emitAll(fallback) }'")));
+ (id<Flow>)onErrorResumeNext:(id<Flow>)receiver fallback:(id<Flow>)fallback __attribute__((swift_name("onErrorResumeNext(_:fallback:)"))) __attribute__((unavailable("Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emitAll(fallback) }'")));
+ (id<Flow>)onErrorReturn:(id<Flow>)receiver fallback:(id _Nullable)fallback __attribute__((swift_name("onErrorReturn(_:fallback:)"))) __attribute__((unavailable("Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emit(fallback) }'")));
+ (id<Flow>)onErrorReturn:(id<Flow>)receiver fallback:(id _Nullable)fallback predicate:(Boolean *(^)(KotlinThrowable *))predicate __attribute__((swift_name("onErrorReturn(_:fallback:predicate:)"))) __attribute__((unavailable("Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { e -> if (predicate(e)) emit(fallback) else throw e }'")));
+ (id<Flow>)publish:(id<Flow>)receiver __attribute__((swift_name("publish(_:)"))) __attribute__((unavailable("Flow analogue of 'publish()' is 'shareIn'. \npublish().connect() is the default strategy (no extra call is needed), \npublish().autoConnect() translates to 'started = SharingStared.Lazily' argument, \npublish().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.")));
+ (id<Flow>)publish:(id<Flow>)receiver bufferSize:(int32_t)bufferSize __attribute__((swift_name("publish(_:bufferSize:)"))) __attribute__((unavailable("Flow analogue of 'publish(bufferSize)' is 'buffer' followed by 'shareIn'. \npublish().connect() is the default strategy (no extra call is needed), \npublish().autoConnect() translates to 'started = SharingStared.Lazily' argument, \npublish().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.")));
+ (id<Flow>)publishOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("publishOn(_:context:)"))) __attribute__((unavailable("Collect flow in the desired context instead")));
+ (id<Flow>)replay:(id<Flow>)receiver __attribute__((swift_name("replay(_:)"))) __attribute__((unavailable("Flow analogue of 'replay()' is 'shareIn' with unlimited replay. \nreplay().connect() is the default strategy (no extra call is needed), \nreplay().autoConnect() translates to 'started = SharingStared.Lazily' argument, \nreplay().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.")));
+ (id<Flow>)replay:(id<Flow>)receiver bufferSize:(int32_t)bufferSize __attribute__((swift_name("replay(_:bufferSize:)"))) __attribute__((unavailable("Flow analogue of 'replay(bufferSize)' is 'shareIn' with the specified replay parameter. \nreplay().connect() is the default strategy (no extra call is needed), \nreplay().autoConnect() translates to 'started = SharingStared.Lazily' argument, \nreplay().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.")));
+ (id<Flow>)scanFold:(id<Flow>)receiver initial:(id _Nullable)initial operation:(id<KotlinSuspendFunction2>)operation __attribute__((swift_name("scanFold(_:initial:operation:)"))) __attribute__((unavailable("Flow has less verbose 'scan' shortcut")));
+ (id<Flow>)scanReduce:(id<Flow>)receiver operation:(id<KotlinSuspendFunction2>)operation __attribute__((swift_name("scanReduce(_:operation:)"))) __attribute__((unavailable("'scanReduce' was renamed to 'runningReduce' to be consistent with Kotlin standard library")));
+ (id<Flow>)skip:(id<Flow>)receiver count:(int32_t)count __attribute__((swift_name("skip(_:count:)"))) __attribute__((unavailable("Flow analogue of 'skip' is 'drop'")));
+ (id<Flow>)startWith:(id<Flow>)receiver value:(id _Nullable)value __attribute__((swift_name("startWith(_:value:)"))) __attribute__((unavailable("Flow analogue of 'startWith' is 'onStart'. Use 'onStart { emit(value) }'")));
+ (id<Flow>)startWith:(id<Flow>)receiver other:(id<Flow>)other __attribute__((swift_name("startWith(_:other:)"))) __attribute__((unavailable("Flow analogue of 'startWith' is 'onStart'. Use 'onStart { emitAll(other) }'")));
+ (void)subscribe:(id<Flow>)receiver __attribute__((swift_name("subscribe(_:)"))) __attribute__((unavailable("Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")));
+ (void)subscribe:(id<Flow>)receiver onEach:(id<KotlinSuspendFunction1>)onEach __attribute__((swift_name("subscribe(_:onEach:)"))) __attribute__((unavailable("Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")));
+ (void)subscribe:(id<Flow>)receiver onEach:(id<KotlinSuspendFunction1>)onEach onError:(id<KotlinSuspendFunction1>)onError __attribute__((swift_name("subscribe(_:onEach:onError:)"))) __attribute__((unavailable("Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")));
+ (id<Flow>)subscribeOn:(id<Flow>)receiver context:(id<KotlinCoroutineContext>)context __attribute__((swift_name("subscribeOn(_:context:)"))) __attribute__((unavailable("Use 'flowOn' instead")));
+ (id<Flow>)switchMap:(id<Flow>)receiver transform:(id<KotlinSuspendFunction1>)transform __attribute__((swift_name("switchMap(_:transform:)"))) __attribute__((unavailable("Flow analogues of 'switchMap' are 'transformLatest', 'flatMapLatest' and 'mapLatest'")));
@end

__attribute__((objc_subclassing_restricted))
@interface MonthKt : Base
+ (Month *)MonthNumber:(int32_t)number __attribute__((swift_name("Month(number:)")));
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
@interface MultithreadedDispatchers_commonKt_ : Base

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
@interface MultithreadedDispatchersKt_ : Base
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
@interface MutexKt_ : Base
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
@interface OnTimeoutKt_ : Base

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
@interface PluginExceptionsKt : Base

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (void)throwArrayMissingFieldExceptionSeenArray:(KotlinIntArray *)seenArray goldenMaskArray:(KotlinIntArray *)goldenMaskArray descriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("throwArrayMissingFieldException(seenArray:goldenMaskArray:descriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (void)throwMissingFieldExceptionSeen:(int32_t)seen goldenMask:(int32_t)goldenMask descriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("throwMissingFieldException(seen:goldenMask:descriptor:)")));
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
@interface ProduceKt_ : Base

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
@interface ReduceKt_ : Base

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
@interface RunnableKt_ : Base
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
@interface SelectKt_ : Base

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
@interface SelectUnbiasedKt_ : Base

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
@interface SemaphoreKt_ : Base
+ (id<Semaphore>)SemaphorePermits:(int32_t)permits acquiredPermits:(int32_t)acquiredPermits __attribute__((swift_name("Semaphore(permits:acquiredPermits:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)withPermit:(id<Semaphore>)receiver action:(id _Nullable (^)(void))action completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withPermit(_:action:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerialDescriptorKt : Base
+ (id)elementDescriptors:(id<SerialDescriptor>)receiver __attribute__((swift_name("elementDescriptors(_:)")));
+ (id)elementNames:(id<SerialDescriptor>)receiver __attribute__((swift_name("elementNames(_:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerialDescriptorsKt : Base
+ (id<SerialDescriptor>)nullable:(id<SerialDescriptor>)receiver __attribute__((swift_name("nullable(_:)")));
+ (id<SerialDescriptor>)PrimitiveSerialDescriptorSerialName:(NSString *)serialName kind:(PrimitiveKind *)kind __attribute__((swift_name("PrimitiveSerialDescriptor(serialName:kind:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)SerialDescriptorSerialName:(NSString *)serialName original:(id<SerialDescriptor>)original __attribute__((swift_name("SerialDescriptor(serialName:original:)")));
+ (id<SerialDescriptor>)buildClassSerialDescriptorSerialName:(NSString *)serialName typeParameters:(KotlinArray<id<SerialDescriptor>> *)typeParameters builderAction:(void (^)(ClassSerialDescriptorBuilder *))builderAction __attribute__((swift_name("buildClassSerialDescriptor(serialName:typeParameters:builderAction:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (id<SerialDescriptor>)buildSerialDescriptorSerialName:(NSString *)serialName kind:(SerialKind *)kind typeParameters:(KotlinArray<id<SerialDescriptor>> *)typeParameters builder:(void (^)(ClassSerialDescriptorBuilder *))builder __attribute__((swift_name("buildSerialDescriptor(serialName:kind:typeParameters:builder:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)listSerialDescriptor __attribute__((swift_name("listSerialDescriptor()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)listSerialDescriptorElementDescriptor:(id<SerialDescriptor>)elementDescriptor __attribute__((swift_name("listSerialDescriptor(elementDescriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)mapSerialDescriptor __attribute__((swift_name("mapSerialDescriptor()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)mapSerialDescriptorKeyDescriptor:(id<SerialDescriptor>)keyDescriptor valueDescriptor:(id<SerialDescriptor>)valueDescriptor __attribute__((swift_name("mapSerialDescriptor(keyDescriptor:valueDescriptor:)")));
+ (id<SerialDescriptor>)serialDescriptor __attribute__((swift_name("serialDescriptor()")));
+ (id<SerialDescriptor>)serialDescriptorType:(id<KotlinKType>)type __attribute__((swift_name("serialDescriptor(type:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)setSerialDescriptor __attribute__((swift_name("setSerialDescriptor()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<SerialDescriptor>)setSerialDescriptorElementDescriptor:(id<SerialDescriptor>)elementDescriptor __attribute__((swift_name("setSerialDescriptor(elementDescriptor:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerialFormatKt : Base
+ (id _Nullable)decodeFromByteArray:(id<BinaryFormat>)receiver bytes:(KotlinByteArray *)bytes __attribute__((swift_name("decodeFromByteArray(_:bytes:)")));
+ (id _Nullable)decodeFromHexString:(id<BinaryFormat>)receiver hex:(NSString *)hex __attribute__((swift_name("decodeFromHexString(_:hex:)")));
+ (id _Nullable)decodeFromHexString:(id<BinaryFormat>)receiver deserializer:(id<DeserializationStrategy>)deserializer hex:(NSString *)hex __attribute__((swift_name("decodeFromHexString(_:deserializer:hex:)")));
+ (id _Nullable)decodeFromString:(id<StringFormat>)receiver string:(NSString *)string __attribute__((swift_name("decodeFromString(_:string:)")));
+ (KotlinByteArray *)encodeToByteArray:(id<BinaryFormat>)receiver value:(id _Nullable)value __attribute__((swift_name("encodeToByteArray(_:value:)")));
+ (NSString *)encodeToHexString:(id<BinaryFormat>)receiver value:(id _Nullable)value __attribute__((swift_name("encodeToHexString(_:value:)")));
+ (NSString *)encodeToHexString:(id<BinaryFormat>)receiver serializer:(id<SerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeToHexString(_:serializer:value:)")));
+ (NSString *)encodeToString:(id<StringFormat>)receiver value:(id _Nullable)value __attribute__((swift_name("encodeToString(_:value:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerializersKt : Base
+ (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (id<KSerializer>)serializer:(id<KotlinKClass>)receiver __attribute__((swift_name("serializer(_:)")));
+ (id<KSerializer>)serializerType:(id<KotlinKType>)type __attribute__((swift_name("serializer(type:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
+ (id<KSerializer>)serializerKClass:(id<KotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<KSerializer>> *)typeArgumentsSerializers isNullable:(BOOL)isNullable __attribute__((swift_name("serializer(kClass:typeArgumentsSerializers:isNullable:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
+ (id<KSerializer> _Nullable)serializerOrNull:(id<KotlinKClass>)receiver __attribute__((swift_name("serializerOrNull(_:)")));
+ (id<KSerializer> _Nullable)serializerOrNullType:(id<KotlinKType>)type __attribute__((swift_name("serializerOrNull(type:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerializersModuleKt : Base
@property (class, readonly) SerializersModule *EmptySerializersModule __attribute__((swift_name("EmptySerializersModule"))) __attribute__((deprecated("Deprecated in the favour of 'EmptySerializersModule()'")));
@end

__attribute__((objc_subclassing_restricted))
@interface SerializersModuleBuildersKt : Base
+ (SerializersModule *)EmptySerializersModule __attribute__((swift_name("EmptySerializersModule()")));
+ (SerializersModule *)SerializersModuleBuilderAction:(void (^)(SerializersModuleBuilder *))builderAction __attribute__((swift_name("SerializersModule(builderAction:)")));
+ (SerializersModule *)serializersModuleOfSerializer:(id<KSerializer>)serializer __attribute__((swift_name("serializersModuleOf(serializer:)")));
+ (SerializersModule *)serializersModuleOfKClass:(id<KotlinKClass>)kClass serializer:(id<KSerializer>)serializer __attribute__((swift_name("serializersModuleOf(kClass:serializer:)")));
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
@interface ShareKt_ : Base
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
@interface SharedFlowKt_ : Base
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
@interface StateFlowKt_ : Base
+ (id<MutableStateFlow>)MutableStateFlowValue:(id _Nullable)value __attribute__((swift_name("MutableStateFlow(value:)")));
+ (id _Nullable)getAndUpdate:(id<MutableStateFlow>)receiver function:(id _Nullable (^)(id _Nullable))function __attribute__((swift_name("getAndUpdate(_:function:)")));
+ (void)update:(id<MutableStateFlow>)receiver function:(id _Nullable (^)(id _Nullable))function __attribute__((swift_name("update(_:function:)")));
+ (id _Nullable)updateAndGet:(id<MutableStateFlow>)receiver function:(id _Nullable (^)(id _Nullable))function __attribute__((swift_name("updateAndGet(_:function:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SupervisorKt : Base
+ (id<CompletableJob>)SupervisorJobParent:(id<Job> _Nullable)parent __attribute__((swift_name("SupervisorJob(parent:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)supervisorScopeBlock:(id<KotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("supervisorScope(block:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SupervisorKt_ : Base
+ (id<CompletableJob>)SupervisorJobParent:(id<Job> _Nullable)parent __attribute__((swift_name("SupervisorJob(parent:)")));

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
@interface Synchronized_commonKt_ : Base
+ (id _Nullable)synchronizedLock:(SynchronizedObject *)lock block:(id _Nullable (^)(void))block __attribute__((swift_name("synchronized(lock:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SynchronizedKt : Base
+ (SynchronizedObject *)reentrantLock __attribute__((swift_name("reentrantLock()")));
+ (id _Nullable)synchronizedLock:(SynchronizedObject *)lock block:(id _Nullable (^)(void))block __attribute__((swift_name("synchronized(lock:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SynchronizedKt_ : Base
+ (id _Nullable)synchronizedImplLock:(SynchronizedObject *)lock block:(id _Nullable (^)(void))block __attribute__((swift_name("synchronizedImpl(lock:block:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface SynchronizedKt__ : Base
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
@interface TimeoutKt_ : Base

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
@interface TraceKt : Base
+ (TraceBase *)TraceSize:(int32_t)size format:(TraceFormat *)format __attribute__((swift_name("Trace(size:format:)")));
@property (class, readonly) TraceFormat *traceFormatDefault __attribute__((swift_name("traceFormatDefault")));
@end

__attribute__((objc_subclassing_restricted))
@interface TraceFormatKt : Base
+ (TraceFormat *)TraceFormatFormat:(NSString *(^)(Int *, id))format __attribute__((swift_name("TraceFormat(format:)")));
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
@interface TransformKt_ : Base
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
@interface UnicodeKt : Base

/**
 * @note annotations
 *   kotlinx.datetime.format.FormatStringsInDatetimeFormats
*/
+ (void)byUnicodePattern:(id<DateTimeFormatBuilder>)receiver pattern:(NSString *)pattern __attribute__((swift_name("byUnicodePattern(_:pattern:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface UtcOffsetKt : Base
+ (UtcOffset *)UtcOffset __attribute__((swift_name("UtcOffset()"))) __attribute__((unavailable("Use UtcOffset.ZERO instead")));
+ (UtcOffset *)UtcOffsetHours:(Int * _Nullable)hours minutes:(Int * _Nullable)minutes seconds:(Int * _Nullable)seconds __attribute__((swift_name("UtcOffset(hours:minutes:seconds:)")));
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
@interface WhileSelectKt_ : Base

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
@interface YieldKt_ : Base

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

__attribute__((objc_subclassing_restricted))
@interface ZipKt_ : Base
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

@protocol KotlinKAnnotatedElement
@required
@end

@protocol KotlinKCallable <KotlinKAnnotatedElement>
@required
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) id<KotlinKType> returnType __attribute__((swift_name("returnType")));
@end

@protocol KotlinKProperty <KotlinKCallable>
@required
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
__attribute__((objc_subclassing_restricted))
@interface KotlinAtomicReference<T> : Base
- (instancetype)initWithValue:(T _Nullable)value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
- (T _Nullable)compareAndExchangeExpected:(T _Nullable)expected newValue:(T _Nullable)newValue __attribute__((swift_name("compareAndExchange(expected:newValue:)")));
- (BOOL)compareAndSetExpected:(T _Nullable)expected newValue:(T _Nullable)newValue __attribute__((swift_name("compareAndSet(expected:newValue:)")));
- (T _Nullable)getAndSetNewValue:(T _Nullable)newValue __attribute__((swift_name("getAndSet(newValue:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property T _Nullable value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinEnumCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinEnumCompanion *shared __attribute__((swift_name("shared")));
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

@protocol KotlinSuspendFunction0 <KotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(completionHandler:)")));
@end

@protocol KotlinAppendable
@required
- (id<KotlinAppendable>)appendValue:(unichar)value __attribute__((swift_name("append(value:)")));
- (id<KotlinAppendable>)appendValue_:(id _Nullable)value __attribute__((swift_name("append(value_:)")));
- (id<KotlinAppendable>)appendValue:(id _Nullable)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("append(value:startIndex:endIndex:)")));
@end

@protocol KotlinKDeclarationContainer
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

__attribute__((objc_subclassing_restricted))
@interface KotlinByteArray : Base
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(Byte *(^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int8_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (KotlinByteIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int8_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

@protocol KotlinAnnotation
@required
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinNothing : Base
@end

@protocol KotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

@protocol KotlinMapEntry
@required
@property (readonly) id _Nullable key __attribute__((swift_name("key")));
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
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


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.6")
*/
__attribute__((objc_subclassing_restricted))
@interface KotlinDurationUnit : KotlinEnum<KotlinDurationUnit *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KotlinDurationUnit *nanoseconds __attribute__((swift_name("nanoseconds")));
@property (class, readonly) KotlinDurationUnit *microseconds __attribute__((swift_name("microseconds")));
@property (class, readonly) KotlinDurationUnit *milliseconds __attribute__((swift_name("milliseconds")));
@property (class, readonly) KotlinDurationUnit *seconds __attribute__((swift_name("seconds")));
@property (class, readonly) KotlinDurationUnit *minutes __attribute__((swift_name("minutes")));
@property (class, readonly) KotlinDurationUnit *hours __attribute__((swift_name("hours")));
@property (class, readonly) KotlinDurationUnit *days __attribute__((swift_name("days")));
+ (KotlinArray<KotlinDurationUnit *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KotlinDurationUnit *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
@interface NSDateComponents : Base
@end

@protocol KotlinKType
@required

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
@property (readonly) NSArray<KotlinKTypeProjection *> *arguments __attribute__((swift_name("arguments")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
@property (readonly) id<KotlinKClassifier> _Nullable classifier __attribute__((swift_name("classifier")));
@property (readonly) BOOL isMarkedNullable __attribute__((swift_name("isMarkedNullable")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
@protocol KotlinTimeSource
@required
- (id<KotlinTimeMark>)markNow __attribute__((swift_name("markNow()")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
@protocol KotlinTimeSourceWithComparableMarks <KotlinTimeSource>
@required
@end

@protocol KotlinSuspendFunction2 <KotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 p2:(id _Nullable)p2 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:p2:completionHandler:)")));
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

@protocol KotlinSuspendFunction6 <KotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 p2:(id _Nullable)p2 p3:(id _Nullable)p3 p4:(id _Nullable)p4 p5:(id _Nullable)p5 p6:(id _Nullable)p6 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:p2:p3:p4:p5:p6:completionHandler:)")));
@end

@interface KotlinByteIterator : Base <KotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (Byte *)next __attribute__((swift_name("next()")));
- (int8_t)nextByte __attribute__((swift_name("nextByte()")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((objc_subclassing_restricted))
@interface KotlinKTypeProjection : Base
- (instancetype)initWithVariance:(KotlinKVariance * _Nullable)variance type:(id<KotlinKType> _Nullable)type __attribute__((swift_name("init(variance:type:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) KotlinKTypeProjectionCompanion *companion __attribute__((swift_name("companion")));
- (KotlinKTypeProjection *)doCopyVariance:(KotlinKVariance * _Nullable)variance type:(id<KotlinKType> _Nullable)type __attribute__((swift_name("doCopy(variance:type:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<KotlinKType> _Nullable type __attribute__((swift_name("type")));
@property (readonly) KotlinKVariance * _Nullable variance __attribute__((swift_name("variance")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
@protocol KotlinTimeMark
@required
- (int64_t)elapsedNow __attribute__((swift_name("elapsedNow()")));
- (BOOL)hasNotPassedNow __attribute__((swift_name("hasNotPassedNow()")));
- (BOOL)hasPassedNow __attribute__((swift_name("hasPassedNow()")));
- (id<KotlinTimeMark>)minusDuration:(int64_t)duration __attribute__((swift_name("minus(duration:)")));
- (id<KotlinTimeMark>)plusDuration:(int64_t)duration __attribute__((swift_name("plus(duration:)")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((objc_subclassing_restricted))
@interface KotlinKVariance : KotlinEnum<KotlinKVariance *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KotlinKVariance *invariant __attribute__((swift_name("invariant")));
@property (class, readonly) KotlinKVariance *in __attribute__((swift_name("in")));
@property (class, readonly) KotlinKVariance *out __attribute__((swift_name("out")));
+ (KotlinArray<KotlinKVariance *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KotlinKVariance *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinKTypeProjection.Companion")))
@interface KotlinKTypeProjectionCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinKTypeProjectionCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (KotlinKTypeProjection *)contravariantType:(id<KotlinKType>)type __attribute__((swift_name("contravariant(type:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (KotlinKTypeProjection *)covariantType:(id<KotlinKType>)type __attribute__((swift_name("covariant(type:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (KotlinKTypeProjection *)invariantType:(id<KotlinKType>)type __attribute__((swift_name("invariant(type:)")));
@property (readonly) KotlinKTypeProjection *STAR __attribute__((swift_name("STAR")));
@end

@interface AbstractPolymorphicSerializer (Extensions)

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<DeserializationStrategy>)findPolymorphicSerializerDecoder:(id<CompositeDecoder>)decoder klassName:(NSString * _Nullable)klassName __attribute__((swift_name("findPolymorphicSerializer(decoder:klassName:)")));

/**
 * @note annotations
 *   kotlinx.serialization.InternalSerializationApi
*/
- (id<SerializationStrategy>)findPolymorphicSerializerEncoder:(id<Encoder>)encoder value:(id)value __attribute__((swift_name("findPolymorphicSerializer(encoder:value:)")));
@end

@interface AtomicBoolean (Extensions)
- (BOOL)getAndUpdateFunction:(Boolean *(^)(Boolean *))function __attribute__((swift_name("getAndUpdate(function:)")));
- (void)loopAction:(void (^)(Boolean *))action __attribute__((swift_name("loop(action:)")));
- (void)updateFunction:(Boolean *(^)(Boolean *))function __attribute__((swift_name("update(function:)")));
- (BOOL)updateAndGetFunction:(Boolean *(^)(Boolean *))function __attribute__((swift_name("updateAndGet(function:)")));
@end

@interface AtomicInt (Extensions)
- (int32_t)getAndUpdateFunction:(Int *(^)(Int *))function __attribute__((swift_name("getAndUpdate(function:)")));
- (void)loopAction:(void (^)(Int *))action __attribute__((swift_name("loop(action:)")));
- (void)updateFunction:(Int *(^)(Int *))function __attribute__((swift_name("update(function:)")));
- (int32_t)updateAndGetFunction:(Int *(^)(Int *))function __attribute__((swift_name("updateAndGet(function:)")));
@end

@interface AtomicLong (Extensions)
- (int64_t)getAndUpdateFunction:(Long *(^)(Long *))function __attribute__((swift_name("getAndUpdate(function:)")));
- (void)loopAction:(void (^)(Long *))action __attribute__((swift_name("loop(action:)")));
- (void)updateFunction:(Long *(^)(Long *))function __attribute__((swift_name("update(function:)")));
- (int64_t)updateAndGetFunction:(Long *(^)(Long *))function __attribute__((swift_name("updateAndGet(function:)")));
@end

@interface AtomicRef (Extensions)
- (id _Nullable)getAndUpdateFunction:(id _Nullable (^)(id _Nullable))function __attribute__((swift_name("getAndUpdate(function:)")));
- (void)loopAction:(void (^)(id _Nullable))action __attribute__((swift_name("loop(action:)")));
- (void)updateFunction:(id _Nullable (^)(id _Nullable))function __attribute__((swift_name("update(function:)")));
- (id _Nullable)updateAndGetFunction:(id _Nullable (^)(id _Nullable))function __attribute__((swift_name("updateAndGet(function:)")));
@end

@interface ClassSerialDescriptorBuilder (Extensions)
- (void)elementElementName:(NSString *)elementName annotations:(NSArray<id<KotlinAnnotation>> *)annotations isOptional:(BOOL)isOptional __attribute__((swift_name("element(elementName:annotations:isOptional:)")));
@end

@interface CoroutineDispatcher (Extensions)

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeBlock:(id<KotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(block:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeBlock:(id<KotlinSuspendFunction1>)block completionHandler_:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(block:completionHandler_:)")));
@end

@interface DatePeriod (Extensions)
- (DatePeriod *)plusOther:(DatePeriod *)other __attribute__((swift_name("plus(other:)")));
@end

@interface DateTimeComponentsCompanion (Extensions)
- (DateTimeComponents *)parseInput:(id)input format:(id<DateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
@end

@interface DateTimePeriod (Extensions)
- (DateTimePeriod *)plusOther_:(DateTimePeriod *)other __attribute__((swift_name("plus(other_:)")));
@end

@interface DayOfWeek (Extensions)
@property (readonly) int32_t isoDayNumber __attribute__((swift_name("isoDayNumber")));
@end

@interface Dispatchers (Extensions)
@property (readonly) CoroutineDispatcher *IO __attribute__((swift_name("IO")));
@property (readonly) CoroutineDispatcher *IO_ __attribute__((swift_name("IO_")));
@end

@interface Instant (Extensions)
- (int32_t)daysUntilOther:(Instant *)other timeZone:(TimeZone *)timeZone __attribute__((swift_name("daysUntil(other:timeZone:)")));
- (NSString *)formatFormat:(id<DateTimeFormat>)format offset:(UtcOffset *)offset __attribute__((swift_name("format(format:offset:)")));
- (Instant *)minusUnit:(DateTimeUnitTimeBased *)unit __attribute__((swift_name("minus(unit:)"))) __attribute__((deprecated("Use the minus overload with an explicit number of units")));
- (Instant *)minusValue:(int32_t)value unit:(DateTimeUnitTimeBased *)unit __attribute__((swift_name("minus(value:unit:)")));
- (Instant *)minusValue:(int64_t)value unit_:(DateTimeUnitTimeBased *)unit __attribute__((swift_name("minus(value:unit_:)")));
- (Instant *)minusPeriod:(DateTimePeriod *)period timeZone:(TimeZone *)timeZone __attribute__((swift_name("minus(period:timeZone:)")));
- (Instant *)minusUnit:(DateTimeUnit *)unit timeZone:(TimeZone *)timeZone __attribute__((swift_name("minus(unit:timeZone:)"))) __attribute__((deprecated("Use the minus overload with an explicit number of units")));
- (int64_t)minusOther:(Instant *)other unit:(DateTimeUnitTimeBased *)unit __attribute__((swift_name("minus(other:unit:)")));
- (DateTimePeriod *)minusOther:(Instant *)other timeZone:(TimeZone *)timeZone __attribute__((swift_name("minus(other:timeZone:)")));
- (Instant *)minusValue:(int32_t)value unit:(DateTimeUnit *)unit timeZone:(TimeZone *)timeZone __attribute__((swift_name("minus(value:unit:timeZone:)")));
- (Instant *)minusValue:(int64_t)value unit:(DateTimeUnit *)unit timeZone_:(TimeZone *)timeZone __attribute__((swift_name("minus(value:unit:timeZone_:)")));
- (int64_t)minusOther:(Instant *)other unit:(DateTimeUnit *)unit timeZone:(TimeZone *)timeZone __attribute__((swift_name("minus(other:unit:timeZone:)")));
- (int32_t)monthsUntilOther:(Instant *)other timeZone:(TimeZone *)timeZone __attribute__((swift_name("monthsUntil(other:timeZone:)")));
- (UtcOffset *)offsetInTimeZone:(TimeZone *)timeZone __attribute__((swift_name("offsetIn(timeZone:)")));
- (DateTimePeriod *)periodUntilOther:(Instant *)other timeZone:(TimeZone *)timeZone __attribute__((swift_name("periodUntil(other:timeZone:)")));
- (Instant *)plusUnit:(DateTimeUnitTimeBased *)unit __attribute__((swift_name("plus(unit:)"))) __attribute__((deprecated("Use the plus overload with an explicit number of units")));
- (Instant *)plusValue:(int32_t)value unit:(DateTimeUnitTimeBased *)unit __attribute__((swift_name("plus(value:unit:)")));
- (Instant *)plusValue:(int64_t)value unit_:(DateTimeUnitTimeBased *)unit __attribute__((swift_name("plus(value:unit_:)")));
- (Instant *)plusPeriod:(DateTimePeriod *)period timeZone:(TimeZone *)timeZone __attribute__((swift_name("plus(period:timeZone:)")));
- (Instant *)plusUnit:(DateTimeUnit *)unit timeZone:(TimeZone *)timeZone __attribute__((swift_name("plus(unit:timeZone:)"))) __attribute__((deprecated("Use the plus overload with an explicit number of units")));
- (Instant *)plusValue:(int32_t)value unit:(DateTimeUnit *)unit timeZone:(TimeZone *)timeZone __attribute__((swift_name("plus(value:unit:timeZone:)")));
- (Instant *)plusValue:(int64_t)value unit:(DateTimeUnit *)unit timeZone_:(TimeZone *)timeZone __attribute__((swift_name("plus(value:unit:timeZone_:)")));
- (LocalDateTime *)toLocalDateTimeTimeZone:(TimeZone *)timeZone __attribute__((swift_name("toLocalDateTime(timeZone:)")));
- (NSDate *)toNSDate __attribute__((swift_name("toNSDate()")));
- (int64_t)untilOther:(Instant *)other unit:(DateTimeUnitTimeBased *)unit __attribute__((swift_name("until(other:unit:)")));
- (int64_t)untilOther:(Instant *)other unit:(DateTimeUnit *)unit timeZone:(TimeZone *)timeZone __attribute__((swift_name("until(other:unit:timeZone:)")));
- (int32_t)yearsUntilOther:(Instant *)other timeZone:(TimeZone *)timeZone __attribute__((swift_name("yearsUntil(other:timeZone:)")));
@property (readonly) BOOL isDistantFuture __attribute__((swift_name("isDistantFuture")));
@property (readonly) BOOL isDistantPast __attribute__((swift_name("isDistantPast")));
@end

@interface KotlinArray (Extensions)
- (id<Flow>)asFlow __attribute__((swift_name("asFlow()")));
- (id<Flow>)asFlow_ __attribute__((swift_name("asFlow_()")));
@end

@interface KotlinBooleanCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinByteCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinCharCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinDoubleCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinDurationCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinFloatCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinIntArray (Extensions)
- (id<Flow>)asFlow __attribute__((swift_name("asFlow()")));
- (id<Flow>)asFlow_ __attribute__((swift_name("asFlow_()")));
@end

@interface KotlinIntCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinIntRange (Extensions)
- (id<Flow>)asFlow __attribute__((swift_name("asFlow()")));
- (id<Flow>)asFlow_ __attribute__((swift_name("asFlow_()")));
@end

@interface KotlinLongArray (Extensions)
- (id<Flow>)asFlow __attribute__((swift_name("asFlow()")));
- (id<Flow>)asFlow_ __attribute__((swift_name("asFlow_()")));
@end

@interface KotlinLongCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinLongRange (Extensions)
- (id<Flow>)asFlow __attribute__((swift_name("asFlow()")));
- (id<Flow>)asFlow_ __attribute__((swift_name("asFlow_()")));
@end

@interface KotlinShortCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinStringCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinUByteCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinUIntCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinULongCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinUShortCompanion (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface KotlinUnit (Extensions)
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

@interface LocalDate (Extensions)
- (Instant *)atStartOfDayInTimeZone:(TimeZone *)timeZone __attribute__((swift_name("atStartOfDayIn(timeZone:)")));
- (LocalDateTime *)atTimeTime:(LocalTime *)time __attribute__((swift_name("atTime(time:)")));
- (LocalDateTime *)atTimeHour:(int32_t)hour minute:(int32_t)minute second:(int32_t)second nanosecond:(int32_t)nanosecond __attribute__((swift_name("atTime(hour:minute:second:nanosecond:)")));
- (int32_t)daysUntilOther:(LocalDate *)other __attribute__((swift_name("daysUntil(other:)")));
- (NSString *)formatFormat:(id<DateTimeFormat>)format __attribute__((swift_name("format(format:)")));
- (LocalDate *)minusPeriod:(DatePeriod *)period __attribute__((swift_name("minus(period:)")));
- (LocalDate *)minusUnit:(DateTimeUnitDateBased *)unit __attribute__((swift_name("minus(unit:)"))) __attribute__((deprecated("Use the minus overload with an explicit number of units")));
- (DatePeriod *)minusOther:(LocalDate *)other __attribute__((swift_name("minus(other:)")));
- (LocalDate *)minusValue:(int32_t)value unit:(DateTimeUnitDateBased *)unit __attribute__((swift_name("minus(value:unit:)")));
- (LocalDate *)minusValue:(int64_t)value unit_:(DateTimeUnitDateBased *)unit __attribute__((swift_name("minus(value:unit_:)")));
- (int32_t)monthsUntilOther:(LocalDate *)other __attribute__((swift_name("monthsUntil(other:)")));
- (DatePeriod *)periodUntilOther:(LocalDate *)other __attribute__((swift_name("periodUntil(other:)")));
- (LocalDate *)plusPeriod:(DatePeriod *)period __attribute__((swift_name("plus(period:)")));
- (LocalDate *)plusUnit:(DateTimeUnitDateBased *)unit __attribute__((swift_name("plus(unit:)"))) __attribute__((deprecated("Use the plus overload with an explicit number of units")));
- (LocalDate *)plusValue:(int32_t)value unit:(DateTimeUnitDateBased *)unit __attribute__((swift_name("plus(value:unit:)")));
- (LocalDate *)plusValue:(int64_t)value unit_:(DateTimeUnitDateBased *)unit __attribute__((swift_name("plus(value:unit_:)")));
- (NSDateComponents *)toNSDateComponents __attribute__((swift_name("toNSDateComponents()")));
- (int32_t)untilOther:(LocalDate *)other unit:(DateTimeUnitDateBased *)unit __attribute__((swift_name("until(other:unit:)")));
- (int32_t)yearsUntilOther:(LocalDate *)other __attribute__((swift_name("yearsUntil(other:)")));
@end

@interface LocalDateTime (Extensions)
- (NSString *)formatFormat:(id<DateTimeFormat>)format __attribute__((swift_name("format(format:)")));
- (Instant *)toInstantTimeZone:(TimeZone *)timeZone __attribute__((swift_name("toInstant(timeZone:)")));
- (Instant *)toInstantOffset:(UtcOffset *)offset __attribute__((swift_name("toInstant(offset:)")));
- (NSDateComponents *)toNSDateComponents __attribute__((swift_name("toNSDateComponents()")));
@end

@interface LocalTime (Extensions)
- (LocalDateTime *)atDateDate:(LocalDate *)date __attribute__((swift_name("atDate(date:)")));
- (LocalDateTime *)atDateYear:(int32_t)year monthNumber:(int32_t)monthNumber dayOfMonth:(int32_t)dayOfMonth __attribute__((swift_name("atDate(year:monthNumber:dayOfMonth:)")));
- (LocalDateTime *)atDateYear:(int32_t)year month:(Month *)month dayOfMonth:(int32_t)dayOfMonth __attribute__((swift_name("atDate(year:month:dayOfMonth:)")));
- (NSString *)formatFormat:(id<DateTimeFormat>)format __attribute__((swift_name("format(format:)")));
@end

@interface Month (Extensions)
@property (readonly) int32_t number __attribute__((swift_name("number")));
@end

@interface NSDate (Extensions)
- (Instant *)toKotlinInstant __attribute__((swift_name("toKotlinInstant()")));
@end

@interface NSTimeZone (Extensions)
- (TimeZone *)toKotlinTimeZone __attribute__((swift_name("toKotlinTimeZone()")));
@end

@interface PolymorphicModuleBuilder (Extensions)
- (void)subclassClazz:(id<KotlinKClass>)clazz __attribute__((swift_name("subclass(clazz:)")));
- (void)subclassSerializer:(id<KSerializer>)serializer __attribute__((swift_name("subclass(serializer:)")));
@end

@interface SerializersModule (Extensions)

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SerialDescriptor> _Nullable)getContextualDescriptorDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("getContextualDescriptor(descriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (NSArray<id<SerialDescriptor>> *)getPolymorphicDescriptorsDescriptor:(id<SerialDescriptor>)descriptor __attribute__((swift_name("getPolymorphicDescriptors(descriptor:)")));
- (SerializersModule *)overwriteWithOther:(SerializersModule *)other __attribute__((swift_name("overwriteWith(other:)")));
- (SerializersModule *)plusOther_:(SerializersModule *)other __attribute__((swift_name("plus(other_:)")));
- (id<KSerializer>)serializer __attribute__((swift_name("serializer()")));
- (id<KSerializer>)serializerType:(id<KotlinKType>)type __attribute__((swift_name("serializer(type:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<KSerializer>)serializerKClass:(id<KotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<KSerializer>> *)typeArgumentsSerializers isNullable:(BOOL)isNullable __attribute__((swift_name("serializer(kClass:typeArgumentsSerializers:isNullable:)")));
- (id<KSerializer> _Nullable)serializerOrNullType:(id<KotlinKType>)type __attribute__((swift_name("serializerOrNull(type:)")));
@end

@interface SerializersModuleBuilder (Extensions)
- (void)contextualSerializer:(id<KSerializer>)serializer __attribute__((swift_name("contextual(serializer:)")));
- (void)polymorphicBaseClass:(id<KotlinKClass>)baseClass baseSerializer:(id<KSerializer> _Nullable)baseSerializer builderAction:(void (^)(PolymorphicModuleBuilder<id> *))builderAction __attribute__((swift_name("polymorphic(baseClass:baseSerializer:builderAction:)")));
@end

@interface SharingStartedCompanion (Extensions)
- (id<SharingStarted>)WhileSubscribedStopTimeout:(int64_t)stopTimeout replayExpiration:(int64_t)replayExpiration __attribute__((swift_name("WhileSubscribed(stopTimeout:replayExpiration:)")));
- (id<SharingStarted>)WhileSubscribedStopTimeout:(int64_t)stopTimeout replayExpiration_:(int64_t)replayExpiration __attribute__((swift_name("WhileSubscribed(stopTimeout:replayExpiration_:)")));
@end

@interface SynchronizedObject (Extensions)
- (id _Nullable)withLockBlock:(id _Nullable (^)(void))block __attribute__((swift_name("withLock(block:)")));
@end

@interface TimeZone (Extensions)
- (UtcOffset *)offsetAtInstant:(Instant *)instant __attribute__((swift_name("offsetAt(instant:)")));
- (NSTimeZone *)toNSTimeZone __attribute__((swift_name("toNSTimeZone()")));
@end

@interface TraceBase (Extensions)
- (TraceBase *)namedName:(NSString *)name __attribute__((swift_name("named(name:)")));
@end

@interface UtcOffset (Extensions)
- (FixedOffsetTimeZone *)asTimeZone __attribute__((swift_name("asTimeZone()")));
- (NSString *)formatFormat:(id<DateTimeFormat>)format __attribute__((swift_name("format(format:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
