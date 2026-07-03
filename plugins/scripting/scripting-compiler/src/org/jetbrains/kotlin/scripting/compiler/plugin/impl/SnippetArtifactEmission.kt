/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Emission helper: takes the *just-compiled* `FirReplSnippet` plus the producing `FirSession` and
 * `GenerationState`, and builds a portable [SnippetArtifact] (classfile bytes + an out-of-band
 * [SnippetArtifactHeader]) that the stateless K2 REPL compiler can consume as a prior snippet on a
 * subsequent compile.
 *
 * This file deliberately does **not** touch [K2ReplCompiler] or its `compileImpl`. It is a pure
 * function over the values that the new orchestrator captures from a successful compile.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility as KotlinVisibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
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
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.api.resultField
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.impl._isSyntheticSnippet
import org.jetbrains.kotlin.scripting.compiler.plugin.services.replStateObjectFqName

/**
 * Builds a [SnippetArtifact] from a successful compile.
 *
 * After the "full cut", the returned artifact bundles:
 *  * every JVM class emitted by [generationState] for this snippet (keyed by JVM internal name) —
 *    these already carry the embedded [SnippetArtifactSidecar] in their `.kotlin_metadata` (written
 *    on the stateless path by `Fir2IrReplSnippetConfiguratorExtensionImpl` + `ReplSnippetLowering`),
 *  * a minimal out-of-band [SnippetArtifactHeader] — class id, snippet name, state-object FQ name,
 *    the emitted result-field name, and the `isImplicit` flag.
 *
 * The REPL-only reconstruction payload (declarations + visibilities + imports) is **not** duplicated
 * here; the read path reads it from the embedded sidecar via the located wrapper class.
 *
 * The caller is expected to pass [firSnippet] **from the same compile pass** that produced
 * [generationState] — they are session-local values.
 *
 * @param firSnippet the resolved snippet FIR node.
 * @param generationState the JVM code-gen state of the same compile pass.
 * @param scriptCompilationConfiguration per-call compilation configuration.
 * @param hostConfiguration host configuration; used to record `stateObjectFqName`.
 * @param resultFieldName the **actual** JVM result-field name emitted for this snippet (e.g. `res2`),
 *   as extracted from the generated IR. When `null` the header falls back to the `resultField`
 *   compilation-configuration value; the emitted name is preferred because for REPL snippets it is
 *   `<resultFieldPrefix><snippetId>`, not the `resultField` default (`$$result`).
 */
internal fun buildSnippetArtifactFromCompile(
    firSnippet: FirReplSnippet,
    generationState: GenerationState,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    resultFieldName: String? = null,
): SnippetArtifact {
    val classFiles: Map<String, ByteArray> = generationState.factory.asList()
        .associate { it.relativePath.removeSuffix(".class") to it.asByteArray() }

    // Prefer the *actual* emitted result-field name (e.g. `res2`); only fall back to the
    // `resultField` compilation-configuration default (`$$result`) when the producer could not
    // extract it from the generated IR (e.g. best-effort backend with no IR).
    val resultPropertyName =
        resultFieldName ?: scriptCompilationConfiguration[ScriptCompilationConfiguration.resultField]
    // For the prototype, `isImplicit` mirrors `_isSyntheticSnippet`: today the only known producer
    // of implicit snippets is the `prependSyntheticSnippets` callback (Option D, Q10 umbrella),
    // which sets `_isSyntheticSnippet` on the compilation configuration. The two are kept
    // conceptually separate (see [SnippetArtifactSidecar.isImplicit]) so future producers can mark
    // snippets implicit without touching the compile-side flag.
    val isImplicit = scriptCompilationConfiguration[ScriptCompilationConfiguration.repl._isSyntheticSnippet] == true

    val header = buildReplHeaderFromFir(
        firSnippet = firSnippet,
        hostConfiguration = hostConfiguration,
        resultPropertyName = resultPropertyName,
        isImplicit = isImplicit,
    )

    return header.toArtifact(classFiles)
}

/**
 * Assembles the minimal out-of-band [SnippetArtifactHeader] from the resolved [firSnippet] and the
 * [hostConfiguration] — the few facts a consumer needs without deserializing a class's
 * `.kotlin_metadata` (wrapper class id, snippet name, REPL state-object FQ name, the emitted
 * result-field name, and the `isImplicit` history-provider flag).
 *
 * The bulky reconstruction payload (declarations + visibilities + imports) is **not** assembled here
 * — it is produced by [buildReplSidecarFromFir] and embedded into the wrapper class's
 * `.kotlin_metadata` on the stateless write path.
 */
internal fun buildReplHeaderFromFir(
    firSnippet: FirReplSnippet,
    hostConfiguration: ScriptingHostConfiguration,
    resultPropertyName: String?,
    isImplicit: Boolean,
): SnippetArtifactHeader {
    val snippetClassId = firSnippet.snippetClass.symbol.classId
    val packageFqName = snippetClassId.packageFqName.asString()
    val snippetClassInternalName = run {
        val pkgSlashed = packageFqName.replace('.', '/')
        val relative = snippetClassId.relativeClassName.asString().replace('.', '$')
        if (pkgSlashed.isEmpty()) relative else "$pkgSlashed/$relative"
    }
    val stateObjectFqName = hostConfiguration[ScriptingHostConfiguration.repl.replStateObjectFqName].orEmpty()

    return SnippetArtifactHeader(
        headerVersion = SnippetArtifactHeader.CURRENT_VERSION,
        snippetName = firSnippet.name.asString(),
        snippetClassInternalName = snippetClassInternalName,
        packageFqName = packageFqName,
        stateObjectFqName = stateObjectFqName,
        resultPropertyName = resultPropertyName,
        isImplicit = isImplicit,
    )
}

/**
 * Assembles the [SnippetArtifactSidecar] — the REPL-only reconstruction payload — from the
 * information that is reachable from the **frontend** alone: the resolved [firSnippet] and its
 * producing [session]. After the "full cut" this is exactly what the read path
 * (`ArtifactBackedFirReplHistoryProvider`) sources from the embedded copy: the
 * `isReplSnippetDeclaration` member refs (with their source-level visibilities) and the file-level
 * imports. Everything else a consumer needs is carried out-of-band on the [SnippetArtifactHeader].
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
                    descriptor = null,
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

