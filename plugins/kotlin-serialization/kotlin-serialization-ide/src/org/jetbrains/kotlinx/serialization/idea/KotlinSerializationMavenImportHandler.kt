/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea

import org.jetbrains.kotlin.plugin.ide.AbstractMavenImportHandler
import org.jetbrains.kotlin.plugin.ide.CompilerPluginSetup
import java.io.File

class KotlinSerializationMavenImportHandler : AbstractMavenImportHandler() {
    override val compilerPluginId: String = "org.jetbrains.kotlinx.serialization"
    override val pluginName: String = "serialization"
    override val pluginJarFileFromIdea: File
        get() = File(KotlinSerializationImportHandler.PLUGIN_JPS_JAR)

    override fun getOptions(
        enabledCompilerPlugins: List<String>,
        compilerPluginOptions: List<String>
    ): List<CompilerPluginSetup.PluginOption>? =
        if ("kotlinx-serialization" in enabledCompilerPlugins) emptyList() else null
}