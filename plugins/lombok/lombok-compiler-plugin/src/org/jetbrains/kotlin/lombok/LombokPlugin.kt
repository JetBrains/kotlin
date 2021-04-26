/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.lombok.LombokConfigurationKeys.CONFIG_FILE
import org.jetbrains.kotlin.resolve.jvm.extensions.SyntheticJavaResolveExtension
import java.io.File
import java.lang.IllegalArgumentException

class LombokComponentRegistrar : ComponentRegistrar {

    companion object {
        fun registerComponents(project: Project, compilerConfiguration: CompilerConfiguration) {
            val config = LombokPluginConfig(compilerConfiguration[CONFIG_FILE])
            SyntheticJavaResolveExtension.registerExtension(project, LombokResolveExtension(config))
        }
    }

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        registerComponents(project, configuration)
    }
}

data class LombokPluginConfig(val configFile: File?)

object LombokConfigurationKeys {
    val CONFIG_FILE: CompilerConfigurationKey<File> = CompilerConfigurationKey.create("lombok config file location")
}

class LombokCommandLineProcessor : CommandLineProcessor {

    companion object {
        const val PLUGIN_ID = "org.jetbrains.kotlin.lombok"

        val CONFIG_FILE_OPTION = CliOption(
            optionName = "config",
            valueDescription = "<path>",
            description = "Lombok configuration file location",
            required = false
        )
    }

    override val pluginId: String = PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = listOf(CONFIG_FILE_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            CONFIG_FILE_OPTION -> configuration.put(CONFIG_FILE, File(value))
            else -> throw IllegalArgumentException("Unknown option $option")
        }
    }
}
