/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters.incremental

import org.jetbrains.kotlin.js.test.converters.ClassicJsBackendFacade
import org.jetbrains.kotlin.js.test.utils.jsClassicIncrementalDataProvider
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.RECOMPILE
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

@Suppress("warnings")
class RecompileModuleJsBackendFacade<R : ResultingArtifact.FrontendOutput<R>>(
    testServices: TestServices,
    private val frontendFacade: Constructor<FrontendFacade<R>>,
    private val frontend2BackendConverter: Constructor<Frontend2BackendConverter<R, ClassicBackendInput>>
) : CommonRecompileModuleJsBackendFacade<R, ClassicBackendInput>(testServices, TargetBackend.JS) {
    override fun TestConfigurationBuilder.configure(module: TestModule) {
        facadeStep(frontendFacade)
        facadeStep(frontend2BackendConverter)
        facadeStep { ClassicJsBackendFacade(it, incrementalCompilationEnabled = true) }
    }

    override fun TestServices.register(module: TestModule) {
        val filesToRecompile = module.files.filter { RECOMPILE in it.directives }
        val incrementalData = testServices.jsClassicIncrementalDataProvider.getIncrementalData(module).copy()
        for (testFile in filesToRecompile) {
            incrementalData.translatedFiles.remove(File("/${testFile.relativePath}"))
        }

        jsClassicIncrementalDataProvider.recordIncrementalData(module, incrementalData)
    }
}
