/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolved
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.Marker
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.*

abstract class MyFirExpressionResolutionExtension(session: FirSession) : FirExpressionResolutionExtension(session), KotlinTypeFacade

fun <T> MyFirExpressionResolutionExtension.interpret(
    functionCall: FirFunctionCall,
    processor: Interpreter<T>,
    additionalArguments: Map<String, Interpreter.Success<Any?>> = emptyMap()
): Interpreter.Success<T>? {

    val refinedArguments: Arguments = functionCall.collectArgumentExpressions()

    val defaultArguments =  processor.expectedArguments.filter { it.defaultValue is Present }.map { it.name }.toSet()
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
        error(message)
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
                    is FirConstExpression<*> -> Interpreter.Success(expression.value!!)
                    is FirVarargArgumentsExpression -> {
                        val args = expression.arguments.map {
                            when (it) {
                                is FirConstExpression<*> -> it.value
                                is FirCallableReferenceAccess -> {
                                    toKPropertyApproximation(it, session)
                                }
                                else -> TODO(it::class.toString())
                            }

                        }
                        Interpreter.Success(args)
                    }
                    is FirFunctionCall -> {
                        val interpreter = expression.loadInterpreter()
                            ?: TODO("receiver ${expression.calleeReference} is not annotated with Interpretable. It can be DataFrame instance, but it's not supported rn")
                        interpret(expression, interpreter, emptyMap())
                    }
                    is FirPropertyAccessExpression -> {
                        (expression.calleeReference as? FirResolvedNamedReference)?.let {
                            val symbol = it.resolvedSymbol
                            if (symbol is FirEnumEntrySymbol) {
                                Interpreter.Success(
                                    DataFrameCallableId(
                                        packageName = symbol.callableId.packageName.asString(),
                                        className = symbol.callableId.className!!.asString(),
                                        callableName = symbol.callableId.callableName.asString()
                                    )
                                )
                            } else {
                                Interpreter.Success(extracted(expression))
                            }
                        }
                    }
                    is FirCallableReferenceAccess -> {
                        Interpreter.Success(toKPropertyApproximation(expression, session))
                    }
                    is FirLambdaArgumentExpression -> {
                        val col: List<ColumnWithPathApproximation> = when (val lambda = expression.expression) {
                            is FirAnonymousFunctionExpression -> {
                                val result = (lambda.anonymousFunction.body!!.statements.last() as FirReturnExpression).result
                                when (result) {
                                    is FirPropertyAccessExpression -> {
                                        extracted(result)
                                    }
                                    is FirFunctionCall -> {
                                        val interpreter = result.loadInterpreter() ?: TODO("")
                                        @Suppress("UNCHECKED_CAST")
                                        interpret(result, interpreter)?.value as List<ColumnWithPathApproximation>
                                    }
                                    else -> TODO(result::class.toString())
                                }
                            }
                            else -> TODO(lambda::class.toString())
                        }
                        Interpreter.Success(col)
                    }
                    else -> TODO(expression::class.toString())
                }
            }

            is Interpreter.ReturnType -> {
                val returnType = it.expression.typeRef.coneType.returnType(session)
                Interpreter.Success(TypeApproximationImpl(returnType.classId?.asFqNameString()!!, returnType.isNullable))
            }

            is Interpreter.Dsl -> {
                { receiver: Any ->
                    ((it.expression as FirLambdaArgumentExpression).expression as FirAnonymousFunctionExpression)
                        .anonymousFunction.body!!
                        .statements.filterIsInstance<FirFunctionCall>()
                        .forEach { call ->
                            val schemaProcessor = call.loadInterpreter() ?: return@forEach
                            interpret(call, schemaProcessor, mapOf("dsl" to Interpreter.Success(receiver)))
                        }
                }.let { Interpreter.Success(it) }
            }

            is Interpreter.Schema -> {
                assert(expectedReturnType.toString() == PluginDataFrameSchema::class.qualifiedName!!) {
                    "'$name' should be ${PluginDataFrameSchema::class.qualifiedName!!}, but plugin expect $expectedReturnType"
                }

                val objectWithSchema = it.expression.getSchema()
                val arg = objectWithSchema.schemaArg
                val schemaTypeArg = (objectWithSchema.typeRef.coneType as ConeClassLikeType).typeArguments[arg]
                if (schemaTypeArg.isStarProjection) {
                    PluginDataFrameSchema(emptyList())
                } else {
                    val declarationSymbols =
                        ((schemaTypeArg.type as ConeClassLikeType).toSymbol(session) as FirRegularClassSymbol).declarationSymbols
                    val columns = declarationSymbols.filterIsInstance<FirPropertySymbol>().map { propertySymbol ->
                        columnOf(propertySymbol)
                    }
                    PluginDataFrameSchema(columns)
                }.let { Interpreter.Success(it) }
            }
        }
        value?.let { value1 -> it.name.identifier to value1 }
    }

    functionCall.typeArguments.forEachIndexed { index, firTypeProjection ->
        val type = firTypeProjection.toConeTypeProjection().type ?: session.builtinTypes.nullableAnyType.type
        if (type is ConeIntersectionType) return@forEachIndexed
        val approximation = TypeApproximationImpl(
            type.classId!!.asFqNameString(),
            type.isMarkedNullable
        )
        arguments["typeArg$index"] = Interpreter.Success(approximation)
    }

    return if (interpretationResults.size == refinedArguments.refinedArguments.size) {
        arguments.putAll(interpretationResults)
        when (val res = processor.interpret(arguments, this)) {
            is Interpreter.Success -> res
            is Interpreter.Error -> {
                error(res.message.toString())
            }
        }
    } else {
        error("")
    }
}

private fun KotlinTypeFacade.extracted(result: FirPropertyAccessExpression): List<ColumnWithPathApproximation> {
    return (result.typeRef as FirResolvedTypeRef).type.let {
        val column = when {
            it.classId == Names.DATA_COLUMN_CLASS_ID -> {
                val arg = it.typeArguments.single() as ConeClassLikeType
                when {
                    arg.classId == Names.DF_CLASS_ID -> TODO()
                    else -> SimpleCol(
                        f(result),
                        TypeApproximationImpl(arg.lookupTag.classId.asFqNameString(), arg.isNullable)
                    )
                }
            }

            it.classId == Names.COLUM_GROUP_CLASS_ID -> TODO()
            else -> TODO()
        }
        listOf(
            ColumnWithPathApproximation(
                path = ColumnPathApproximation(path(result)),
                column
            )
        )
    }
}


private fun MyFirExpressionResolutionExtension.columnOf(it: FirPropertySymbol): SimpleCol =
    when (it.resolvedReturnType.classId) {
        Names.DF_CLASS_ID -> {
            val nestedColumns = it.resolvedReturnType.typeArguments[0].type
                ?.toRegularClassSymbol(session)
                ?.declarationSymbols
                ?.filterIsInstance<FirPropertySymbol>()
                ?.map { columnOf(it) }
                ?: emptyList()

            SimpleFrameColumn(it.name.identifier, nestedColumns, false, anyDataFrame)
        }
        Names.DATA_ROW_CLASS_ID -> {
            val nestedColumns = it.resolvedReturnType.typeArguments[0].type
                ?.toRegularClassSymbol(session)
                ?.declarationSymbols
                ?.filterIsInstance<FirPropertySymbol>()
                ?.map { columnOf(it) }
                ?: emptyList()
            SimpleColumnGroup(it.name.identifier, nestedColumns, anyRow)
        }

        else -> SimpleCol(
            it.name.identifier, TypeApproximation(it.resolvedReturnType)
        )
    }

fun path(propertyAccessExpression: FirPropertyAccessExpression): List<String> {
    val colName = f(propertyAccessExpression)
    return when (val explicitReceiver = propertyAccessExpression.explicitReceiver) {
        null -> listOf(colName)
        else -> path(explicitReceiver as FirPropertyAccessExpression) + colName
    }
}

fun f(propertyAccessExpression: FirPropertyAccessExpression): String {
    return propertyAccessExpression.calleeReference.resolved!!.name.identifier
}

private fun MyFirExpressionResolutionExtension.toKPropertyApproximation(firCallableReferenceAccess: FirCallableReferenceAccess, session: FirSession): KPropertyApproximation {
    val propertyName = firCallableReferenceAccess.calleeReference.name.identifier
    return (firCallableReferenceAccess.calleeReference as FirResolvedCallableReference).let {
        val symbol = it.toResolvedCallableSymbol()!!
        val columnName = symbol.annotations
            .find { it.fqName(session)!!.asString() == ColumnName::class.qualifiedName!! }
            ?.let {
                (it.argumentMapping.mapping[Name.identifier(ColumnName::name.name)] as FirConstExpression<*>).value as String
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
        refinedArgument += RefinedArgument(parameterName, it)
    }

    (argumentList as FirResolvedArgumentList).mapping.forEach { (expression, parameter) ->
        refinedArgument += RefinedArgument(parameter.name, expression)
    }
    return Arguments(refinedArgument)
}

internal val FirExpressionResolutionExtension.getSchema: FirExpression.() -> ObjectWithSchema get() = { getSchema(session) }

internal fun FirExpression.getSchema(session: FirSession): ObjectWithSchema {
    return typeRef.toClassLikeSymbol(session)!!.let {
        val (typeRef, symbol) = if (it is FirTypeAliasSymbol) {
            it.resolvedExpandedTypeRef to it.resolvedExpandedTypeRef.toClassLikeSymbol(session)!!
        } else {
            typeRef to it
        }
        symbol.annotations.firstNotNullOfOrNull {
            runIf(it.fqName(session)?.asString() == HasSchema::class.qualifiedName!!) {
                val argumentName = Name.identifier(HasSchema::schemaArg.name)
                @Suppress("UNCHECKED_CAST") val schemaArg = (it.findArgumentByName(argumentName) as FirConstExpression<Int>).value
                ObjectWithSchema(schemaArg, typeRef)
            }
        } ?: error("Annotate ${symbol} with @HasSchema")
    }
}

internal class ObjectWithSchema(val schemaArg: Int, val typeRef: FirTypeRef)
