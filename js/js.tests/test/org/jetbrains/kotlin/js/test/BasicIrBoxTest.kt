/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.ir.backend.js.CompilationMode
import org.jetbrains.kotlin.ir.backend.js.CompiledModule
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

private val fullRuntimeKlibPath = "js/js.translator/testData/out/klibs/runtimeFull/"
private val defaultRuntimeKlibPath = "js/js.translator/testData/out/klibs/runtimeDefault/"

private val JS_IR_RUNTIME_MODULE_NAME = "JS_IR_RUNTIME"

private val fullRuntimeKlib = CompiledModule(JS_IR_RUNTIME_MODULE_NAME, null, null, fullRuntimeKlibPath, emptyList(), true)
private val defaultRuntimeKlib = CompiledModule(JS_IR_RUNTIME_MODULE_NAME, null, null, defaultRuntimeKlibPath, emptyList(), true)

abstract class BasicIrBoxTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    pathToRootOutputDir: String = BasicBoxTest.TEST_DATA_DIR_PATH,
    generateSourceMap: Boolean = false,
    generateNodeJsRunner: Boolean = false
) : BasicBoxTest(
    pathToTestDir,
    testGroupOutputDirPrefix,
    pathToRootOutputDir = pathToRootOutputDir,
    typedArraysEnabled = true,
    generateSourceMap = generateSourceMap,
    generateNodeJsRunner = generateNodeJsRunner,
    targetBackend = TargetBackend.JS_IR
) {

    override val skipMinification = true

    // TODO Design incremental compilation for IR and add test support
    override val incrementalCompilationChecksEnabled = false

    private val compilationCache = mutableMapOf<String, CompiledModule>()

    override fun doTest(filePath: String, expectedResult: String, mainCallParameters: MainCallParameters, coroutinesPackage: String) {
        compilationCache.clear()
        super.doTest(filePath, expectedResult, mainCallParameters, coroutinesPackage)
    }

    private val runtimes = mapOf(JsIrTestRuntime.DEFAULT to defaultRuntimeKlib,
                                 JsIrTestRuntime.FULL to fullRuntimeKlib)

    override fun translateFiles(
        units: List<TranslationUnit>,
        outputFile: File,
        config: JsConfig,
        outputPrefixFile: File?,
        outputPostfixFile: File?,
        mainCallParameters: MainCallParameters,
        incrementalData: IncrementalData,
        remap: Boolean,
        testPackage: String?,
        testFunction: String,
        runtime: JsIrTestRuntime,
        isMainModule: Boolean
    ) {
        val filesToCompile = units
            .map { (it as TranslationUnit.SourceFile).file }
            // TODO: split input files to some parts (global common, local common, test)
            .filterNot { it.virtualFilePath.contains(BasicBoxTest.COMMON_FILES_DIR_PATH) }

//        config.configuration.put(CommonConfigurationKeys.EXCLUDED_ELEMENTS_FROM_DUMPING, setOf("<JS_IR_RUNTIME>"))
//        config.configuration.put(
//            CommonConfigurationKeys.PHASES_TO_VALIDATE_AFTER,
//            setOf(
//                "RemoveInlineFunctionsWithReifiedTypeParametersLowering",
//                "InnerClassConstructorCallsLowering",
//                "InlineClassLowering", "ConstLowering"
//            )
//        )

        val runtimeKlib = runtimes[runtime]

        val dependencyNames = config.configuration[JSConfigurationKeys.LIBRARIES]!!.map { File(it).name }
        val dependencies = listOfNotNull(runtimeKlib) + dependencyNames.mapNotNull {
            compilationCache[it]
        }

//        config.configuration.put(CommonConfigurationKeys.PHASES_TO_DUMP_STATE, setOf("UnitMaterializationLowering"))
//        config.configuration.put(CommonConfigurationKeys.PHASES_TO_DUMP_STATE_BEFORE, setOf("ReturnableBlockLowering"))
//        config.configuration.put(CommonConfigurationKeys.PHASES_TO_DUMP_STATE_AFTER, setOf("MultipleCatchesLowering"))
//        config.configuration.put(CommonConfigurationKeys.PHASES_TO_VALIDATE, setOf("ALL"))

        val actualOutputFile = outputFile.absolutePath.let {
            if (!isMainModule) it.replace("_v5.js", "/") else it
        }

        val result = compile(
            config.project,
            filesToCompile,
            config.configuration,
            listOf(FqName((testPackage?.let { "$it." } ?: "") + testFunction)),
            if (isMainModule) CompilationMode.JS_AGAINST_KLIB else CompilationMode.KLIB,
            dependencies,
            actualOutputFile
        )

        compilationCache[outputFile.name.replace(".js", ".meta.js")] = result

        val generatedCode = result.generatedCode
        if (generatedCode != null) {
            val wrappedCode = wrapWithModuleEmulationMarkers(generatedCode, moduleId = config.moduleId, moduleKind = config.moduleKind)
            outputFile.write(wrappedCode)
        }
    }

    override fun runGeneratedCode(
        jsFiles: List<String>,
        testModuleName: String?,
        testPackage: String?,
        testFunction: String,
        expectedResult: String,
        withModuleSystem: Boolean,
        runtime: JsIrTestRuntime
    ) {
        // TODO: should we do anything special for module systems?
        // TODO: return list of js from translateFiles and provide then to this function with other js files

        V8IrJsTestChecker.check(jsFiles, testModuleName, null, testFunction, expectedResult, withModuleSystem)
    }
}


private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
