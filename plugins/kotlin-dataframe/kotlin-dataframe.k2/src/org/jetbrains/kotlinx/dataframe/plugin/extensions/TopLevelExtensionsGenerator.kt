package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
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
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
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
            var resolvedReturnTypeRef: FirResolvedTypeRef = property.resolvedReturnTypeRef
            val columnName = property.getAnnotationByClassId(Names.COLUMN_NAME_ANNOTATION, session)?.let { annotation ->
                (annotation.argumentMapping.mapping[Names.COLUMN_NAME_ARGUMENT] as? FirLiteralExpression)?.value as? String?
            }
            val name = property.name

            val firPropertySymbol = FirRegularPropertySymbol(callableId)

            val typeParameters = owner.typeParameterSymbols.map {
                val propertyTypeParameterSymbol = FirTypeParameterSymbol()
                if (resolvedReturnTypeRef.coneType == it.toConeType()) {
                    resolvedReturnTypeRef = propertyTypeParameterSymbol.toConeType().toFirResolvedTypeRef()
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

            val columnGroupProjection: ConeTypeProjection? = if (resolvedReturnTypeRef.coneType.isDataRow(session)) {
                resolvedReturnTypeRef.coneType.typeArguments[0]
            } else if (resolvedReturnTypeRef.toClassLikeSymbol(session)?.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, session) == true) {
                resolvedReturnTypeRef.coneType
            } else {
                null
            }

            if (
                resolvedReturnTypeRef.coneType.isList &&
                (resolvedReturnTypeRef.coneType.typeArguments[0] as? ConeClassLikeType)
                    ?.toSymbol(session)
                    ?.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, session) == true
            ) {
                require(columnGroupProjection == null)
                resolvedReturnTypeRef = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID),
                    typeArguments = arrayOf(resolvedReturnTypeRef.coneType.typeArguments[0]),
                    isMarkedNullable = false
                ).toFirResolvedTypeRef()
            }

            val columnReturnType = when {
                columnGroupProjection != null -> {
                    ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(Names.COLUM_GROUP_CLASS_ID),
                        typeArguments = arrayOf(columnGroupProjection),
                        isMarkedNullable = false
                    ).toFirResolvedTypeRef()
                }

                else -> resolvedReturnTypeRef.projectOverDataColumnType().toFirResolvedTypeRef()
            }

            val extension = when (mode) {
                Receiver.DATA_ROW -> generateExtensionProperty(
                    callableIdOrSymbol = CallableIdOrSymbol.Symbol(firPropertySymbol),
                    receiverType = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
                        typeArguments = arrayOf(marker),
                        isMarkedNullable = false
                    ),
                    propertyName = PropertyName.of(name, columnName?.let { PropertyName.buildAnnotation(it) }),
                    returnTypeRef = resolvedReturnTypeRef,
                    source = owner.source,
                    typeParameters = typeParameters,
                )
                Receiver.COLUMNS_CONTAINER -> generateExtensionProperty(
                    callableIdOrSymbol = CallableIdOrSymbol.Symbol(firPropertySymbol),
                    receiverType = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(Names.COLUMNS_CONTAINER_CLASS_ID),
                        typeArguments = arrayOf(marker),
                        isMarkedNullable = false
                    ),
                    propertyName = PropertyName.of(name, columnName?.let { PropertyName.buildAnnotation(it) }),
                    returnTypeRef = columnReturnType,
                    source = owner.source,
                    typeParameters = typeParameters,
                )
            }
            extension.symbol
        }

        val owner = context?.owner
        return when (owner) {
            null -> generate(Receiver.DATA_ROW) + generate(Receiver.COLUMNS_CONTAINER)
            else -> emptyList()
        }
    }

    private enum class Receiver { DATA_ROW, COLUMNS_CONTAINER }
}
