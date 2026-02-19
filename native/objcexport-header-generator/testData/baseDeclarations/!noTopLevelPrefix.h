__attribute__((swift_name("KotlinBase")))
@interface Base : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end
@interface Base (BaseCopying) <NSCopying>
@end
__attribute__((swift_name("KotlinMutableSet")))
@interface MutableSet<ObjectType> : NSMutableSet<ObjectType>
@end
__attribute__((swift_name("KotlinMutableDictionary")))
@interface MutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end
@interface NSError (NSErrorKotlinException)
@property (readonly) id _Nullable kotlinException;
@end
__attribute__((swift_name("KotlinNumber")))
@interface Number : NSNumber
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
@interface Byte : Number
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end
__attribute__((swift_name("KotlinUByte")))
@interface UByte : Number
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end
__attribute__((swift_name("KotlinShort")))
@interface Short : Number
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end
__attribute__((swift_name("KotlinUShort")))
@interface UShort : Number
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end
__attribute__((swift_name("KotlinInt")))
@interface Int : Number
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end
__attribute__((swift_name("KotlinUInt")))
@interface UInt : Number
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end
__attribute__((swift_name("KotlinLong")))
@interface Long : Number
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end
__attribute__((swift_name("KotlinULong")))
@interface ULong : Number
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end
__attribute__((swift_name("KotlinFloat")))
@interface Float : Number
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end
__attribute__((swift_name("KotlinDouble")))
@interface Double : Number
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end
__attribute__((swift_name("KotlinBoolean")))
@interface Boolean : Number
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end
