/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support

/**
 * Represents a package name.
 */
internal class PackageName private constructor(private val fqn: String, val segments: List<String>): Comparable<PackageName> {
    constructor(segments: List<String>) : this(segments.joinToString("."), segments)
    constructor(fqn: String) : this(fqn, if (fqn.isNotEmpty()) fqn.split('.') else emptyList())

    fun isEmpty(): Boolean = segments.isEmpty()

    override fun toString() = fqn
    override fun equals(other: Any?) = fqn == (other as? PackageName)?.fqn
    override fun hashCode() = fqn.hashCode()

    override fun compareTo(other: PackageName) = fqn.compareTo(other.fqn)

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
internal class TestName: Comparable<TestName> {
    private val fqn: String

    val packageName: PackageName
    val packagePartClassName: String?
    val functionName: String

    constructor(purePackageSegments: List<String>, classNames: List<String>, functionName: String) {
        this.functionName = functionName

        val segments = purePackageSegments.toMutableList()
        if (classNames.lastOrNull().isPackagePartClassName()) {
            packagePartClassName = classNames.last()
            segments += classNames.dropLast(1)
        } else {
            packagePartClassName = null
            segments += classNames
        }

        packageName = PackageName(segments)
        fqn = buildString {
            if (!packageName.isEmpty()) append(packageName).append('.')
            if (packagePartClassName != null) append(packagePartClassName).append('.')
            append(functionName)
        }
    }

    constructor(fqn: String) {
        this.fqn = fqn

        val segments = fqn.split('.').toMutableList()
        functionName = segments.removeLast()
        packagePartClassName = if (segments.lastOrNull().isPackagePartClassName()) segments.removeLast() else null
        packageName = PackageName(segments)
    }

    override fun toString() = fqn
    override fun equals(other: Any?) = fqn == (other as? TestName)?.fqn
    override fun hashCode() = fqn.hashCode()

    override fun compareTo(other: TestName) = fqn.compareTo(other.fqn)

    companion object {
        private fun Char.isEffectivelyUpperCase() = if (isUpperCase()) true else !isLowerCase()
        private fun String?.isPackagePartClassName() = this != null && firstOrNull()?.isEffectivelyUpperCase() == true && endsWith("Kt")
    }
}
