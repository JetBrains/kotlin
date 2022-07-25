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
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.DataFrameCallableId
import org.jetbrains.kotlinx.dataframe.plugin.KPropertyApproximation
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol

fun <T> FirExpressionResolutionExtension.interpret(
    functionCall: FirFunctionCall,
    processor: Interpreter<T>,
    additionalArguments: Map<String, Interpreter.Success<Any?>> = emptyMap()
): Interpreter.Success<T>? {

    val refinedArguments: Arguments = functionCall.collectArgumentExpressions()
    val actualArgsMap = refinedArguments.associateBy { it.name.identifier }.toSortedMap()
    val expectedArgsMap = processor.expectedArguments
        .filter { it.defaultValue is Absent }
        .associateBy { it.name }.toSortedMap().minus(additionalArguments.keys)

    if (expectedArgsMap.keys != actualArgsMap.keys) {
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
                        Interpreter.Success(expression.arguments.map { (it as FirConstExpression<*>).value })
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
                                TODO(expression::class.toString())
                            }
                        }
                    }
                    is FirCallableReferenceAccess -> {
                        val propertyName = expression.calleeReference.name.identifier
                        (expression.calleeReference as FirResolvedCallableReference).let {
                            val symbol = it.toResolvedCallableSymbol()!!
                            val columnName = symbol.annotations
                                .find { it.fqName(session)!!.asString() == ColumnName::class.qualifiedName!! }
                                ?.let {
                                    (it.argumentMapping.mapping[Name.identifier(ColumnName::name.name)] as FirConstExpression<*>).value as String
                                }
                            val kotlinType = symbol.resolvedReturnTypeRef.type
                            val type = kotlinType.classId?.asFqNameString()!!
                            Interpreter.Success(KPropertyApproximation(columnName ?: propertyName, TypeApproximation(type, kotlinType.isNullable)))
                        }
                    }
                    else -> TODO(expression::class.toString())
                }
            }

            is Interpreter.ReturnType -> {
                val returnType = it.expression.typeRef.coneType.returnType(session)
                Interpreter.Success(TypeApproximation(returnType.classId?.asFqNameString()!!, returnType.isNullable))
            }

            is Interpreter.Dsl -> {
                { receiver: Any ->
                    ((it.expression as FirLambdaArgumentExpression).expression as FirAnonymousFunctionExpression)
                        .anonymousFunction.body!!
                        .statements.filterIsInstance<FirFunctionCall>()
                        .forEach { call ->
                            val schemaProcessor = call.loadInterpreter() ?: return@forEach
                            interpret(call, schemaProcessor, mapOf("receiver" to Interpreter.Success(receiver)))
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
                    val columns = declarationSymbols.filterIsInstance<FirPropertySymbol>().map {
                        SimpleCol(
                            it.name.identifier, TypeApproximationImpl(
                                it.resolvedReturnType.classId!!.asFqNameString(),
                                it.resolvedReturnType.isNullable
                            )
                        )
                    }
                    PluginDataFrameSchema(columns)
                }.let { Interpreter.Success(it) }
            }
        }
        value?.let { value1 -> it.name.identifier to value1 }
    }


    return if (interpretationResults.size == refinedArguments.refinedArguments.size) {
        arguments.putAll(interpretationResults)
        when (val res = processor.interpret(arguments)) {
            is Interpreter.Success -> res
            is Interpreter.Error -> {
                error(res.message.toString())
            }
        }
    } else {
        error("")
    }
}

internal fun FirFunctionCall.collectArgumentExpressions(): Arguments {
    val refinedArgument = mutableListOf<RefinedArgument>()

    val parameterName = Name.identifier("this")
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
