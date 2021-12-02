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
