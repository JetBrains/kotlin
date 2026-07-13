/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationConfigurationKeys.SERIALIZATION_DISABLE_INTRINSIC
import org.jetbrains.kotlinx.serialization.compiler.fir.FirSerializationExtensionRegistrar

object SerializationPluginNames {
    const val PLUGIN_ID = "org.jetbrains.kotlinx.serialization"
}

object SerializationConfigurationKeys {
    // Disable replacement of serializer<T>() call with direct serializer retrieval.
    val SERIALIZATION_DISABLE_INTRINSIC: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("SERIALIZATION_DISABLE_INTRINSIC")
}

class SerializationPluginOptions : CommandLineProcessor {
    companion object {
        val DISABLE_INTRINSIC_OPTION = CliOption(
            "disableIntrinsic", "true/false",
            "Disable replacement of serializer<T>() call with direct serializer retrieval. Use if you experience errors during inlining.",
            required = false, allowMultipleOccurrences = false
        )
    }

    override val pluginId get() = SerializationPluginNames.PLUGIN_ID
    override val pluginOptions = listOf(DISABLE_INTRINSIC_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        DISABLE_INTRINSIC_OPTION -> configuration.put(SERIALIZATION_DISABLE_INTRINSIC, value == "true")
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}


class SerializationComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        Companion.registerExtensions(this, loadDisableIntrinsic(configuration))
    }

    private fun loadDisableIntrinsic(configuration: CompilerConfiguration) =
        if (configuration.get(SERIALIZATION_DISABLE_INTRINSIC) == true) SerializationIntrinsicsState.DISABLED else SerializationIntrinsicsState.NORMAL

    override val pluginId: String get() = SerializationPluginNames.PLUGIN_ID

    override val supportsK2: Boolean
        get() = true

    companion object {
        fun registerExtensions(extensionStorage: ExtensionStorage, intrinsicsState: SerializationIntrinsicsState = SerializationIntrinsicsState.NORMAL) = with(extensionStorage) {
            IrGenerationExtension.registerExtension(SerializationLoweringExtension(intrinsicsState))
            FirExtensionRegistrar.registerExtension(FirSerializationExtensionRegistrar())
        }
    }
}
