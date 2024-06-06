#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class Instant, ClockCompanion, ClockSystem, DateTimePeriodCompanion, DateTimePeriod, DatePeriodCompanion, DatePeriod, KSerializer<T0>, KotlinThrowable, KotlinArray<T>, KotlinException, KotlinRuntimeException, DateTimeUnitCompanion, DateTimeUnit, DateTimeUnitMonthBased, DateTimeUnitDayBased, DateTimeUnitTimeBased, DateTimeUnitDateBasedCompanion, DateTimeUnitDateBased, DateTimeUnitDayBasedCompanion, DateTimeUnitMonthBasedCompanion, DateTimeUnitTimeBasedCompanion, KotlinEnumCompanion, KotlinEnum<E>, DayOfWeek, TimeZoneCompanion, LocalDateTime, TimeZone, UtcOffset, FixedOffsetTimeZoneCompanion, FixedOffsetTimeZone, KotlinIllegalArgumentException, InstantCompanion, Month, LocalDateCompanion, LocalDate, LocalDateFormats, LocalTime, LocalDateTimeCompanion, LocalDateTimeFormats, LocalTimeCompanion, LocalTimeFormats, UtcOffsetCompanion, UtcOffsetFormats, AmPmMarker, DateTimeComponentsCompanion, DateTimeComponentsFormats, DateTimeFormatCompanion, Padding, DayOfWeekNames, MonthNames, DayOfWeekNamesCompanion, MonthNamesCompanion, AbstractPolymorphicSerializer<T0>, DateBasedDateTimeUnitSerializer, DeserializationStrategy<T0>, CompositeDecoder, SerializationStrategy<T0>, Encoder, SerialDescriptor, DatePeriodComponentSerializer, Decoder, DatePeriodIso8601Serializer, DateTimePeriodComponentSerializer, DateTimePeriodIso8601Serializer, DateTimeUnitSerializer, DayBasedDateTimeUnitSerializer, DayOfWeekSerializer, FixedOffsetTimeZoneSerializer, InstantComponentSerializer, InstantIso8601Serializer, LocalDateComponentSerializer, LocalDateIso8601Serializer, LocalDateTimeComponentSerializer, LocalDateTimeIso8601Serializer, LocalTimeComponentSerializer, LocalTimeIso8601Serializer, MonthBasedDateTimeUnitSerializer, MonthSerializer, TimeBasedDateTimeUnitSerializer, TimeZoneSerializer, UtcOffsetSerializer, NSDate, NSDateComponents, NSTimeZone, DateTimeComponents, KotlinUnit;

@protocol Clock, KotlinComparable, DateTimeFormat, DateTimeFormatBuilderWithDate, DateTimeFormatBuilderWithDateTime, DateTimeFormatBuilderWithTime, DateTimeFormatBuilderWithUtcOffset, DateTimeFormatBuilderWithDateTimeComponents, KotlinAppendable, DateTimeFormatBuilder, KotlinKClass, KotlinTimeSourceWithComparableMarks, KotlinIterator, KotlinKDeclarationContainer, KotlinKAnnotatedElement, KotlinKClassifier, KotlinTimeMark, KotlinTimeSource;

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
- (KSerializer<DatePeriod *> *)serializer __attribute__((swift_name("serializer()")));
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
- (KSerializer<DateTimePeriod *> *)serializer __attribute__((swift_name("serializer()")));
@end

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
- (KSerializer<DateTimeUnit *> *)serializer __attribute__((swift_name("serializer()")));
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
- (KSerializer<DateTimeUnitDateBased *> *)serializer __attribute__((swift_name("serializer()")));
@end

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
- (KSerializer<DateTimeUnitDayBased *> *)serializer __attribute__((swift_name("serializer()")));
@end

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
- (KSerializer<DateTimeUnitMonthBased *> *)serializer __attribute__((swift_name("serializer()")));
@end

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
- (KSerializer<DateTimeUnitTimeBased *> *)serializer __attribute__((swift_name("serializer()")));
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

@interface TimeZone : Base
@property (class, readonly, getter=companion) TimeZoneCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (Instant *)toInstant:(LocalDateTime *)receiver __attribute__((swift_name("toInstant(_:)")));
- (LocalDateTime *)toLocalDateTime:(Instant *)receiver __attribute__((swift_name("toLocalDateTime(_:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@end

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
- (KSerializer<FixedOffsetTimeZone *> *)serializer __attribute__((swift_name("serializer()")));
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
- (KSerializer<Instant *> *)serializer __attribute__((swift_name("serializer()")));
@property (readonly) Instant *DISTANT_FUTURE __attribute__((swift_name("DISTANT_FUTURE")));
@property (readonly) Instant *DISTANT_PAST __attribute__((swift_name("DISTANT_PAST")));
@end

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
- (KSerializer<LocalDate *> *)serializer __attribute__((swift_name("serializer()")));
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
- (KSerializer<LocalDateTime *> *)serializer __attribute__((swift_name("serializer()")));
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
- (KSerializer<LocalTime *> *)serializer __attribute__((swift_name("serializer()")));
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
- (KSerializer<TimeZone *> *)serializer __attribute__((swift_name("serializer()")));
@property (readonly) FixedOffsetTimeZone *UTC __attribute__((swift_name("UTC")));
@property (readonly) NSSet<NSString *> *availableZoneIds __attribute__((swift_name("availableZoneIds")));
@end

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
- (KSerializer<UtcOffset *> *)serializer __attribute__((swift_name("serializer()")));
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

__attribute__((objc_subclassing_restricted))
@interface AbstractPolymorphicSerializer<T0> : Base
@end

__attribute__((objc_subclassing_restricted))
@interface DateBasedDateTimeUnitSerializer : AbstractPolymorphicSerializer<DateTimeUnitDateBased *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dateBasedDateTimeUnitSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateBasedDateTimeUnitSerializer *shared __attribute__((swift_name("shared")));
- (DeserializationStrategy<DateTimeUnitDateBased *> * _Nullable)findPolymorphicSerializerOrNullDecoder:(CompositeDecoder *)decoder klassName:(NSString * _Nullable)klassName __attribute__((swift_name("findPolymorphicSerializerOrNull(decoder:klassName:)")));
- (SerializationStrategy<DateTimeUnitDateBased *> * _Nullable)findPolymorphicSerializerOrNullEncoder:(Encoder *)encoder value:(DateTimeUnitDateBased *)value __attribute__((swift_name("findPolymorphicSerializerOrNull(encoder:value:)")));
@property (readonly) id<KotlinKClass> baseClass __attribute__((swift_name("baseClass")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface KSerializer<T0> : Base
@end

__attribute__((objc_subclassing_restricted))
@interface DatePeriodComponentSerializer : KSerializer<DatePeriod *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)datePeriodComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DatePeriodComponentSerializer *shared __attribute__((swift_name("shared")));
- (DatePeriod *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(DatePeriod *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DatePeriodIso8601Serializer : KSerializer<DatePeriod *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)datePeriodIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DatePeriodIso8601Serializer *shared __attribute__((swift_name("shared")));
- (DatePeriod *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(DatePeriod *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimePeriodComponentSerializer : KSerializer<DateTimePeriod *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dateTimePeriodComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimePeriodComponentSerializer *shared __attribute__((swift_name("shared")));
- (DateTimePeriod *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(DateTimePeriod *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimePeriodIso8601Serializer : KSerializer<DateTimePeriod *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dateTimePeriodIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimePeriodIso8601Serializer *shared __attribute__((swift_name("shared")));
- (DateTimePeriod *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(DateTimePeriod *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DateTimeUnitSerializer : AbstractPolymorphicSerializer<DateTimeUnit *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dateTimeUnitSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DateTimeUnitSerializer *shared __attribute__((swift_name("shared")));
- (DeserializationStrategy<DateTimeUnit *> * _Nullable)findPolymorphicSerializerOrNullDecoder:(CompositeDecoder *)decoder klassName:(NSString * _Nullable)klassName __attribute__((swift_name("findPolymorphicSerializerOrNull(decoder:klassName:)")));
- (SerializationStrategy<DateTimeUnit *> * _Nullable)findPolymorphicSerializerOrNullEncoder:(Encoder *)encoder value:(DateTimeUnit *)value __attribute__((swift_name("findPolymorphicSerializerOrNull(encoder:value:)")));
@property (readonly) id<KotlinKClass> baseClass __attribute__((swift_name("baseClass")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DayBasedDateTimeUnitSerializer : KSerializer<DateTimeUnitDayBased *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dayBasedDateTimeUnitSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DayBasedDateTimeUnitSerializer *shared __attribute__((swift_name("shared")));
- (DateTimeUnitDayBased *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(DateTimeUnitDayBased *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface DayOfWeekSerializer : KSerializer<DayOfWeek *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dayOfWeekSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) DayOfWeekSerializer *shared __attribute__((swift_name("shared")));
- (DayOfWeek *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(DayOfWeek *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface FixedOffsetTimeZoneSerializer : KSerializer<FixedOffsetTimeZone *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)fixedOffsetTimeZoneSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) FixedOffsetTimeZoneSerializer *shared __attribute__((swift_name("shared")));
- (FixedOffsetTimeZone *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(FixedOffsetTimeZone *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface InstantComponentSerializer : KSerializer<Instant *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)instantComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InstantComponentSerializer *shared __attribute__((swift_name("shared")));
- (Instant *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(Instant *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface InstantIso8601Serializer : KSerializer<Instant *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)instantIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InstantIso8601Serializer *shared __attribute__((swift_name("shared")));
- (Instant *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(Instant *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalDateComponentSerializer : KSerializer<LocalDate *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localDateComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateComponentSerializer *shared __attribute__((swift_name("shared")));
- (LocalDate *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(LocalDate *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalDateIso8601Serializer : KSerializer<LocalDate *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localDateIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateIso8601Serializer *shared __attribute__((swift_name("shared")));
- (LocalDate *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(LocalDate *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalDateTimeComponentSerializer : KSerializer<LocalDateTime *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localDateTimeComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateTimeComponentSerializer *shared __attribute__((swift_name("shared")));
- (LocalDateTime *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(LocalDateTime *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalDateTimeIso8601Serializer : KSerializer<LocalDateTime *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localDateTimeIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalDateTimeIso8601Serializer *shared __attribute__((swift_name("shared")));
- (LocalDateTime *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(LocalDateTime *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalTimeComponentSerializer : KSerializer<LocalTime *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localTimeComponentSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalTimeComponentSerializer *shared __attribute__((swift_name("shared")));
- (LocalTime *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(LocalTime *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface LocalTimeIso8601Serializer : KSerializer<LocalTime *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)localTimeIso8601Serializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) LocalTimeIso8601Serializer *shared __attribute__((swift_name("shared")));
- (LocalTime *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(LocalTime *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface MonthBasedDateTimeUnitSerializer : KSerializer<DateTimeUnitMonthBased *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)monthBasedDateTimeUnitSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) MonthBasedDateTimeUnitSerializer *shared __attribute__((swift_name("shared")));
- (DateTimeUnitMonthBased *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(DateTimeUnitMonthBased *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface MonthSerializer : KSerializer<Month *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)monthSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) MonthSerializer *shared __attribute__((swift_name("shared")));
- (Month *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(Month *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface TimeBasedDateTimeUnitSerializer : KSerializer<DateTimeUnitTimeBased *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)timeBasedDateTimeUnitSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) TimeBasedDateTimeUnitSerializer *shared __attribute__((swift_name("shared")));
- (DateTimeUnitTimeBased *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(DateTimeUnitTimeBased *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface TimeZoneSerializer : KSerializer<TimeZone *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)timeZoneSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) TimeZoneSerializer *shared __attribute__((swift_name("shared")));
- (TimeZone *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(TimeZone *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((objc_subclassing_restricted))
@interface UtcOffsetSerializer : KSerializer<UtcOffset *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)utcOffsetSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) UtcOffsetSerializer *shared __attribute__((swift_name("shared")));
- (UtcOffset *)deserializeDecoder:(Decoder *)decoder __attribute__((swift_name("deserialize(decoder:)")));
- (void)serializeEncoder:(Encoder *)encoder value:(UtcOffset *)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) SerialDescriptor *descriptor __attribute__((swift_name("descriptor")));
@end

@interface DatePeriod (Extensions)
- (DatePeriod *)plusOther:(DatePeriod *)other __attribute__((swift_name("plus(other:)")));
@end

@interface DateTimePeriod (Extensions)
- (DateTimePeriod *)plusOther_:(DateTimePeriod *)other __attribute__((swift_name("plus(other_:)")));
@end

@interface DayOfWeek (Extensions)
@property (readonly) int32_t isoDayNumber __attribute__((swift_name("isoDayNumber")));
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

@interface TimeZone (Extensions)
- (UtcOffset *)offsetAtInstant:(Instant *)instant __attribute__((swift_name("offsetAt(instant:)")));
- (NSTimeZone *)toNSTimeZone __attribute__((swift_name("toNSTimeZone()")));
@end

@interface UtcOffset (Extensions)
- (FixedOffsetTimeZone *)asTimeZone __attribute__((swift_name("asTimeZone()")));
- (NSString *)formatFormat:(id<DateTimeFormat>)format __attribute__((swift_name("format(format:)")));
@end

@interface DateTimeComponentsCompanion (Extensions)
- (DateTimeComponents *)parseInput:(id)input format:(id<DateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface NSDate : Base
@end

@interface NSDate (Extensions)
- (Instant *)toKotlinInstant __attribute__((swift_name("toKotlinInstant()")));
@end

__attribute__((objc_subclassing_restricted))
@interface NSTimeZone : Base
@end

@interface NSTimeZone (Extensions)
- (TimeZone *)toKotlinTimeZone __attribute__((swift_name("toKotlinTimeZone()")));
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
@interface InstantKt : Base
+ (Instant *)toInstant:(NSString *)receiver __attribute__((swift_name("toInstant(_:)"))) __attribute__((deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339")));
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
@interface MonthKt : Base
+ (Month *)MonthNumber:(int32_t)number __attribute__((swift_name("Month(number:)")));
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
@interface KotlinArray<T> : Base
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<KotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinEnumCompanion : Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) KotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

@protocol KotlinAppendable
@required
- (id<KotlinAppendable>)appendValue:(unichar)value __attribute__((swift_name("append(value:)")));
- (id<KotlinAppendable>)appendValue_:(id _Nullable)value __attribute__((swift_name("append(value_:)")));
- (id<KotlinAppendable>)appendValue:(id _Nullable)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("append(value:startIndex:endIndex:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface DeserializationStrategy<T0> : Base
@end

__attribute__((objc_subclassing_restricted))
@interface CompositeDecoder : Base
@end

__attribute__((objc_subclassing_restricted))
@interface SerializationStrategy<T0> : Base
@end

__attribute__((objc_subclassing_restricted))
@interface Encoder : Base
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

__attribute__((objc_subclassing_restricted))
@interface SerialDescriptor : Base
@end

__attribute__((objc_subclassing_restricted))
@interface Decoder : Base
@end

__attribute__((objc_subclassing_restricted))
@interface NSDateComponents : Base
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

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
