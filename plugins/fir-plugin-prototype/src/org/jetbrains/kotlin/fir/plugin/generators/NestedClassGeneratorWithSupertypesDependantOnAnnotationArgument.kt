/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.plugin.generators.NestedClassGeneratorWithSupertypesDependantOnAnnotationArgument.Key
import org.jetbrains.kotlin.fir.plugin.generators.SupertypesDependantOnAnnotationArgumentComponent.Companion.ANNOTATION_ID
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * ```
 * @AddNestedClassesBasedOnArgument(XXX::class)
 * interface Some {
 *     class Generated : XXX()
 * }
 * ```
 */
class NestedClassGeneratorWithSupertypesDependantOnAnnotationArgument(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val GENERATED = Name.identifier("Generated")
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        return when {
            classSymbol in session.myComponent.matchedClasses -> setOf(GENERATED)
            else -> emptySet()
        }
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        return when (name) {
            GENERATED -> {
                if (owner !in session.myComponent.matchedClasses) return null
                createNestedClass(owner, GENERATED, Key, classKind = ClassKind.INTERFACE) {
                    modality = Modality.ABSTRACT
                }.symbol
            }
            else -> null
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(SupertypesDependantOnAnnotationArgumentComponent.PREDICATE)
    }

    object Key : GeneratedDeclarationKey()
}

class NestedClassSupertypesDependantOnAnnotationArgumentAdder(session: FirSession) : FirSupertypeGenerationExtension(session) {
    companion object {
        private val B = Name.identifier("B")
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return declaration.origin.key == Key
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService,
    ): List<ConeKotlinType> = emptyList()

    @ExperimentalSupertypesGenerationApi
    override fun computeAdditionalSupertypesForGeneratedNestedClass(
        klass: FirRegularClass,
        typeResolver: TypeResolveService,
    ): List<FirResolvedTypeRef> {
        require(klass.origin.key == Key)
        val container = klass.getContainingDeclaration(session) ?: return emptyList()
        val annotation = container.annotations.getAnnotationByClassId(ANNOTATION_ID, session) as? FirAnnotationCall ?: return emptyList()
        val getClassCall = annotation.arguments.singleOrNull() as? FirGetClassCall ?: return emptyList()
        val qualifierParts = createQualifier(getClassCall.argument) ?: return emptyList()
        val unresolvedType = createUserType(qualifierParts)
        return listOf(typeResolver.resolveUserType(unresolvedType))
    }

    private fun createQualifier(argument: FirExpression): List<Name>? {
        val result = mutableListOf<Name>()

        fun createQualifierImpl(argument: FirExpression?) {
            if (argument !is FirPropertyAccessExpression) return
            createQualifierImpl(argument.explicitReceiver)
            val reference = argument.calleeReference as? FirSimpleNamedReference ?: return
            result.add(reference.name)
        }

        createQualifierImpl(argument)
        return result.takeIf { it.isNotEmpty() }
    }

    private fun createUserType(qualifierParts: List<Name>): FirUserTypeRef {
        return buildUserTypeRef {
            isMarkedNullable = false
            qualifierParts.mapTo(qualifier) {
                FirQualifierPartImpl(source = null, it, typeArgumentList = FirTypeArgumentListImpl(source = null))
            }
        }
    }
}

class SupertypesDependantOnAnnotationArgumentComponent(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        private val ANNOTATION = "AddNestedClassesBasedOnArgument".fqn()
        val ANNOTATION_ID = ClassId.topLevel(ANNOTATION)
        val PREDICATE = LookupPredicate.create {
            annotated(ANNOTATION)
        }
    }

    private val predicateBasedProvider = session.predicateBasedProvider

    val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE)
            .filterIsInstance<FirRegularClassSymbol>()
    }
}

private val FirSession.myComponent: SupertypesDependantOnAnnotationArgumentComponent by FirSession.sessionComponentAccessor()

private val FirDeclarationOrigin.key: GeneratedDeclarationKey?
    get() = (this as? FirDeclarationOrigin.Plugin)?.key
