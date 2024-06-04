/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.rhizomedb.fir.*

class RhizomedbFirResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        if (classSymbol !is FirRegularClassSymbol || classSymbol.classKind.isSingleton) {
            return emptySet()
        }

        val matcher = session.rhizomedbPredicateMatcher
        return if (matcher.isEntityTypeAnnotated(classSymbol)) {
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
        require(owner is FirRegularClassSymbol) { "$owner::class not a FirRegularClassSymbol" }
        if (owner.companionObjectSymbol != null) {
            return null
        }
        return createCompanionObject(owner, RhizomedbPluginKey).symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isCompanion) {
            return emptySet()
        }
        val entity = classSymbol.getContainingClass(session) ?: return emptySet()

        val mather = session.rhizomedbPredicateMatcher
        val props = entity.declarationProperties.filter(mather::isAttributeAnnotated)
        return buildSet {
            for (prop in props) {
                add(prop.name.toAttributeName())
            }
        }
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val classSymbol = context?.owner ?: return emptyList()

        val mather = session.rhizomedbPredicateMatcher
        if (!mather.isEntityType(classSymbol)) {
            return emptyList()
        }
        val entity = classSymbol.getContainingClass(session) ?: return emptyList()
        if (!mather.isEntity(entity)) {
            return emptyList()
        }

        val propName = callableId.callableName.toPropertyName() ?: return emptyList()
        val prop = entity.declarationProperties.firstOrNull { it.name == propName } ?: return emptyList()

        val attribute = session.attributesProvider.getBackingAttribute(prop) ?: return emptyList()
        val property = createMemberProperty(
            classSymbol,
            RhizomedbAttributePluginKey(attribute.kind),
            attribute.attributeName,
            attribute.attributeType,
        )

        return listOf(property.symbol)
    }
}
