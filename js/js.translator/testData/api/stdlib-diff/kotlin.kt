--- 14,14 ---
+ public inline fun </*0*/ T> arrayOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.Array<T>
+ public inline fun </*0*/ reified T> arrayOfNulls(/*0*/ size: kotlin.Int): kotlin.Array<T?>
+ public inline fun booleanArrayOf(/*0*/ vararg elements: kotlin.Boolean /*kotlin.BooleanArray*/): kotlin.BooleanArray
+ public inline fun byteArrayOf(/*0*/ vararg elements: kotlin.Byte /*kotlin.ByteArray*/): kotlin.ByteArray
+ public inline fun charArrayOf(/*0*/ vararg elements: kotlin.Char /*kotlin.CharArray*/): kotlin.CharArray
- @kotlin.js.library public fun </*0*/ T> arrayOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.Array<T>
- public inline fun </*0*/ reified @kotlin.internal.PureReifiable T> arrayOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.Array<T>
- public fun </*0*/ reified @kotlin.internal.PureReifiable T> arrayOfNulls(/*0*/ size: kotlin.Int): kotlin.Array<T?>
- @kotlin.js.library public fun booleanArrayOf(/*0*/ vararg elements: kotlin.Boolean /*kotlin.BooleanArray*/): kotlin.BooleanArray
- public fun booleanArrayOf(/*0*/ vararg elements: kotlin.Boolean /*kotlin.BooleanArray*/): kotlin.BooleanArray
- @kotlin.js.library public fun byteArrayOf(/*0*/ vararg elements: kotlin.Byte /*kotlin.ByteArray*/): kotlin.ByteArray
- public fun byteArrayOf(/*0*/ vararg elements: kotlin.Byte /*kotlin.ByteArray*/): kotlin.ByteArray
- @kotlin.js.library public fun charArrayOf(/*0*/ vararg elements: kotlin.Char /*kotlin.CharArray*/): kotlin.CharArray
- public fun charArrayOf(/*0*/ vararg elements: kotlin.Char /*kotlin.CharArray*/): kotlin.CharArray
--- 35,31 ---
+ public inline fun doubleArrayOf(/*0*/ vararg elements: kotlin.Double /*kotlin.DoubleArray*/): kotlin.DoubleArray
- @kotlin.js.library public fun doubleArrayOf(/*0*/ vararg elements: kotlin.Double /*kotlin.DoubleArray*/): kotlin.DoubleArray
- public fun doubleArrayOf(/*0*/ vararg elements: kotlin.Double /*kotlin.DoubleArray*/): kotlin.DoubleArray
--- 38,33 ---
- public inline fun </*0*/ reified @kotlin.internal.PureReifiable T> emptyArray(): kotlin.Array<T>
--- 42,36 ---
+ public inline fun floatArrayOf(/*0*/ vararg elements: kotlin.Float /*kotlin.FloatArray*/): kotlin.FloatArray
+ public inline fun intArrayOf(/*0*/ vararg elements: kotlin.Int /*kotlin.IntArray*/): kotlin.IntArray
- @kotlin.js.library public fun floatArrayOf(/*0*/ vararg elements: kotlin.Float /*kotlin.FloatArray*/): kotlin.FloatArray
- public fun floatArrayOf(/*0*/ vararg elements: kotlin.Float /*kotlin.FloatArray*/): kotlin.FloatArray
- @kotlin.js.library public fun intArrayOf(/*0*/ vararg elements: kotlin.Int /*kotlin.IntArray*/): kotlin.IntArray
- public fun intArrayOf(/*0*/ vararg elements: kotlin.Int /*kotlin.IntArray*/): kotlin.IntArray
--- 50,42 ---
+ public inline fun longArrayOf(/*0*/ vararg elements: kotlin.Long /*kotlin.LongArray*/): kotlin.LongArray
- @kotlin.js.library public fun longArrayOf(/*0*/ vararg elements: kotlin.Long /*kotlin.LongArray*/): kotlin.LongArray
- public fun longArrayOf(/*0*/ vararg elements: kotlin.Long /*kotlin.LongArray*/): kotlin.LongArray
--- 71,62 ---
+ public inline fun shortArrayOf(/*0*/ vararg elements: kotlin.Short /*kotlin.ShortArray*/): kotlin.ShortArray
- @kotlin.js.library public fun shortArrayOf(/*0*/ vararg elements: kotlin.Short /*kotlin.ShortArray*/): kotlin.ShortArray
- public fun shortArrayOf(/*0*/ vararg elements: kotlin.Short /*kotlin.ShortArray*/): kotlin.ShortArray
--- 202,192 ---
+ @kotlin.SinceKotlin(version = "1.2") public fun kotlin.Double.toBits(): kotlin.Long
+ @kotlin.SinceKotlin(version = "1.2") public fun kotlin.Float.toBits(): kotlin.Int
- @kotlin.SinceKotlin(version = "1.2") @kotlin.js.library(name = "doubleToBits") public fun kotlin.Double.toBits(): kotlin.Long
- @kotlin.SinceKotlin(version = "1.2") @kotlin.js.library(name = "floatToBits") public fun kotlin.Float.toBits(): kotlin.Int
--- 206,196 ---
+ @kotlin.SinceKotlin(version = "1.2") public fun kotlin.Double.toRawBits(): kotlin.Long
+ @kotlin.SinceKotlin(version = "1.2") public fun kotlin.Float.toRawBits(): kotlin.Int
- @kotlin.SinceKotlin(version = "1.2") @kotlin.js.library(name = "doubleToRawBits") public fun kotlin.Double.toRawBits(): kotlin.Long
- @kotlin.SinceKotlin(version = "1.2") @kotlin.js.library(name = "floatToRawBits") public fun kotlin.Float.toRawBits(): kotlin.Int
--- 233,223 ---
- public interface Annotation {
- }
- 
--- 245,232 ---
+     public constructor ArithmeticException(/*0*/ message: kotlin.String?)
-     /*primary*/ public constructor ArithmeticException(/*0*/ message: kotlin.String?)
--- 261,248 ---
+     @kotlin.SinceKotlin(version = "1.4") public constructor AssertionError(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ @kotlin.SinceKotlin(version = "1.4") public constructor AssertionError(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
--- 267,254 ---
+     public open override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
+     public open override /*1*/ fun hashCode(): kotlin.Int
--- 269,258 ---
+     public open override /*1*/ fun toString(): kotlin.String
--- 303,293 ---
+     public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
+     public open override /*2*/ fun hashCode(): kotlin.Int
--- 339,331 ---
+     public open override /*2*/ fun toString(): kotlin.String
--- 367,360 ---
+     public open override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
+     public open override /*1*/ fun hashCode(): kotlin.Int
--- 379,374 ---
+     @kotlin.js.JsName(name = "toString") public open override /*1*/ fun toString(): kotlin.String
--- 421,417 ---
- public interface CharSequence {
-     public abstract val length: kotlin.Int
-         public abstract fun <get-length>(): kotlin.Int
-     public abstract operator fun get(/*0*/ index: kotlin.Int): kotlin.Char
-     public abstract fun subSequence(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.CharSequence
- }
- 
--- 430,419 ---
+     public constructor ClassCastException(/*0*/ message: kotlin.String?)
-     /*primary*/ public constructor ClassCastException(/*0*/ message: kotlin.String?)
--- 444,433 ---
+     public constructor ConcurrentModificationException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ public constructor ConcurrentModificationException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
--- 494,483 ---
+     public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
+     public open override /*2*/ fun hashCode(): kotlin.Int
--- 526,517 ---
+     public open override /*2*/ fun toString(): kotlin.String
--- 567,559 ---
+     public open override /*1*/ fun compareTo(/*0*/ other: E): kotlin.Int
+     public open override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
+     public open override /*1*/ fun hashCode(): kotlin.Int
-     protected final fun clone(): kotlin.Any
-     public final override /*1*/ fun compareTo(/*0*/ other: E): kotlin.Int
-     public final override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
-     public final override /*1*/ fun hashCode(): kotlin.Int
--- 580,571 ---
+     public constructor Error(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ public constructor Error(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
--- 587,578 ---
+     public constructor Exception(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ public constructor Exception(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
--- 637,628 ---
+     public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
+     public open override /*2*/ fun hashCode(): kotlin.Int
--- 669,662 ---
+     public open override /*2*/ fun toString(): kotlin.String
--- 703,697 ---
- public interface Function</*0*/ out R> {
- }
- 
--- 709,700 ---
+     public constructor IllegalArgumentException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ public constructor IllegalArgumentException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
--- 716,707 ---
+     public constructor IllegalStateException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ public constructor IllegalStateException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
--- 722,713 ---
+     public constructor IndexOutOfBoundsException(/*0*/ message: kotlin.String?)
-     /*primary*/ public constructor IndexOutOfBoundsException(/*0*/ message: kotlin.String?)
--- 740,731 ---
+     public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
+     public open override /*2*/ fun hashCode(): kotlin.Int
--- 780,773 ---
+     public open override /*2*/ fun toString(): kotlin.String
--- 851,845 ---
+     public final inline operator fun compareTo(/*0*/ other: kotlin.Byte): kotlin.Int
+     public final inline operator fun compareTo(/*0*/ other: kotlin.Double): kotlin.Int
+     public final inline operator fun compareTo(/*0*/ other: kotlin.Float): kotlin.Int
+     public final inline operator fun compareTo(/*0*/ other: kotlin.Int): kotlin.Int
-     public final operator fun compareTo(/*0*/ other: kotlin.Byte): kotlin.Int
-     public final operator fun compareTo(/*0*/ other: kotlin.Double): kotlin.Int
-     public final operator fun compareTo(/*0*/ other: kotlin.Float): kotlin.Int
-     public final operator fun compareTo(/*0*/ other: kotlin.Int): kotlin.Int
--- 856,850 ---
+     public final inline operator fun compareTo(/*0*/ other: kotlin.Short): kotlin.Int
-     public final operator fun compareTo(/*0*/ other: kotlin.Short): kotlin.Int
--- 858,852 ---
+     public final inline operator fun div(/*0*/ other: kotlin.Byte): kotlin.Long
+     public final inline operator fun div(/*0*/ other: kotlin.Double): kotlin.Double
+     public final inline operator fun div(/*0*/ other: kotlin.Float): kotlin.Float
+     public final inline operator fun div(/*0*/ other: kotlin.Int): kotlin.Long
-     public final operator fun div(/*0*/ other: kotlin.Byte): kotlin.Long
-     public final operator fun div(/*0*/ other: kotlin.Double): kotlin.Double
-     public final operator fun div(/*0*/ other: kotlin.Float): kotlin.Float
-     public final operator fun div(/*0*/ other: kotlin.Int): kotlin.Long
--- 863,857 ---
+     public final inline operator fun div(/*0*/ other: kotlin.Short): kotlin.Long
+     public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
+     public open override /*2*/ fun hashCode(): kotlin.Int
-     public final operator fun div(/*0*/ other: kotlin.Short): kotlin.Long
--- 866,862 ---
+     public final inline operator fun minus(/*0*/ other: kotlin.Byte): kotlin.Long
+     public final inline operator fun minus(/*0*/ other: kotlin.Double): kotlin.Double
+     public final inline operator fun minus(/*0*/ other: kotlin.Float): kotlin.Float
+     public final inline operator fun minus(/*0*/ other: kotlin.Int): kotlin.Long
-     public final operator fun minus(/*0*/ other: kotlin.Byte): kotlin.Long
-     public final operator fun minus(/*0*/ other: kotlin.Double): kotlin.Double
-     public final operator fun minus(/*0*/ other: kotlin.Float): kotlin.Float
-     public final operator fun minus(/*0*/ other: kotlin.Int): kotlin.Long
--- 871,867 ---
+     public final inline operator fun minus(/*0*/ other: kotlin.Short): kotlin.Long
-     public final operator fun minus(/*0*/ other: kotlin.Short): kotlin.Long
--- 873,869 ---
+     public final inline operator fun plus(/*0*/ other: kotlin.Byte): kotlin.Long
+     public final inline operator fun plus(/*0*/ other: kotlin.Double): kotlin.Double
+     public final inline operator fun plus(/*0*/ other: kotlin.Float): kotlin.Float
+     public final inline operator fun plus(/*0*/ other: kotlin.Int): kotlin.Long
-     public final operator fun plus(/*0*/ other: kotlin.Byte): kotlin.Long
-     public final operator fun plus(/*0*/ other: kotlin.Double): kotlin.Double
-     public final operator fun plus(/*0*/ other: kotlin.Float): kotlin.Float
-     public final operator fun plus(/*0*/ other: kotlin.Int): kotlin.Long
--- 878,874 ---
+     public final inline operator fun plus(/*0*/ other: kotlin.Short): kotlin.Long
-     public final operator fun plus(/*0*/ other: kotlin.Short): kotlin.Long
--- 883,879 ---
+     @kotlin.SinceKotlin(version = "1.1") public final inline operator fun rem(/*0*/ other: kotlin.Byte): kotlin.Long
+     @kotlin.SinceKotlin(version = "1.1") public final inline operator fun rem(/*0*/ other: kotlin.Double): kotlin.Double
+     @kotlin.SinceKotlin(version = "1.1") public final inline operator fun rem(/*0*/ other: kotlin.Float): kotlin.Float
+     @kotlin.SinceKotlin(version = "1.1") public final inline operator fun rem(/*0*/ other: kotlin.Int): kotlin.Long
-     @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Byte): kotlin.Long
-     @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Double): kotlin.Double
-     @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Float): kotlin.Float
-     @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Int): kotlin.Long
--- 888,884 ---
+     @kotlin.SinceKotlin(version = "1.1") public final inline operator fun rem(/*0*/ other: kotlin.Short): kotlin.Long
-     @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Short): kotlin.Long
--- 891,887 ---
+     public final inline operator fun times(/*0*/ other: kotlin.Byte): kotlin.Long
+     public final inline operator fun times(/*0*/ other: kotlin.Double): kotlin.Double
+     public final inline operator fun times(/*0*/ other: kotlin.Float): kotlin.Float
+     public final inline operator fun times(/*0*/ other: kotlin.Int): kotlin.Long
-     public final operator fun times(/*0*/ other: kotlin.Byte): kotlin.Long
-     public final operator fun times(/*0*/ other: kotlin.Double): kotlin.Double
-     public final operator fun times(/*0*/ other: kotlin.Float): kotlin.Float
-     public final operator fun times(/*0*/ other: kotlin.Int): kotlin.Long
--- 896,892 ---
+     public final inline operator fun times(/*0*/ other: kotlin.Short): kotlin.Long
-     public final operator fun times(/*0*/ other: kotlin.Short): kotlin.Long
--- 904,900 ---
+     public open override /*2*/ fun toString(): kotlin.String
--- 905,902 ---
+     public final inline operator fun unaryPlus(): kotlin.Long
-     public final operator fun unaryPlus(): kotlin.Long
--- 933,930 ---
+     public constructor NoSuchElementException(/*0*/ message: kotlin.String?)
-     /*primary*/ public constructor NoSuchElementException(/*0*/ message: kotlin.String?)
--- 939,936 ---
+     public constructor NoWhenBranchMatchedException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ public constructor NoWhenBranchMatchedException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
--- 952,949 ---
+     public constructor NullPointerException(/*0*/ message: kotlin.String?)
-     /*primary*/ public constructor NullPointerException(/*0*/ message: kotlin.String?)
--- 968,965 ---
+     public constructor NumberFormatException(/*0*/ message: kotlin.String?)
-     /*primary*/ public constructor NumberFormatException(/*0*/ message: kotlin.String?)
--- 1055,1052 ---
+     public constructor RuntimeException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ public constructor RuntimeException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
--- 1073,1070 ---
+     public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
+     public open override /*2*/ fun hashCode(): kotlin.Int
--- 1109,1108 ---
+     public open override /*2*/ fun toString(): kotlin.String
--- 1145,1145 ---
+     public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
--- 1146,1147 ---
+     public open override /*2*/ fun hashCode(): kotlin.Int
--- 1148,1150 ---
+     public open override /*2*/ fun toString(): kotlin.String
--- 1159,1162 ---
+ @kotlin.js.JsName(name = "Error") public open external class Throwable {
- public open class Throwable {
--- 1162,1165 ---
+     public constructor Throwable(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ public constructor Throwable(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
--- 1168,1171 ---
+     public open override /*1*/ fun toString(): kotlin.String
--- 1486,1490 ---
+     public constructor UninitializedPropertyAccessException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ public constructor UninitializedPropertyAccessException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
--- 1494,1498 ---
- public object Unit {
-     public open override /*1*/ fun toString(): kotlin.String
- }
- 
--- 1505,1505 ---
+     public constructor UnsupportedOperationException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
-     /*primary*/ public constructor UnsupportedOperationException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
