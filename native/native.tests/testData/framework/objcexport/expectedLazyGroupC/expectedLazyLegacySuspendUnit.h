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
@property (readonly) NSMutableArray<KtKotlinWeakReference<id> *> *weakRefs __attribute__((swift_name("weakRefs")));
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
@interface KtNoAutoreleaseEnum : KtKotlinEnum<KtNoAutoreleaseEnum *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtNoAutoreleaseEnum *entry __attribute__((swift_name("entry")));
+ (KtKotlinArray<KtNoAutoreleaseEnum *> *)values __attribute__((swift_name("values()")));
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
@interface KtObjCNameObjCEnum : KtKotlinEnum<KtObjCNameObjCEnum *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtObjCNameObjCEnum *objcOne __attribute__((swift_name("swiftOne")));
@property (class, readonly) KtObjCNameObjCEnum *objcTwo __attribute__((swift_name("companion")));
@property (class, readonly) KtObjCNameObjCEnum *objcThree __attribute__((swift_name("swiftThree")));
+ (KtKotlinArray<KtObjCNameObjCEnum *> *)values __attribute__((swift_name("values()")));
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
@interface KtRecList<T> : KtBase
- (instancetype)initWithValue:(NSArray<id> *)value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
@property (readonly) NSArray<id> *value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecFunc")))
@interface KtRecFunc<T> : KtBase
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
- (void)throwErrorWithCompletionHandler:(void (^)(KtKotlinUnit * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("throwError(completionHandler:)")));
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
- (KtKotlinArray<NSString *> *)getValueThisRef:(KtKotlinNothing * _Nullable)thisRef property:(id<KtKotlinKProperty>)property __attribute__((swift_name("getValue(thisRef:property:)")));
- (void)setValueThisRef:(KtKotlinNothing * _Nullable)thisRef property:(id<KtKotlinKProperty>)property value:(KtKotlinArray<NSString *> *)value __attribute__((swift_name("setValue(thisRef:property:value:)")));
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
@interface KtEnumeration : KtKotlinEnum<KtEnumeration *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtEnumeration *answer __attribute__((swift_name("answer")));
@property (class, readonly) KtEnumeration *year __attribute__((swift_name("year")));
@property (class, readonly) KtEnumeration *temperature __attribute__((swift_name("temperature")));
+ (KtKotlinArray<KtEnumeration *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<KtEnumeration *> *entries __attribute__((swift_name("entries")));
@property (readonly) int32_t enumValue __attribute__((swift_name("enumValue")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TripleVals")))
@interface KtTripleVals<T> : KtBase
- (instancetype)initWithFirst:(T _Nullable)first second:(T _Nullable)second third:(T _Nullable)third __attribute__((swift_name("init(first:second:third:)"))) __attribute__((objc_designated_initializer));
- (KtTripleVals<T> *)doCopyFirst:(T _Nullable)first second:(T _Nullable)second third:(T _Nullable)third __attribute__((swift_name("doCopy(first:second:third:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) T _Nullable first __attribute__((swift_name("first")));
@property (readonly) T _Nullable second __attribute__((swift_name("second")));
@property (readonly) T _Nullable third __attribute__((swift_name("third")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TripleVars")))
@interface KtTripleVars<T> : KtBase
- (instancetype)initWithFirst:(T _Nullable)first second:(T _Nullable)second third:(T _Nullable)third __attribute__((swift_name("init(first:second:third:)"))) __attribute__((objc_designated_initializer));
- (KtTripleVars<T> *)doCopyFirst:(T _Nullable)first second:(T _Nullable)second third:(T _Nullable)third __attribute__((swift_name("doCopy(first:second:third:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property T _Nullable first __attribute__((swift_name("first")));
@property T _Nullable second __attribute__((swift_name("second")));
@property T _Nullable third __attribute__((swift_name("third")));
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
__attribute__((swift_name("WithGenericDeeplyNestedType")))
@interface KtWithGenericDeeplyNestedType<T> : KtBase
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
@interface KtTransformInheritingDefault<T> : KtBase <KtTransformWithDefault>
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
@interface KtTestInvalidIdentifiersE : KtKotlinEnum<KtTestInvalidIdentifiersE *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtTestInvalidIdentifiersE *_4s __attribute__((swift_name("_4s")));
@property (class, readonly) KtTestInvalidIdentifiersE *_5s __attribute__((swift_name("_5s")));
@property (class, readonly) KtTestInvalidIdentifiersE *__ __attribute__((swift_name("__")));
@property (class, readonly) KtTestInvalidIdentifiersE *__ __attribute__((swift_name("__")));
+ (KtKotlinArray<KtTestInvalidIdentifiersE *> *)values __attribute__((swift_name("values()")));
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
+ (void)fooA:(KtKotlinAtomicReference<id> *)a __attribute__((swift_name("foo(a:)")));
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
+ (KtTripleVals<id> * _Nullable)getValue3:(id _Nullable)receiver __attribute__((swift_name("getValue3(_:)")));
+ (KtInt * _Nullable)getValueOrNull1:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull1(_:)")));
+ (NSString * _Nullable)getValueOrNull2:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull2(_:)")));
+ (KtTripleVals<id> * _Nullable)getValueOrNull3:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull3(_:)")));
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
+ (NSArray<id> *)varargToListArgs:(KtKotlinArray<id> *)args __attribute__((swift_name("varargToList(args:)")));
+ (void)warning __attribute__((swift_name("warning()"))) __attribute__((deprecated("warning")));
@property (class, readonly) int32_t PROPERTY_NAME_MUST_NOT_BE_ALTERED_BY_SWIFT __attribute__((swift_name("PROPERTY_NAME_MUST_NOT_BE_ALTERED_BY_SWIFT")));
@property (class, readonly) NSArray<id> *anyList __attribute__((swift_name("anyList")));
@property (class) id anyValue __attribute__((swift_name("anyValue")));
@property (class, readonly) id boolAnyVal __attribute__((swift_name("boolAnyVal")));
@property (class, readonly) BOOL boolVal __attribute__((swift_name("boolVal")));
@property (class, readonly) double dbl __attribute__((swift_name("dbl")));
@property (class) KtKotlinArray<NSString *> *delegatedGlobalArray __attribute__((swift_name("delegatedGlobalArray")));
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
@interface KtInvariantSuper<T> : KtBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Invariant")))
@interface KtInvariant<T> : KtInvariantSuper<T>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("OutVariantSuper")))
@interface KtOutVariantSuper<__covariant T> : KtBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OutVariant")))
@interface KtOutVariant<__covariant T> : KtOutVariantSuper<T>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("InVariantSuper")))
@interface KtInVariantSuper<__contravariant T> : KtBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InVariant")))
@interface KtInVariant<__contravariant T> : KtInVariantSuper<T>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VectorKt")))
@interface KtVectorKt : KtBase
+ (id _Nullable)createNullableVectorIsNull:(BOOL)isNull __attribute__((swift_name("createNullableVector(isNull:)")));
+ (float __attribute__((__vector_size__(16))))createVectorF0:(float)f0 f1:(float)f1 f2:(float)f2 f3:(float)f3 __attribute__((swift_name("createVector(f0:f1:f2:f3:)")));
+ (float __attribute__((__vector_size__(16))))createVectorI0:(int32_t)i0 i1:(int32_t)i1 i2:(int32_t)i2 i3:(int32_t)i3 __attribute__((swift_name("createVector(i0:i1:i2:i3:)")));
+ (int32_t)sumNullableVectorInt:(id _Nullable)receiver __attribute__((swift_name("sumNullableVectorInt(_:)")));
+ (float)sumVectorFloat:(float __attribute__((__vector_size__(16))))receiver __attribute__((swift_name("sumVectorFloat(_:)")));
+ (int32_t)sumVectorInt:(float __attribute__((__vector_size__(16))))receiver __attribute__((swift_name("sumVectorInt(_:)")));
@property (class) float __attribute__((__vector_size__(16))) vector __attribute__((swift_name("vector")));
@end

