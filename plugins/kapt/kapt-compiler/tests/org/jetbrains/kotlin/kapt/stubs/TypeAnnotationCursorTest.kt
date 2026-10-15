/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import org.jetbrains.org.objectweb.asm.TypePath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TypeAnnotationCursorTest {
    @Test
    fun annotationsAtTheCurrentNodePreserveOrderAndDuplicates() {
        val cursor = cursor(null to "runtime", "0;" to "argument", null to "source", null to "runtime")

        assertEquals(listOf("runtime", "source", "runtime"), cursor.annotationsHere())
        assertEquals(listOf("argument"), cursor.typeArgument(0).annotationsHere())
    }

    @Test
    fun droppingNestedAnnotationsPreservesCurrentNodeOrderAndDuplicates() {
        val cursor = cursor(null to "runtime", "0;" to "argument", null to "source", "[" to "element", null to "runtime")

        assertEquals(listOf(null to "runtime", null to "source", null to "runtime"), cursor.annotationsHereAsRoot())
        assertEquals(listOf("argument"), cursor.typeArgument(0).annotationsHere())
        assertEquals(listOf("element"), cursor.arrayElement().annotationsHere())
    }

    @Test
    fun droppingNestedAnnotationsRebasesTheCurrentNode() {
        val cursor = cursor(null to "root", "[1;*" to "bound", "[1;*0;" to "nested", "[0;" to "sibling")
        val bound = cursor.arrayElement().typeArgument(1).wildcardBound()

        assertEquals(listOf(null to "bound"), bound.annotationsHereAsRoot())
        assertEquals(listOf(null to "bound", "0;" to "nested"), bound.paths())
        assertEquals(emptyList<Pair<TypePath?, String>>(), cursor.arrayElement().annotationsHereAsRoot())
    }

    @Test
    fun siblingCursorsDoNotAffectEachOtherOrTheirParent() {
        val cursor = cursor(null to "root", "0;" to "first", "1;" to "second")
        val first = cursor.typeArgument(0)
        val second = cursor.typeArgument(1)

        assertEquals(listOf("first"), first.annotationsHere())
        assertEquals(listOf("second"), second.annotationsHere())
        assertEquals(listOf("root"), cursor.annotationsHere())
        assertEquals(listOf("first"), cursor.typeArgument(0).annotationsHere())
        assertEquals(emptyList<String>(), cursor.typeArgument(2).annotationsHere())
    }

    @Test
    fun mixedPathsAreRebasedForTheSignatureParser() {
        val cursor = cursor("[1;" to "wildcard", "[1;*" to "bound", "[1;*0;[." to "nested", "[0;" to "sibling")
        val argument = cursor.arrayElement().typeArgument(1)

        assertEquals(listOf("wildcard"), argument.annotationsHere())
        assertEquals(
            listOf(null to "wildcard", "*" to "bound", "*0;[." to "nested"),
            argument.paths(),
        )
        assertEquals(listOf("bound"), argument.wildcardBound().annotationsHere())
        assertEquals(
            listOf(null to "bound", "0;[." to "nested"),
            argument.wildcardBound().paths(),
        )
    }

    @Test
    fun arrayStepsAreNotGenericArgumentSteps() {
        val cursor = cursor("[" to "element", "0;" to "argument")

        assertEquals(listOf("element"), cursor.arrayElement().annotationsHere())
        assertEquals(listOf("argument"), cursor.typeArgument(0).annotationsHere())
    }

    @Test
    fun emptySubtreesRemainEmpty() {
        val cursor = cursor(null to "root").arrayElement().typeArgument(0).wildcardBound()

        assertEquals(emptyList<String>(), cursor.annotationsHere())
        assertEquals(emptyList<Pair<String?, String>>(), cursor.paths())
        assertEquals(emptyList<Pair<TypePath?, String>>(), cursor.annotationsHereAsRoot())
    }

    @Test
    fun inputListIsSnapshotted() {
        val annotations = mutableListOf<Pair<TypePath?, String>>(null to "first")
        val cursor = TypeAnnotationCursor(annotations)
        annotations += null to "second"

        assertEquals(listOf("first"), cursor.annotationsHere())
    }

    @Test
    fun negativeTypeArgumentIndexIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { cursor().typeArgument(-1) }
    }

    private fun cursor(vararg annotations: Pair<String?, String>): TypeAnnotationCursor<String> =
        TypeAnnotationCursor(annotations.map { [path, annotation] -> TypePath.fromString(path) to annotation })

    private fun TypeAnnotationCursor<String>.paths(): List<Pair<String?, String>> =
        asRelativeTypeAnnotations().map { [path, annotation] -> path?.toString() to annotation }
}
