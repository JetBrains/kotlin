/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.io.File

class DataFramePluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        const val ANNOTATIONS_JAR =
            "plugins/kotlin-dataframe/plugin-annotations/build/libs/plugin-annotations-1.6.255-SNAPSHOT.jar"
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val jar = File(ANNOTATIONS_JAR)
        testServices.assertions.assertTrue(jar.exists()) { "Jar with annotations does not exist. Please run :plugins:kotlin-dataframe:plugin-annotations:jar" }
        configuration.addJvmClasspathRoot(jar)
    }
}