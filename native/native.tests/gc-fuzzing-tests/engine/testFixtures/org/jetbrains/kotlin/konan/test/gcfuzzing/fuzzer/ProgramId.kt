/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.fuzzer

import kotlin.reflect.KClass

private val <T : ProgramId> KClass<T>.tag: Char
    get() = this.simpleName!![0]

sealed class ProgramId {
    private val repr by lazy { toString() }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProgramId) return false
        return repr == other.repr
    }

    override fun hashCode(): Int = repr.hashCode()

    class Initial(val seed: UInt) : ProgramId() {
        val x = Initial::class
        override fun toString(): String = "${Initial::class.tag}$seed"
    }

    companion object {
        fun fromString(str: String): ProgramId? {
            fun tokenize(str: String): List<String> {
                val knownTags = listOf(
                    Initial::class,
                ).map { it.tag }.joinToString("")
                val regex = Regex("([$knownTags]|\\d+|[^$knownTags\\d]+)")
                return regex.findAll(str).map { it.value }.toList()
            }

            val tokens = tokenize(str).iterator()
            fun nextToken() = if (tokens.hasNext()) tokens.next() else null
            fun nextTag() = nextToken()?.single()
            fun nextUInt() = nextToken()?.toUIntOrNull()
            fun nextProgramId(): ProgramId? = when (nextTag()) {
                Initial::class.tag -> {
                    val seed = nextUInt() ?: return null
                    Initial(seed)
                }
                else -> null
            }
            return nextProgramId()
        }
    }
}
