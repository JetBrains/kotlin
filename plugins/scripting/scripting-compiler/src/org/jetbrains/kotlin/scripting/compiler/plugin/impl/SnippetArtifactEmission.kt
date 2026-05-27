/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Emission helper: takes the *just-compiled* `FirReplSnippet` plus the producing `FirSession` and
 * `GenerationState`, and builds a portable [SnippetArtifact] (classfile bytes + JSON sidecar) that
 * the stateless K2 REPL compiler can consume as a prior snippet on a subsequent compile.
 *
 * This file deliberately does **not** touch [K2ReplCompiler] or its `compileImpl`. It is a pure
 * function over the values that the new orchestrator captures from a successful compile.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.api.resultField
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.impl._isSyntheticSnippet
import org.jetbrains.kotlin.scripting.compiler.plugin.services.replStateObjectFqName

/**
 * Builds a [SnippetArtifact] from a successful compile.
 *
 * The returned artifact bundles:
 *  * every JVM class emitted by [generationState] for this snippet (keyed by JVM internal name),
 *  * a JSON-encoded [SnippetArtifactSidecar] capturing the REPL-only information that is not
 *    preserved by `.kotlin_metadata` (most importantly the `isReplSnippetDeclaration` markers,
 *    file-level imports, and the REPL state-object FQ name).
 *
 * The caller is expected to pass [firSnippet] and [session] **from the same compile pass** that
 * produced [generationState] — they are session-local values.
 *
 * @param firSnippet the resolved snippet FIR node.
 * @param session the FIR session that produced [firSnippet].
 * @param generationState the JVM code-gen state of the same compile pass.
 * @param scriptCompilationConfiguration per-call compilation configuration.
 * @param hostConfiguration host configuration; used to record `stateObjectFqName`.
 * @param historyIndex the position of this snippet in the REPL history (0-based; should equal
 *   `priorSnippets.size` at the time of compile).
 */
internal fun buildSnippetArtifactFromCompile(
    firSnippet: FirReplSnippet,
    session: FirSession,
    generationState: GenerationState,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    historyIndex: Int,
): SnippetArtifact {
    val classFiles: Map<String, ByteArray> = generationState.factory.asList()
        .associate { it.relativePath.removeSuffix(".class") to it.asByteArray() }

    val sidecar = buildSidecar(
        firSnippet = firSnippet,
        session = session,
        scriptCompilationConfiguration = scriptCompilationConfiguration,
        hostConfiguration = hostConfiguration,
        historyIndex = historyIndex,
    )

    return sidecar.toArtifact(classFiles)
}

@OptIn(DirectDeclarationsAccess::class)
private fun buildSidecar(
    firSnippet: FirReplSnippet,
    session: FirSession,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    historyIndex: Int,
): SnippetArtifactSidecar {
    val snippetClassId = firSnippet.snippetClass.symbol.classId
    val packageFqName = snippetClassId.packageFqName.asString()
    val snippetClassInternalName = run {
        val pkgSlashed = packageFqName.replace('.', '/')
        val relative = snippetClassId.relativeClassName.asString().replace('.', '$')
        if (pkgSlashed.isEmpty()) relative else "$pkgSlashed/$relative"
    }

    val declarations = firSnippet.snippetClass.declarations
        .filter { it.isReplSnippetDeclaration == true }
        .mapNotNull { decl ->
            when (decl) {
                is FirProperty -> SnippetArtifactSidecar.MemberRef(
                    SnippetArtifactSidecar.MemberRef.Kind.PROPERTY, decl.name.asString(), descriptor = null,
                )
                is FirNamedFunction -> SnippetArtifactSidecar.MemberRef(
                    SnippetArtifactSidecar.MemberRef.Kind.FUNCTION, decl.name.asString(), descriptor = null,
                )
                is FirRegularClass -> SnippetArtifactSidecar.MemberRef(
                    SnippetArtifactSidecar.MemberRef.Kind.CLASS, decl.name.asString(), descriptor = null,
                )
                is FirTypeAlias -> SnippetArtifactSidecar.MemberRef(
                    SnippetArtifactSidecar.MemberRef.Kind.TYPEALIAS, decl.name.asString(), descriptor = null,
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

    val stateObjectFqName = hostConfiguration[ScriptingHostConfiguration.repl.replStateObjectFqName].orEmpty()
    val resultPropertyName = scriptCompilationConfiguration[ScriptCompilationConfiguration.resultField]
    val isSynthetic = scriptCompilationConfiguration[ScriptCompilationConfiguration.repl._isSyntheticSnippet] == true

    return SnippetArtifactSidecar(
        sidecarVersion = SnippetArtifactSidecar.CURRENT_VERSION,
        snippetName = firSnippet.name.asString(),
        snippetClassInternalName = snippetClassInternalName,
        packageFqName = packageFqName,
        historyIndex = historyIndex,
        replSnippetDeclarations = declarations,
        imports = imports,
        stateObjectFqName = stateObjectFqName,
        resultPropertyName = resultPropertyName,
        isSynthetic = isSynthetic,
    )
}

/**
 * Returns the [FirReplHistoryProvider]-derived "current history size" for the given host
 * configuration, or `0` if no provider is installed. Convenience: lets the orchestrator derive
 * `historyIndex` without poking at the host config every time.
 */
internal fun ScriptingHostConfiguration.firReplHistorySizeOrZero(): Int =
    this[ScriptingHostConfiguration.repl.firReplHistoryProvider]?.getSnippetCount() ?: 0

