/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew.utils

import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.*
import org.jetbrains.kotlin.js.testNew.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.testNew.handlers.JsBoxRunner.Companion.TEST_FUNCTION
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.INFER_MAIN_MODULE
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.NO_JS_MODULE_SYSTEM
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.RUN_PLAIN_BOX_FUNCTION
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.io.File

private const val MODULE_EMULATION_FILE = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/moduleEmulation.js"

private fun extractJsFiles(
    testServices: TestServices, modulesToArtifact: Map<TestModule, BinaryArtifacts.Js>
): Pair<List<String>, List<String>> {
    val outputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices)

    fun copyInputJsFile(module: TestModule, inputJsFile: TestFile): String {
        val newName = JsEnvironmentConfigurator.getJsArtifactSimpleName(testServices, module.name) + "-js-" + inputJsFile.name
        val targetFile = File(outputDir, newName)
        targetFile.writeText(inputJsFile.originalContent)
        return targetFile.absolutePath
    }

    val inputJsFiles = modulesToArtifact
        .flatMap { moduleToArtifact -> moduleToArtifact.key.files.map { moduleToArtifact.key to it } }
        .filter { it.second.isJsFile }


    val after = inputJsFiles
        .filter { (_, inputJsFile) -> inputJsFile.name.endsWith("__after.js") }
        .map { (module, inputJsFile) -> copyInputJsFile(module, inputJsFile) }
    val before = inputJsFiles
        .filterNot { (_, inputJsFile) -> inputJsFile.name.endsWith("__after.js") }
        .map { (module, inputJsFile) -> copyInputJsFile(module, inputJsFile) }

    return before to after
}

private fun getAdditionalFiles(testServices: TestServices): List<String> {
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

    val withModuleSystem = testWithModuleSystem(testServices)

    val additionalFiles = mutableListOf<String>()
    if (withModuleSystem) additionalFiles += File(MODULE_EMULATION_FILE).absolutePath

    originalFile.parentFile.resolve(originalFile.nameWithoutExtension + JavaScript.DOT_EXTENSION)
        .takeIf { it.exists() }
        ?.let { additionalFiles += it.absolutePath }

    return additionalFiles
}

private fun getAdditionalMainFiles(testServices: TestServices): List<String> {
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
    val additionalFiles = mutableListOf<String>()

    originalFile.parentFile.resolve(originalFile.nameWithoutExtension + "__main.js")
        .takeIf { it.exists() }
        ?.let { additionalFiles += it.absolutePath }

    return additionalFiles
}

fun testWithModuleSystem(testServices: TestServices): Boolean {
    val globalDirectives = testServices.moduleStructure.allDirectives
    val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(getMainModule(testServices))
    val mainModuleKind = configuration[JSConfigurationKeys.MODULE_KIND]
    return mainModuleKind != ModuleKind.PLAIN && NO_JS_MODULE_SYSTEM !in globalDirectives
}

fun getAllFilesForRunner(
    testServices: TestServices, modulesToArtifact: Map<TestModule, BinaryArtifacts.Js>
): Triple<List<String>, List<String>, List<String>> {
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
    val outputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices)
    val dceOutputDir = JsEnvironmentConfigurator.getDceJsArtifactsOutputDir(testServices)
    val pirOutputDir = JsEnvironmentConfigurator.getPirJsArtifactsOutputDir(testServices)

    val commonFiles = JsAdditionalSourceProvider.getAdditionalJsFiles(originalFile.parent).map { it.absolutePath }
    val (inputJsFilesBefore, inputJsFilesAfter) = extractJsFiles(testServices, modulesToArtifact)
    val additionalFiles = getAdditionalFiles(testServices)
    val additionalMainFiles = getAdditionalMainFiles(testServices)

    val artifactsPaths = modulesToArtifact.values.map { it.outputFile.absolutePath }
    val dceJsFiles = artifactsPaths.map { it.replace(outputDir.absolutePath, dceOutputDir.absolutePath) }
    val pirJsFiles = artifactsPaths.map { it.replace(outputDir.absolutePath, pirOutputDir.absolutePath) }

    val allJsFiles = additionalFiles + inputJsFilesBefore + artifactsPaths + commonFiles + additionalMainFiles + inputJsFilesAfter
    val dceAllJsFiles = additionalFiles + inputJsFilesBefore + dceJsFiles + commonFiles + additionalMainFiles + inputJsFilesAfter
    val pirAllJsFiles = additionalFiles + inputJsFilesBefore + pirJsFiles + commonFiles + additionalMainFiles + inputJsFilesAfter

    return Triple(allJsFiles, dceAllJsFiles, pirAllJsFiles)
}

fun getOnlyJsFilesForRunner(testServices: TestServices, modulesToArtifact: Map<TestModule, BinaryArtifacts.Js>): List<String> {
    return getAllFilesForRunner(testServices, modulesToArtifact).first
}

private fun getMainModule(testServices: TestServices): TestModule {
    val modules = testServices.moduleStructure.modules
    val inferMainModule = INFER_MAIN_MODULE in testServices.moduleStructure.allDirectives
    return when {
        inferMainModule -> modules.last()
        else -> modules.singleOrNull { it.name == ModuleStructureExtractor.DEFAULT_MODULE_NAME } ?: modules.single()
    }
}

fun getMainModuleName(testServices: TestServices): String {
    return getMainModule(testServices).name
}

fun getTestModuleName(testServices: TestServices): String? {
    val runPlainBoxFunction = RUN_PLAIN_BOX_FUNCTION in testServices.moduleStructure.allDirectives
    if (runPlainBoxFunction) return null
    return getMainModule(testServices).name
}

fun extractTestPackage(testServices: TestServices): String? {
    val runPlainBoxFunction = RUN_PLAIN_BOX_FUNCTION in testServices.moduleStructure.allDirectives
    if (runPlainBoxFunction) return null
    val ktFiles = testServices.moduleStructure.modules.flatMap { module ->
        module.files
            .filter { it.isKtFile }
            .map {
                val project = testServices.compilerConfigurationProvider.getProject(module)
                testServices.sourceFileProvider.getKtFileForSourceFile(it, project)
            }
    }

    return ktFiles.single { ktFile ->
        val boxFunction = ktFile.declarations.find { it is KtNamedFunction && it.name == TEST_FUNCTION }
        boxFunction != null
    }.packageFqName.asString().takeIf { it.isNotEmpty() }
}

fun getTestChecker(testServices: TestServices): AbstractJsTestChecker {
    val runTestInNashorn = java.lang.Boolean.getBoolean("kotlin.js.useNashorn")
    val targetBackend = testServices.defaultsProvider.defaultTargetBackend ?: TargetBackend.JS
    return if (targetBackend.isIR) {
        if (runTestInNashorn) NashornIrJsTestChecker else V8IrJsTestChecker
    } else {
        if (runTestInNashorn) NashornJsTestChecker else V8JsTestChecker
    }
}
