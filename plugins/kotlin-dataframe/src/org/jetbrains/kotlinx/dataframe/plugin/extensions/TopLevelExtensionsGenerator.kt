package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
import org.jetbrains.kotlinx.dataframe.plugin.utils.generateExtensionProperty
import org.jetbrains.kotlinx.dataframe.plugin.utils.projectOverDataColumnType
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.plugin.extensions.impl.PropertyName

/**
 * extensions inside scope classes are generated here:
 * @see org.jetbrains.kotlinx.dataframe.plugin.extensions.TokenGenerator
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
                val callableId = propertySymbol.callableId
                DataSchemaField(
                    classSymbol,
                    propertySymbol,
                    CallableId(packageName = callableId.packageName, className = null, callableName = callableId.callableName)
                )
            }
        }
    }

    private data class DataSchemaField(
        val classSymbol: FirRegularClassSymbol,
        val propertySymbol: FirPropertySymbol,
        val callableId: CallableId
    )

    @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
    override fun getTopLevelCallableIds(): Set<CallableId> {
        return buildSet {
            fields.mapTo(this) { it.callableId }
        }
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner
        return when (owner) {
            null -> fields.filter { it.callableId == callableId }.flatMap { (owner, property, callableId) ->
                var resolvedReturnTypeRef = property.resolvedReturnTypeRef
                val columnName = property.getAnnotationByClassId(Names.COLUMN_NAME_ANNOTATION, session)?.let { annotation ->
                    (annotation.argumentMapping.mapping[Names.COLUMN_NAME_ARGUMENT] as? FirLiteralExpression)?.value as? String?
                }
                val name = property.name
                val marker = owner.constructType(arrayOf(), isMarkedNullable = false).toTypeProjection(Variance.INVARIANT)

                val columnGroupProjection: ConeTypeProjection? = if (resolvedReturnTypeRef.coneType.classId?.equals(
                        Names.DATA_ROW_CLASS_ID) == true) {
                    resolvedReturnTypeRef.coneType.typeArguments[0]
                } else if (resolvedReturnTypeRef.toClassLikeSymbol(session)?.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, session) == true) {
                    resolvedReturnTypeRef.coneType
                } else {
                    null
                }

                if (
                    resolvedReturnTypeRef.coneType.classId?.equals(Names.LIST) == true &&
                    (resolvedReturnTypeRef.coneType.typeArguments[0] as? ConeClassLikeType)?.toSymbol(session)?.hasAnnotation(
                        Names.DATA_SCHEMA_CLASS_ID, session) == true
                ) {
                    require(columnGroupProjection == null)
                    resolvedReturnTypeRef = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID),
                        typeArguments = arrayOf(resolvedReturnTypeRef.coneType.typeArguments[0]),
                        isMarkedNullable = false
                    ).toFirResolvedTypeRef()
                }

                val rowExtension = generateExtensionProperty(
                    callableId = callableId,
                    receiverType = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
                        typeArguments = arrayOf(marker),
                        isMarkedNullable = false
                    ),
                    propertyName = PropertyName.of(name, columnName?.let { PropertyName.buildAnnotation(it) }),
                    returnTypeRef = resolvedReturnTypeRef,
                    source = owner.source
                )

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
                val columnsContainerExtension = generateExtensionProperty(
                    callableId = callableId,
                    receiverType = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(Names.COLUMNS_CONTAINER_CLASS_ID),
                        typeArguments = arrayOf(marker),
                        isMarkedNullable = false
                    ),
                    propertyName = PropertyName.of(name, columnName?.let { PropertyName.buildAnnotation(it) }),
                    returnTypeRef = columnReturnType,
                    source = owner.source
                )
                listOf(rowExtension.symbol, columnsContainerExtension.symbol)
            }

            else -> emptyList()
        }
    }
}
