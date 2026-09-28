/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptorRepresentation
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.name.ClassId

@OptIn(DirectDeclarationsAccess::class)
internal fun buildSnippetArtifactMetadataFromFir(
    firSnippet: FirReplSnippet,
    session: FirSession,
    priorSnippetClassId: ClassId?,
    serializedCompilationConfiguration: ByteArray?,
): SnippetArtifactMetadata {
    val declarations = firSnippet.snippetClass.declarations
        .filter { it.isReplSnippetDeclaration == true }
        .mapNotNull { decl ->
            when (decl) {
                is FirProperty -> SnippetArtifactMetadata.MemberRef(
                    kind = SnippetArtifactMetadata.MemberRef.Kind.PROPERTY,
                    name = decl.name.asString(),
                    signature = null,
                    visibility = decl.toMemberRefVisibility(),
                )
                is FirNamedFunction -> SnippetArtifactMetadata.MemberRef(
                    kind = SnippetArtifactMetadata.MemberRef.Kind.FUNCTION,
                    name = decl.name.asString(),
                    // Only functions may share a name within a snippet
                    signature = replMemberOverloadSignature(decl),
                    visibility = decl.toMemberRefVisibility(),
                )
                is FirRegularClass -> SnippetArtifactMetadata.MemberRef(
                    kind = SnippetArtifactMetadata.MemberRef.Kind.CLASS,
                    name = decl.name.asString(),
                    signature = null,
                    visibility = decl.toMemberRefVisibility(),
                )
                is FirTypeAlias -> SnippetArtifactMetadata.MemberRef(
                    kind = SnippetArtifactMetadata.MemberRef.Kind.TYPEALIAS,
                    name = decl.name.asString(),
                    signature = null,
                    visibility = decl.toMemberRefVisibility(),
                )
                else -> null
            }
        }

    val imports = session.firProvider.getFirReplSnippetContainerFile(firSnippet.symbol)?.imports.orEmpty()
        .map { import ->
            SnippetArtifactMetadata.ImportEntry(
                fqName = import.importedFqName?.asString().orEmpty(),
                isAllUnder = import.isAllUnder,
                aliasName = import.aliasName?.asString(),
            )
        }

    return SnippetArtifactMetadata(
        version = SnippetArtifactMetadata.CURRENT_VERSION,
        priorSnippetClassId = priorSnippetClassId?.asString(),
        replSnippetDeclarations = declarations,
        imports = imports,
        serializedCompilationConfiguration = serializedCompilationConfiguration,
    )
}

private fun FirMemberDeclaration.toMemberRefVisibility(): SnippetArtifactMetadata.MemberRef.Visibility {
    val v: KotlinVisibility = status.visibility
    return when (v) {
        Visibilities.Public -> SnippetArtifactMetadata.MemberRef.Visibility.PUBLIC
        Visibilities.Internal -> SnippetArtifactMetadata.MemberRef.Visibility.INTERNAL
        Visibilities.Protected -> SnippetArtifactMetadata.MemberRef.Visibility.PROTECTED
        Visibilities.Private,
        Visibilities.PrivateToThis -> SnippetArtifactMetadata.MemberRef.Visibility.PRIVATE
        else -> SnippetArtifactMetadata.MemberRef.Visibility.UNKNOWN
    }
}

/**
 * Signature for snippet function declarations for best-effort overload selection.
 */
internal fun replMemberOverloadSignature(declaration: FirCallableDeclaration): String? {
    if (declaration !is FirFunction) return null
    fun FirTypeRef.render(): String = coneTypeOrNull?.computeJvmDescriptorRepresentation() ?: "?"
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

