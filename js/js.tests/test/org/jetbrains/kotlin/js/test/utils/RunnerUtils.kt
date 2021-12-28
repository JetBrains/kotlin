/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.utils

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.converters.augmentWithModuleName
import org.jetbrains.kotlin.js.test.handlers.JsBoxRunner.Companion.TEST_FUNCTION
import org.jetbrains.kotlin.js.testOld.*
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.NO_JS_MODULE_SYSTEM
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.RUN_PLAIN_BOX_FUNCTION
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator.Companion.getMainModule
import java.io.File

private const val MODULE_EMULATION_FILE = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/moduleEmulation.js"

val File.esModulesSubDir: File
    get() = File(absolutePath + "_esm")

private fun extractJsFiles(testServices: TestServices, modules: List<TestModule>): Pair<List<String>, List<String>> {
    val outputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices)

    fun copyInputJsFile(module: TestModule, inputJsFile: TestFile): String {
        val newName = JsEnvironmentConfigurator.getJsArtifactSimpleName(testServices, module.name) + "-js-" + inputJsFile.name
        val targetFile = File(outputDir, newName)
        targetFile.writeText(inputJsFile.originalContent)
        return targetFile.absolutePath
    }

    val inputJsFiles = modules
        .flatMap { module -> module.files.map { module to it } }
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

private fun getAdditionalMjsFiles(testServices: TestServices): List<String> {
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

    return originalFile.parentFile.resolve(originalFile.nameWithoutExtension + ".mjs")
        .takeIf { it.exists() }
        ?.let { listOf(it.absolutePath) } ?: emptyList()
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
): Map<TranslationMode, List<String>> {
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

    val commonFiles = JsAdditionalSourceProvider.getAdditionalJsFiles(originalFile.parent).map { it.absolutePath }
    val (inputJsFilesBefore, inputJsFilesAfter) = extractJsFiles(testServices, testServices.moduleStructure.modules)
    val additionalFiles = getAdditionalFiles(testServices)
    val additionalMainFiles = getAdditionalMainFiles(testServices)

    if (modulesToArtifact.values.any { it is BinaryArtifacts.Js.JsIrArtifact }) {
        // JS IR
        val (module, compilerResult) = modulesToArtifact.entries.mapNotNull { (m, c) -> (c as? BinaryArtifacts.Js.JsIrArtifact)?.let { m to c.compilerResult } }.single()

        val result = mutableMapOf<TranslationMode, List<String>>()

        compilerResult.outputs.entries.forEach { (mode, outputs) ->
            val paths = mutableListOf<String>()

            val outputFile = JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, mode) + ".js"
            outputs.dependencies.forEach { (moduleId, _) ->
                paths += outputFile.augmentWithModuleName(moduleId)
            }
            paths += outputFile

            result[mode] = additionalFiles + commonFiles + inputJsFilesBefore + paths + additionalMainFiles + inputJsFilesAfter
        }

        return result
    } else {
        // Old BE and ES modules
        val outputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices)
        val dceOutputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.FULL_DCE)

        val artifactsPaths = modulesToArtifact.values.map { it.outputFile.absolutePath }.filter { !File(it).isDirectory }
        val allJsFiles = additionalFiles + inputJsFilesBefore + commonFiles + artifactsPaths + additionalMainFiles + inputJsFilesAfter

        val result = mutableMapOf(TranslationMode.FULL to allJsFiles)

        val globalDirectives = testServices.moduleStructure.allDirectives
        val runIrDce = JsEnvironmentConfigurationDirectives.RUN_IR_DCE in globalDirectives
        if (runIrDce) {
            val dceJsFiles = artifactsPaths.map { it.replace(outputDir.absolutePath, dceOutputDir.absolutePath) }
            val dceAllJsFiles = additionalFiles + inputJsFilesBefore + commonFiles + dceJsFiles + additionalMainFiles + inputJsFilesAfter
            result[TranslationMode.FULL_DCE] = dceAllJsFiles
        }

        return result
    }
}

fun extractAllFilesForEsRunner(testServices: TestServices, esmOutputDir: File): Pair<List<String>, List<String>> {
    val modules = testServices.moduleStructure.modules
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

    val commonFiles = JsAdditionalSourceProvider.getAdditionalJsFiles(originalFile.parent).map { it.absolutePath }
    val (inputJsFilesBefore, inputJsFilesAfter) = extractJsFiles(testServices, modules)
    val additionalFiles = getAdditionalFiles(testServices)
    val additionalMjsFiles = getAdditionalMjsFiles(testServices)
    val additionalMainFiles = getAdditionalMainFiles(testServices)

    val allNonEsModuleFiles = additionalFiles + inputJsFilesBefore + commonFiles

    // Copy __main file if present
    if (additionalMainFiles.isNotEmpty()) {
        val newFileName = File(esmOutputDir, "test.mjs")
        newFileName.delete()
        File(additionalMainFiles.first()).copyTo(newFileName)
    }

    // Copy all .mjs files into generated directory
    modules.flatMap { it.files }
        .filter { it.isMjsFile }
        .map { File(esmOutputDir, it.name).writeText(it.originalContent) }

    additionalMjsFiles.forEach { mjsFile ->
        val outFile = File(esmOutputDir, File(mjsFile).name)
        File(mjsFile).copyTo(outFile, overwrite = true)
    }

    return Pair(allNonEsModuleFiles, inputJsFilesAfter)
}

fun getOnlyJsFilesForRunner(testServices: TestServices, modulesToArtifact: Map<TestModule, BinaryArtifacts.Js>): List<String> {
    return getAllFilesForRunner(testServices, modulesToArtifact).let {
        it[TranslationMode.FULL] ?: it[TranslationMode.PER_MODULE]!!
    }
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

    return ktFiles.singleOrNull { ktFile ->
        val boxFunction = ktFile.declarations.find { it is KtNamedFunction && it.name == TEST_FUNCTION }
        boxFunction != null
    }?.packageFqName?.asString()?.takeIf { it.isNotEmpty() }
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
