/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.definitions

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.fir.CollectAdditionalScriptSourcesExtension
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.PreAnalysisHandler
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.getKtSourceFilesForSourceFiles
import org.jetbrains.kotlin.test.services.isKtsFile
import org.jetbrains.kotlin.test.services.sourceFileProvider
import java.io.File

val testScriptDefinitionClasspath by lazy {
    ForTestCompileRuntime.testScriptDefinitionClasspathForTests()
}

class ScriptWithCustomDefEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoots(testScriptDefinitionClasspath)
        val dirSplitRegex = Regex(" *, *")
        ScriptingTestDirectives.directivesToPassViaEnvironment.forEach { [directive, envName] ->
            if (directive is StringDirective) {
                module.directives[directive].flatMap { it.split(dirSplitRegex).filter { it.isNotEmpty() } }.let {
                    if (it.isNotEmpty()) {
                        configuration.put(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION, envName, it)
                    }
                }
            } else {
                if (directive in module.directives) {
                    configuration.put(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION, envName, "true")
                }
            }
        }
    }

    override val directiveContainers: List<DirectivesContainer> = listOf(ScriptingTestDirectives)
}

class ScriptWithCustomDefRuntimeClassPathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> = testScriptDefinitionClasspath
}

/**
 * Refines the scripts of the test modules before the analysis, the same way it is done in the CLI pipeline (see `collectSources`):
 * the refined configurations are stored in the refined configurations cache of the host configuration in the compiler configuration,
 * where they are later found by the FIR sessions of the test.
 */
class ScriptWithCustomDefPreRefinementHandler(testServices: TestServices) : PreAnalysisHandler(testServices) {
    override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        for (module in moduleStructure.modules) {
            val scriptSources = testServices.sourceFileProvider
                .getKtSourceFilesForSourceFiles(module.files.filter { it.isKtsFile }, keepNonKtFiles = false)
                .values
            if (scriptSources.isEmpty()) continue
            val configuration = compilerConfigurationProvider.getCompilerConfiguration(module, CompilationStage.FIRST)
            val projectEnvironment = VfsBasedProjectEnvironment(
                compilerConfigurationProvider.getProject(module),
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                compilerConfigurationProvider.getPackagePartProviderFactory(module),
            )
            CollectAdditionalScriptSourcesExtension().collectSources(projectEnvironment, configuration, { null }, scriptSources)
        }
    }
}
