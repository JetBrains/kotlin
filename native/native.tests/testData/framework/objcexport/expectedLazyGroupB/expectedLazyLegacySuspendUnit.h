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
@interface KtKT43780Enum : KtKotlinEnum<KtKT43780Enum *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) KtKT43780Enum *otherEntry __attribute__((swift_name("otherEntry")));
@property (class, readonly) KtKT43780Enum *companion __attribute__((swift_name("companion")));
+ (KtKotlinArray<KtKT43780Enum *> *)values __attribute__((swift_name("values()")));
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
- (void)fillWithCompletionHandler:(void (^)(KtKotlinUnit * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("fill(completionHandler:)")));
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

