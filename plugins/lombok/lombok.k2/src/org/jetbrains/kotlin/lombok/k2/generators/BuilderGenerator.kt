/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupported
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.FirErrorTypeRefBuilder
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Builder
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class BuilderGenerator(
    session: FirSession,
) : AbstractBuilderGenerator<Builder>(session) {
    override val builderModality: Modality = Modality.FINAL

    override val annotationClassId: ClassId = LombokNames.BUILDER_ID

    override fun getBuilder(symbol: FirBasedSymbol<*>): Builder? {
        return lombokService.getBuilder(symbol)
    }

    override fun getExtraTypeArguments(): List<ConeTypeProjection> {
        return emptyList()
    }

    override fun getBuilderType(builderSymbol: FirClassSymbol<*>): ConeKotlinType {
        return builderSymbol.defaultType()
    }

    override fun MutableMap<Name, FirNamedFunction>.addSpecialBuilderMethods(
        builder: Builder,
        builderSymbol: FirClassSymbol<*>,
        builderDeclaration: FirDeclaration,
        existingFunctionNames: Set<Name>,
    ) {
        if (builder.visibility == null) return

        addIfNonClashing(Name.identifier(builder.buildMethodName), existingFunctionNames) { name ->
            val builderTypeArguments = builderSymbol.typeParameterSymbols.map { typeParameter -> typeParameter.toConeType() }.toTypedArray()

            val returnTypeRef = when (builderDeclaration) {
                is FirRegularClass -> builderDeclaration.defaultType().toFirResolvedTypeRef()
                is FirNamedFunction -> builderDeclaration.returnTypeRef
                is FirConstructor -> builderDeclaration.returnTypeRef
                else -> FirErrorTypeRefBuilder().apply {
                    source = builderDeclaration.source
                    diagnostic =
                        ConeUnsupported("Unsupported builder declaration ${builderDeclaration::class.simpleName}", builderDeclaration.source)
                }.build()
            }.let {
                // Construct a new type with new type arguments
                // that bound to builder's class type parameters instead of its original class with `@Builder` annotation
                if (it is FirResolvedTypeRef && builderTypeArguments.isNotEmpty()) {
                    it.coneType.classId!!.constructClassLikeType(builderTypeArguments).toFirResolvedTypeRef()
                } else {
                    it
                }
            }
            if (builderSymbol.hasJavaOrigin) {
                builderSymbol.createJavaMethod(
                    name,
                    valueParameters = emptyList(),
                    returnTypeRef = returnTypeRef,
                    visibility = builder.visibility,
                    modality = Modality.FINAL
                )
            } else {
                createMemberFunction(
                    owner = builderSymbol,
                    key = BuilderGeneratorKey(builder),
                    name = name,
                    returnType = returnTypeRef.coneType,
                ) {
                    visibility = builder.visibility
                    modality = Modality.FINAL
                }
            }
        }
    }

    override fun FirRegularClassBuilder.completeBuilder(
        classSymbol: FirClassSymbol<*>, builderSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext,
    ) {
        superTypeRefs += listOf(session.builtinTypes.anyType)
    }
}
