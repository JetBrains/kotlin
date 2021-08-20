/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import com.intellij.openapi.project.Project
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.VersionReader
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class RuntimeLibraryInClasspathTest {
    private val coreLibraryPath = getSerializationCoreLibraryJar()

    @Test
    fun testRuntimeLibraryExists() {
        TestCase.assertNotNull(
            "kotlinx-serialization runtime library is not found. Make sure it is present in test classpath",
            coreLibraryPath
        )
    }

    @Test
    fun testRuntimeHasSufficientVersion() {
        val version = VersionReader.getVersionsFromManifest(coreLibraryPath!!)
        assertTrue(version.currentCompilerMatchRequired(), "Runtime version too high")
        assertTrue(version.implementationVersionMatchSupported(), "Runtime version too low")
    }
}

internal fun getSerializationCoreLibraryJar(): File? = getSerializationLibraryJar("kotlinx.serialization.KSerializer")

internal fun getSerializationLibraryJar(classToDetect: String): File? = try {
    PathUtil.getResourcePathForClass(Class.forName(classToDetect))
} catch (e: ClassNotFoundException) {
    null
}

internal fun TestConfigurationBuilder.configureForKotlinxSerialization(librariesPaths: List<File>) {
    useConfigurators(
        { services ->
            object : EnvironmentConfigurator(services) {
                override fun configureCompilerConfiguration(
                    configuration: CompilerConfiguration,
                    module: TestModule
                ) {
                    configuration.addJvmClasspathRoots(librariesPaths)
                }

                override fun registerCompilerExtensions(project: Project) {
                    SerializationComponentRegistrar.registerExtensions(project)
                }
            }
        })
    useCustomRuntimeClasspathProvider {
        object : RuntimeClasspathProvider() {
            override fun runtimeClassPaths(): List<File> = librariesPaths
        }
    }
}