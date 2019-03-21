/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.backend.js.CompilationMode
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.ir.backend.js.jsPhases
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.JsIrTestRuntime
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind

fun buildConfiguration(environment: KotlinCoreEnvironment): CompilerConfiguration {
    val runtimeConfiguration = environment.configuration.copy()
    runtimeConfiguration.put(CommonConfigurationKeys.MODULE_NAME, "JS_IR_RUNTIME")
    runtimeConfiguration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

    runtimeConfiguration.languageVersionSettings = LanguageVersionSettingsImpl(
        LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE,
        specificFeatures = mapOf(
            LanguageFeature.AllowContractsForCustomFunctions to LanguageFeature.State.ENABLED,
            LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED
        ),
        analysisFlags = mapOf(
            AnalysisFlags.useExperimental to listOf("kotlin.contracts.ExperimentalContracts", "kotlin.Experimental"),
            AnalysisFlags.allowResultReturnType to true
        )
    )

    return runtimeConfiguration
}

private val fullRuntimeKlibPath = "js/js.translator/testData/out/klibs/runtimeFull/"
private val defaultRuntimeKlibPath = "js/js.translator/testData/out/klibs/runtimeDefault/"

fun main() {

    val environment = KotlinCoreEnvironment.createForTests(Disposable { }, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    fun createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(environment.project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

        return psiManager.findFile(file) as KtFile
    }


    fun buildKlib(sources: List<String>, outputPath: String) {
        val configuration = buildConfiguration(environment)
        compile(
            project = environment.project,
            files = sources.map(::createPsiFile),
            configuration = configuration,
            phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jsPhases),
            compileMode = CompilationMode.KLIB,
            immediateDependencies = emptyList(),
            allDependencies = emptyList(),
            outputKlibPath = outputPath
        )
    }

    buildKlib(JsIrTestRuntime.FULL.sources, fullRuntimeKlibPath)
    buildKlib(JsIrTestRuntime.DEFAULT.sources, defaultRuntimeKlibPath)
}
