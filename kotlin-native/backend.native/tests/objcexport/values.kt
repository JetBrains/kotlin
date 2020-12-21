/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// All classes and methods should be used in tests
@file:Suppress("UNUSED")

package conversions

import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen
import kotlin.native.internal.ObjCErrorException
import kotlin.native.ref.WeakReference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.test.*
import kotlinx.cinterop.*

// Ensure loaded function IR classes aren't ordered by arity:
internal fun referenceFunction1(block: (Any?) -> Unit) {}

// Constants
const val dbl: Double = 3.14
const val flt: Float = 2.73F
const val integer: Int = 42
const val longInt: Long = 1984

// Vars
var intVar: Int = 451
var str = "Kotlin String"
var strAsAny: Any = "Kotlin String as Any"

// MIN/MAX values as Numbers
var minDoubleVal: kotlin.Number = Double.MIN_VALUE
var maxDoubleVal: kotlin.Number = Double.MAX_VALUE

// Infinities and NaN
val nanDoubleVal: Double = Double.NaN
val nanFloatVal: Float = Float.NaN
val infDoubleVal: Double = Double.POSITIVE_INFINITY
val infFloatVal: Float = Float.NEGATIVE_INFINITY

private fun <T> T.toNullable(): T? = this

fun box(booleanValue: Boolean) = booleanValue.toNullable()
fun box(byteValue: Byte) = byteValue.toNullable()
fun box(shortValue: Short) = shortValue.toNullable()
fun box(intValue: Int) = intValue.toNullable()
fun box(longValue: Long) = longValue.toNullable()
fun box(uByteValue: UByte) = uByteValue.toNullable()
fun box(uShortValue: UShort) = uShortValue.toNullable()
fun box(uIntValue: UInt) = uIntValue.toNullable()
fun box(uLongValue: ULong) = uLongValue.toNullable()
fun box(floatValue: Float) = floatValue.toNullable()
fun box(doubleValue: Double) = doubleValue.toNullable()

private inline fun <reified T> ensureEquals(actual: T?, expected: T) {
    if (actual !is T) error(T::class)
    if (actual != expected) error(T::class)
}

fun ensureEqualBooleans(actual: Boolean?, expected: Boolean) = ensureEquals(actual, expected)
fun ensureEqualBytes(actual: Byte?, expected: Byte) = ensureEquals(actual, expected)
fun ensureEqualShorts(actual: Short?, expected: Short) = ensureEquals(actual, expected)
fun ensureEqualInts(actual: Int?, expected: Int) = ensureEquals(actual, expected)
fun ensureEqualLongs(actual: Long?, expected: Long) = ensureEquals(actual, expected)
fun ensureEqualUBytes(actual: UByte?, expected: UByte) = ensureEquals(actual, expected)
fun ensureEqualUShorts(actual: UShort?, expected: UShort) = ensureEquals(actual, expected)
fun ensureEqualUInts(actual: UInt?, expected: UInt) = ensureEquals(actual, expected)
fun ensureEqualULongs(actual: ULong?, expected: ULong) = ensureEquals(actual, expected)
fun ensureEqualFloats(actual: Float?, expected: Float) = ensureEquals(actual, expected)
fun ensureEqualDoubles(actual: Double?, expected: Double) = ensureEquals(actual, expected)

// Boolean
val boolVal: Boolean = true
val boolAnyVal: Any = false

// Lists
val numbersList: List<Number> = listOf(1.toByte(), 2.toShort(), 13)
val anyList: List<Any> = listOf("Str", 42, 3.14, true)

// lateinit
lateinit var lateinitIntVar: Any

// lazy
val lazyVal: String by lazy {
    println("Lazy value initialization")
    "Lazily initialized string"
}

// Delegation
var delegatedGlobalArray: Array<String> by DelegateClass()

class DelegateClass: ReadWriteProperty<Nothing?, Array<String>> {

    private var holder: Array<String> = arrayOf("property")

    override fun getValue(thisRef: Nothing?, property: KProperty<*>): Array<String> {
        return arrayOf("Delegated", "global", "array") + holder
    }

    override fun setValue(thisRef: Nothing?, property: KProperty<*>, value: Array<String>) {
        holder = value
    }
}

// Getter with delegation
val delegatedList: List<String>
    get() = delegatedGlobalArray.toList()

// Null
val nullVal: Any? = null
var nullVar: String? = ""

// Any
var anyValue: Any = "Str"

// Functions
fun emptyFun() { }

fun strFun(): String = "fooStr"

fun argsFun(i: Int, l: Long, d: Double, s: String): Any = s + i + l + d

fun funArgument(foo: () -> String): String = foo()

// Generic functions
fun <T, R> genericFoo(t: T, foo: (T) -> R): R = foo(t)

fun <T : Number, R : T> fooGenericNumber(r: R, foo: (T) -> Number): Number = foo(r)

fun <T> varargToList(vararg args: T): List<T> = args.toList()

// Extensions
fun String.subExt(i: Int): String {
    return if (i < this.length) this[i].toString() else "nothing"
}

fun Any?.toString(): String = this?.toString() ?: "null"

fun Any?.print() = println(this.toString())

fun Char.boxChar(): Char? = this
fun Char?.isA(): Boolean = (this == 'A')

// Lambdas
val sumLambda = { x: Int, y: Int -> x + y }


// Inheritance
interface I {
    fun iFun(): String = "I::iFun"
}

fun I.iFunExt() = iFun()

private interface PI {
    fun piFun(): Any
    fun iFun(): String = "PI::iFun"
}

class DefaultInterfaceExt : I

open class OpenClassI : I {
    override fun iFun(): String = "OpenClassI::iFun"
}

class FinalClassExtOpen : OpenClassI() {
    override fun iFun(): String = "FinalClassExtOpen::iFun"
}

open class MultiExtClass : OpenClassI(), PI {
    override fun piFun(): Any {
        return 42
    }

    override fun iFun(): String = super<PI>.iFun()
}

open class ConstrClass(open val i: Int, val s: String, val a: Any = "AnyS") : OpenClassI()

class ExtConstrClass(override val i: Int) : ConstrClass(i, "String") {
    override fun iFun(): String  = "ExtConstrClass::iFun::$i-$s-$a"
}

// Enum
enum class Enumeration(val enumValue: Int) {
    ANSWER(42), YEAR(1984), TEMPERATURE(451)
}

fun passEnum(): Enumeration {
    return Enumeration.ANSWER
}

fun receiveEnum(e: Int) {
    println("ENUM got: ${get(e).enumValue}")
}

fun get(value: Int): Enumeration {
    return Enumeration.values()[value]
}

// Data class values and generated properties: component# and toString()
data class TripleVals<T>(val first: T, val second: T, val third: T)

data class TripleVars<T>(var first: T, var second: T, var third: T) {
    override fun toString(): String {
        return "[$first, $second, $third]"
    }
}

open class WithCompanionAndObject {
    companion object {
        val str = "String"
        var named: I? = Named
    }

    object Named : OpenClassI() {
        override fun iFun(): String = "WithCompanionAndObject.Named::iFun"
    }
}

fun getCompanionObject() = WithCompanionAndObject.Companion
fun getNamedObject() = WithCompanionAndObject.Named
fun getNamedObjectInterface(): OpenClassI = WithCompanionAndObject.Named

typealias EE = Enumeration
fun EE.getAnswer() : EE  = Enumeration.ANSWER

inline class IC1(val value: Int)
inline class IC2(val value: String)
inline class IC3(val value: TripleVals<Any?>?)

fun box(ic1: IC1): Any = ic1
fun box(ic2: IC2): Any = ic2
fun box(ic3: IC3): Any = ic3

fun concatenateInlineClassValues(ic1: IC1, ic1N: IC1?, ic2: IC2, ic2N: IC2?, ic3: IC3, ic3N: IC3?): String =
        "${ic1.value} ${ic1N?.value} ${ic2.value} ${ic2N?.value} ${ic3.value} ${ic3N?.value}"

fun IC1.getValue1() = this.value
fun IC1?.getValueOrNull1() = this?.value

fun IC2.getValue2() = value
fun IC2?.getValueOrNull2() = this?.value

fun IC3.getValue3() = value
fun IC3?.getValueOrNull3() = this?.value

fun isFrozen(obj: Any): Boolean = obj.isFrozen
fun kotlinLambda(block: (Any) -> Any): Any = block

fun multiply(int: Int, long: Long) = int * long

class MyException : Exception()
class MyError : Error()

@Throws(MyException::class, MyError::class)
fun throwException(error: Boolean): Unit {
    throw if (error) MyError() else MyException()
}

interface SwiftOverridableMethodsWithThrows {
    @Throws(MyException::class) fun unit(): Unit
    @Throws(MyException::class) fun nothing(): Nothing
    @Throws(MyException::class) fun any(): Any
    @Throws(MyException::class) fun block(): () -> Int
}

interface MethodsWithThrows : SwiftOverridableMethodsWithThrows {
    @Throws(MyException::class) fun nothingN(): Nothing?
    @Throws(MyException::class) fun anyN(): Any?
    @Throws(MyException::class) fun blockN(): (() -> Int)?
    @Throws(MyException::class) fun pointer(): CPointer<*>
    @Throws(MyException::class) fun pointerN(): CPointer<*>?
    @Throws(MyException::class) fun int(): Int
    @Throws(MyException::class) fun longN(): Long?
    @Throws(MyException::class) fun double(): Double

    interface UnitCaller {
        @Throws(MyException::class) fun call(methods: MethodsWithThrows): Unit
    }
}

open class Throwing : MethodsWithThrows {
    @Throws(MyException::class) constructor(doThrow: Boolean) {
        if (doThrow) throw MyException()
    }

    override fun unit(): Unit = throw MyException()
    override fun nothing(): Nothing = throw MyException()
    override fun nothingN(): Nothing? = throw MyException()
    override fun any(): Any = throw MyException()
    override fun anyN(): Any? = throw MyException()
    override fun block(): () -> Int = throw MyException()
    override fun blockN(): (() -> Int)? = throw MyException()
    override fun pointer(): CPointer<*> = throw MyException()
    override fun pointerN(): CPointer<*>? = throw MyException()
    override fun int(): Int = throw MyException()
    override fun longN(): Long? = throw MyException()
    override fun double(): Double = throw MyException()
}

class NotThrowing : MethodsWithThrows {
    @Throws(MyException::class) constructor() {}

    override fun unit(): Unit {}
    override fun nothing(): Nothing = throw MyException()
    override fun nothingN(): Nothing? = null
    override fun any(): Any = Any()
    override fun anyN(): Any? = Any()
    override fun block(): () -> Int = { 42 }
    override fun blockN(): (() -> Int)? = null
    override fun pointer(): CPointer<*> = 1L.toCPointer<COpaque>()!!
    override fun pointerN(): CPointer<*>? = null
    override fun int(): Int = 42
    override fun longN(): Long? = null
    override fun double(): Double = 3.14
}

@Throws(Throwable::class)
fun testSwiftThrowing(methods: SwiftOverridableMethodsWithThrows) = with(methods) {
    assertSwiftThrowing { unit() }
    assertSwiftThrowing { nothing() }
    assertSwiftThrowing { any() }
    assertSwiftThrowing { block() }
}

private inline fun assertSwiftThrowing(block: () -> Unit) =
        assertFailsWith<ObjCErrorException>(block = block)

@Throws(Throwable::class)
fun testSwiftNotThrowing(methods: SwiftOverridableMethodsWithThrows) = with(methods) {
    unit()
    assertEquals(42, any())
    assertEquals(17, block()())
}

@Throws(MyError::class)
fun callUnit(methods: SwiftOverridableMethodsWithThrows) = methods.unit()

@Throws(Throwable::class)
fun callUnitCaller(caller: MethodsWithThrows.UnitCaller, methods: MethodsWithThrows) {
    assertFailsWith<MyException> { caller.call(methods) }
}

interface ThrowsWithBridgeBase {
    @Throws(MyException::class)
    fun plusOne(x: Int): Any
}

abstract class ThrowsWithBridge : ThrowsWithBridgeBase {
    abstract override fun plusOne(x: Int): Int
}

@Throws(Throwable::class)
fun testSwiftThrowing(test: ThrowsWithBridgeBase, flag: Boolean) {
    assertFailsWith<ObjCErrorException> {
        if (flag) {
            test.plusOne(0)
        } else {
            val test1 = test as ThrowsWithBridge
            val ignore: Int = test1.plusOne(1)
        }
    }
}

@Throws(Throwable::class)
fun testSwiftNotThrowing(test: ThrowsWithBridgeBase) {
    assertEquals(3, test.plusOne(2))
    val test1 = test as ThrowsWithBridge
    assertEquals<Int>(4, test1.plusOne(3))
}

fun Any.same() = this

// https://github.com/JetBrains/kotlin-native/issues/2571
val PROPERTY_NAME_MUST_NOT_BE_ALTERED_BY_SWIFT = 111

// https://github.com/JetBrains/kotlin-native/issues/2667
class Deeply {
    class Nested {
        class Type {
            val thirtyTwo = 32
        }

        interface IType
    }
}

class WithGenericDeeply() {
    class Nested {
        class Type<T> {
            val thirtyThree = 33
        }
    }
}

// https://github.com/JetBrains/kotlin-native/issues/3167
class TypeOuter {
    class Type {
        val thirtyFour = 34
    }
}

data class CKeywords(val float: Float, val `enum`: Int, var goto: Boolean)

interface Base1 {
    fun same(value: Int?): Int?
}

interface ExtendedBase1 : Base1 {
    override fun same(value: Int?): Int?
}

interface Base2 {
    fun same(value: Int?): Int?
}

internal interface Base3 {
    fun same(value: Int?): Int
}

open class Base23 : Base2, Base3 {
    override fun same(value: Int?): Int = error("should not reach here")
}

fun call(base1: Base1, value: Int?) = base1.same(value)
fun call(extendedBase1: ExtendedBase1, value: Int?) = extendedBase1.same(value)
fun call(base2: Base2, value: Int?) = base2.same(value)
fun call(base3: Any, value: Int?) = (base3 as Base3).same(value)
fun call(base23: Base23, value: Int?) = base23.same(value)

interface Transform<T, R> {
    fun map(value: T): R
}

interface TransformWithDefault<T> : Transform<T, T> {
    override fun map(value: T): T = value
}

class TransformInheritingDefault<T> : TransformWithDefault<T>

interface TransformIntString {
    fun map(intValue: Int): String
}

abstract class TransformIntToString : Transform<Int, String>, TransformIntString {
    override abstract fun map(intValue: Int): String
}

open class TransformIntToDecimalString : TransformIntToString() {
    override fun map(intValue: Int): String = intValue.toString()
}

private class TransformDecimalStringToInt : Transform<String, Int> {
    override fun map(stringValue: String): Int = stringValue.toInt()
}

fun createTransformDecimalStringToInt(): Transform<String, Int> = TransformDecimalStringToInt()

open class TransformIntToLong : Transform<Int, Long> {
    override fun map(value: Int): Long = value.toLong()
}

class GH2931 {
    class Data

    class Holder {
        val data = Data()

        init {
            freeze()
        }
    }
}

class GH2945(var errno: Int) {
    fun testErrnoInSelector(p: Int, errno: Int) = p + errno
}

class GH2830 {
    interface I
    private class PrivateImpl : I

    fun getI(): Any = PrivateImpl()
}

class GH2959 {
    interface I {
        val id: Int
    }
    private class PrivateImpl(override val id: Int) : I

    fun getI(id: Int): List<I> = listOf(PrivateImpl(id))
}

fun runUnitBlock(block: () -> Unit): Boolean {
    val blockAny: () -> Any? = block
    return blockAny() === Unit
}

fun asUnitBlock(block: () -> Any?): () -> Unit = { block() }

fun runNothingBlock(block: () -> Nothing) = try {
    block()
    false
} catch (e: Throwable) {
    true
}

fun asNothingBlock(block: () -> Any?): () -> Nothing = {
    block()
    TODO()
}

fun getNullBlock(): (() -> Unit)? = null
fun isBlockNull(block: (() -> Unit)?): Boolean = block == null

interface IntBlocks<T> {
    fun getPlusOneBlock(): T
    fun callBlock(argument: Int, block: T): Int
}

object IntBlocksImpl : IntBlocks<(Int) -> Int> {
    override fun getPlusOneBlock(): (Int) -> Int = { it: Int -> it + 1 }
    override fun callBlock(argument: Int, block: (Int) -> Int): Int = block(argument)
}

interface UnitBlockCoercion<T : Any> {
    fun coerce(block: () -> Unit): T
    fun uncoerce(block: T): () -> Unit
}

object UnitBlockCoercionImpl : UnitBlockCoercion<() -> Unit> {
    override fun coerce(block: () -> Unit): () -> Unit = block
    override fun uncoerce(block: () -> Unit): () -> Unit = block
}

fun isFunction(obj: Any?): Boolean = obj is Function<*>
fun isFunction0(obj: Any?): Boolean = obj is Function0<*>

abstract class MyAbstractList : List<Any?>

fun takeForwardDeclaredClass(obj: objcnames.classes.ForwardDeclaredClass) {}
fun takeForwardDeclaredProtocol(obj: objcnames.protocols.ForwardDeclaredProtocol) {}

class TestKClass {
    fun getKotlinClass(clazz: ObjCClass) = getOriginalKotlinClass(clazz)
    fun getKotlinClass(protocol: ObjCProtocol) = getOriginalKotlinClass(protocol)

    fun isTestKClass(kClass: KClass<*>): Boolean = (kClass == TestKClass::class)
    fun isI(kClass: KClass<*>): Boolean = (kClass == TestKClass.I::class)

    interface I
}

// https://kotlinlang.slack.com/archives/C3SGXARS6/p1560954372179300
interface ForwardI2 : ForwardI1
interface ForwardI1 {
    fun getForwardI2(): ForwardI2
}

abstract class ForwardC2 : ForwardC1()
abstract class ForwardC1 {
    abstract fun getForwardC2(): ForwardC2
}

interface TestSR10177Workaround

interface TestClashes1 {
    val clashingProperty: Int
}

interface TestClashes2 {
    val clashingProperty: Any
    val clashingProperty_: Any
}

class TestClashesImpl : TestClashes1, TestClashes2 {
    override val clashingProperty: Int
        get() = 1

    override val clashingProperty_: Int
        get() = 2
}

class TestInvalidIdentifiers {
    class `$Foo`
    class `Bar$`

    fun `a$d$d`(`$1`: Int, `2`: Int, `3`: Int): Int = `$1` + `2` + `3`

    var `$status`: String = ""

    enum class E(val value: Int) {
        `4$`(4),
        `5$`(5),
        `_`(6),
        `__`(7)
    }

    companion object `Companion$` {
        val `42` = 42
    }

    val `$` = '$'
    val `_` = '_'
}

@Suppress("UNUSED_PARAMETER")
open class TestDeprecation() {
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN) open class OpenHidden : TestDeprecation()
    @Suppress("DEPRECATION_ERROR") class ExtendingHidden : OpenHidden() {
        class Nested
    }

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN) interface HiddenInterface {
        fun effectivelyHidden(): Any
    }

    @Suppress("DEPRECATION_ERROR") open class ImplementingHidden : Any(), HiddenInterface {
        override fun effectivelyHidden(): Int = -1
    }

    @Suppress("DEPRECATION_ERROR")
    fun callEffectivelyHidden(obj: Any): Int = (obj as HiddenInterface).effectivelyHidden() as Int

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN) class Hidden : TestDeprecation() {
        open class Nested {
            class Nested
            inner class Inner
        }

        inner class Inner {
            inner class Inner
        }
    }

    @Suppress("DEPRECATION_ERROR") class ExtendingNestedInHidden : Hidden.Nested()

    @Suppress("DEPRECATION_ERROR") fun getHidden() = Hidden()

    @Deprecated("hidden", level = DeprecationLevel.HIDDEN) constructor(hidden: Byte) : this()
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN) fun hidden() {}
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN) val hiddenVal: Any? = null
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN) var hiddenVar: Any? = null
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN) open fun openHidden() {}
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN) open val openHiddenVal: Any? = null
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN) open var openHiddenVar: Any? = null

    @Deprecated("error", level = DeprecationLevel.ERROR) open class OpenError : TestDeprecation()
    @Suppress("DEPRECATION_ERROR") class ExtendingError : OpenError()

    @Deprecated("error", level = DeprecationLevel.ERROR) interface ErrorInterface
    @Suppress("DEPRECATION_ERROR") class ImplementingError : ErrorInterface

    @Deprecated("error", level = DeprecationLevel.ERROR) class Error : TestDeprecation()
    @Suppress("DEPRECATION_ERROR") fun getError() = Error()

    @Deprecated("error", level = DeprecationLevel.ERROR) constructor(error: Short) : this()
    @Deprecated("error", level = DeprecationLevel.ERROR) fun error() {}
    @Deprecated("error", level = DeprecationLevel.ERROR) val errorVal: Any? = null
    @Deprecated("error", level = DeprecationLevel.ERROR) var errorVar: Any? = null
    @Deprecated("error", level = DeprecationLevel.ERROR) open fun openError() {}
    @Deprecated("error", level = DeprecationLevel.ERROR) open val openErrorVal: Any? = null
    @Deprecated("error", level = DeprecationLevel.ERROR) open var openErrorVar: Any? = null

    @Deprecated("warning", level = DeprecationLevel.WARNING) open class OpenWarning : TestDeprecation()
    @Suppress("DEPRECATION") class ExtendingWarning : OpenWarning()

    @Deprecated("warning", level = DeprecationLevel.WARNING) interface WarningInterface
    @Suppress("DEPRECATION") class ImplementingWarning : WarningInterface

    @Deprecated("warning", level = DeprecationLevel.WARNING) class Warning : TestDeprecation()
    @Suppress("DEPRECATION") fun getWarning() = Warning()

    @Deprecated("warning", level = DeprecationLevel.WARNING) constructor(warning: Int) : this()
    @Deprecated("warning", level = DeprecationLevel.WARNING) fun warning() {}
    @Deprecated("warning", level = DeprecationLevel.WARNING) val warningVal: Any? = null
    @Deprecated("warning", level = DeprecationLevel.WARNING) var warningVar: Any? = null
    @Deprecated("warning", level = DeprecationLevel.WARNING) open fun openWarning() {}
    @Deprecated("warning", level = DeprecationLevel.WARNING) open val openWarningVal: Any? = null
    @Deprecated("warning", level = DeprecationLevel.WARNING) open var openWarningVar: Any? = null

    constructor(normal: Long) : this()
    fun normal() {}
    val normalVal: Any? = null
    var normalVar: Any? = null
    open fun openNormal(): Int = 1
    open val openNormalVal: Any? = null
    open var openNormalVar: Any? = null

    class HiddenOverride() : TestDeprecation() {
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) constructor(hidden: Byte) : this()
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override fun openHidden() {}
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override val openHiddenVal: Any? = null
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override var openHiddenVar: Any? = null

        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) constructor(error: Short) : this()
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override fun openError() {}
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override val openErrorVal: Any? = null
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override var openErrorVar: Any? = null

        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) constructor(warning: Int) : this()
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override fun openWarning() {}
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override val openWarningVal: Any? = null
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override var openWarningVar: Any? = null

        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) constructor(normal: Long) : this()
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override fun openNormal(): Int = 2
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override val openNormalVal: Any? = null
        @Deprecated("hidden", level = DeprecationLevel.HIDDEN) override var openNormalVar: Any? = null
    }

    class ErrorOverride() : TestDeprecation() {
        @Deprecated("error", level = DeprecationLevel.ERROR) constructor(hidden: Byte) : this()
        @Deprecated("error", level = DeprecationLevel.ERROR) override fun openHidden() {}
        @Deprecated("error", level = DeprecationLevel.ERROR) override val openHiddenVal: Any? = null
        @Deprecated("error", level = DeprecationLevel.ERROR) override var openHiddenVar: Any? = null

        @Deprecated("error", level = DeprecationLevel.ERROR) constructor(error: Short) : this()
        @Deprecated("error", level = DeprecationLevel.ERROR) override fun openError() {}
        @Deprecated("error", level = DeprecationLevel.ERROR) override val openErrorVal: Any? = null
        @Deprecated("error", level = DeprecationLevel.ERROR) override var openErrorVar: Any? = null

        @Deprecated("error", level = DeprecationLevel.ERROR) constructor(warning: Int) : this()
        @Deprecated("error", level = DeprecationLevel.ERROR) override fun openWarning() {}
        @Deprecated("error", level = DeprecationLevel.ERROR) override val openWarningVal: Any? = null
        @Deprecated("error", level = DeprecationLevel.ERROR) override var openWarningVar: Any? = null

        @Deprecated("error", level = DeprecationLevel.ERROR) constructor(normal: Long) : this()
        @Deprecated("error", level = DeprecationLevel.ERROR) override fun openNormal(): Int = 3
        @Deprecated("error", level = DeprecationLevel.ERROR) override val openNormalVal: Any? = null
        @Deprecated("error", level = DeprecationLevel.ERROR) override var openNormalVar: Any? = null
    }

    class WarningOverride() : TestDeprecation() {
        @Deprecated("warning", level = DeprecationLevel.WARNING) constructor(hidden: Byte) : this()
        @Deprecated("warning", level = DeprecationLevel.WARNING) override fun openHidden() {}
        @Deprecated("warning", level = DeprecationLevel.WARNING) override val openHiddenVal: Any? = null
        @Deprecated("warning", level = DeprecationLevel.WARNING) override var openHiddenVar: Any? = null

        @Deprecated("warning", level = DeprecationLevel.WARNING) constructor(error: Short) : this()
        @Deprecated("warning", level = DeprecationLevel.WARNING) override fun openError() {}
        @Deprecated("warning", level = DeprecationLevel.WARNING) override val openErrorVal: Any? = null
        @Deprecated("warning", level = DeprecationLevel.WARNING) override var openErrorVar: Any? = null

        @Deprecated("warning", level = DeprecationLevel.WARNING) constructor(warning: Int) : this()
        @Deprecated("warning", level = DeprecationLevel.WARNING) override fun openWarning() {}
        @Deprecated("warning", level = DeprecationLevel.WARNING) override val openWarningVal: Any? = null
        @Deprecated("warning", level = DeprecationLevel.WARNING) override var openWarningVar: Any? = null

        @Deprecated("warning", level = DeprecationLevel.WARNING) constructor(normal: Long) : this()
        @Deprecated("warning", level = DeprecationLevel.WARNING) override fun openNormal(): Int = 4
        @Deprecated("warning", level = DeprecationLevel.WARNING) override val openNormalVal: Any? = null
        @Deprecated("warning", level = DeprecationLevel.WARNING) override var openNormalVar: Any? = null
    }

    class NormalOverride() : TestDeprecation() {
        constructor(hidden: Byte) : this()
        override fun openHidden() {}
        override val openHiddenVal: Any? = null
        override var openHiddenVar: Any? = null

        constructor(error: Short) : this()
        override fun openError() {}
        override val openErrorVal: Any? = null
        override var openErrorVar: Any? = null

        constructor(warning: Int) : this()
        override fun openWarning() {}
        override val openWarningVal: Any? = null
        override var openWarningVar: Any? = null

        constructor(normal: Long) : this()
        override fun openNormal(): Int = 5
        override val openNormalVal: Any? = null
        override var openNormalVar: Any? = null
    }

    @Suppress("DEPRECATION_ERROR") fun test(hiddenNested: Hidden.Nested) {}
    @Suppress("DEPRECATION_ERROR") fun test(hiddenNestedNested: Hidden.Nested.Nested) {}
    @Suppress("DEPRECATION_ERROR") fun test(hiddenNestedInner: Hidden.Nested.Inner) {}
    @Suppress("DEPRECATION_ERROR") fun test(hiddenInner: Hidden.Inner) {}
    @Suppress("DEPRECATION_ERROR") fun test(hiddenInnerInner: Hidden.Inner.Inner) {}

    @Suppress("DEPRECATION_ERROR") fun test(topLevelHidden: TopLevelHidden) {}
    @Suppress("DEPRECATION_ERROR") fun test(topLevelHiddenNested: TopLevelHidden.Nested) {}
    @Suppress("DEPRECATION_ERROR") fun test(topLevelHiddenNestedNested: TopLevelHidden.Nested.Nested) {}
    @Suppress("DEPRECATION_ERROR") fun test(topLevelHiddenNestedInner: TopLevelHidden.Nested.Inner) {}
    @Suppress("DEPRECATION_ERROR") fun test(topLevelHiddenInner: TopLevelHidden.Inner) {}
    @Suppress("DEPRECATION_ERROR") fun test(topLevelHiddenInnerInner: TopLevelHidden.Inner.Inner) {}

    @Suppress("DEPRECATION_ERROR") fun test(extendingHiddenNested: ExtendingHidden.Nested) {}
    @Suppress("DEPRECATION_ERROR") fun test(extendingNestedInHidden: ExtendingNestedInHidden) {}

}

@Deprecated("hidden", level = DeprecationLevel.HIDDEN) class TopLevelHidden {
    class Nested {
        class Nested
        inner class Inner
    }

    inner class Inner {
        inner class Inner
    }
}

@Deprecated("hidden", level = DeprecationLevel.HIDDEN) fun hidden() {}
@Deprecated("hidden", level = DeprecationLevel.HIDDEN) val hiddenVal: Any? = null
@Deprecated("hidden", level = DeprecationLevel.HIDDEN) var hiddenVar: Any? = null

@Deprecated("error", level = DeprecationLevel.ERROR) fun error() {}
@Deprecated("error", level = DeprecationLevel.ERROR) val errorVal: Any? = null
@Deprecated("error", level = DeprecationLevel.ERROR) var errorVar: Any? = null

@Deprecated("warning", level = DeprecationLevel.WARNING) fun warning() {}
@Deprecated("warning", level = DeprecationLevel.WARNING) val warningVal: Any? = null
@Deprecated("warning", level = DeprecationLevel.WARNING) var warningVar: Any? = null

fun gc() {
    kotlin.native.internal.GC.collect()
}

class TestWeakRefs(private val frozen: Boolean) {
    private var obj: Any? = Any().also {
        if (frozen) it.freeze()
    }

    fun getObj() = obj!!

    fun clearObj() {
        obj = null
    }

    fun createCycle(): List<Any> {
        val node1 = Node(null)
        val node2 = Node(node1)
        node1.next = node2

        if (frozen) node1.freeze()

        return listOf(node1, node2)
    }

    private class Node(var next: Node?)
}

class SharedRefs {
    class MutableData {
        var x = 0

        fun update() { x += 1 }
    }

    fun createRegularObject(): MutableData = create { MutableData() }

    fun createLambda(): () -> Unit = create {
        var mutableData = 0
        {
            println(mutableData++)
        }
    }

    fun createCollection(): MutableList<Any> = create {
        mutableListOf()
    }

    fun createFrozenRegularObject() = createRegularObject().freeze()
    fun createFrozenLambda() = createLambda().freeze()
    fun createFrozenCollection() = createCollection().freeze()

    fun hasAliveObjects(): Boolean {
        kotlin.native.internal.GC.collect()
        return mustBeRemoved.any { it.get() != null }
    }

    private fun <T : Any> create(block: () -> T) = block()
            .also { mustBeRemoved += WeakReference(it) }

    private val mustBeRemoved = mutableListOf<WeakReference<*>>()
}

interface TestRememberNewObject {
    fun getObject(): Any
    fun waitForCleanup()
}

fun testRememberNewObject(test: TestRememberNewObject) {
    val obj = autoreleasepool { test.getObject() }
    test.waitForCleanup()
    assertNotEquals("", obj.toString()) // Likely crashes if object is removed.
}

open class ClassForTypeCheck

fun testClassTypeCheck(x: Any) = x is ClassForTypeCheck

interface InterfaceForTypeCheck

fun testInterfaceTypeCheck(x: Any) = x is InterfaceForTypeCheck

interface IAbstractInterface {
    fun foo(): Int
}

interface IAbstractInterface2 {
    fun foo() = 42
}

fun testAbstractInterfaceCall(x: IAbstractInterface) = x.foo()
fun testAbstractInterfaceCall2(x: IAbstractInterface2) = x.foo()

abstract class AbstractInterfaceBase : IAbstractInterface {
    override fun foo() = bar()

    abstract fun bar(): Int
}

abstract class AbstractInterfaceBase2 : IAbstractInterface2

abstract class AbstractInterfaceBase3 : IAbstractInterface {
    abstract override fun foo(): Int
}

var gh3525BaseInitCount = 0

open class GH3525Base {
    init {
        gh3525BaseInitCount++
    }
}

var gh3525InitCount = 0

object GH3525 : GH3525Base() {
    init {
        gh3525InitCount++
    }
}

class TestStringConversion {
    lateinit var str: Any
}

fun foo(a: kotlin.native.concurrent.AtomicReference<*>) {}

interface GH3825 {
    @Throws(MyException::class) fun call0(callback: () -> Boolean)
    @Throws(MyException::class) fun call1(doThrow: Boolean, callback: () -> Unit)
    @Throws(MyException::class) fun call2(callback: () -> Unit, doThrow: Boolean)
}

class GH3825KotlinImpl : GH3825 {
    override fun call0(callback: () -> Boolean) {
        if (callback()) throw MyException()
    }

    override fun call1(doThrow: Boolean, callback: () -> Unit) {
        if (doThrow) throw MyException()
        callback()
    }

    override fun call2(callback: () -> Unit, doThrow: Boolean) {
        if (doThrow) throw MyException()
        callback()
    }
}

@Throws(Throwable::class)
fun testGH3825(gh3825: GH3825) {
    var count = 0

    assertFailsWith<ObjCErrorException> { gh3825.call0({ true }) }
    gh3825.call0({ count += 1; false })
    assertEquals(1, count)

    assertFailsWith<ObjCErrorException> { gh3825.call1(true, { fail() }) }
    gh3825.call1(false, { count += 1 })
    assertEquals(2, count)

    assertFailsWith<ObjCErrorException> { gh3825.call2({ fail() }, true) }
    gh3825.call2({ count += 1 }, false)
    assertEquals(3, count)
}

fun mapBoolean2String(): Map<Boolean, String> = mapOf(Pair(false, "false"), Pair(true, "true"))
fun mapByte2Short(): Map<Byte, Short> = mapOf(Pair(-1, 2))
fun mapShort2Byte(): Map<Short, Byte> = mapOf(Pair(-2, 1))
fun mapInt2Long(): Map<Int, Long> = mapOf(Pair(-4, 8))
fun mapLong2Long(): Map<Long, Long> = mapOf(Pair(-8, 8))
fun mapUByte2Boolean(): Map<UByte, Boolean> = mapOf(Pair(0x80U, true))
fun mapUShort2Byte(): Map<UShort, Byte> = mapOf(Pair(0x8000U, 1))
fun mapUInt2Long(): Map<UInt, Long> = mapOf(Pair(0x7FFF_FFFFU, 7), Pair(0x8000_0000U, 8))
fun mapULong2Long(): Map<ULong, Long> = mapOf(Pair(0x8000_0000_0000_0000UL, 8))
fun mapFloat2Float(): Map<Float, Float> = mapOf(Pair(3.14f, 100f))
fun mapDouble2String(): Map<Double, String> = mapOf(Pair(2.718281828459045, "2.718281828459045"))

fun mutBoolean2String(): MutableMap<Boolean, String> = mutableMapOf(Pair(false, "false"), Pair(true, "true"))
fun mutByte2Short(): MutableMap<Byte, Short> = mutableMapOf(Pair(-1, 2))
fun mutShort2Byte(): MutableMap<Short, Byte> = mutableMapOf(Pair(-2, 1))
fun mutInt2Long(): MutableMap<Int, Long> = mutableMapOf(Pair(-4, 8))
fun mutLong2Long(): MutableMap<Long, Long> = mutableMapOf(Pair(-8, 8))
fun mutUByte2Boolean(): MutableMap<UByte, Boolean> = mutableMapOf(Pair(128U, true))
fun mutUShort2Byte(): MutableMap<UShort, Byte> = mutableMapOf(Pair(32768U, 1))
fun mutUInt2Long(): MutableMap<UInt, Long> = mutableMapOf(Pair(0x8000_0000U, 8))
fun mutULong2Long(): MutableMap<ULong, Long> = mutableMapOf(Pair(0x8000_0000_0000_0000UL, 8))
fun mutFloat2Float(): MutableMap<Float, Float> = mutableMapOf(Pair(3.14f, 100f))
fun mutDouble2String(): MutableMap<Double, String> = mutableMapOf(Pair(2.718281828459045, "2.718281828459045"))

interface Foo_FakeOverrideInInterface<T> {
    fun foo(t: T?)
}

interface Bar_FakeOverrideInInterface : Foo_FakeOverrideInInterface<String>

fun callFoo_FakeOverrideInInterface(obj: Bar_FakeOverrideInInterface) {
    obj.foo(null)
}