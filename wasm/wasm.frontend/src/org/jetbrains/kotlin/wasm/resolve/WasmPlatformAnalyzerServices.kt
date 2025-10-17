/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.*

// TODO: Here for IDE ABI, remove after rename in IDE
typealias WasmJsPlatformAnalyzerServices = WasmPlatformAnalyzerServices

object WasmPlatformAnalyzerServices : PlatformDependentAnalyzerServices() {
    override val platformConfigurator: PlatformConfigurator = WasmJsPlatformConfigurator
    override val defaultImportsProvider: DefaultImportsProvider = WasmJsDefaultImportsProvider

    val builtIns: KotlinBuiltIns
        get() = DefaultBuiltIns.Instance
}

object WasmWasiPlatformAnalyzerServices : PlatformDependentAnalyzerServices() {
    override val platformConfigurator: PlatformConfigurator = WasmWasiPlatformConfigurator
    override val defaultImportsProvider: DefaultImportsProvider = WasmWasiDefaultImportsProvider
    val builtIns: KotlinBuiltIns
        get() = DefaultBuiltIns.Instance
}
