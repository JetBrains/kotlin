__attribute__((swift_name("KotlinBase")))
@interface KtBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface KtBase (KtBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface KtMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface KtMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorKtKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface KtNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface KtByte : KtNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface KtUByte : KtNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface KtShort : KtNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface KtUShort : KtNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface KtInt : KtNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface KtUInt : KtNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface KtLong : KtNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface KtULong : KtNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface KtFloat : KtNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface KtDouble : KtNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface KtBoolean : KtNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CoroutineException")))
@interface KtCoroutineException : KtKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ContinuationHolder")))
@interface KtContinuationHolder : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)resumeValue:(id _Nullable)value __attribute__((swift_name("resume(value:)")));
- (void)resumeWithExceptionException:(KtKotlinThrowable *)exception __attribute__((swift_name("resumeWithException(exception:)")));
@end

__attribute__((swift_name("SuspendFun")))
@protocol KtSuspendFun
@required

/**
 * @note This method converts instances of CoroutineException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)suspendFunDoYield:(BOOL)doYield doThrow:(BOOL)doThrow completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("suspendFun(doYield:doThrow:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ResultHolder")))
@interface KtResultHolder : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property int32_t completed __attribute__((swift_name("completed")));
@property KtKotlinThrowable * _Nullable exception __attribute__((swift_name("exception")));
@property id _Nullable result __attribute__((swift_name("result")));
@end

__attribute__((swift_name("SuspendBridge")))
@protocol KtSuspendBridge
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)intValue:(id _Nullable)value completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("int(value:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)intAsAnyValue:(id _Nullable)value completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("intAsAny(value:completionHandler:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingValue:(id _Nullable)value completionHandler:(void (^)(KtKotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothing(value:completionHandler:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsAnyValue:(id _Nullable)value completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsAny(value:completionHandler:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsIntValue:(id _Nullable)value completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsInt(value:completionHandler:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsUnitValue:(id _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsUnit(value:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)nullableUnitValue:(id _Nullable)value completionHandler:(void (^)(KtKotlinUnit * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("nullableUnit(value:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)unitValue:(id _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("unit(value:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)unitAsAnyValue:(id _Nullable)value completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("unitAsAny(value:completionHandler:)")));
@end

__attribute__((swift_name("AbstractSuspendBridge")))
@interface KtAbstractSuspendBridge : KtBase <KtSuspendBridge>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)intAsAnyValue:(KtInt *)value completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("intAsAny(value:completionHandler:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsAnyValue:(KtInt *)value completionHandler:(void (^)(KtKotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsAny(value:completionHandler:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsIntValue:(KtInt *)value completionHandler:(void (^)(KtKotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsInt(value:completionHandler:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsUnitValue:(KtInt *)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsUnit(value:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)nullableUnitValue:(KtInt *)value completionHandler:(void (^)(KtKotlinUnit * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("nullableUnit(value:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)unitValue:(KtInt *)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("unit(value:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)unitAsAnyValue:(KtInt *)value completionHandler:(void (^)(KtKotlinUnit * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("unitAsAny(value:completionHandler:)")));
@end

__attribute__((swift_name("ThrowCancellationException")))
@interface KtThrowCancellationException : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ThrowCancellationExceptionImpl")))
@interface KtThrowCancellationExceptionImpl : KtThrowCancellationException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)throwCancellationExceptionWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("throwCancellationException(completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("suspendFunctionChild0")))
@interface KtsuspendFunctionChild0 : KtBase <KtKotlinSuspendFunction0>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeWithCompletionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("suspendFunctionChild1")))
@interface KtsuspendFunctionChild1 : KtBase <KtKotlinSuspendFunction1>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(NSString *)p1 completionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CoroutinesKt")))
@interface KtCoroutinesKt : KtBase

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)callSuspendBridgeBridge:(KtAbstractSuspendBridge *)bridge resultHolder:(KtResultHolder *)resultHolder error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("callSuspendBridge(bridge:resultHolder:)")));
+ (void)callSuspendFunSuspendFun:(id<KtSuspendFun>)suspendFun doYield:(BOOL)doYield doThrow:(BOOL)doThrow resultHolder:(KtResultHolder *)resultHolder __attribute__((swift_name("callSuspendFun(suspendFun:doYield:doThrow:resultHolder:)")));

/**
 * @note This method converts instances of CoroutineException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)callSuspendFun2SuspendFun:(id<KtSuspendFun>)suspendFun doYield:(BOOL)doYield doThrow:(BOOL)doThrow completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("callSuspendFun2(suspendFun:doYield:doThrow:completionHandler:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)createCoroutineUninterceptedAndResumeFn:(id<KtKotlinSuspendFunction0>)fn resultHolder:(KtResultHolder *)resultHolder error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("createCoroutineUninterceptedAndResume(fn:resultHolder:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)createCoroutineUninterceptedAndResumeFn:(id<KtKotlinSuspendFunction1>)fn receiver:(id _Nullable)receiver resultHolder:(KtResultHolder *)resultHolder error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("createCoroutineUninterceptedAndResume(fn:receiver:resultHolder:)")));
+ (void)gc __attribute__((swift_name("gc()")));
+ (id<KtKotlinKSuspendFunction0>)getKSuspendCallableReference0 __attribute__((swift_name("getKSuspendCallableReference0()")));
+ (id<KtKotlinKSuspendFunction1>)getKSuspendCallableReference1 __attribute__((swift_name("getKSuspendCallableReference1()")));
+ (id<KtKotlinSuspendFunction0>)getSuspendCallableReference0 __attribute__((swift_name("getSuspendCallableReference0()")));
+ (id<KtKotlinSuspendFunction1>)getSuspendCallableReference1 __attribute__((swift_name("getSuspendCallableReference1()")));
+ (KtsuspendFunctionChild0 *)getSuspendChild0 __attribute__((swift_name("getSuspendChild0()")));
+ (KtsuspendFunctionChild1 *)getSuspendChild1 __attribute__((swift_name("getSuspendChild1()")));
+ (id<KtKotlinSuspendFunction0>)getSuspendLambda0 __attribute__((swift_name("getSuspendLambda0()")));
+ (id<KtKotlinSuspendFunction1>)getSuspendLambda1 __attribute__((swift_name("getSuspendLambda1()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)invoke1Block:(id<KtKotlinSuspendFunction1>)block argument:(id _Nullable)argument completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke1(block:argument:completionHandler:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (id _Nullable)startCoroutineUninterceptedOrReturnFn:(id<KtKotlinSuspendFunction0>)fn resultHolder:(KtResultHolder *)resultHolder error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("startCoroutineUninterceptedOrReturn(fn:resultHolder:)"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (id _Nullable)startCoroutineUninterceptedOrReturnFn:(id<KtKotlinSuspendFunction1>)fn receiver:(id _Nullable)receiver resultHolder:(KtResultHolder *)resultHolder error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("startCoroutineUninterceptedOrReturn(fn:receiver:resultHolder:)"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (id _Nullable)startCoroutineUninterceptedOrReturnFn:(id<KtKotlinSuspendFunction2>)fn receiver:(id _Nullable)receiver param:(id _Nullable)param resultHolder:(KtResultHolder *)resultHolder error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("startCoroutineUninterceptedOrReturn(fn:receiver:param:resultHolder:)"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)suspendFunWithCompletionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("suspendFun(completionHandler:)")));

/**
 * @note This method converts instances of CoroutineException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)suspendFunResult:(id _Nullable)result doSuspend:(BOOL)doSuspend doThrow:(BOOL)doThrow completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("suspendFun(result:doSuspend:doThrow:completionHandler:)")));

/**
 * @note This method converts instances of CoroutineException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)suspendFunAsyncResult:(id _Nullable)result continuationHolder:(KtContinuationHolder *)continuationHolder completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("suspendFunAsync(result:continuationHolder:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)throwCancellationExceptionWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("throwCancellationException(completionHandler:)")));

/**
 * @note This method converts instances of CoroutineException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (BOOL)throwExceptionException:(KtKotlinThrowable *)exception error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("throwException(exception:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)unitSuspendFunWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("unitSuspendFun(completionHandler:)")));

/**
 * @note This method converts instances of CoroutineException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)unitSuspendFunDoSuspend:(BOOL)doSuspend doThrow:(BOOL)doThrow completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("unitSuspendFun(doSuspend:doThrow:completionHandler:)")));

/**
 * @note This method converts instances of CoroutineException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)unitSuspendFunAsyncContinuationHolder:(KtContinuationHolder *)continuationHolder completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("unitSuspendFunAsync(continuationHolder:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DataClassWithExplicitComponentMethod")))
@interface KtDataClassWithExplicitComponentMethod : KtBase
- (instancetype)initWithX:(int32_t)x y:(int32_t)y __attribute__((swift_name("init(x:y:)"))) __attribute__((objc_designated_initializer));
- (int32_t)component1Arg:(int32_t)arg __attribute__((swift_name("component1(arg:)")));
- (KtDataClassWithExplicitComponentMethod *)doCopyX:(int32_t)x y:(int32_t)y __attribute__((swift_name("doCopy(x:y:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t x __attribute__((swift_name("x")));
@property (readonly) int32_t y __attribute__((swift_name("y")));
@end

__attribute__((swift_name("ComponentInterface")))
@protocol KtComponentInterface
@required
- (int32_t)component1 __attribute__((swift_name("component1()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DataClassWithInheritedComponentMethod")))
@interface KtDataClassWithInheritedComponentMethod : KtBase <KtComponentInterface>
- (instancetype)initWithX:(int32_t)x __attribute__((swift_name("init(x:)"))) __attribute__((objc_designated_initializer));
- (int32_t)component1 __attribute__((swift_name("component1()")));
- (KtDataClassWithInheritedComponentMethod *)doCopyX:(int32_t)x __attribute__((swift_name("doCopy(x:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t x __attribute__((swift_name("x")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RegularClassWithComponentMethods")))
@interface KtRegularClassWithComponentMethods : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)component1 __attribute__((swift_name("component1()")));
- (int32_t)component3 __attribute__((swift_name("component3()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DataClassWithStrangeNames")))
@interface KtDataClassWithStrangeNames : KtBase
- (instancetype)initWithComponent124:(int32_t)component124 componentABC:(int32_t)componentABC __attribute__((swift_name("init(component124:componentABC:)"))) __attribute__((objc_designated_initializer));
- (int32_t)component16 __attribute__((swift_name("component16()")));
- (KtDataClassWithStrangeNames *)doCopyComponent124:(int32_t)component124 componentABC:(int32_t)componentABC __attribute__((swift_name("doCopy(component124:componentABC:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t component124 __attribute__((swift_name("component124")));
@property (readonly) int32_t componentABC __attribute__((swift_name("componentABC")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DataClassComponentMethodsKt")))
@interface KtDataClassComponentMethodsKt : KtBase
+ (int32_t)component1 __attribute__((swift_name("component1()")));
+ (int32_t)component4 __attribute__((swift_name("component4()")));
@end

__attribute__((swift_name("DeallocRetainBase")))
@interface KtDeallocRetainBase : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DeallocRetainKt")))
@interface KtDeallocRetainKt : KtBase
+ (void)assertNullValue:(id _Nullable)value __attribute__((swift_name("assertNull(value:)")));
+ (KtKotlinWeakReference *)createWeakReferenceValue:(id)value __attribute__((swift_name("createWeakReference(value:)")));
+ (void)garbageCollect __attribute__((swift_name("garbageCollect()")));
+ (BOOL)isExperimentalMM __attribute__((swift_name("isExperimentalMM()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EnumLeftRightUpDown")))
@interface KtEnumLeftRightUpDown : KtKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtEnumLeftRightUpDown *left __attribute__((swift_name("left")));
@property (class, readonly) KtEnumLeftRightUpDown *right __attribute__((swift_name("right")));
@property (class, readonly) KtEnumLeftRightUpDown *up __attribute__((swift_name("up")));
@property (class, readonly) KtEnumLeftRightUpDown *down __attribute__((swift_name("down")));
+ (KtKotlinArray *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KtEnumLeftRightUpDown *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EnumOneTwoThreeValues")))
@interface KtEnumOneTwoThreeValues : KtKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtEnumOneTwoThreeValues *one __attribute__((swift_name("one")));
@property (class, readonly) KtEnumOneTwoThreeValues *two __attribute__((swift_name("two")));
@property (class, readonly) KtEnumOneTwoThreeValues *three __attribute__((swift_name("three")));
@property (class, readonly) KtEnumOneTwoThreeValues *values __attribute__((swift_name("values")));
@property (class, readonly) KtEnumOneTwoThreeValues *entries __attribute__((swift_name("entries")));
+ (KtKotlinArray *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KtEnumOneTwoThreeValues *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EnumValuesValues_")))
@interface KtEnumValuesValues_ : KtKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtEnumValuesValues_ *values __attribute__((swift_name("values")));
@property (class, readonly) KtEnumValuesValues_ *values __attribute__((swift_name("values")));
@property (class, readonly) KtEnumValuesValues_ *entries __attribute__((swift_name("entries")));
@property (class, readonly) KtEnumValuesValues_ *entries __attribute__((swift_name("entries")));
+ (KtKotlinArray *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KtEnumValuesValues_ *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EmptyEnum")))
@interface KtEmptyEnum : KtKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (KtKotlinArray *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KtEmptyEnum *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EnumValuesKt")))
@interface KtEnumValuesKt : KtBase
+ (KtNoEnumEntriesEnum *)dceAvoidance __attribute__((swift_name("dceAvoidance()")));
@end

__attribute__((swift_name("FunInterface")))
@protocol KtFunInterface
@required
- (int32_t)run __attribute__((swift_name("run()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FunInterfacesKt")))
@interface KtFunInterfacesKt : KtBase
+ (id<KtFunInterface>)getLambda __attribute__((swift_name("getLambda()")));
+ (id<KtFunInterface>)getObject __attribute__((swift_name("getObject()")));
@end

__attribute__((swift_name("FHolder")))
@interface KtFHolder : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("F2Holder")))
@interface KtF2Holder : KtFHolder
- (instancetype)initWithValue:(id _Nullable (^)(id _Nullable, id _Nullable))value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (readonly) id _Nullable (^value)(id _Nullable, id _Nullable) __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("F32Holder")))
@interface KtF32Holder : KtFHolder
- (instancetype)initWithValue:(id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (readonly) id _Nullable (^value)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable) __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("F33Holder")))
@interface KtF33Holder : KtFHolder
- (instancetype)initWithValue:(id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (readonly) id<KtKotlinFunction33> value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FunctionalTypesKt")))
@interface KtFunctionalTypesKt : KtBase
+ (void)callDynType2List:(NSArray<id _Nullable (^)(id _Nullable, id _Nullable)> *)list param:(id _Nullable)param __attribute__((swift_name("callDynType2(list:param:)")));
+ (void)callDynType32List:(NSArray<id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable)> *)list param:(id _Nullable)param __attribute__((swift_name("callDynType32(list:param:)")));
+ (void)callDynType33List:(NSArray<id<KtKotlinFunction33>> *)list param:(id _Nullable)param __attribute__((swift_name("callDynType33(list:param:)")));
+ (void)callStaticType2Fct:(id _Nullable (^)(id _Nullable, id _Nullable))fct param:(id _Nullable)param __attribute__((swift_name("callStaticType2(fct:param:)")));
+ (void)callStaticType32Fct:(id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))fct param:(id _Nullable)param __attribute__((swift_name("callStaticType32(fct:param:)")));
+ (void)callStaticType33Fct:(id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))fct param:(id _Nullable)param __attribute__((swift_name("callStaticType33(fct:param:)")));
+ (KtF32Holder *)getDynType32 __attribute__((swift_name("getDynType32()")));
+ (KtF2Holder *)getDynTypeLambda2 __attribute__((swift_name("getDynTypeLambda2()")));
+ (KtF33Holder *)getDynTypeLambda33 __attribute__((swift_name("getDynTypeLambda33()")));
+ (KtF2Holder *)getDynTypeRef2 __attribute__((swift_name("getDynTypeRef2()")));
+ (KtF33Holder *)getDynTypeRef33 __attribute__((swift_name("getDynTypeRef33()")));
+ (id _Nullable (^)(id _Nullable, id _Nullable))getStaticLambda2 __attribute__((swift_name("getStaticLambda2()")));
+ (id _Nullable (^)(id _Nullable, id _Nullable))getStaticRef2 __attribute__((swift_name("getStaticRef2()")));
+ (id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))getStaticType32 __attribute__((swift_name("getStaticType32()")));
+ (id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))getStaticTypeLambda33 __attribute__((swift_name("getStaticTypeLambda33()")));
+ (id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))getStaticTypeRef33 __attribute__((swift_name("getStaticTypeRef33()")));
@end

__attribute__((swift_name("GH4002ArgumentBase")))
@interface KtGH4002ArgumentBase : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH4002Argument")))
@interface KtGH4002Argument : KtGH4002ArgumentBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestIncompatiblePropertyTypeWarning")))
@interface KtTestIncompatiblePropertyTypeWarning : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestIncompatiblePropertyTypeWarning.Generic")))
@interface KtTestIncompatiblePropertyTypeWarningGeneric : KtBase
- (instancetype)initWithValue:(id _Nullable)value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end

__attribute__((swift_name("TestIncompatiblePropertyTypeWarningInterfaceWithGenericProperty")))
@protocol KtTestIncompatiblePropertyTypeWarningInterfaceWithGenericProperty
@required
@property (readonly) KtTestIncompatiblePropertyTypeWarningGeneric *p __attribute__((swift_name("p")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestIncompatiblePropertyTypeWarning.ClassOverridingInterfaceWithGenericProperty")))
@interface KtTestIncompatiblePropertyTypeWarningClassOverridingInterfaceWithGenericProperty : KtBase <KtTestIncompatiblePropertyTypeWarningInterfaceWithGenericProperty>
- (instancetype)initWithP:(KtTestIncompatiblePropertyTypeWarningGeneric *)p __attribute__((swift_name("init(p:)"))) __attribute__((objc_designated_initializer));
@property (readonly) KtTestIncompatiblePropertyTypeWarningGeneric *p __attribute__((swift_name("p")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestGH3992")))
@interface KtTestGH3992 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("TestGH3992.C")))
@interface KtTestGH3992C : KtBase
- (instancetype)initWithA:(KtTestGH3992A *)a __attribute__((swift_name("init(a:)"))) __attribute__((objc_designated_initializer));
@property (readonly) KtTestGH3992A *a __attribute__((swift_name("a")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestGH3992.D")))
@interface KtTestGH3992D : KtTestGH3992C
- (instancetype)initWithA:(KtTestGH3992B *)a __attribute__((swift_name("init(a:)"))) __attribute__((objc_designated_initializer));
@property (readonly) KtTestGH3992B *a __attribute__((swift_name("a")));
@end

__attribute__((swift_name("TestGH3992.A")))
@interface KtTestGH3992A : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestGH3992.B")))
@interface KtTestGH3992B : KtTestGH3992A
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ClassNotAvailableInSwift")))
@interface KtClassNotAvailableInSwift : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParentClass")))
@interface KtParentClass : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParentClass.NestedClass")))
@interface KtParentClassNestedClass : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParentClass.NestedClassDeeplyNestedClass")))
@interface KtParentClassNestedClassDeeplyNestedClass : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParentClass.InnerClass")))
@interface KtParentClassInnerClass : NSObject
@end

__attribute__((swift_name("InterfaceNotAvailableInSwift")))
@protocol KtInterfaceNotAvailableInSwift
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UnavailableEnum")))
@interface KtUnavailableEnum : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UnavailableObject")))
@interface KtUnavailableObject : NSObject
@end

__attribute__((swift_name("SealedClass")))
@interface KtSealedClass : KtBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SealedClass.A")))
@interface KtSealedClassA : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SealedClass.B")))
@interface KtSealedClassB : KtSealedClass
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SealedClass.C")))
@interface KtSealedClassC : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WrapperOverUnavailable")))
@interface KtWrapperOverUnavailable : KtBase
- (instancetype)initWithArg:(id)arg __attribute__((swift_name("init(arg:)"))) __attribute__((objc_designated_initializer));
@property (readonly) id arg __attribute__((swift_name("arg")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HiddenfromobjcKt")))
@interface KtHiddenfromobjcKt : KtBase
+ (NSString *)consumeUnavailableParam:(id)param __attribute__((swift_name("consumeUnavailable(param:)")));
+ (KtSealedClass *)createSealedClass __attribute__((swift_name("createSealedClass()")));
+ (id)createUnavailableEnum __attribute__((swift_name("createUnavailableEnum()")));
+ (id)createUnavailableInterface __attribute__((swift_name("createUnavailableInterface()")));
+ (id)doSomethingMeaningless:(NSString *)receiver another:(id)another __attribute__((swift_name("doSomethingMeaningless(_:another:)")));
+ (id)getUnavailableObject __attribute__((swift_name("getUnavailableObject()")));
+ (id)produceUnavailable __attribute__((swift_name("produceUnavailable()")));
+ (id _Nullable)useOfNullableUnavailableClassParam:(id _Nullable)param __attribute__((swift_name("useOfNullableUnavailableClass(param:)")));
+ (NSString *)useOfNullableUnavailableEnumParam:(id _Nullable)param __attribute__((swift_name("useOfNullableUnavailableEnum(param:)")));
+ (NSString * _Nullable)useOfNullableUnavailableInterfaceParam:(id _Nullable)param __attribute__((swift_name("useOfNullableUnavailableInterface(param:)")));
+ (NSString * _Nullable)useOfNullableUnavailableObjectParam:(id _Nullable)param __attribute__((swift_name("useOfNullableUnavailableObject(param:)")));
+ (id)useOfUnavailableClassParam:(id)param __attribute__((swift_name("useOfUnavailableClass(param:)")));
+ (NSString *)useOfUnavailableEnumParam:(id)param __attribute__((swift_name("useOfUnavailableEnum(param:)")));
+ (NSString *)useOfUnavailableObjectParam:(id)param __attribute__((swift_name("useOfUnavailableObject(param:)")));
+ (NSString *)useSealedClassParam:(KtSealedClass *)param __attribute__((swift_name("useSealedClass(param:)")));
+ (NSString *)useUnavailableA:(id)a __attribute__((swift_name("useUnavailable(a:)")));
@end

__attribute__((swift_name("InterfaceNameManglingI1")))
@protocol KtInterfaceNameManglingI1
@required
- (int32_t)clashingMethod __attribute__((swift_name("clashingMethod()")));
- (int32_t)interfaceClashingMethodWithObjCNameInBoth __attribute__((swift_name("interfaceClashingMethodWithObjCNameInBoth()")));
- (int32_t)interfaceClashingMethodWithObjCNameInI1 __attribute__((swift_name("interfaceClashingMethodWithObjCNameInI1()")));
- (int32_t)interfaceClashingMethodWithObjCNameInI2 __attribute__((swift_name("interfaceClashingMethodWithObjCNameInI2()")));
@property (readonly) int32_t clashingProperty __attribute__((swift_name("clashingProperty")));
@end

__attribute__((swift_name("InterfaceNameManglingI2")))
@protocol KtInterfaceNameManglingI2
@required
- (id)clashingMethod __attribute__((swift_name("clashingMethod()")));
- (id)interfaceClashingMethodWithObjCNameInBoth __attribute__((swift_name("interfaceClashingMethodWithObjCNameInBoth()")));
- (id)interfaceClashingMethodWithObjCNameInI2 __attribute__((swift_name("interfaceClashingMethodWithObjCNameInI2()")));
- (id)interfaceClashingMethodWithObjCNameInI1 __attribute__((swift_name("interfaceClashingMethodWithObjCNameInI1()")));
@property (readonly) id clashingProperty __attribute__((swift_name("clashingProperty")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InterfaceNameManglingC1")))
@interface KtInterfaceNameManglingC1 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)clashingMethod __attribute__((swift_name("clashingMethod()")));
@property (readonly) NSString *clashingProperty __attribute__((swift_name("clashingProperty")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InterfaceNameManglingC2")))
@interface KtInterfaceNameManglingC2 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)clashingMethod __attribute__((swift_name("clashingMethod()")));
@property (readonly) int32_t clashingProperty __attribute__((swift_name("clashingProperty")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InterfaceMethodNameManglingKt")))
@interface KtInterfaceMethodNameManglingKt : KtBase
+ (id<KtInterfaceNameManglingI1>)i1 __attribute__((swift_name("i1()")));
+ (id<KtInterfaceNameManglingI2>)i2 __attribute__((swift_name("i2()")));
+ (KtInterfaceNameManglingC1 *)o1 __attribute__((swift_name("o1()")));
+ (KtInterfaceNameManglingC2 *)o2 __attribute__((swift_name("o2()")));
@end


/**
 * Summary class [KDocExport].
 *
 * @property xyzzy Doc for property xyzzy
 * @property zzz See below.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KDocExport")))
@interface KtKDocExport : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/** Non-primary ctor KDoc: */
- (instancetype)initWithName:(NSString *)name __attribute__((swift_name("init(name:)"))) __attribute__((objc_designated_initializer));

/** @property xyzzy KDoc for foo? */
@property (readonly) NSString *foo __attribute__((swift_name("foo")));

/**
 * @param xyzzy is documented.
 *
 * This is multi-line KDoc. See a blank line above.
 */
@property (readonly) NSString *xyzzy __attribute__((swift_name("xyzzy")));

/** @property foo KDoc for yxxyz? */
@property int32_t yxxyz __attribute__((swift_name("yxxyz")));
@end

__attribute__((swift_name("SomeClassWithProperty")))
@interface KtSomeClassWithProperty : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * Returns dispatcher that executes coroutines immediately when it is already in the right context
 * (e.g. current looper is the same as this handler's looper) without an additional [re-dispatch][CoroutineDispatcher.dispatch].
 *
 * Immediate dispatcher is safe from stack overflows and in case of nested invocations forms event-loop similar to [Dispatchers.Unconfined].
 * The event loop is an advanced topic and its implications can be found in [Dispatchers.Unconfined] documentation.
 * The formed event-loop is shared with [Unconfined] and other immediate dispatchers, potentially overlapping tasks between them.
 *
 * Example of usage:
 * ```
 * suspend fun updateUiElement(val text: String) {
 *   **
 *    * If it is known that updateUiElement can be invoked both from the Main thread and from other threads,
 *    * `immediate` dispatcher is used as a performance optimization to avoid unnecessary dispatch.
 *    *
 *    * In that case, when `updateUiElement` is invoked from the Main thread, `uiElement.text` will be
 *    * invoked immediately without any dispatching, otherwise, the `Dispatchers.Main` dispatch cycle will be triggered.
 *    **
 *   withContext(Dispatchers.Main.immediate) {
 *     uiElement.text = text
 *   }
 *   // Do context-independent logic such as logging
 * }
 * ```
 *
 * Method may throw [UnsupportedOperationException] if immediate dispatching is not supported by current dispatcher,
 * please refer to specific dispatcher documentation.
 *
 * [Dispatchers.Main] supports immediate execution for Android, JavaFx and Swing platforms.
 */
@property (readonly) KtSomeClassWithProperty *heavyFormattedKDocFoo __attribute__((swift_name("heavyFormattedKDocFoo")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KdocExportKt")))
@interface KtKdocExportKt : KtBase

/**
 * Useless function [whatever]
 *
 * This kdoc has some additional formatting.
 * @param a keep intact and return
 * @return value of [a]
 * Check for additional comment (note) below
 *
 * @note This method converts instances of IllegalArgumentException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (NSString * _Nullable)whateverA:(NSString *)a error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("whatever(a:)")));
@end

__attribute__((swift_name("KotlinPrivateOverrideI1")))
@protocol KtKotlinPrivateOverrideI1
@required
- (int32_t)i123AbstractMethod __attribute__((swift_name("i123AbstractMethod()")));
- (int32_t)i1OpenMethod __attribute__((swift_name("i1OpenMethod()")));
@end

__attribute__((swift_name("KotlinPrivateOverrideI2")))
@protocol KtKotlinPrivateOverrideI2
@required
- (int32_t)i123AbstractMethod __attribute__((swift_name("i123AbstractMethod()")));
- (int32_t)i234AbstractMethod __attribute__((swift_name("i234AbstractMethod()")));
- (int32_t)i2AbstractMethod __attribute__((swift_name("i2AbstractMethod()")));
@end

__attribute__((swift_name("KotlinPrivateOverrideA1")))
@interface KtKotlinPrivateOverrideA1 : KtBase <KtKotlinPrivateOverrideI1, KtKotlinPrivateOverrideI2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)a1AbstractMethod __attribute__((swift_name("a1AbstractMethod()")));
- (int32_t)a1OpenMethod __attribute__((swift_name("a1OpenMethod()")));
@end

__attribute__((swift_name("KotlinPrivateOverrideI3")))
@protocol KtKotlinPrivateOverrideI3
@required
- (int32_t)i123AbstractMethod __attribute__((swift_name("i123AbstractMethod()")));
- (int32_t)i234AbstractMethod __attribute__((swift_name("i234AbstractMethod()")));
- (int32_t)i3AbstractMethod __attribute__((swift_name("i3AbstractMethod()")));
@end

__attribute__((swift_name("KotlinPrivateOverrideI4")))
@protocol KtKotlinPrivateOverrideI4
@required
- (int32_t)i234AbstractMethod __attribute__((swift_name("i234AbstractMethod()")));
- (int32_t)i4AbstractMethod __attribute__((swift_name("i4AbstractMethod()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinPrivateOverrideKt")))
@interface KtKotlinPrivateOverrideKt : KtBase
+ (id)createP1 __attribute__((swift_name("createP1()")));
+ (id)createP12 __attribute__((swift_name("createP12()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt35940Kt")))
@interface KtKt35940Kt : KtBase
+ (NSString *)testKt35940 __attribute__((swift_name("testKt35940()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT38641")))
@interface KtKT38641 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT38641.IntType")))
@interface KtKT38641IntType : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (getter=description, setter=setDescription:) int32_t description_ __attribute__((swift_name("description_")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT38641.Val")))
@interface KtKT38641Val : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly, getter=description) NSString *description_ __attribute__((swift_name("description_")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT38641.Var")))
@interface KtKT38641Var : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (getter=description, setter=setDescription:) NSString *description_ __attribute__((swift_name("description_")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT38641.TwoProperties")))
@interface KtKT38641TwoProperties : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly, getter=description) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@end

__attribute__((swift_name("KT38641.OverrideVal")))
@interface KtKT38641OverrideVal : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly, getter=description) NSString *description_ __attribute__((swift_name("description_")));
@end

__attribute__((swift_name("KT38641OverrideVar")))
@protocol KtKT38641OverrideVar
@required
@property (getter=description, setter=setDescription:) NSString *description_ __attribute__((swift_name("description_")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt38641Kt")))
@interface KtKt38641Kt : KtBase
+ (NSString *)getOverrideValDescriptionImpl:(KtKT38641OverrideVal *)impl __attribute__((swift_name("getOverrideValDescription(impl:)")));
+ (NSString *)getOverrideVarDescriptionImpl:(id<KtKT38641OverrideVar>)impl __attribute__((swift_name("getOverrideVarDescription(impl:)")));
+ (void)setOverrideVarDescriptionImpl:(id<KtKT38641OverrideVar>)impl newValue:(NSString *)newValue __attribute__((swift_name("setOverrideVarDescription(impl:newValue:)")));
@end

__attribute__((swift_name("JsonConfiguration")))
@interface KtJsonConfiguration : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("This class is deprecated for removal during serialization 1.0 API stabilization.\nFor configuring Json instances, the corresponding builder function can be used instead, e.g. instead of'Json(JsonConfiguration.Stable.copy(isLenient = true))' 'Json { isLenient = true }' should be used.\nInstead of storing JsonConfiguration instances of the code, Json instances can be used directly:'Json(MyJsonConfiguration.copy(prettyPrint = true))' can be replaced with 'Json(from = MyApplicationJson) { prettyPrint = true }'")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MoreTrickyChars")))
@interface KtMoreTrickyChars : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("'\"\\@$(){}\r\n")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt39206Kt")))
@interface KtKt39206Kt : KtBase
+ (int32_t)myFunc __attribute__((swift_name("myFunc()"))) __attribute__((deprecated("Don't call this\nPlease")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ckt41907")))
@interface KtCkt41907 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("Ikt41907")))
@protocol KtIkt41907
@required
- (void)fooC:(KtCkt41907 *)c __attribute__((swift_name("foo(c:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt41907Kt")))
@interface KtKt41907Kt : KtBase
+ (void)escapeCC:(KtCkt41907 *)c __attribute__((swift_name("escapeC(c:)")));
+ (void)testKt41907O:(id<KtIkt41907>)o __attribute__((swift_name("testKt41907(o:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT43599")))
@interface KtKT43599 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) NSString *memberProperty __attribute__((swift_name("memberProperty")));
@end

@interface KtKT43599 (Kt43599Kt)
@property (readonly) NSString *extensionProperty __attribute__((swift_name("extensionProperty")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt43599Kt")))
@interface KtKt43599Kt : KtBase
+ (void)setTopLevelLateinitPropertyValue:(NSString *)value __attribute__((swift_name("setTopLevelLateinitProperty(value:)")));
@property (class, readonly) NSString *topLevelLateinitProperty __attribute__((swift_name("topLevelLateinitProperty")));
@property (class, readonly) NSString *topLevelProperty __attribute__((swift_name("topLevelProperty")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT43780TestObject")))
@interface KtKT43780TestObject : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)kT43780TestObject __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtKT43780TestObject *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *Shared __attribute__((swift_name("Shared")));
@property (readonly) NSString *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t x __attribute__((swift_name("x")));
@property (readonly) int32_t y __attribute__((swift_name("y")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT43780TestClassWithCompanion")))
@interface KtKT43780TestClassWithCompanion : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) KtKT43780TestClassWithCompanionCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT43780TestClassWithCompanion.Companion")))
@interface KtKT43780TestClassWithCompanionCompanion : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtKT43780TestClassWithCompanionCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t z __attribute__((swift_name("z")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Shared")))
@interface KtShared : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)shared __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared_) KtShared *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t x __attribute__((swift_name("x")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Companion")))
@interface KtCompanion : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) KtCompanionCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) int32_t t __attribute__((swift_name("t")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Companion.Companion")))
@interface KtCompanionCompanion : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtCompanionCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t x __attribute__((swift_name("x")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT43780Enum")))
@interface KtKT43780Enum : KtKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtKT43780Enum *otherEntry __attribute__((swift_name("otherEntry")));
@property (class, readonly) KtKT43780Enum *companion __attribute__((swift_name("companion")));
+ (KtKotlinArray *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KtKT43780Enum *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT43780Enum.Companion")))
@interface KtKT43780EnumCompanion : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtKT43780EnumCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t x __attribute__((swift_name("x")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ClassWithInternalCompanion")))
@interface KtClassWithInternalCompanion : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t y __attribute__((swift_name("y")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ClassWithPrivateCompanion")))
@interface KtClassWithPrivateCompanion : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t y __attribute__((swift_name("y")));
@end

__attribute__((swift_name("Host")))
@protocol KtHost
@required
@property (readonly) NSString *test __attribute__((swift_name("test")));
@end

__attribute__((swift_name("AbstractHost")))
@interface KtAbstractHost : KtBase <KtHost>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt46431Kt")))
@interface KtKt46431Kt : KtBase
+ (id<KtHost>)createAbstractHost __attribute__((swift_name("createAbstractHost()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT49937")))
@interface KtKT49937 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)description __attribute__((swift_name("description()")));
@end


/**
 * @note annotations
 *   Foo
 *   BugReport(assignedTo="me", status="open")
*/
__attribute__((swift_name("MyInterface")))
@protocol KtMyInterface
@required
@end


/**
 * @note annotations
 *   Foo
 *   BugReport(assignedTo="me", status="open")
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Bar")))
@interface KtBar : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * My method
 *   @param nodocParam is one arg
 *  @param fooParam is second arg
 * @param fooParam annotations Foo BugReport(assignedTo="me", status="fixed")
 * @return their sum
 *
 * @note annotations
 *   Foo
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)bazNodocParam:(int32_t)nodocParam fooParam:(int32_t)fooParam completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("baz(nodocParam:fooParam:completionHandler:)"))) __attribute__((deprecated("warning")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) NSString *farewell __attribute__((swift_name("farewell")));

/** My property
 ***
 *
 *
 * @note annotations
 *   Foo
 *   BugReport(assignedTo="me", status="open")
*/
@property (readonly) NSString *greeting __attribute__((swift_name("greeting")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT54119KotlinKey")))
@interface KtKT54119KotlinKey : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt54119Kt")))
@interface KtKt54119Kt : KtBase
+ (BOOL)callContainsSet:(NSSet<id> *)set __attribute__((swift_name("callContains(set:)")));
+ (BOOL)callContainsEntryMap:(NSDictionary<id, id> *)map __attribute__((swift_name("callContainsEntry(map:)")));
+ (BOOL)callContainsKeyMap:(NSDictionary<id, id> *)map __attribute__((swift_name("callContainsKey(map:)")));
+ (BOOL)callContainsValueMap:(NSDictionary<id, id> *)map __attribute__((swift_name("callContainsValue(map:)")));
+ (id _Nullable)callGetMap:(NSDictionary<id, id> *)map __attribute__((swift_name("callGet(map:)")));
+ (id _Nullable)callGetElementSet:(NSSet<id> *)set __attribute__((swift_name("callGetElement(set:)")));
+ (int32_t)callGetOrThrowConcurrentModificationMap:(NSDictionary<id, id> *)map __attribute__((swift_name("callGetOrThrowConcurrentModification(map:)")));
@end

@interface KtKotlinSequenceScope (Kt55736Kt)

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)fillWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("fill(completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt55736Kt")))
@interface KtKt55736Kt : KtBase
+ (NSArray<KtInt *> *)callbackBlock:(id<KtKotlinSuspendFunction1>)block __attribute__((swift_name("callback(block:)")));
+ (id<KtKotlinKSuspendFunction1>)getFillFunction __attribute__((swift_name("getFillFunction()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt56521")))
@interface KtKt56521 : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)kt56521 __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtKt56521 *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt56521Kt")))
@interface KtKt56521Kt : KtBase
+ (KtKt56521 *)getKt56521 __attribute__((swift_name("getKt56521()")));
@property (class) int32_t initialized __attribute__((swift_name("initialized")));
@end

__attribute__((swift_name("IKt57373")))
@protocol KtIKt57373
@required
@property (readonly) int32_t bar __attribute__((swift_name("bar")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DKt57373")))
@interface KtDKt57373 : KtBase <KtIKt57373>
- (instancetype)initWithFoo:(id<KtIKt57373>)foo __attribute__((swift_name("init(foo:)"))) __attribute__((objc_designated_initializer));
@property (readonly) int32_t bar __attribute__((swift_name("bar")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CKt57373")))
@interface KtCKt57373 : KtBase <KtIKt57373>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t bar __attribute__((swift_name("bar")));
@end

__attribute__((swift_name("Ckt57791")))
@interface KtCkt57791 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)baz __attribute__((swift_name("baz()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Okt57791")))
@interface KtOkt57791 : KtCkt57791
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (instancetype)okt57791 __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtOkt57791 *shared __attribute__((swift_name("shared")));
- (int32_t)baz __attribute__((swift_name("baz()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ckt57791Final")))
@interface KtCkt57791Final : KtCkt57791
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)baz __attribute__((swift_name("baz()")));
@end

__attribute__((swift_name("Foo")))
@protocol KtFoo
@required
- (KtCkt57791Final *)getCkt57791 __attribute__((swift_name("getCkt57791()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt57791Kt")))
@interface KtKt57791Kt : KtBase
+ (BOOL)foobarF:(BOOL)f foo:(id<KtFoo>)foo __attribute__((swift_name("foobar(f:foo:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LibraryKt")))
@interface KtLibraryKt : KtBase
+ (NSString *)readDataFromLibraryClassInput:(KtA *)input __attribute__((swift_name("readDataFromLibraryClass(input:)")));
+ (NSString *)readDataFromLibraryEnumInput:(KtE *)input __attribute__((swift_name("readDataFromLibraryEnum(input:)")));
+ (NSString *)readDataFromLibraryInterfaceInput:(id<KtI>)input __attribute__((swift_name("readDataFromLibraryInterface(input:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ArraysConstructor")))
@interface KtArraysConstructor : KtBase
- (instancetype)initWithInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("init(int1:int2:)"))) __attribute__((objc_designated_initializer));
- (NSString *)log __attribute__((swift_name("log()")));
- (void)setInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("set(int1:int2:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ArraysDefault")))
@interface KtArraysDefault : KtBase
- (instancetype)initWithInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("init(int1:int2:)"))) __attribute__((objc_designated_initializer));
- (NSString *)log __attribute__((swift_name("log()")));
- (void)setInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("set(int1:int2:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ArraysInitBlock")))
@interface KtArraysInitBlock : KtBase
- (instancetype)initWithInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("init(int1:int2:)"))) __attribute__((objc_designated_initializer));
- (NSString *)log __attribute__((swift_name("log()")));
- (void)setInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("set(int1:int2:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinLivenessTracker")))
@interface KtKotlinLivenessTracker : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)addObj:(id _Nullable)obj __attribute__((swift_name("add(obj:)")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (BOOL)objectsAreDead __attribute__((swift_name("objectsAreDead()")));
@property (readonly) NSMutableArray<KtKotlinWeakReference *> *weakRefs __attribute__((swift_name("weakRefs")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinObject")))
@interface KtKotlinObject : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("NoAutoreleaseSendHelper")))
@protocol KtNoAutoreleaseSendHelper
@required
- (void (^)(KtKotlinObject *))blockReceivingKotlinObject __attribute__((swift_name("blockReceivingKotlinObject()")));
- (void)sendBlockBlock:(KtKotlinObject *(^)(void))block __attribute__((swift_name("sendBlock(block:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)sendCompletionWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("sendCompletion(completionHandler:)")));
- (void)sendKotlinObjectKotlinObject:(KtKotlinObject *)kotlinObject __attribute__((swift_name("sendKotlinObject(kotlinObject:)")));
- (void)sendListList:(NSArray<id> *)list __attribute__((swift_name("sendList(list:)")));
- (void)sendNumberNumber:(id)number __attribute__((swift_name("sendNumber(number:)")));
- (void)sendStringString:(NSString *)string __attribute__((swift_name("sendString(string:)")));
- (void)sendSwiftObjectSwiftObject:(id)swiftObject __attribute__((swift_name("sendSwiftObject(swiftObject:)")));
@end

__attribute__((swift_name("NoAutoreleaseReceiveHelper")))
@protocol KtNoAutoreleaseReceiveHelper
@required
- (KtKotlinObject *(^)(void))receiveBlock __attribute__((swift_name("receiveBlock()")));
- (KtKotlinObject *)receiveKotlinObject __attribute__((swift_name("receiveKotlinObject()")));
- (NSArray<id> *)receiveList __attribute__((swift_name("receiveList()")));
- (id)receiveNumber __attribute__((swift_name("receiveNumber()")));
- (NSString *)receiveString __attribute__((swift_name("receiveString()")));
- (id)receiveSwiftObject __attribute__((swift_name("receiveSwiftObject()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NoAutoreleaseKotlinSendHelper")))
@interface KtNoAutoreleaseKotlinSendHelper : KtBase <KtNoAutoreleaseSendHelper>
- (instancetype)initWithKotlinLivenessTracker:(KtKotlinLivenessTracker *)kotlinLivenessTracker __attribute__((swift_name("init(kotlinLivenessTracker:)"))) __attribute__((objc_designated_initializer));
- (void (^)(KtKotlinObject *))blockReceivingKotlinObject __attribute__((swift_name("blockReceivingKotlinObject()")));
- (void)sendBlockBlock:(KtKotlinObject *(^)(void))block __attribute__((swift_name("sendBlock(block:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)sendCompletionWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("sendCompletion(completionHandler:)")));
- (void)sendKotlinObjectKotlinObject:(KtKotlinObject *)kotlinObject __attribute__((swift_name("sendKotlinObject(kotlinObject:)")));
- (void)sendListList:(NSArray<id> *)list __attribute__((swift_name("sendList(list:)")));
- (void)sendNumberNumber:(id)number __attribute__((swift_name("sendNumber(number:)")));
- (void)sendStringString:(NSString *)string __attribute__((swift_name("sendString(string:)")));
- (void)sendSwiftObjectSwiftObject:(id)swiftObject __attribute__((swift_name("sendSwiftObject(swiftObject:)")));
@property (readonly) KtKotlinLivenessTracker *kotlinLivenessTracker __attribute__((swift_name("kotlinLivenessTracker")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NoAutoreleaseKotlinReceiveHelper")))
@interface KtNoAutoreleaseKotlinReceiveHelper : KtBase <KtNoAutoreleaseReceiveHelper>
- (instancetype)initWithKotlinLivenessTracker:(KtKotlinLivenessTracker *)kotlinLivenessTracker __attribute__((swift_name("init(kotlinLivenessTracker:)"))) __attribute__((objc_designated_initializer));
- (KtKotlinObject *(^)(void))receiveBlock __attribute__((swift_name("receiveBlock()")));
- (KtKotlinObject *)receiveKotlinObject __attribute__((swift_name("receiveKotlinObject()")));
- (NSArray<id> *)receiveList __attribute__((swift_name("receiveList()")));
- (id)receiveNumber __attribute__((swift_name("receiveNumber()")));
- (NSString *)receiveString __attribute__((swift_name("receiveString()")));
- (id)receiveSwiftObject __attribute__((swift_name("receiveSwiftObject()")));
@property (readonly) KtKotlinLivenessTracker *kotlinLivenessTracker __attribute__((swift_name("kotlinLivenessTracker")));
@property id swiftObject __attribute__((swift_name("swiftObject")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NoAutoreleaseSingleton")))
@interface KtNoAutoreleaseSingleton : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)noAutoreleaseSingleton __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtNoAutoreleaseSingleton *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t x __attribute__((swift_name("x")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NoAutoreleaseEnum")))
@interface KtNoAutoreleaseEnum : KtKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtNoAutoreleaseEnum *entry __attribute__((swift_name("entry")));
+ (KtKotlinArray *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KtNoAutoreleaseEnum *> *entries __attribute__((swift_name("entries")));
@property (readonly) int32_t x __attribute__((swift_name("x")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NoAutoreleaseKt")))
@interface KtNoAutoreleaseKt : KtBase
+ (void)callReceiveBlockHelper:(id<KtNoAutoreleaseReceiveHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callReceiveBlock(helper:tracker:)")));
+ (void)callReceiveBlockAndCallHelper:(id<KtNoAutoreleaseReceiveHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callReceiveBlockAndCall(helper:tracker:)")));
+ (void)callReceiveKotlinObjectHelper:(id<KtNoAutoreleaseReceiveHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callReceiveKotlinObject(helper:tracker:)")));
+ (void)callReceiveListHelper:(id<KtNoAutoreleaseReceiveHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callReceiveList(helper:tracker:)")));
+ (void)callReceiveNumberHelper:(id<KtNoAutoreleaseReceiveHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callReceiveNumber(helper:tracker:)")));
+ (void)callReceiveStringHelper:(id<KtNoAutoreleaseReceiveHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callReceiveString(helper:tracker:)")));
+ (void)callReceiveSwiftObjectHelper:(id<KtNoAutoreleaseReceiveHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callReceiveSwiftObject(helper:tracker:)")));
+ (void)callSendBlockHelper:(id<KtNoAutoreleaseSendHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callSendBlock(helper:tracker:)")));
+ (void)callSendCompletionHelper:(id<KtNoAutoreleaseSendHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callSendCompletion(helper:tracker:)")));
+ (void)callSendKotlinObjectHelper:(id<KtNoAutoreleaseSendHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callSendKotlinObject(helper:tracker:)")));
+ (void)callSendListHelper:(id<KtNoAutoreleaseSendHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callSendList(helper:tracker:)")));
+ (void)callSendNumberHelper:(id<KtNoAutoreleaseSendHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callSendNumber(helper:tracker:)")));
+ (void)callSendStringHelper:(id<KtNoAutoreleaseSendHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("callSendString(helper:tracker:)")));
+ (void)callSendSwiftObjectHelper:(id<KtNoAutoreleaseSendHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker swiftObject:(id)swiftObject __attribute__((swift_name("callSendSwiftObject(helper:tracker:swiftObject:)")));
+ (void)gc __attribute__((swift_name("gc()")));
+ (void)objc_autoreleasePoolPopHandle:(void * _Nullable)handle __attribute__((swift_name("objc_autoreleasePoolPop(handle:)")));
+ (void * _Nullable)objc_autoreleasePoolPush __attribute__((swift_name("objc_autoreleasePoolPush()")));
+ (void)sendKotlinObjectToBlockHelper:(id<KtNoAutoreleaseSendHelper>)helper tracker:(KtKotlinLivenessTracker *)tracker __attribute__((swift_name("sendKotlinObjectToBlock(helper:tracker:)")));
+ (void)useIntArrayArray:(KtKotlinIntArray *)array __attribute__((swift_name("useIntArray(array:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObjCNameC1A")))
@interface KtObjCNameC1A : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)foo __attribute__((swift_name("foo()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObjCNameAKt")))
@interface KtObjCNameAKt : KtBase
+ (NSString *)registerForConnectionEventsWithOptions:(NSString *)withOptions __attribute__((swift_name("registerForConnectionEvents(options:)")));
+ (NSString *)scanForPeripheralsWithServices:(int32_t)withServices options:(NSString *)options __attribute__((swift_name("scanForPeripherals(withServices:options:)")));
+ (BOOL)supportsFeatures:(BOOL)features __attribute__((swift_name("supports(_:)")));
+ (NSString *)withUserId:(NSString *)userId __attribute__((swift_name("with(userId:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObjCNameC1B")))
@interface KtObjCNameC1B : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)foo __attribute__((swift_name("foo()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MySwiftArray")))
@interface KtMyObjCArray : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)indexOfObject:(int32_t)object __attribute__((swift_name("index(of:)")));
@property (readonly) int32_t count __attribute__((swift_name("count")));
@end

__attribute__((swift_name("ObjCNameI1")))
@protocol KtObjCNameI1
@required
- (int32_t)someOtherFunctionReceiver:(int32_t)receiver otherParam:(int32_t)otherParam __attribute__((swift_name("someOtherFunction(receiver:otherParam:)")));
@property (readonly) int32_t someOtherValue __attribute__((swift_name("someOtherValue")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SwiftNameC2")))
@interface KtObjCNameC2 : KtBase <KtObjCNameI1>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)someOtherFunctionReceiver:(int32_t)receiver otherParam:(int32_t)otherParam __attribute__((swift_name("someOtherFunction(receiver:otherParam:)")));
@property int32_t someOtherValue __attribute__((swift_name("someOtherValue")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SwiftNameC2.SwiftNestedClass")))
@interface KtObjCNameC2ObjCNestedClass : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property int32_t nestedValue __attribute__((swift_name("nestedValue")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SwiftExactNestedClass")))
@interface ObjCExactNestedClass : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property int32_t nestedValue __attribute__((swift_name("nestedValue")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SwiftNameC3")))
@interface ObjCNameC3 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SwiftNameC3.SwiftNestedClass")))
@interface ObjCNameC3ObjCNestedClass : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property int32_t nestedValue __attribute__((swift_name("nestedValue")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObjCNameC4")))
@interface KtObjCNameC4 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)fooObjCReceiver:(int32_t)receiver objCParam:(int32_t)objCParam __attribute__((swift_name("foo(objCReceiver:objCParam:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObjCNameSwiftObject")))
@interface KtObjCNameObjCObject : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)objCNameObjCObject __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtObjCNameObjCObject *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObjCNameSwiftEnum")))
@interface KtObjCNameObjCEnum : KtKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtObjCNameObjCEnum *objcOne __attribute__((swift_name("swiftOne")));
@property (class, readonly) KtObjCNameObjCEnum *objcTwo __attribute__((swift_name("companion")));
@property (class, readonly) KtObjCNameObjCEnum *objcThree __attribute__((swift_name("swiftThree")));
+ (KtKotlinArray *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KtObjCNameObjCEnum *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObjCNameSwiftEnum.Companion")))
@interface KtObjCNameObjCEnumCompanion : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtObjCNameObjCEnumCompanion *shared __attribute__((swift_name("shared")));
- (int32_t)foo __attribute__((swift_name("foo()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObjCAvoidPreprocessorName")))
@interface KtObjCAvoidPreprocessorName : KtBase
- (instancetype)initWithTime:(int32_t)time __attribute__((swift_name("init(time:)"))) __attribute__((objc_designated_initializer));
@property (readonly) int32_t time __attribute__((swift_name("time")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObjCNameBKt")))
@interface KtObjCNameBKt : KtBase
+ (int32_t)getSomeValueOf:(id<KtObjCNameI1>)receiver __attribute__((swift_name("getSomeValue(of:)")));
@end

__attribute__((swift_name("OverrideKotlinMethods2")))
@protocol KtOverrideKotlinMethods2
@required
- (int32_t)one __attribute__((swift_name("one()")));
@end

__attribute__((swift_name("OverrideKotlinMethods3")))
@interface KtOverrideKotlinMethods3 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("OverrideKotlinMethods4")))
@interface KtOverrideKotlinMethods4 : KtOverrideKotlinMethods3 <KtOverrideKotlinMethods2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)one __attribute__((swift_name("one()")));
@end

__attribute__((swift_name("OverrideKotlinMethods5")))
@protocol KtOverrideKotlinMethods5
@required
- (int32_t)one __attribute__((swift_name("one()")));
@end

__attribute__((swift_name("OverrideKotlinMethods6")))
@protocol KtOverrideKotlinMethods6 <KtOverrideKotlinMethods5>
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OverrideKotlinMethodsKt")))
@interface KtOverrideKotlinMethodsKt : KtBase

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test0Obj:(id)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test0(obj:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test1Obj:(id)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test1(obj:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test2Obj:(id<KtOverrideKotlinMethods2>)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test2(obj:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test3Obj:(KtOverrideKotlinMethods3 *)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test3(obj:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test4Obj:(KtOverrideKotlinMethods4 *)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test4(obj:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test5Obj:(id<KtOverrideKotlinMethods5>)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test5(obj:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test6Obj:(id<KtOverrideKotlinMethods6>)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test6(obj:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OverrideMethodsOfAnyKt")))
@interface KtOverrideMethodsOfAnyKt : KtBase

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)testObj:(id)obj other:(id)other swift:(BOOL)swift error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test(obj:other:swift:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecList")))
@interface KtRecList : KtBase
- (instancetype)initWithValue:(NSArray<id> *)value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
@property (readonly) NSArray<id> *value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecFunc")))
@interface KtRecFunc : KtBase
- (instancetype)initWithValue:(id (^)(void))value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
@property (readonly) id (^value)(void) __attribute__((swift_name("value")));
@end

__attribute__((swift_name("RefinedClassA")))
@interface KtRefinedClassA : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)fooRefined __attribute__((swift_private));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RefinedClassB")))
@interface KtRefinedClassB : KtRefinedClassA
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)fooRefined __attribute__((swift_private));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RefinedKt")))
@interface KtRefinedKt : KtBase
+ (NSString *)fooRefined __attribute__((swift_private));

/**
 * @note annotations
 *   refined.MyShouldRefineInSwift
*/
+ (NSString *)myFooRefined __attribute__((swift_private));
@property (class, readonly) NSString *barRefined __attribute__((swift_private));

/**
 * @note annotations
 *   refined.MyShouldRefineInSwift
*/
@property (class, readonly) NSString *myBarRefined __attribute__((swift_private));
@end

__attribute__((swift_name("Person")))
@interface KtPerson : KtBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Person.User")))
@interface KtPersonUser : KtPerson
- (instancetype)initWithId:(int32_t)id __attribute__((swift_name("init(id:)"))) __attribute__((objc_designated_initializer));
- (KtPersonUser *)doCopyId:(int32_t)id __attribute__((swift_name("doCopy(id:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t id __attribute__((swift_name("id")));
@end

__attribute__((swift_name("Person.Worker")))
@interface KtPersonWorker : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Person.WorkerEmployee")))
@interface KtPersonWorkerEmployee : KtPersonWorker
- (instancetype)initWithId:(int32_t)id __attribute__((swift_name("init(id:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (KtPersonWorkerEmployee *)doCopyId:(int32_t)id __attribute__((swift_name("doCopy(id:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t id __attribute__((swift_name("id")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Person.WorkerContractor")))
@interface KtPersonWorkerContractor : KtPersonWorker
- (instancetype)initWithId:(int32_t)id __attribute__((swift_name("init(id:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (KtPersonWorkerContractor *)doCopyId:(int32_t)id __attribute__((swift_name("doCopy(id:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t id __attribute__((swift_name("id")));
@end

__attribute__((swift_name("SwiftNameManglingI1")))
@protocol KtSwiftNameManglingI1
@required
- (int32_t)clashingMethod __attribute__((swift_name("clashingMethod()")));
- (int32_t)clashingMethodWithObjCNameInBoth __attribute__((swift_name("swiftClashingMethodWithObjCNameInBoth()")));
- (int32_t)clashingMethodWithObjCNameInI1 __attribute__((swift_name("swiftClashingMethodWithObjCNameInI1()")));
- (int32_t)swiftClashingMethodWithObjCNameInI2 __attribute__((swift_name("swiftClashingMethodWithObjCNameInI2()")));
@property (readonly) int32_t clashingProperty __attribute__((swift_name("clashingProperty")));
@end

__attribute__((swift_name("SwiftNameManglingI2")))
@protocol KtSwiftNameManglingI2
@required
- (id)clashingMethod __attribute__((swift_name("clashingMethod()")));
- (id)clashingMethodWithObjCNameInBoth __attribute__((swift_name("swiftClashingMethodWithObjCNameInBoth()")));
- (id)clashingMethodWithObjCNameInI2 __attribute__((swift_name("swiftClashingMethodWithObjCNameInI2()")));
- (id)swiftClashingMethodWithObjCNameInI1 __attribute__((swift_name("swiftClashingMethodWithObjCNameInI1()")));
@property (readonly) id clashingProperty __attribute__((swift_name("clashingProperty")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SwiftNameManglingKt")))
@interface KtSwiftNameManglingKt : KtBase
+ (id<KtSwiftNameManglingI1>)i1 __attribute__((swift_name("i1()")));
+ (id<KtSwiftNameManglingI2>)i2 __attribute__((swift_name("i2()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ThrowableAsError")))
@interface KtThrowableAsError : KtKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

__attribute__((swift_name("ThrowsThrowableAsError")))
@protocol KtThrowsThrowableAsError
@required

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
- (BOOL)throwErrorAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("throwError()")));
@end

__attribute__((swift_name("ThrowsThrowableAsErrorSuspend")))
@protocol KtThrowsThrowableAsErrorSuspend
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)throwErrorWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("throwError(completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ThrowableAsErrorKt")))
@interface KtThrowableAsErrorKt : KtBase
+ (KtThrowableAsError * _Nullable)callAndCatchThrowableAsErrorThrowsThrowableAsError:(id<KtThrowsThrowableAsError>)throwsThrowableAsError __attribute__((swift_name("callAndCatchThrowableAsError(throwsThrowableAsError:)")));
+ (KtThrowableAsError * _Nullable)callAndCatchThrowableAsErrorSuspendThrowsThrowableAsErrorSuspend:(id<KtThrowsThrowableAsErrorSuspend>)throwsThrowableAsErrorSuspend __attribute__((swift_name("callAndCatchThrowableAsErrorSuspend(throwsThrowableAsErrorSuspend:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ThrowsEmptyKt")))
@interface KtThrowsEmptyKt : KtBase

/**
 * @warning All uncaught Kotlin exceptions are fatal.
*/
+ (BOOL)throwsEmptyAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("throwsEmpty()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelManglingAKt")))
@interface KtTopLevelManglingAKt : KtBase
+ (NSString *)foo __attribute__((swift_name("foo()")));
+ (int32_t)sameNumberValue:(int32_t)value __attribute__((swift_name("sameNumber(value:)")));
+ (int64_t)sameNumberValue:(int64_t)value __attribute__((swift_name("sameNumber(value:)")));
@property (class, readonly) NSString *bar __attribute__((swift_name("bar")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelManglingBKt")))
@interface KtTopLevelManglingBKt : KtBase
+ (NSString *)foo __attribute__((swift_name("foo()")));
@property (class, readonly) NSString *bar __attribute__((swift_name("bar")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DelegateClass")))
@interface KtDelegateClass : KtBase <KtKotlinReadWriteProperty>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (KtKotlinArray *)getValueThisRef:(KtKotlinNothing * _Nullable)thisRef property:(id<KtKotlinKProperty>)property __attribute__((swift_name("getValue(thisRef:property:)")));
- (void)setValueThisRef:(KtKotlinNothing * _Nullable)thisRef property:(id<KtKotlinKProperty>)property value:(KtKotlinArray *)value __attribute__((swift_name("setValue(thisRef:property:value:)")));
@end

__attribute__((swift_name("I")))
@protocol KtI
@required
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultInterfaceExt")))
@interface KtDefaultInterfaceExt : KtBase <KtI>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("OpenClassI")))
@interface KtOpenClassI : KtBase <KtI>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FinalClassExtOpen")))
@interface KtFinalClassExtOpen : KtOpenClassI
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end

__attribute__((swift_name("MultiExtClass")))
@interface KtMultiExtClass : KtOpenClassI
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
- (id)piFun __attribute__((swift_name("piFun()")));
@end

__attribute__((swift_name("ConstrClass")))
@interface KtConstrClass : KtOpenClassI
- (instancetype)initWithI:(int32_t)i s:(NSString *)s a:(id)a __attribute__((swift_name("init(i:s:a:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (readonly) id a __attribute__((swift_name("a")));
@property (readonly) int32_t i __attribute__((swift_name("i")));
@property (readonly) NSString *s __attribute__((swift_name("s")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ExtConstrClass")))
@interface KtExtConstrClass : KtConstrClass
- (instancetype)initWithI:(int32_t)i __attribute__((swift_name("init(i:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithI:(int32_t)i s:(NSString *)s a:(id)a __attribute__((swift_name("init(i:s:a:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@property (readonly) int32_t i __attribute__((swift_name("i")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Enumeration")))
@interface KtEnumeration : KtKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtEnumeration *answer __attribute__((swift_name("answer")));
@property (class, readonly) KtEnumeration *year __attribute__((swift_name("year")));
@property (class, readonly) KtEnumeration *temperature __attribute__((swift_name("temperature")));
+ (KtKotlinArray *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KtEnumeration *> *entries __attribute__((swift_name("entries")));
@property (readonly) int32_t enumValue __attribute__((swift_name("enumValue")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TripleVals")))
@interface KtTripleVals : KtBase
- (instancetype)initWithFirst:(id _Nullable)first second:(id _Nullable)second third:(id _Nullable)third __attribute__((swift_name("init(first:second:third:)"))) __attribute__((objc_designated_initializer));
- (KtTripleVals *)doCopyFirst:(id _Nullable)first second:(id _Nullable)second third:(id _Nullable)third __attribute__((swift_name("doCopy(first:second:third:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id _Nullable first __attribute__((swift_name("first")));
@property (readonly) id _Nullable second __attribute__((swift_name("second")));
@property (readonly) id _Nullable third __attribute__((swift_name("third")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TripleVars")))
@interface KtTripleVars : KtBase
- (instancetype)initWithFirst:(id _Nullable)first second:(id _Nullable)second third:(id _Nullable)third __attribute__((swift_name("init(first:second:third:)"))) __attribute__((objc_designated_initializer));
- (KtTripleVars *)doCopyFirst:(id _Nullable)first second:(id _Nullable)second third:(id _Nullable)third __attribute__((swift_name("doCopy(first:second:third:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property id _Nullable first __attribute__((swift_name("first")));
@property id _Nullable second __attribute__((swift_name("second")));
@property id _Nullable third __attribute__((swift_name("third")));
@end

__attribute__((swift_name("WithCompanionAndObject")))
@interface KtWithCompanionAndObject : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) KtWithCompanionAndObjectCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithCompanionAndObject.Companion")))
@interface KtWithCompanionAndObjectCompanion : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtWithCompanionAndObjectCompanion *shared __attribute__((swift_name("shared")));
@property id<KtI> _Nullable named __attribute__((swift_name("named")));
@property (readonly) NSString *str __attribute__((swift_name("str")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithCompanionAndObject.Named")))
@interface KtWithCompanionAndObjectNamed : KtOpenClassI
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (instancetype)named __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtWithCompanionAndObjectNamed *shared __attribute__((swift_name("shared")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyException")))
@interface KtMyException : KtKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyError")))
@interface KtMyError : KtKotlinError
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

__attribute__((swift_name("SwiftOverridableMethodsWithThrows")))
@protocol KtSwiftOverridableMethodsWithThrows
@required

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("any()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("block()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)nothingAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothing()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)unitAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("unit()")));
@end

__attribute__((swift_name("MethodsWithThrows")))
@protocol KtMethodsWithThrows <KtSwiftOverridableMethodsWithThrows>
@required

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("anyN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("blockN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (double)doubleAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("double()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (int32_t)intAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("int()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtLong * _Nullable)longNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("longN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtKotlinNothing * _Nullable)nothingNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothingN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointer()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointerN()"))) __attribute__((swift_error(nonnull_error)));
@end

__attribute__((swift_name("MethodsWithThrowsUnitCaller")))
@protocol KtMethodsWithThrowsUnitCaller
@required

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)callMethods:(id<KtMethodsWithThrows>)methods error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("call(methods:)")));
@end

__attribute__((swift_name("Throwing")))
@interface KtThrowing : KtBase <KtMethodsWithThrows>

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (instancetype _Nullable)initWithDoThrow:(BOOL)doThrow error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("init(doThrow:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("any()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("anyN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("block()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("blockN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (double)doubleAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("double()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (int32_t)intAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("int()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtLong * _Nullable)longNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("longN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)nothingAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothing()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtKotlinNothing * _Nullable)nothingNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothingN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointer()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointerN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)unitAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("unit()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotThrowing")))
@interface KtNotThrowing : KtBase <KtMethodsWithThrows>

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (instancetype _Nullable)initAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("any()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("anyN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("block()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("blockN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (double)doubleAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("double()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (int32_t)intAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("int()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtLong * _Nullable)longNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("longN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)nothingAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothing()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtKotlinNothing * _Nullable)nothingNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothingN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointer()")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointerN()"))) __attribute__((swift_error(nonnull_error)));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)unitAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("unit()")));
@end

__attribute__((swift_name("ThrowsWithBridgeBase")))
@protocol KtThrowsWithBridgeBase
@required

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)plusOneX:(int32_t)x error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("plusOne(x:)")));
@end

__attribute__((swift_name("ThrowsWithBridge")))
@interface KtThrowsWithBridge : KtBase <KtThrowsWithBridgeBase>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt * _Nullable)plusOneX:(int32_t)x error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("plusOne(x:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Deeply")))
@interface KtDeeply : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Deeply.Nested")))
@interface KtDeeplyNested : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Deeply.NestedType")))
@interface KtDeeplyNestedType : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t thirtyTwo __attribute__((swift_name("thirtyTwo")));
@end

__attribute__((swift_name("DeeplyNestedIType")))
@protocol KtDeeplyNestedIType
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithGenericDeeply")))
@interface KtWithGenericDeeply : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithGenericDeeply.Nested")))
@interface KtWithGenericDeeplyNested : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithGenericDeeply.NestedType")))
@interface KtWithGenericDeeplyNestedType : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t thirtyThree __attribute__((swift_name("thirtyThree")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TypeOuter")))
@interface KtTypeOuter : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TypeOuter.Type_")))
@interface KtTypeOuterType_ : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t thirtyFour __attribute__((swift_name("thirtyFour")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CKeywords")))
@interface KtCKeywords : KtBase
- (instancetype)initWithFloat:(float)float_ enum:(int32_t)enum_ goto:(BOOL)goto_ __attribute__((swift_name("init(float:enum:goto:)"))) __attribute__((objc_designated_initializer));
- (KtCKeywords *)doCopyFloat:(float)float_ enum:(int32_t)enum_ goto:(BOOL)goto_ __attribute__((swift_name("doCopy(float:enum:goto:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly, getter=enum) int32_t enum_ __attribute__((swift_name("enum_")));
@property (readonly, getter=float) float float_ __attribute__((swift_name("float_")));
@property (getter=goto, setter=setGoto:) BOOL goto_ __attribute__((swift_name("goto_")));
@end

__attribute__((swift_name("Base1")))
@protocol KtBase1
@required
- (KtInt * _Nullable)sameValue:(KtInt * _Nullable)value __attribute__((swift_name("same(value:)")));
@end

__attribute__((swift_name("ExtendedBase1")))
@protocol KtExtendedBase1 <KtBase1>
@required
@end

__attribute__((swift_name("Base2")))
@protocol KtBase2
@required
- (KtInt * _Nullable)sameValue:(KtInt * _Nullable)value __attribute__((swift_name("same(value:)")));
@end

__attribute__((swift_name("Base23")))
@interface KtBase23 : KtBase <KtBase2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (KtInt *)sameValue:(KtInt * _Nullable)value __attribute__((swift_name("same(value:)")));
@end

__attribute__((swift_name("Transform")))
@protocol KtTransform
@required
- (id _Nullable)mapValue:(id _Nullable)value __attribute__((swift_name("map(value:)")));
@end

__attribute__((swift_name("TransformWithDefault")))
@protocol KtTransformWithDefault <KtTransform>
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TransformInheritingDefault")))
@interface KtTransformInheritingDefault : KtBase <KtTransformWithDefault>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("TransformIntString")))
@protocol KtTransformIntString
@required
- (NSString *)mapIntValue:(int32_t)intValue __attribute__((swift_name("map(intValue:)")));
@end

__attribute__((swift_name("TransformIntToString")))
@interface KtTransformIntToString : KtBase <KtTransform, KtTransformIntString>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)mapValue:(KtInt *)value __attribute__((swift_name("map(value:)")));
- (NSString *)mapIntValue:(int32_t)value __attribute__((swift_name("map(intValue:)")));
@end

__attribute__((swift_name("TransformIntToDecimalString")))
@interface KtTransformIntToDecimalString : KtTransformIntToString
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)mapValue:(KtInt *)value __attribute__((swift_name("map(value:)")));
- (NSString *)mapIntValue:(int32_t)value __attribute__((swift_name("map(intValue:)")));
@end

__attribute__((swift_name("TransformIntToLong")))
@interface KtTransformIntToLong : KtBase <KtTransform>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (KtLong *)mapValue:(KtInt *)value __attribute__((swift_name("map(value:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2931")))
@interface KtGH2931 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2931.Data")))
@interface KtGH2931Data : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2931.Holder")))
@interface KtGH2931Holder : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) KtGH2931Data *data __attribute__((swift_name("data")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2945")))
@interface KtGH2945 : KtBase
- (instancetype)initWithErrno:(int32_t)errno __attribute__((swift_name("init(errno:)"))) __attribute__((objc_designated_initializer));
- (int32_t)testErrnoInSelectorP:(int32_t)p errno:(int32_t)errno __attribute__((swift_name("testErrnoInSelector(p:errno:)")));
@property int32_t errno __attribute__((swift_name("errno")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2830")))
@interface KtGH2830 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id)getI __attribute__((swift_name("getI()")));
@end

__attribute__((swift_name("GH2830I")))
@protocol KtGH2830I
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2959")))
@interface KtGH2959 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSArray<id<KtGH2959I>> *)getIId:(int32_t)id __attribute__((swift_name("getI(id:)")));
@end

__attribute__((swift_name("GH2959I")))
@protocol KtGH2959I
@required
@property (readonly) int32_t id __attribute__((swift_name("id")));
@end

__attribute__((swift_name("IntBlocks")))
@protocol KtIntBlocks
@required
- (int32_t)callBlockArgument:(int32_t)argument block:(id _Nullable)block __attribute__((swift_name("callBlock(argument:block:)")));
- (id _Nullable)getPlusOneBlock __attribute__((swift_name("getPlusOneBlock()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IntBlocksImpl")))
@interface KtIntBlocksImpl : KtBase <KtIntBlocks>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)intBlocksImpl __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtIntBlocksImpl *shared __attribute__((swift_name("shared")));
- (int32_t)callBlockArgument:(int32_t)argument block:(KtInt *(^)(KtInt *))block __attribute__((swift_name("callBlock(argument:block:)")));
- (KtInt *(^)(KtInt *))getPlusOneBlock __attribute__((swift_name("getPlusOneBlock()")));
@end

__attribute__((swift_name("UnitBlockCoercion")))
@protocol KtUnitBlockCoercion
@required
- (id)coerceBlock:(void (^)(void))block __attribute__((swift_name("coerce(block:)")));
- (void (^)(void))uncoerceBlock:(id)block __attribute__((swift_name("uncoerce(block:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UnitBlockCoercionImpl")))
@interface KtUnitBlockCoercionImpl : KtBase <KtUnitBlockCoercion>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unitBlockCoercionImpl __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtUnitBlockCoercionImpl *shared __attribute__((swift_name("shared")));
- (KtKotlinUnit *(^)(void))coerceBlock:(void (^)(void))block __attribute__((swift_name("coerce(block:)")));
- (void (^)(void))uncoerceBlock:(KtKotlinUnit *(^)(void))block __attribute__((swift_name("uncoerce(block:)")));
@end

__attribute__((swift_name("MyAbstractList")))
@interface KtMyAbstractList : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestKClass")))
@interface KtTestKClass : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<KtKotlinKClass> _Nullable)getKotlinClassClazz:(Class)clazz __attribute__((swift_name("getKotlinClass(clazz:)")));
- (id<KtKotlinKClass> _Nullable)getKotlinClassProtocol:(Protocol *)protocol __attribute__((swift_name("getKotlinClass(protocol:)")));
- (BOOL)isIKClass:(id<KtKotlinKClass>)kClass __attribute__((swift_name("isI(kClass:)")));
- (BOOL)isTestKClassKClass:(id<KtKotlinKClass>)kClass __attribute__((swift_name("isTestKClass(kClass:)")));
@end

__attribute__((swift_name("TestKClassI")))
@protocol KtTestKClassI
@required
@end

__attribute__((swift_name("ForwardI2")))
@protocol KtForwardI2 <KtForwardI1>
@required
@end

__attribute__((swift_name("ForwardI1")))
@protocol KtForwardI1
@required
- (id<KtForwardI2>)getForwardI2 __attribute__((swift_name("getForwardI2()")));
@end

__attribute__((swift_name("ForwardC2")))
@interface KtForwardC2 : KtForwardC1
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("ForwardC1")))
@interface KtForwardC1 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (KtForwardC2 *)getForwardC2 __attribute__((swift_name("getForwardC2()")));
@end

__attribute__((swift_name("TestSR10177Workaround")))
@protocol KtTestSR10177Workaround
@required
@end

__attribute__((swift_name("TestClashes1")))
@protocol KtTestClashes1
@required
@property (readonly) int32_t clashingProperty __attribute__((swift_name("clashingProperty")));
@end

__attribute__((swift_name("TestClashes2")))
@protocol KtTestClashes2
@required
@property (readonly) id clashingProperty __attribute__((swift_name("clashingProperty")));
@property (readonly) id clashingProperty_ __attribute__((swift_name("clashingProperty_")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestClashesImpl")))
@interface KtTestClashesImpl : KtBase <KtTestClashes1, KtTestClashes2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t clashingProperty __attribute__((swift_name("clashingProperty")));
@property (readonly) KtInt *clashingProperty_ __attribute__((swift_name("clashingProperty_")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers")))
@interface KtTestInvalidIdentifiers : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) KtTestInvalidIdentifiersCompanionS *companion __attribute__((swift_name("companion")));
- (int32_t)aSdSdS1:(int32_t)S1 _2:(int32_t)_2 _3:(int32_t)_3 __attribute__((swift_name("aSdSd(S1:_2:_3:)")));
@property (readonly) unichar __ __attribute__((swift_name("__")));
@property (readonly) unichar __ __attribute__((swift_name("__")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers.E")))
@interface KtTestInvalidIdentifiersE : KtKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtTestInvalidIdentifiersE *_4s __attribute__((swift_name("_4s")));
@property (class, readonly) KtTestInvalidIdentifiersE *_5s __attribute__((swift_name("_5s")));
@property (class, readonly) KtTestInvalidIdentifiersE *__ __attribute__((swift_name("__")));
@property (class, readonly) KtTestInvalidIdentifiersE *__ __attribute__((swift_name("__")));
+ (KtKotlinArray *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KtTestInvalidIdentifiersE *> *entries __attribute__((swift_name("entries")));
@property (readonly) int32_t value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers.CompanionS")))
@interface KtTestInvalidIdentifiersCompanionS : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companionS __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtTestInvalidIdentifiersCompanionS *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t _42 __attribute__((swift_name("_42")));
@end

__attribute__((swift_name("TestDeprecation")))
@interface KtTestDeprecation : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (int32_t)callEffectivelyHiddenObj:(id)obj __attribute__((swift_name("callEffectivelyHidden(obj:)")));
- (void)error __attribute__((swift_name("error()"))) __attribute__((unavailable("error")));
- (KtTestDeprecationError *)getError __attribute__((swift_name("getError()")));
- (id)getHidden __attribute__((swift_name("getHidden()")));
- (KtTestDeprecationWarning *)getWarning __attribute__((swift_name("getWarning()")));
- (void)normal __attribute__((swift_name("normal()")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("error")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((deprecated("warning")));
- (void)testExtendingHiddenNested:(id)extendingHiddenNested __attribute__((swift_name("test(extendingHiddenNested:)")));
- (void)testExtendingNestedInHidden:(id)extendingNestedInHidden __attribute__((swift_name("test(extendingNestedInHidden:)")));
- (void)testHiddenInner:(id)hiddenInner __attribute__((swift_name("test(hiddenInner:)")));
- (void)testHiddenInnerInner:(id)hiddenInnerInner __attribute__((swift_name("test(hiddenInnerInner:)")));
- (void)testHiddenNested:(id)hiddenNested __attribute__((swift_name("test(hiddenNested:)")));
- (void)testHiddenNestedInner:(id)hiddenNestedInner __attribute__((swift_name("test(hiddenNestedInner:)")));
- (void)testHiddenNestedNested:(id)hiddenNestedNested __attribute__((swift_name("test(hiddenNestedNested:)")));
- (void)testTopLevelHidden:(id)topLevelHidden __attribute__((swift_name("test(topLevelHidden:)")));
- (void)testTopLevelHiddenInner:(id)topLevelHiddenInner __attribute__((swift_name("test(topLevelHiddenInner:)")));
- (void)testTopLevelHiddenInnerInner:(id)topLevelHiddenInnerInner __attribute__((swift_name("test(topLevelHiddenInnerInner:)")));
- (void)testTopLevelHiddenNested:(id)topLevelHiddenNested __attribute__((swift_name("test(topLevelHiddenNested:)")));
- (void)testTopLevelHiddenNestedInner:(id)topLevelHiddenNestedInner __attribute__((swift_name("test(topLevelHiddenNestedInner:)")));
- (void)testTopLevelHiddenNestedNested:(id)topLevelHiddenNestedNested __attribute__((swift_name("test(topLevelHiddenNestedNested:)")));
- (void)warning __attribute__((swift_name("warning()"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable errorVal __attribute__((swift_name("errorVal"))) __attribute__((unavailable("error")));
@property id _Nullable errorVar __attribute__((swift_name("errorVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable normalVal __attribute__((swift_name("normalVal")));
@property id _Nullable normalVar __attribute__((swift_name("normalVar")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("error")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable warningVal __attribute__((swift_name("warningVal"))) __attribute__((deprecated("warning")));
@property id _Nullable warningVar __attribute__((swift_name("warningVar"))) __attribute__((deprecated("warning")));
@end

__attribute__((swift_name("TestDeprecation.OpenHidden")))
@interface KtTestDeprecationOpenHidden : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingHidden")))
@interface KtTestDeprecationExtendingHidden : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingHiddenNested")))
@interface KtTestDeprecationExtendingHiddenNested : NSObject
@end

__attribute__((swift_name("TestDeprecationHiddenInterface")))
@protocol KtTestDeprecationHiddenInterface
@required
@end

__attribute__((swift_name("TestDeprecation.ImplementingHidden")))
@interface KtTestDeprecationImplementingHidden : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)effectivelyHidden __attribute__((swift_name("effectivelyHidden()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.Hidden")))
@interface KtTestDeprecationHidden : NSObject
@end

__attribute__((swift_name("TestDeprecation.HiddenNested")))
@interface KtTestDeprecationHiddenNested : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenNestedNested")))
@interface KtTestDeprecationHiddenNestedNested : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenNestedInner")))
@interface KtTestDeprecationHiddenNestedInner : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenInner")))
@interface KtTestDeprecationHiddenInner : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenInnerInner")))
@interface KtTestDeprecationHiddenInnerInner : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingNestedInHidden")))
@interface KtTestDeprecationExtendingNestedInHidden : NSObject
@end

__attribute__((swift_name("TestDeprecation.OpenError")))
@interface KtTestDeprecationOpenError : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingError")))
@interface KtTestDeprecationExtendingError : KtTestDeprecationOpenError
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("TestDeprecationErrorInterface")))
@protocol KtTestDeprecationErrorInterface
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ImplementingError")))
@interface KtTestDeprecationImplementingError : KtBase <KtTestDeprecationErrorInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.Error")))
@interface KtTestDeprecationError : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

__attribute__((swift_name("TestDeprecation.OpenWarning")))
@interface KtTestDeprecationOpenWarning : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingWarning")))
@interface KtTestDeprecationExtendingWarning : KtTestDeprecationOpenWarning
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("TestDeprecationWarningInterface")))
@protocol KtTestDeprecationWarningInterface
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ImplementingWarning")))
@interface KtTestDeprecationImplementingWarning : KtBase <KtTestDeprecationWarningInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.Warning")))
@interface KtTestDeprecationWarning : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenOverride")))
@interface KtTestDeprecationHiddenOverride : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("hidden")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()"))) __attribute__((unavailable("hidden")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((unavailable("hidden")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("hidden")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("hidden")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal"))) __attribute__((unavailable("hidden")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar"))) __attribute__((unavailable("hidden")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((unavailable("hidden")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((unavailable("hidden")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ErrorOverride")))
@interface KtTestDeprecationErrorOverride : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithHidden:(int8_t)hidden __attribute__((swift_name("init(hidden:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("error")));
- (void)openHidden __attribute__((swift_name("openHidden()"))) __attribute__((unavailable("error")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()"))) __attribute__((unavailable("error")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("error")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openHiddenVal __attribute__((swift_name("openHiddenVal"))) __attribute__((unavailable("error")));
@property id _Nullable openHiddenVar __attribute__((swift_name("openHiddenVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal"))) __attribute__((unavailable("error")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((unavailable("error")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((unavailable("error")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.WarningOverride")))
@interface KtTestDeprecationWarningOverride : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithHidden:(int8_t)hidden __attribute__((swift_name("init(hidden:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((deprecated("warning")));
- (void)openHidden __attribute__((swift_name("openHidden()"))) __attribute__((deprecated("warning")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()"))) __attribute__((deprecated("warning")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openHiddenVal __attribute__((swift_name("openHiddenVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openHiddenVar __attribute__((swift_name("openHiddenVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((deprecated("warning")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.NormalOverride")))
@interface KtTestDeprecationNormalOverride : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithHidden:(int8_t)hidden __attribute__((swift_name("init(hidden:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer));
- (void)openError __attribute__((swift_name("openError()")));
- (void)openHidden __attribute__((swift_name("openHidden()")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()")));
- (void)openWarning __attribute__((swift_name("openWarning()")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar")));
@property (readonly) id _Nullable openHiddenVal __attribute__((swift_name("openHiddenVal")));
@property id _Nullable openHiddenVar __attribute__((swift_name("openHiddenVar")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden")))
@interface KtTopLevelHidden : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.Nested")))
@interface KtTopLevelHiddenNested : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.NestedNested")))
@interface KtTopLevelHiddenNestedNested : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.NestedInner")))
@interface KtTopLevelHiddenNestedInner : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.Inner")))
@interface KtTopLevelHiddenInner : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.InnerInner")))
@interface KtTopLevelHiddenInnerInner : NSObject
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestWeakRefs")))
@interface KtTestWeakRefs : KtBase
- (instancetype)initWithFrozen:(BOOL)frozen __attribute__((swift_name("init(frozen:)"))) __attribute__((objc_designated_initializer));
- (void)clearObj __attribute__((swift_name("clearObj()")));
- (NSArray<id> *)createCycle __attribute__((swift_name("createCycle()")));
- (id)getObj __attribute__((swift_name("getObj()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedRefs")))
@interface KtSharedRefs : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSMutableArray<id> *)createCollection __attribute__((swift_name("createCollection()")));
- (NSMutableArray<id> *)createFrozenCollection __attribute__((swift_name("createFrozenCollection()")));
- (void (^)(void))createFrozenLambda __attribute__((swift_name("createFrozenLambda()")));
- (KtSharedRefsMutableData *)createFrozenRegularObject __attribute__((swift_name("createFrozenRegularObject()")));
- (void (^)(void))createLambda __attribute__((swift_name("createLambda()")));
- (KtSharedRefsMutableData *)createRegularObject __attribute__((swift_name("createRegularObject()")));
- (BOOL)hasAliveObjects __attribute__((swift_name("hasAliveObjects()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedRefs.MutableData")))
@interface KtSharedRefsMutableData : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)update __attribute__((swift_name("update()")));
@property int32_t x __attribute__((swift_name("x")));
@end

__attribute__((swift_name("TestRememberNewObject")))
@protocol KtTestRememberNewObject
@required
- (id)getObject __attribute__((swift_name("getObject()")));
- (void)waitForCleanup __attribute__((swift_name("waitForCleanup()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT49497Model")))
@interface KtKT49497Model : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("ClassForTypeCheck")))
@interface KtClassForTypeCheck : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("InterfaceForTypeCheck")))
@protocol KtInterfaceForTypeCheck
@required
@end

__attribute__((swift_name("IAbstractInterface")))
@protocol KtIAbstractInterface
@required
- (int32_t)foo __attribute__((swift_name("foo()")));
@end

__attribute__((swift_name("IAbstractInterface2")))
@protocol KtIAbstractInterface2
@required
- (int32_t)foo __attribute__((swift_name("foo()")));
@end

__attribute__((swift_name("AbstractInterfaceBase")))
@interface KtAbstractInterfaceBase : KtBase <KtIAbstractInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)bar __attribute__((swift_name("bar()")));
- (int32_t)foo __attribute__((swift_name("foo()")));
@end

__attribute__((swift_name("AbstractInterfaceBase2")))
@interface KtAbstractInterfaceBase2 : KtBase <KtIAbstractInterface2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("AbstractInterfaceBase3")))
@interface KtAbstractInterfaceBase3 : KtBase <KtIAbstractInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)foo __attribute__((swift_name("foo()")));
@end

__attribute__((swift_name("GH3525Base")))
@interface KtGH3525Base : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH3525")))
@interface KtGH3525 : KtGH3525Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (instancetype)gH3525 __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KtGH3525 *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestStringConversion")))
@interface KtTestStringConversion : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property id str __attribute__((swift_name("str")));
@end

__attribute__((swift_name("GH3825")))
@protocol KtGH3825
@required

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call0AndReturnError:(NSError * _Nullable * _Nullable)error callback:(KtBoolean *(^)(void))callback __attribute__((swift_name("call0(callback:)")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call1DoThrow:(BOOL)doThrow error:(NSError * _Nullable * _Nullable)error callback:(void (^)(void))callback __attribute__((swift_name("call1(doThrow:callback:)")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call2Callback:(void (^)(void))callback doThrow:(BOOL)doThrow error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("call2(callback:doThrow:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH3825KotlinImpl")))
@interface KtGH3825KotlinImpl : KtBase <KtGH3825>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call0AndReturnError:(NSError * _Nullable * _Nullable)error callback:(KtBoolean *(^)(void))callback __attribute__((swift_name("call0(callback:)")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call1DoThrow:(BOOL)doThrow error:(NSError * _Nullable * _Nullable)error callback:(void (^)(void))callback __attribute__((swift_name("call1(doThrow:callback:)")));

/**
 * @note This method converts instances of MyException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call2Callback:(void (^)(void))callback doThrow:(BOOL)doThrow error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("call2(callback:doThrow:)")));
@end

__attribute__((swift_name("Foo_FakeOverrideInInterface")))
@protocol KtFoo_FakeOverrideInInterface
@required
- (void)fooT:(id _Nullable)t __attribute__((swift_name("foo(t:)")));
@end

__attribute__((swift_name("Bar_FakeOverrideInInterface")))
@protocol KtBar_FakeOverrideInInterface <KtFoo_FakeOverrideInInterface>
@required
@end

@interface KtEnumeration (ValuesKt)
- (KtEnumeration *)getAnswer __attribute__((swift_name("getAnswer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ValuesKt")))
@interface KtValuesKt : KtBase
+ (id)argsFunI:(int32_t)i l:(int64_t)l d:(double)d s:(NSString *)s __attribute__((swift_name("argsFun(i:l:d:s:)")));
+ (void (^)(void))asUnitBlockBlock:(id _Nullable (^)(void))block __attribute__((swift_name("asUnitBlock(block:)")));
+ (id)boxIc1:(int32_t)ic1 __attribute__((swift_name("box(ic1:)")));
+ (id)boxIc2:(id)ic2 __attribute__((swift_name("box(ic2:)")));
+ (id)boxIc3:(id _Nullable)ic3 __attribute__((swift_name("box(ic3:)")));
+ (KtBoolean * _Nullable)boxBooleanValue:(BOOL)booleanValue __attribute__((swift_name("box(booleanValue:)")));
+ (KtByte * _Nullable)boxByteValue:(int8_t)byteValue __attribute__((swift_name("box(byteValue:)")));
+ (KtDouble * _Nullable)boxDoubleValue:(double)doubleValue __attribute__((swift_name("box(doubleValue:)")));
+ (KtFloat * _Nullable)boxFloatValue:(float)floatValue __attribute__((swift_name("box(floatValue:)")));
+ (KtInt * _Nullable)boxIntValue:(int32_t)intValue __attribute__((swift_name("box(intValue:)")));
+ (KtLong * _Nullable)boxLongValue:(int64_t)longValue __attribute__((swift_name("box(longValue:)")));
+ (KtShort * _Nullable)boxShortValue:(int16_t)shortValue __attribute__((swift_name("box(shortValue:)")));
+ (KtUByte * _Nullable)boxUByteValue:(uint8_t)uByteValue __attribute__((swift_name("box(uByteValue:)")));
+ (KtUInt * _Nullable)boxUIntValue:(uint32_t)uIntValue __attribute__((swift_name("box(uIntValue:)")));
+ (KtULong * _Nullable)boxULongValue:(uint64_t)uLongValue __attribute__((swift_name("box(uLongValue:)")));
+ (KtUShort * _Nullable)boxUShortValue:(uint16_t)uShortValue __attribute__((swift_name("box(uShortValue:)")));
+ (id _Nullable)boxChar:(unichar)receiver __attribute__((swift_name("boxChar(_:)")));
+ (KtInt * _Nullable)callBase1:(id<KtBase1>)base1 value:(KtInt * _Nullable)value __attribute__((swift_name("call(base1:value:)")));
+ (int32_t)callBase23:(KtBase23 *)base23 value:(KtInt * _Nullable)value __attribute__((swift_name("call(base23:value:)")));
+ (KtInt * _Nullable)callBase2:(id<KtBase2>)base2 value:(KtInt * _Nullable)value __attribute__((swift_name("call(base2:value:)")));
+ (KtInt * _Nullable)callExtendedBase1:(id<KtExtendedBase1>)extendedBase1 value:(KtInt * _Nullable)value __attribute__((swift_name("call(extendedBase1:value:)")));
+ (int32_t)callBase3:(id)base3 value:(KtInt * _Nullable)value __attribute__((swift_name("call(base3:value:)")));
+ (void)callFoo_FakeOverrideInInterfaceObj:(id<KtBar_FakeOverrideInInterface>)obj __attribute__((swift_name("callFoo_FakeOverrideInInterface(obj:)")));

/**
 * @note This method converts instances of MyError to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (BOOL)callUnitMethods:(id<KtSwiftOverridableMethodsWithThrows>)methods error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("callUnit(methods:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)callUnitCallerCaller:(id<KtMethodsWithThrowsUnitCaller>)caller methods:(id<KtMethodsWithThrows>)methods error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("callUnitCaller(caller:methods:)")));
+ (NSString *)concatenateInlineClassValuesIc1:(int32_t)ic1 ic1N:(id _Nullable)ic1N ic2:(id)ic2 ic2N:(id _Nullable)ic2N ic3:(id _Nullable)ic3 ic3N:(id _Nullable)ic3N __attribute__((swift_name("concatenateInlineClassValues(ic1:ic1N:ic2:ic2N:ic3:ic3N:)")));
+ (id<KtTransform>)createTransformDecimalStringToInt __attribute__((swift_name("createTransformDecimalStringToInt()")));
+ (void)emptyFun __attribute__((swift_name("emptyFun()")));
+ (void)ensureEqualBooleansActual:(KtBoolean * _Nullable)actual expected:(BOOL)expected __attribute__((swift_name("ensureEqualBooleans(actual:expected:)")));
+ (void)ensureEqualBooleansAsAnyActual:(id _Nullable)actual expected:(BOOL)expected __attribute__((swift_name("ensureEqualBooleansAsAny(actual:expected:)")));
+ (void)ensureEqualBytesActual:(KtByte * _Nullable)actual expected:(int8_t)expected __attribute__((swift_name("ensureEqualBytes(actual:expected:)")));
+ (void)ensureEqualDoublesActual:(KtDouble * _Nullable)actual expected:(double)expected __attribute__((swift_name("ensureEqualDoubles(actual:expected:)")));
+ (void)ensureEqualFloatsActual:(KtFloat * _Nullable)actual expected:(float)expected __attribute__((swift_name("ensureEqualFloats(actual:expected:)")));
+ (void)ensureEqualIntsActual:(KtInt * _Nullable)actual expected:(int32_t)expected __attribute__((swift_name("ensureEqualInts(actual:expected:)")));
+ (void)ensureEqualLongsActual:(KtLong * _Nullable)actual expected:(int64_t)expected __attribute__((swift_name("ensureEqualLongs(actual:expected:)")));
+ (void)ensureEqualShortsActual:(KtShort * _Nullable)actual expected:(int16_t)expected __attribute__((swift_name("ensureEqualShorts(actual:expected:)")));
+ (void)ensureEqualUBytesActual:(KtUByte * _Nullable)actual expected:(uint8_t)expected __attribute__((swift_name("ensureEqualUBytes(actual:expected:)")));
+ (void)ensureEqualUIntsActual:(KtUInt * _Nullable)actual expected:(uint32_t)expected __attribute__((swift_name("ensureEqualUInts(actual:expected:)")));
+ (void)ensureEqualULongsActual:(KtULong * _Nullable)actual expected:(uint64_t)expected __attribute__((swift_name("ensureEqualULongs(actual:expected:)")));
+ (void)ensureEqualUShortsActual:(KtUShort * _Nullable)actual expected:(uint16_t)expected __attribute__((swift_name("ensureEqualUShorts(actual:expected:)")));
+ (void)error __attribute__((swift_name("error()"))) __attribute__((unavailable("error")));
+ (void)fooA:(KtKotlinAtomicReference *)a __attribute__((swift_name("foo(a:)")));
+ (id)fooGenericNumberR:(id)r foo:(id (^)(id))foo __attribute__((swift_name("fooGenericNumber(r:foo:)")));
+ (NSString *)funArgumentFoo:(NSString *(^)(void))foo __attribute__((swift_name("funArgument(foo:)")));
+ (void)gc __attribute__((swift_name("gc()")));
+ (id _Nullable)genericFooT:(id _Nullable)t foo:(id _Nullable (^)(id _Nullable))foo __attribute__((swift_name("genericFoo(t:foo:)")));
+ (KtEnumeration *)getValue:(int32_t)value __attribute__((swift_name("get(value:)")));
+ (KtWithCompanionAndObjectCompanion *)getCompanionObject __attribute__((swift_name("getCompanionObject()")));
+ (KtWithCompanionAndObjectNamed *)getNamedObject __attribute__((swift_name("getNamedObject()")));
+ (KtOpenClassI *)getNamedObjectInterface __attribute__((swift_name("getNamedObjectInterface()")));
+ (void (^ _Nullable)(void))getNullBlock __attribute__((swift_name("getNullBlock()")));
+ (int32_t)getValue1:(int32_t)receiver __attribute__((swift_name("getValue1(_:)")));
+ (NSString *)getValue2:(id)receiver __attribute__((swift_name("getValue2(_:)")));
+ (KtTripleVals * _Nullable)getValue3:(id _Nullable)receiver __attribute__((swift_name("getValue3(_:)")));
+ (KtInt * _Nullable)getValueOrNull1:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull1(_:)")));
+ (NSString * _Nullable)getValueOrNull2:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull2(_:)")));
+ (KtTripleVals * _Nullable)getValueOrNull3:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull3(_:)")));
+ (NSString *)iFunExt:(id<KtI>)receiver __attribute__((swift_name("iFunExt(_:)")));
+ (BOOL)isA:(id _Nullable)receiver __attribute__((swift_name("isA(_:)")));
+ (BOOL)isBlockNullBlock:(void (^ _Nullable)(void))block __attribute__((swift_name("isBlockNull(block:)")));
+ (BOOL)isFreezingEnabled __attribute__((swift_name("isFreezingEnabled()")));
+ (BOOL)isFrozenObj:(id)obj __attribute__((swift_name("isFrozen(obj:)")));
+ (BOOL)isFunctionObj:(id _Nullable)obj __attribute__((swift_name("isFunction(obj:)")));
+ (BOOL)isFunction0Obj:(id _Nullable)obj __attribute__((swift_name("isFunction0(obj:)")));
+ (id)kotlinLambdaBlock:(id (^)(id))block __attribute__((swift_name("kotlinLambda(block:)")));
+ (NSDictionary<KtBoolean *, NSString *> *)mapBoolean2String __attribute__((swift_name("mapBoolean2String()")));
+ (NSDictionary<KtByte *, KtShort *> *)mapByte2Short __attribute__((swift_name("mapByte2Short()")));
+ (NSDictionary<KtDouble *, NSString *> *)mapDouble2String __attribute__((swift_name("mapDouble2String()")));
+ (NSDictionary<KtFloat *, KtFloat *> *)mapFloat2Float __attribute__((swift_name("mapFloat2Float()")));
+ (NSDictionary<KtInt *, KtLong *> *)mapInt2Long __attribute__((swift_name("mapInt2Long()")));
+ (NSDictionary<KtLong *, KtLong *> *)mapLong2Long __attribute__((swift_name("mapLong2Long()")));
+ (NSDictionary<KtShort *, KtByte *> *)mapShort2Byte __attribute__((swift_name("mapShort2Byte()")));
+ (NSDictionary<KtUByte *, KtBoolean *> *)mapUByte2Boolean __attribute__((swift_name("mapUByte2Boolean()")));
+ (NSDictionary<KtUInt *, KtLong *> *)mapUInt2Long __attribute__((swift_name("mapUInt2Long()")));
+ (NSDictionary<KtULong *, KtLong *> *)mapULong2Long __attribute__((swift_name("mapULong2Long()")));
+ (NSDictionary<KtUShort *, KtByte *> *)mapUShort2Byte __attribute__((swift_name("mapUShort2Byte()")));
+ (int64_t)multiplyInt:(int32_t)int_ long:(int64_t)long_ __attribute__((swift_name("multiply(int:long:)")));
+ (KtMutableDictionary<KtBoolean *, NSString *> *)mutBoolean2String __attribute__((swift_name("mutBoolean2String()")));
+ (KtMutableDictionary<KtByte *, KtShort *> *)mutByte2Short __attribute__((swift_name("mutByte2Short()")));
+ (KtMutableDictionary<KtDouble *, NSString *> *)mutDouble2String __attribute__((swift_name("mutDouble2String()")));
+ (KtMutableDictionary<KtFloat *, KtFloat *> *)mutFloat2Float __attribute__((swift_name("mutFloat2Float()")));
+ (KtMutableDictionary<KtInt *, KtLong *> *)mutInt2Long __attribute__((swift_name("mutInt2Long()")));
+ (KtMutableDictionary<KtLong *, KtLong *> *)mutLong2Long __attribute__((swift_name("mutLong2Long()")));
+ (KtMutableDictionary<KtShort *, KtByte *> *)mutShort2Byte __attribute__((swift_name("mutShort2Byte()")));
+ (KtMutableDictionary<KtUByte *, KtBoolean *> *)mutUByte2Boolean __attribute__((swift_name("mutUByte2Boolean()")));
+ (KtMutableDictionary<KtUInt *, KtLong *> *)mutUInt2Long __attribute__((swift_name("mutUInt2Long()")));
+ (KtMutableDictionary<KtULong *, KtLong *> *)mutULong2Long __attribute__((swift_name("mutULong2Long()")));
+ (KtMutableDictionary<KtUShort *, KtByte *> *)mutUShort2Byte __attribute__((swift_name("mutUShort2Byte()")));
+ (KtEnumeration *)passEnum __attribute__((swift_name("passEnum()")));
+ (void)print:(id _Nullable)receiver __attribute__((swift_name("print(_:)")));
+ (void)receiveEnumE:(int32_t)e __attribute__((swift_name("receiveEnum(e:)")));
+ (void)runNothingBlockBlock:(void (^)(void))block __attribute__((swift_name("runNothingBlock(block:)")));
+ (BOOL)runUnitBlockBlock:(void (^)(void))block __attribute__((swift_name("runUnitBlock(block:)")));
+ (id)same:(id)receiver __attribute__((swift_name("same(_:)")));
+ (NSString *)strFun __attribute__((swift_name("strFun()")));
+ (NSString *)subExt:(NSString *)receiver i:(int32_t)i __attribute__((swift_name("subExt(_:i:)")));
+ (int32_t)testAbstractInterfaceCallX:(id<KtIAbstractInterface>)x __attribute__((swift_name("testAbstractInterfaceCall(x:)")));
+ (int32_t)testAbstractInterfaceCall2X:(id<KtIAbstractInterface2>)x __attribute__((swift_name("testAbstractInterfaceCall2(x:)")));
+ (BOOL)testClassTypeCheckX:(id)x __attribute__((swift_name("testClassTypeCheck(x:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)testGH3825Gh3825:(id<KtGH3825>)gh3825 error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("testGH3825(gh3825:)")));
+ (BOOL)testInterfaceTypeCheckX:(id)x __attribute__((swift_name("testInterfaceTypeCheck(x:)")));
+ (void)testRememberNewObjectTest:(id<KtTestRememberNewObject>)test __attribute__((swift_name("testRememberNewObject(test:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)testSwiftNotThrowingMethods:(id<KtSwiftOverridableMethodsWithThrows>)methods error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("testSwiftNotThrowing(methods:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)testSwiftNotThrowingTest:(id<KtThrowsWithBridgeBase>)test error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("testSwiftNotThrowing(test:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (KtKotlinObjCErrorException * _Nullable)testSwiftThrowingMethods:(id<KtSwiftOverridableMethodsWithThrows>)methods error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("testSwiftThrowing(methods:)")));

/**
 * @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)testSwiftThrowingTest:(id<KtThrowsWithBridgeBase>)test flag:(BOOL)flag error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("testSwiftThrowing(test:flag:)")));

/**
 * @note This method converts instances of MyException, MyError to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (BOOL)throwExceptionError:(BOOL)error error:(NSError * _Nullable * _Nullable)error_ __attribute__((swift_name("throwException(error:)")));
+ (NSString *)toString:(id _Nullable)receiver __attribute__((swift_name("toString(_:)")));
+ (NSArray<id> *)varargToListArgs:(KtKotlinArray *)args __attribute__((swift_name("varargToList(args:)")));
+ (void)warning __attribute__((swift_name("warning()"))) __attribute__((deprecated("warning")));
@property (class, readonly) int32_t PROPERTY_NAME_MUST_NOT_BE_ALTERED_BY_SWIFT __attribute__((swift_name("PROPERTY_NAME_MUST_NOT_BE_ALTERED_BY_SWIFT")));
@property (class, readonly) NSArray<id> *anyList __attribute__((swift_name("anyList")));
@property (class) id anyValue __attribute__((swift_name("anyValue")));
@property (class, readonly) id boolAnyVal __attribute__((swift_name("boolAnyVal")));
@property (class, readonly) BOOL boolVal __attribute__((swift_name("boolVal")));
@property (class, readonly) double dbl __attribute__((swift_name("dbl")));
@property (class) KtKotlinArray *delegatedGlobalArray __attribute__((swift_name("delegatedGlobalArray")));
@property (class, readonly) NSArray<NSString *> *delegatedList __attribute__((swift_name("delegatedList")));
@property (class, readonly) id _Nullable errorVal __attribute__((swift_name("errorVal"))) __attribute__((unavailable("error")));
@property (class) id _Nullable errorVar __attribute__((swift_name("errorVar"))) __attribute__((unavailable("error")));
@property (class, readonly) float flt __attribute__((swift_name("flt")));
@property (class) int32_t gh3525BaseInitCount __attribute__((swift_name("gh3525BaseInitCount")));
@property (class) int32_t gh3525InitCount __attribute__((swift_name("gh3525InitCount")));
@property (class, readonly) double infDoubleVal __attribute__((swift_name("infDoubleVal")));
@property (class, readonly) float infFloatVal __attribute__((swift_name("infFloatVal")));
@property (class) int32_t intVar __attribute__((swift_name("intVar")));
@property (class, readonly) int32_t integer __attribute__((swift_name("integer")));
@property (class, readonly) BOOL isExperimentalMM __attribute__((swift_name("isExperimentalMM")));
@property (class) id lateinitIntVar __attribute__((swift_name("lateinitIntVar")));
@property (class, readonly) NSString *lazyVal __attribute__((swift_name("lazyVal")));
@property (class, readonly) int64_t longInt __attribute__((swift_name("longInt")));
@property (class) id maxDoubleVal __attribute__((swift_name("maxDoubleVal")));
@property (class) id minDoubleVal __attribute__((swift_name("minDoubleVal")));
@property (class, readonly) double nanDoubleVal __attribute__((swift_name("nanDoubleVal")));
@property (class, readonly) float nanFloatVal __attribute__((swift_name("nanFloatVal")));
@property (class, readonly) id _Nullable nullVal __attribute__((swift_name("nullVal")));
@property (class) NSString * _Nullable nullVar __attribute__((swift_name("nullVar")));
@property (class, readonly) NSArray<id> *numbersList __attribute__((swift_name("numbersList")));
@property (class) NSString *str __attribute__((swift_name("str")));
@property (class) id strAsAny __attribute__((swift_name("strAsAny")));
@property (class, readonly) KtInt *(^sumLambda)(KtInt *, KtInt *) __attribute__((swift_name("sumLambda")));
@property (class, readonly) id _Nullable warningVal __attribute__((swift_name("warningVal"))) __attribute__((deprecated("warning")));
@property (class) id _Nullable warningVar __attribute__((swift_name("warningVar"))) __attribute__((deprecated("warning")));
@end

__attribute__((swift_name("InvariantSuper")))
@interface KtInvariantSuper : KtBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Invariant")))
@interface KtInvariant : KtInvariantSuper
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("OutVariantSuper")))
@interface KtOutVariantSuper : KtBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OutVariant")))
@interface KtOutVariant : KtOutVariantSuper
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("InVariantSuper")))
@interface KtInVariantSuper : KtBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InVariant")))
@interface KtInVariant : KtInVariantSuper
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

