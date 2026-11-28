/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.model.IsCompanion
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInAnalysisApi
import utils.AbstractModelTest
import utils.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for KEEP-0449 companion blocks: members declared inside a `companion { ... }`
 * block (no `object` keyword) live in the static scope of the enclosing classlike.
 *
 * They are represented in Dokka in the same way as Java static declarations and
 * enum synthetic declarations (`values` / `valueOf` / `entries`):
 * the documentable carries [IsCompanion] in its extra container (companion-ness also
 * participates in the callable's declaration ID through the signature hash).
 *
 * See [KEEP-0449](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companion-block-extension.md).
 */
@MustRunOnChangesInAnalysisApi
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
                assertEquals(null, name.extra[IsCompanion])
            }
        }
    }

    @Test
    fun `companion-block and instance with same name have distinct declaration ids`() {
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

                // The companion marker is part of the hashed signature, so the IDs differ
                assertNotEquals(
                    instance.declarationId,
                    companion.declarationId,
                    "Declaration IDs must differ between instance and companion-block members"
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
    // the documentable carries the [IsCompanion] extra (driven by
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
            assertEquals(GenericTypeConstructor(KdElementId("companions/Vector"), emptyList()), ext.receiver.type)
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
            ext.extra[IsCompanion].assertNotNull("CompanionBlockMember on companion extension property 'UnitX'")
            assertTrue(ext.receiver != null, "companion extension property must keep its receiver")
            assertEquals(GenericTypeConstructor(KdElementId("companions/Vector"), emptyList()), ext.receiver.type)
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

            assertEquals(null, length.extra[IsCompanion])
            assertEquals(null, nameExt.extra[IsCompanion])
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
