/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.*
import org.jetbrains.kotlin.fir.dataframe.Names.COLUM_GROUP_CLASS_ID
import org.jetbrains.kotlin.fir.dataframe.Names.DF_CLASS_ID
import org.jetbrains.kotlin.fir.dataframe.loadInterpreter
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn

class SchemaContext(val properties: List<SchemaProperty>)

class FirDataFrameReceiverInjector(
    session: FirSession,
    private val scopeState: MutableMap<ClassId, SchemaContext>,
    private val scopeIds: ArrayDeque<ClassId>,
    val tokenState: MutableMap<ClassId, SchemaContext>,
    val tokenIds: ArrayDeque<ClassId>,
    val path: String?
) : FirExpressionResolutionExtension(session), KotlinTypeFacade {

    override val resolutionPath: String?
        get() = path

    private val associatedScopes = mutableMapOf<ClassId, List<ConeKotlinType>>()

    override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
        val classId = runIf(functionCall.calleeReference.name == Name.identifier("injectAccessors")) {
            ((functionCall.typeArguments.getOrNull(0) as? FirTypeProjectionWithVariance)?.typeRef?.coneType as? ConeClassLikeType)?.classId
        }
        val types =  buildList {
            if (classId != null) {
                associatedScopes[classId]?.let {
                    addAll(it)
                }
            }
            addAll(generateAccessorsScopesForRefinedCall(functionCall, scopeState, scopeIds, tokenIds, tokenState, associatedScopes))
        }
        return types
    }

    object DataFramePluginKey : GeneratedDeclarationKey()
}

fun KotlinTypeFacade.generateAccessorsScopesForRefinedCall(
    functionCall: FirFunctionCall,
    scopeState: MutableMap<ClassId, SchemaContext>,
    scopeIds: ArrayDeque<ClassId>,
    tokenIds: ArrayDeque<ClassId>,
    tokenState: MutableMap<ClassId, SchemaContext>,
    associatedScopes: MutableMap<ClassId, List<ConeKotlinType>>,
    reporter: InterpretationErrorReporter = InterpretationErrorReporter.DEFAULT
): List<ConeKotlinType> {
    val (rootMarker, dataFrameSchema) = analyzeRefinedCallShape(functionCall, reporter) ?: return emptyList()

    val types: MutableList<ConeClassLikeType> = mutableListOf()

    fun PluginDataFrameSchema.materialize(rootMarker: ConeTypeProjection? = null): ConeTypeProjection {
        val scopeId = scopeIds.removeLast()
        var tokenId = rootMarker?.type?.classId
        if (tokenId == null) {
            tokenId = tokenIds.removeLast()
        }
        val marker = rootMarker ?: ConeClassLikeLookupTagImpl(tokenId)
            .constructClassType(emptyArray(), isNullable = false)
        val properties = columns().map {
            @Suppress("USELESS_IS_CHECK")
            when (it) {
                is SimpleColumnGroup -> {
                    val nestedClassMarker = PluginDataFrameSchema(it.columns()).materialize()
                    val columnsContainerReturnType =
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(COLUM_GROUP_CLASS_ID),
                            typeArguments = arrayOf(nestedClassMarker),
                            isNullable = false
                        )

                    val dataRowReturnType =
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
                            typeArguments = arrayOf(nestedClassMarker),
                            isNullable = false
                        )

                    SchemaProperty(marker, it.name, dataRowReturnType, columnsContainerReturnType)
                }

                is SimpleFrameColumn -> {
                    val nestedClassMarker = PluginDataFrameSchema(it.columns()).materialize()
                    val frameColumnReturnType =
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(DF_CLASS_ID),
                            typeArguments = arrayOf(nestedClassMarker),
                            isNullable = it.nullable
                        )

                    SchemaProperty(
                        marker = marker,
                        name = it.name,
                        dataRowReturnType = frameColumnReturnType,
                        columnContainerReturnType = frameColumnReturnType.toFirResolvedTypeRef().projectOverDataColumnType()
                    )
                }

                is SimpleCol -> SchemaProperty(
                    marker = marker,
                    name = it.name,
                    dataRowReturnType = it.type.type(),
                    columnContainerReturnType = it.type.type().toFirResolvedTypeRef().projectOverDataColumnType()
                )

                else -> TODO("shouldn't happen")
            }
        }
        scopeState[scopeId] = SchemaContext(properties)
        tokenState[tokenId] = SchemaContext(properties)
        types += ConeClassLikeLookupTagImpl(scopeId).constructClassType(emptyArray(), isNullable = false)
        return marker
    }
    dataFrameSchema.materialize(rootMarker)
    associatedScopes[rootMarker.classId!!] = types
    return types
}

data class CallResult(val rootMarker: ConeClassLikeType, val dataFrameSchema: PluginDataFrameSchema)

fun KotlinTypeFacade.analyzeRefinedCallShape(call: FirFunctionCall, reporter: InterpretationErrorReporter): CallResult? {
    val callReturnType = call.typeRef.coneTypeSafe<ConeClassLikeType>() ?: return null
    if (callReturnType.classId != DF_CLASS_ID) return null
    val rootMarker = callReturnType.typeArguments[0]
    if (rootMarker !is ConeClassLikeType) {
        return null
    }
    val origin = rootMarker.toSymbol(session)?.origin
    if (origin !is FirDeclarationOrigin.Plugin || origin.key != FirDataFrameReceiverInjector.DataFramePluginKey) {
        return null
    }

    val processor = call.loadInterpreter(session) ?: return null

    val dataFrameSchema = interpret(call, processor, reporter = reporter)
        .let {
            val value = it?.value
            if (value !is PluginDataFrameSchema) {
                if (!reporter.errorReported) {
                    reporter.reportInterpretationError(call, "${processor::class} must return ${PluginDataFrameSchema::class}, but was ${value}")
                }
                return null
            }
            value
        }


    return CallResult(rootMarker, dataFrameSchema)
}

fun FirFunctionCall.functionSymbol(): FirNamedFunctionSymbol {
    val firResolvedNamedReference = calleeReference as FirResolvedNamedReference
    return firResolvedNamedReference.resolvedSymbol as FirNamedFunctionSymbol
}

class Arguments(val refinedArguments: List<RefinedArgument>) : List<RefinedArgument> by refinedArguments

data class RefinedArgument(val name: Name, val expression: FirExpression) {

    override fun toString(): String {
        return "RefinedArgument(name=$name, expression=${expression})"
    }
}

data class SchemaProperty(
    val marker: ConeTypeProjection,
    val name: String,
    val dataRowReturnType: ConeKotlinType,
    val columnContainerReturnType: ConeKotlinType,
    val override: Boolean = false
)
