/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Exec
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Exec.Companion.register

@OptIn(ExperimentalWasmDsl::class)
@Deprecated(level = DeprecationLevel.HIDDEN, message = "For compatibility only")
abstract class D8Exec() {
    companion object {
        fun create(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: D8Exec.() -> Unit = {},
        ): TaskProvider<D8Exec> = register(compilation, name, configuration)
    }
}