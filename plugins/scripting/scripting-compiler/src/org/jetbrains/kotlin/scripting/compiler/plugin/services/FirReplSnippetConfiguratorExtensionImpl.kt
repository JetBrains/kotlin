/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.scripting.compiler.plugin.services

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.FirReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirReplSnippetBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildScriptReceiverParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.Name.identifier
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.currentLineId
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

        @Suppress("DuplicatedCode")
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

        /*
         If the last statement in a snippet is a valid expression, we store the value in a generated
         result field in the snippet class before returning the value.
         E.g.
         ```
         "Hello"
         ```
         Gets turned into
         ```
         val `$res0` = "Hello"
         `$res0`
         ```
         */
        configuration[ScriptCompilationConfiguration.repl.resultFieldPrefix]?.takeIf { it.isNotBlank() }?.let { resultPrefix ->
            val lastExpression = body.statements.lastOrNull().takeIf {
                it is FirExpression && it !is FirErrorExpression
            } as? FirExpression

            if (lastExpression != null) {
                val snippetResultPropertyName = resultFieldName(configuration, resultPrefix)
                if (snippetResultPropertyName == null) return

                @OptIn(UnresolvedExpressionTypeAccess::class)
                val lastExpressionTypeRef =
                    lastExpression.takeUnless { it is FirLazyExpression }?.coneTypeOrNull?.toFirResolvedTypeRef()
                        ?: FirImplicitTypeRefImplWithoutSource
                val updatedStatements = body.statements.toMutableList()
                val saveReturnValueStatement = buildProperty {
                    this.name = snippetResultPropertyName
                    this.symbol = FirPropertySymbol(CallableId(context.packageFqName, this.name))
                    source = null
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.ScriptCustomization.ResultProperty
                    initializer = lastExpression
                    returnTypeRef = lastExpressionTypeRef
                    status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
                    isLocal = true // Must be marked local to be lowered correctly
                    isVar = false
                }.also {
                    this.resultFieldName = it.name
                }

                val useResultPropertyAsReturnValueExpression = buildPropertyAccessExpression {
                    calleeReference = buildResolvedNamedReference {
                        name = snippetResultPropertyName
                        resolvedSymbol = saveReturnValueStatement.symbol
                        source = null
                    }
                }
                updatedStatements.add(updatedStatements.lastIndex, saveReturnValueStatement)
                updatedStatements[updatedStatements.lastIndex] = useResultPropertyAsReturnValueExpression

                // For now, the FirReplSnippetBuilder API does not allow us to modify
                // the body directly, so instead we copy it while inserting the new result
                // property just before the last expression.
                val updatedBody = FirBlockBuilder().run {
                    val originalBody = this@configure.body
                    source = originalBody.source
                    @OptIn(UnresolvedExpressionTypeAccess::class)
                    coneTypeOrNull = originalBody.coneTypeOrNull
                    annotations.addAll(originalBody.annotations)
                    statements.addAll(updatedStatements)
                    build()
                }
                body = updatedBody
            }
        }
    }

    /**
     * Calculate the name of the return field or `null` if saving return values are disabled.
     *
     * For REPL snippets, we use a combination of [ReplScriptCompilationConfigurationKeys.resultFieldPrefix]
     * and the snippet number which must be provided through [ReplScriptCompilationConfigurationKeys.currentLineId]
     * in order to generate the result field. This also mean we ignore [ScriptCompilationConfigurationKeys.resultField]
     */
    private fun resultFieldName(configuration: ScriptCompilationConfiguration, prefix: String): Name? {
        val replSnippetNo = configuration[ScriptCompilationConfiguration.repl.currentLineId]?.no
        return if (replSnippetNo != null) {
            identifier("$prefix$replSnippetNo")
        } else {
            null
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