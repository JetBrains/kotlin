/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Emission helper: assembles the [SnippetArtifactSidecar] — the REPL-only reconstruction payload —
 * from the *just-resolved* `FirReplSnippet` and its producing `FirSession`, so it can be embedded
 * into the snippet wrapper class's own `.kotlin_metadata` on the write side.
 *
 * This file deliberately does **not** touch [K2ReplCompiler] or its `compileImpl`. It is a pure
 * function over the values `Fir2IrReplSnippetConfiguratorExtensionImpl` captures during a compile.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility as KotlinVisibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeOrNull

/**
 * Assembles the [SnippetArtifactSidecar] — the REPL-only reconstruction payload — from the
 * information that is reachable from the **frontend** alone: the resolved [firSnippet] and its
 * producing [session]. This is exactly what the read path (`ClasspathBackedFirReplHistoryProvider`)
 * sources from the embedded copy: the `isReplSnippetDeclaration` member refs (with their
 * source-level visibilities) and the file-level imports.
 *
 * The sole producer is the `.kotlin_metadata`-embedding write path
 * (`Fir2IrReplSnippetConfiguratorExtensionImpl.prepareSnippet`), which runs *before* code-gen; the
 * encoded bytes are embedded into the wrapper class's `.kotlin_metadata` via the generic
 * `ProtoBuf.CompilerPluginData` channel.
 */
@OptIn(DirectDeclarationsAccess::class)
internal fun buildReplSidecarFromFir(
    firSnippet: FirReplSnippet,
    session: FirSession,
): SnippetArtifactSidecar {
    val declarations = firSnippet.snippetClass.declarations
        .filter { it.isReplSnippetDeclaration == true }
        .mapNotNull { decl ->
            when (decl) {
                is FirProperty -> SnippetArtifactSidecar.MemberRef(
                    kind = SnippetArtifactSidecar.MemberRef.Kind.PROPERTY,
                    name = decl.name.asString(),
                    descriptor = null,
                    visibility = decl.toMemberRefVisibility(),
                    returnTypeSignature = decl.returnTypeRef.toRenderableSignature(),
                )
                is FirNamedFunction -> SnippetArtifactSidecar.MemberRef(
                    kind = SnippetArtifactSidecar.MemberRef.Kind.FUNCTION,
                    name = decl.name.asString(),
                    // Overload discriminator: only functions can share a name within one snippet,
                    // so this is the sole kind that carries a non-null descriptor (see
                    // replMemberOverloadSignature). The read side
                    // (ClasspathBackedFirReplHistoryProvider) uses it to pair each deserialised
                    // overload with the correct MemberRef when re-tagging visibilities.
                    descriptor = replMemberOverloadSignature(decl),
                    visibility = decl.toMemberRefVisibility(),
                    returnTypeSignature = decl.returnTypeRef.toRenderableSignature(),
                )
                is FirRegularClass -> SnippetArtifactSidecar.MemberRef(
                    kind = SnippetArtifactSidecar.MemberRef.Kind.CLASS,
                    name = decl.name.asString(),
                    descriptor = null,
                    visibility = decl.toMemberRefVisibility(),
                    returnTypeSignature = null,
                )
                is FirTypeAlias -> SnippetArtifactSidecar.MemberRef(
                    kind = SnippetArtifactSidecar.MemberRef.Kind.TYPEALIAS,
                    name = decl.name.asString(),
                    descriptor = null,
                    visibility = decl.toMemberRefVisibility(),
                    returnTypeSignature = null,
                )
                else -> null
            }
        }

    val imports = session.firProvider.getFirReplSnippetContainerFile(firSnippet.symbol)?.imports.orEmpty()
        .map { import ->
            SnippetArtifactSidecar.ImportEntry(
                fqName = import.importedFqName?.asString().orEmpty(),
                isAllUnder = import.isAllUnder,
                aliasName = import.aliasName?.asString(),
            )
        }

    return SnippetArtifactSidecar(
        sidecarVersion = SnippetArtifactSidecar.CURRENT_VERSION,
        replSnippetDeclarations = declarations,
        imports = imports,
    )
}

/**
 * Project a FIR [KotlinVisibility][org.jetbrains.kotlin.descriptors.Visibility] onto the small
 * enum carried in the sidecar.
 *
 * Anything outside the four well-known visibilities (i.e. `Local`, `InvisibleFake`,
 * `Inherited`, …) is mapped to [SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN]; the consumer
 * defaults UNKNOWN to PUBLIC, which keeps unrecognised cases from accidentally hiding real
 * declarations from subsequent snippets.
 */
private fun FirMemberDeclaration.toMemberRefVisibility(): SnippetArtifactSidecar.MemberRef.Visibility {
    val v: KotlinVisibility = status.visibility
    return when (v) {
        Visibilities.Public -> SnippetArtifactSidecar.MemberRef.Visibility.PUBLIC
        Visibilities.Internal -> SnippetArtifactSidecar.MemberRef.Visibility.INTERNAL
        Visibilities.Protected -> SnippetArtifactSidecar.MemberRef.Visibility.PROTECTED
        Visibilities.Private,
        Visibilities.PrivateToThis -> SnippetArtifactSidecar.MemberRef.Visibility.PRIVATE
        else -> SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN
    }
}

/**
 * Render a [FirTypeRef] into the renderable string we carry on `MemberRef.returnTypeSignature`.
 *
 * Returns `null` when the type cannot be derived (e.g. unresolved / error type). This is a
 * best-effort prototype signature — it is *not* a JVM descriptor; protobuf promotion will replace
 * it with a structured type descriptor.
 */
private fun FirTypeRef.toRenderableSignature(): String? =
    coneTypeOrNull?.toString()?.takeIf { it.isNotBlank() }

/**
 * A best-effort, overload-discriminating signature key for a REPL-snippet member declaration,
 * carried on [SnippetArtifactSidecar.MemberRef.descriptor] and computed **identically** on the
 * write side (from the just-resolved live FIR) and the read side (`ClasspathBackedFirReplHistoryProvider`,
 * from the prior snippet's deserialised `.kotlin_metadata` FIR).
 *
 * Only functions can share a name within one snippet (overloads); for every other declaration kind
 * the name is already unique, so this returns `null` and the read side falls back to name-only
 * matching for it.
 *
 * The key is **not** a JVM descriptor -- it is a stable string built from the renderable cone types
 * (`ConeKotlinType.toString()`) of the function's extension receiver, context parameters and value
 * parameters. That is all that is needed to tell two same-named overloads apart, and both sides
 * render the same deserialised/resolved types, so the keys match. Value parameters whose type is
 * unresolved render as `?`; two overloads that differ only in an unresolvable type therefore cannot
 * be told apart, which is acceptable for this best-effort re-tagging (the fallback is name-only).
 */
internal fun replMemberOverloadSignature(declaration: FirCallableDeclaration): String? {
    if (declaration !is FirFunction) return null
    fun FirTypeRef.render(): String = coneTypeOrNull?.toString() ?: "?"
    val receiver = declaration.receiverParameter?.typeRef?.render()
    val contextTypes = declaration.contextParameters.map { it.returnTypeRef.render() }
    val valueTypes = declaration.valueParameters.map { it.returnTypeRef.render() }
    return buildString {
        if (receiver != null) append(receiver).append("#")
        if (contextTypes.isNotEmpty()) append(contextTypes.joinToString(",", "[", "]"))
        append("(")
        append(valueTypes.joinToString(","))
        append(")")
    }
}

