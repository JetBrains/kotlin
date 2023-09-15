/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

class PluginConfiguration(
    val logLevel: LogLevel,
    val behaviour: UnsupportedFeatureBehaviour,
    val conversionSelection: TargetsSelection,
    val verificationSelection: TargetsSelection,
) {
    private val internalErrorInfos = mutableListOf<String>()
    private val minorErrors = mutableListOf<String>()

    init {
        if (conversionSelection < verificationSelection) {
            throw IllegalArgumentException("Conversion options may not be stricter than verification options; converting $conversionSelection but verifying $verificationSelection.")
        }
    }

    fun clearMinorErrors() {
        minorErrors.clear()
    }

    fun addMinorError(error: String) {
        minorErrors.add(error)
    }

    fun forEachMinorError(action: (String) -> Unit) {
        minorErrors.forEach(action)
    }

    fun formatErrorWithInfos(error: String): String =
        internalErrorInfos.joinToString(prefix = "$error\n", separator = "\n")

    fun addErrorInfo(msg: String) {
        internalErrorInfos.add(msg)
    }
}