/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

/**
 * Dynamic driver does not "know" upfront which phases will be executed.
 */
internal class DynamicCompilerDriver : CompilerDriver() {

    companion object {
        // Will become non-trivial in the future.
        fun supportsConfig(): Boolean =
                false
    }

    override fun run(config: KonanConfig, environment: KotlinCoreEnvironment) = when (config.produce) {
        CompilerOutputKind.PROGRAM -> error("Dynamic compiler driver does not support `program` output yet.")
        CompilerOutputKind.DYNAMIC -> error("Dynamic compiler driver does not support `dynamic` output yet.")
        CompilerOutputKind.STATIC -> error("Dynamic compiler driver does not support `static` output yet.")
        CompilerOutputKind.FRAMEWORK -> error("Dynamic compiler driver does not support `framework` output yet.")
        CompilerOutputKind.LIBRARY -> error("Dynamic compiler driver does not support `library` output yet.")
        CompilerOutputKind.BITCODE -> error("Dynamic compiler driver does not support `bitcode` output yet.")
        CompilerOutputKind.DYNAMIC_CACHE -> error("Dynamic compiler driver does not support `dynamic_cache` output yet.")
        CompilerOutputKind.STATIC_CACHE -> error("Dynamic compiler driver does not support `static_cache` output yet.")
        CompilerOutputKind.PRELIMINARY_CACHE -> TODO()
    }
}