#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class StringBuilder, CharArray, CharIterator;

@protocol CharSequence, Appendable, Iterator;

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
@property (class, readonly) StringBuilder *a __attribute__((swift_name("a")));
@end

@protocol CharSequence
@required
- (unichar)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id)subSequenceStartIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("subSequence(startIndex:endIndex:)")));
@property (readonly) int32_t length __attribute__((swift_name("length")));
@end

@protocol Appendable
@required
- (id<Appendable>)appendValue:(unichar)value __attribute__((swift_name("append(value:)")));
- (id<Appendable>)appendValue_:(id _Nullable)value __attribute__((swift_name("append(value_:)")));
- (id<Appendable>)appendValue:(id _Nullable)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("append(value:startIndex:endIndex:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface StringBuilder : Base <CharSequence, Appendable>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithContent:(id)content __attribute__((swift_name("init(content:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCapacity:(int32_t)capacity __attribute__((swift_name("init(capacity:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithContent_:(NSString *)content __attribute__((swift_name("init(content_:)"))) __attribute__((objc_designated_initializer));
- (StringBuilder *)appendValue__:(id _Nullable)value __attribute__((swift_name("append(value__:)")));
- (StringBuilder *)appendValue___:(BOOL)value __attribute__((swift_name("append(value___:)")));
- (StringBuilder *)appendValue____:(int8_t)value __attribute__((swift_name("append(value____:)")));
- (StringBuilder *)appendValue:(unichar)value __attribute__((swift_name("append(value:)")));
- (StringBuilder *)appendValue_____:(CharArray *)value __attribute__((swift_name("append(value_____:)")));
- (StringBuilder *)appendValue_:(id _Nullable)value __attribute__((swift_name("append(value_:)")));
- (StringBuilder *)appendValue______:(double)value __attribute__((swift_name("append(value______:)")));
- (StringBuilder *)appendValue_______:(float)value __attribute__((swift_name("append(value_______:)")));
- (StringBuilder *)appendValue________:(int32_t)value __attribute__((swift_name("append(value________:)")));
- (StringBuilder *)appendValue_________:(int64_t)value __attribute__((swift_name("append(value_________:)")));
- (StringBuilder *)appendValue__________:(int16_t)value __attribute__((swift_name("append(value__________:)")));
- (StringBuilder *)appendValue___________:(NSString * _Nullable)value __attribute__((swift_name("append(value___________:)")));
- (StringBuilder *)appendValue:(id _Nullable)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("append(value:startIndex:endIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (StringBuilder *)appendRangeValue:(CharArray *)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("appendRange(value:startIndex:endIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (StringBuilder *)appendRangeValue:(id)value startIndex:(int32_t)startIndex endIndex_:(int32_t)endIndex __attribute__((swift_name("appendRange(value:startIndex:endIndex_:)")));
- (int32_t)capacity __attribute__((swift_name("capacity()")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (StringBuilder *)deleteAtIndex:(int32_t)index __attribute__((swift_name("deleteAt(index:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (StringBuilder *)deleteRangeStartIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("deleteRange(startIndex:endIndex:)")));
- (void)ensureCapacityMinimumCapacity:(int32_t)minimumCapacity __attribute__((swift_name("ensureCapacity(minimumCapacity:)")));
- (unichar)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (int32_t)indexOfString:(NSString *)string __attribute__((swift_name("indexOf(string:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (int32_t)indexOfString:(NSString *)string startIndex:(int32_t)startIndex __attribute__((swift_name("indexOf(string:startIndex:)")));
- (StringBuilder *)insertIndex:(int32_t)index value:(id _Nullable)value __attribute__((swift_name("insert(index:value:)")));
- (StringBuilder *)insertIndex:(int32_t)index value_:(BOOL)value __attribute__((swift_name("insert(index:value_:)")));
- (StringBuilder *)insertIndex:(int32_t)index value__:(int8_t)value __attribute__((swift_name("insert(index:value__:)")));
- (StringBuilder *)insertIndex:(int32_t)index value___:(unichar)value __attribute__((swift_name("insert(index:value___:)")));
- (StringBuilder *)insertIndex:(int32_t)index value____:(CharArray *)value __attribute__((swift_name("insert(index:value____:)")));
- (StringBuilder *)insertIndex:(int32_t)index value_____:(id _Nullable)value __attribute__((swift_name("insert(index:value_____:)")));
- (StringBuilder *)insertIndex:(int32_t)index value______:(double)value __attribute__((swift_name("insert(index:value______:)")));
- (StringBuilder *)insertIndex:(int32_t)index value_______:(float)value __attribute__((swift_name("insert(index:value_______:)")));
- (StringBuilder *)insertIndex:(int32_t)index value________:(int32_t)value __attribute__((swift_name("insert(index:value________:)")));
- (StringBuilder *)insertIndex:(int32_t)index value_________:(int64_t)value __attribute__((swift_name("insert(index:value_________:)")));
- (StringBuilder *)insertIndex:(int32_t)index value__________:(int16_t)value __attribute__((swift_name("insert(index:value__________:)")));
- (StringBuilder *)insertIndex:(int32_t)index value___________:(NSString * _Nullable)value __attribute__((swift_name("insert(index:value___________:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (StringBuilder *)insertRangeIndex:(int32_t)index value:(CharArray *)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("insertRange(index:value:startIndex:endIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (StringBuilder *)insertRangeIndex:(int32_t)index value:(id)value startIndex:(int32_t)startIndex endIndex_:(int32_t)endIndex __attribute__((swift_name("insertRange(index:value:startIndex:endIndex_:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (int32_t)lastIndexOfString:(NSString *)string __attribute__((swift_name("lastIndexOf(string:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (int32_t)lastIndexOfString:(NSString *)string startIndex:(int32_t)startIndex __attribute__((swift_name("lastIndexOf(string:startIndex:)")));
- (StringBuilder *)reverse __attribute__((swift_name("reverse()")));
- (void)setIndex:(int32_t)index value:(unichar)value __attribute__((swift_name("set(index:value:)")));
- (void)setLengthNewLength:(int32_t)newLength __attribute__((swift_name("setLength(newLength:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (StringBuilder *)setRangeStartIndex:(int32_t)startIndex endIndex:(int32_t)endIndex value:(NSString *)value __attribute__((swift_name("setRange(startIndex:endIndex:value:)")));
- (id)subSequenceStartIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("subSequence(startIndex:endIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (NSString *)substringStartIndex:(int32_t)startIndex __attribute__((swift_name("substring(startIndex:)")));
- (NSString *)substringStartIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("substring(startIndex:endIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (void)toCharArrayDestination:(CharArray *)destination destinationOffset:(int32_t)destinationOffset startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("toCharArray(destination:destinationOffset:startIndex:endIndex:)")));
- (NSString *)description __attribute__((swift_name("description()")));
- (void)trimToSize __attribute__((swift_name("trimToSize()")));
@property (readonly) int32_t length __attribute__((swift_name("length")));
@end

__attribute__((objc_subclassing_restricted))
@interface CharArray : Base
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(id _Nullable (^)(id _Nullable))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (unichar)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (CharIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(unichar)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

@protocol Iterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

@interface CharIterator : Base <Iterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id)next __attribute__((swift_name("next()")));
- (unichar)nextChar __attribute__((swift_name("nextChar()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
