package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.packageName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.plugin.DataFramePlugin
import org.jetbrains.kotlinx.dataframe.plugin.extensions.impl.PropertyName
import org.jetbrains.kotlinx.dataframe.plugin.utils.CallableIdOrSymbol
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
import org.jetbrains.kotlinx.dataframe.plugin.utils.generateExtensionProperty
import org.jetbrains.kotlinx.dataframe.plugin.utils.isDataRow
import org.jetbrains.kotlinx.dataframe.plugin.utils.projectOverDataColumnType

/**
 * extensions inside scope classes are generated here:
 * @see TokenGenerator
 */
class TopLevelExtensionsGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private companion object {
        val dataSchema = FqName(DataSchema::class.qualifiedName!!)
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(predicate).filterIsInstance<FirRegularClassSymbol>()
    }

    private val predicate: LookupPredicate = LookupPredicate.BuilderContext.annotated(dataSchema)

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    private val fields by lazy {
        matchedClasses.filterNot { it.isLocal }.flatMap { classSymbol ->
            classSymbol.declaredProperties(session).map { propertySymbol ->
                DataSchemaField(
                    classSymbol,
                    propertySymbol,
                    CallableId(packageName = propertySymbol.callableId.packageName, className = null, callableName = propertySymbol.name)
                )
            }
        }
    }

    private data class DataSchemaField(
        val classSymbol: FirRegularClassSymbol,
        val propertySymbol: FirPropertySymbol,
        val callableId: CallableId,
    )

    @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
    override fun getTopLevelCallableIds(): Set<CallableId> {
        return buildSet {
            fields.mapTo(this) { it.callableId }
        }
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        // type parameters, every type that refers to them and property symbol should be unique for each property:
        // codegen for the 2nd property will fail with "type parameter symbol is already bound to property"
        // so let's call this function twice, generate only 1 property at the time
        fun generate(mode: Receiver) = fields.filter { it.callableId == callableId }.map { (owner, property, callableId) ->
            buildExtensionPropertiesApi(
                callableId,
                owner,
                mode,
                property.resolvedReturnType,
                property.getAnnotationByClassId(Names.COLUMN_NAME_ANNOTATION, this@TopLevelExtensionsGenerator.session)?.let { annotation ->
                    (annotation.argumentMapping.mapping[Names.COLUMN_NAME_ARGUMENT] as? FirLiteralExpression)?.value as? String?
                },
                property.name
            )
        }

        val owner = context?.owner
        return when (owner) {
            null -> generate(Receiver.DATA_ROW) + generate(Receiver.COLUMNS_CONTAINER)
            else -> emptyList()
        }
    }

    enum class Receiver { DATA_ROW, COLUMNS_CONTAINER }
}

fun FirDeclarationGenerationExtension.buildExtensionPropertiesApi(
    callableId: CallableId,
    owner: FirRegularClassSymbol,
    mode: TopLevelExtensionsGenerator.Receiver,
    resolvedReturnType: ConeKotlinType,
    columnName: String?,
    name: Name
): FirPropertySymbol {
    var resolvedReturnType = resolvedReturnType
    val columnName = columnName

    val firPropertySymbol = FirRegularPropertySymbol(callableId)

    val typeParameters = owner.typeParameterSymbols.map {
        val propertyTypeParameterSymbol = FirTypeParameterSymbol()
        if (resolvedReturnType == it.toConeType()) {
            resolvedReturnType = propertyTypeParameterSymbol.toConeType()
        }
        buildTypeParameter {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Plugin(DataFramePlugin)

            this.name = it.name
            this.symbol = propertyTypeParameterSymbol
            containingDeclarationSymbol = firPropertySymbol
            variance = Variance.INVARIANT
            isReified = false
            bounds += session.builtinTypes.nullableAnyType
        }
    }

    val marker = owner.constructType(
        typeParameters.map { it.toConeType() }.toTypedArray(),
        isMarkedNullable = false
    ).toTypeProjection(Variance.INVARIANT)

    val columnGroupProjection: ConeTypeProjection? = if (resolvedReturnType.isDataRow(session)) {
        resolvedReturnType.typeArguments[0]
    } else if (resolvedReturnType.toClassLikeSymbol(session)?.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, session) == true) {
        resolvedReturnType
    } else {
        null
    }

    if (
        resolvedReturnType.isList &&
        (resolvedReturnType.typeArguments[0] as? ConeClassLikeType)
            ?.toSymbol(session)
            ?.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, session) == true
    ) {
        require(columnGroupProjection == null)
        resolvedReturnType = ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID),
            typeArguments = arrayOf(resolvedReturnType.typeArguments[0]),
            isMarkedNullable = false
        )
    }

    val columnReturnType = when {
        columnGroupProjection != null -> {
            Names.COLUM_GROUP_CLASS_ID.constructClassLikeType(
                typeArguments = arrayOf(columnGroupProjection),
                isMarkedNullable = false
            )
        }

        else -> resolvedReturnType.projectOverDataColumnType()
    }

    val extension = when (mode) {
        TopLevelExtensionsGenerator.Receiver.DATA_ROW -> generateExtensionProperty(
            callableIdOrSymbol = CallableIdOrSymbol.Symbol(firPropertySymbol),
            receiverType = Names.DATA_ROW_CLASS_ID.constructClassLikeType(
                typeArguments = arrayOf(marker),
                isMarkedNullable = false
            ),
            propertyName = PropertyName.of(name, columnName?.let { PropertyName.buildAnnotation(it) }),
            returnType = resolvedReturnType,
            source = owner.source,
            typeParameters = typeParameters,
        )
        TopLevelExtensionsGenerator.Receiver.COLUMNS_CONTAINER -> generateExtensionProperty(
            callableIdOrSymbol = CallableIdOrSymbol.Symbol(firPropertySymbol),
            receiverType = Names.COLUMNS_CONTAINER_CLASS_ID.constructClassLikeType(
                typeArguments = arrayOf(marker),
                isMarkedNullable = false
            ),
            propertyName = PropertyName.of(name, columnName?.let { PropertyName.buildAnnotation(it) }),
            returnType = columnReturnType,
            source = owner.source,
            typeParameters = typeParameters,
        )
    }
    return extension.symbol
}
