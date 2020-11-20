/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.GeneratedClass
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class AllOpenTopLevelDeclarationsGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    override fun generateClasses(
        annotatedDeclaration: FirDeclaration,
        owners: List<FirAnnotatedDeclaration>
    ): List<GeneratedDeclaration<FirRegularClass>> {
        val file = owners.first() as FirFile
        val klass = annotatedDeclaration as? FirRegularClass ?: return emptyList()
        val newClass = buildRegularClass {
            session = this@AllOpenTopLevelDeclarationsGenerator.session
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            origin = FirDeclarationOrigin.Plugin(key)
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL
            )
            classKind = ClassKind.OBJECT
            name = Name.identifier("TopLevel${klass.name}")
            symbol = FirRegularClassSymbol(ClassId(file.packageFqName, name))
            scopeProvider = klass.scopeProvider
        }
        return listOf(GeneratedDeclaration(newClass, file))
    }

    override fun generateMembersForGeneratedClass(generatedClass: GeneratedClass): List<FirDeclaration> {
        val klass = generatedClass.klass
        val function = buildSimpleFunction {
            session = klass.session
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            origin = FirDeclarationOrigin.Plugin(key)
            returnTypeRef = session.builtinTypes.intType
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL
            )
            name = Name.identifier("hello")
            symbol = FirNamedFunctionSymbol(CallableId(klass.symbol.classId, name))
        }
        return listOf(function)
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
        get() = has("A".fqn())

    private object Key : FirPluginKey()
}
