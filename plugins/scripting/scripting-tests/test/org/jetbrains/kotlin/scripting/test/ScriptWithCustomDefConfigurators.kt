/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

private val testScriptDefinitionClasspath by lazy {
    System.getProperty("kotlin.script.test.script.definition.classpath")!!.split(File.pathSeparator).map {
        File(it).also {
            require(it.exists()) {
                "The file required for custom test script definition not found: $it"
            }
        }
    }
}

class ScriptWithCustomDefEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoots(testScriptDefinitionClasspath)
        val dirSplitRegex = Regex(" *, *")
        ScriptingTestDirectives.directivesToPassViaEnvironment.forEach { (directive, envName) ->
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