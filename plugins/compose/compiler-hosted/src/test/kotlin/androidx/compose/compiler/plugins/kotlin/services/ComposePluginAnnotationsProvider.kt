/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class ComposePluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        val defaultClassPath by lazy {
            System.getProperty("java.class.path")!!.split(
                System.getProperty("path.separator")!!
            ).map { File(it) }
        }
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val platform = module.targetPlatform
        when {
            platform.isJvm() -> {
                defaultClassPath.firstOrNull { it.absolutePath.contains("compose") && it.absolutePath.contains("runtime") }?.let {
                    configuration.addJvmClasspathRoot(it)
                }
            }
        }
    }
}
