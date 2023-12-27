/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import java.io.File

class JvmAbiComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val outputPath = configuration.getNotNull(JvmAbiConfigurationKeys.OUTPUT_PATH)
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
        val removeDataClassCopy = configuration.getBoolean(JvmAbiConfigurationKeys.REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE)
        val builderExtension = JvmAbiClassBuilderInterceptor(
            removeDataClassCopy,
            configuration.getBoolean(JvmAbiConfigurationKeys.REMOVE_PRIVATE_CLASSES),
        )
        val outputExtension = JvmAbiOutputExtension(
            File(outputPath), builderExtension::buildAbiClassInfoAndReleaseResources,
            messageCollector, configuration.getBoolean(JvmAbiConfigurationKeys.REMOVE_DEBUG_INFO),
            removeDataClassCopy,
            configuration.getBoolean(JvmAbiConfigurationKeys.PRESERVE_DECLARATION_ORDER),
        )

        ClassGeneratorExtension.registerExtension(builderExtension)
        ClassFileFactoryFinalizerExtension.registerExtension(outputExtension)
    }

    override val supportsK2: Boolean
        get() = true
}
