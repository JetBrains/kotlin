/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildEnumEntry
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*

/*
 * Generates an enum 'foo.GeneratedEnum` with an entry for every class annotated '@GenerateEnumConstant'.
 */
class EnumGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val FOO_PACKAGE = FqName.topLevel(Name.identifier("foo"))
        private val GENERATED_CLASS_ID = ClassId(FOO_PACKAGE, Name.identifier("GeneratedEnum"))

        private val PREDICATE = LookupPredicate.create { annotated("GenerateEnumConstant".fqn()) }
    }

    object Key : GeneratedDeclarationKey() {
        override fun toString(): String {
            return "EnumGeneratorKey"
        }
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }
    private val entryNames by lazy {
        matchedClasses.map { it.name }
    }

    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId != GENERATED_CLASS_ID) return null

        val selfType = classId.constructClassLikeType()
        val enumClass = createTopLevelClass(classId, Key, classKind = ClassKind.ENUM_CLASS) {
            superType(session.builtinTypes.enumType.type.withArguments(arrayOf(selfType)))
        }

        return enumClass.symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (classSymbol.classId != GENERATED_CLASS_ID) return emptySet()

        return buildSet {
            add(SpecialNames.INIT)
            addAll(entryNames)
            add(StandardNames.ENUM_VALUES)
            add(StandardNames.ENUM_VALUE_OF)
            add(StandardNames.ENUM_ENTRIES)
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        if (context.owner.classId != GENERATED_CLASS_ID) return emptyList()

        val constructor = createConstructor(context.owner, Key, isPrimary = true) {
            visibility = Visibilities.Private

            status {
                isFromEnumClass = true
            }
        }

        return listOf(constructor.symbol)
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val classId = callableId.classId
        if (classId != GENERATED_CLASS_ID || context == null) return emptyList()

        return when (callableId.callableName) {
            StandardNames.ENUM_ENTRIES -> listOf(
                createEnumEntriesGetter(
                    context.owner,
                    context.owner.resolvedStatus,
                    FirResolvePhase.BODY_RESOLVE,
                    session.moduleData,
                    classId.packageFqName,
                    classId.relativeClassName,
                    Key.origin
                ).symbol
            )
            else -> emptyList()
        }
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val classId = callableId.classId
        if (classId != GENERATED_CLASS_ID || context == null) return emptyList()

        return when (callableId.callableName) {
            StandardNames.ENUM_VALUES -> listOf(
                createEnumValuesFunction(
                    context.owner,
                    context.owner.resolvedStatus,
                    FirResolvePhase.BODY_RESOLVE,
                    session.moduleData,
                    classId.packageFqName,
                    classId.relativeClassName,
                    Key.origin
                ).symbol
            )
            StandardNames.ENUM_VALUE_OF -> listOf(
                createEnumValueOfFunction(
                    context.owner,
                    context.owner.resolvedStatus,
                    FirResolvePhase.BODY_RESOLVE,
                    session.moduleData,
                    classId.packageFqName,
                    classId.relativeClassName,
                    Key.origin
                ).symbol
            )
            else -> emptyList()
        }
    }

    override fun generateEnumEntries(callableId: CallableId, context: MemberGenerationContext): List<FirEnumEntrySymbol> {
        val classId = callableId.classId
        if (classId != GENERATED_CLASS_ID || callableId.callableName !in entryNames) return emptyList()

        val entry = buildEnumEntry {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = Key.origin

            returnTypeRef = classId.constructClassLikeType().toFirResolvedTypeRef()

            name = callableId.callableName
            symbol = FirEnumEntrySymbol(callableId)
            status = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.copy(isStatic = true)
        }

        return listOf(entry.symbol)
    }

    override fun getTopLevelClassIds(): Set<ClassId> {
        return if (matchedClasses.isEmpty()) emptySet() else setOf(GENERATED_CLASS_ID)
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        return packageFqName == FOO_PACKAGE
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}
