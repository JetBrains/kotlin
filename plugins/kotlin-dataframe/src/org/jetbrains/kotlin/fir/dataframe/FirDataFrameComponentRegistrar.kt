/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.extensions.*
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.prefixIfNot

interface IGeneratedNames {
    val scopes: Set<ClassId>
    val callables: Set<CallableId>
    val tokens: Set<ClassId>
    fun nextName(s: String): ClassId
    fun nextScope(s: String): ClassId
}

class Checker(private val generator: IGeneratedNames) : IGeneratedNames by generator {
    override fun nextName(s: String): ClassId {
        val id = generator.nextName(s)
        if (!id.shortClassName.asString().startsWith("Token")) {
            error("there are places that rely on token name to start with Token")
        }
        return id
    }
}

class GeneratedNames : IGeneratedNames {
    override val scopes = mutableSetOf<ClassId>()
    override val callables = mutableSetOf<CallableId>()
    override val tokens = mutableSetOf<ClassId>()

    override fun nextName(s: String): ClassId {
        val s = s.prefixIfNot("Token")
        val newId = ClassId(CallableId.PACKAGE_FQ_NAME_FOR_LOCAL, FqName(s), true)
        tokens.add(newId)
        return newId
    }

    override fun nextScope(s: String): ClassId {
        val newId = ClassId(CallableId.PACKAGE_FQ_NAME_FOR_LOCAL, FqName("${s}Scope"), true)
        scopes.add(newId)
        return newId
    }
}
val PATH: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("annotation qualified name")

// listOf("-P", "plugin:org.jetbrains.kotlinx.dataframe:path=/home/nikita/IdeaProjects/run-df")
class DataFrameCommandLineProcessor : CommandLineProcessor {
    companion object {
        val RESOLUTION_DIRECTORY = CliOption(
            "path", "<path>", "", required = false, allowMultipleOccurrences = false
        )
    }
    override val pluginId: String = "org.jetbrains.kotlinx.dataframe"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(RESOLUTION_DIRECTORY)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        return when (option) {
            RESOLUTION_DIRECTORY -> configuration.put(PATH, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}

class FirDataFrameExtensionRegistrar(
    private val path: String?
) : FirExtensionRegistrar() {
    @OptIn(FirExtensionApiInternals::class)
    override fun ExtensionRegistrarContext.configurePlugin() {
        // if input data schema for refinement is also generated schema, maybe it'll be possible to save names to a set
        val generator = Checker(GeneratedNames())
        +::ExtensionsGenerator
        +::ReturnTypeBasedReceiverInjector
        +{ it: FirSession ->
            CandidateInterceptor(path, it, generator::nextName)
        }
        +::TokenGenerator
    }
}

class FirDataFrameComponentRegistrar : CompilerPluginRegistrar() {

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(FirDataFrameExtensionRegistrar(configuration.get(PATH)))
        IrGenerationExtension.registerExtension(IrBodyFiller())
    }

    override val supportsK2: Boolean = true
}
