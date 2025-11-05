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
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend

abstract class AbstractJsInvalidationPerFileTest :
    JsAbstractInvalidationTest(TargetBackend.JS_IR, JsGenerationGranularity.PER_FILE, "incrementalOut/invalidationFir/perFile")

abstract class AbstractJsInvalidationPerModuleTest :
    JsAbstractInvalidationTest(TargetBackend.JS_IR, JsGenerationGranularity.PER_MODULE, "incrementalOut/invalidationFir/perModule")

abstract class AbstractJsES6InvalidationPerFileTest :
    JsAbstractInvalidationTest(TargetBackend.JS_IR_ES6, JsGenerationGranularity.PER_FILE, "incrementalOut/invalidationFirES6/perFile")

abstract class AbstractJsES6InvalidationPerModuleTest :
    JsAbstractInvalidationTest(TargetBackend.JS_IR_ES6, JsGenerationGranularity.PER_MODULE, "incrementalOut/invalidationFirES6/perModule")

abstract class AbstractJsInvalidationPerFileWithPLTest :
    AbstractJsInvalidationWithPLTest(JsGenerationGranularity.PER_FILE, "incrementalOut/invalidationFirWithPL/perFile")

abstract class AbstractJsInvalidationPerModuleWithPLTest :
    AbstractJsInvalidationWithPLTest(JsGenerationGranularity.PER_MODULE, "incrementalOut/invalidationFirWithPL/perModule")

abstract class AbstractJsInvalidationWithPLTest(granularity: JsGenerationGranularity, workingDirPath: String) :
    JsAbstractInvalidationTest(
        TargetBackend.JS_IR,
        granularity,
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
