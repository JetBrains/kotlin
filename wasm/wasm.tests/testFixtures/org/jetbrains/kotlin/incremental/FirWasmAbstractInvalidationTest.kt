/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend

abstract class AbstractFirWasmInvalidationTest :
    WasmAbstractInvalidationTest(TargetBackend.WASM, "incrementalOut/invalidationFir")

abstract class AbstractFirWasmInvalidationWithPLTest :
    AbstractWasmInvalidationWithPLTest("incrementalOut/invalidationFirWithPL")

abstract class AbstractWasmInvalidationWithPLTest(workingDirPath: String) :
    WasmAbstractInvalidationTest(
        TargetBackend.WASM,
        workingDirPath
    ) {
    override fun createConfiguration(
        moduleName: String,
        moduleKind: ModuleKind,
        languageFeatures: List<String>,
        allLibraries: List<String>,
        friendLibraries: List<String>,
        includedLibrary: String?,
    ): CompilerConfiguration {
        val config = super.createConfiguration(
            moduleName = moduleName,
            moduleKind = moduleKind,
            languageFeatures = languageFeatures,
            allLibraries = allLibraries,
            friendLibraries = friendLibraries,
            includedLibrary = includedLibrary,
        )
        config.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.WARNING))
        return config
    }
}
