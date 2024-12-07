package org.jetbrains.kotlin.gradle.artifacts.uklibsModel

import java.io.File

//    /**
//     * 1. No parts of the graph are isolated
//     * 2. There are no cycles (this is already done by KGP)
//     * 3. Everything is rooted in a single node (already checked in KGP)
//     * 4. There are no fragments with duplicated attributes
//     */

data class Module(
    val fragments: Set<Fragment>,
)

data class Fragment(
    val identifier: String,
    val attributes: Set<String>,
    val file: () -> File,
)

fun <E> Set<E>.isProperSubsetOf(another: Set<E>): Boolean = another.size > size && isSubsetOf(another)
fun <E> Set<E>.isSubsetOf(another: Set<E>): Boolean = another.containsAll(this)