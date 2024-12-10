package org.jetbrains.kotlin.gradle.artifacts.uklibsModel

import java.io.File
import java.util.Comparator

//    /**
//     * 1. No parts of the graph are isolated (this is kind of also checked by everything being rooted)
//     * 2. There are no cycles (this is already done by KGP)
//     * 3. Everything is rooted in a single node (already checked in KGP in MultipleSourceSetRootsInCompilationChecker)
//     * 4. There are no fragments with duplicated attributes (so the check for bamboos)
//     */

data class Module(
    val fragments: Set<Fragment>,
)

data class Fragment(
    val identifier: String,
    val attributes: Set<String>,
    val file: () -> File,
)

fun <E> Set<E>.isSubsetOf(another: Set<E>): Boolean = another.containsAll(this)
private fun Iterable<Fragment>.visibleByConsumingModulesFragmentWith(
    consumingFragmentAttributes: Set<String>
): List<Fragment> = filter { consumingFragmentAttributes.isSubsetOf(it.attributes) }

private fun List<Fragment>.orderedForCompilationClasspath(): List<Fragment> = sortedWith(
    object : Comparator<Fragment> {
        override fun compare(left: Fragment, right: Fragment): Int {
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
internal fun Iterable<Fragment>.formCompilationClasspathInConsumingModuleFragment(
    consumingFragmentAttributes: Set<String>
) = visibleByConsumingModulesFragmentWith(consumingFragmentAttributes).orderedForCompilationClasspath()

