package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.collectAllProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.List
import org.jetbrains.kotlinx.dataframe.codeGen.FieldKind
import org.jetbrains.kotlinx.dataframe.plugin.classId
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names.DATA_ROW_CLASS_ID
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names.DATA_SCHEMA_CLASS_ID
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names.DF_CLASS_ID
import java.util.*

class ToDataFrameDsl : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: FirExpression? by arg(lens = Interpreter.Id)
    val Arguments.body by dsl()
    val Arguments.typeArg0: ConeTypeProjection? by arg(lens = Interpreter.Id)

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val dsl = CreateDataFrameDslImplApproximation()
        body(dsl, mapOf("typeArg0" to Interpreter.Success(typeArg0)))
        return PluginDataFrameSchema(dsl.columns)
    }
}

class ToDataFrame : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: FirExpression? by arg(lens = Interpreter.Id)
    val Arguments.maxDepth: Number by arg(defaultValue = Present(DEFAULT_MAX_DEPTH))
    val Arguments.typeArg0: ConeTypeProjection by arg(lens = Interpreter.Id)

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return toDataFrame(maxDepth.toInt(), typeArg0, TraverseConfiguration())
    }
}

class ToDataFrameDefault : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: FirExpression? by arg(lens = Interpreter.Id)
    val Arguments.typeArg0: ConeTypeProjection by arg(lens = Interpreter.Id)

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return toDataFrame(DEFAULT_MAX_DEPTH, typeArg0, TraverseConfiguration())
    }
}

class ToDataFrameColumn : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: FirExpression? by arg(lens = Interpreter.Id)
    val Arguments.typeArg0 by type()
    val Arguments.columnName: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(listOf(simpleColumnOf(columnName, typeArg0.type)))
    }
}

private const val DEFAULT_MAX_DEPTH = 0

class Properties0 : AbstractInterpreter<Unit>() {
    val Arguments.dsl: CreateDataFrameDslImplApproximation by arg()
    val Arguments.maxDepth: Int by arg()
    val Arguments.body by dsl()
    val Arguments.typeArg0: ConeTypeProjection by arg(lens = Interpreter.Id)

    override fun Arguments.interpret() {
        dsl.configuration.maxDepth = maxDepth
        body(dsl.configuration.traverseConfiguration, emptyMap())
        val schema = toDataFrame(dsl.configuration.maxDepth, typeArg0, dsl.configuration.traverseConfiguration)
        dsl.columns.addAll(schema.columns())
    }
}

class ToDataFrameDslStringInvoke : AbstractInterpreter<Unit>() {
    val Arguments.dsl: CreateDataFrameDslImplApproximation by arg()
    val Arguments.receiver: String by arg()
    val Arguments.builder by dsl()

    override fun Arguments.interpret() {
        val addDsl = CreateDataFrameDslImplApproximation()
        builder(addDsl, emptyMap())
        dsl.columns.add(SimpleColumnGroup(receiver, addDsl.columns))
    }
}

class CreateDataFrameConfiguration {
    var maxDepth = DEFAULT_MAX_DEPTH
    var traverseConfiguration: TraverseConfiguration = TraverseConfiguration()
}

class TraverseConfiguration {
    val excludeProperties = mutableSetOf<FirCallableReferenceAccess>()
    val excludeClasses = mutableSetOf<FirGetClassCall>()
    val preserveClasses = mutableSetOf<FirGetClassCall>()
    val preserveProperties = mutableSetOf<FirCallableReferenceAccess>()
}

class Preserve0 : AbstractInterpreter<Unit>() {
    val Arguments.dsl: TraverseConfiguration by arg()
    val Arguments.classes: FirVarargArgumentsExpression by arg(lens = Interpreter.Id)

    override fun Arguments.interpret() {
        dsl.preserveClasses.addAll(classes.arguments.filterIsInstance<FirGetClassCall>())
    }
}

class Preserve1 : AbstractInterpreter<Unit>() {
    val Arguments.dsl: TraverseConfiguration by arg()
    val Arguments.properties: FirVarargArgumentsExpression by arg(lens = Interpreter.Id)

    override fun Arguments.interpret() {
        dsl.preserveProperties.addAll(properties.arguments.filterIsInstance<FirCallableReferenceAccess>())
    }
}

class Exclude0 : AbstractInterpreter<Unit>() {
    val Arguments.dsl: TraverseConfiguration by arg()
    val Arguments.classes: FirVarargArgumentsExpression by arg(lens = Interpreter.Id)

    override fun Arguments.interpret() {
        dsl.excludeClasses.addAll(classes.arguments.filterIsInstance<FirGetClassCall>())
    }
}

class Exclude1 : AbstractInterpreter<Unit>() {
    val Arguments.dsl: TraverseConfiguration by arg()
    val Arguments.properties: FirVarargArgumentsExpression by arg(lens = Interpreter.Id)

    override fun Arguments.interpret() {
        dsl.excludeProperties.addAll(properties.arguments.filterIsInstance<FirCallableReferenceAccess>())
    }
}

@Suppress("INVISIBLE_MEMBER")

@OptIn(SymbolInternals::class)
internal fun KotlinTypeFacade.toDataFrame(
    maxDepth: Int,
    arg: ConeTypeProjection,
    traverseConfiguration: TraverseConfiguration,
): PluginDataFrameSchema {
    val excludes =
        traverseConfiguration.excludeProperties.mapNotNullTo(mutableSetOf()) { it.calleeReference.toResolvedPropertySymbol() }
    val excludedClasses = traverseConfiguration.excludeClasses.mapTo(mutableSetOf()) { it.argument.resolvedType }
    val preserveClasses = traverseConfiguration.preserveClasses.mapNotNullTo(mutableSetOf()) { it.classId }
    val preserveProperties =
        traverseConfiguration.preserveProperties.mapNotNullTo(mutableSetOf()) { it.calleeReference.toResolvedPropertySymbol() }

    fun convert(classLike: ConeKotlinType, depth: Int, makeNullable: Boolean): List<SimpleCol> {
        val symbol = classLike.toRegularClassSymbol(session) ?: return emptyList()
        val scope = symbol.unsubstitutedScope(session, ScopeSession(), false, FirResolvePhase.STATUS)
        val declarations = if (symbol.fir is FirJavaClass) {
            scope
                .collectAllFunctions()
                .filter { !it.isStatic && it.valueParameterSymbols.isEmpty() && it.typeParameterSymbols.isEmpty() }
                .mapNotNull { function ->
                    val name = function.name.identifier
                    if (name.startsWith("get") || name.startsWith("is")) {
                        val propertyName = name
                            .replaceFirst("get", "")
                            .replaceFirst("is", "")
                            .let {
                                if (it.firstOrNull()?.isUpperCase() == true) {
                                    it.replaceFirstChar { it.lowercase(Locale.getDefault()) }
                                } else {
                                    null
                                }
                            }
                        propertyName?.let { function to it }
                    } else {
                        null
                    }
                }
        } else {
            scope
                .collectAllProperties()
                .filterIsInstance<FirPropertySymbol>()
                .map {
                    it to it.name.identifier
                }
        }

        return declarations
            .filterNot { excludes.contains(it.first) }
            .filterNot { excludedClasses.contains(it.first.resolvedReturnType) }
            .filter { it.first.visibility == Visibilities.Public }
            .map { (it, name) ->
                var returnType = it.fir.returnTypeRef.resolveIfJavaType(session, JavaTypeParameterStack.EMPTY, null)
                    .coneType.upperBoundIfFlexible()

                returnType = if (returnType is ConeTypeParameterType) {
                    if (returnType.canBeNull(session)) {
                        session.builtinTypes.nullableAnyType.coneType
                    } else {
                        session.builtinTypes.anyType.coneType
                    }
                } else {
                    returnType.withArguments {
                        val type = it.type
                        if (type is ConeTypeParameterType) {
                            session.builtinTypes.nullableAnyType.coneType
                        } else {
                            type?.upperBoundIfFlexible() ?: it
                        }
                    }
                }

                val fieldKind = returnType.getFieldKind(session)

                val keepSubtree =
                    depth >= maxDepth && !fieldKind.shouldBeConvertedToColumnGroup && !fieldKind.shouldBeConvertedToFrameColumn
                if (keepSubtree || returnType.isValueType(session) || returnType.classId in preserveClasses || it in preserveProperties) {
                    SimpleDataColumn(
                        name,
                        TypeApproximation(
                            returnType.withNullability(
                                makeNullable,
                                session.typeContext
                            )
                        )
                    )
                } else if (
                    returnType.isSubtypeOf(
                        StandardClassIds.Iterable.constructClassLikeType(arrayOf(ConeStarProjection)),
                        session
                    ) ||
                    returnType.isSubtypeOf(
                        StandardClassIds.Iterable.constructClassLikeType(
                            arrayOf(ConeStarProjection),
                            isMarkedNullable = true
                        ), session
                    )
                ) {
                    val type: ConeKotlinType = when (val typeArgument = returnType.typeArguments[0]) {
                        is ConeKotlinType -> typeArgument
                        ConeStarProjection -> session.builtinTypes.nullableAnyType.coneType
                        else -> session.builtinTypes.nullableAnyType.coneType
                    }
                    if (type.isValueType(session)) {
                        val columnType = List.constructClassLikeType(arrayOf(type), returnType.isMarkedNullable)
                            .withNullability(makeNullable, session.typeContext)
                            .wrap()
                        SimpleDataColumn(name, columnType)
                    } else {
                        SimpleFrameColumn(name, convert(type, depth + 1, makeNullable = false))
                    }
                } else {
                    SimpleColumnGroup(name, convert(returnType, depth + 1, returnType.isMarkedNullable || makeNullable))
                }
            }
    }

    arg.type?.let { type ->
        if (!type.canBeUnfolded(session)) {
            return PluginDataFrameSchema(listOf(simpleColumnOf("value", type)))
        }
    }

    return when {
        arg.isStarProjection -> PluginDataFrameSchema.EMPTY
        else -> {
            val classLike = when (val type = arg.type) {
                is ConeClassLikeType -> type
                is ConeFlexibleType -> type.upperBound
                else -> null
            } ?: return PluginDataFrameSchema.EMPTY
            val columns = convert(classLike, 0, makeNullable = classLike.isMarkedNullable)
            PluginDataFrameSchema(columns)
        }
    }
}

fun ConeKotlinType.canBeUnfolded(session: FirSession): Boolean =
    !isValueType(session) && hasProperties(session)

private fun ConeKotlinType.isValueType(session: FirSession) =
    this.isArrayTypeOrNullableArrayType ||
            this.classId == StandardClassIds.Unit ||
            this.classId == StandardClassIds.Any ||
            this.classId == StandardClassIds.Map ||
            this.classId == StandardClassIds.MutableMap ||
            this.classId == StandardClassIds.String ||
            this.classId in StandardClassIds.primitiveTypes ||
            this.classId in StandardClassIds.unsignedTypes ||
            classId in setOf(
        Names.DURATION_CLASS_ID,
        Names.LOCAL_DATE_CLASS_ID,
        Names.LOCAL_DATE_TIME_CLASS_ID,
        Names.INSTANT_CLASS_ID,
        Names.STDLIB_INSTANT_CLASS_ID,
        Names.DATE_TIME_PERIOD_CLASS_ID,
        Names.DATE_TIME_UNIT_CLASS_ID,
        Names.TIME_ZONE_CLASS_ID
    ) ||
            this.isSubtypeOf(
                StandardClassIds.Number.constructClassLikeType(emptyArray(), isMarkedNullable = true),
                session
            ) ||
            this.toRegularClassSymbol(session)?.isEnumClass ?: false ||
            this.isSubtypeOf(
                Names.TEMPORAL_ACCESSOR_CLASS_ID.constructClassLikeType(emptyArray(), isMarkedNullable = true), session
            ) ||
            this.isSubtypeOf(
                Names.TEMPORAL_AMOUNT_CLASS_ID.constructClassLikeType(emptyArray(), isMarkedNullable = true), session
            )


private fun ConeKotlinType.hasProperties(session: FirSession): Boolean {
    val symbol = this.toRegularClassSymbol(session) as? FirClassSymbol<*> ?: return false
    val scope = symbol.unsubstitutedScope(
        session,
        ScopeSession(),
        withForcedTypeCalculator = false,
        memberRequiredPhase = null
    )

    return scope.collectAllProperties().any { it.visibility == Visibilities.Public } ||
            scope.collectAllFunctions().any { it.visibility == Visibilities.Public && it.isGetterLike() }
}

private fun FirNamedFunctionSymbol.isGetterLike(): Boolean {
    val functionName = this.name.asString()
    return (functionName.startsWith("get") || functionName.startsWith("is")) &&
            this.valueParameterSymbols.isEmpty() &&
            this.typeParameterSymbols.isEmpty()
}

// org.jetbrains.kotlinx.dataframe.codeGen.getFieldKind
private fun ConeKotlinType.getFieldKind(session: FirSession) = FieldKind.of(
    this,
    isDataFrame = { classId == DF_CLASS_ID },
    isListToFrame = { classId == List && typeArguments[0].type.hasAnnotation(DATA_SCHEMA_CLASS_ID, session) },
    isDataRow = { classId == DATA_ROW_CLASS_ID },
    isObjectToGroup = { hasAnnotation(DATA_SCHEMA_CLASS_ID, session) }
)

private fun ConeKotlinType?.hasAnnotation(id: ClassId, session: FirSession) =
    this?.toSymbol(session)?.hasAnnotation(id, session) == true


class CreateDataFrameDslImplApproximation {
    val configuration: CreateDataFrameConfiguration = CreateDataFrameConfiguration()
    val columns: MutableList<SimpleCol> = mutableListOf()
}

class ToDataFrameFrom : AbstractInterpreter<Unit>() {
    val Arguments.dsl: CreateDataFrameDslImplApproximation by arg()
    val Arguments.receiver: String by arg()
    val Arguments.expression: TypeApproximation by type()
    override fun Arguments.interpret() {
        dsl.columns += simpleColumnOf(receiver, expression.type)
    }
}
