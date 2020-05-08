/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirEffectiveVisibilityImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.FirClassGenerationExtension
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class AllOpenClassGenerator(session: FirSession) : FirClassGenerationExtension(session) {
    override fun <T> generateClass(
        containingFile: FirFile,
        annotatedDeclaration: T
    ): List<GeneratedClass> where T : FirDeclaration, T : FirAnnotationContainer {
        if (annotatedDeclaration !is FirRegularClass) return emptyList()
        val klass = buildClassImpl {
            session = this@AllOpenClassGenerator.session
            origin = FirDeclarationOrigin.Plugin(AllOpenPluginKey)
            status = FirResolvedDeclarationStatusImpl(Visibilities.PUBLIC, FirEffectiveVisibilityImpl.Public, Modality.FINAL)
            classKind = ClassKind.OBJECT
            scopeProvider = (session.firProvider as FirProviderImpl).kotlinScopeProvider
            name = Name.identifier("Foo${annotatedDeclaration.name.identifier}")
            symbol = FirRegularClassSymbol(ClassId(containingFile.packageFqName, name))
            superTypeRefs += session.builtinTypes.anyType
        }
        return listOf(GeneratedClass(klass, containingFile))
    }

    override val directlyApplicableAnnotations: Set<AnnotationFqn> = setOf(FqName("org.jetbrains.kotlin.fir.plugin.WithClass"))

    override val childrenApplicableAnnotations: Set<AnnotationFqn>
        get() = emptySet()

    override val metaAnnotations: Map<AnnotationFqn, MetaAnnotationMode>
        get() = emptyMap()

    override val key: FirPluginKey
        get() = AllOpenPluginKey
}