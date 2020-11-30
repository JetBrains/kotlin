/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class JvmAbiComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val outputPath = configuration.getNotNull(JvmAbiConfigurationKeys.OUTPUT_PATH)
        if (configuration.get(JvmAbiConfigurationKeys.LEGACY_ABI_GEN, false)) {
            // Use the two-pass implementation
            require(!outputPath.endsWith(".jar")) {
                "Legacy jvm-abi-gen does not support jar output."
            }
            val extension = JvmAbiAnalysisHandlerExtension(configuration.copy().apply {
                put(JVMConfigurationKeys.OUTPUT_DIRECTORY, File(outputPath))
            })
            AnalysisHandlerExtension.registerExtension(project, extension)
        } else {
            // Use the single-pass implementation, using the new ABI flag in the metadata.
            configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
            val builderExtension = JvmAbiClassBuilderInterceptor()
            val outputExtension = JvmAbiOutputExtension(File(outputPath), builderExtension.abiClassInfo)
            ClassBuilderInterceptorExtension.registerExtension(project, builderExtension)
            ClassFileFactoryFinalizerExtension.registerExtension(project, outputExtension)
        }
    }
}