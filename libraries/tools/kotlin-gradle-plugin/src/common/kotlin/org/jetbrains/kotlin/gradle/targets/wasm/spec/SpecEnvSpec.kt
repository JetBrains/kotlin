///*
// * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
// * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
// */
//
package org.jetbrains.kotlin.gradle.targets.wasm.spec

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

@ExperimentalWasmDsl
abstract class SpecEnvSpec internal constructor() {
    companion object {
        val EXTENSION_NAME: String
            get() = "WasmSpecEnv"
    }

    abstract val executable: Property<String>

    fun produceEnv(): Provider<SpecEnv> {
        return this.executable.map { SpecEnv(it) }
    }
}