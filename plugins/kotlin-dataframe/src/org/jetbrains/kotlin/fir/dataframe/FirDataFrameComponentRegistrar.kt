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
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

//val size = 5000
val size = 500

class GeneratedNames {
    val scopes = mutableSetOf<ClassId>()
    val callables = List(size) {
        CallableId(FqName("org.jetbrains.kotlinx.dataframe.api"), Name.identifier("refined_$it"))
    }
//    val tokens = List(size) {
//        ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("Token$it"))
//    }.toMutableSet()

    val tokens = mutableSetOf<ClassId>()
    private val scopeIds = ArrayDeque(List(size) {
        val name = Name.identifier("Scope$it")
        ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")), name)
    })
    private val tokenIds = ArrayDeque(List(size) {
        ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("Token$it"))
    })
    val callableNames = ArrayDeque(callables)

    val scopeState = mutableMapOf<ClassId, SchemaContext>()
    val tokenState = mutableMapOf<ClassId, SchemaContext>()
    val callableState = mutableMapOf<Name, FirSimpleFunction>()

    fun nextName(): ClassId {
        val newId = tokenIds.removeLast()
        tokens.add(newId)
        return newId
    }

    fun nextScope(): ClassId {
        val newId = scopeIds.removeLast()
        scopes.add(newId)
        return newId
    }
}
val PATH: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("annotation qualified name")

// listOf("-P", "plugin:org.jetbrains.kotlinx.dataframe:path=/home/nikitak/IdeaProjects/run-df")
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

class FirDataFrameExtensionRegistrar(val path: String?) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        with(GeneratedNames()) {
            +::FirDataFrameExtensionsGenerator
            +{ it: FirSession ->
                FirDataFrameReceiverInjector(it, scopeState, tokenState, path, this::nextName, this::nextScope)
            }
            +{ it: FirSession -> FirDataFrameAdditionalCheckers(it) }
            +{ it: FirSession -> FirDataFrameCandidateInterceptor(it, callableNames, callableState, this::nextName) }
            +{ it: FirSession -> FirDataFrameTokenGenerator(it, tokens, tokenState) }
        }
    }
}

class FirDataFrameComponentRegistrar : CompilerPluginRegistrar() {

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(FirDataFrameExtensionRegistrar(configuration.get(PATH)))
        IrGenerationExtension.registerExtension(DataFrameIrBodyFiller())
    }

    override val supportsK2: Boolean = true
}
