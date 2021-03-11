/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.utils.SmartList
import kotlin.random.Random

interface JKElementInfo

sealed class SuperFunctionInfo
data class ExternalSuperFunctionInfo(val descriptor: FunctionDescriptor) : SuperFunctionInfo()
data class InternalSuperFunctionInfo(val label: JKElementInfoLabel) : SuperFunctionInfo()

data class FunctionInfo(val superFunctions: List<SuperFunctionInfo>) : JKElementInfo

enum class JKTypeInfo(val unknownNullability: Boolean, val unknownMutability: Boolean) : JKElementInfo {
    KNOWN_NULLABILITY_KNOWN_MUTABILITY(false, false),
    UNKNOWN_NULLABILITY_KNOWN_MUTABILITY(true, false),
    KNOWN_NULLABILITY_UNKNOWN_MUTABILITY(false, true),
    UNKNOWN_NULLABILITY_UNKNOWN_MUTABILITY(true, true)
    ;

    companion object {
        operator fun invoke(unknownNullability: Boolean, unknownMutability: Boolean) = when {
            !unknownNullability && !unknownMutability -> KNOWN_NULLABILITY_KNOWN_MUTABILITY
            unknownNullability && !unknownMutability -> UNKNOWN_NULLABILITY_KNOWN_MUTABILITY
            !unknownNullability && unknownMutability -> KNOWN_NULLABILITY_UNKNOWN_MUTABILITY
            else -> UNKNOWN_NULLABILITY_UNKNOWN_MUTABILITY
        }
    }
}


inline class JKElementInfoLabel(val label: String) {
    fun render(): String = "/*@@$label@@*/"

    companion object {
        val LABEL_REGEX = """/\*@@(\w+)@@\*/""".toRegex()
    }
}

fun String.asLabel(): JKElementInfoLabel? =
    JKElementInfoLabel.LABEL_REGEX.matchEntire(this)?.groupValues?.getOrNull(1)?.let { JKElementInfoLabel(it) }

class JKElementInfoStorage {
    private val labelToInfo = mutableMapOf<JKElementInfoLabel, MutableList<JKElementInfo>>()
    private val elementToLabel = mutableMapOf<Any, JKElementInfoLabel>()

    fun getOrCreateInfoForElement(element: Any): JKElementInfoLabel =
        elementToLabel.getOrPut(element) { JKElementInfoLabel(createRandomString()) }

    fun getInfoForLabel(label: JKElementInfoLabel): List<JKElementInfo>? =
        labelToInfo[label]

    fun addEntry(element: Any, info: JKElementInfo) {
        val label = elementToLabel.getOrPut(element) { JKElementInfoLabel(createRandomString()) }
        labelToInfo.getOrPut(label) { SmartList() } += info
        elementToLabel[element] = label
    }

    companion object {
        private val charPool = ('a'..'z').toList()
        private const val generatedStringLength = 6

        private fun createRandomString() = (1..generatedStringLength).joinToString("") {
            charPool[Random.nextInt(0, charPool.size)].toString()
        }
    }
}