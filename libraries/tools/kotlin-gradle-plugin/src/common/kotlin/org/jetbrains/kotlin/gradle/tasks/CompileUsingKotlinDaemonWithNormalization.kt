/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal

internal interface CompileUsingKotlinDaemonWithNormalization : CompileUsingKotlinDaemon {

    @get:Internal
    val normalizedKotlinDaemonJvmArguments: Provider<List<String>>
        get() = kotlinDaemonJvmArguments.map {
            it.map { arg -> arg.trim().removePrefix("-") }
        }
}