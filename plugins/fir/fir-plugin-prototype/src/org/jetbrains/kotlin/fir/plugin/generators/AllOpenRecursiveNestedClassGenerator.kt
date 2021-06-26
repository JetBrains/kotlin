/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.references.impl.FirReferencePlaceholderForResolvedAnnotations
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.GeneratedClass
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class AllOpenRecursiveNestedClassGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    override fun generateClasses(
        annotatedDeclaration: FirDeclaration,
        owners: List<FirAnnotatedDeclaration>
    ): List<GeneratedDeclaration<FirRegularClass>> {
        if (owners.size > 2) return emptyList()
        val owner = annotatedDeclaration as? FirRegularClass ?: return emptyList()
        val newClass = buildRegularClass {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            origin = FirDeclarationOrigin.Plugin(key)
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            classKind = ClassKind.CLASS
            name = Name.identifier("Nested")
            symbol = FirRegularClassSymbol(owner.symbol.classId.createNestedClassId(name))
            scopeProvider = owner.scopeProvider
            annotations += buildAnnotationCall {
                annotationTypeRef = buildResolvedTypeRef {
                    type = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(annotationClassId),
                        emptyArray(),
                        isNullable = false
                    )
                }
                calleeReference = FirReferencePlaceholderForResolvedAnnotations
            }
        }
        return listOf(GeneratedDeclaration(newClass, owner))
    }

    override fun generateMembersForGeneratedClass(generatedClass: GeneratedClass): List<FirDeclaration> {
        return emptyList()
    }

    override fun generateMembers(
        annotatedDeclaration: FirDeclaration,
        owners: List<FirAnnotatedDeclaration>
    ): List<GeneratedDeclaration<*>> {
        return emptyList()
    }

    override val key: FirPluginKey
        get() = Key

    override val predicate: DeclarationPredicate
        get() = has("B".fqn())

    private val annotationClassId = "B".fqn().let {
        ClassId(it.parent(), it.shortName())
    }

    private object Key : FirPluginKey()
}
