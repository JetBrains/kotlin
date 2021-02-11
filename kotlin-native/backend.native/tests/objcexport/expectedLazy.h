__attribute__((swift_name("KotlinBase")))
@interface KtBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end;

@interface KtBase (KtBaseCopying) <NSCopying>
@end;

__attribute__((swift_name("KotlinMutableSet")))
@interface KtMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end;

__attribute__((swift_name("KotlinMutableDictionary")))
@interface KtMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end;

@interface NSError (NSErrorKtKotlinException)
@property (readonly) id _Nullable kotlinException;
@end;

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
@end;

__attribute__((swift_name("KotlinByte")))
@interface KtByte : KtNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end;

__attribute__((swift_name("KotlinUByte")))
@interface KtUByte : KtNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end;

__attribute__((swift_name("KotlinShort")))
@interface KtShort : KtNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end;

__attribute__((swift_name("KotlinUShort")))
@interface KtUShort : KtNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end;

__attribute__((swift_name("KotlinInt")))
@interface KtInt : KtNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end;

__attribute__((swift_name("KotlinUInt")))
@interface KtUInt : KtNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end;

__attribute__((swift_name("KotlinLong")))
@interface KtLong : KtNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end;

__attribute__((swift_name("KotlinULong")))
@interface KtULong : KtNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end;

__attribute__((swift_name("KotlinFloat")))
@interface KtFloat : KtNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end;

__attribute__((swift_name("KotlinDouble")))
@interface KtDouble : KtNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end;

__attribute__((swift_name("KotlinBoolean")))
@interface KtBoolean : KtNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CoroutineException")))
@interface KtCoroutineException : KtKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ContinuationHolder")))
@interface KtContinuationHolder<T> : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)resumeValue:(T _Nullable)value __attribute__((swift_name("resume(value:)")));
- (void)resumeWithExceptionException:(KtKotlinThrowable *)exception __attribute__((swift_name("resumeWithException(exception:)")));
@end;

__attribute__((swift_name("SuspendFun")))
@protocol KtSuspendFun
@required

/**
 @note This method converts instances of CoroutineException, CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void)suspendFunDoYield:(BOOL)doYield doThrow:(BOOL)doThrow completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("suspendFun(doYield:doThrow:completionHandler:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ResultHolder")))
@interface KtResultHolder<T> : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property int32_t completed __attribute__((swift_name("completed")));
@property T _Nullable result __attribute__((swift_name("result")));
@property KtKotlinThrowable * _Nullable exception __attribute__((swift_name("exception")));
@end;

__attribute__((swift_name("SuspendBridge")))
@protocol KtSuspendBridge
@required

/**
 @note This method converts instances of CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void)intValue:(id _Nullable)value completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("int(value:completionHandler:)")));

/**
 @note This method converts instances of CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void)intAsAnyValue:(id _Nullable)value completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("intAsAny(value:completionHandler:)")));

/**
 @note This method converts instances of CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void)unitValue:(id _Nullable)value completionHandler:(void (^)(KtKotlinUnit * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("unit(value:completionHandler:)")));

/**
 @note This method converts instances of CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void)unitAsAnyValue:(id _Nullable)value completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("unitAsAny(value:completionHandler:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingValue:(id _Nullable)value completionHandler:(void (^)(KtKotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothing(value:completionHandler:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsIntValue:(id _Nullable)value completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsInt(value:completionHandler:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsAnyValue:(id _Nullable)value completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsAny(value:completionHandler:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsUnitValue:(id _Nullable)value completionHandler:(void (^)(KtKotlinUnit * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsUnit(value:completionHandler:)")));
@end;

__attribute__((swift_name("AbstractSuspendBridge")))
@interface KtAbstractSuspendBridge : KtBase <KtSuspendBridge>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 @note This method converts instances of CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void)intAsAnyValue:(KtInt *)value completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("intAsAny(value:completionHandler:)")));

/**
 @note This method converts instances of CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void)unitAsAnyValue:(KtInt *)value completionHandler:(void (^)(KtKotlinUnit * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("unitAsAny(value:completionHandler:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsIntValue:(KtInt *)value completionHandler:(void (^)(KtKotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsInt(value:completionHandler:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsAnyValue:(KtInt *)value completionHandler:(void (^)(KtKotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsAny(value:completionHandler:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
- (void)nothingAsUnitValue:(KtInt *)value completionHandler:(void (^)(KtKotlinNothing * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("nothingAsUnit(value:completionHandler:)")));
@end;

__attribute__((swift_name("ThrowCancellationException")))
@interface KtThrowCancellationException : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ThrowCancellationExceptionImpl")))
@interface KtThrowCancellationExceptionImpl : KtThrowCancellationException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 @note This method converts instances of CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void)throwCancellationExceptionWithCompletionHandler:(void (^)(KtKotlinUnit * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("throwCancellationException(completionHandler:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CoroutinesKt")))
@interface KtCoroutinesKt : KtBase

/**
 @note This method converts instances of CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
+ (void)suspendFunWithCompletionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("suspendFun(completionHandler:)")));

/**
 @note This method converts instances of CoroutineException, CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
+ (void)suspendFunResult:(id _Nullable)result doSuspend:(BOOL)doSuspend doThrow:(BOOL)doThrow completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("suspendFun(result:doSuspend:doThrow:completionHandler:)")));

/**
 @note This method converts instances of CoroutineException, CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
+ (void)suspendFunAsyncResult:(id _Nullable)result continuationHolder:(KtContinuationHolder<id> *)continuationHolder completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("suspendFunAsync(result:continuationHolder:completionHandler:)")));

/**
 @note This method converts instances of CoroutineException, CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
+ (BOOL)throwExceptionException:(KtKotlinThrowable *)exception error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("throwException(exception:)")));
+ (void)callSuspendFunSuspendFun:(id<KtSuspendFun>)suspendFun doYield:(BOOL)doYield doThrow:(BOOL)doThrow resultHolder:(KtResultHolder<KtInt *> *)resultHolder __attribute__((swift_name("callSuspendFun(suspendFun:doYield:doThrow:resultHolder:)")));

/**
 @note This method converts instances of CoroutineException, CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
+ (void)callSuspendFun2SuspendFun:(id<KtSuspendFun>)suspendFun doYield:(BOOL)doYield doThrow:(BOOL)doThrow completionHandler:(void (^)(KtInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("callSuspendFun2(suspendFun:doYield:doThrow:completionHandler:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)callSuspendBridgeBridge:(KtAbstractSuspendBridge *)bridge resultHolder:(KtResultHolder<KtKotlinUnit *> *)resultHolder error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("callSuspendBridge(bridge:resultHolder:)")));

/**
 @note This method converts instances of CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
+ (void)throwCancellationExceptionWithCompletionHandler:(void (^)(KtKotlinUnit * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("throwCancellationException(completionHandler:)")));
+ (id<KtKotlinSuspendFunction0>)getSuspendLambda0 __attribute__((swift_name("getSuspendLambda0()")));
+ (id<KtKotlinSuspendFunction0>)getSuspendCallableReference0 __attribute__((swift_name("getSuspendCallableReference0()")));
+ (id<KtKotlinSuspendFunction1>)getSuspendLambda1 __attribute__((swift_name("getSuspendLambda1()")));
+ (id<KtKotlinSuspendFunction1>)getSuspendCallableReference1 __attribute__((swift_name("getSuspendCallableReference1()")));

/**
 @note This method converts instances of CancellationException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
+ (void)invoke1Block:(id<KtKotlinSuspendFunction1>)block argument:(id _Nullable)argument completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke1(block:argument:completionHandler:)")));
@end;

__attribute__((swift_name("DeallocRetainBase")))
@interface KtDeallocRetainBase : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DeallocRetainKt")))
@interface KtDeallocRetainKt : KtBase
+ (void)garbageCollect __attribute__((swift_name("garbageCollect()")));
+ (KtKotlinWeakReference<id> *)createWeakReferenceValue:(id)value __attribute__((swift_name("createWeakReference(value:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EnumLeftRightUpDown")))
@interface KtEnumLeftRightUpDown : KtKotlinEnum<KtEnumLeftRightUpDown *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtEnumLeftRightUpDown *left __attribute__((swift_name("left")));
@property (class, readonly) KtEnumLeftRightUpDown *right __attribute__((swift_name("right")));
@property (class, readonly) KtEnumLeftRightUpDown *up __attribute__((swift_name("up")));
@property (class, readonly) KtEnumLeftRightUpDown *down __attribute__((swift_name("down")));
+ (KtKotlinArray<KtEnumLeftRightUpDown *> *)values __attribute__((swift_name("values()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EnumOneTwoThreeValues")))
@interface KtEnumOneTwoThreeValues : KtKotlinEnum<KtEnumOneTwoThreeValues *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtEnumOneTwoThreeValues *one __attribute__((swift_name("one")));
@property (class, readonly) KtEnumOneTwoThreeValues *two __attribute__((swift_name("two")));
@property (class, readonly) KtEnumOneTwoThreeValues *three __attribute__((swift_name("three")));
@property (class, readonly) KtEnumOneTwoThreeValues *values __attribute__((swift_name("values")));
+ (KtKotlinArray<KtEnumOneTwoThreeValues *> *)values __attribute__((swift_name("values()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EnumValuesValues_")))
@interface KtEnumValuesValues_ : KtKotlinEnum<KtEnumValuesValues_ *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtEnumValuesValues_ *values __attribute__((swift_name("values")));
@property (class, readonly) KtEnumValuesValues_ *values __attribute__((swift_name("values")));
+ (KtKotlinArray<KtEnumValuesValues_ *> *)values __attribute__((swift_name("values()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EmptyEnum")))
@interface KtEmptyEnum : KtKotlinEnum<KtEmptyEnum *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (KtKotlinArray<KtEmptyEnum *> *)values __attribute__((swift_name("values()")));
@end;

__attribute__((swift_name("FunInterface")))
@protocol KtFunInterface
@required
- (int32_t)run __attribute__((swift_name("run()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FunInterfacesKt")))
@interface KtFunInterfacesKt : KtBase
+ (id<KtFunInterface>)getObject __attribute__((swift_name("getObject()")));
+ (id<KtFunInterface>)getLambda __attribute__((swift_name("getLambda()")));
@end;

__attribute__((swift_name("FHolder")))
@interface KtFHolder : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("F2Holder")))
@interface KtF2Holder : KtFHolder
- (instancetype)initWithValue:(id _Nullable (^)(id _Nullable, id _Nullable))value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (readonly) id _Nullable (^value)(id _Nullable, id _Nullable) __attribute__((swift_name("value")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("F32Holder")))
@interface KtF32Holder : KtFHolder
- (instancetype)initWithValue:(id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (readonly) id _Nullable (^value)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable) __attribute__((swift_name("value")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("F33Holder")))
@interface KtF33Holder : KtFHolder
- (instancetype)initWithValue:(id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (readonly) id<KtKotlinFunction33> value __attribute__((swift_name("value")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FunctionalTypesKt")))
@interface KtFunctionalTypesKt : KtBase
+ (void)callDynType2List:(NSArray<id _Nullable (^)(id _Nullable, id _Nullable)> *)list param:(id _Nullable)param __attribute__((swift_name("callDynType2(list:param:)")));
+ (void)callStaticType2Fct:(id _Nullable (^)(id _Nullable, id _Nullable))fct param:(id _Nullable)param __attribute__((swift_name("callStaticType2(fct:param:)")));
+ (void)callDynType32List:(NSArray<id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable)> *)list param:(id _Nullable)param __attribute__((swift_name("callDynType32(list:param:)")));
+ (void)callStaticType32Fct:(id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))fct param:(id _Nullable)param __attribute__((swift_name("callStaticType32(fct:param:)")));
+ (void)callDynType33List:(NSArray<id<KtKotlinFunction33>> *)list param:(id _Nullable)param __attribute__((swift_name("callDynType33(list:param:)")));
+ (void)callStaticType33Fct:(id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))fct param:(id _Nullable)param __attribute__((swift_name("callStaticType33(fct:param:)")));
+ (KtF2Holder *)getDynTypeLambda2 __attribute__((swift_name("getDynTypeLambda2()")));
+ (id _Nullable (^)(id _Nullable, id _Nullable))getStaticLambda2 __attribute__((swift_name("getStaticLambda2()")));
+ (KtF2Holder *)getDynTypeRef2 __attribute__((swift_name("getDynTypeRef2()")));
+ (id _Nullable (^)(id _Nullable, id _Nullable))getStaticRef2 __attribute__((swift_name("getStaticRef2()")));
+ (KtF32Holder *)getDynType32 __attribute__((swift_name("getDynType32()")));
+ (id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))getStaticType32 __attribute__((swift_name("getStaticType32()")));
+ (KtF33Holder *)getDynTypeRef33 __attribute__((swift_name("getDynTypeRef33()")));
+ (id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))getStaticTypeRef33 __attribute__((swift_name("getStaticTypeRef33()")));
+ (KtF33Holder *)getDynTypeLambda33 __attribute__((swift_name("getDynTypeLambda33()")));
+ (id _Nullable (^)(id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable, id _Nullable))getStaticTypeLambda33 __attribute__((swift_name("getStaticTypeLambda33()")));
@end;

__attribute__((swift_name("GH4002ArgumentBase")))
@interface KtGH4002ArgumentBase : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH4002Argument")))
@interface KtGH4002Argument : KtGH4002ArgumentBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestIncompatiblePropertyTypeWarning")))
@interface KtTestIncompatiblePropertyTypeWarning : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestIncompatiblePropertyTypeWarningGeneric")))
@interface KtTestIncompatiblePropertyTypeWarningGeneric<T> : KtBase
- (instancetype)initWithValue:(T _Nullable)value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
@property (readonly) T _Nullable value __attribute__((swift_name("value")));
@end;

__attribute__((swift_name("TestIncompatiblePropertyTypeWarningInterfaceWithGenericProperty")))
@protocol KtTestIncompatiblePropertyTypeWarningInterfaceWithGenericProperty
@required
@property (readonly) KtTestIncompatiblePropertyTypeWarningGeneric<id> *p __attribute__((swift_name("p")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestIncompatiblePropertyTypeWarning.ClassOverridingInterfaceWithGenericProperty")))
@interface KtTestIncompatiblePropertyTypeWarningClassOverridingInterfaceWithGenericProperty : KtBase <KtTestIncompatiblePropertyTypeWarningInterfaceWithGenericProperty>
- (instancetype)initWithP:(KtTestIncompatiblePropertyTypeWarningGeneric<NSString *> *)p __attribute__((swift_name("init(p:)"))) __attribute__((objc_designated_initializer));
@property (readonly) KtTestIncompatiblePropertyTypeWarningGeneric<NSString *> *p __attribute__((swift_name("p")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestGH3992")))
@interface KtTestGH3992 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("TestGH3992.C")))
@interface KtTestGH3992C : KtBase
- (instancetype)initWithA:(KtTestGH3992A *)a __attribute__((swift_name("init(a:)"))) __attribute__((objc_designated_initializer));
@property (readonly) KtTestGH3992A *a __attribute__((swift_name("a")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestGH3992.D")))
@interface KtTestGH3992D : KtTestGH3992C
- (instancetype)initWithA:(KtTestGH3992B *)a __attribute__((swift_name("init(a:)"))) __attribute__((objc_designated_initializer));
@property (readonly) KtTestGH3992B *a __attribute__((swift_name("a")));
@end;

__attribute__((swift_name("TestGH3992.A")))
@interface KtTestGH3992A : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestGH3992.B")))
@interface KtTestGH3992B : KtTestGH3992A
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt35940Kt")))
@interface KtKt35940Kt : KtBase
+ (NSString *)testKt35940 __attribute__((swift_name("testKt35940()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT38641")))
@interface KtKT38641 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT38641.IntType")))
@interface KtKT38641IntType : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (getter=description, setter=setDescription:) int32_t description_ __attribute__((swift_name("description_")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT38641.Val")))
@interface KtKT38641Val : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly, getter=description) NSString *description_ __attribute__((swift_name("description_")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT38641.Var")))
@interface KtKT38641Var : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (getter=description, setter=setDescription:) NSString *description_ __attribute__((swift_name("description_")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT38641.TwoProperties")))
@interface KtKT38641TwoProperties : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly, getter=description) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@end;

__attribute__((swift_name("KT38641.OverrideVal")))
@interface KtKT38641OverrideVal : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly, getter=description) NSString *description_ __attribute__((swift_name("description_")));
@end;

__attribute__((swift_name("KT38641OverrideVar")))
@protocol KtKT38641OverrideVar
@required
@property (getter=description, setter=setDescription:) NSString *description_ __attribute__((swift_name("description_")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt38641Kt")))
@interface KtKt38641Kt : KtBase
+ (NSString *)getOverrideValDescriptionImpl:(KtKT38641OverrideVal *)impl __attribute__((swift_name("getOverrideValDescription(impl:)")));
+ (NSString *)getOverrideVarDescriptionImpl:(id<KtKT38641OverrideVar>)impl __attribute__((swift_name("getOverrideVarDescription(impl:)")));
+ (void)setOverrideVarDescriptionImpl:(id<KtKT38641OverrideVar>)impl newValue:(NSString *)newValue __attribute__((swift_name("setOverrideVarDescription(impl:newValue:)")));
@end;

__attribute__((swift_name("JsonConfiguration")))
@interface KtJsonConfiguration : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("This class is deprecated for removal during serialization 1.0 API stabilization.\nFor configuring Json instances, the corresponding builder function can be used instead, e.g. instead of'Json(JsonConfiguration.Stable.copy(isLenient = true))' 'Json { isLenient = true }' should be used.\nInstead of storing JsonConfiguration instances of the code, Json instances can be used directly:'Json(MyJsonConfiguration.copy(prettyPrint = true))' can be replaced with 'Json(from = MyApplicationJson) { prettyPrint = true }'")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MoreTrickyChars")))
@interface KtMoreTrickyChars : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("'\"\\@$(){}\r\n")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt39206Kt")))
@interface KtKt39206Kt : KtBase
+ (int32_t)myFunc __attribute__((swift_name("myFunc()"))) __attribute__((deprecated("Don't call this\nPlease")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ckt41907")))
@interface KtCkt41907 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("Ikt41907")))
@protocol KtIkt41907
@required
- (void)fooC:(KtCkt41907 *)c __attribute__((swift_name("foo(c:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt41907Kt")))
@interface KtKt41907Kt : KtBase
+ (void)escapeCC:(KtCkt41907 *)c __attribute__((swift_name("escapeC(c:)")));
+ (void)testKt41907O:(id<KtIkt41907>)o __attribute__((swift_name("testKt41907(o:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KT43599")))
@interface KtKT43599 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) NSString *memberProperty __attribute__((swift_name("memberProperty")));
@end;

@interface KtKT43599 (Kt43599Kt)
@property (readonly) NSString *extensionProperty __attribute__((swift_name("extensionProperty")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kt43599Kt")))
@interface KtKt43599Kt : KtBase
+ (void)setTopLevelLateinitPropertyValue:(NSString *)value __attribute__((swift_name("setTopLevelLateinitProperty(value:)")));
@property (class, readonly) NSString *topLevelProperty __attribute__((swift_name("topLevelProperty")));
@property (class, readonly) NSString *topLevelLateinitProperty __attribute__((swift_name("topLevelLateinitProperty")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LibraryKt")))
@interface KtLibraryKt : KtBase
+ (NSString *)readDataFromLibraryClassInput:(KtA *)input __attribute__((swift_name("readDataFromLibraryClass(input:)")));
+ (NSString *)readDataFromLibraryInterfaceInput:(id<KtI>)input __attribute__((swift_name("readDataFromLibraryInterface(input:)")));
+ (NSString *)readDataFromLibraryEnumInput:(KtE *)input __attribute__((swift_name("readDataFromLibraryEnum(input:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ArraysConstructor")))
@interface KtArraysConstructor : KtBase
- (instancetype)initWithInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("init(int1:int2:)"))) __attribute__((objc_designated_initializer));
- (void)setInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("set(int1:int2:)")));
- (NSString *)log __attribute__((swift_name("log()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ArraysDefault")))
@interface KtArraysDefault : KtBase
- (instancetype)initWithInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("init(int1:int2:)"))) __attribute__((objc_designated_initializer));
- (void)setInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("set(int1:int2:)")));
- (NSString *)log __attribute__((swift_name("log()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ArraysInitBlock")))
@interface KtArraysInitBlock : KtBase
- (instancetype)initWithInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("init(int1:int2:)"))) __attribute__((objc_designated_initializer));
- (void)setInt1:(int32_t)int1 int2:(int32_t)int2 __attribute__((swift_name("set(int1:int2:)")));
- (NSString *)log __attribute__((swift_name("log()")));
@end;

__attribute__((swift_name("OverrideKotlinMethods2")))
@protocol KtOverrideKotlinMethods2
@required
- (int32_t)one __attribute__((swift_name("one()")));
@end;

__attribute__((swift_name("OverrideKotlinMethods3")))
@interface KtOverrideKotlinMethods3 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("OverrideKotlinMethods4")))
@interface KtOverrideKotlinMethods4 : KtOverrideKotlinMethods3 <KtOverrideKotlinMethods2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)one __attribute__((swift_name("one()")));
@end;

__attribute__((swift_name("OverrideKotlinMethods5")))
@protocol KtOverrideKotlinMethods5
@required
- (int32_t)one __attribute__((swift_name("one()")));
@end;

__attribute__((swift_name("OverrideKotlinMethods6")))
@protocol KtOverrideKotlinMethods6 <KtOverrideKotlinMethods5>
@required
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OverrideKotlinMethodsKt")))
@interface KtOverrideKotlinMethodsKt : KtBase

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test0Obj:(id)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test0(obj:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test1Obj:(id)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test1(obj:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test2Obj:(id<KtOverrideKotlinMethods2>)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test2(obj:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test3Obj:(KtOverrideKotlinMethods3 *)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test3(obj:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test4Obj:(KtOverrideKotlinMethods4 *)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test4(obj:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test5Obj:(id<KtOverrideKotlinMethods5>)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test5(obj:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)test6Obj:(id<KtOverrideKotlinMethods6>)obj error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test6(obj:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OverrideMethodsOfAnyKt")))
@interface KtOverrideMethodsOfAnyKt : KtBase

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)testObj:(id)obj other:(id)other swift:(BOOL)swift error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("test(obj:other:swift:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ThrowsEmptyKt")))
@interface KtThrowsEmptyKt : KtBase

/**
 @warning All uncaught Kotlin exceptions are fatal.
*/
+ (BOOL)throwsEmptyAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("throwsEmpty()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelManglingAKt")))
@interface KtTopLevelManglingAKt : KtBase
+ (NSString *)foo __attribute__((swift_name("foo()")));
+ (int32_t)sameNumberValue:(int32_t)value __attribute__((swift_name("sameNumber(value:)")));
+ (int64_t)sameNumberValue:(int64_t)value __attribute__((swift_name("sameNumber(value:)")));
@property (class, readonly) NSString *bar __attribute__((swift_name("bar")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelManglingBKt")))
@interface KtTopLevelManglingBKt : KtBase
+ (NSString *)foo __attribute__((swift_name("foo()")));
@property (class, readonly) NSString *bar __attribute__((swift_name("bar")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DelegateClass")))
@interface KtDelegateClass : KtBase <KtKotlinReadWriteProperty>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (KtKotlinArray<NSString *> *)getValueThisRef:(KtKotlinNothing * _Nullable)thisRef property:(id<KtKotlinKProperty>)property __attribute__((swift_name("getValue(thisRef:property:)")));
- (void)setValueThisRef:(KtKotlinNothing * _Nullable)thisRef property:(id<KtKotlinKProperty>)property value:(KtKotlinArray<NSString *> *)value __attribute__((swift_name("setValue(thisRef:property:value:)")));
@end;

__attribute__((swift_name("I")))
@protocol KtI
@required
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultInterfaceExt")))
@interface KtDefaultInterfaceExt : KtBase <KtI>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("OpenClassI")))
@interface KtOpenClassI : KtBase <KtI>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FinalClassExtOpen")))
@interface KtFinalClassExtOpen : KtOpenClassI
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end;

__attribute__((swift_name("MultiExtClass")))
@interface KtMultiExtClass : KtOpenClassI
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id)piFun __attribute__((swift_name("piFun()")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end;

__attribute__((swift_name("ConstrClass")))
@interface KtConstrClass : KtOpenClassI
- (instancetype)initWithI:(int32_t)i s:(NSString *)s a:(id)a __attribute__((swift_name("init(i:s:a:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (readonly) int32_t i __attribute__((swift_name("i")));
@property (readonly) NSString *s __attribute__((swift_name("s")));
@property (readonly) id a __attribute__((swift_name("a")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ExtConstrClass")))
@interface KtExtConstrClass : KtConstrClass
- (instancetype)initWithI:(int32_t)i __attribute__((swift_name("init(i:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithI:(int32_t)i s:(NSString *)s a:(id)a __attribute__((swift_name("init(i:s:a:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@property (readonly) int32_t i __attribute__((swift_name("i")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Enumeration")))
@interface KtEnumeration : KtKotlinEnum<KtEnumeration *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtEnumeration *answer __attribute__((swift_name("answer")));
@property (class, readonly) KtEnumeration *year __attribute__((swift_name("year")));
@property (class, readonly) KtEnumeration *temperature __attribute__((swift_name("temperature")));
+ (KtKotlinArray<KtEnumeration *> *)values __attribute__((swift_name("values()")));
@property (readonly) int32_t enumValue __attribute__((swift_name("enumValue")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TripleVals")))
@interface KtTripleVals<T> : KtBase
- (instancetype)initWithFirst:(T _Nullable)first second:(T _Nullable)second third:(T _Nullable)third __attribute__((swift_name("init(first:second:third:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (T _Nullable)component1 __attribute__((swift_name("component1()")));
- (T _Nullable)component2 __attribute__((swift_name("component2()")));
- (T _Nullable)component3 __attribute__((swift_name("component3()")));
- (KtTripleVals<T> *)doCopyFirst:(T _Nullable)first second:(T _Nullable)second third:(T _Nullable)third __attribute__((swift_name("doCopy(first:second:third:)")));
@property (readonly) T _Nullable first __attribute__((swift_name("first")));
@property (readonly) T _Nullable second __attribute__((swift_name("second")));
@property (readonly) T _Nullable third __attribute__((swift_name("third")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TripleVars")))
@interface KtTripleVars<T> : KtBase
- (instancetype)initWithFirst:(T _Nullable)first second:(T _Nullable)second third:(T _Nullable)third __attribute__((swift_name("init(first:second:third:)"))) __attribute__((objc_designated_initializer));
- (NSString *)description __attribute__((swift_name("description()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (T _Nullable)component1 __attribute__((swift_name("component1()")));
- (T _Nullable)component2 __attribute__((swift_name("component2()")));
- (T _Nullable)component3 __attribute__((swift_name("component3()")));
- (KtTripleVars<T> *)doCopyFirst:(T _Nullable)first second:(T _Nullable)second third:(T _Nullable)third __attribute__((swift_name("doCopy(first:second:third:)")));
@property T _Nullable first __attribute__((swift_name("first")));
@property T _Nullable second __attribute__((swift_name("second")));
@property T _Nullable third __attribute__((swift_name("third")));
@end;

__attribute__((swift_name("WithCompanionAndObject")))
@interface KtWithCompanionAndObject : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithCompanionAndObject.Companion")))
@interface KtWithCompanionAndObjectCompanion : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (readonly) NSString *str __attribute__((swift_name("str")));
@property id<KtI> _Nullable named __attribute__((swift_name("named")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithCompanionAndObject.Named")))
@interface KtWithCompanionAndObjectNamed : KtOpenClassI
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (instancetype)named __attribute__((swift_name("init()")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyException")))
@interface KtMyException : KtKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyError")))
@interface KtMyError : KtKotlinError
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(KtKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((swift_name("SwiftOverridableMethodsWithThrows")))
@protocol KtSwiftOverridableMethodsWithThrows
@required

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)unitAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("unit()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)nothingAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothing()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("any()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("block()")));
@end;

__attribute__((swift_name("MethodsWithThrows")))
@protocol KtMethodsWithThrows <KtSwiftOverridableMethodsWithThrows>
@required

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtKotlinNothing * _Nullable)nothingNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothingN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("anyN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("blockN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointer()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointerN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (int32_t)intAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("int()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtLong * _Nullable)longNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("longN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (double)doubleAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("double()"))) __attribute__((swift_error(nonnull_error)));
@end;

__attribute__((swift_name("MethodsWithThrowsUnitCaller")))
@protocol KtMethodsWithThrowsUnitCaller
@required

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)callMethods:(id<KtMethodsWithThrows>)methods error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("call(methods:)")));
@end;

__attribute__((swift_name("Throwing")))
@interface KtThrowing : KtBase <KtMethodsWithThrows>

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (instancetype _Nullable)initWithDoThrow:(BOOL)doThrow error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("init(doThrow:)"))) __attribute__((objc_designated_initializer));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)unitAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("unit()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)nothingAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothing()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtKotlinNothing * _Nullable)nothingNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothingN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("any()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("anyN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("block()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("blockN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointer()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointerN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (int32_t)intAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("int()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtLong * _Nullable)longNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("longN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (double)doubleAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("double()"))) __attribute__((swift_error(nonnull_error)));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotThrowing")))
@interface KtNotThrowing : KtBase <KtMethodsWithThrows>

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (instancetype _Nullable)initAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)unitAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("unit()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)nothingAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothing()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtKotlinNothing * _Nullable)nothingNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("nothingN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("any()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)anyNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("anyN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("block()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt *(^ _Nullable)(void))blockNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("blockN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointer()")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (void * _Nullable)pointerNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("pointerN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (int32_t)intAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("int()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtLong * _Nullable)longNAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("longN()"))) __attribute__((swift_error(nonnull_error)));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (double)doubleAndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("double()"))) __attribute__((swift_error(nonnull_error)));
@end;

__attribute__((swift_name("ThrowsWithBridgeBase")))
@protocol KtThrowsWithBridgeBase
@required

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (id _Nullable)plusOneX:(int32_t)x error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("plusOne(x:)")));
@end;

__attribute__((swift_name("ThrowsWithBridge")))
@interface KtThrowsWithBridge : KtBase <KtThrowsWithBridgeBase>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (KtInt * _Nullable)plusOneX:(int32_t)x error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("plusOne(x:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Deeply")))
@interface KtDeeply : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Deeply.Nested")))
@interface KtDeeplyNested : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Deeply.NestedType")))
@interface KtDeeplyNestedType : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t thirtyTwo __attribute__((swift_name("thirtyTwo")));
@end;

__attribute__((swift_name("DeeplyNestedIType")))
@protocol KtDeeplyNestedIType
@required
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithGenericDeeply")))
@interface KtWithGenericDeeply : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithGenericDeeply.Nested")))
@interface KtWithGenericDeeplyNested : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithGenericDeeplyNestedType")))
@interface KtWithGenericDeeplyNestedType<T> : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t thirtyThree __attribute__((swift_name("thirtyThree")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TypeOuter")))
@interface KtTypeOuter : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TypeOuter.Type_")))
@interface KtTypeOuterType_ : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t thirtyFour __attribute__((swift_name("thirtyFour")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CKeywords")))
@interface KtCKeywords : KtBase
- (instancetype)initWithFloat:(float)float_ enum:(int32_t)enum_ goto:(BOOL)goto_ __attribute__((swift_name("init(float:enum:goto:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (float)component1 __attribute__((swift_name("component1()")));
- (int32_t)component2 __attribute__((swift_name("component2()")));
- (BOOL)component3 __attribute__((swift_name("component3()")));
- (KtCKeywords *)doCopyFloat:(float)float_ enum:(int32_t)enum_ goto:(BOOL)goto_ __attribute__((swift_name("doCopy(float:enum:goto:)")));
@property (readonly, getter=float) float float_ __attribute__((swift_name("float_")));
@property (readonly, getter=enum) int32_t enum_ __attribute__((swift_name("enum_")));
@property (getter=goto, setter=setGoto:) BOOL goto_ __attribute__((swift_name("goto_")));
@end;

__attribute__((swift_name("Base1")))
@protocol KtBase1
@required
- (KtInt * _Nullable)sameValue:(KtInt * _Nullable)value __attribute__((swift_name("same(value:)")));
@end;

__attribute__((swift_name("ExtendedBase1")))
@protocol KtExtendedBase1 <KtBase1>
@required
@end;

__attribute__((swift_name("Base2")))
@protocol KtBase2
@required
- (KtInt * _Nullable)sameValue:(KtInt * _Nullable)value __attribute__((swift_name("same(value:)")));
@end;

__attribute__((swift_name("Base23")))
@interface KtBase23 : KtBase <KtBase2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (KtInt *)sameValue:(KtInt * _Nullable)value __attribute__((swift_name("same(value:)")));
@end;

__attribute__((swift_name("Transform")))
@protocol KtTransform
@required
- (id _Nullable)mapValue:(id _Nullable)value __attribute__((swift_name("map(value:)")));
@end;

__attribute__((swift_name("TransformWithDefault")))
@protocol KtTransformWithDefault <KtTransform>
@required
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TransformInheritingDefault")))
@interface KtTransformInheritingDefault<T> : KtBase <KtTransformWithDefault>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("TransformIntString")))
@protocol KtTransformIntString
@required
- (NSString *)mapIntValue:(int32_t)intValue __attribute__((swift_name("map(intValue:)")));
@end;

__attribute__((swift_name("TransformIntToString")))
@interface KtTransformIntToString : KtBase <KtTransform, KtTransformIntString>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)mapValue:(KtInt *)intValue __attribute__((swift_name("map(value:)")));
- (NSString *)mapIntValue:(int32_t)intValue __attribute__((swift_name("map(intValue:)")));
@end;

__attribute__((swift_name("TransformIntToDecimalString")))
@interface KtTransformIntToDecimalString : KtTransformIntToString
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)mapValue:(KtInt *)intValue __attribute__((swift_name("map(value:)")));
- (NSString *)mapIntValue:(int32_t)intValue __attribute__((swift_name("map(intValue:)")));
@end;

__attribute__((swift_name("TransformIntToLong")))
@interface KtTransformIntToLong : KtBase <KtTransform>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (KtLong *)mapValue:(KtInt *)value __attribute__((swift_name("map(value:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2931")))
@interface KtGH2931 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2931.Data")))
@interface KtGH2931Data : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2931.Holder")))
@interface KtGH2931Holder : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) KtGH2931Data *data __attribute__((swift_name("data")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2945")))
@interface KtGH2945 : KtBase
- (instancetype)initWithErrno:(int32_t)errno __attribute__((swift_name("init(errno:)"))) __attribute__((objc_designated_initializer));
- (int32_t)testErrnoInSelectorP:(int32_t)p errno:(int32_t)errno __attribute__((swift_name("testErrnoInSelector(p:errno:)")));
@property int32_t errno __attribute__((swift_name("errno")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2830")))
@interface KtGH2830 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id)getI __attribute__((swift_name("getI()")));
@end;

__attribute__((swift_name("GH2830I")))
@protocol KtGH2830I
@required
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2959")))
@interface KtGH2959 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSArray<id<KtGH2959I>> *)getIId:(int32_t)id __attribute__((swift_name("getI(id:)")));
@end;

__attribute__((swift_name("GH2959I")))
@protocol KtGH2959I
@required
@property (readonly) int32_t id __attribute__((swift_name("id")));
@end;

__attribute__((swift_name("IntBlocks")))
@protocol KtIntBlocks
@required
- (id _Nullable)getPlusOneBlock __attribute__((swift_name("getPlusOneBlock()")));
- (int32_t)callBlockArgument:(int32_t)argument block:(id _Nullable)block __attribute__((swift_name("callBlock(argument:block:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IntBlocksImpl")))
@interface KtIntBlocksImpl : KtBase <KtIntBlocks>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)intBlocksImpl __attribute__((swift_name("init()")));
- (KtInt *(^)(KtInt *))getPlusOneBlock __attribute__((swift_name("getPlusOneBlock()")));
- (int32_t)callBlockArgument:(int32_t)argument block:(KtInt *(^)(KtInt *))block __attribute__((swift_name("callBlock(argument:block:)")));
@end;

__attribute__((swift_name("UnitBlockCoercion")))
@protocol KtUnitBlockCoercion
@required
- (id)coerceBlock:(void (^)(void))block __attribute__((swift_name("coerce(block:)")));
- (void (^)(void))uncoerceBlock:(id)block __attribute__((swift_name("uncoerce(block:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UnitBlockCoercionImpl")))
@interface KtUnitBlockCoercionImpl : KtBase <KtUnitBlockCoercion>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unitBlockCoercionImpl __attribute__((swift_name("init()")));
- (KtKotlinUnit *(^)(void))coerceBlock:(void (^)(void))block __attribute__((swift_name("coerce(block:)")));
- (void (^)(void))uncoerceBlock:(KtKotlinUnit *(^)(void))block __attribute__((swift_name("uncoerce(block:)")));
@end;

__attribute__((swift_name("MyAbstractList")))
@interface KtMyAbstractList : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestKClass")))
@interface KtTestKClass : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<KtKotlinKClass> _Nullable)getKotlinClassClazz:(Class)clazz __attribute__((swift_name("getKotlinClass(clazz:)")));
- (id<KtKotlinKClass> _Nullable)getKotlinClassProtocol:(Protocol *)protocol __attribute__((swift_name("getKotlinClass(protocol:)")));
- (BOOL)isTestKClassKClass:(id<KtKotlinKClass>)kClass __attribute__((swift_name("isTestKClass(kClass:)")));
- (BOOL)isIKClass:(id<KtKotlinKClass>)kClass __attribute__((swift_name("isI(kClass:)")));
@end;

__attribute__((swift_name("TestKClassI")))
@protocol KtTestKClassI
@required
@end;

__attribute__((swift_name("ForwardI2")))
@protocol KtForwardI2 <KtForwardI1>
@required
@end;

__attribute__((swift_name("ForwardI1")))
@protocol KtForwardI1
@required
- (id<KtForwardI2>)getForwardI2 __attribute__((swift_name("getForwardI2()")));
@end;

__attribute__((swift_name("ForwardC2")))
@interface KtForwardC2 : KtForwardC1
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("ForwardC1")))
@interface KtForwardC1 : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (KtForwardC2 *)getForwardC2 __attribute__((swift_name("getForwardC2()")));
@end;

__attribute__((swift_name("TestSR10177Workaround")))
@protocol KtTestSR10177Workaround
@required
@end;

__attribute__((swift_name("TestClashes1")))
@protocol KtTestClashes1
@required
@property (readonly) int32_t clashingProperty __attribute__((swift_name("clashingProperty")));
@end;

__attribute__((swift_name("TestClashes2")))
@protocol KtTestClashes2
@required
@property (readonly) id clashingProperty __attribute__((swift_name("clashingProperty")));
@property (readonly) id clashingProperty_ __attribute__((swift_name("clashingProperty_")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestClashesImpl")))
@interface KtTestClashesImpl : KtBase <KtTestClashes1, KtTestClashes2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t clashingProperty __attribute__((swift_name("clashingProperty")));
@property (readonly) KtInt *clashingProperty_ __attribute__((swift_name("clashingProperty_")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers")))
@interface KtTestInvalidIdentifiers : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)a_d_d_1:(int32_t)_1 _2:(int32_t)_2 _3:(int32_t)_3 __attribute__((swift_name("a_d_d(_1:_2:_3:)")));
@property NSString *_status __attribute__((swift_name("_status")));
@property (readonly) unichar __ __attribute__((swift_name("__")));
@property (readonly) unichar __ __attribute__((swift_name("__")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers._Foo")))
@interface KtTestInvalidIdentifiers_Foo : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers.Bar_")))
@interface KtTestInvalidIdentifiersBar_ : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers.E")))
@interface KtTestInvalidIdentifiersE : KtKotlinEnum<KtTestInvalidIdentifiersE *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtTestInvalidIdentifiersE *_4_ __attribute__((swift_name("_4_")));
@property (class, readonly) KtTestInvalidIdentifiersE *_5_ __attribute__((swift_name("_5_")));
@property (class, readonly) KtTestInvalidIdentifiersE *__ __attribute__((swift_name("__")));
@property (class, readonly) KtTestInvalidIdentifiersE *__ __attribute__((swift_name("__")));
+ (KtKotlinArray<KtTestInvalidIdentifiersE *> *)values __attribute__((swift_name("values()")));
@property (readonly) int32_t value __attribute__((swift_name("value")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers.Companion_")))
@interface KtTestInvalidIdentifiersCompanion_ : KtBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion_ __attribute__((swift_name("init()")));
@property (readonly) int32_t _42 __attribute__((swift_name("_42")));
@end;

__attribute__((swift_name("TestDeprecation")))
@interface KtTestDeprecation : KtBase
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)callEffectivelyHiddenObj:(id)obj __attribute__((swift_name("callEffectivelyHidden(obj:)")));
- (id)getHidden __attribute__((swift_name("getHidden()")));
- (KtTestDeprecationError *)getError __attribute__((swift_name("getError()")));
- (void)error __attribute__((swift_name("error()"))) __attribute__((unavailable("error")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("error")));
- (KtTestDeprecationWarning *)getWarning __attribute__((swift_name("getWarning()")));
- (void)warning __attribute__((swift_name("warning()"))) __attribute__((deprecated("warning")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((deprecated("warning")));
- (void)normal __attribute__((swift_name("normal()")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()")));
- (void)testHiddenNested:(id)hiddenNested __attribute__((swift_name("test(hiddenNested:)")));
- (void)testHiddenNestedNested:(id)hiddenNestedNested __attribute__((swift_name("test(hiddenNestedNested:)")));
- (void)testHiddenNestedInner:(id)hiddenNestedInner __attribute__((swift_name("test(hiddenNestedInner:)")));
- (void)testHiddenInner:(id)hiddenInner __attribute__((swift_name("test(hiddenInner:)")));
- (void)testHiddenInnerInner:(id)hiddenInnerInner __attribute__((swift_name("test(hiddenInnerInner:)")));
- (void)testTopLevelHidden:(id)topLevelHidden __attribute__((swift_name("test(topLevelHidden:)")));
- (void)testTopLevelHiddenNested:(id)topLevelHiddenNested __attribute__((swift_name("test(topLevelHiddenNested:)")));
- (void)testTopLevelHiddenNestedNested:(id)topLevelHiddenNestedNested __attribute__((swift_name("test(topLevelHiddenNestedNested:)")));
- (void)testTopLevelHiddenNestedInner:(id)topLevelHiddenNestedInner __attribute__((swift_name("test(topLevelHiddenNestedInner:)")));
- (void)testTopLevelHiddenInner:(id)topLevelHiddenInner __attribute__((swift_name("test(topLevelHiddenInner:)")));
- (void)testTopLevelHiddenInnerInner:(id)topLevelHiddenInnerInner __attribute__((swift_name("test(topLevelHiddenInnerInner:)")));
- (void)testExtendingHiddenNested:(id)extendingHiddenNested __attribute__((swift_name("test(extendingHiddenNested:)")));
- (void)testExtendingNestedInHidden:(id)extendingNestedInHidden __attribute__((swift_name("test(extendingNestedInHidden:)")));
@property (readonly) id _Nullable errorVal __attribute__((swift_name("errorVal"))) __attribute__((unavailable("error")));
@property id _Nullable errorVar __attribute__((swift_name("errorVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("error")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable warningVal __attribute__((swift_name("warningVal"))) __attribute__((deprecated("warning")));
@property id _Nullable warningVar __attribute__((swift_name("warningVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable normalVal __attribute__((swift_name("normalVal")));
@property id _Nullable normalVar __attribute__((swift_name("normalVar")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar")));
@end;

__attribute__((swift_name("TestDeprecation.OpenHidden")))
@interface KtTestDeprecationOpenHidden : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingHidden")))
@interface KtTestDeprecationExtendingHidden : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingHiddenNested")))
@interface KtTestDeprecationExtendingHiddenNested : NSObject
@end;

__attribute__((swift_name("TestDeprecationHiddenInterface")))
@protocol KtTestDeprecationHiddenInterface
@required
@end;

__attribute__((swift_name("TestDeprecation.ImplementingHidden")))
@interface KtTestDeprecationImplementingHidden : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)effectivelyHidden __attribute__((swift_name("effectivelyHidden()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.Hidden")))
@interface KtTestDeprecationHidden : NSObject
@end;

__attribute__((swift_name("TestDeprecation.HiddenNested")))
@interface KtTestDeprecationHiddenNested : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenNestedNested")))
@interface KtTestDeprecationHiddenNestedNested : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenNestedInner")))
@interface KtTestDeprecationHiddenNestedInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenInner")))
@interface KtTestDeprecationHiddenInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenInnerInner")))
@interface KtTestDeprecationHiddenInnerInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingNestedInHidden")))
@interface KtTestDeprecationExtendingNestedInHidden : NSObject
@end;

__attribute__((swift_name("TestDeprecation.OpenError")))
@interface KtTestDeprecationOpenError : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingError")))
@interface KtTestDeprecationExtendingError : KtTestDeprecationOpenError
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("TestDeprecationErrorInterface")))
@protocol KtTestDeprecationErrorInterface
@required
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ImplementingError")))
@interface KtTestDeprecationImplementingError : KtBase <KtTestDeprecationErrorInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.Error")))
@interface KtTestDeprecationError : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((swift_name("TestDeprecation.OpenWarning")))
@interface KtTestDeprecationOpenWarning : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingWarning")))
@interface KtTestDeprecationExtendingWarning : KtTestDeprecationOpenWarning
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("TestDeprecationWarningInterface")))
@protocol KtTestDeprecationWarningInterface
@required
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ImplementingWarning")))
@interface KtTestDeprecationImplementingWarning : KtBase <KtTestDeprecationWarningInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.Warning")))
@interface KtTestDeprecationWarning : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenOverride")))
@interface KtTestDeprecationHiddenOverride : KtTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("hidden")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((unavailable("hidden")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()"))) __attribute__((unavailable("hidden")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("hidden")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("hidden")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((unavailable("hidden")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((unavailable("hidden")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal"))) __attribute__((unavailable("hidden")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar"))) __attribute__((unavailable("hidden")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ErrorOverride")))
@interface KtTestDeprecationErrorOverride : KtTestDeprecation
- (instancetype)initWithHidden:(int8_t)hidden __attribute__((swift_name("init(hidden:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)openHidden __attribute__((swift_name("openHidden()"))) __attribute__((unavailable("error")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("error")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((unavailable("error")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openHiddenVal __attribute__((swift_name("openHiddenVal"))) __attribute__((unavailable("error")));
@property id _Nullable openHiddenVar __attribute__((swift_name("openHiddenVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("error")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((unavailable("error")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal"))) __attribute__((unavailable("error")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar"))) __attribute__((unavailable("error")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.WarningOverride")))
@interface KtTestDeprecationWarningOverride : KtTestDeprecation
- (instancetype)initWithHidden:(int8_t)hidden __attribute__((swift_name("init(hidden:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)openHidden __attribute__((swift_name("openHidden()"))) __attribute__((deprecated("warning")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((deprecated("warning")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((deprecated("warning")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openHiddenVal __attribute__((swift_name("openHiddenVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openHiddenVar __attribute__((swift_name("openHiddenVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar"))) __attribute__((deprecated("warning")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.NormalOverride")))
@interface KtTestDeprecationNormalOverride : KtTestDeprecation
- (instancetype)initWithHidden:(int8_t)hidden __attribute__((swift_name("init(hidden:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("Overrides deprecated member in 'conversions.TestDeprecation'. error")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((deprecated("Overrides deprecated member in 'conversions.TestDeprecation'. warning")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("Overrides deprecated member in 'conversions.TestDeprecation'. error")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("Overrides deprecated member in 'conversions.TestDeprecation'. error")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((deprecated("Overrides deprecated member in 'conversions.TestDeprecation'. warning")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((deprecated("Overrides deprecated member in 'conversions.TestDeprecation'. warning")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden")))
@interface KtTopLevelHidden : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.Nested")))
@interface KtTopLevelHiddenNested : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.NestedNested")))
@interface KtTopLevelHiddenNestedNested : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.NestedInner")))
@interface KtTopLevelHiddenNestedInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.Inner")))
@interface KtTopLevelHiddenInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.InnerInner")))
@interface KtTopLevelHiddenInnerInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestWeakRefs")))
@interface KtTestWeakRefs : KtBase
- (instancetype)initWithFrozen:(BOOL)frozen __attribute__((swift_name("init(frozen:)"))) __attribute__((objc_designated_initializer));
- (id)getObj __attribute__((swift_name("getObj()")));
- (void)clearObj __attribute__((swift_name("clearObj()")));
- (NSArray<id> *)createCycle __attribute__((swift_name("createCycle()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedRefs")))
@interface KtSharedRefs : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (KtSharedRefsMutableData *)createRegularObject __attribute__((swift_name("createRegularObject()")));
- (void (^)(void))createLambda __attribute__((swift_name("createLambda()")));
- (NSMutableArray<id> *)createCollection __attribute__((swift_name("createCollection()")));
- (KtSharedRefsMutableData *)createFrozenRegularObject __attribute__((swift_name("createFrozenRegularObject()")));
- (void (^)(void))createFrozenLambda __attribute__((swift_name("createFrozenLambda()")));
- (NSMutableArray<id> *)createFrozenCollection __attribute__((swift_name("createFrozenCollection()")));
- (BOOL)hasAliveObjects __attribute__((swift_name("hasAliveObjects()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedRefs.MutableData")))
@interface KtSharedRefsMutableData : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)update __attribute__((swift_name("update()")));
@property int32_t x __attribute__((swift_name("x")));
@end;

__attribute__((swift_name("TestRememberNewObject")))
@protocol KtTestRememberNewObject
@required
- (id)getObject __attribute__((swift_name("getObject()")));
- (void)waitForCleanup __attribute__((swift_name("waitForCleanup()")));
@end;

__attribute__((swift_name("ClassForTypeCheck")))
@interface KtClassForTypeCheck : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("InterfaceForTypeCheck")))
@protocol KtInterfaceForTypeCheck
@required
@end;

__attribute__((swift_name("IAbstractInterface")))
@protocol KtIAbstractInterface
@required
- (int32_t)foo __attribute__((swift_name("foo()")));
@end;

__attribute__((swift_name("IAbstractInterface2")))
@protocol KtIAbstractInterface2
@required
- (int32_t)foo __attribute__((swift_name("foo()")));
@end;

__attribute__((swift_name("AbstractInterfaceBase")))
@interface KtAbstractInterfaceBase : KtBase <KtIAbstractInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)foo __attribute__((swift_name("foo()")));
- (int32_t)bar __attribute__((swift_name("bar()")));
@end;

__attribute__((swift_name("AbstractInterfaceBase2")))
@interface KtAbstractInterfaceBase2 : KtBase <KtIAbstractInterface2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("AbstractInterfaceBase3")))
@interface KtAbstractInterfaceBase3 : KtBase <KtIAbstractInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)foo __attribute__((swift_name("foo()")));
@end;

__attribute__((swift_name("GH3525Base")))
@interface KtGH3525Base : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH3525")))
@interface KtGH3525 : KtGH3525Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (instancetype)gH3525 __attribute__((swift_name("init()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestStringConversion")))
@interface KtTestStringConversion : KtBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property id str __attribute__((swift_name("str")));
@end;

__attribute__((swift_name("GH3825")))
@protocol KtGH3825
@required

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call0AndReturnError:(NSError * _Nullable * _Nullable)error callback:(KtBoolean *(^)(void))callback __attribute__((swift_name("call0(callback:)")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call1DoThrow:(BOOL)doThrow error:(NSError * _Nullable * _Nullable)error callback:(void (^)(void))callback __attribute__((swift_name("call1(doThrow:callback:)")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call2Callback:(void (^)(void))callback doThrow:(BOOL)doThrow error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("call2(callback:doThrow:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH3825KotlinImpl")))
@interface KtGH3825KotlinImpl : KtBase <KtGH3825>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call0AndReturnError:(NSError * _Nullable * _Nullable)error callback:(KtBoolean *(^)(void))callback __attribute__((swift_name("call0(callback:)")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call1DoThrow:(BOOL)doThrow error:(NSError * _Nullable * _Nullable)error callback:(void (^)(void))callback __attribute__((swift_name("call1(doThrow:callback:)")));

/**
 @note This method converts instances of MyException to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
- (BOOL)call2Callback:(void (^)(void))callback doThrow:(BOOL)doThrow error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("call2(callback:doThrow:)")));
@end;

__attribute__((swift_name("Foo_FakeOverrideInInterface")))
@protocol KtFoo_FakeOverrideInInterface
@required
- (void)fooT:(id _Nullable)t __attribute__((swift_name("foo(t:)")));
@end;

__attribute__((swift_name("Bar_FakeOverrideInInterface")))
@protocol KtBar_FakeOverrideInInterface <KtFoo_FakeOverrideInInterface>
@required
@end;

@interface KtEnumeration (ValuesKt)
- (KtEnumeration *)getAnswer __attribute__((swift_name("getAnswer()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ValuesKt")))
@interface KtValuesKt : KtBase
+ (KtBoolean * _Nullable)boxBooleanValue:(BOOL)booleanValue __attribute__((swift_name("box(booleanValue:)")));
+ (KtByte * _Nullable)boxByteValue:(int8_t)byteValue __attribute__((swift_name("box(byteValue:)")));
+ (KtShort * _Nullable)boxShortValue:(int16_t)shortValue __attribute__((swift_name("box(shortValue:)")));
+ (KtInt * _Nullable)boxIntValue:(int32_t)intValue __attribute__((swift_name("box(intValue:)")));
+ (KtLong * _Nullable)boxLongValue:(int64_t)longValue __attribute__((swift_name("box(longValue:)")));
+ (KtUByte * _Nullable)boxUByteValue:(uint8_t)uByteValue __attribute__((swift_name("box(uByteValue:)")));
+ (KtUShort * _Nullable)boxUShortValue:(uint16_t)uShortValue __attribute__((swift_name("box(uShortValue:)")));
+ (KtUInt * _Nullable)boxUIntValue:(uint32_t)uIntValue __attribute__((swift_name("box(uIntValue:)")));
+ (KtULong * _Nullable)boxULongValue:(uint64_t)uLongValue __attribute__((swift_name("box(uLongValue:)")));
+ (KtFloat * _Nullable)boxFloatValue:(float)floatValue __attribute__((swift_name("box(floatValue:)")));
+ (KtDouble * _Nullable)boxDoubleValue:(double)doubleValue __attribute__((swift_name("box(doubleValue:)")));
+ (void)ensureEqualBooleansActual:(KtBoolean * _Nullable)actual expected:(BOOL)expected __attribute__((swift_name("ensureEqualBooleans(actual:expected:)")));
+ (void)ensureEqualBytesActual:(KtByte * _Nullable)actual expected:(int8_t)expected __attribute__((swift_name("ensureEqualBytes(actual:expected:)")));
+ (void)ensureEqualShortsActual:(KtShort * _Nullable)actual expected:(int16_t)expected __attribute__((swift_name("ensureEqualShorts(actual:expected:)")));
+ (void)ensureEqualIntsActual:(KtInt * _Nullable)actual expected:(int32_t)expected __attribute__((swift_name("ensureEqualInts(actual:expected:)")));
+ (void)ensureEqualLongsActual:(KtLong * _Nullable)actual expected:(int64_t)expected __attribute__((swift_name("ensureEqualLongs(actual:expected:)")));
+ (void)ensureEqualUBytesActual:(KtUByte * _Nullable)actual expected:(uint8_t)expected __attribute__((swift_name("ensureEqualUBytes(actual:expected:)")));
+ (void)ensureEqualUShortsActual:(KtUShort * _Nullable)actual expected:(uint16_t)expected __attribute__((swift_name("ensureEqualUShorts(actual:expected:)")));
+ (void)ensureEqualUIntsActual:(KtUInt * _Nullable)actual expected:(uint32_t)expected __attribute__((swift_name("ensureEqualUInts(actual:expected:)")));
+ (void)ensureEqualULongsActual:(KtULong * _Nullable)actual expected:(uint64_t)expected __attribute__((swift_name("ensureEqualULongs(actual:expected:)")));
+ (void)ensureEqualFloatsActual:(KtFloat * _Nullable)actual expected:(float)expected __attribute__((swift_name("ensureEqualFloats(actual:expected:)")));
+ (void)ensureEqualDoublesActual:(KtDouble * _Nullable)actual expected:(double)expected __attribute__((swift_name("ensureEqualDoubles(actual:expected:)")));
+ (void)emptyFun __attribute__((swift_name("emptyFun()")));
+ (NSString *)strFun __attribute__((swift_name("strFun()")));
+ (id)argsFunI:(int32_t)i l:(int64_t)l d:(double)d s:(NSString *)s __attribute__((swift_name("argsFun(i:l:d:s:)")));
+ (NSString *)funArgumentFoo:(NSString *(^)(void))foo __attribute__((swift_name("funArgument(foo:)")));
+ (id _Nullable)genericFooT:(id _Nullable)t foo:(id _Nullable (^)(id _Nullable))foo __attribute__((swift_name("genericFoo(t:foo:)")));
+ (id)fooGenericNumberR:(id)r foo:(id (^)(id))foo __attribute__((swift_name("fooGenericNumber(r:foo:)")));
+ (NSArray<id> *)varargToListArgs:(KtKotlinArray<id> *)args __attribute__((swift_name("varargToList(args:)")));
+ (NSString *)subExt:(NSString *)receiver i:(int32_t)i __attribute__((swift_name("subExt(_:i:)")));
+ (NSString *)toString:(id _Nullable)receiver __attribute__((swift_name("toString(_:)")));
+ (void)print:(id _Nullable)receiver __attribute__((swift_name("print(_:)")));
+ (id _Nullable)boxChar:(unichar)receiver __attribute__((swift_name("boxChar(_:)")));
+ (BOOL)isA:(id _Nullable)receiver __attribute__((swift_name("isA(_:)")));
+ (NSString *)iFunExt:(id<KtI>)receiver __attribute__((swift_name("iFunExt(_:)")));
+ (KtEnumeration *)passEnum __attribute__((swift_name("passEnum()")));
+ (void)receiveEnumE:(int32_t)e __attribute__((swift_name("receiveEnum(e:)")));
+ (KtEnumeration *)getValue:(int32_t)value __attribute__((swift_name("get(value:)")));
+ (KtWithCompanionAndObjectCompanion *)getCompanionObject __attribute__((swift_name("getCompanionObject()")));
+ (KtWithCompanionAndObjectNamed *)getNamedObject __attribute__((swift_name("getNamedObject()")));
+ (KtOpenClassI *)getNamedObjectInterface __attribute__((swift_name("getNamedObjectInterface()")));
+ (id)boxIc1:(int32_t)ic1 __attribute__((swift_name("box(ic1:)")));
+ (id)boxIc2:(id)ic2 __attribute__((swift_name("box(ic2:)")));
+ (id)boxIc3:(id _Nullable)ic3 __attribute__((swift_name("box(ic3:)")));
+ (NSString *)concatenateInlineClassValuesIc1:(int32_t)ic1 ic1N:(id _Nullable)ic1N ic2:(id)ic2 ic2N:(id _Nullable)ic2N ic3:(id _Nullable)ic3 ic3N:(id _Nullable)ic3N __attribute__((swift_name("concatenateInlineClassValues(ic1:ic1N:ic2:ic2N:ic3:ic3N:)")));
+ (int32_t)getValue1:(int32_t)receiver __attribute__((swift_name("getValue1(_:)")));
+ (KtInt * _Nullable)getValueOrNull1:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull1(_:)")));
+ (NSString *)getValue2:(id)receiver __attribute__((swift_name("getValue2(_:)")));
+ (NSString * _Nullable)getValueOrNull2:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull2(_:)")));
+ (KtTripleVals<id> * _Nullable)getValue3:(id _Nullable)receiver __attribute__((swift_name("getValue3(_:)")));
+ (KtTripleVals<id> * _Nullable)getValueOrNull3:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull3(_:)")));
+ (BOOL)isFrozenObj:(id)obj __attribute__((swift_name("isFrozen(obj:)")));
+ (id)kotlinLambdaBlock:(id (^)(id))block __attribute__((swift_name("kotlinLambda(block:)")));
+ (int64_t)multiplyInt:(int32_t)int_ long:(int64_t)long_ __attribute__((swift_name("multiply(int:long:)")));

/**
 @note This method converts instances of MyException, MyError to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
+ (BOOL)throwExceptionError:(BOOL)error error:(NSError * _Nullable * _Nullable)error_ __attribute__((swift_name("throwException(error:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (KtKotlinObjCErrorException * _Nullable)testSwiftThrowingMethods:(id<KtSwiftOverridableMethodsWithThrows>)methods error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("testSwiftThrowing(methods:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)testSwiftNotThrowingMethods:(id<KtSwiftOverridableMethodsWithThrows>)methods error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("testSwiftNotThrowing(methods:)")));

/**
 @note This method converts instances of MyError to errors.
 Other uncaught Kotlin exceptions are fatal.
*/
+ (BOOL)callUnitMethods:(id<KtSwiftOverridableMethodsWithThrows>)methods error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("callUnit(methods:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)callUnitCallerCaller:(id<KtMethodsWithThrowsUnitCaller>)caller methods:(id<KtMethodsWithThrows>)methods error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("callUnitCaller(caller:methods:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)testSwiftThrowingTest:(id<KtThrowsWithBridgeBase>)test flag:(BOOL)flag error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("testSwiftThrowing(test:flag:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)testSwiftNotThrowingTest:(id<KtThrowsWithBridgeBase>)test error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("testSwiftNotThrowing(test:)")));
+ (id)same:(id)receiver __attribute__((swift_name("same(_:)")));
+ (KtInt * _Nullable)callBase1:(id<KtBase1>)base1 value:(KtInt * _Nullable)value __attribute__((swift_name("call(base1:value:)")));
+ (KtInt * _Nullable)callExtendedBase1:(id<KtExtendedBase1>)extendedBase1 value:(KtInt * _Nullable)value __attribute__((swift_name("call(extendedBase1:value:)")));
+ (KtInt * _Nullable)callBase2:(id<KtBase2>)base2 value:(KtInt * _Nullable)value __attribute__((swift_name("call(base2:value:)")));
+ (int32_t)callBase3:(id)base3 value:(KtInt * _Nullable)value __attribute__((swift_name("call(base3:value:)")));
+ (int32_t)callBase23:(KtBase23 *)base23 value:(KtInt * _Nullable)value __attribute__((swift_name("call(base23:value:)")));
+ (id<KtTransform>)createTransformDecimalStringToInt __attribute__((swift_name("createTransformDecimalStringToInt()")));
+ (BOOL)runUnitBlockBlock:(void (^)(void))block __attribute__((swift_name("runUnitBlock(block:)")));
+ (void (^)(void))asUnitBlockBlock:(id _Nullable (^)(void))block __attribute__((swift_name("asUnitBlock(block:)")));
+ (BOOL)runNothingBlockBlock:(void (^)(void))block __attribute__((swift_name("runNothingBlock(block:)")));
+ (void (^)(void))asNothingBlockBlock:(id _Nullable (^)(void))block __attribute__((swift_name("asNothingBlock(block:)")));
+ (void (^ _Nullable)(void))getNullBlock __attribute__((swift_name("getNullBlock()")));
+ (BOOL)isBlockNullBlock:(void (^ _Nullable)(void))block __attribute__((swift_name("isBlockNull(block:)")));
+ (BOOL)isFunctionObj:(id _Nullable)obj __attribute__((swift_name("isFunction(obj:)")));
+ (BOOL)isFunction0Obj:(id _Nullable)obj __attribute__((swift_name("isFunction0(obj:)")));
+ (void)takeForwardDeclaredClassObj:(ForwardDeclaredClass *)obj __attribute__((swift_name("takeForwardDeclaredClass(obj:)")));
+ (void)takeForwardDeclaredProtocolObj:(id<ForwardDeclared>)obj __attribute__((swift_name("takeForwardDeclaredProtocol(obj:)")));
+ (void)error __attribute__((swift_name("error()"))) __attribute__((unavailable("error")));
+ (void)warning __attribute__((swift_name("warning()"))) __attribute__((deprecated("warning")));
+ (void)gc __attribute__((swift_name("gc()")));
+ (void)testRememberNewObjectTest:(id<KtTestRememberNewObject>)test __attribute__((swift_name("testRememberNewObject(test:)")));
+ (BOOL)testClassTypeCheckX:(id)x __attribute__((swift_name("testClassTypeCheck(x:)")));
+ (BOOL)testInterfaceTypeCheckX:(id)x __attribute__((swift_name("testInterfaceTypeCheck(x:)")));
+ (int32_t)testAbstractInterfaceCallX:(id<KtIAbstractInterface>)x __attribute__((swift_name("testAbstractInterfaceCall(x:)")));
+ (int32_t)testAbstractInterfaceCall2X:(id<KtIAbstractInterface2>)x __attribute__((swift_name("testAbstractInterfaceCall2(x:)")));
+ (void)fooA:(KtKotlinAtomicReference<id> *)a __attribute__((swift_name("foo(a:)")));

/**
 @note This method converts all Kotlin exceptions to errors.
*/
+ (BOOL)testGH3825Gh3825:(id<KtGH3825>)gh3825 error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("testGH3825(gh3825:)")));
+ (NSDictionary<KtBoolean *, NSString *> *)mapBoolean2String __attribute__((swift_name("mapBoolean2String()")));
+ (NSDictionary<KtByte *, KtShort *> *)mapByte2Short __attribute__((swift_name("mapByte2Short()")));
+ (NSDictionary<KtShort *, KtByte *> *)mapShort2Byte __attribute__((swift_name("mapShort2Byte()")));
+ (NSDictionary<KtInt *, KtLong *> *)mapInt2Long __attribute__((swift_name("mapInt2Long()")));
+ (NSDictionary<KtLong *, KtLong *> *)mapLong2Long __attribute__((swift_name("mapLong2Long()")));
+ (NSDictionary<KtUByte *, KtBoolean *> *)mapUByte2Boolean __attribute__((swift_name("mapUByte2Boolean()")));
+ (NSDictionary<KtUShort *, KtByte *> *)mapUShort2Byte __attribute__((swift_name("mapUShort2Byte()")));
+ (NSDictionary<KtUInt *, KtLong *> *)mapUInt2Long __attribute__((swift_name("mapUInt2Long()")));
+ (NSDictionary<KtULong *, KtLong *> *)mapULong2Long __attribute__((swift_name("mapULong2Long()")));
+ (NSDictionary<KtFloat *, KtFloat *> *)mapFloat2Float __attribute__((swift_name("mapFloat2Float()")));
+ (NSDictionary<KtDouble *, NSString *> *)mapDouble2String __attribute__((swift_name("mapDouble2String()")));
+ (KtMutableDictionary<KtBoolean *, NSString *> *)mutBoolean2String __attribute__((swift_name("mutBoolean2String()")));
+ (KtMutableDictionary<KtByte *, KtShort *> *)mutByte2Short __attribute__((swift_name("mutByte2Short()")));
+ (KtMutableDictionary<KtShort *, KtByte *> *)mutShort2Byte __attribute__((swift_name("mutShort2Byte()")));
+ (KtMutableDictionary<KtInt *, KtLong *> *)mutInt2Long __attribute__((swift_name("mutInt2Long()")));
+ (KtMutableDictionary<KtLong *, KtLong *> *)mutLong2Long __attribute__((swift_name("mutLong2Long()")));
+ (KtMutableDictionary<KtUByte *, KtBoolean *> *)mutUByte2Boolean __attribute__((swift_name("mutUByte2Boolean()")));
+ (KtMutableDictionary<KtUShort *, KtByte *> *)mutUShort2Byte __attribute__((swift_name("mutUShort2Byte()")));
+ (KtMutableDictionary<KtUInt *, KtLong *> *)mutUInt2Long __attribute__((swift_name("mutUInt2Long()")));
+ (KtMutableDictionary<KtULong *, KtLong *> *)mutULong2Long __attribute__((swift_name("mutULong2Long()")));
+ (KtMutableDictionary<KtFloat *, KtFloat *> *)mutFloat2Float __attribute__((swift_name("mutFloat2Float()")));
+ (KtMutableDictionary<KtDouble *, NSString *> *)mutDouble2String __attribute__((swift_name("mutDouble2String()")));
+ (void)callFoo_FakeOverrideInInterfaceObj:(id<KtBar_FakeOverrideInInterface>)obj __attribute__((swift_name("callFoo_FakeOverrideInInterface(obj:)")));
@property (class, readonly) double dbl __attribute__((swift_name("dbl")));
@property (class, readonly) float flt __attribute__((swift_name("flt")));
@property (class, readonly) int32_t integer __attribute__((swift_name("integer")));
@property (class, readonly) int64_t longInt __attribute__((swift_name("longInt")));
@property (class) int32_t intVar __attribute__((swift_name("intVar")));
@property (class) NSString *str __attribute__((swift_name("str")));
@property (class) id strAsAny __attribute__((swift_name("strAsAny")));
@property (class) id minDoubleVal __attribute__((swift_name("minDoubleVal")));
@property (class) id maxDoubleVal __attribute__((swift_name("maxDoubleVal")));
@property (class, readonly) double nanDoubleVal __attribute__((swift_name("nanDoubleVal")));
@property (class, readonly) float nanFloatVal __attribute__((swift_name("nanFloatVal")));
@property (class, readonly) double infDoubleVal __attribute__((swift_name("infDoubleVal")));
@property (class, readonly) float infFloatVal __attribute__((swift_name("infFloatVal")));
@property (class, readonly) BOOL boolVal __attribute__((swift_name("boolVal")));
@property (class, readonly) id boolAnyVal __attribute__((swift_name("boolAnyVal")));
@property (class, readonly) NSArray<id> *numbersList __attribute__((swift_name("numbersList")));
@property (class, readonly) NSArray<id> *anyList __attribute__((swift_name("anyList")));
@property (class) id lateinitIntVar __attribute__((swift_name("lateinitIntVar")));
@property (class, readonly) NSString *lazyVal __attribute__((swift_name("lazyVal")));
@property (class) KtKotlinArray<NSString *> *delegatedGlobalArray __attribute__((swift_name("delegatedGlobalArray")));
@property (class, readonly) NSArray<NSString *> *delegatedList __attribute__((swift_name("delegatedList")));
@property (class, readonly) id _Nullable nullVal __attribute__((swift_name("nullVal")));
@property (class) NSString * _Nullable nullVar __attribute__((swift_name("nullVar")));
@property (class) id anyValue __attribute__((swift_name("anyValue")));
@property (class, readonly) KtInt *(^sumLambda)(KtInt *, KtInt *) __attribute__((swift_name("sumLambda")));
@property (class, readonly) int32_t PROPERTY_NAME_MUST_NOT_BE_ALTERED_BY_SWIFT __attribute__((swift_name("PROPERTY_NAME_MUST_NOT_BE_ALTERED_BY_SWIFT")));
@property (class, readonly) id _Nullable errorVal __attribute__((swift_name("errorVal"))) __attribute__((unavailable("error")));
@property (class) id _Nullable errorVar __attribute__((swift_name("errorVar"))) __attribute__((unavailable("error")));
@property (class, readonly) id _Nullable warningVal __attribute__((swift_name("warningVal"))) __attribute__((deprecated("warning")));
@property (class) id _Nullable warningVar __attribute__((swift_name("warningVar"))) __attribute__((deprecated("warning")));
@property (class) int32_t gh3525BaseInitCount __attribute__((swift_name("gh3525BaseInitCount")));
@property (class) int32_t gh3525InitCount __attribute__((swift_name("gh3525InitCount")));
@end;

