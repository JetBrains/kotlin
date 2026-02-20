/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.codegen.ProjectInfo
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.wasm.config.wasmGenerateClosedWorldMultimodule
import org.jetbrains.kotlin.wasm.config.wasmIncludedModuleOnly

abstract class AbstractFirWasmInvalidationTest :
    WasmAbstractInvalidationTest(TargetBackend.WASM, "incrementalOut/invalidationFir") {
}

abstract class AbstractFirWasmInvalidationMultiModuleTestBase(workingDirPath: String) :
    WasmAbstractInvalidationTest(TargetBackend.WASM, workingDirPath)  {

    private val ignoredTests = setOf(
        "classFunctionsAndFields", //Invalid signature //KT-84599
        "kotlinTest", //Eager initializer KT-83579
        "multiModuleEagerInitialization", //Eager initializer KT-83579
    )

    override fun isIgnoredTest(projectInfo: ProjectInfo): Boolean =
        super.isIgnoredTest(projectInfo) || projectInfo.name in ignoredTests
}

abstract class AbstractFirWasmInvalidationMultiModuleTest :
    AbstractFirWasmInvalidationMultiModuleTestBase("incrementalOut/invalidationFirMultimodule") {
    override fun modifyConfig(configuration: CompilerConfiguration) {
        configuration.wasmGenerateClosedWorldMultimodule = true
    }
}

abstract class AbstractFirWasmInvalidationSingleModuleTest :
    AbstractFirWasmInvalidationMultiModuleTestBase("incrementalOut/invalidationFirSinglemodule") {
    override fun modifyConfig(configuration: CompilerConfiguration) {
        configuration.wasmIncludedModuleOnly = true
    }
}

abstract class AbstractFirWasmInvalidationWithPLTest :
    AbstractWasmInvalidationWithPLTest("incrementalOut/invalidationFirWithPL")

abstract class AbstractFirWasmInvalidationWithPLMultiModuleTest :
    AbstractWasmInvalidationWithPLTest("incrementalOut/invalidationFirWithPLMultimodule") {
    override fun modifyConfig(configuration: CompilerConfiguration) {
        super.modifyConfig(configuration)
        configuration.wasmGenerateClosedWorldMultimodule = true
    }
}

abstract class AbstractFirWasmInvalidationWithPLSingleModuleTest :
    AbstractWasmInvalidationWithPLTest("incrementalOut/invalidationFirWithPLSinglemodule") {
    override fun modifyConfig(configuration: CompilerConfiguration) {
        super.modifyConfig(configuration)
        configuration.wasmIncludedModuleOnly = true
    }
}


abstract class AbstractWasmInvalidationWithPLTest(workingDirPath: String) :
    WasmAbstractInvalidationTest(TargetBackend.WASM, workingDirPath) {
    override fun modifyConfig(configuration: CompilerConfiguration) {
        configuration.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.WARNING))
    }
}