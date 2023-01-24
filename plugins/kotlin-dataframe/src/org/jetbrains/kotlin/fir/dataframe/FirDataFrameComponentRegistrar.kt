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

val size = 5000
//val size = 500

interface IGeneratedNames {
    val scopes: Set<ClassId>
    val callables: Set<CallableId>
    val tokens: Set<ClassId>
    val scopeState: MutableMap<ClassId, SchemaContext>
    val tokenState: MutableMap<ClassId, SchemaContext>
    val callableState: MutableMap<Name, FirSimpleFunction>
    fun nextName(s: String): ClassId
    fun nextScope(s: String): ClassId
    fun nextFunction(s: String): CallableId
}

class PredefinedNames : IGeneratedNames {

    override val scopeState = mutableMapOf<ClassId, SchemaContext>()
    override val tokenState = mutableMapOf<ClassId, SchemaContext>()
    override val callableState = mutableMapOf<Name, FirSimpleFunction>()

    private val _tokens = List(size) {
        ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("Token$it"))
    }
    override val tokens: MutableSet<ClassId> = _tokens.toMutableSet()
    private val t = ArrayDeque(_tokens)
    override fun nextName(s: String): ClassId {
        val token = t.removeLast()
        tokens.add(token)
        return token
    }

    private val _callables = List(size) {
        CallableId(FqName("org.jetbrains.kotlinx.dataframe.api"), Name.identifier("refined_$it"))
    }
    override val callables: MutableSet<CallableId> = _callables.toMutableSet()
    private val c = ArrayDeque(_callables)
    override fun nextFunction(s: String): CallableId {
        val name = c.removeLast()
        callables.add(name)
        return name
    }

    private val _scopes = List(size) {
        ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("Scope$it"))
    }
    override val scopes: MutableSet<ClassId> = _scopes.toMutableSet()
    private val s = ArrayDeque(_scopes)
    override fun nextScope(s: String): ClassId {
        val scope = this.s.removeLast()
        scopes.add(scope)
        return scope
    }

}

class GeneratedNames : IGeneratedNames {
    override val scopes = mutableSetOf<ClassId>()
    override val callables = mutableSetOf<CallableId>()
//    val tokens = List(size) {
//        ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("Token$it"))
//    }.toMutableSet()

    override val tokens = mutableSetOf<ClassId>()
//    val callableNames = ArrayDeque(List(size) {
//        CallableId(FqName("org.jetbrains.kotlinx.dataframe.api"), Name.identifier("refined_$it"))
//    })

    override val scopeState = mutableMapOf<ClassId, SchemaContext>()
    override val tokenState = mutableMapOf<ClassId, SchemaContext>()
    override val callableState = mutableMapOf<Name, FirSimpleFunction>()

    override fun nextName(s: String): ClassId {
        val newId = ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier(s))
        tokens.add(newId)
        return newId
    }

    override fun nextScope(s: String): ClassId {
        val newId = ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("${s}Scope"))
        scopes.add(newId)
        return newId
    }

    override fun nextFunction(s: String): CallableId {
        val callableId = CallableId(FqName("org.jetbrains.kotlinx.dataframe.api"), Name.identifier(s))
        callables.add(callableId)
        return callableId
//        val callableId = callableNames.removeLast()
//        callables.add(callableId)
//        return callableId
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
        val flag = true
        val generator = if (flag) PredefinedNames() else GeneratedNames()
        with(generator) {
            +::FirDataFrameExtensionsGenerator
            +{ it: FirSession ->
                FirDataFrameReceiverInjector(it, scopeState, tokenState, path, this::nextName, this::nextScope)
            }
            +{ it: FirSession -> FirDataFrameAdditionalCheckers(it) }
            +{ it: FirSession -> FirDataFrameCandidateInterceptor(it, ::nextFunction, callableState, this::nextName) }
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
