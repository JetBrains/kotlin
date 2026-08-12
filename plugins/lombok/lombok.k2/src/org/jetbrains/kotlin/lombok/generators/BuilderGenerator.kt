/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupported
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.Builder
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class BuilderGenerator(session: FirSession) : AbstractBuilderGenerator<Builder>(session) {
    companion object {
        private val PREDICATE = DeclarationPredicate.create {
            annotated(
                listOf(
                    LombokNames.BUILDER,
                    LombokNames.SINGULAR,
                    LombokNames.BUILDER_DEFAULT_ID.asSingleFqName()
                )
            )
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }

    override val builderModality: Modality = Modality.OPEN

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

    override fun MutableMap<Name, FirNamedFunctionSymbol>.addSpecialBuilderMethods(
        builder: Builder,
        builderSymbol: FirClassSymbol<*>,
        builderDeclaration: FirDeclaration,
        substitutor: ConeSubstitutor,
        existingFunctionNames: Set<Name>,
    ) {
        val visibility = builder.builderFunctionsAccessLevel.toVisibility(builderSymbol) ?: return

        addIfNonClashing(Name.identifier(builder.buildMethodName), existingFunctionNames) { name ->
            val declaredReturnTypeRef = when (builderDeclaration) {
                is FirRegularClass -> builderDeclaration.defaultType().toFirResolvedTypeRef()
                is FirNamedFunction -> builderDeclaration.returnTypeRef
                is FirConstructor -> builderDeclaration.returnTypeRef
                else -> buildErrorTypeRef {
                    source = builderDeclaration.source
                    diagnostic =
                        ConeUnsupported(
                            "Unsupported builder declaration ${builderDeclaration::class.simpleName}",
                            builderDeclaration.source
                        )
                }
            }

            // Rebind the type to the builder class's own type parameters, which are fresh copies of the annotated
            // declaration's ones (see `extractTypeParametersMapping`), instead of the originals.
            // Java type refs need no substitution: their type variables are resolved by name through the owning
            // declaration's `javaTypeParameterStack`, which is already populated with those copies — the same split
            // as in `addSetterMethod` and `parameterType`.
            val returnTypeRef = if (declaredReturnTypeRef is FirResolvedTypeRef) {
                substitutor.substituteOrSelf(declaredReturnTypeRef.coneType).toFirResolvedTypeRef()
            } else {
                declaredReturnTypeRef
            }

            createJavaOrKotlinMemberFunction(
                owner = builderSymbol,
                name = name,
                valueParameters = emptyList(),
                returnTypeRef = returnTypeRef,
                visibility = visibility,
                modality = Modality.OPEN,
                createKey = {
                    BuilderGeneratorKey(BuilderDeclarationType.Function.Build(builderDeclaration.symbol))
                }
            )
        }
    }

    override fun FirRegularClassBuilder.completeBuilder(
        classSymbol: FirClassSymbol<*>, builderSymbol: FirClassSymbol<*>, builder: Builder,
    ) {
        superTypeRefs += listOf(session.builtinTypes.anyType)
    }
}
