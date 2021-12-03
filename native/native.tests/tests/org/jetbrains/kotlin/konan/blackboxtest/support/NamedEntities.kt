/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support

/**
 * Represents a package name.
 */
internal class PackageName private constructor(private val fqn: String, val segments: List<String>) {
    constructor(segments: List<String>) : this(segments.joinToString("."), segments)
    constructor(fqn: String) : this(fqn, if (fqn.isNotEmpty()) fqn.split('.') else emptyList())

    fun isEmpty(): Boolean = segments.isEmpty()

    override fun toString() = fqn
    override fun equals(other: Any?) = fqn == (other as? PackageName)?.fqn
    override fun hashCode() = fqn.hashCode()

    companion object {
        val EMPTY = PackageName("", emptyList())
    }
}

/**
 * Represents a single test name (i.e. a function annotated with [kotlin.test.Test]) inside of a [TestFile].
 *
 * [packageName] - containing package name. For the sake of simplification [packageName] may also include top-level and nested class names,
 *   but it does not include package-part class name (i.e. "SomethingKt").
 * [packagePartClassName] - package-part class name (if there is any)
 * [functionName] - name of test function
 */
internal data class TestName(private val fqn: String) {
    val packageName: PackageName
    val packagePartClassName: String?
    val functionName: String

    init {
        val segments = fqn.split('.').toMutableList()
        functionName = segments.removeLast()

        val maybePackagePartClassSegment = segments.lastOrNull()
        packagePartClassName = if (maybePackagePartClassSegment?.firstOrNull()?.isEffectivelyUpperCase() == true
            && maybePackagePartClassSegment.endsWith("Kt")
        ) {
            segments.removeLast()
        } else
            null

        packageName = PackageName(segments)
    }

    override fun toString() = fqn

    companion object {
        private fun Char.isEffectivelyUpperCase() = if (isUpperCase()) true else !isLowerCase()
    }
}
