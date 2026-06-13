/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import lombok.Getter
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lombok.LombokDirectives.ENABLE_LOMBOK
import org.jetbrains.kotlin.lombok.LombokDirectives.WITH_GUAVA
import org.jetbrains.kotlin.lombok.LombokDirectives.WITH_ADVANCED_LOGGERS
import org.jetbrains.kotlin.lombok.LombokEnvironmentConfigurator.Companion.GUAVA_JAR
import org.jetbrains.kotlin.lombok.LombokEnvironmentConfigurator.Companion.ADVANCED_LOGGER_JARS
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

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        if (ENABLE_LOMBOK !in module.directives ||
            // Include the common file only in a single module to get rid of errors and exceptions
            module.allDependencies.any { dependency -> dependency.dependencyModule.files.any { it.originalFile.endsWith(COMMON_SOURCE_PATH) } }
        ) {
            return emptyList()
        }
        return listOf(ForTestCompileRuntime.transformTestDataPath(COMMON_SOURCE_PATH).toTestFile())
    }
}

class LombokEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        const val LOMBOK_CONFIG_NAME = "lombok.config"
        private const val TEST_PROPERTY_PREFIX = "org.jetbrains.kotlin.test"

        val GUAVA_JAR: File by lazy {
            EnvironmentBasedStandardLibrariesPathProvider.getFile("$TEST_PROPERTY_PREFIX.guava")
        }

        val ADVANCED_LOGGER_JARS: List<File> by lazy {
            buildList {
                listOf(
                    "slf4j-api",
                    "slf4j-ext",
                    "log4j-over-slf4j",
                    "commons-logging",
                    "flogger",
                    "flogger-system-backend",
                    "jboss-logging",
                    "log4j-api",
                    "log4j-core",
                ).forEach {
                    add(EnvironmentBasedStandardLibrariesPathProvider.getFile("$TEST_PROPERTY_PREFIX.$it"))
                }
            }
        }
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(LombokDirectives)

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (ENABLE_LOMBOK !in module.directives) return
        configuration.addJvmClasspathRoot(PathUtil.getResourcePathForClass(Getter::class.java))
        if (WITH_GUAVA in module.directives) {
            configuration.addJvmClasspathRoot(GUAVA_JAR)
        }
        if (WITH_ADVANCED_LOGGERS in module.directives) {
            configuration.addJvmClasspathRoots(ADVANCED_LOGGER_JARS)
        }

        createStopBubblingConfig(module)
        val lombokConfig = findLombokConfig(module) ?: return
        lombokConfig.copyTo(testServices.sourceFileProvider.getJavaSourceDirectoryForModule(module).resolve(lombokConfig.name))
        configuration.put(LombokConfigurationKeys.LOMBOK_CONFIG_FILE, lombokConfig)
    }

    /**
     * This config will prevent lombok from finding `lombok.config` file in parent directories.
     */
    private fun createStopBubblingConfig(module: TestModule) {
        val file = testServices.sourceFileProvider.getJavaSourceDirectoryForModule(module).parentFile.resolve(LOMBOK_CONFIG_NAME)
        file.writeText("config.stopBubbling = true")
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
        return buildList {
            if (WITH_GUAVA in module.directives) {
                add(GUAVA_JAR)
            }
            if (WITH_ADVANCED_LOGGERS in module.directives) {
                addAll(ADVANCED_LOGGER_JARS)
            }
        }
    }
}

object LombokDirectives : SimpleDirectivesContainer() {
    val ENABLE_LOMBOK by directive("Enables lombok plugin")
    val WITH_GUAVA by directive("Add guava to classpath")
    val WITH_ADVANCED_LOGGERS by directive("Add slf4j, slf4j-ext, log4j, commons-logging, flogger, jboss-logging and log4j2 to classpath")
}
