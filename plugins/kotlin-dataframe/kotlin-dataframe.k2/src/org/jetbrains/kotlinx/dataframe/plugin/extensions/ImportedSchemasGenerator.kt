/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.plugin.DataFramePlugin
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

@OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
class ImportedSchemasGenerator(
    session: FirSession,
    val predicate: LookupPredicate,
) : FirDeclarationGenerationExtension(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    private val predicateBasedProvider = session.predicateBasedProvider

    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(predicate).filterIsInstance<FirRegularClassSymbol>()
    }

    val topLevelSchemas: Map<Name, ImportedDataSchema> get() = session.importedSchemasService.topLevelSchemas
    val nestedSchemas: Map<Name, Map<Name, List<SimpleCol>>> get() = session.importedSchemasService.nestedSchemas

    val groupBy: Map<CallableId, List<ExtensionProperty>> by lazy {
        matchedClasses.flatMap { classSymbol ->
            buildList {
                topLevelSchemas[classSymbol.name]?.schema?.columns()?.mapTo(this) { column ->
                    val id = CallableId(classSymbol.packageFqName(), Name.identifier(column.name))
                    val returnType = generateReturnType(column, classSymbol, Name.identifier(column.name))

                    ExtensionProperty(id, returnType, owner = classSymbol)
                }

                nestedSchemas[classSymbol.name]?.entries?.forEach { (nested, columns) ->
                    columns.mapTo(this) { column ->
                        val id = CallableId(classSymbol.packageFqName(), Name.identifier(column.name))
                        val returnType = generateReturnType(column, classSymbol, Name.identifier(column.name))
                        val nestedClassId = ClassId(classSymbol.packageFqName(), classSymbol.name).createNestedClassId(nested)
                        val owner = nestedClassId.createConeType(session).toRegularClassSymbol(session) ?: error(nestedClassId)
                        ExtensionProperty(id, returnType, owner)
                    }
                }
            }
        }.groupBy { it.id }
    }

    data class ExtensionProperty(val id: CallableId, val returnType: ConeKotlinType, val owner: FirRegularClassSymbol)

    override fun getTopLevelCallableIds(): Set<CallableId> {
        return groupBy.keys
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val containingClassSymbol = classSymbol.getContainingClassSymbol()
        if (containingClassSymbol != null && session.predicateBasedProvider.matches(predicate, containingClassSymbol)) {
            return nestedSchemas[containingClassSymbol.name]?.get(classSymbol.name)?.map { Name.identifier(it.name) }?.toSet() ?: emptySet()
        }
        if (!session.predicateBasedProvider.matches(predicate, classSymbol)) return emptySet()
        return topLevelSchemas[classSymbol.name]?.schema?.columns()?.map { Name.identifier(it.name) }?.toSet() ?: emptySet()
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return generateTopLevelExtensions(callableId)
        val containingClassSymbol = owner.getContainingClassSymbol()
        val col = if (containingClassSymbol != null) {
            nestedSchemas[containingClassSymbol.name]?.get(owner.name)
        } else {
            topLevelSchemas[owner.name]?.schema?.columns()
        }?.find { it.name == callableId.callableName.identifier }


        if (col == null) return emptyList()
        val container = containingClassSymbol ?: owner
        val returnType = when (col) {
            is SimpleColumnGroup -> {
                val tag = container.classId.createNestedClassId(Name.identifier(col.name))
                Names.DATA_ROW_CLASS_ID.createConeType(session, arrayOf(tag.createConeType(session)))
            }
            is SimpleDataColumn -> col.type.type
            is SimpleFrameColumn -> {
                val tag = container.classId.createNestedClassId(Name.identifier(col.name))
                Names.DF_CLASS_ID.createConeType(session, arrayOf(tag.createConeType(session)))
            }
        }

        val property = createMemberProperty(context.owner, DataFramePlugin, Name.identifier(col.name), returnType) {
            modality = Modality.ABSTRACT
        }
        return listOf(property.symbol)
    }

    fun generateTopLevelExtensions(callableId: CallableId): List<FirPropertySymbol> {
        return TopLevelExtensionsGenerator.Receiver.entries.flatMap { mode ->
            groupBy[callableId]?.map {
                buildExtensionPropertiesApi(
                    callableId,
                    it.owner,
                    mode,
                    it.returnType,
                    null,
                    callableId.callableName
                )
            }.orEmpty()
        }
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        if (!session.predicateBasedProvider.matches(predicate, classSymbol)) return emptySet()
        return nestedSchemas[classSymbol.name]?.keys.orEmpty()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*> {
        return createNestedClass(owner, name, DataFramePlugin).symbol
    }

    private fun generateReturnType(
        column: SimpleCol,
        classSymbol: FirRegularClassSymbol,
        nested: Name,
    ): ConeKotlinType {
        // all nested schemas become nested classes!
        val marker = ClassId(classSymbol.packageFqName(), classSymbol.name).createNestedClassId(nested)
        val returnType = when (column) {
            is SimpleColumnGroup -> {
                Names.DATA_ROW_CLASS_ID.createConeType(session, arrayOf(marker.createConeType(session)))
            }
            is SimpleDataColumn -> column.type.type
            is SimpleFrameColumn -> {
                Names.DF_CLASS_ID.createConeType(session, arrayOf(marker.createConeType(session)))
            }
        }
        return returnType
    }
}