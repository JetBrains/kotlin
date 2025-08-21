/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibModule
import java.util.Comparator

internal object EmptyConsumingFragmentAttributes : IllegalStateException("Can't form compilation classpath without attributes") {
    private fun readResolve(): Any = EmptyConsumingFragmentAttributes
}

// FIXME: Make attributes type safe instead of Set<String> an invert this function
internal fun UklibModule.resolveCompilationClasspathForConsumer(
    attributes: Set<String>,
) = fragments.findAllConsumableFor(attributes)

internal fun Iterable<UklibFragment>.findAllConsumableFor(
    attributes: Set<String>
): List<UklibFragment> {
    if (attributes.isEmpty()) throw EmptyConsumingFragmentAttributes
    return visibleByConsumingModuleFragmentWith(attributes).orderedForCompilationClasspath()
}

private fun <E> Set<E>.isSubsetOf(another: Set<E>): Boolean = another.containsAll(this)
private fun Iterable<UklibFragment>.visibleByConsumingModuleFragmentWith(
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

