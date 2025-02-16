/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClassBuilder
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.SuperBuilder
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class SuperBuilderGenerator(session: FirSession) : AbstractBuilderGenerator<SuperBuilder>(session) {
    companion object {
        const val CLASS_TYPE_PARAMETER_INDEX = 0
        const val BUILDER_TYPE_PARAMETER_INDEX = 1
    }

    override val builderModality: Modality = Modality.ABSTRACT

    override val annotationClassId: ClassId = LombokNames.SUPER_BUILDER_ID

    override fun getBuilder(symbol: FirBasedSymbol<*>): SuperBuilder? {
        // There is also a build impl class, but it's private, and it's used only for internal purposes. Not relevant for API.
        return lombokService.getSuperBuilder(symbol)
    }

    override fun constructBuilderType(builderClassId: ClassId): ConeClassLikeType {
        return builderClassId.constructClassLikeType(arrayOf(ConeStarProjection, ConeStarProjection), isMarkedNullable = false)
    }

    override fun getBuilderType(builderSymbol: FirClassSymbol<*>): ConeKotlinType? {
        return builderSymbol.typeParameterSymbols.elementAtOrNull(BUILDER_TYPE_PARAMETER_INDEX)?.defaultType
    }

    override fun MutableMap<Name, FirJavaMethod>.addSpecialBuilderMethods(
        builder: SuperBuilder,
        classSymbol: FirClassSymbol<*>,
        builderSymbol: FirClassSymbol<*>,
        existingFunctionNames: Set<Name>,
    ) {
        // Don't care about manually written builder classes without specified type parameters
        // Because they are anyway incorrect, Lombok reports Java errors on them and doesn't generate corresponding code
        val builderType = getBuilderType(builderSymbol) ?: return
        val classType = builderSymbol.typeParameterSymbols.elementAtOrNull(CLASS_TYPE_PARAMETER_INDEX)?.defaultType ?: return

        addIfNonClashing(Name.identifier("self"), existingFunctionNames) {
            builderSymbol.createJavaMethod(
                it,
                valueParameters = emptyList(),
                returnTypeRef = builderType.toFirResolvedTypeRef(),
                visibility = Visibilities.Protected,
                modality = Modality.ABSTRACT
            )
        }
        addIfNonClashing(Name.identifier(builder.buildMethodName), existingFunctionNames) {
            builderSymbol.createJavaMethod(
                it,
                valueParameters = emptyList(),
                returnTypeRef = classType.toFirResolvedTypeRef(),
                visibility = Visibilities.Public,
                modality = Modality.ABSTRACT
            )
        }
    }

    override fun FirJavaClassBuilder.completeBuilder(
        classSymbol: FirClassSymbol<*>,
        builderSymbol: FirClassSymbol<*>,
    ) {
        val classTypeParameterSymbol = FirTypeParameterSymbol()
        val builderTypeParameterSymbol = FirTypeParameterSymbol()

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
                coneType = classSymbol.defaultType()
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
                        classTypeParameterSymbol.defaultType,
                        builderTypeParameterSymbol.defaultType,
                    ),
                    isMarkedNullable = false
                )
            }
        }

        val superBuilderClass = classSymbol.resolvedSuperTypeRefs.mapNotNull { superTypeRef ->
            val superTypeSymbol = superTypeRef.toRegularClassSymbol(session) ?: return@mapNotNull null
            val superBuilders = builderClassesCache.getValue(superTypeSymbol) ?: return@mapNotNull null
            require(superBuilders.size <= 1) { "@SuperBuilder is only supported on types -> not more than one super type is possible" }
            superBuilders.firstNotNullOfOrNull { it.component2() }
        }.singleOrNull()
        val superBuilderTypeRef = superBuilderClass?.symbol?.constructType(
            typeArguments = arrayOf(
                classTypeParameterSymbol.defaultType,
                builderTypeParameterSymbol.defaultType,
            ),
            isMarkedNullable = false
        )?.toFirResolvedTypeRef() ?: session.builtinTypes.anyType

        superTypeRefs += listOf(superBuilderTypeRef)
    }
}