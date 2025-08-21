/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentUtil
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import java.io.File

/**
 * @param consumeOutput if non-null, jvm-abi-gen does not output any files to the disk, and passes the result to this lambda instead.
 */
class JvmAbiComponentRegistrar(
    private val consumeOutput: ((OutputFileCollection) -> Unit)? = null,
) : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val removeDataClassCopy = configuration.getBoolean(JvmAbiConfigurationKeys.REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE)
        val builderExtension = JvmAbiClassBuilderInterceptor(
            removeDataClassCopy,
            configuration.getBoolean(JvmAbiConfigurationKeys.REMOVE_PRIVATE_CLASSES),
            configuration.getBoolean(JvmAbiConfigurationKeys.TREAT_INTERNAL_AS_PRIVATE),
        )
        val outputExtension = JvmAbiOutputExtension(
            builderExtension::buildAbiClassInfoAndReleaseResources,
            configuration.getBoolean(JvmAbiConfigurationKeys.REMOVE_DEBUG_INFO),
            removeDataClassCopy,
            configuration.getBoolean(JvmAbiConfigurationKeys.PRESERVE_DECLARATION_ORDER),
            configuration.getBoolean(JvmAbiConfigurationKeys.TREAT_INTERNAL_AS_PRIVATE),
        ) { outputFiles ->
            if (consumeOutput != null) {
                consumeOutput(outputFiles)
            } else {
                val outputPath = File(configuration.getNotNull(JvmAbiConfigurationKeys.OUTPUT_PATH))
                if (outputPath.extension == "jar") {
                    // We don't include the runtime or main class in interface jars and always reset time stamps.
                    CompileEnvironmentUtil.writeToJar(outputPath, false, true, true, null, outputFiles, configuration.messageCollector)
                } else {
                    outputFiles.writeAllTo(outputPath)
                }
            }
        }

        ClassGeneratorExtension.registerExtension(builderExtension)
        ClassFileFactoryFinalizerExtension.registerExtension(outputExtension)
    }

    override val supportsK2: Boolean
        get() = true
}
