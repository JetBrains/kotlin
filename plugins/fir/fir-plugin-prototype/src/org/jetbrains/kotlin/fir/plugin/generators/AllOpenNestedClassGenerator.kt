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
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.plugin.AllOpenPluginKey
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.GeneratedClass
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.Name

class AllOpenNestedClassGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    override fun generateClasses(
        annotatedDeclaration: FirDeclaration,
        owners: List<FirAnnotatedDeclaration>
    ): List<GeneratedDeclaration<FirRegularClass>> {
        val owner = annotatedDeclaration as? FirRegularClass ?: return emptyList()
        val newClass = buildRegularClass {
            session = this@AllOpenNestedClassGenerator.session
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            origin = FirDeclarationOrigin.Plugin(key)
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Private,
                Modality.FINAL
            ).apply {
                isInner = true
            }
            classKind = ClassKind.CLASS
            name = Name.identifier("Foo")
            symbol = FirRegularClassSymbol(owner.symbol.classId.createNestedClassId(name))
            scopeProvider = owner.scopeProvider
        }
        return listOf(GeneratedDeclaration(newClass, owner))
    }

    override fun generateMembersForGeneratedClass(generatedClass: GeneratedClass): List<FirDeclaration> {
        val klass = generatedClass.klass

        val classId = klass.symbol.classId
        val constructor = buildConstructor {
            session = klass.session
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            origin = FirDeclarationOrigin.Plugin(key)
            returnTypeRef = buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(classId), emptyArray(), isNullable = false)
            }
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL
            )
            symbol = FirConstructorSymbol(CallableId(classId, classId.shortClassName))
        }

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
            symbol = FirNamedFunctionSymbol(CallableId(classId, name))
        }
        return listOf(constructor, function)
    }

    override fun generateMembers(
        annotatedDeclaration: FirDeclaration,
        owners: List<FirAnnotatedDeclaration>
    ): List<GeneratedDeclaration<*>> {
        return emptyList()
    }

    override val predicate: DeclarationPredicate = has("WithNestedFoo".fqn())

    override val key: FirPluginKey
        get() = AllOpenPluginKey
}
