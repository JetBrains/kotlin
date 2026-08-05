/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Assembles the [SnippetArtifactSidecar] from a just-resolved `FirReplSnippet` and its producing
 * `FirSession`, for embedding into the snippet wrapper class's `.kotlin_metadata` on the write side.
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
 * Builds the sidecar from the frontend alone: [firSnippet]'s `isReplSnippetDeclaration` members
 * (including their visibilities) and the file-level imports from [session] for that snippet.
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
                    // Only functions may share a name within a snippet, so only this kind carries
                    // a non-null descriptor.
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
 * Projects a FIR visibility onto the sidecar's [SnippetArtifactSidecar.MemberRef.Visibility].
 * Anything outside the four well-known visibilities maps to
 * [UNKNOWN][SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN].
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
 * Renders this type into the string carried on `MemberRef.returnTypeSignature`.
 * Returns `null` if the type cannot be derived (for example an unresolved or error type).
 * This is not a JVM descriptor.
 */
private fun FirTypeRef.toRenderableSignature(): String? =
    coneTypeOrNull?.toString()?.takeIf { it.isNotBlank() }

/**
 * Best-effort overload-discriminating signature key carried on
 * [SnippetArtifactSidecar.MemberRef.descriptor]. Computed identically on the write side (live
 * FIR) and the read side (deserialized FIR) so both sides produce the same key for the same
 * overload. Returns `null` for non-function declarations, whose name is already unique.
 *
 * This is not a JVM descriptor. It is a string built from the renderable cone types
 * (`ConeKotlinType.toString()`) of the receiver, context parameters, and value parameters.
 * Unresolved parameter types render as `?`, so overloads that differ only in an unresolvable type
 * cannot be told apart; the read side then falls back to name-only matching.
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

