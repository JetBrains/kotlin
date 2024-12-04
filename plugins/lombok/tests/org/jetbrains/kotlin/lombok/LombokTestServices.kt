/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import lombok.Getter
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lombok.LombokDirectives.ENABLE_LOMBOK
import org.jetbrains.kotlin.lombok.LombokDirectives.WITH_GUAVA
import org.jetbrains.kotlin.lombok.LombokEnvironmentConfigurator.Companion.GUAVA_JAR
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class LombokAdditionalSourceFileProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        const val COMMON_SOURCE_PATH = "plugins/lombok/testData/common.kt"
    }

    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        if (ENABLE_LOMBOK !in module.directives) return emptyList()
        return listOf(File(COMMON_SOURCE_PATH).toTestFile())
    }
}

class LombokEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        const val LOMBOK_CONFIG_NAME = "lombok.config"
        private const val guavaPropertyName = "org.jetbrains.kotlin.test.guava-location"

        val GUAVA_JAR: File
            get() = EnvironmentBasedStandardLibrariesPathProvider.getFile(guavaPropertyName)
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(LombokDirectives)

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (ENABLE_LOMBOK !in module.directives) return
        configuration.addJvmClasspathRoot(PathUtil.getResourcePathForClass(Getter::class.java))
        if (WITH_GUAVA in module.directives) {
            configuration.addJvmClasspathRoot(GUAVA_JAR)
        }

        val lombokConfig = findLombokConfig(module) ?: return
        lombokConfig.copyTo(testServices.sourceFileProvider.getJavaSourceDirectoryForModule(module).resolve(lombokConfig.name))
        configuration.put(LombokConfigurationKeys.CONFIG_FILE, lombokConfig)
    }

    private fun findLombokConfig(module: TestModule): File? {
        return module.files.singleOrNull { it.name == LOMBOK_CONFIG_NAME }?.let {
            testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(it)
        }
    }

    override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
        if (ENABLE_LOMBOK !in module.directives) return
        LombokComponentRegistrar.registerComponents(this, configuration)
    }
}

class LombokRuntimeClassPathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        if (ENABLE_LOMBOK !in module.directives) return emptyList()
        return if (WITH_GUAVA in module.directives) {
            listOf(GUAVA_JAR)
        } else {
            emptyList()
        }
    }
}

object LombokDirectives : SimpleDirectivesContainer() {
    val ENABLE_LOMBOK by directive("Enables lombok plugin")
    val WITH_GUAVA by directive("Add guava to classpath")
}
