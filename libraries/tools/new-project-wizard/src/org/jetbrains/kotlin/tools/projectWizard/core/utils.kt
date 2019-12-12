package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.Setting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.BodyIR
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.ReadOnlyProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

operator fun Path.div(other: String): Path =
    resolve(other)

operator fun Path.div(other: Path): Path =
    resolve(other)

operator fun String.div(other: Path): Path =
    Paths.get(this).resolve(other)

operator fun String.div(other: String): Path =
    Paths.get(this).resolve(other)

fun String.asPath(): Path = Paths.get(this)

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

@Suppress("NOTHING_TO_INLINE")
inline fun Any?.ignore() = Unit

internal fun <T> T.asSingletonList() = listOf(this)

inline fun <reified R> Iterable<*>.filterIsInstanceWith(predicate: (R) -> Boolean): List<R> {
    val result = mutableListOf<R>()
    for (element in this) {
        if (element is R && predicate(element)) {
            result += element
        }
    }
    return result
}

fun Path.asAbsolute(prefix: Path) =
    if (isAbsolute) this else prefix / this

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

    fun build() = irs.toList()
}

fun <T> buildList(builder: ListBuilder<T>.() -> Unit) =
    ListBuilder<T>().apply(builder).build()

object RandomIdGenerator {
    private val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private const val idLength = 8
    fun generate() = (0 until idLength).joinToString(separator = "") {
        chars[Random.nextInt(chars.size)].toString()
    }

}