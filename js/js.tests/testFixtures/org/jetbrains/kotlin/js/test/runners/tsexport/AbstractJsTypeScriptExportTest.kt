/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners.tsexport

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.runners.AbstractJsES6Test
import org.jetbrains.kotlin.js.test.runners.AbstractJsTest
import org.jetbrains.kotlin.js.test.utils.configureJsTypeScriptExportTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

abstract class AbstractJsTypeScriptExportTest(
    testGroupOutputDirPrefix: String = "typescript-export/es5",
    private val isWholeFileJsExport: Boolean = false,
) : AbstractJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.Companion.TEST_DATA_DIR_PATH}/typescript-export/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJsTypeScriptExportTest(isWholeFileJsExport)
    }
}

abstract class AbstractJsTypeScriptWholeFileExportTest : AbstractJsTypeScriptExportTest(
    testGroupOutputDirPrefix = "typescript-export/es5-whole-file",
    isWholeFileJsExport = true,
)

open class AbstractJsTypeScriptExportWithInlinedFunInKlibTest : AbstractJsTypeScriptExportTest(
    testGroupOutputDirPrefix = "typescript-export/es5-withInlinedFunInKlib"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
        }
    }
}

open class AbstractJsES6TypeScriptExportTest(
    testGroupOutputDirPrefix: String = "typescript-export/es6",
    private val isWholeFileJsExport: Boolean = false,
) : AbstractJsES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.Companion.TEST_DATA_DIR_PATH}/typescript-export/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJsTypeScriptExportTest(isWholeFileJsExport)
    }
}

abstract class AbstractJsES6TypeScriptWholeFileExportTest : AbstractJsES6TypeScriptExportTest(
    testGroupOutputDirPrefix = "typescript-export/es6-whole-file",
    isWholeFileJsExport = true,
)
