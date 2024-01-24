/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utils

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.P
import kotlin.collections.orEmpty
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.asserter
import kotlin.test.fail

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
        this?.onEach { it equals other } ?: run { fail("Collection is empty") }
    infix fun <T> Collection<T>?.exists(e: T) {
        assertTrue(this.orEmpty().isNotEmpty(), "Collection cannot be null or empty")
        assertTrue(this!!.any{it == e}, "Collection doesn't contain $e")
    }

    infix fun <T> Collection<T>?.counts(n: Int) = this.orEmpty().assertCount(n)

    infix fun <T> T?.notNull(name: String): T = this.assertNotNull(name)

    fun <T> Collection<T>.assertCount(n: Int, prefix: String = "") =
        assertEquals(n, count(), "${prefix}Expected $n, got ${count()}")
}

/*
 * TODO replace with kotlin.test.assertContains after migrating to Kotlin 1.5+
 */
internal fun <T> assertContains(iterable: Iterable<T>, element: T, ) {
    asserter.assertTrue(
        { "Expected the collection to contain the element.\nCollection <$iterable>, element <$element>." },
        iterable.contains(element)
    )
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
        is DefinitelyNonNullable -> inner.name
        is TypeParameter -> name
        is PrimitiveJavaType -> name
        is TypeConstructor -> dri.classNames
        is JavaObject -> "Object"
        is Void -> "void"
        is Dynamic -> "dynamic"
        is UnresolvedBound -> "<ERROR CLASS>"
        is TypeAliased -> typeAlias.name
    }
