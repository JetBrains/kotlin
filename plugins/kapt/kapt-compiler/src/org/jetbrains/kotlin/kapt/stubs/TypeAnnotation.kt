/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import org.jetbrains.org.objectweb.asm.TypePath
import org.jetbrains.org.objectweb.asm.TypeReference

/** ASM uses -1 for the superclass and zero-based interface indices for other supertype annotation targets. */
internal const val SUPERCLASS_TYPE_ANNOTATION_INDEX = -1

/** A converted annotation with its JVM target and path within that target's type. */
internal data class TypeAnnotation<A>(
    val reference: TypeReference,
    val path: TypePath?,
    val annotation: A,
)

/** Selects a JVM target, leaving paths relative to that target for signature parsing or error correction. */
internal fun <A> List<TypeAnnotation<A>>.forTarget(reference: TypeReference): List<Pair<TypePath?, A>> =
    mapNotNull { if (it.reference.value == reference.value) it.path to it.annotation else null }
