/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class JvmAbiComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val outputPath = configuration.getNotNull(JvmAbiConfigurationKeys.OUTPUT_PATH)
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        if (configuration.get(JvmAbiConfigurationKeys.LEGACY_ABI_GEN, false)) {
            if (configuration.getBoolean(CommonConfigurationKeys.USE_FIR)) {
                messageCollector.report(CompilerMessageSeverity.ERROR, "Legacy jvm-abi-gen does not support K2 compiler.")
            } else {
                messageCollector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Legacy jvm-abi-gen is deprecated and will be removed in a future version. " +
                            "Please migrate to the new jvm-abi-gen implementation."
                )
            }
            // Use the two-pass implementation
            if (outputPath.endsWith(".jar")) {
                messageCollector.report(CompilerMessageSeverity.ERROR, "Legacy jvm-abi-gen does not support jar output.")
            }
            val extension = JvmAbiAnalysisHandlerExtension(configuration.copy().apply {
                put(JVMConfigurationKeys.OUTPUT_DIRECTORY, File(outputPath))
            })
            AnalysisHandlerExtension.registerExtension(extension)
        } else {
            // Use the single-pass implementation, using the new ABI flag in the metadata.
            configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
            val builderExtension = JvmAbiClassBuilderInterceptor()
            val outputExtension = JvmAbiOutputExtension(File(outputPath), builderExtension.abiClassInfo, messageCollector)
            ClassBuilderInterceptorExtension.registerExtension( builderExtension)
            ClassFileFactoryFinalizerExtension.registerExtension( outputExtension)
        }
    }

    override val supportsK2: Boolean
        get() = true
}
