/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

@OptIn(FirExtensionApiInternals::class)
class DataFrameLikeCallsRefinementExtension(session: FirSession) : FirFunctionCallRefinementExtension(session) {
    companion object {
        val REFINE = ClassId(FqName.ROOT, Name.identifier("Refine"))
        val DATAFRAME = ClassId(FqName.ROOT, Name.identifier("DataFrame"))

        data object KEY : GeneratedDeclarationKey()
    }

    override fun intercept(callInfo: CallInfo, symbol: FirNamedFunctionSymbol): CallReturnType? {
        if (!symbol.hasAnnotation(REFINE, session)) return null
        val lookupTag = ConeClassLikeLookupTagImpl(DATAFRAME)
        val refinedTypeId = localClassId(Name.identifier("DataFrameType"))
        val schemaId = localClassId(Name.identifier("Schema1"))
        val schemaSymbol = FirRegularClassSymbol(schemaId)
        val schemaClass = buildRegularClass {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(KEY)
            status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.ABSTRACT, EffectiveVisibility.Local)
            deprecationsProvider = EmptyDeprecationsProvider
            classKind = ClassKind.CLASS
            scopeProvider = FirKotlinScopeProvider()
            superTypeRefs += FirImplicitAnyTypeRef(null)

            name = schemaId.shortClassName
            this.symbol = schemaSymbol
        }


        val refinedTypeSymbol = FirRegularClassSymbol(refinedTypeId)
        val refinedTypeDeclaration = buildRegularClass {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(KEY)
            status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.ABSTRACT, EffectiveVisibility.Local)
            deprecationsProvider = EmptyDeprecationsProvider
            classKind = ClassKind.CLASS
            scopeProvider = FirKotlinScopeProvider()

            name = refinedTypeId.shortClassName
            this.symbol = refinedTypeSymbol
            superTypeRefs += buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagWithFixedSymbol(schemaId, schemaSymbol),
                    emptyArray(),
                    isMarkedNullable = false
                )
            }
        }

        val typeRef = buildResolvedTypeRef {
            coneType = ConeClassLikeTypeImpl(
                lookupTag,
                arrayOf(
                    ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagWithFixedSymbol(refinedTypeId, refinedTypeSymbol),
                        emptyArray(),
                        isMarkedNullable = false
                    )
                ),
                isMarkedNullable = false
            )
        }
        return CallReturnType(typeRef) { functionSymbol ->
            session.callDataStorage.generatedCallData.getValue(functionSymbol, GeneratedCallData(refinedTypeDeclaration, schemaClass))
        }
    }

    @OptIn(SymbolInternals::class)
    override fun transform(call: FirFunctionCall, originalSymbol: FirNamedFunctionSymbol): FirFunctionCall {
        val resolvedLet = findLet()
        val parameter = resolvedLet.valueParameterSymbols[0]

        val explicitReceiver = call.explicitReceiver ?: return call
        val receiverType = explicitReceiver.resolvedType
        val returnType = call.resolvedType
        val originalSource = call.calleeReference.source
        val callDispatchReceiver = call.dispatchReceiver
        val callExtensionReceiver = call.extensionReceiver

        val symbol = call.calleeReference.resolved?.toResolvedNamedFunctionSymbol() ?: return call
        val callData = session.callDataStorage.generatedCallData.getValue(symbol)
        val refinedType = callData.type
        val schemaClass = callData.schema

        call.transformCalleeReference(object : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
                return if (element is FirResolvedNamedReference) {
                    @Suppress("UNCHECKED_CAST")
                    buildResolvedNamedReference {
                        this.name = element.name
                        resolvedSymbol = originalSymbol
                    } as E
                } else {
                    element
                }
            }
        }, null)

        val scope = localClassId(Name.identifier("Scope1"))
        val scopeSymbol = FirRegularClassSymbol(scope)
        val columns: List<Column> = listOf(Column(Name.identifier("column"), session.builtinTypes.intType))

        schemaClass.callShapeData = CallShapeData.Schema(columns)

        val scopeClass = buildRegularClass {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(KEY)
            status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.ABSTRACT, EffectiveVisibility.Local)
            deprecationsProvider = EmptyDeprecationsProvider
            classKind = ClassKind.CLASS
            scopeProvider = FirKotlinScopeProvider()
            superTypeRefs += FirImplicitAnyTypeRef(null)
            name = scope.shortClassName
            this.symbol = scopeSymbol
        }

        scopeClass.callShapeData = CallShapeData.Scope(schemaClass.symbol, columns)

        refinedType.callShapeData = CallShapeData.RefinedType(listOf(scopeSymbol))

        val argument = buildAnonymousFunctionExpression {
            val fSymbol = FirAnonymousFunctionSymbol()
            val target = FirFunctionTarget(null, isLambda = true)
            isTrailingLambda = true
            anonymousFunction = buildAnonymousFunction {
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Plugin(KEY)
                status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
                deprecationsProvider = EmptyDeprecationsProvider
                returnTypeRef = buildResolvedTypeRef {
                    coneType = returnType
                }
                val itName = Name.identifier("it")
                val parameterSymbol = FirValueParameterSymbol()
                valueParameters += buildValueParameter {
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Plugin(KEY)
                    returnTypeRef = buildResolvedTypeRef {
                        coneType = receiverType
                    }
                    name = itName
                    this.symbol = parameterSymbol
                    containingDeclarationSymbol = fSymbol
                    isCrossinline = false
                    isNoinline = false
                    isVararg = false
                }
                body = buildBlock {
                    this.coneTypeOrNull = returnType

                    // Schema is required for static extensions resolve and holds information for subsequent call modifications
                    statements += schemaClass

                    // Scope (provides extensions API)
                    statements += scopeClass

                    // Return type - dataframe schema
                    statements += refinedType

                    statements += buildReturnExpression {
                        val itPropertyAccess = buildPropertyAccessExpression {
                            coneTypeOrNull = receiverType
                            calleeReference = buildResolvedNamedReference {
                                name = parameterSymbol.name
                                resolvedSymbol = parameterSymbol
                            }
                        }
                        if (callDispatchReceiver != null) {
                            call.replaceDispatchReceiver(itPropertyAccess)
                        }
                        call.replaceExplicitReceiver(itPropertyAccess)
                        if (callExtensionReceiver != null) {
                            call.replaceExtensionReceiver(itPropertyAccess)
                        }

                        result = call
                        this.target = target
                    }
                }
                this.symbol = fSymbol
                isLambda = true
                hasExplicitParameterList = false
                typeRef = buildResolvedTypeRef {
                    coneType = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(ClassId(FqName("kotlin"), Name.identifier("Function1"))),
                        typeArguments = arrayOf(receiverType, returnType),
                        isMarkedNullable = false
                    )
                }
                invocationKind = EventOccurrencesRange.EXACTLY_ONCE
                inlineStatus = InlineStatus.Inline
            }.also { target.bind(it) }
        }

        for (generatedClass in listOf(schemaClass, scopeClass, refinedType)) {
            generatedClass.anchor = call.source
        }
        refinedType.generatedClasses = listOf(schemaClass, scopeClass, refinedType).map { it.symbol }
        val newCall = buildFunctionCall {
            this.coneTypeOrNull = returnType
            typeArguments += buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef {
                    coneType = receiverType
                }
                variance = Variance.INVARIANT
            }

            typeArguments += buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef {
                    coneType = returnType
                }
                variance = Variance.INVARIANT
            }
            dispatchReceiver = call.dispatchReceiver
            this.explicitReceiver = call.explicitReceiver
            extensionReceiver = call.extensionReceiver
            argumentList = buildResolvedArgumentList(original = null, linkedMapOf(argument to parameter.fir))
            calleeReference = buildResolvedNamedReference {
                source = originalSource
                name = Name.identifier("let")
                resolvedSymbol = resolvedLet
            }
        }
        return newCall
    }

    override fun ownsSymbol(symbol: FirRegularClassSymbol): Boolean {
        return symbol.anchor != null
    }

    override fun anchorElement(symbol: FirRegularClassSymbol): KtSourceElement {
        return symbol.anchor!!
    }

    override fun restoreSymbol(call: FirFunctionCall, name: Name): FirRegularClassSymbol? {
        val newType = (call.resolvedType.typeArguments.getOrNull(0) as? ConeClassLikeType)?.toRegularClassSymbol(session)
        return newType?.generatedClasses?.find { it.name == name }
    }

    private fun localClassId(name: Name) = ClassId(CallableId.PACKAGE_FQ_NAME_FOR_LOCAL, FqName.ROOT.child(name), isLocal = true)

    private fun findLet(): FirFunctionSymbol<*> {
        return session.symbolProvider.getTopLevelFunctionSymbols(FqName("kotlin"), Name.identifier("let")).single()
    }
}

class CallDataStorage(session: FirSession) : FirExtensionSessionComponent(session) {
    val generatedCallData =
        session.firCachesFactory.createCache<FirNamedFunctionSymbol, GeneratedCallData, GeneratedCallData?> { symbol, context ->
            context ?: error("context not provided for $symbol")
        }
}

val FirSession.callDataStorage: CallDataStorage by FirSession.sessionComponentAccessor()

class GeneratedCallData(val type: FirRegularClass, val schema: FirRegularClass)
