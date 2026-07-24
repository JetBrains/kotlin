/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.runtime

import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.konan.config.runtimeFile
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class RuntimeModulesConfig(private val config: NativeSecondStageCompilationConfig) {
    /**
     * Returns `true` when the binary will contain [RuntimeModule.DEBUG].
     */
    internal val containsDebuggingRuntime: Boolean
        get() = config.debug

    private val RuntimeModule.absolutePath: String
        get() = Path(config.distribution.defaultNatives(config.target)).resolve(filename).absolutePathString()

    private val compilerInterfaceAbsolutePath by lazy {
        config.configuration.runtimeFile
                ?: RuntimeModule.COMPILER_INTERFACE.absolutePath
    }

    internal fun absolutePathFor(module: RuntimeModule): String = when (module) {
        RuntimeModule.COMPILER_INTERFACE -> compilerInterfaceAbsolutePath
        else -> module.absolutePath
    }
}