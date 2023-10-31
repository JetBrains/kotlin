/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

class PluginConfiguration(
    val logLevel: LogLevel,
    val errorStyle: ErrorStyle,
    val behaviour: UnsupportedFeatureBehaviour,
    val conversionSelection: TargetsSelection,
    val verificationSelection: TargetsSelection,
) {
    init {
        if (conversionSelection < verificationSelection) {
            throw IllegalArgumentException("Conversion options may not be stricter than verification options; converting $conversionSelection but verifying $verificationSelection.")
        }
    }
}