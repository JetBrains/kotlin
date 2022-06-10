/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol

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
        val processor = findSchemaProcessor(functionCall) ?: return emptyList()

        val dataFrameSchema = interpret(functionCall, processor)

        val properties = dataFrameSchema.columns().map {
            val typeApproximation = when (val type = it.type) {
                is TypeApproximationImpl -> type
                ColumnGroupTypeApproximation -> TODO("support column groups in data schema")
            }
            val type = ConeClassLikeLookupTagImpl(ClassId.topLevel(FqName(typeApproximation.fqName))).constructType(
                emptyArray(), false
            )
            SchemaProperty(false, it.name(), type, callReturnType.typeArguments[0])
        }

        val id = ids.removeLast()
        state[id] = SchemaContext(properties, callReturnType.typeArguments[0])
        return listOf(ConeClassLikeLookupTagImpl(id).constructClassType(emptyArray(), isNullable = false))
    }

    private fun <T> interpret(functionCall: FirFunctionCall, processor: Interpreter<T>, additionalArguments: Map<String, Any> = emptyMap()): T {
        fun loadInterpreter(call: FirFunctionCall): Interpreter<*>? {
            val symbol =
                (call.calleeReference as FirResolvedNamedReference).resolvedSymbol as FirNamedFunctionSymbol
            val argName = Name.identifier("interpreter")
            return symbol.annotations
                .find { it.fqName(session)?.equals(INTERPRETABLE_FQNAME) ?: false }
                ?.let { annotation ->
                    (annotation.findArgumentByName(argName) as FirGetClassCall).classId.load<Interpreter<*>>()
                }

        }
        val refinedArguments: Arguments = functionCall.collectArgumentExpressions()
        val actualArgsMap = refinedArguments.associateBy { it.name.identifier }.toSortedMap()
        val expectedArgsMap = processor.expectedArguments.associateBy { it.name }.toSortedMap().minus(additionalArguments.keys)

        val context = buildString {
            appendLine("ERROR: Function argument annotations")
            appendLine("$processor")
            val parametersString = functionCall.argumentMapping?.map { it.value }?.joinToString { "${it.name}: ${it.returnTypeRef.coneType}" }
            appendLine("Function: ${functionCall.calleeReference.name.identifier}($parametersString)")
        }
        val argumentExpressions = refinedArguments.refinedArguments.map { it.getArgumentExpression(session, context) }

        if (expectedArgsMap.keys != actualArgsMap.keys) {
            val message = buildString {
                appendLine("ERROR: Different set of arguments")
                appendLine("Implementation class: $processor")
                appendLine("Not found in actual: ${expectedArgsMap.keys - actualArgsMap.keys}")
                appendLine("Make sure all arguments are annotated")
                val diff = actualArgsMap.keys - expectedArgsMap.keys
                appendLine("Passed, but not expected: ${diff}")
                appendLine("add arguments to an interpeter:")
                appendLine(diff.map { actualArgsMap[it] })
            }
            error(message)
        }

        val arguments = mutableMapOf<String, Any>()
        arguments += additionalArguments
        argumentExpressions.associateTo(arguments) {
            val name = it.parameterName.identifier
            val expectedReturnType = expectedArgsMap[name]!!.klass
            @Suppress("UNCHECKED_CAST") val value: Any = when (it.annotationName.identifier) {
                "Value" -> {
                    when (val expression = it.expression) {
                        is FirConstExpression<*> -> expression.value!!
                        is FirVarargArgumentsExpression -> {
                            expression.arguments.map { (it as FirConstExpression<*>).value }
                        }
                        else -> {
                            val call = expression as FirFunctionCall
                            val interpreter = loadInterpreter(call)
                                ?: TODO("receiver ${call.calleeReference} is not annotated with Interpretable. It can be DataFrame instance, but it's not supported rn")
                            interpret(expression, interpreter, emptyMap()) ?: error("allow interpreters to return null values")
                        }
                    }
                }
                "ReturnType" -> {
                    val returnType = it.expression.typeRef.coneType.returnType(session)
                    TypeApproximation(returnType.classId?.asFqNameString()!!, returnType.isNullable)
                }
                "Dsl" -> {
                    { receiver: Any ->
                        ((it.expression as FirLambdaArgumentExpression).expression as FirAnonymousFunctionExpression)
                            .anonymousFunction.body!!
                            .statements.filterIsInstance<FirFunctionCall>()
                            .forEach { call ->
                                val schemaProcessor = loadInterpreter(call) ?: return@forEach
                                interpret(call, schemaProcessor, mapOf("receiver" to receiver))
                            }
                    }
                }
                "Schema" -> {
                    assert(expectedReturnType.toString() == PluginDataFrameSchema::class.qualifiedName!!) {
                        "'$name' should be ${PluginDataFrameSchema::class.qualifiedName!!}, but plugin expect $expectedReturnType"
                    }

                    val arg = it.expression.getSchema().schemaArg
                    val schemaTypeArg = (it.expression.typeRef.coneType as ConeClassLikeType).typeArguments[arg]
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
                    }
                }
                else -> error(it.annotationName.identifier)
            }
            it.parameterName.identifier to value
        }
        return processor.interpret(arguments)
    }

    private inline fun <reified T> ClassId.load(): T {
        val constructor = Class.forName(asFqNameString())
            .constructors
            .firstOrNull { constructor -> constructor.parameterCount == 0 }
            ?: error("Interpreter $this must have an empty constructor")

        return constructor.newInstance() as T
    }

    fun FirExpression.getSchema(): ObjectWithSchema {
        return typeRef.coneTypeSafe<ConeClassLikeType>()!!.toSymbol(session)!!.let {
            it.annotations.firstNotNullOfOrNull {
                runIf(it.fqName(session)?.asString() == HasSchema::class.qualifiedName!!) {
                    val argumentName = Name.identifier(HasSchema::schemaArg.name)
                    @Suppress("UNCHECKED_CAST") val schemaArg = (it.findArgumentByName(argumentName) as FirConstExpression<Int>).value
                    ObjectWithSchema(schemaArg)
                }
            } ?: error("Annotate ${it} with @HasSchema")
        }
    }

    class ObjectWithSchema(val schemaArg: Int)

    private fun findSchemaProcessor(functionCall: FirFunctionCall): SchemaModificationInterpreter? {
        val annotation = (functionCall.calleeReference.resolvedSymbol as FirNamedFunctionSymbol).annotations.firstOrNull {
            val name1 = it.fqName(session)!!
            val name2 = FqName("org.jetbrains.kotlinx.dataframe.annotations.SchemaProcessor")
            name1 == name2
        } ?: return null

        val name = Name.identifier("processor")
        val getClassCall = (annotation.argumentMapping.mapping[name] as FirGetClassCall)
        return getClassCall.classId.load<SchemaModificationInterpreter>()
    }

    private val FirGetClassCall.classId: ClassId
        get() {
            return when (val argument = argument) {
                is FirResolvedQualifier -> argument.classId!!
                is FirClassReferenceExpression -> argument.classTypeRef.coneType.classId!!
                else -> error("")
            }
        }

    private fun FirFunctionCall.collectArgumentExpressions(): Arguments {
        val refinedArgument = mutableListOf<RefinedArgument>()


        functionSymbol().let { functionSymbol ->
            val interestingAnnotations = functionSymbol.annotations.mapNotNull {
                it.fqName(session)?.takeIf {
                    val interpreterAnnotationsNames = setOf(Interpretable::class.simpleName!!, SchemaProcessor::class.simpleName!!)
                    // interpreter annotations processed in different place:
                    // SchemaProcessor is loaded when return type of the function is DataFrame
                    // Interpretable is loaded when argument is @Value
                    it.startsWith(DF_ANNOTATIONS_PACKAGE) && it.shortName().identifier !in interpreterAnnotationsNames
                }?.let { fqName ->
                    it to fqName
                }
            }
            val parameterName = Name.identifier("this")
            refinedArgument += RefinedArgument(parameterName, interestingAnnotations.map { it.first }, explicitReceiver!!)
        }
//        ((explicitReceiver as FirFunctionCall).calleeReference as FirResolvedNamedReference).let {
//
//            val functionSymbol = it.resolvedSymbol as FirNamedFunctionSymbol
//            val interestingAnnotations = functionSymbol.annotations.mapNotNull {
//                it.fqName(session)?.takeIf { it.startsWith(DF_ANNOTATIONS_PACKAGE) && it.shortName().identifier != Interpretable::class.simpleName!! }
//            }
//            val parameterName = Name.identifier("this")
//            val argumentExpression = when (interestingAnnotations.size) {
//                0 -> null
//                1 -> ArgumentExpression(interestingAnnotations[0].shortName(), parameterName, explicitReceiver!!)
//                else -> error("receiver of ${this.calleeReference} has several lens annotations, but it's not supported: $interestingAnnotations")
//            }
//
//            refinedArgument += RefinedArgument(parameterName, functionSymbol.annotations, argumentExpression)
//        }

        (argumentList as FirResolvedArgumentList).mapping.forEach { (expression, parameter) ->
            val interestingAnnotations = parameter.annotations
                .mapNotNull {
                    it.fqName(session)?.takeIf { it.startsWith(DF_ANNOTATIONS_PACKAGE) }?.let { fqName ->
                        it to fqName
                    }
                }

            refinedArgument += RefinedArgument(parameter.name, interestingAnnotations.map { it.first }, expression)
        }
        return Arguments(refinedArgument)
    }

    object DataFramePluginKey : FirPluginKey()
}

fun FirFunctionCall.functionSymbol(): FirNamedFunctionSymbol {
    val firResolvedNamedReference = calleeReference as FirResolvedNamedReference
    return firResolvedNamedReference.resolvedSymbol as FirNamedFunctionSymbol
}

class Arguments(val refinedArguments: List<RefinedArgument>) : List<RefinedArgument> by refinedArguments

data class RefinedArgument(val name: Name, val interestingAnnotations: List<FirAnnotation>, val expression: FirExpression) {
    fun getArgumentExpression(session: FirSession, context: String): ArgumentExpression {
        return when (interestingAnnotations.size) {
            0 -> error("$context Parameter ${name} has no lens annotations, but it's not supported: $interestingAnnotations")
            1 -> ArgumentExpression(interestingAnnotations[0].fqName(session)!!.shortName(), name, expression)
            else -> error("$context \n Parameter ${name} has several lens annotations, but it's not supported: $interestingAnnotations")
        }
    }

    override fun toString(): String {
        return "RefinedArgument(name=$name, annotations=[${interestingAnnotations.joinToString { it.classId!!.asString() }}], expression=${expression})"
    }
}

data class ArgumentExpression(val annotationName: Name, val parameterName: Name, val expression: FirExpression)

data class SchemaProperty(val override: Boolean, val name: String, val type: ConeKotlinType, val coneTypeProjection: ConeTypeProjection)