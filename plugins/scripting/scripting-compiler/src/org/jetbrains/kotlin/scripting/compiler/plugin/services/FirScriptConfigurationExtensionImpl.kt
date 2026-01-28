/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.FirScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.definitions.annotationsForSamWithReceivers
import org.jetbrains.kotlin.scripting.resolve.toSourceCode
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration


class FirScriptConfiguratorExtensionImpl(
    session: FirSession,
) : FirScriptConfiguratorExtension(session) {

    override fun FirScriptBuilder.configureContainingFile(fileBuilder: FirFileBuilder) {
    }

    // TODO: find out some way to differentiate detection form REPL snippets, to allow reporting conflicts on FIR building
    override fun accepts(sourceFile: KtSourceFile?, scriptSource: KtSourceElement): Boolean =
        sourceFile != null

    @OptIn(SymbolInternals::class)
    override fun FirScriptBuilder.configure(sourceFile: KtSourceFile?, context: Context<*>) {
        fun addErrorElement(message: String) {
            declarations.add(
                0,
                buildAnonymousInitializer {
                    symbol = FirAnonymousInitializerSymbol()
                    source = this@configure.source
                    moduleData = this@configure.moduleData
                    origin = FirDeclarationOrigin.ScriptCustomization.Default
                    body = buildSingleExpressionBlock(
                        buildErrorExpression {
                            source = this@configure.source
                            diagnostic = ConeSimpleDiagnostic(message)
                        }
                    )
                    containingDeclarationSymbol = symbol
                }
            )
        }

        val configuration =
            getOrLoadConfiguration(session, sourceFile!!)?.valueOr {
                addErrorElement("Unable to get script compilation configuration: ${it.reports.joinToString("; ") { it.message}}")
                return
            } ?: run {
                addErrorElement("Unable to get script compilation configuration. Scripting is not configured.")
                return
            }

        configuration.getNoDefault(ScriptCompilationConfiguration.baseClass)?.let { baseClass ->
            val baseClassTypeRef =
                tryResolveOrBuildParameterTypeRefFromKotlinType(baseClass, source.fakeElement(KtFakeSourceElementKind.ScriptBaseClass))

            receivers.add(
                buildScriptReceiverParameter {
                    typeRef = baseClassTypeRef
                    isBaseClassReceiver = true
                    symbol = FirReceiverParameterSymbol()
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.ScriptCustomization.ParameterFromBaseClass
                    containingDeclarationSymbol = this@configure.symbol
                }
            )

            if (baseClassTypeRef is FirResolvedTypeRef) {
                baseClassTypeRef.toRegularClassSymbol(session)?.fir?.primaryConstructorIfAny(session)?.fir?.valueParameters?.forEach { baseCtorParameter ->
                    parameters.add(
                        buildProperty {
                            moduleData = session.moduleData
                            source = this@configure.source.fakeElement(KtFakeSourceElementKind.ScriptParameter)
                            origin = FirDeclarationOrigin.ScriptCustomization.ParameterFromBaseClass
                            // TODO: copy type parameters?
                            returnTypeRef = baseCtorParameter.returnTypeRef
                            name = baseCtorParameter.name
                            symbol = FirRegularPropertySymbol(CallableId(name))
                            status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                            isLocal = true
                            isVar = false
                        }
                    )
                }
            }
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

        configuration[ScriptCompilationConfiguration.providedProperties]?.forEach { (propertyName, propertyType) ->
            parameters.add(
                buildProperty {
                    moduleData = session.moduleData
                    source = this@configure.source.fakeElement(KtFakeSourceElementKind.ScriptParameter)
                    origin = FirDeclarationOrigin.ScriptCustomization.Parameter
                    returnTypeRef = this@configure.tryResolveOrBuildParameterTypeRefFromKotlinType(propertyType)
                    name = Name.identifier(propertyName)
                    symbol = FirLocalPropertySymbol()
                    status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                    isLocal = true
                    isVar = false
                }
            )
        }

        configuration[ScriptCompilationConfiguration.explainField]?.let {
            parameters.add(
                buildProperty {
                    moduleData = session.moduleData
                    source = this@configure.source.fakeElement(KtFakeSourceElementKind.ScriptParameter)
                    origin = FirDeclarationOrigin.ScriptCustomization.Parameter
                    returnTypeRef = this@configure.tryResolveOrBuildParameterTypeRefFromKotlinType(KotlinType(MutableMap::class))
                    name = Name.identifier(it)
                    symbol = FirLocalPropertySymbol()
                    status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                    isLocal = true
                    isVar = false
                }
            )
        }

        configuration[ScriptCompilationConfiguration.annotationsForSamWithReceivers]?.forEach {
            knownAnnotationsForSamWithReceiver.add(it.typeName)
        }

        configuration[ScriptCompilationConfiguration.resultField]?.takeIf { it.isNotBlank() }?.let { resultFieldName ->
            val lastScriptBlock = declarations.lastOrNull() as? FirAnonymousInitializer
            val lastExpression =
                when (val lastScriptBlockBody = lastScriptBlock?.body) {
                    is FirLazyBlock -> null
                    is FirSingleExpressionBlock -> lastScriptBlockBody.statement as? FirExpression
                    else -> lastScriptBlockBody?.statements?.singleOrNull()?.takeIf { it is FirExpression } as? FirExpression
                }?.takeUnless { it is FirErrorExpression }

            if (lastExpression != null) {
                declarations.removeLast()
                @OptIn(UnresolvedExpressionTypeAccess::class)
                val lastExpressionTypeRef =
                    lastExpression.takeUnless { it is FirLazyExpression }?.coneTypeOrNull?.toFirResolvedTypeRef()
                        ?: FirImplicitTypeRefImplWithoutSource
                declarations.add(
                    buildProperty {
                        this.name = Name.identifier(resultFieldName)
                        this.symbol = FirRegularPropertySymbol(CallableId(context.packageFqName, this.name))
                        source = lastScriptBlock?.source
                        moduleData = session.moduleData
                        origin = FirDeclarationOrigin.ScriptCustomization.ResultProperty
                        initializer = lastExpression
                        returnTypeRef = lastExpressionTypeRef
                        getter = FirDefaultPropertyGetter(
                            source = lastScriptBlock?.source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor),
                            moduleData = session.moduleData,
                            origin = FirDeclarationOrigin.ScriptCustomization.ResultProperty,
                            propertyTypeRef = lastExpressionTypeRef,
                            visibility = Visibilities.Public,
                            propertySymbol = this.symbol,
                            modality = Modality.FINAL,
                        )

                        status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
                        isLocal = false
                        isVar = false
                    }.also {
                        resultPropertyName = it.name
                    }
                )
            }
        }
    }

    private fun FirScriptBuilder.tryResolveOrBuildParameterTypeRefFromKotlinType(
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

    internal val knownAnnotationsForSamWithReceiver: Set<String>
        field = hashSetOf<String>()

    companion object {
        fun getFactory(): Factory {
            return Factory { session -> FirScriptConfiguratorExtensionImpl(session) }
        }

        @Deprecated("Use other getFactory methods. This one left only for transitional compatibility (KT-83969)")
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirScriptConfiguratorExtensionImpl(session) }
        }
    }
}

internal fun getOrLoadConfiguration(session: FirSession, file: KtSourceFile): ResultWithDiagnostics<ScriptCompilationConfiguration>? {
    val sourceCode = file.toSourceCode()
    return session.scriptDefinitionProviderService?.let {
        it.getRefinedConfiguration(sourceCode)
            ?: it.getBaseConfiguration(sourceCode)
    }
}

