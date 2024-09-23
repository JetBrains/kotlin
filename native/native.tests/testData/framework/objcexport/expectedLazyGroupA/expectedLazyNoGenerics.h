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

