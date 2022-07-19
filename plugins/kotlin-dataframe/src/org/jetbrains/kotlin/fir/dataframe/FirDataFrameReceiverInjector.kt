/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema

class SchemaContext(val properties: List<SchemaProperty>, val coneTypeProjection: ConeTypeProjection)


class FirDataFrameReceiverInjector(
    session: FirSession,
    private val state: MutableMap<ClassId, SchemaContext>,
    private val ids: ArrayDeque<ClassId>
) : FirExpressionResolutionExtension(session) {
    companion object {
        val DF_CLASS_ID = ClassId.topLevel(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "DataFrame")))
        val DF_ANNOTATIONS_PACKAGE = Name.identifier("org.jetbrains.kotlinx.dataframe.annotations")
        val INTERPRETABLE_FQNAME = FqName(Interpretable::class.qualifiedName!!)
    }

    @Suppress("UNCHECKED_CAST")
    override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
        val callReturnType = functionCall.typeRef.coneTypeSafe<ConeClassLikeType>() ?: return emptyList()
        if (callReturnType.classId != DF_CLASS_ID) return emptyList()
        val processor = functionCall.loadInterpreter(session) ?: return emptyList()

        val dataFrameSchema = interpret(functionCall, processor)?.let { it.value as PluginDataFrameSchema } ?: return emptyList()

        // region generate code
        val properties = dataFrameSchema.columns().map {
            val typeApproximation = when (val type = it.type) {
                is TypeApproximationImpl -> type
                ColumnGroupTypeApproximation -> TODO("support column groups in data schema")
                FrameColumnTypeApproximation -> TODO()
            }
            val type = ConeClassLikeLookupTagImpl(ClassId.topLevel(FqName(typeApproximation.fqName))).constructType(
                emptyArray(), false
            )
            SchemaProperty(false, it.name(), type, callReturnType.typeArguments[0])
        }

        val id = ids.removeLast()
        state[id] = SchemaContext(properties, callReturnType.typeArguments[0])
        return listOf(ConeClassLikeLookupTagImpl(id).constructClassType(emptyArray(), isNullable = false))
        // endregion
    }

    private fun findSchemaProcessor(functionCall: FirFunctionCall): SchemaModificationInterpreter? {
        val firNamedFunctionSymbol = functionCall.calleeReference.resolvedSymbol as? FirNamedFunctionSymbol ?: error("cannot resolve symbol for ${functionCall.calleeReference.name}")
        val annotation = firNamedFunctionSymbol.annotations.firstOrNull {
            val name1 = it.fqName(session)!!
            val name2 = FqName("org.jetbrains.kotlinx.dataframe.annotations.SchemaProcessor")
            name1 == name2
        } ?: return null

        val name = Name.identifier("processor")
        val getClassCall = (annotation.argumentMapping.mapping[name] as FirGetClassCall)
        return getClassCall.classId.load<SchemaModificationInterpreter>()
    }

    object DataFramePluginKey : GeneratedDeclarationKey()
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

data class SchemaProperty(val override: Boolean, val name: String, val type: ConeKotlinType, val coneTypeProjection: ConeTypeProjection)