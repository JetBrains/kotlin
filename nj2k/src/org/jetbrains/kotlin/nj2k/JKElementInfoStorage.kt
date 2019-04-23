/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import org.jetbrains.kotlin.descriptors.FunctionDescriptor


interface JKElementInfo

sealed class SuperFunctionInfo
data class ExternalSuperFunctionInfo(val descriptor: FunctionDescriptor) : SuperFunctionInfo()
data class InternalSuperFunctionInfo(val label: JKElementInfoLabel) : SuperFunctionInfo()

data class FunctionInfo(val originalDescriptor: FunctionDescriptor, val superFunctions: List<SuperFunctionInfo>) : JKElementInfo

object UnknownNullability : JKElementInfo

inline class JKElementInfoLabel(val label: String) {
    inline fun render(): String = "/*@@$label@@*/"

    companion object {
        val LABEL_REGEX = """/\*@@(\w+)@@\*/""".toRegex()
    }
}

fun String.asLabel(): JKElementInfoLabel? =
    JKElementInfoLabel.LABEL_REGEX.matchEntire(this)?.groupValues?.getOrNull(1)?.let { JKElementInfoLabel(it) }

class JKElementInfoStorage {
    private val labelToInfo = mutableMapOf<JKElementInfoLabel, List<JKElementInfo>>()
    private val elementToLabel = mutableMapOf<Any, JKElementInfoLabel>()

    fun getOrCreateInfoForElement(element: Any): JKElementInfoLabel =
        elementToLabel.getOrPut(element) { JKElementInfoLabel(createRandomString()) }

    fun getOrCreateInfoForLabel(label: JKElementInfoLabel): List<JKElementInfo> =
        labelToInfo.getOrPut(label) { emptyList() }

    fun getInfoForLabel(label: JKElementInfoLabel): List<JKElementInfo>? =
        labelToInfo[label]


    fun addEntry(element: Any, info: JKElementInfo) {
        val label = elementToLabel.getOrPut(element) { JKElementInfoLabel(createRandomString()) }
        labelToInfo[label] = labelToInfo[label].orEmpty() + info
        elementToLabel[element] = label
    }

    companion object {
        private val charPool = ('a'..'z').toList()
        private const val generatedStringLength = 6

        private fun createRandomString(): String {
            return (1..generatedStringLength)
                .map { kotlin.random.Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("")
        }
    }
}