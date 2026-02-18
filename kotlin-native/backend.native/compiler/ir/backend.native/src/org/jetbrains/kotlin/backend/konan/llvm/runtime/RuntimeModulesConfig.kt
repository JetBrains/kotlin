/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.runtime

import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.konan.config.runtimeFile
import org.jetbrains.kotlin.konan.file.File

class RuntimeModulesConfig(private val config: NativeSecondStageCompilationConfig) {
    /**
     * Returns `true` when the binary will contain [RuntimeModule.DEBUG].
     */
    internal val containsDebuggingRuntime: Boolean
        get() = config.debug

    private val RuntimeModule.absolutePath: String
        get() = File(config.distribution.defaultNatives(config.target)).child(filename).absolutePath

    private val compilerInterfaceAbsolutePath by lazy {
        config.configuration.runtimeFile
                ?: RuntimeModule.COMPILER_INTERFACE.absolutePath
    }

    internal fun absolutePathFor(module: RuntimeModule): String = when (module) {
        RuntimeModule.COMPILER_INTERFACE -> compilerInterfaceAbsolutePath
        else -> module.absolutePath
    }
}