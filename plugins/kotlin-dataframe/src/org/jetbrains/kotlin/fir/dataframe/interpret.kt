/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.dataframe.utils.Names
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.scopes.collectAllProperties
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isStarProjection
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.returnType
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.Marker
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.ColumnPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.DataFrameCallableId
import org.jetbrains.kotlinx.dataframe.plugin.KPropertyApproximation
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn

interface InterpretationErrorReporter {
    val errorReported: Boolean
    fun reportInterpretationError(call: FirFunctionCall, message: String)

    fun doNotReportInterpretationError()

    companion object {
        val DEFAULT = object : InterpretationErrorReporter {
            override val errorReported: Boolean = false
            override fun reportInterpretationError(call: FirFunctionCall, message: String) {

            }

            override fun doNotReportInterpretationError() = Unit
        }
    }
}

fun <T> KotlinTypeFacade.interpret(
    functionCall: FirFunctionCall,
    processor: Interpreter<T>,
    additionalArguments: Map<String, Interpreter.Success<Any?>> = emptyMap(),
    reporter: InterpretationErrorReporter,
): Interpreter.Success<T>? {

    val refinedArguments: Arguments = functionCall.collectArgumentExpressions()

    val defaultArguments = processor.expectedArguments.filter { it.defaultValue is Present }.map { it.name }.toSet()
    val actualArgsMap = refinedArguments.associateBy { it.name.identifier }.toSortedMap()
    val expectedArgsMap = processor.expectedArguments
        .filterNot { it.name.startsWith("typeArg") }
        .associateBy { it.name }.toSortedMap().minus(additionalArguments.keys)

    if (expectedArgsMap.keys - defaultArguments != actualArgsMap.keys - defaultArguments) {
        val message = buildString {
            appendLine("ERROR: Different set of arguments")
            appendLine("Implementation class: $processor")
            appendLine("Not found in actual: ${expectedArgsMap.keys - actualArgsMap.keys}")
            val diff = actualArgsMap.keys - expectedArgsMap.keys
            appendLine("Passed, but not expected: ${diff}")
            appendLine("add arguments to an interpeter:")
            appendLine(diff.map { actualArgsMap[it] })
        }
        reporter.reportInterpretationError(functionCall, message)
        return null
    }

    val arguments = mutableMapOf<String, Interpreter.Success<Any?>>()
    arguments += additionalArguments
    val interpretationResults = refinedArguments.refinedArguments.mapNotNull {
        val name = it.name.identifier
        val expectedArgument = expectedArgsMap[name]!!
        val expectedReturnType = expectedArgument.klass
        val value: Interpreter.Success<Any?>? = when (expectedArgument.lens) {
            is Interpreter.Value -> {
                when (val expression = it.expression) {
                    is FirLiteralExpression -> Interpreter.Success(expression.value!!)
                    is FirVarargArgumentsExpression -> {
                        val args = expression.arguments.map {
                            when (it) {
                                is FirLiteralExpression -> it.value
                                is FirCallableReferenceAccess -> {
                                    toKPropertyApproximation(it, session)
                                }

                                else -> null
                            }

                        }
                        Interpreter.Success(args)
                    }

                    is FirFunctionCall -> {
                        val interpreter = expression.loadInterpreter()
                        interpreter?.let {
                            val result = interpret(expression, interpreter, emptyMap(), reporter)
                            result
                        }
                    }

                    is FirPropertyAccessExpression -> {
                        (expression.calleeReference as? FirResolvedNamedReference)?.let {
                            val symbol = it.resolvedSymbol
                            val literalInitializer = (symbol as? FirPropertySymbol)?.resolvedInitializer as? FirLiteralExpression
                            if (symbol is FirEnumEntrySymbol) {
                                Interpreter.Success(
                                    DataFrameCallableId(
                                        packageName = symbol.callableId.packageName.asString(),
                                        className = symbol.callableId.className!!.asString(),
                                        callableName = symbol.callableId.callableName.asString()
                                    )
                                )
                            } else if (literalInitializer != null) {
                                Interpreter.Success(literalInitializer.value)
                            } else {
                                Interpreter.Success(columnWithPathApproximations(expression))
                            }
                        }
                    }

                    is FirCallableReferenceAccess -> {
                        Interpreter.Success(toKPropertyApproximation(expression, session))
                    }

                    is FirAnonymousFunctionExpression -> {
                        val result = (expression.anonymousFunction.body?.statements?.lastOrNull() as? FirReturnExpression)?.result
                        val col: Any? = when (result) {
                            is FirPropertyAccessExpression -> {
                                columnWithPathApproximations(result)
                            }

                            is FirFunctionCall -> {
                                val interpreter = result.loadInterpreter()
                                if (interpreter == null) {
                                    reporter.reportInterpretationError(result, "Cannot load interpreter")
                                }
                                interpreter?.let {
                                    val value = interpret(result, interpreter, reporter = reporter)?.value
                                    value
                                }
                            }

                            is FirErrorExpression -> null

                            else -> null
                        }
                        col?.let { Interpreter.Success(it) }
                    }

                    else -> null
                }
            }

            is Interpreter.ReturnType -> {
                val returnType = it.expression.resolvedType.returnType(session)
                Interpreter.Success(Marker(returnType))
            }

            is Interpreter.Dsl -> {
                { receiver: Any ->
                    (it.expression as FirAnonymousFunctionExpression)
                        .anonymousFunction.body!!
                        .statements.filterIsInstance<FirFunctionCall>()
                        .forEach { call ->
                            val schemaProcessor = call.loadInterpreter() ?: return@forEach
                            interpret(
                                call,
                                schemaProcessor,
                                mapOf("dsl" to Interpreter.Success(receiver)),
                                reporter
                            )
                        }
                }.let { Interpreter.Success(it) }
            }

            is Interpreter.Schema -> {
                assert(expectedReturnType.toString() == PluginDataFrameSchema::class.qualifiedName!!) {
                    "'$name' should be ${PluginDataFrameSchema::class.qualifiedName!!}, but plugin expect $expectedReturnType"
                }

                val objectWithSchema = it.expression.getSchema()
                if (objectWithSchema == null) {
                    reporter.doNotReportInterpretationError()
                    null
                } else {
                    val arg = objectWithSchema.schemaArg
                    val schemaTypeArg = (objectWithSchema.typeRef as ConeClassLikeType).typeArguments[arg]
                    val schema = pluginDataFrameSchema(schemaTypeArg)
                    Interpreter.Success(schema)
                }
            }

            is Interpreter.Id -> {
                Interpreter.Success(it.expression)
            }
        }
        value?.let { value1 -> it.name.identifier to value1 }
    }

    functionCall.typeArguments.forEachIndexed { index, firTypeProjection ->
        val type = firTypeProjection.toConeTypeProjection().type ?: session.builtinTypes.nullableAnyType.type
        if (type is ConeIntersectionType) return@forEachIndexed
//        val approximation = TypeApproximationImpl(
//            type.classId!!.asFqNameString(),
//            type.isMarkedNullable
//        )
        val approximation = Marker(type)
        arguments["typeArg$index"] = Interpreter.Success(approximation)
    }

    return if (interpretationResults.size == refinedArguments.refinedArguments.size) {
        arguments.putAll(interpretationResults)
        when (val res = processor.interpret(arguments, this)) {
            is Interpreter.Success -> res
            is Interpreter.Error -> {
                reporter.reportInterpretationError(functionCall, res.message ?: "")
                return null
            }
        }
    } else {
        return null
    }
}

fun KotlinTypeFacade.pluginDataFrameSchema(schemaTypeArg: ConeTypeProjection): PluginDataFrameSchema {
    val schema = if (schemaTypeArg.isStarProjection) {
        PluginDataFrameSchema(emptyList())
    } else {
        val coneClassLikeType = schemaTypeArg.type as? ConeClassLikeType ?: return PluginDataFrameSchema(emptyList())
        pluginDataFrameSchema(coneClassLikeType)
    }
    return schema
}

fun KotlinTypeFacade.pluginDataFrameSchema(coneClassLikeType: ConeClassLikeType): PluginDataFrameSchema {
    val symbol = coneClassLikeType.toSymbol(session) as FirRegularClassSymbol
    val declarationSymbols = if (symbol.isLocal && symbol.resolvedSuperTypes.firstOrNull() != session.builtinTypes.anyType.type) {
        val rootSchemaSymbol = symbol.resolvedSuperTypes.first().toSymbol(session) as FirRegularClassSymbol
        rootSchemaSymbol.declaredMemberScope(session, FirResolvePhase.DECLARATIONS)
    } else {
        symbol.declaredMemberScope(session, FirResolvePhase.DECLARATIONS)
    }.let { scope ->
        val names = scope.getCallableNames()
        names.flatMap { scope.getProperties(it) }
    }

    val mapping = symbol.typeParameterSymbols
        .mapIndexed { i, symbol -> symbol to coneClassLikeType.typeArguments[i] }
        .toMap()

    var propertySymbols = declarationSymbols.filterIsInstance<FirPropertySymbol>()
    val annotations = propertySymbols.mapNotNull {
        val orderArgument = it.getAnnotationByClassId(
            Names.ORDER_ANNOTATION,
            session
        )?.argumentMapping?.mapping?.get(Names.ORDER_ARGUMENT)
        (orderArgument as? FirLiteralExpression)?.value as? Int
    }
    if (propertySymbols.size == annotations.size) {
        propertySymbols = propertySymbols.zip(annotations).sortedBy { it.second }.map { it.first }
    }
    val columns = propertySymbols.map { propertySymbol ->
        columnOf(propertySymbol, mapping)
    }

    return PluginDataFrameSchema(columns)
}

private fun KotlinTypeFacade.columnWithPathApproximations(result: FirPropertyAccessExpression): List<ColumnWithPathApproximation> {
    return result.resolvedType.let {
        val column = when {
            it.classId == Names.DATA_COLUMN_CLASS_ID -> {
                val arg = it.typeArguments.single() as ConeClassLikeType
                SimpleCol(f(result),Marker(arg))
            }

            it.classId == Names.COLUM_GROUP_CLASS_ID -> {
                val arg: ConeClassLikeType = it.typeArguments.single() as ConeClassLikeType
                val path = f(result)
                SimpleColumnGroup(path, pluginDataFrameSchema(arg).columns(), anyRow)
            }
            else -> return emptyList()
        }
        listOf(
            ColumnWithPathApproximation(
                path = ColumnPathApproximation(path(result)),
                column
            )
        )
    }
}

private fun KotlinTypeFacade.columnOf(it: FirPropertySymbol, mapping: Map<FirTypeParameterSymbol, ConeTypeProjection>): SimpleCol =
    when {
        shouldBeConvertedToFrameColumn(it) -> {
                val nestedColumns = it.resolvedReturnType.typeArguments[0].type
                    ?.toRegularClassSymbol(session)
                    ?.declaredMemberScope(session, FirResolvePhase.DECLARATIONS)
                    ?.collectAllProperties()
                    ?.filterIsInstance<FirPropertySymbol>()
                    ?.map { columnOf(it, mapping) }
                    ?: emptyList()

                SimpleFrameColumn(it.name.identifier, nestedColumns, false, anyDataFrame)
            }
        shouldBeConvertedToColumnGroup(it) -> {
            val type = if (isDataRow(it)) it.resolvedReturnType.typeArguments[0].type!! else it.resolvedReturnType
            val nestedColumns = type
                .toRegularClassSymbol(session)
                ?.declaredMemberScope(session, FirResolvePhase.DECLARATIONS)
                ?.collectAllProperties()
                ?.filterIsInstance<FirPropertySymbol>()
                ?.map { columnOf(it, mapping) }
                ?: emptyList()
            SimpleColumnGroup(it.name.identifier, nestedColumns, anyRow)
        }
        else -> {
            val type = when (val type = it.resolvedReturnType) {
                is ConeTypeParameterType -> mapping[type.lookupTag.typeParameterSymbol] as ConeKotlinType
                else -> type
            }

            SimpleCol(it.name.identifier, TypeApproximation(type))
        }
    }

private fun KotlinTypeFacade.shouldBeConvertedToColumnGroup(it: FirPropertySymbol) =
    isDataRow(it) ||
        it.resolvedReturnType.toRegularClassSymbol(session)?.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, session) == true

private fun isDataRow(it: FirPropertySymbol) =
    it.resolvedReturnType.classId == Names.DATA_ROW_CLASS_ID

private fun KotlinTypeFacade.shouldBeConvertedToFrameColumn(it: FirPropertySymbol) =
    isDataFrame(it) ||
        (it.resolvedReturnType.classId == Names.LIST &&
            it.resolvedReturnType.typeArguments[0].type?.toRegularClassSymbol(session)?.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, session) == true)

private fun isDataFrame(it: FirPropertySymbol) =
    it.resolvedReturnType.classId == Names.DF_CLASS_ID

fun path(propertyAccessExpression: FirPropertyAccessExpression): List<String> {
    val colName = f(propertyAccessExpression)
    val typeRef = propertyAccessExpression.dispatchReceiver?.resolvedType
    val joinDsl = ClassId(FqName("org.jetbrains.kotlinx.dataframe.api"), Name.identifier("JoinDsl"))
    if (typeRef?.classId?.equals(joinDsl) == true && colName == "right") {
        return emptyList()
    }
    return when (val explicitReceiver = propertyAccessExpression.explicitReceiver) {
        null -> listOf(colName)
        else -> path(explicitReceiver as FirPropertyAccessExpression) + colName
    }
}

fun f(propertyAccessExpression: FirPropertyAccessExpression): String {
    return propertyAccessExpression.calleeReference.resolved!!.name.identifier
}

private fun KotlinTypeFacade.toKPropertyApproximation(
    firCallableReferenceAccess: FirCallableReferenceAccess,
    session: FirSession
): KPropertyApproximation {
    val propertyName = firCallableReferenceAccess.calleeReference.name.identifier
    return (firCallableReferenceAccess.calleeReference as FirResolvedCallableReference).let {
        val symbol = it.toResolvedCallableSymbol()!!
        val columnName = symbol.annotations
            .find { it.fqName(session)!!.asString() == ColumnName::class.qualifiedName!! }
            ?.let {
                (it.argumentMapping.mapping[Name.identifier(ColumnName::name.name)] as FirLiteralExpression).value as String
            }
        val kotlinType = symbol.resolvedReturnTypeRef.type

        val type1 = Marker(kotlinType)
        KPropertyApproximation(columnName ?: propertyName, type1)
    }
}

internal fun FirFunctionCall.collectArgumentExpressions(): Arguments {
    val refinedArgument = mutableListOf<RefinedArgument>()

    val parameterName = Name.identifier("receiver")
    explicitReceiver?.let {
        if (it is FirResolvedQualifier && it.resolvedToCompanionObject) {
            return@let
        }
        refinedArgument += RefinedArgument(parameterName, it)
    }

    (argumentList as FirResolvedArgumentList).mapping.forEach { (expression, parameter) ->
        refinedArgument += RefinedArgument(parameter.name, expression)
    }
    return Arguments(refinedArgument)
}

internal val KotlinTypeFacade.getSchema: FirExpression.() -> ObjectWithSchema? get() = { getSchema(session) }

internal fun FirExpression.getSchema(session: FirSession): ObjectWithSchema? {
    return resolvedType.toSymbol(session)?.let {
        val (typeRef: ConeKotlinType, symbol) = if (it is FirTypeAliasSymbol) {
            it.resolvedExpandedTypeRef.coneType to it.resolvedExpandedTypeRef.toClassLikeSymbol(session)!!
        } else {
            resolvedType to it
        }
        symbol.annotations.firstNotNullOfOrNull {
            runIf(it.fqName(session)?.asString() == HasSchema::class.qualifiedName!!) {
                val argumentName = Name.identifier(HasSchema::schemaArg.name)
                val schemaArg = (it.findArgumentByName(argumentName) as FirLiteralExpression).value
                ObjectWithSchema((schemaArg as Number).toInt(), typeRef)
            }
        } ?: error("Annotate ${symbol} with @HasSchema")
    }
}

internal class ObjectWithSchema(val schemaArg: Int, val typeRef: ConeKotlinType)
