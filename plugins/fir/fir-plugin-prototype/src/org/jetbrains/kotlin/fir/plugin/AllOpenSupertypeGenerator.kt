/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class AllOpenSupertypeGenerator(session: FirSession) : FirSupertypeGenerationExtension(session) {
    companion object {
        private val newSupertypeClassId = ClassId(FqName("org.jetbrains.kotlin.fir.plugin"), Name.identifier("LibraryClassWithNestedClass"))
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration<*>,
        resolvedSupertypes: List<FirTypeRef>
    ): List<FirResolvedTypeRef> {
        return listOf(
            buildResolvedTypeRef {
                type = newSupertypeClassId.constructClassLikeType(emptyArray(), isNullable = false)
            }
        )
    }

    override val key: FirPluginKey
        get() = AllOpenPluginKey

    override val predicate: DeclarationPredicate = has("C".fqn())
}