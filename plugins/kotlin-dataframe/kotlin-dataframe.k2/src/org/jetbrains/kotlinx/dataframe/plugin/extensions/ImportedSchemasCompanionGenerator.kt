/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlinx.dataframe.plugin.ImportedSchemaCompanionKey
import org.jetbrains.kotlinx.dataframe.plugin.ImportedSchemaMetadata
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
import kotlin.collections.get
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class ImportedSchemasCompanionGenerator(
    session: FirSession,
    val predicate: LookupPredicate,
) : FirDeclarationGenerationExtension(session) {
    val topLevelMetadata: Map<Name, ImportedSchemaMetadata> get() = session.importedSchemasService.topLevelMetadata

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val containingClassSymbol = classSymbol.getContainingClassSymbol() ?: return emptySet()
        return if (classSymbol.isCompanion && session.predicateBasedProvider.matches(predicate, containingClassSymbol)) {
            setOf(Names.READ, Names.DEFAULT, Names.SCHEMA_KTYPE, SpecialNames.INIT)
        } else {
            emptySet()
        }
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val metadata = context.metadata() ?: return emptyList()
        if (callableId.callableName == Names.SCHEMA_KTYPE) {
            val kType = StandardClassIds.KType.constructClassLikeType()
            val property = createMemberProperty(context.owner, ImportedSchemaCompanionKey(metadata), Names.SCHEMA_KTYPE, kType) {
                status {
                    isOverride = true
                }
            }
            return listOf(property.symbol)
        }
        return emptyList()
    }

    @OptIn(ExperimentalContracts::class)
    fun MemberGenerationContext?.metadata(): ImportedSchemaMetadata? {
        contract {
            returnsNotNull() implies (this@metadata != null)
        }
        return topLevelMetadata[this?.owner?.getContainingClassSymbol()?.classId?.shortClassName]
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val metadata = context.metadata() ?: return emptyList()
        if (callableId.callableName == Names.READ) {
            val type = context.owner.getContainingClassSymbol()?.defaultType()
            val function = createMemberFunction(
                context.owner,
                ImportedSchemaCompanionKey(metadata),
                Names.READ,
                Names.DF_CLASS_ID.createConeType(session, arrayOf(type!!))
            ) {
                status {
                    isOverride = true
                }
                valueParameter(Name.identifier("path"), session.builtinTypes.stringType.coneType)
            }
            return listOf(function.symbol)
        }

        if (callableId.callableName == Names.DEFAULT) {
            val type = context.owner.getContainingClassSymbol()?.defaultType()
            val function = createMemberFunction(
                context.owner,
                ImportedSchemaCompanionKey(metadata),
                Names.DEFAULT,
                Names.DF_CLASS_ID.createConeType(session, arrayOf(type!!))
            ) {
                status {
                    isOverride = true
                }
            }
            return listOf(function.symbol)
        }
        return emptyList()
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val metadata = context.metadata() ?: return emptyList()
        val constructor = createConstructor(context.owner, ImportedSchemaCompanionKey(metadata), isPrimary = true)
        return listOf(constructor.symbol)
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        if (!session.predicateBasedProvider.matches(predicate, classSymbol)) return emptySet()
        return setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        @OptIn(SymbolInternals::class)
        if (owner is FirRegularClassSymbol && owner.companionObjectSymbol != null) {
            return null
        }
        val metadata = topLevelMetadata[context.owner.classId.shortClassName] ?: return null
        if (name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
            return createCompanionObject(owner, ImportedSchemaCompanionKey(metadata)) {
                val type = Names.DATAFRAME_PROVIDER
                    .constructClassLikeType(arrayOf(owner.defaultType()))
                superType(type)
            }.symbol
        }
        return super.generateNestedClassLikeDeclaration(owner, name, context)
    }
}