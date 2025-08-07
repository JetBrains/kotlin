/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.LLVMKotlinDisposePassesProfile
import llvm.LLVMKotlinPassesProfileAsString
import llvm.LLVMKotlinPassesProfileRef
import llvm.LLVMKotlinPassesProfileRefVar
import org.jetbrains.kotlin.util.DynamicStats
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PlatformType
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats

internal data class LlvmPassProfile(val pass: String, val duration: Time)

private fun LLVMKotlinPassesProfileRef.parse(pipelineName: String) = LLVMKotlinPassesProfileAsString(this)!!
        .toKString()
        .lineSequence()
        .filter { it.isNotEmpty() }
        .map {
            val line = it.split("\t")
            check(line.size == 4) {
                "Expected line `$it` to have exactly 4 tab-separated columns"
            }
            val pass = "$pipelineName.${line[0]}"
            val nanos = line[1].toLong()
            val userNanos = line[2].toLong()
            val cpuNanos = line[3].toLong()
            LlvmPassProfile(pass, Time(nanos, userNanos, cpuNanos))
        }

@JvmInline
internal value class LlvmPassesProfile private constructor(val entries: List<LlvmPassProfile>) {
    constructor(profile: LLVMKotlinPassesProfileRef, pipelineName: String) : this(profile.parse(pipelineName).toList())
}

/**
 * Run [block] that expects [LLVMKotlinPassesProfileRef] as output variable and return its result augmented by the parsed profile.
 *
 * @param enabled when false, no profile will be computed
 * @param pipelineName will be prepended to each phase name
 */
internal inline fun <T> withLLVMPassesProfile(
        enabled: Boolean,
        pipelineName: String,
        block: (CValuesRef<LLVMKotlinPassesProfileRefVar>?) -> T,
): Pair<T, LlvmPassesProfile?> {
    if (!enabled) {
        return block(null) to null
    }
    return memScoped {
        val profileRef = alloc<LLVMKotlinPassesProfileRefVar>().also {
            it.value = null
        }
        val result = block(profileRef.ptr)
        val profile = profileRef.value
        defer {
            LLVMKotlinDisposePassesProfile(profile)
        }
        result to LlvmPassesProfile(profile!!, pipelineName)
    }
}

internal fun PerformanceManager.addLlvmPassesProfile(profile: LlvmPassesProfile) {
    addOtherUnitStats(UnitStats(
            name = "$presentableName (llvm)",
            outputKind = null,
            platform = PlatformType.Native,
            filesCount = 0,
            linesCount = 0,
            initStats = null,
            analysisStats = null,
            translationToIrStats = null,
            irPreLoweringStats = null,
            irSerializationStats = null,
            klibWritingStats = null,
            irLoweringStats = null,
            backendStats = null,
            dynamicStats = profile.entries.map { (pass, duration) ->
                DynamicStats(PhaseType.Backend, pass, duration)
            }
    ))
}