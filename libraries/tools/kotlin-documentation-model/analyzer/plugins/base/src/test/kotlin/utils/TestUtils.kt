package utils

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.P
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.collections.orEmpty

@DslMarker
annotation class TestDSL

@TestDSL
abstract class ModelDSL : BaseAbstractTest() {
    operator fun Documentable?.div(name: String): Documentable? =
        this?.children?.find { it.name == name }

    inline fun <reified T : Documentable> Documentable?.cast(): T =
        (this as? T).assertNotNull()
}

@TestDSL
interface AssertDSL {
    infix fun Any?.equals(other: Any?) = assertEquals(other, this)
    infix fun Collection<Any>?.allEquals(other: Any?) =
        this?.also { c -> c.forEach { it equals other } } ?: run { assert(false) { "Collection is empty" } }
    infix fun <T> Collection<T>?.exists(e: T) {
        assertTrue(this.orEmpty().isNotEmpty(), "Collection cannot be null or empty")
        assertTrue(this!!.any{it == e}, "Collection doesn't contain $e")
    }

    infix fun <T> Collection<T>?.counts(n: Int) = this.orEmpty().assertCount(n)

    infix fun <T> T?.notNull(name: String): T = this.assertNotNull(name)

    fun <T> Collection<T>.assertCount(n: Int, prefix: String = "") =
        assert(count() == n) { "${prefix}Expected $n, got ${count()}" }
}

inline fun <reified T : Any> Any?.assertIsInstance(name: String): T =
    this.let { it as? T } ?: throw AssertionError("$name should not be null")

fun TagWrapper.text(): String = when (val t = this) {
    is NamedTagWrapper -> "${t.name}: [${t.root.text()}]"
    else -> t.root.text()
}

fun DocTag.text(): String = when (val t = this) {
    is Text -> t.body
    is Code -> t.children.joinToString("\n") { it.text() }
    is P -> t.children.joinToString("") { it.text() } + "\n"
    else -> t.children.joinToString("") { it.text() }
}

fun <T : Documentable> T?.comments(): String = docs().map { it.text() }
    .joinToString(separator = "\n") { it }

fun <T> T?.assertNotNull(name: String = ""): T = this ?: throw AssertionError("$name should not be null")

fun <T : Documentable> T?.docs() = this?.documentation.orEmpty().values.flatMap { it.children }

val DClass.supers
    get() = supertypes.flatMap { it.component2() }

val Bound.name: String?
    get() = when (this) {
        is Nullable -> inner.name
        is TypeParameter -> name
        is PrimitiveJavaType -> name
        is TypeConstructor -> dri.classNames
        is JavaObject -> "Object"
        is Void -> "void"
        is Dynamic -> "dynamic"
        is UnresolvedBound -> "<ERROR CLASS>"
        is TypeAliased -> typeAlias.name
    }
