/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs

import java.io.File
import java.util.Comparator

internal data class UklibModule(
    val fragments: Set<UklibFragment>,
)

internal data class UklibFragment(
    val identifier: String,
    val attributes: Set<String>,
    val file: () -> File,
)

internal fun <E> Set<E>.isSubsetOf(another: Set<E>): Boolean = another.containsAll(this)
private fun Iterable<UklibFragment>.visibleByConsumingModulesFragmentWith(
    consumingFragmentAttributes: Set<String>
): List<UklibFragment> = filter { consumingFragmentAttributes.isSubsetOf(it.attributes) }

private fun List<UklibFragment>.orderedForCompilationClasspath(): List<UklibFragment> = sortedWith(
    object : Comparator<UklibFragment> {
        override fun compare(left: UklibFragment, right: UklibFragment): Int {
            if (left.attributes == right.attributes) {
                return 0
            } else if (left.attributes.isSubsetOf(right.attributes)) {
                return -1
            } else {
                return 1
            }
        }
    }
)
internal fun Iterable<UklibFragment>.formCompilationClasspathInConsumingModuleFragment(
    consumingFragmentAttributes: Set<String>
) = visibleByConsumingModulesFragmentWith(consumingFragmentAttributes).orderedForCompilationClasspath()

