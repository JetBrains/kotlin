/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.calls.CallInfo
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.resolvedType
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

        object KEY : GeneratedDeclarationKey()
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
            origin = FirDeclarationOrigin.Source
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
            origin = FirDeclarationOrigin.Source
            status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.ABSTRACT, EffectiveVisibility.Local)
            deprecationsProvider = EmptyDeprecationsProvider
            classKind = ClassKind.CLASS
            scopeProvider = FirKotlinScopeProvider()

            name = refinedTypeId.shortClassName
            this.symbol = refinedTypeSymbol
            superTypeRefs += buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(
                    ConeClassLookupTagWithFixedSymbol(schemaId, schemaSymbol),
                    emptyArray(),
                    isNullable = false
                )
            }
        }

        val typeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                lookupTag,
                arrayOf(
                    ConeClassLikeTypeImpl(
                        ConeClassLookupTagWithFixedSymbol(refinedTypeId, refinedTypeSymbol),
                        emptyArray(),
                        isNullable = false
                    )
                ),
                isNullable = false
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

        val symbol = call.calleeReference.resolved?.toResolvedNamedFunctionSymbol() ?: return call
        val callData = session.callDataStorage.generatedCallData.getValue(symbol)
        val refinedType = callData.type
        val schemaClass = callData.schema

        val scope = localClassId(Name.identifier("Scope1"))
        val scopeSymbol = FirRegularClassSymbol(scope)
        val columns: List<Column> = listOf(Column(Name.identifier("column"), session.builtinTypes.intType))

        schemaClass.callShapeData = CallShapeData.Schema(columns)

        val scopeClass = buildRegularClass {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Source
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
            isTrailingLambda = true
            anonymousFunction = buildAnonymousFunction {
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Plugin(KEY)
                status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
                deprecationsProvider = EmptyDeprecationsProvider
                returnTypeRef = buildResolvedTypeRef {
                    type = returnType
                }
                val itName = Name.identifier("it")
                val parameterSymbol = FirValueParameterSymbol(itName)
                valueParameters += buildValueParameter {
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Plugin(KEY)
                    returnTypeRef = buildResolvedTypeRef {
                        type = receiverType
                    }
                    name = itName
                    this.symbol = parameterSymbol
                    containingFunctionSymbol = fSymbol
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
                }
                this.symbol = fSymbol
                isLambda = true
                hasExplicitParameterList = false
                typeRef = buildResolvedTypeRef {
                    type = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(ClassId(FqName("kotlin"), Name.identifier("Function1"))),
                        typeArguments = arrayOf(receiverType, returnType),
                        isNullable = false
                    )
                }
                invocationKind = EventOccurrencesRange.EXACTLY_ONCE
                inlineStatus = InlineStatus.Inline
            }
        }

        val newCall = buildFunctionCall {
            this.coneTypeOrNull = returnType
            typeArguments += buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef {
                    type = receiverType
                }
                variance = Variance.INVARIANT
            }

            typeArguments += buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef {
                    type = returnType
                }
                variance = Variance.INVARIANT
            }
            dispatchReceiver = call.dispatchReceiver
            this.explicitReceiver = call.explicitReceiver
            extensionReceiver = call.extensionReceiver
            argumentList = buildResolvedArgumentList(original = null, linkedMapOf(argument to parameter.fir))
            calleeReference = buildResolvedNamedReference {
                name = Name.identifier("let")
                resolvedSymbol = resolvedLet
            }
        }
        return newCall
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
