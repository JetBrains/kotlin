/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getAnnotationWithResolvedArgumentsByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.collectAllProperties
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.dataframe.annotations.HasSchema
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ColumnType
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColumnsResolver
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupBy
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ResolvedDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.DataFrameCallableId
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

fun <T> KotlinTypeFacade.interpret(
    functionCall: FirFunctionCall,
    processor: Interpreter<T>,
    additionalArguments: Map<String, Interpreter.Success<Any?>> = emptyMap(),
    reporter: InterpretationErrorReporter,
): Interpreter.Success<T>? {
    val refinedArguments: RefinedArguments = functionCall.collectArgumentExpressions()

    val defaultArguments = processor.expectedArguments().filter { it.defaultValue is Present }.map { it.name }.toSet() + THIS_CALL
    val actualValueArguments = refinedArguments.associateBy { it.name.identifier }.toSortedMap()
    val conflictingKeys = additionalArguments.keys intersect actualValueArguments.keys
    if (conflictingKeys.isNotEmpty()) {
        if (isTest) {
            interpretationFrameworkError("Conflicting keys: $conflictingKeys")
        }
        return null
    }
    val expectedArgsMap = processor.expectedArguments()
        .associateBy { it.name }.toSortedMap().minus(additionalArguments.keys)

    val typeArguments = buildMap {
        functionCall.typeArguments.forEachIndexed { index, firTypeProjection ->
            val key = "typeArg$index"
            val lens = expectedArgsMap[key]?.lens ?: return@forEachIndexed
            val value: Any = if (lens == Interpreter.Id) {
                firTypeProjection.toConeTypeProjection()
            } else {
                val type = firTypeProjection.toConeTypeProjection().type ?: session.builtinTypes.nullableAnyType.coneType
                if (type is ConeIntersectionType) return@forEachIndexed
                ColumnType(type)
            }
            put(key, Interpreter.Success(value))
        }
    }

    val unexpectedArguments =
        (expectedArgsMap.keys - defaultArguments) != (actualValueArguments.keys + typeArguments.keys - defaultArguments)
    if (unexpectedArguments) {
        if (isTest) {
            val message = buildString {
                appendLine("ERROR: Different set of arguments")
                appendLine("Implementation class: $processor")
                appendLine("Not found in actual: ${expectedArgsMap.keys - actualValueArguments.keys}")
                val diff = actualValueArguments.keys - expectedArgsMap.keys
                appendLine("Passed, but not expected: $diff")
                appendLine("add arguments to an interpeter:")
                appendLine(diff.map { actualValueArguments[it] })
            }
            interpretationFrameworkError(message)
        }
        return null
    }

    val arguments = mutableMapOf<String, Interpreter.Success<Any?>>()
    arguments += additionalArguments
    arguments[THIS_CALL] = Interpreter.Success(functionCall)
    arguments += typeArguments
    val interpretationResults = refinedArguments.refinedArguments.mapNotNull {
        val name = it.name.identifier
        val expectedArgument = expectedArgsMap[name] ?: error("$processor $name")
        val expectedReturnType = expectedArgument.klass
        val value: Interpreter.Success<Any?>? = when (expectedArgument.lens) {
            is Interpreter.Value -> {
                context(session) {
                    extractValue(it.expression, reporter)
                }
            }

            is Interpreter.ReturnType -> {
                val type = it.expression.resolvedType as? ConeClassLikeType
                type
                    ?.let {
                        if (type.isSomeFunctionType(session)) {
                            type.returnType(session)
                        } else {
                            type
                        }
                    }
                    ?.let { returnType -> Interpreter.Success(ColumnType(returnType)) }
            }

            is Interpreter.Dsl -> {
                { receiver: Any, dslArguments: Map<String, Interpreter.Success<Any?>> ->
                    val map = mapOf("dsl" to Interpreter.Success(receiver)) + dslArguments
                    (it.expression as FirAnonymousFunctionExpression)
                        .anonymousFunction.body!!
                        .statements.filterIsInstance<FirFunctionCall>()
                        .forEach { call ->
                            val schemaProcessor = call.loadInterpreter() ?: return@forEach
                            interpret(
                                call,
                                schemaProcessor,
                                map,
                                reporter
                            )
                        }
                }.let { Interpreter.Success(it) }
            }

            is Interpreter.Schema -> {
                assert(expectedReturnType.toString() == PluginDataFrameSchema::class.qualifiedName!!) {
                    "'$name' should be ${PluginDataFrameSchema::class.qualifiedName!!}, but plugin expect $expectedReturnType"
                }

                val objectWithSchema = it.expression.resolvedType.findSchemaArgument(isTest)
                if (objectWithSchema == null) {
                    reporter.doNotReportInterpretationError()
                    null
                } else {
                    Interpreter.Success(objectWithSchema.getSchema())
                }
            }

            is Interpreter.GroupBy -> {
                assert(expectedReturnType.toString() == GroupBy::class.qualifiedName!!) {
                    "'$name' should be ${GroupBy::class.qualifiedName!!}, but plugin expect $expectedReturnType"
                }
                // ok for ReducedGroupBy too
                val resolvedType = it.expression.resolvedType.fullyExpandedType()
                val keys = pluginDataFrameSchema(resolvedType.typeArguments[0])
                val groups = pluginDataFrameSchema(resolvedType.typeArguments[1])
                Interpreter.Success(GroupBy(keys, groups))
            }

            is Interpreter.Id -> {
                Interpreter.Success(it.expression)
            }
        }
        value?.let { value1 -> it.name.identifier to value1 }
    }

    return if (interpretationResults.size == refinedArguments.refinedArguments.size) {
        arguments.putAll(interpretationResults)
        when (val res = processor.interpret(arguments, this)) {
            is Interpreter.Success -> res
            is Interpreter.Error -> {
                reporter.reportInterpretationError(functionCall, res.message ?: "")
                null
            }
        }
    } else {
        null
    }
}

context(session: FirSession)
private fun KotlinTypeFacade.extractValue(
    expression: FirExpression?,
    reporter: InterpretationErrorReporter,
): Interpreter.Success<Any?>? = when (expression) {
    is FirLiteralExpression -> Interpreter.Success(expression.value!!)
    is FirVarargArgumentsExpression -> {
        val args = expression.arguments.map {
            when (it) {
                is FirLiteralExpression -> it.value

                is FirFunctionCall -> {
                    it.loadInterpreter()?.let { processor ->
                        interpret(it, processor, emptyMap(), reporter)
                    }
                }

                else -> extractValue(it, reporter)
            }

        }
        Interpreter.Success(args)
    }

    is FirFunctionCall -> {
        val interpreter = expression.loadInterpreter()
        if (interpreter == null) {
            // if the plugin already transformed call, its original form is the last expression of .let { }
            val argument = expression.arguments.getOrNull(0)
            val last = (argument as? FirAnonymousFunctionExpression)?.anonymousFunction?.body?.statements?.lastOrNull()
            val call = (last as? FirReturnExpression)?.result as? FirFunctionCall
            call?.loadInterpreter()?.let {
                interpret(call, it, emptyMap(), reporter)
            }
        } else {
            interpreter.let {
                val result = interpret(expression, interpreter, emptyMap(), reporter)
                result
            }
        }
    }

    is FirPropertyAccessExpression -> {
        (expression.calleeReference as? FirResolvedNamedReference)?.let {
            val symbol = it.resolvedSymbol
            val firPropertySymbol = symbol as? FirPropertySymbol
            val literalInitializer = firPropertySymbol?.resolvedInitializer

            if (symbol is FirEnumEntrySymbol) {
                Interpreter.Success(
                    DataFrameCallableId(
                        packageName = symbol.callableId.packageName.asString(),
                        className = symbol.callableId.className!!.asString(),
                        callableName = symbol.callableId.callableName.asString()
                    )
                )
            } else if (literalInitializer != null) {
                extractValue(literalInitializer, reporter)
            } else {
                Interpreter.Success(columnWithPathApproximations(expression))
            }
        }
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

            is FirLiteralExpression -> result.value

            else -> null
        }
        col?.let { Interpreter.Success(it) }
    }

    else -> null
}

fun interpretationFrameworkError(message: String): Nothing = throw InterpretationFrameworkError(message)

class InterpretationFrameworkError(message: String) : Error(message)

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

context(sessionHolder: SessionHolder)
fun pluginDataFrameSchema(schemaTypeArg: ConeTypeProjection): PluginDataFrameSchema {
    val schema = if (schemaTypeArg.isStarProjection) {
        PluginDataFrameSchema.EMPTY
    } else {
        val coneClassLikeType = schemaTypeArg.type as? ConeClassLikeType ?: return PluginDataFrameSchema.EMPTY
        pluginDataFrameSchema(coneClassLikeType)
    }
    return schema
}

context(sessionHolder: SessionHolder)
fun pluginDataFrameSchema(coneClassLikeType: ConeClassLikeType): PluginDataFrameSchema {
    val symbol = coneClassLikeType.toSymbol() as? FirRegularClassSymbol ?: return PluginDataFrameSchema.EMPTY
    val anyType = sessionHolder.session.builtinTypes.anyType.coneType
    val declarationSymbols = if (symbol.isLocal && symbol.resolvedSuperTypes.firstOrNull() != anyType) {
        val rootSchemaSymbol = symbol.resolvedSuperTypes.first().toSymbol() as? FirRegularClassSymbol
        rootSchemaSymbol?.declaredMemberScope(sessionHolder.session, FirResolvePhase.DECLARATIONS)
    } else {
        symbol.unsubstitutedScope(sessionHolder.session, ScopeSession(), false, FirResolvePhase.DECLARATIONS)
    }.let { scope ->
        val names = scope?.getCallableNames() ?: emptySet()
        names.flatMap { scope?.getProperties(it) ?: emptyList() }
    }

    val mapping = symbol.typeParameterSymbols
        .mapIndexed { i, symbol -> symbol to coneClassLikeType.typeArguments[i] }
        .toMap()

    val propertySymbols = declarationSymbols
        .filterIsInstance<FirPropertySymbol>()
        .sortPropertiesByOrderAnnotation()

    val columns = propertySymbols.mapNotNull { propertySymbol ->
        columnOf(propertySymbol, mapping)
    }

    return PluginDataFrameSchema(columns)
}

context(sessionHolder: SessionHolder)
private fun List<FirPropertySymbol>.sortPropertiesByOrderAnnotation(): List<FirPropertySymbol> {
    var result = this
    val annotations = result.mapNotNull {
        val orderArgument = it.getAnnotationByClassId(
            Names.ORDER_ANNOTATION,
            sessionHolder.session
        )?.argumentMapping?.mapping?.get(Names.ORDER_ARGUMENT)
        (orderArgument as? FirLiteralExpression)?.value as? Int
    }
    if (result.size == annotations.size) {
        result = result.zip(annotations).sortedBy { it.second }.map { it.first }
    }
    return result
}

context(session: FirSession)
private fun KotlinTypeFacade.columnWithPathApproximations(propertyAccess: FirPropertyAccessExpression): ColumnsResolver {
    return propertyAccess.resolvedType.let {
        val column = when (it.classId) {
            Names.DATA_COLUMN_CLASS_ID -> {
                val type = when (val arg = it.typeArguments.single()) {
                    is ConeStarProjection -> session.builtinTypes.nullableAnyType.coneType
                    else -> arg as ConeClassLikeType
                }
                simpleColumnOf(propertyAccess.columnName(), type)
            }
            Names.COLUM_GROUP_CLASS_ID -> {
                val arg = it.typeArguments.single()
                val name = propertyAccess.columnName()
                SimpleColumnGroup(name, pluginDataFrameSchema(arg).columns())
            }
            else -> return object : ColumnsResolver {
                override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                    return emptyList()
                }
            }
        }
        ResolvedDataColumn(
            ColumnWithPathApproximation(
                path = ColumnPath(path(propertyAccess)),
                column
            )
        )
    }
}

context(sessionHolder: SessionHolder)
private fun columnOf(it: FirPropertySymbol, mapping: Map<FirTypeParameterSymbol, ConeTypeProjection>): SimpleCol? {
    val annotation = it.getAnnotationByClassId(Names.COLUMN_NAME_ANNOTATION, sessionHolder.session)
    val columnName = (annotation?.argumentMapping?.mapping?.get(Names.COLUMN_NAME_ARGUMENT) as? FirLiteralExpression)?.value as? String
    val name = columnName ?: it.name.identifier
    return when {
        shouldBeConvertedToFrameColumn(it) -> {
            val nestedColumns = it.resolvedReturnType.typeArguments[0].type
                ?.toRegularClassSymbol()
                ?.declaredMemberScope(sessionHolder.session, FirResolvePhase.DECLARATIONS)
                ?.collectAllProperties()
                ?.filterIsInstance<FirPropertySymbol>()
                ?.sortPropertiesByOrderAnnotation()
                ?.mapNotNull { columnOf(it, mapping) }
                ?: emptyList()

            SimpleFrameColumn(name, nestedColumns)
        }

        shouldBeConvertedToColumnGroup(it) -> {
            val type = if (isDataRow(it)) it.resolvedReturnType.typeArguments[0].type!! else it.resolvedReturnType
            val nestedColumns = type
                .toRegularClassSymbol()
                ?.declaredMemberScope(sessionHolder.session, FirResolvePhase.DECLARATIONS)
                ?.collectAllProperties()
                ?.filterIsInstance<FirPropertySymbol>()
                ?.sortPropertiesByOrderAnnotation()
                ?.mapNotNull { columnOf(it, mapping) }
                ?: emptyList()
            SimpleColumnGroup(name, nestedColumns)
        }

        else -> {
            val type = when (val type = it.resolvedReturnType) {
                is ConeTypeParameterType -> {
                    val projection = mapping[type.lookupTag.typeParameterSymbol]
                    if (projection is ConeStarProjection) {
                        type.lookupTag.typeParameterSymbol.resolvedBounds.singleOrNull()?.coneType
                    } else {
                        projection as? ConeKotlinType
                    }
                }

                else -> type
            }
            type?.let { type ->
                SimpleDataColumn(name, ColumnType(type))
            }
        }
    }
}

context(sessionHolder: SessionHolder)
private fun shouldBeConvertedToColumnGroup(it: FirPropertySymbol) =
    isDataRow(it) ||
            it.resolvedReturnType.toRegularClassSymbol()?.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, sessionHolder.session) == true

private fun isDataRow(it: FirPropertySymbol) =
    it.resolvedReturnType.classId == Names.DATA_ROW_CLASS_ID

context(sessionHolder: SessionHolder)
private fun shouldBeConvertedToFrameColumn(it: FirPropertySymbol) =
    isDataFrame(it) ||
            (it.resolvedReturnType.classId == Names.LIST &&
                    it.resolvedReturnType.typeArguments[0].type?.toRegularClassSymbol()
                        ?.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, sessionHolder.session) == true)

private fun isDataFrame(it: FirPropertySymbol) =
    it.resolvedReturnType.classId == Names.DF_CLASS_ID

context(session: FirSession)
fun path(propertyAccessExpression: FirPropertyAccessExpression): List<String> {
    val colName = propertyAccessExpression.columnName()
    val typeRef = propertyAccessExpression.dispatchReceiver?.resolvedType
    val joinDsl = ClassId(FqName("org.jetbrains.kotlinx.dataframe.api"), Name.identifier("JoinDsl"))
    if (typeRef?.classId?.equals(joinDsl) == true && colName == "right") {
        return emptyList()
    }
    return when (val explicitReceiver = propertyAccessExpression.explicitReceiver) {
        null, is FirThisReceiverExpression -> listOf(colName)
        else -> {
            val propertyAccess = explicitReceiver as FirPropertyAccessExpression
            if (propertyAccess.calleeReference.symbol is FirValueParameterSymbol) {
                listOf(colName)
            } else {
                path(propertyAccess) + colName
            }
        }
    }
}

context(session: FirSession)
fun FirPropertyAccessExpression.columnName(): String {
    val name = toResolvedCallableSymbol()?.name
    val columnName =
        extensionReceiver?.resolvedType?.typeArguments?.getOrNull(0)?.type?.toRegularClassSymbol(session)
            ?.declaredProperties(session)
            ?.firstOrNull { it.name == name }
            ?.let {
                val expression = it.getAnnotationWithResolvedArgumentsByClassId(Names.COLUMN_NAME_ANNOTATION, session)
                    ?.argumentMapping?.mapping[Names.COLUMN_NAME_ARGUMENT] as? FirLiteralExpression
                expression?.value as? String
            }
    return columnName ?: calleeReference.resolved!!.name.identifier
}

internal fun FirFunctionCall.collectArgumentExpressions(): RefinedArguments {
    val refinedArgument = mutableListOf<RefinedArgument>()

    val parameterName = Name.identifier("receiver")
    (explicitReceiver ?: extensionReceiver)?.let {
        if (it is FirResolvedQualifier && it.resolvedToCompanionObject) {
            return@let
        }
        refinedArgument += RefinedArgument(parameterName, it)
    }

    (argumentList as FirResolvedArgumentList).mapping.forEach { (expression, parameter) ->
        refinedArgument += RefinedArgument(parameter.name, expression)
    }
    return RefinedArguments(refinedArgument)
}

context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.findSchemaArgument(isTest: Boolean): ObjectWithSchema? {
    return toSymbol()?.let {
        val (typeRef: ConeKotlinType, symbol) = if (it is FirTypeAliasSymbol) {
            it.resolvedExpandedTypeRef.coneType to it.resolvedExpandedTypeRef.toClassLikeSymbol(sessionHolder.session)!!
        } else {
            this to it
        }
        val objectWithSchema = symbol.resolvedAnnotationsWithArguments.firstNotNullOfOrNull {
            runIf(it.fqName(sessionHolder.session)?.asString() == HasSchema::class.qualifiedName!!) {
                val argumentName = Name.identifier(HasSchema::schemaArg.name)
                val schemaArg = (it.findArgumentByName(argumentName) as FirLiteralExpression).value
                ObjectWithSchema((schemaArg as Number).toInt(), typeRef)
            }
        }
        if (objectWithSchema == null && isTest) {
            error("Annotate $symbol with @HasSchema")
        }
        objectWithSchema
    }
}

private const val THIS_CALL = "functionCall"

internal class ObjectWithSchema(val schemaArg: Int, val annotatedType: ConeKotlinType)

context(sessionHolder: SessionHolder)
internal fun ObjectWithSchema.getSchema(): PluginDataFrameSchema {
    val arg = schemaArg
    val schemaTypeArg = (annotatedType as ConeClassLikeType).typeArguments[arg]
    val schema = pluginDataFrameSchema(schemaTypeArg)
    return schema
}

