/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.js.test.runtimeSources
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

fun buildConfiguration(environment: KotlinCoreEnvironment): CompilerConfiguration {
    val runtimeConfiguration = environment.configuration.copy()
    runtimeConfiguration.put(CommonConfigurationKeys.MODULE_NAME, "JS_IR_RUNTIME")

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

private val stdKlibFile = File("js/js.translator/testData/out/klibs/stdlib.klib")

fun main() {

    val environment = KotlinCoreEnvironment.createForTests(Disposable { }, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    fun createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(environment.project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

        return psiManager.findFile(file) as KtFile
    }

    val result = compile(
        environment.project,
        runtimeSources.map(::createPsiFile),
        buildConfiguration(environment),
        null,
        true,
        emptyList(),
        emptyList()
    )

    TODO("Write library into $stdKlibFile")
}