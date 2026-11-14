/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import org.jetbrains.org.objectweb.asm.TypePath

/** An immutable view of type annotations relative to a subtree of the mapped Java type. */
internal class TypeAnnotationCursor<Annotation> private constructor(
    private val typeAnnotations: List<Pair<TypePath?, Annotation>>,
    // current offset in type paths
    private val offset: Int,
) {
    constructor(typeAnnotations: List<Pair<TypePath?, Annotation>>) : this(typeAnnotations.toList(), 0)

    /** An empty cursor stays empty for every child, so no annotation can appear at any depth below it. */
    val isEmpty: Boolean get() = typeAnnotations.isEmpty()

    fun annotationsHere(): List<Annotation> =
        typeAnnotations.filter { [path, _] -> (path?.length ?: 0) == offset }.map { it.second }

    /** Returns current-node annotations with root-relative (null) paths; omits nested annotations. */
    fun annotationsHereAsRoot(): List<Pair<TypePath?, Annotation>> = annotationsHere().map { null to it }

    fun arrayElement(): TypeAnnotationCursor<Annotation> = child(TypePath.ARRAY_ELEMENT)

    fun innerType(): TypeAnnotationCursor<Annotation> = child(TypePath.INNER_TYPE)

    fun typeArgument(index: Int): TypeAnnotationCursor<Annotation> {
        require(index >= 0) { "Type argument index must be non-negative: $index" }
        return child(TypePath.TYPE_ARGUMENT, index)
    }

    fun wildcardBound(): TypeAnnotationCursor<Annotation> = child(TypePath.WILDCARD_BOUND)

    private fun child(step: Int, argument: Int = 0): TypeAnnotationCursor<Annotation> = TypeAnnotationCursor(
        typeAnnotations.filter { [path, _] ->
            path != null && path.length > offset && path.getStep(offset) == step &&
                    (step != TypePath.TYPE_ARGUMENT || path.getStepArgument(offset) == argument)
        },
        offset + 1,
    )

    /** Rebase paths only when handing a complete subtree to the signature parser. */
    fun asRelativeTypeAnnotations(): List<Pair<TypePath?, Annotation>> {
        if (offset == 0) return typeAnnotations
        return typeAnnotations.map { [path, annotation] ->
            val relativePath = buildString {
                if (path != null) {
                    for (index in offset until path.length) {
                        when (val step = path.getStep(index)) {
                            TypePath.ARRAY_ELEMENT -> append('[')
                            TypePath.INNER_TYPE -> append('.')
                            TypePath.WILDCARD_BOUND -> append('*')
                            TypePath.TYPE_ARGUMENT -> append(path.getStepArgument(index)).append(';')
                            else -> error("Unsupported type path step: $step")
                        }
                    }
                }
            }
            TypePath.fromString(relativePath) to annotation
        }
    }
}
