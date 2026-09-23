/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.compilerPluginMetadata
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.REPL_SNIPPET_ARTIFACT_PLUGIN_ID
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactMetadata
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactMetadataCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings.REPL_SNIPPET_EVAL_FUN_NAME

@OptIn(SymbolInternals::class)
internal fun readEmbeddedMetadata(classSymbol: FirRegularClassSymbol): SnippetArtifactMetadata? {
    val bytes = classSymbol.fir.compilerPluginMetadata?.get(REPL_SNIPPET_ARTIFACT_PLUGIN_ID) ?: return null
    return try {
        SnippetArtifactMetadataCodec.decode(bytes)
    } catch (t: Throwable) {
        throw IllegalStateException("Cannot decode the REPL snippet metadata embedded into ${classSymbol.classId}", t)
    }
}

@OptIn(SymbolInternals::class)
internal fun readEmbeddedChainLink(classSymbol: FirRegularClassSymbol): String? {
    val bytes = classSymbol.fir.compilerPluginMetadata?.get(REPL_SNIPPET_ARTIFACT_PLUGIN_ID) ?: return null
    return try {
        SnippetArtifactMetadataCodec.decodeChainLinkOnly(bytes)
    } catch (t: Throwable) {
        throw IllegalStateException("Cannot decode the REPL snippet metadata embedded into ${classSymbol.classId}", t)
    }
}

internal fun SnippetArtifactMetadata.MemberRef.Visibility.toFirVisibility(): Visibility? = when (this) {
    SnippetArtifactMetadata.MemberRef.Visibility.PUBLIC -> Visibilities.Public
    SnippetArtifactMetadata.MemberRef.Visibility.INTERNAL -> Visibilities.Internal
    SnippetArtifactMetadata.MemberRef.Visibility.PROTECTED -> Visibilities.Protected
    SnippetArtifactMetadata.MemberRef.Visibility.PRIVATE -> Visibilities.Private
    SnippetArtifactMetadata.MemberRef.Visibility.UNKNOWN -> null
}

@OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
internal fun findEvalSymbol(classSymbol: FirRegularClassSymbol): FirNamedFunctionSymbol? {
    for (decl in classSymbol.fir.declarations) {
        if (decl is FirNamedFunction && decl.name.asString() == "\$\$eval") {
            return decl.symbol
        }
    }
    return null
}

@OptIn(FirImplementationDetail::class)
internal class ReconstructedFirReplSnippet(
    snippetName: String,
    snippetModuleData: FirModuleData,
    snippetClassFir: FirRegularClass,
    snippetSymbol: FirReplSnippetSymbol,
    evalSymbol: FirNamedFunctionSymbol?,
) : FirReplSnippet() {
    override val annotations: List<FirAnnotation> = emptyList()
    override val moduleData: FirModuleData = snippetModuleData
    override val origin: FirDeclarationOrigin = FirDeclarationOrigin.Library
    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override val name: Name = Name.identifier(snippetName)
    override val symbol: FirReplSnippetSymbol = snippetSymbol
    override val source: KtSourceElement
        get() = throw UnsupportedOperationException(
            "ReconstructedFirReplSnippet has no source — reading source on a previous REPL snippet stub is not supported"
        )
    override val receivers: List<FirScriptReceiverParameter> = emptyList()
    override var snippetClass: FirRegularClass = snippetClassFir
    override val evalFunctionSymbol: FirNamedFunctionSymbol =
        evalSymbol ?: FirNamedFunctionSymbol(CallableId(snippetClassFir.symbol.classId, REPL_SNIPPET_EVAL_FUN_NAME))

    init {
        symbol.bind(this)
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {}

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this
    override fun <D> transformReceivers(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this
    override fun <D> transformSnippetClass(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this
}
