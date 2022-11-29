/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

internal fun AbstractKotlinCompile<*>.nagUserFreeArgsModifiedOnExecution(
    freeArgsValue: List<String>
) {
    if (!suppressKotlinOptionsFreeArgsModificationWarning.get()) {
        logger.warn(
            "kotlinOptions.freeCompilerArgs were changed on task $path execution phase: ${freeArgsValue.joinToString()}\n" +
                    "This behaviour is deprecated and become an error in future releases!"
        )
    }
}
