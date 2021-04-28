/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.ide

import org.jetbrains.kotlin.lombok.LombokCommandLineProcessor
import org.jetbrains.kotlin.lombok.LombokCommandLineProcessor.Companion.CONFIG_FILE_OPTION
import org.jetbrains.kotlin.plugin.ide.AbstractMavenImportHandler
import org.jetbrains.kotlin.plugin.ide.CompilerPluginSetup.PluginOption
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class LombokMavenProjectImportHandler : AbstractMavenImportHandler() {
    override val compilerPluginId: String = LombokCommandLineProcessor.PLUGIN_ID
    override val pluginName: String = MAVEN_SUBPLUGIN_NAME
    override val pluginJarFileFromIdea: File = PathUtil.kotlinPathsForIdeaPlugin.lombokPluginJarPath

    override fun getOptions(
        enabledCompilerPlugins: List<String>,
        compilerPluginOptions: List<String>
    ): List<PluginOption>? {
        if (!enabledCompilerPlugins.contains(pluginName)) return null

        return compilerPluginOptions.mapNotNull { option ->
            if (option.startsWith(CONFIG_FILE_PREFIX)) {
                val location = option.substring(CONFIG_FILE_PREFIX.length)
                PluginOption(CONFIG_FILE_OPTION.optionName, location)
            } else {
                null
            }
        }
    }

    companion object {
        private const val MAVEN_SUBPLUGIN_NAME = "lombok"
        private val CONFIG_FILE_PREFIX = "$MAVEN_SUBPLUGIN_NAME:${CONFIG_FILE_OPTION.optionName}="
    }
}
