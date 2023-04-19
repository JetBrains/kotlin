/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.utils

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.extension
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.converters.augmentWithModuleName
import org.jetbrains.kotlin.js.test.converters.finalizePath
import org.jetbrains.kotlin.js.test.converters.kind
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
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import java.io.File

const val MODULE_EMULATION_FILE = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/moduleEmulation.js"

fun TestModule.getNameFor(filePath: String, testServices: TestServices): String {
    return JsEnvironmentConfigurator.getJsArtifactSimpleName(testServices, name) + "-js-" + filePath
}

fun TestModule.getNameFor(file: TestFile, testServices: TestServices): String {
    return getNameFor(file.name, testServices)
}

private fun extractJsFiles(
    testServices: TestServices,
    modules: List<TestModule>,
    mode: TranslationMode = TranslationMode.FULL_DEV,
): Pair<List<String>, List<String>> {
    val outputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, mode)

    fun copyInputJsFile(module: TestModule, inputJsFile: TestFile): String {
        val newName = module.getNameFor(inputJsFile, testServices)
        val targetFile = File(outputDir, newName)
        targetFile.writeText(inputJsFile.originalContent)
        return targetFile.absolutePath
    }

    val inputJsFiles = modules
        .flatMap { module -> module.files.map { module to it } }
        .filter { it.second.isJsFile || it.second.isMjsFile }

    val after = inputJsFiles
        .filter { (module, inputJsFile) -> inputJsFile.name.endsWith("__after${module.kind.extension}") }
        .map { (module, inputJsFile) -> copyInputJsFile(module, inputJsFile) }
    val before = inputJsFiles
        .filterNot { (module, inputJsFile) -> inputJsFile.name.endsWith("__after${module.kind.extension}") }
        .map { (module, inputJsFile) -> copyInputJsFile(module, inputJsFile) }

    return before to after
}

fun getAdditionalFilePathes(testServices: TestServices, mode: TranslationMode = TranslationMode.FULL_DEV): List<String> {
    return getAdditionalFiles(testServices, mode, true).map { it.absolutePath }
}

fun getAdditionalFiles(
    testServices: TestServices,
    mode: TranslationMode = TranslationMode.FULL_DEV,
    shouldCopyFiles: Boolean = false
): List<File> {
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

    val withModuleSystem = testWithModuleSystem(testServices)

    val additionalFiles = mutableListOf<File>()
    if (withModuleSystem) additionalFiles += File(MODULE_EMULATION_FILE)

    originalFile.parentFile.resolve(originalFile.nameWithoutExtension + JavaScript.DOT_EXTENSION)
        .takeIf { it.exists() }
        ?.let { additionalFiles += it }

    originalFile.parentFile.resolve(originalFile.nameWithoutExtension + JavaScript.DOT_MODULE_EXTENSION)
        .takeIf { it.exists() }
        ?.let {
            File(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, mode), it.name).apply {
                if (shouldCopyFiles) it.copyTo(this, true)
            }
        }
        ?.let { additionalFiles += it }

    return additionalFiles
}

fun getAdditionalMainFilePathes(testServices: TestServices, mode: TranslationMode = TranslationMode.FULL_DEV): List<String> {
    return getAdditionalMainFiles(testServices, mode, shouldCopyFiles = true).map { it.absolutePath }
}

fun getAdditionalMainFiles(
    testServices: TestServices,
    mode: TranslationMode = TranslationMode.FULL_DEV,
    shouldCopyFiles: Boolean = false
): List<File> {
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
    val additionalFiles = mutableListOf<File>()

    originalFile.parentFile.resolve(originalFile.nameWithoutExtension + "__main.js")
        .takeIf { it.exists() }
        ?.let { additionalFiles += it }

    originalFile.parentFile.resolve(originalFile.nameWithoutExtension + "__main.mjs")
        .takeIf { it.exists() }
        ?.let {
            File(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, mode), it.name).apply {
                if (shouldCopyFiles) it.copyTo(this, true)
            }
        }
        ?.let { additionalFiles += it }

    return additionalFiles
}

fun testWithModuleSystem(testServices: TestServices): Boolean {
    val globalDirectives = testServices.moduleStructure.allDirectives
    val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(getMainModule(testServices))
    val mainModuleKind = configuration[JSConfigurationKeys.MODULE_KIND]
    return mainModuleKind != ModuleKind.PLAIN && mainModuleKind != ModuleKind.ES && NO_JS_MODULE_SYSTEM !in globalDirectives
}

fun getModeOutputFilePath(testServices: TestServices, module: TestModule, mode: TranslationMode): String {
    return JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, mode).finalizePath(module.kind)
}

fun getAllFilesForRunner(
    testServices: TestServices, modulesToArtifact: Map<TestModule, BinaryArtifacts.Js>
): Map<TranslationMode, List<String>> {
    val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

    val commonFiles = JsAdditionalSourceProvider.getAdditionalJsFiles(originalFile.parent).map { it.absolutePath }

    if (modulesToArtifact.values.any { it is BinaryArtifacts.Js.JsIrArtifact }) {
        // JS IR
        val (module, compilerResult) = modulesToArtifact.entries.mapNotNull { (m, c) -> (c as? BinaryArtifacts.Js.JsIrArtifact)?.let { m to c.compilerResult } }
            .single()
        val result = mutableMapOf<TranslationMode, List<String>>()

        compilerResult.outputs.entries.forEach { (mode, outputs) ->
            val paths = mutableListOf<String>()

            val outputFile = getModeOutputFilePath(testServices, module, mode)
            val (inputJsFilesBefore, inputJsFilesAfter) = extractJsFiles(testServices, testServices.moduleStructure.modules, mode)
            val additionalFiles = getAdditionalFilePathes(testServices, mode)
            val additionalMainFiles = getAdditionalMainFilePathes(testServices, mode)

            outputs.dependencies.forEach { (moduleId, _) ->
                paths += outputFile.augmentWithModuleName(moduleId)
            }
            paths += outputFile

            result[mode] = additionalFiles + inputJsFilesBefore + paths + commonFiles + additionalMainFiles + inputJsFilesAfter
        }

        return result
    } else {
        val (inputJsFilesBefore, inputJsFilesAfter) = extractJsFiles(testServices, testServices.moduleStructure.modules)
        val additionalFiles = getAdditionalFilePathes(testServices)
        val additionalMainFiles = getAdditionalMainFilePathes(testServices)
        // Old BE
        val outputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices)
        val dceOutputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.FULL_PROD_MINIMIZED_NAMES)

        val artifactsPaths = modulesToArtifact.values.map { it.outputFile.absolutePath }.filter { !File(it).isDirectory }
        val allJsFiles = additionalFiles + inputJsFilesBefore + artifactsPaths + commonFiles + additionalMainFiles + inputJsFilesAfter

        val result = mutableMapOf<TranslationMode, List<String>>()

        val globalDirectives = testServices.moduleStructure.allDirectives
        val runIrDce = JsEnvironmentConfigurationDirectives.RUN_IR_DCE in globalDirectives
        val onlyIrDce = JsEnvironmentConfigurationDirectives.ONLY_IR_DCE in globalDirectives
        if (!onlyIrDce) {
            result[TranslationMode.FULL_DEV] = allJsFiles
        }
        if (runIrDce) {
            val dceJsFiles = artifactsPaths.map { it.replace(outputDir.absolutePath, dceOutputDir.absolutePath) }
            val dceAllJsFiles = additionalFiles + inputJsFilesBefore + dceJsFiles + commonFiles + additionalMainFiles + inputJsFilesAfter
            result[TranslationMode.FULL_PROD_MINIMIZED_NAMES] = dceAllJsFiles
        }

        return result
    }
}

fun getOnlyJsFilesForRunner(testServices: TestServices, modulesToArtifact: Map<TestModule, BinaryArtifacts.Js>): List<String> {
    return getAllFilesForRunner(testServices, modulesToArtifact).let {
        it[TranslationMode.FULL_DEV] ?: it[TranslationMode.PER_MODULE_DEV]!!
    }
}

fun getTestModuleName(testServices: TestServices): String? {
    val runPlainBoxFunction = RUN_PLAIN_BOX_FUNCTION in testServices.moduleStructure.allDirectives
    if (runPlainBoxFunction) return null
    return getMainModule(testServices).name
}

fun getBoxFunction(testServices: TestServices): KtNamedFunction? {
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

    return ktFiles.mapNotNull { ktFile ->
        ktFile.declarations.filterIsInstanceAnd<KtNamedFunction> { it.name == TEST_FUNCTION }.firstOrNull()
    }.singleOrNull()
}

fun extractTestPackage(testServices: TestServices, ignoreEsModules: Boolean = true): String? {
    val runPlainBoxFunction = RUN_PLAIN_BOX_FUNCTION in testServices.moduleStructure.allDirectives
    if (runPlainBoxFunction) return null

    val ktFiles = testServices.moduleStructure.modules.flatMap { module ->
        module.files
            .filter { it.isKtFile }
            .map {
                val project = testServices.compilerConfigurationProvider.getProject(module)
                module to testServices.sourceFileProvider.getKtFileForSourceFile(it, project)
            }
    }

    val fileWithBoxFunction = ktFiles.find { (module, ktFile) ->
        (!ignoreEsModules || module.kind != ModuleKind.ES) &&
                ktFile.declarations.find { it is KtNamedFunction && it.name == TEST_FUNCTION } != null
    } ?: return null

    return fileWithBoxFunction.second.packageFqName.asString().takeIf { it.isNotEmpty() }
}

fun extractEntryModulePath(
    mode: TranslationMode,
    testServices: TestServices,
): String? =
    if (getBoxFunction(testServices) == null) {
        testServices.moduleStructure.modules
            .find { JsEnvironmentConfigurator.isMainModule(it, testServices) }
            ?.run {
                files
                    .find { it.isMjsFile && JsEnvironmentConfigurationDirectives.ENTRY_ES_MODULE in it.directives }
                    ?.let {
                        JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, mode).absolutePath +
                                File.separator + getNameFor(it, testServices)
                    }
            }

    } else {
        testServices.moduleStructure.modules
            .find { JsEnvironmentConfigurator.isMainModule(it, testServices) }
            ?.let { getModeOutputFilePath(testServices, it, mode) }
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
