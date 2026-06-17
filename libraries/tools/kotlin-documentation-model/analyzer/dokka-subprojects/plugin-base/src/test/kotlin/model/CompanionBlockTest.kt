/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalDokkaApi::class)

package model

import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.IsCompanion
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.GenericTypeConstructor
import utils.AbstractModelTest
import utils.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for KEEP-0449 companion blocks: members declared inside a `companion { ... }`
 * block (no `object` keyword) live in the static scope of the enclosing classlike.
 *
 * They are represented in Dokka in the same way as Java static declarations and
 * enum synthetic declarations (`values` / `valueOf` / `entries`):
 *   - the [org.jetbrains.dokka.links.DRI]'s [org.jetbrains.dokka.links.Callable.isCompanion] is `true`
 *   - the documentable carries [IsCompanion] in its extra container
 *
 * See [KEEP-0449](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md).
 */
class CompanionBlockTest : AbstractModelTest("/src/main/kotlin/companions/Test.kt", "companions") {


    @Test
    fun `companion-block function is marked as companion-block member`() {
        inlineModelTest(
            """
            |class Vector(val x: Double, val y: Double) {
            |    companion {
            |        fun unit(): Vector = Vector(1.0, 1.0)
            |    }
            |}
            """.trimIndent()
        ) {
            with((this / "companions" / "Vector").cast<DClass>()) {
                val unit = functions.firstOrNull { it.name == "unit" }
                    .assertNotNull("companion-block function 'unit'")
                unit.extra[IsCompanion].assertNotNull("CompanionBlockMember on 'unit'")
                assertEquals(true, @OptIn(ExperimentalDokkaApi::class) unit.dri.callable?.isCompanion, "DRI for 'unit' should be marked isStatic=true")
            }
        }
    }

    @Test
    fun `companion-block property is marked as companion-block member`() {
        inlineModelTest(
            """
            |class Vector(val x: Double, val y: Double) {
            |    companion {
            |        val Zero: Vector = Vector(0.0, 0.0)
            |        const val Dimensions: Int = 2
            |    }
            |}
            """.trimIndent()
        ) {
            with((this / "companions" / "Vector").cast<DClass>()) {
                val zero = properties.firstOrNull { it.name == "Zero" }
                    .assertNotNull("companion-block property 'Zero'")
                val dimensions = properties.firstOrNull { it.name == "Dimensions" }
                    .assertNotNull("companion-block property 'Dimensions'")

                zero.extra[IsCompanion].assertNotNull("CompanionBlockMember on 'Zero'")
                dimensions.extra[IsCompanion].assertNotNull("CompanionBlockMember on 'Dimensions'")
                assertEquals(true, @OptIn(ExperimentalDokkaApi::class) zero.dri.callable?.isCompanion)
                assertEquals(true, @OptIn(ExperimentalDokkaApi::class) dimensions.dri.callable?.isCompanion)
            }
        }
    }

    @Test
    fun `instance member is not marked as companion-block member`() {
        inlineModelTest(
            """
            |class Vector(val x: Double, val y: Double) {
            |    fun length(): Double = 0.0
            |    val name: String = "v"
            |    companion {
            |        fun unit(): Vector = Vector(1.0, 1.0)
            |    }
            |}
            """.trimIndent()
        ) {
            with((this / "companions" / "Vector").cast<DClass>()) {
                val length = functions.firstOrNull { it.name == "length" }
                    .assertNotNull("instance function 'length'")
                val name = properties.firstOrNull { it.name == "name" }
                    .assertNotNull("instance property 'name'")

                assertEquals(null, length.extra[IsCompanion])
                assertEquals(false, @OptIn(ExperimentalDokkaApi::class) length.dri.callable?.isCompanion)
                assertEquals(null, name.extra[IsCompanion])
                assertEquals(false, @OptIn(ExperimentalDokkaApi::class) name.dri.callable?.isCompanion)
            }
        }
    }

    @Test
    fun `companion-block and instance with same name have distinct DRIs`() {
        inlineModelTest(
            """
            |class Holder {
            |    fun foo() {}
            |    companion {
            |        @kotlin.jvm.JvmName("fooStatic")
            |        fun foo() {}
            |    }
            |}
            """.trimIndent()
        ) {
            with((this / "companions" / "Holder").cast<DClass>()) {
                val foos = functions.filter { it.name == "foo" }
                assertEquals(2, foos.size, "expected an instance and a companion-block 'foo'")

                val instance = foos.single { it.extra[IsCompanion] == null }
                val companion = foos.single { it.extra[IsCompanion] != null }

                assertEquals(false, @OptIn(ExperimentalDokkaApi::class) instance.dri.callable?.isCompanion)
                assertEquals(true, @OptIn(ExperimentalDokkaApi::class) companion.dri.callable?.isCompanion)
                assertTrue(
                    instance.dri.callable?.signature() != companion.dri.callable?.signature(),
                    "Callable signatures must differ between instance and companion-block members"
                )
                assertEquals(
                    true,
                    companion.dri.callable?.signature()?.endsWith("/companion"),
                    "Companion-block signature must end with the /companion marker"
                )
            }
        }
    }

    @Test
    fun `enum synthetic declarations are marked as companion-block members`() {
        inlineModelTest(
            """
            |enum class E { A, B }
            """.trimIndent()
        ) {
            with((this / "companions" / "E").cast<DEnum>()) {
                val values = functions.firstOrNull { it.name == "values" }
                    .assertNotNull("synthetic enum function 'values'")
                val valueOf = functions.firstOrNull { it.name == "valueOf" }
                    .assertNotNull("synthetic enum function 'valueOf'")

                values.extra[IsCompanion].assertNotNull("CompanionBlockMember on 'values'")
                valueOf.extra[IsCompanion].assertNotNull("CompanionBlockMember on 'valueOf'")
                assertEquals(true, @OptIn(ExperimentalDokkaApi::class) values.dri.callable?.isCompanion)
                assertEquals(true, @OptIn(ExperimentalDokkaApi::class) valueOf.dri.callable?.isCompanion)
            }
        }
    }

    @Test
    fun `companion object member is not treated as companion-block member`() {
        inlineModelTest(
            """
            |class Holder {
            |    companion object {
            |        fun create(): Holder = Holder()
            |    }
            |}
            """.trimIndent()
        ) {
            // a `companion object` is rendered as a nested classlike — its members are NOT
            // companion-block members. They live as functions on the companion object,
            // not in the enclosing class's static scope.
            with((this / "companions" / "Holder").cast<DClass>()) {
                // No companion-block functions on the enclosing class.
                val staticOnHolder = functions.filter { it.extra[IsCompanion] != null }
                assertEquals(emptyList(), staticOnHolder)
                val staticPropsOnHolder = properties.filter { it.extra[IsCompanion] != null }
                assertEquals(emptyList(), staticPropsOnHolder)
            }
        }
    }

    // ----------------------------------------------------------------------
    // KEEP-0449 companion extensions: top-level extensions declared with the
    // `companion` modifier, e.g. `companion fun Vector.unit(): Vector`.
    //
    // They are represented in Dokka the same way as companion-block members:
    // the documentable carries the [CompanionBlockMember] extra (driven by
    // the Analysis API's `KaCallableSymbol.isCompanion` flag).
    // ----------------------------------------------------------------------

    @Test
    fun `companion extension function is marked as companion-block member`() {
        inlineModelTest(
            """
            |class Vector(val x: Double, val y: Double)
            |
            |companion fun Vector.unit(): Vector = Vector(1.0, 1.0)
            """.trimIndent()
        ) {
            val pkg = packages.single()
            val ext = pkg.functions.firstOrNull { it.name == "unit" }
                .assertNotNull("top-level companion extension 'unit'")
            ext.extra[IsCompanion].assertNotNull("CompanionBlockMember on companion extension 'unit'")
            // sanity-check it is in fact an extension: receiver is non-null and resolves to Vector
            assertTrue(ext.receiver != null, "companion extension must keep its receiver in the documentable")
            assertEquals(GenericTypeConstructor(DRI("companions", "Vector"), emptyList()), ext.receiver?.type)
        }
    }

    @Test
    fun `companion extension property is marked as companion-block member`() {
        inlineModelTest(
            """
            |class Vector(val x: Double, val y: Double)
            |
            |companion val Vector.UnitX: Vector get() = Vector(1.0, 0.0)
            """.trimIndent()
        ) {
            val pkg = packages.single()
            val ext = pkg.properties.firstOrNull { it.name == "UnitX" }
                .assertNotNull("top-level companion extension property 'UnitX'")
            ext.extra[IsCompanion].assertNotNull("CompanionBlockMember on companion extension 'UnitX'")
            assertTrue(ext.receiver != null, "companion extension property must keep its receiver")
            assertEquals(GenericTypeConstructor(DRI("companions", "Vector"), emptyList()), ext.receiver?.type)
        }
    }

    @Test
    fun `regular top-level extension is not marked as companion-block member`() {
        inlineModelTest(
            """
            |class Vector(val x: Double, val y: Double)
            |
            |fun Vector.length(): Double = 0.0
            |val Vector.name: String get() = "v"
            """.trimIndent()
        ) {
            val pkg = packages.single()
            val length = pkg.functions.firstOrNull { it.name == "length" }
                .assertNotNull("plain extension 'length'")
            val nameExt = pkg.properties.firstOrNull { it.name == "name" }
                .assertNotNull("plain extension property 'name'")

            assertEquals(null, @OptIn(ExperimentalDokkaApi::class) length.extra[IsCompanion])
            assertEquals(null, @OptIn(ExperimentalDokkaApi::class) nameExt.extra[IsCompanion])
        }
    }

    @Test
    fun `companion-block scope is empty when there are only regular extensions`() {
        inlineModelTest(
            """
            |class Vector(val x: Double, val y: Double)
            |
            |fun Vector.length(): Double = 0.0
            """.trimIndent()
        ) {
            val pkg = packages.single()
            val companionExtensions = pkg.functions.filter { it.extra[IsCompanion] != null }
            assertEquals(emptyList<DFunction>(), companionExtensions)
        }
    }
}
