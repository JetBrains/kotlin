/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class JvmAbiComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        @Suppress("NAME_SHADOWING")
        val configuration = configuration.copy()
        configuration.get(JvmAbiConfigurationKeys.OUTPUT_DIR)?.let {
            val dir = File(it)
            configuration.put(JVMConfigurationKeys.OUTPUT_DIRECTORY, dir)
        }
        val extension = JvmAbiAnalysisHandlerExtension(configuration)
        AnalysisHandlerExtension.registerExtension(project, extension)
    }
}