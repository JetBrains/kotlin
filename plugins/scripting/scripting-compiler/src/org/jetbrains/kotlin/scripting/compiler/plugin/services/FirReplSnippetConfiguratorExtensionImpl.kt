/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.FirReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.copyWithNewSourceKind
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirReplSnippetBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildScriptReceiverParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
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
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.scripting.definitions.ScriptPriorities
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
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

    override fun FirReplSnippetBuilder.configure(sourceFile: KtSourceFile?, context: Context<*>) {
        val configuration = getOrLoadConfiguration(session, sourceFile!!)?.valueOrNull() ?: run {
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

    override fun MutableList<FirElement>.configure(sourceFile: KtSourceFile?, scriptSource: KtSourceElement, context: Context<*>) {
        val configuration = getOrLoadConfiguration(session, sourceFile!!)?.valueOrNull() ?: run {
            // TODO: add error or log, if necessary (see implementation for scripts) (KT-74742)
            return
        }

        val script = scriptSource.psi as? KtScript
        val replSnippetId = script?.getUserData(ScriptPriorities.PRIORITY_KEY)?.toString()
        val resultFieldName = if (replSnippetId != null) {
            configuration[ScriptCompilationConfiguration.repl.resultFieldPrefix]
                ?.takeIf { it.isNotBlank() }?.let { "$it$replSnippetId" }
        } else {
            configuration[ScriptCompilationConfiguration.resultField]
                ?.takeIf { it.isNotBlank() }
        }

        if (resultFieldName == null) return

        val last = lastOrNull()
        if (last == null || !last.isExpression()) return

        val callableId = CallableId(context.packageFqName, Name.identifier(resultFieldName))
        val propertySymbol = FirRegularPropertySymbol(callableId)
        val propertyReturnType = FirImplicitTypeRefImplWithoutSource

        val property = buildProperty {
            source = last.source?.fakeElement(KtFakeSourceElementKind.ReplResultField)
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.ScriptCustomization.ResultProperty
            returnTypeRef = propertyReturnType
            name = callableId.callableName
            isVar = false
            status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
            symbol = propertySymbol
            dispatchReceiverType = context.dispatchReceiverTypesStack.lastOrNull()
            isLocal = false

            initializer = last

            backingField = FirDefaultPropertyBackingField(
                moduleData = session.moduleData,
                origin = origin,
                source = null,
                annotations = annotations,
                returnTypeRef = returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                isVar = isVar,
                propertySymbol = symbol,
                status = status,
            )

            getter = FirDefaultPropertyAccessor.createGetterOrSetter(
                source = null,
                session.moduleData,
                origin,
                propertyReturnType.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef),
                status.visibility,
                propertySymbol,
                isGetter = true,
            )
        }

        this[this.lastIndex] = property
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

@OptIn(ExperimentalContracts::class)
private fun FirElement.isExpression(): Boolean {
    contract { returns(true) implies (this@isExpression is FirExpression) }
    return this is FirExpression && (this !is FirBlock || this.statements.lastOrNull()?.isExpression() == true)
}