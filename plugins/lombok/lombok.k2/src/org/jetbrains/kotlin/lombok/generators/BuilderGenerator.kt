/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupported
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations.Builder
import org.jetbrains.kotlin.lombok.LombokNames
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

    /**
     * For `@Builder` annotation we always consider builder class modality as OPEN.
     *
     * However, [canHostClassLevelBuilder] does not allow us to create builder classes
     * for abstract / sealed owner class at all
     */
    override fun getBuilderModality(owner: FirClassSymbol<*>): Modality = Modality.OPEN

    override fun getBuilder(symbol: FirBasedSymbol<*>): Builder? {
        return lombokService.getBuilder(symbol)
    }

    override fun getExtraTypeArguments(): List<ConeTypeProjection> {
        return emptyList()
    }

    override fun getBuilderType(builderSymbol: FirClassSymbol<*>): ConeKotlinType {
        return builderSymbol.defaultType()
    }

    override fun MutableMap<Name, MutableList<FirNamedFunctionSymbol>>.addSpecialBuilderMethods(
        builder: Builder,
        builderSymbol: FirClassSymbol<*>,
        ownerDeclarationSymbol: FirBasedSymbol<*>,
        substitutor: ConeSubstitutor,
        existingFunctionNames: Set<Name>,
    ) {
        val visibility = builder.builderFunctionsVisibility(builderSymbol) ?: return

        val buildMethodName = Name.identifier(builder.buildMethodName)
        val declaredReturnTypeRef = when (ownerDeclarationSymbol) {
            is FirRegularClassSymbol -> ownerDeclarationSymbol.defaultType().toFirResolvedTypeRef()
            is FirNamedFunctionSymbol -> ownerDeclarationSymbol.implicitResolvedOrJavaTypeRef()
            is FirConstructorSymbol -> ownerDeclarationSymbol.implicitResolvedOrJavaTypeRef()
            else -> buildErrorTypeRef {
                source = ownerDeclarationSymbol.source
                diagnostic =
                    ConeUnsupported(
                        "Unsupported builder declaration ${ownerDeclarationSymbol::class.simpleName}",
                        ownerDeclarationSymbol.source
                    )
            }
        }
        if (declaredReturnTypeRef is FirImplicitTypeRef) return
        addFunctionIfNotClashingWithSources(buildMethodName, existingFunctionNames, allowGeneratedClash = false) { name ->
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
                key = BuilderGeneratorKey(
                    BuilderDeclarationType.Function.Build(ownerDeclarationSymbol, lombokService.config.singularUseGuava)
                ),
            )
        }
    }

    override fun Builder.getSpecialBuilderMethodNames(): List<Name> = listOf(Name.identifier(buildMethodName))

    override fun FirRegularClassBuilder.completeBuilder(
        classSymbol: FirClassSymbol<*>,
        builderSymbol: FirClassSymbol<*>,
        builder: Builder,
        context: NestedClassGenerationContext,
    ) {
        superTypeRefs += listOf(session.builtinTypes.anyType)
    }

    override fun MutableList<BuilderDescription<Builder>>.addAnnotatedCallables(owner: FirRegularClassSymbol) {
        for (declarationSymbol in @OptIn(DirectDeclarationsAccess::class) owner.declarationSymbols) {
            if (declarationSymbol is FirFunctionSymbol<*>) {
                // Left alone; `FirLombokBuilderChecker` reports it as
                // `BUILDER_WITH_RECEIVER_OR_CONTEXT_PARAMETERS`.
                if (declarationSymbol.hasReceiverOrContextParameters) continue
                // A constructor builder instantiates the very class the constructor belongs to, so it is
                // dropped wherever the class-level one is. A function builder is untouched: it builds
                // whatever the function returns, which need not be this class.
                if (declarationSymbol is FirConstructorSymbol && !owner.canBeInstantiatedByBuilder) continue
                getBuilder(declarationSymbol)?.let { builder ->
                    @OptIn(SymbolInternals::class)
                    val items = calculateBuilderItems(owner.fir, declarationSymbol, emptyMap())
                    add(BuilderDescription(builder, declarationSymbol, items))
                }
            }
        }
    }
}
