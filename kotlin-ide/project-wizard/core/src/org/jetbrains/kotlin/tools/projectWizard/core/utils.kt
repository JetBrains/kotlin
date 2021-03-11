package org.jetbrains.kotlin.tools.projectWizard.core

import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.ReadOnlyProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

inline infix fun <A, B, C> ((A) -> B).andThen(crossinline then: (B) -> C): (A) -> C =
    { then(this(it)) }

inline fun <A, B, C> compose(crossinline function: (A) -> B, crossinline then: (B) -> C): (A) -> C =
    { then(function(it)) }


/**
 * Composes two function together like normal composition if first function returns non-null value;
 * Otherwise, applies second function
 */
inline infix fun <A : Any> ((A) -> A?).andThenNullable(crossinline then: (A) -> A?): (A) -> A? =
    { initial ->
        val thisResult = this(initial)
        if (thisResult != null) then(thisResult) else then(initial)
    }


@Suppress("NOTHING_TO_INLINE")
inline fun <T> idFunction(): (T) -> T = { it }

operator fun Path.div(@NonNls other: String): Path =
    resolve(other)

operator fun Path.div(other: Path): Path =
    resolve(other.toString())

@JvmName("divNullable")
operator fun Path.div(other: Path?): Path =
    other?.let { resolve(other.toString()) } ?: this

operator fun @receiver:NonNls String.div(other: Path): Path =
    Paths.get(this).resolve(other)

operator fun @receiver:NonNls String.div(@NonNls other: String): Path =
    Paths.get(this).resolve(other)

fun @receiver:NonNls String.asPath(): Path = Paths.get(this)

fun Path.asStringWithUnixSlashes() = toString().replace('\\', '/')

fun <T : Any> safe(operation: () -> T): TaskResult<T> =
    try {
        Success(operation())
    } catch (e: IOException) {
        Failure(IOError(e))
    } catch (e: Exception) {
        Failure(ExceptionErrorImpl(e))
    }

internal inline fun <T> cached(crossinline createValue: (name: String) -> T) = object : ReadOnlyProperty<Any?, T> {
    var value: T? = null
    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        value ?: createValue(property.name).also { value = it }
}

@PublishedApi
internal inline fun <reified T> Any?.safeAs(): T? = this as? T

@Suppress("NOTHING_TO_INLINE", "unused")
inline fun Any?.ignore() = Unit

internal fun <T> T.asSingletonList() = listOf(this)

@DslMarker
annotation class Builder

@Builder
class ListBuilder<T> {
    private val irs = mutableListOf<T>()

    operator fun T.unaryPlus() {
        irs += this
    }

    fun addIfNotNull(value: T?) {
        if (value != null) irs += value
    }

    operator fun List<T>.unaryPlus() {
        irs += this
    }

    // out here for working with vararg arguments
    operator fun Array<out T>.unaryPlus() {
        irs += this
    }

    fun isNotEmpty(): Boolean = irs.isNotEmpty()

    fun build() = irs.toList()
}

fun <T> buildList(builder: ListBuilder<T>.() -> Unit) =
    ListBuilder<T>().apply(builder).build()

fun <T> buildPersistenceList(builder: ListBuilder<T>.() -> Unit) =
    buildList(builder).toPersistentList()

object RandomIdGenerator {
    private val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private const val idLength = 8
    fun generate() = (0 until idLength).joinToString(separator = "") {
        chars[Random.nextInt(chars.size)].toString()
    }

}