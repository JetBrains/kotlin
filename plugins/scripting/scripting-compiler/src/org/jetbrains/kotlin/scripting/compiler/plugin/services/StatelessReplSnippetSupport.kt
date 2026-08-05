/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirScriptReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.compilerPluginMetadata
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.REPL_SIDECAR_PLUGIN_ID
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecarProtoCodec

/**
 * Reconstruction helpers for the stateless [ClasspathBackedFirReplHistoryProvider].
 *
 * These turn a previous snippet's wrapper [FirRegularClassSymbol] plus its embedded
 * [SnippetArtifactSidecar] into a [FirReplSnippetSymbol] view usable by the FIR REPL-snippet
 * resolve and codegen extensions.
 */

private val STATELESS_REPL_DEBUG_ENABLED: Boolean =
    System.getProperty("kotlin.scripting.repl.stateless.debug") == "true"

internal fun statelessReplDebug(message: String) {
    if (STATELESS_REPL_DEBUG_ENABLED) System.err.println("[STATELESS_REPL] $message")
}

/**
 * Decodes the [SnippetArtifactSidecar] embedded in [classSymbol]'s `.kotlin_metadata` via the
 * generic `ProtoBuf.CompilerPluginData` channel (keyed by [REPL_SIDECAR_PLUGIN_ID]).
 *
 * Returns `null` if the previous snippet's wrapper class carries no such payload, for example
 * when it was produced by a compiler version that predates the metadata-embedding write side.
 */
@OptIn(SymbolInternals::class)
internal fun readEmbeddedSidecar(classSymbol: FirRegularClassSymbol): SnippetArtifactSidecar? {
    val bytes = classSymbol.fir.compilerPluginMetadata?.get(REPL_SIDECAR_PLUGIN_ID) ?: return null
    return try {
        SnippetArtifactSidecarProtoCodec.decode(bytes)
    } catch (t: Throwable) {
        statelessReplDebug("readEmbeddedSidecar: failed to decode embedded sidecar for ${classSymbol.classId}: ${t.message}")
        null
    }
}

/**
 * Maps a sidecar [SnippetArtifactSidecar.MemberRef.Visibility] back to a FIR [Visibility].
 * Returns `null` for [SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN], meaning "no opinion,
 * leave the materialized declaration's visibility alone".
 */
internal fun SnippetArtifactSidecar.MemberRef.Visibility.toFirVisibility(): Visibility? = when (this) {
    SnippetArtifactSidecar.MemberRef.Visibility.PUBLIC -> Visibilities.Public
    SnippetArtifactSidecar.MemberRef.Visibility.INTERNAL -> Visibilities.Internal
    SnippetArtifactSidecar.MemberRef.Visibility.PROTECTED -> Visibilities.Protected
    SnippetArtifactSidecar.MemberRef.Visibility.PRIVATE -> Visibilities.Private
    SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN -> null
}

/**
 * Restamps the visibility on a deserialized REPL-snippet member with [newVisibility], preserving
 * the existing modality and status flags. The result reflects the source-level visibility rather
 * than the `public` access the member is JVM-emitted with (see [SnippetArtifactSidecar.MemberRef]).
 */
internal fun restampVisibility(
    fir: FirMemberDeclaration,
    newVisibility: Visibility,
    ownerSymbol: FirRegularClassSymbol,
) {
    val current = fir.status
    val modality = current.modality ?: Modality.FINAL
    val forClass = fir is FirRegularClass || fir is FirTypeAlias
    val newEffective = newVisibility.toEffectiveVisibility(ownerSymbol, forClass = forClass)
    val newStatus = (current as? FirDeclarationStatusImpl)
        ?.resolved(newVisibility, modality, newEffective)
        ?: FirResolvedDeclarationStatusImpl(newVisibility, modality, newEffective)
    fir.replaceStatus(newStatus)
}

/** Looks up `$$eval` on the deserialized wrapper class, if present. */
@OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
internal fun findEvalSymbol(classSymbol: FirRegularClassSymbol): FirNamedFunctionSymbol? {
    for (decl in classSymbol.fir.declarations) {
        if (decl is FirNamedFunction && decl.name.asString() == "\$\$eval") {
            return decl.symbol
        }
    }
    return null
}

/**
 * A minimal stub of [FirReplSnippet] sufficient for the resolve-extension code path that reads
 * `symbol.moduleData` and `symbol.snippetClassSymbol.declarationSymbols`.
 *
 * All other fields are unused by previous-snippet consumers in the current pipeline.
 * [source] throws if anyone reads it.
 */
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
        evalSymbol ?: FirNamedFunctionSymbol(CallableId(snippetClassFir.symbol.classId, Name.identifier("\$\$eval")))

    init {
        symbol.bind(this)
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        // no-op stub
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this
    override fun <D> transformReceivers(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this
    override fun <D> transformSnippetClass(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this
}
