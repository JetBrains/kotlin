/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.WASM_FAILS_IN_MULTI_MODULE_MODE
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.WASM_FAILS_IN_MULTI_MODULE_MODE_WINDOWS
import org.jetbrains.kotlin.test.services.configuration.enableByConfigurationKey
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys.WASM_GENERATE_CLOSED_WORLD_MULTIMODULE
import org.jetbrains.kotlin.wasm.test.utils.configureIgnoredTestSuppressor

private fun TestConfigurationBuilder.configureMultimodule() {
    configureIgnoredTestSuppressor(WASM_FAILS_IN_MULTI_MODULE_MODE)
    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    if (isWindows) {
        configureIgnoredTestSuppressor(WASM_FAILS_IN_MULTI_MODULE_MODE_WINDOWS)
    }
    enableByConfigurationKey(WASM_GENERATE_CLOSED_WORLD_MULTIMODULE)
}

open class AbstractFirWasmJsCodegenMultiModuleBoxTest(
    testGroupOutputDirPrefix: String = "codegen/multiModuleBox/",
) : AbstractFirWasmJsCodegenBoxTest(
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureMultimodule()
    }
}

open class AbstractFirWasmJsCodegenMultiModuleInteropTest : AbstractFirWasmJsCodegenBoxTest(
    testGroupOutputDirPrefix = "codegen/wasmJsMultiModuleInterop"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureMultimodule()
    }
}

open class AbstractFirWasmTypeScriptExportMultiModuleTest : AbstractFirWasmTypeScriptExportTest(
    testGroupOutputDirPrefix = "typescript-export-multi-module/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureMultimodule()
    }
}

open class AbstractFirWasmJsMultiModuleSteppingTest(
    testGroupOutputDirPrefix: String = "debug/stepping/firBoxMultiModule",
) : AbstractFirWasmJsSteppingTest(
    testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureMultimodule()
    }
}