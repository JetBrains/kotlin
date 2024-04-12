/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.rhizomedb.fir.resolve.RhizomedbSymbolNames
import org.jetbrains.rhizomedb.fir.services.attributesProviderProvider

class RhizomedbFirResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        val result = mutableSetOf<Name>()
        if (classSymbol.shouldHaveGeneratedMethodsInCompanion(session))
            result += SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        return result
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        if (owner !is FirRegularClassSymbol) return null
        if (!session.predicateBasedProvider.matches(RhizomedbFirPredicates.annotatedWithEntityType, owner)) return null
        return when (name) {
            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> generateCompanionDeclaration(owner)
            else -> error("Can't generate class ${owner.classId.createNestedClassId(name).asSingleFqName()}")
        }
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isCompanion) {
            return emptySet()
        }
        val entity = classSymbol.getContainingDeclaration(session) as? FirClassSymbol ?: return emptySet()
        if (!entity.shouldHaveGeneratedMethodsInCompanion(session)) return emptySet()

        val props = entity.declarationSymbols.filterIsInstance<FirPropertySymbol>().filter { it.hasAttributeAnnotation(session) }
        return buildSet {
            for (prop in props) {
                add(prop.name.toAttributeName())
            }
        }
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        val entity = owner.getContainingDeclaration(session) as? FirClassSymbol ?: return emptyList()

        val prop = entity.declarationSymbols.filterIsInstance<FirPropertySymbol>().firstOrNull {
            it.name == callableId.callableName.toPropertyName()
        } ?: return emptyList()

        val attribute = session.attributesProviderProvider.getAttributeForProperty(prop, entity)
        val property = createMemberProperty(
            owner,
            RhizomedbPluginKey,
            attribute.attributeName,
            attribute.attributeType,
        )

        return listOf(property.symbol)
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.companionObjectSymbol != null) return null
        val companion = createCompanionObject(owner, RhizomedbPluginKey) {
            superType(RhizomedbSymbolNames.entityTypeClassId.constructClassLikeType(arrayOf(owner.defaultType()), false))
        }

        return companion.symbol
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(RhizomedbFirPredicates.selfOrParentAnnotatedWithEntityType)
    }
}
