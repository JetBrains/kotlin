/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx

sealed class ValidationError

sealed class RefinementGraphError : ValidationError()
data class CircularReferenceError(
    val loop: List<FragmentId>
) : RefinementGraphError()

data class OrphansDetectedError(
    val orphans: List<FragmentId>
) : RefinementGraphError()

fun KotlinModule.validate(): List<ValidationError>? {
    return listOfNotNull(
        refinementGraphCheck(),
        listOf()
    ).flatten().takeIf { it.isNotEmpty() } // return null if no errors found
}

private fun KotlinModule.refinementGraphCheck(): List<RefinementGraphError> {
    val variants = fragments.filterValues { it is Variant }.keys
    val circularReferenceErrors = variants.mapNotNull { circularReferenceCheck(it) }
    val orphanErrors = orphanFragments()
        .takeIf { it.isNotEmpty() }
        ?.let { OrphansDetectedError(it.toList()) }

    return circularReferenceErrors + listOfNotNull(orphanErrors)
}

private fun KotlinModule.circularReferenceCheck(variantId: FragmentId): CircularReferenceError? {
    val seen = mutableSetOf<FragmentId>()
    val pool = mutableListOf(variantId)

    while (pool.isNotEmpty()) {
        val item = pool.removeAt(0)
        if (item in seen) return CircularReferenceError(seen.toList())
        seen.add(item)

        pool.addAll(refinements[item] ?: emptySet())
    }

    return null
}

fun KotlinModule.orphanFragments(): Set<FragmentId> {
    val allMentionedFragmentIds = refinements.flatMap { it.value + it.key }
    return fragments.keys - allMentionedFragmentIds
}

private fun KotlinModule.attributeConsistency() {
    val variants = fragments.filterValues { it is Variant }.keys
}
