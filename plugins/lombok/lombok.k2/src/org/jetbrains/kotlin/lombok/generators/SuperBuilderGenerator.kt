/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.utils.isClass
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.SuperBuilder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class SuperBuilderGenerator(session: FirSession) : AbstractBuilderGenerator<SuperBuilder>(session) {
    companion object {
        const val CLASS_TYPE_PARAMETER_INDEX_FROM_END = 2
        const val BUILDER_TYPE_PARAMETER_INDEX_FROM_END = 1
        private val startProjectionsList = listOf(ConeStarProjection, ConeStarProjection)
    }

    private val processedBuilderKeys: FirCache<BuilderKey, Boolean, Nothing?> = session.firCachesFactory.createCache { true }

    override val builderModality: Modality = Modality.ABSTRACT

    override val annotationClassId: ClassId = LombokNames.SUPER_BUILDER_ID

    override fun getBuilder(symbol: FirBasedSymbol<*>): SuperBuilder? {
        // There is also a build impl class, but it's private, and it's used only for internal purposes. Not relevant for API.
        if (lombokService.getBuilder(symbol) != null) return null
        return lombokService.getSuperBuilder(symbol)
    }

    override fun getExtraTypeArguments(): List<ConeTypeProjection> {
        return startProjectionsList
    }

    override fun getBuilderType(builderSymbol: FirClassSymbol<*>): ConeKotlinType? {
        return builderSymbol.typeParameterSymbols.elementAtOrNull(builderSymbol.typeParameterSymbols.size - BUILDER_TYPE_PARAMETER_INDEX_FROM_END)?.defaultType
    }

    override fun MutableMap<Name, FirNamedFunctionSymbol>.addSpecialBuilderMethods(
        builder: SuperBuilder,
        builderSymbol: FirClassSymbol<*>,
        builderDeclaration: FirDeclaration,
        existingFunctionNames: Set<Name>,
    ) {
        // Don't care about manually written builder classes without specified type parameters
        // Because they are anyway incorrect, Lombok reports Java errors on them and doesn't generate corresponding code
        val builderType = getBuilderType(builderSymbol) ?: return
        val classType =
            builderSymbol.typeParameterSymbols.elementAtOrNull(builderSymbol.typeParameterSymbols.size - CLASS_TYPE_PARAMETER_INDEX_FROM_END)?.defaultType
                ?: return

        addIfNonClashing(Name.identifier("self"), existingFunctionNames) {
            builderSymbol.createJavaMethod(
                it,
                valueParameters = emptyList(),
                returnTypeRef = builderType.toFirResolvedTypeRef(),
                visibility = Visibilities.Protected,
                modality = Modality.ABSTRACT
            ).symbol
        }
        addIfNonClashing(Name.identifier(builder.buildMethodName), existingFunctionNames) {
            builderSymbol.createJavaMethod(
                it,
                valueParameters = emptyList(),
                returnTypeRef = classType.toFirResolvedTypeRef(),
                visibility = Visibilities.Public,
                modality = Modality.ABSTRACT
            ).symbol
        }
    }

    override fun FirRegularClassBuilder.completeBuilder(
        classSymbol: FirClassSymbol<*>,
        builderSymbol: FirClassSymbol<*>,
        builder: SuperBuilder,
    ) {
        val classTypeParameterSymbol = FirTypeParameterSymbol()
        val builderTypeParameterSymbol = FirTypeParameterSymbol()
        val builderTypeArguments = typeParameters.map { it.toConeType() as ConeTypeProjection }.toTypedArray()

        typeParameters += buildTypeParameter {
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Java.Source
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            this.name = Name.identifier("C")
            symbol = classTypeParameterSymbol
            containingDeclarationSymbol = builderSymbol
            variance = Variance.INVARIANT
            isReified = false
            bounds += buildResolvedTypeRef {
                coneType = classSymbol.constructType(builderTypeArguments)
            }
        }
        typeParameters += buildTypeParameter {
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Java.Source
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            this.name = Name.identifier("B")
            symbol = builderTypeParameterSymbol
            containingDeclarationSymbol = builderSymbol
            variance = Variance.INVARIANT
            isReified = false
            bounds += buildResolvedTypeRef {
                coneType = builderSymbol.constructType(
                    typeArguments = arrayOf(
                        *builderTypeArguments,
                        classTypeParameterSymbol.defaultType,
                        builderTypeParameterSymbol.defaultType,
                    )
                )
            }
        }

        val superTypeRef = classSymbol.resolvedSuperTypeRefs.firstNotNullOfOrNull { superTypeRef ->
            val superTypeSymbol = superTypeRef.toRegularClassSymbol(session) ?: return@firstNotNullOfOrNull null

            if (!superTypeSymbol.isClass) return@firstNotNullOfOrNull null

            val builderNames = getBuilderNames(superTypeSymbol)

            // `@SingleBuilder` is only applicable to classes. It means it can be <= 1 builder in a super type.
            val builderName = builderNames.firstOrNull() ?: return@firstNotNullOfOrNull null

            val builderKey = BuilderKey(superTypeSymbol, builderName)

            if (processedBuilderKeys.getValueIfComputed(builderKey) != null) {
                return@firstNotNullOfOrNull buildErrorTypeRef {
                    diagnostic = ConeSimpleDiagnostic(
                        "Loop in supertypes involving ${superTypeSymbol.classId}",
                        DiagnosticKind.LoopInSupertype,
                    )
                }
            } else {
                // Use the mutating `getValue` because the `FirCache` doesn't provide API for elements putting
                require(processedBuilderKeys.getValue(builderKey))
            }

            val superBuilder = builderClassesCache.getValue(builderKey) ?: return@firstNotNullOfOrNull null

            superBuilder.constructType(
                typeArguments = arrayOf(
                    *superTypeRef.coneType.typeArguments,
                    classTypeParameterSymbol.defaultType,
                    builderTypeParameterSymbol.defaultType,
                )
            ).toFirResolvedTypeRef()
        }

        superTypeRefs += superTypeRef ?: session.builtinTypes.anyType
    }
}
