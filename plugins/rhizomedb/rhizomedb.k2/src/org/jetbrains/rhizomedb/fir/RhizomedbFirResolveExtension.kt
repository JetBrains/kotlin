/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.rhizomedb.fir.services.attributesProvider
import org.jetbrains.rhizomedb.fir.services.rhizomedbEntityPredicateMatcher

class RhizomedbFirResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        if (classSymbol !is FirRegularClassSymbol || classSymbol.classKind != ClassKind.CLASS) {
            return emptySet()
        }

        val entityMather = session.rhizomedbEntityPredicateMatcher
        return if (entityMather.isEntityTypeAnnotated(classSymbol)) {
            setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
        } else {
            emptySet()
        }
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        if (owner !is FirRegularClassSymbol || owner.companionObjectSymbol != null) {
            return null
        }
        return createCompanionObject(owner, RhizomedbPluginKey) {
//            superType(RhizomedbSymbolNames.entityTypeClassId.constructClassLikeType(arrayOf(owner.defaultType()), false))
        }.symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val entityMather = session.rhizomedbEntityPredicateMatcher
        if (!classSymbol.isCompanion || !entityMather.isEntityType(classSymbol)) {
            return emptySet()
        }

        val entity = classSymbol.getContainingDeclaration(session) as? FirClassSymbol ?: return emptySet()
        if (entity.classKind != ClassKind.CLASS || !entityMather.isEntity(entity)) {
            return emptySet()
        }

        val props = entity.declarationProperties.filter(entityMather::isAttributeAnnotated)
        return buildSet {
            for (prop in props) {
                add(prop.name.toAttributeName())
            }
        }
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val classSymbol = context?.owner ?: return emptyList()
        val entity = classSymbol.getContainingDeclaration(session) as? FirClassSymbol ?: return emptyList()

        val propName = callableId.callableName.toPropertyName()
        val prop = entity.declarationProperties.firstOrNull { it.name == propName } ?: return emptyList()

        val attribute = session.attributesProvider.getBackingAttribute(prop) ?: return emptyList()
        val property = createMemberProperty(
            classSymbol,
            RhizomedbPluginKey,
            attribute.attributeName,
            attribute.attributeType,
        )

        return listOf(property.symbol)
    }
}

val FirClassSymbol<*>.declarationProperties: List<FirPropertySymbol>
    get() = declarationSymbols.filterIsInstance<FirPropertySymbol>()
