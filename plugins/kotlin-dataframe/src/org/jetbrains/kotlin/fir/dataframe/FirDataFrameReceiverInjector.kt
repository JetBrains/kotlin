/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.Names.COLUM_GROUP_CLASS_ID
import org.jetbrains.kotlin.fir.dataframe.Names.DF_CLASS_ID
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
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn

class SchemaContext(val properties: List<SchemaProperty>)

object Names {
    val DF_CLASS_ID: ClassId
        get() = ClassId.topLevel(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "DataFrame")))
    val COLUM_GROUP_CLASS_ID: ClassId
        get() = ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("ColumnGroup"))
    val DATA_COLUMN_CLASS_ID: ClassId
        get() = ClassId(
            FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")),
            Name.identifier("DataColumn")
        )
    val COLUMNS_CONTAINER_CLASS_ID: ClassId
        get() = ClassId(
            FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")),
            Name.identifier("ColumnsContainer")
        )
    val DATA_ROW_CLASS_ID: ClassId
        get() = ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")), Name.identifier("DataRow"))
    val DF_ANNOTATIONS_PACKAGE: Name
        get() = Name.identifier("org.jetbrains.kotlinx.dataframe.annotations")
    val INTERPRETABLE_FQNAME: FqName
        get() = FqName(Interpretable::class.qualifiedName!!)
}

class FirDataFrameReceiverInjector(
    session: FirSession,
    private val state: MutableMap<ClassId, SchemaContext>,
    private val ids: ArrayDeque<ClassId>
) : FirExpressionResolutionExtension(session) {

    override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
        val callReturnType = functionCall.typeRef.coneTypeSafe<ConeClassLikeType>() ?: return emptyList()
        if (callReturnType.classId != DF_CLASS_ID) return emptyList()
        val processor = functionCall.loadInterpreter(session) ?: return emptyList()

        val dataFrameSchema = interpret(functionCall, processor)?.let { it.value as PluginDataFrameSchema } ?: return emptyList()

        val types: MutableList<ConeClassLikeType> = mutableListOf()

        // TODO: generate a new marker for each call when there is an API to cast functionCall result to this type
        val rootMarker = callReturnType.typeArguments[0]

        fun PluginDataFrameSchema.materialize(rootMarker: ConeTypeProjection? = null): ConeTypeProjection {
            val id = ids.removeLast()
            val marker = rootMarker ?: ConeClassLikeLookupTagImpl(id)
                .constructClassType(emptyArray(), isNullable = false)
            val properties = columns().map {
                @Suppress("USELESS_IS_CHECK")
                when (it) {
                    is SimpleColumnGroup -> {
                        val nestedClassMarker = PluginDataFrameSchema(it.columns()).materialize()
                        val columnGroupReturnType =
                            ConeClassLikeTypeImpl(
                                ConeClassLikeLookupTagImpl(COLUM_GROUP_CLASS_ID),
                                typeArguments = arrayOf(nestedClassMarker),
                                isNullable = false
                            )
                        SchemaProperty(marker, it.name, columnGroupReturnType)
                    }

                    is SimpleFrameColumn -> {
                        val nestedClassMarker = PluginDataFrameSchema(it.columns()).materialize()
                        val frameColumnReturnType =
                            ConeClassLikeTypeImpl(
                                ConeClassLikeLookupTagImpl(DF_CLASS_ID),
                                typeArguments = arrayOf(nestedClassMarker),
                                isNullable = it.nullable
                            )

                        SchemaProperty(marker, it.name, frameColumnReturnType)
                    }
                    is SimpleCol -> SchemaProperty(marker, it.name, it.type.convert())
                    else -> TODO("shouldn't happen")
                }
            }
            state[id] = SchemaContext(properties)
            types += ConeClassLikeLookupTagImpl(id).constructClassType(emptyArray(), isNullable = false)
            return marker
        }

        dataFrameSchema.materialize(rootMarker)
        return types
    }

    private fun findSchemaProcessor(functionCall: FirFunctionCall): SchemaModificationInterpreter? {
        val firNamedFunctionSymbol = functionCall.calleeReference.resolvedSymbol as? FirNamedFunctionSymbol
            ?: error("cannot resolve symbol for ${functionCall.calleeReference.name}")
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

private fun TypeApproximation.convert(): ConeKotlinType {
    return when (this) {
        is ColumnGroupTypeApproximation -> TODO()
        is FrameColumnTypeApproximation -> TODO()
        is TypeApproximationImpl -> ConeClassLikeLookupTagImpl(
            ClassId(
                FqName(fqName.substringBeforeLast(".")),
                Name.identifier(fqName.substringAfterLast("."))
            )
        ).constructType(emptyArray(), this.nullable)
    }
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

data class SchemaProperty(val marker: ConeTypeProjection, val name: String, val returnType: ConeKotlinType, val override: Boolean = false)