// EMIT_JVM_TYPE_ANNOTATIONS
// FULL_JDK
// LANGUAGE: +ContextParameters

package test

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

open class Base
interface First
interface Second

class Methods(val constructorParameter: @Anno @SourceAnno String) {
    constructor(value: @Anno List<@SourceAnno String>) : this(value.first())

    fun plain(first: @Anno String, second: @SourceAnno Int): @Anno @SourceAnno String = first
    fun primitive(): @Anno @SourceAnno Int = 0
    fun unit(): @Anno @SourceAnno Unit {}

    fun generic(first: List<@Anno String>, second: @SourceAnno List<Number>): List<@SourceAnno String> = first

    fun <@Anno T, @SourceAnno U> typeVariables(first: @SourceAnno T, second: @Anno U): @Anno T = first

    fun <@Anno @SourceAnno T, U> bounds(first: T, second: U): T where
            T : @Anno Base, T : @SourceAnno First, T : @Anno @SourceAnno Second,
            U : @Anno First, U : @SourceAnno Second = first

    fun <T, U, V> typeVarBounds(first: T, second: U, third: V): T where
            U : @Anno T, U : @SourceAnno First,
            V : @Anno First, V : @SourceAnno T = first

    fun @Anno String.extension(value: @SourceAnno Int): @SourceAnno String = this

    context(first: @Anno String, second: @SourceAnno Number)
    fun @SourceAnno String.contextual(value: @Anno Int): @Anno String = this

    var property: @Anno @SourceAnno String = ""

    var @Anno String.extensionProperty: @SourceAnno String
        get() = this
        set(value) {}

    suspend fun suspending(value: @Anno String): @SourceAnno List<@Anno String> = listOf(value)

    @JvmOverloads
    fun overloads(first: @Anno String, second: @SourceAnno String = ""): @SourceAnno String = first

    @OptIn(ExperimentalUnsignedTypes::class)
    @JvmName("unsignedVarargs")
    fun unsignedVarargs(vararg values: @Anno UInt) {}

    inner class Inner(value: @Anno String, generic: List<@SourceAnno String>)
    inner class PlainInner(value: @Anno @SourceAnno String)

    companion object {
        @JvmStatic
        fun staticMethod(value: @SourceAnno String): @Anno String = value
    }
}

enum class Enum(value: @Anno String, generic: List<@SourceAnno String>) {
    ;
}

enum class PlainEnum(value: @Anno @SourceAnno String) { ; }

interface AbstractMethods {
    fun abstractMethod(value: @Anno String): @SourceAnno String
    fun defaultMethod(value: @SourceAnno String): @Anno String = value
}

interface GenericMethods<T> {
    fun genericDefault(value: @Anno T): @SourceAnno T = value
}

fun topLevel(value: @Anno String): @SourceAnno String = value
