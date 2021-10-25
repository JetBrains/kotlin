/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew.handlers

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.backend.js.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.jsResolveLibraries
import org.jetbrains.kotlin.ir.backend.js.toResolverLogger
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.esModulesSubDir
import org.jetbrains.kotlin.js.testNew.converters.ClassicJsBackendFacade.Companion.wrapWithModuleEmulationMarkers
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.util.DummyLogger
import java.io.File

class JsIrArtifactsDumpHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        when (val artifact = info.unwrap()) {
            is BinaryArtifacts.Js.JsIrArtifact -> {
                val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                val moduleId = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)
                val moduleKind = configuration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
                val outputFile = File(JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name) + ".js")
                val outputDceFile = File(JsEnvironmentConfigurator.getDceJsArtifactPath(testServices, module.name) + ".js")
                val outputPirFile = File(JsEnvironmentConfigurator.getPirJsArtifactPath(testServices, module.name) + ".js")

                val runIrPir = JsEnvironmentConfigurationDirectives.RUN_IR_PIR in module.directives
                val dontSkipDceDriven = JsEnvironmentConfigurationDirectives.SKIP_DCE_DRIVEN !in module.directives
                val dontSkipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE !in module.directives

                if (dontSkipRegularMode) {
                    artifact.compilerResult.outputs!!.writeTo(outputFile, moduleId, moduleKind)
                    artifact.compilerResult.outputsAfterDce?.writeTo(outputDceFile, moduleId, moduleKind)
                } else if (runIrPir && dontSkipDceDriven) {
                    artifact.compilerResult.outputs!!.writeTo(outputPirFile, moduleId, moduleKind)
                } else {
                    TODO("unreachable")
                }

            }
            is BinaryArtifacts.Js.JsKlibArtifact -> {
                testServices.jsLibraryProvider.setDescriptorAndLibraryByName(info.outputFile.absolutePath, artifact.descriptor as ModuleDescriptorImpl, artifact.library)
            }
            is BinaryArtifacts.Js.JsEsArtifact -> {
                val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                val moduleName = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)
                val esmTestFile = info.outputFile.parentFile.esModulesSubDir.resolve("test.mjs")
                createEsTestFile(esmTestFile, moduleName)

                val dceEsmTestFile = artifact.outputDceFile?.parentFile?.esModulesSubDir?.resolve("test.mjs") ?: return
                createEsTestFile(dceEsmTestFile, moduleName)
            }
            else -> return
        }

        // TODO write dts
    }

    private fun createEsTestFile(file: File, moduleName: String) {
        val customTestModule = testServices.moduleStructure.modules
            .flatMap { it.files }
            .singleOrNull { JsEnvironmentConfigurationDirectives.ENTRY_ES_MODULE in it.directives }
        val customTestModuleText = customTestModule?.let { testServices.sourceFileProvider.getContentOfSourceFile(it) }

        val defaultTestModule =
            """                     
                                    import { box } from './${moduleName}/index.js';
                                    let res = box();
                                    if (res !== "OK") {
                                        throw "Wrong result: " + String(res);
                                    }
                                    """.trimIndent()
        file.writeText(customTestModuleText ?: defaultTestModule)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    private fun CompilationOutputs.writeTo(outputFile: File, moduleId: String, moduleKind: ModuleKind) {
        val wrappedCode = wrapWithModuleEmulationMarkers(jsCode, moduleId = moduleId, moduleKind = moduleKind)
        outputFile.write(wrappedCode)

        dependencies.forEach { (moduleId, outputs) ->
            val moduleWrappedCode = wrapWithModuleEmulationMarkers(outputs.jsCode, moduleKind, moduleId)
            val dependencyPath = outputFile.absolutePath.replace("_v5.js", "-${moduleId}_v5.js")
            File(dependencyPath).write(moduleWrappedCode)
        }
    }

    private fun File.write(text: String) {
        parentFile.mkdirs()
        writeText(text)
    }
}