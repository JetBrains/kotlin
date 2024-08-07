package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
import org.jetbrains.kotlinx.dataframe.plugin.utils.generateExtensionProperty
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.ConstantValueKind

class TokenGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    object Key : GeneratedDeclarationKey()

    @OptIn(SymbolInternals::class)
    private val propertiesCache: FirCache<FirClassSymbol<*>, Map<Name, List<FirProperty>>?, Nothing?> =
        session.firCachesFactory.createCache { k ->
            val callShapeData = k.fir.callShapeData ?: return@createCache null
            when (callShapeData) {
                is CallShapeData.Schema -> callShapeData.columns.withIndex().associate { (index, property) ->
                    val resolvedTypeRef = buildResolvedTypeRef {
                        type = property.dataRowReturnType
                    }
                    val propertyName = Name.identifier(property.name)
                    propertyName to listOf(buildProperty(resolvedTypeRef, propertyName, k, order = index))
                }
                is CallShapeData.RefinedType -> callShapeData.scopes.associate {
                    val propertyName = Name.identifier(it.name.identifier.replaceFirstChar { it.lowercaseChar() })
                    propertyName to listOf(buildProperty(it.defaultType().toFirResolvedTypeRef(), propertyName, k, isScopeProperty = true))
                }
                is CallShapeData.Scope -> callShapeData.columns.associate { schemaProperty ->
                    val propertyName = Name.identifier(schemaProperty.name)
                    val callableId = CallableId(k.classId, propertyName)
                    val dataRowExtension = generateExtensionProperty(
                        callableId = callableId,
                        symbol = k,
                        receiverType = ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
                            typeArguments = arrayOf(schemaProperty.marker),
                            isNullable = false
                        ),
                        propertyName = propertyName,
                        returnTypeRef = schemaProperty.dataRowReturnType.toFirResolvedTypeRef(),
                        effectiveVisibility = EffectiveVisibility.Local
                    )

                    val columnContainerExtension = generateExtensionProperty(
                        callableId = callableId,
                        receiverType = ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(Names.COLUMNS_CONTAINER_CLASS_ID),
                            typeArguments = arrayOf(schemaProperty.marker),
                            isNullable = false
                        ),
                        propertyName = propertyName,
                        returnTypeRef = schemaProperty.columnContainerReturnType.toFirResolvedTypeRef(),
                        symbol = k,
                        effectiveVisibility = EffectiveVisibility.Local
                    )
                    propertyName to listOf(dataRowExtension, columnContainerExtension)
                }
            }
        }

    @OptIn(SymbolInternals::class)
    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val destination = mutableSetOf<Name>()
        when (classSymbol.fir.callShapeData) {
            is CallShapeData.RefinedType -> destination.add(SpecialNames.INIT)
            is CallShapeData.Schema -> destination.add(SpecialNames.INIT)
            is CallShapeData.Scope -> destination.add(SpecialNames.INIT)
            null -> Unit
        }
        return propertiesCache.getValue(classSymbol)?.values?.flatten()?.mapTo(destination) { it.name } ?: emptySet()
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        val properties = propertiesCache.getValue(owner)?.get(callableId.callableName) ?: return emptyList()
        return properties.map { it.symbol }
    }

    private fun buildProperty(
        resolvedTypeRef: FirResolvedTypeRef,
        propertyName: Name,
        k: FirClassSymbol<*>,
        isScopeProperty: Boolean = false,
        order: Int? = null,
    ): FirProperty {
        return createMemberProperty(k, Key, propertyName, resolvedTypeRef.type) {
            modality = Modality.ABSTRACT
            visibility = Visibilities.Public
        }.apply {
            val annotations = mutableListOf<FirAnnotation>()
            if (order != null) {
                annotations += buildAnnotation {
                    annotationTypeRef = buildResolvedTypeRef {
                        type = Names.ORDER_ANNOTATION.defaultType(emptyList())
                    }
                    argumentMapping = buildAnnotationArgumentMapping {
                        mapping[Names.ORDER_ARGUMENT] = buildLiteralExpression(null, ConstantValueKind.Int, order, setType = true)
                    }
                }
            }
            if (isScopeProperty) {
                annotations += buildAnnotation {
                    annotationTypeRef = buildResolvedTypeRef {
                        type = Names.SCOPE_PROPERTY_ANNOTATION.defaultType(emptyList())
                    }
                    argumentMapping = buildAnnotationArgumentMapping()
                }
            }
            replaceAnnotations(annotations)
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        return listOf(createConstructor(context.owner, Key, isPrimary = true).symbol)
    }
}
