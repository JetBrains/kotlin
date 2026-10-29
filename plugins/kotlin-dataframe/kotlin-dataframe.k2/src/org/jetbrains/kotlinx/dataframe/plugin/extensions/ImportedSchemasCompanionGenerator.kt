/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlinx.dataframe.plugin.ImportedSchemaCompanionKey
import org.jetbrains.kotlinx.dataframe.plugin.ImportedSchemaMetadata
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
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
                valueParameter(Name.identifier("path"), StandardTypes.String)
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

    override fun needsCompanionObject(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Boolean =
        session.predicateBasedProvider.matches(predicate, classSymbol) && topLevelMetadata[context.owner.classId.shortClassName] != null

    override fun getCompanionObjectInfo(
        owner: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): CompanionObjectInfo {
        val type = Names.DATAFRAME_PROVIDER.constructClassLikeType(arrayOf(owner.defaultType()))
        return CompanionObjectInfo(superTypes = listOf(type))
    }
}
