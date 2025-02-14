/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.FirReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirReplSnippetBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildScriptReceiverParameter
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.util.PropertiesCollection

/**
 * The interface to an evaluation context getter
 */
typealias IsReplSnippetSourcePredicate = (KtSourceFile?, KtSourceElement) -> Boolean

/**
 * Predicate to use to distinguish REPL snippets from regular scripts. Not optional - should be provided by the REPL implementation
 */
val ReplScriptingHostConfigurationKeys.isReplSnippetSource by PropertiesCollection.key<IsReplSnippetSourcePredicate>(isTransient = true)

class FirReplSnippetConfiguratorExtensionImpl(
    session: FirSession,
    private val hostConfiguration: ScriptingHostConfiguration,
) : FirReplSnippetConfiguratorExtension(session) {

    override fun isReplSnippetsSource(sourceFile: KtSourceFile?, scriptSource: KtSourceElement): Boolean =
        hostConfiguration[ScriptingHostConfiguration.repl.isReplSnippetSource]?.invoke(sourceFile, scriptSource) ?: false

    override fun FirReplSnippetBuilder.configureContainingFile(fileBuilder: FirFileBuilder) {
    }

    override fun FirReplSnippetBuilder.configure(sourceFile: KtSourceFile?, context: Context<PsiElement>) {
        val configuration = getOrLoadConfiguration(session, sourceFile!!) ?: run {
            // TODO: add error or log, if necessary (see implementation for scripts) (KT-74742)
            return
        }

        configuration[ScriptCompilationConfiguration.implicitReceivers]?.forEach { implicitReceiver ->
            receivers.add(
                buildScriptReceiverParameter {
                    typeRef = this@configure.tryResolveOrBuildParameterTypeRefFromKotlinType(implicitReceiver)
                    isBaseClassReceiver = false
                    symbol = FirReceiverParameterSymbol()
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.ScriptCustomization.Parameter
                    containingDeclarationSymbol = this@configure.symbol
                }
            )
        }
    }

    // TODO: deduplicate with the very similar code in the script configurator (KT-74741)
    private fun FirReplSnippetBuilder.tryResolveOrBuildParameterTypeRefFromKotlinType(
        kotlinType: KotlinType,
        sourceElement: KtSourceElement = source.fakeElement(KtFakeSourceElementKind.ScriptParameter),
    ): FirTypeRef {
        // TODO: check/support generics and other cases (KT-72638)
        // such a conversion by simple splitting by a '.', is overly simple and does not support all cases, e.g. generics or backticks
        // but to support it properly, one may need to reimplement or reuse types paring code
        // but since such cases a considered exotic, it is not implemented yet.
        val fqName = FqName.fromSegments(kotlinType.typeName.split("."))
        val classId = ClassId(fqName.parent(), fqName.shortName())
        val classFromDeps = session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId)
        return if (classFromDeps != null) {
            buildResolvedTypeRef {
                source = sourceElement
                coneType = classFromDeps.constructType(isMarkedNullable = kotlinType.isNullable)
            }
        } else {
            buildUserTypeRef {
                source = sourceElement
                isMarkedNullable = kotlinType.isNullable
                qualifier.addAll(
                    fqName.pathSegments().map {
                        FirQualifierPartImpl(null, it, FirTypeArgumentListImpl(null))
                    }
                )
            }
        }
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirReplSnippetConfiguratorExtensionImpl(session, hostConfiguration) }
        }
    }
}