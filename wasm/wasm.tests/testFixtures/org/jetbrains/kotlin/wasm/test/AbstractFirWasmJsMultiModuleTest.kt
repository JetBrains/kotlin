/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.configuration.enableByConfigurationKey
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys.WASM_GENERATE_CLOSED_WORLD_MULTIMODULE

private fun TestConfigurationBuilder.configureMultimodule() {
    enableByConfigurationKey(WASM_GENERATE_CLOSED_WORLD_MULTIMODULE)
}

@WasmFirCompilerExtraTest
open class AbstractFirWasmTypeScriptExportMultiModuleTest : AbstractFirWasmTypeScriptExportTest(
    testGroupOutputDirPrefix = "typescript-export-multi-module/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureMultimodule()
    }
}

@WasmFirCompilerExtraTest
open class AbstractFirWasmJsMultiModuleSteppingTest(
    testGroupOutputDirPrefix: String = "debug/stepping/firBoxMultiModule",
) : AbstractFirWasmJsSteppingTest(
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureMultimodule()
    }
}
