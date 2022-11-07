/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.extensions.*
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class GeneratedNames {
    val scopes = List(500) {
        val name = Name.identifier("Scope$it")
        ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")), name)
    }.toSet()
    val callables = List(100) {
        CallableId(FqName("org.jetbrains.kotlinx.dataframe.api"), Name.identifier("refined_$it"))
    }
    val tokens = List(500) {
        ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("Token$it"))
    }.toSet()
    val scopeIds = ArrayDeque(scopes)
    val tokenIds = ArrayDeque(tokens)
    val callableNames = ArrayDeque(callables)

    val scopeState = mutableMapOf<ClassId, SchemaContext>()
    val tokenState = mutableMapOf<ClassId, SchemaContext>()
    val callableState = mutableMapOf<Name, FirSimpleFunction>()
}

class FirDataFrameExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        with(GeneratedNames()) {
            +{ it: FirSession -> FirDataFrameExtensionsGenerator(it, scopes, scopeState, callables, callableState) }
            +{ it: FirSession -> FirDataFrameReceiverInjector(it, scopeState, scopeIds, tokenState, tokenIds) }
            +{ it: FirSession -> FirDataFrameAdditionalCheckers(it, tokenState) }
            +{ it: FirSession -> FirDataFrameCandidateInterceptor(it, callableNames, tokenIds, callableState) }
            +{ it: FirSession -> FirDataFrameTokenGenerator(it, tokens, tokenState) }
        }
    }
}

class FirDataFrameComponentRegistrar : CompilerPluginRegistrar() {

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(FirDataFrameExtensionRegistrar())
        IrGenerationExtension.registerExtension(DataFrameIrBodyFiller())
    }

    override val supportsK2: Boolean = true
}
