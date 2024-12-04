/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.core.renderTypeForUnsafeNumberAnnotation
import org.jetbrains.kotlin.types.Variance
import org.junit.Test
import kotlin.test.assertEquals

class UnsafeNumberAnnotationTypeRenderingTest {
    @Test
    fun `test simple class type with no arguments`() {
        val type = CirClassType.createInterned(
            classId = CirEntityId.create("kotlin/String"), outerType = null, arguments = emptyList(), isMarkedNullable = false,
        )
        assertEquals("kotlin.String", renderTypeForUnsafeNumberAnnotation(type))
    }

    @Test
    fun `test simple nullable class type`() {
        val type = CirClassType.createInterned(
            classId = CirEntityId.create("kotlin/String"), outerType = null, arguments = emptyList(), isMarkedNullable = true,
        )
        assertEquals("kotlin.String?", renderTypeForUnsafeNumberAnnotation(type))
    }

    @Test
    fun `test expanded type alias type with no arguments`() {
        val type = CirTypeAliasType.createInterned(
            typeAliasId = CirEntityId.create("T"),
            underlyingType = CirClassType.createInterned(
                classId = CirEntityId.create("kotlin/String"), outerType = null, arguments = emptyList(), isMarkedNullable = false,
            ),
            arguments = emptyList(),
            isMarkedNullable = false,
        )
        assertEquals("kotlin.String", renderTypeForUnsafeNumberAnnotation(type))
    }

    @Test
    fun `test class type with arguments`() {
        val type = CirClassType.createInterned(
            classId = CirEntityId.create("kotlin/Triple"), outerType = null, isMarkedNullable = false,
            arguments = listOf(
                CirRegularTypeProjection(
                    projectionKind = Variance.INVARIANT, type = CirClassType.createInterned(
                        classId = CirEntityId.create("kotlin/String"), outerType = null, arguments = emptyList(), isMarkedNullable = false,
                    )
                ),
                CirRegularTypeProjection(
                    projectionKind = Variance.OUT_VARIANCE, type = CirClassType.createInterned(
                        classId = CirEntityId.create("kotlin/Number"), outerType = null, arguments = emptyList(), isMarkedNullable = false,
                    )
                ),
                CirStarTypeProjection,
            ),
        )
        assertEquals("kotlin.Triple<kotlin.String, out kotlin.Number, *>", renderTypeForUnsafeNumberAnnotation(type))
    }

    @Test
    fun `test type alias type with an argument`() {
        val type = CirTypeAliasType.createInterned(
            typeAliasId = CirEntityId.create("T"), isMarkedNullable = false, arguments = emptyList(),
            underlyingType = CirClassType.createInterned(
                classId = CirEntityId.create("kotlin/Triple"), outerType = null, isMarkedNullable = false,
                arguments = listOf(
                    CirRegularTypeProjection(
                        projectionKind = Variance.INVARIANT, type = CirClassType.createInterned(
                            classId = CirEntityId.create("kotlin/String"),
                            outerType = null,
                            arguments = emptyList(),
                            isMarkedNullable = false,
                        )
                    ),
                    CirRegularTypeProjection(
                        projectionKind = Variance.OUT_VARIANCE, type = CirClassType.createInterned(
                            classId = CirEntityId.create("kotlin/Number"),
                            outerType = null,
                            arguments = emptyList(),
                            isMarkedNullable = false,
                        )
                    ),
                    CirStarTypeProjection,
                ),
            ),
        )
        assertEquals("kotlin.Triple<kotlin.String, out kotlin.Number, *>", renderTypeForUnsafeNumberAnnotation(type))
    }

    @Test
    fun `test flexible type`() {
        val type = CirFlexibleType(
            lowerBound = CirClassType.createInterned(
                classId = CirEntityId.create("kotlin/String"), outerType = null, arguments = emptyList(), isMarkedNullable = false,
            ),
            upperBound = CirClassType.createInterned(
                classId = CirEntityId.create("kotlin/String"), outerType = null, arguments = emptyList(), isMarkedNullable = true,
            )
        )
        assertEquals("kotlin.String..kotlin.String?", renderTypeForUnsafeNumberAnnotation(type))
    }

    @Test
    fun `test flexible type argument`() {
        val type = CirClassType.createInterned(
            classId = CirEntityId.create("kotlin.Array"), outerType = null, isMarkedNullable = false, arguments = listOf(
                CirRegularTypeProjection(
                    projectionKind = Variance.OUT_VARIANCE,
                    type = CirFlexibleType(
                        lowerBound = CirClassType.createInterned(
                            classId = CirEntityId.create("kotlin/String"),
                            outerType = null,
                            arguments = emptyList(),
                            isMarkedNullable = false,
                        ),
                        upperBound = CirClassType.createInterned(
                            classId = CirEntityId.create("kotlin/String"),
                            outerType = null,
                            arguments = emptyList(),
                            isMarkedNullable = true,
                        )
                    )
                )
            )
        )
        assertEquals("kotlin.Array<out kotlin.String..kotlin.String?>", renderTypeForUnsafeNumberAnnotation(type))
    }

    @Test
    fun `test nested type argument`() {
        val type = CirClassType.createInterned(
            classId = CirEntityId.create("kotlin.Array"), outerType = null, isMarkedNullable = false, arguments = listOf(
                CirRegularTypeProjection(
                    projectionKind = Variance.OUT_VARIANCE,
                    type = CirClassType.createInterned(
                        classId = CirEntityId.create("kotlin.Array"), outerType = null, isMarkedNullable = false, arguments = listOf(
                            CirRegularTypeProjection(
                                projectionKind = Variance.IN_VARIANCE,
                                type = CirClassType.createInterned(
                                    classId = CirEntityId.create("kotlin/String"),
                                    outerType = null,
                                    arguments = emptyList(),
                                    isMarkedNullable = false,
                                )
                            )
                        )
                    )
                )
            )
        )

        assertEquals("kotlin.Array<out kotlin.Array<in kotlin.String>>", renderTypeForUnsafeNumberAnnotation(type))
    }

    @Test
    fun `test type parameter type in type argument`() {
        val type = CirClassType.createInterned(
            classId = CirEntityId.create("kotlin.Array"), outerType = null, isMarkedNullable = false, arguments = listOf(
                CirRegularTypeProjection(
                    projectionKind = Variance.INVARIANT,
                    type = CirTypeParameterType.createInterned(index = 0, isMarkedNullable = true)
                )
            )
        )

        assertEquals("kotlin.Array<#0?>", renderTypeForUnsafeNumberAnnotation(type))
    }

    @Test
    fun `test combined type`() {
        val type = CirClassType.createInterned(
            classId = CirEntityId.create("kotlin.Array"), outerType = null, isMarkedNullable = false, arguments = listOf(
                CirRegularTypeProjection(
                    projectionKind = Variance.OUT_VARIANCE,
                    type = CirClassType.createInterned(
                        classId = CirEntityId.create("kotlin/Pair"), outerType = null, isMarkedNullable = true,
                        arguments = listOf(
                            CirStarTypeProjection,
                            CirRegularTypeProjection(
                                projectionKind = Variance.IN_VARIANCE, type = CirFlexibleType(
                                    lowerBound = CirClassType.createInterned(
                                        classId = CirEntityId.create("kotlin/String"),
                                        outerType = null,
                                        arguments = emptyList(),
                                        isMarkedNullable = false,
                                    ),
                                    upperBound = CirClassType.createInterned(
                                        classId = CirEntityId.create("kotlin/String"),
                                        outerType = null,
                                        arguments = emptyList(),
                                        isMarkedNullable = true,
                                    )
                                )
                            ),
                        ),
                    )
                )
            )
        )

        assertEquals(
            "kotlin.Array<out kotlin.Pair<*, in kotlin.String..kotlin.String?>?>",
            renderTypeForUnsafeNumberAnnotation(type)
        )
    }
}
