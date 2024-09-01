/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class DataFrameLikeTypeMembersGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {

    @OptIn(SymbolInternals::class)
    private val propertiesCache: FirCache<FirClassSymbol<*>, Map<Name, List<FirProperty>>?, Nothing?> =
        session.firCachesFactory.createCache { k ->
            val callShapeData = k.fir.callShapeData ?: return@createCache null
            when (callShapeData) {
                is CallShapeData.RefinedType -> callShapeData.scopes.associate {
                    val propertyName = Name.identifier(it.name.asString().replaceFirstChar { it.lowercaseChar() })
                    propertyName to listOf(buildScopeReferenceProperty(it.classId, it, propertyName))
                }
                is CallShapeData.Scope -> callShapeData.columns.associate {
                    it.name to listOf(buildScopeApiProperty(callShapeData.token, scopeSymbol = k, it.name))
                }
                is CallShapeData.Schema -> callShapeData.columns.associate {
                    it.name to listOf(buildTokenProperty(k, it.name))
                }
            }
        }


    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val properties = propertiesCache.getValue(classSymbol)
        return properties?.flatMapTo(mutableSetOf(SpecialNames.INIT)) { it.value.map { it.name } } ?: emptySet()
    }


    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val constructor = createConstructor(context.owner, DataFrameLikeCallsRefinementExtension.Companion.KEY, isPrimary = true) {
            visibility = Visibilities.Local
        }
        return listOf(constructor.symbol)
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        return propertiesCache.getValue(owner)?.flatMap { it.value.map { it.symbol } } ?: emptyList()
    }

    private fun buildScopeApiProperty(
        tokenSymbol: FirClassSymbol<*>,
        scopeSymbol: FirClassSymbol<*>,
        propName: Name,
    ): FirProperty {
        return createMemberProperty(
            scopeSymbol,
            DataFrameLikeCallsRefinementExtension.Companion.KEY,
            propName,
            session.builtinTypes.intType.coneType
        ) {
            visibility = Visibilities.Local
            extensionReceiverType {
                ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(DataFrameLikeCallsRefinementExtension.DATAFRAME),
                    arrayOf(
                        ConeClassLikeTypeImpl(
                            ConeClassLookupTagWithFixedSymbol(tokenSymbol.classId, tokenSymbol),
                            emptyArray(),
                            isNullable = false
                        )
                    ),
                    isNullable = false
                )
            }
        }
    }

    private fun buildTokenProperty(
        tokenSymbol: FirClassSymbol<*>,
        propName: Name,
    ): FirProperty {
        return createMemberProperty(
            tokenSymbol,
            DataFrameLikeCallsRefinementExtension.Companion.KEY,
            propName,
            session.builtinTypes.intType.coneType
        ) {
            visibility = Visibilities.Local
        }
    }

    private fun buildScopeReferenceProperty(
        scope: ClassId,
        scopeSymbol: FirRegularClassSymbol,
        name: Name
    ): FirProperty {
        return createMemberProperty(
            scopeSymbol,
            DataFrameLikeCallsRefinementExtension.Companion.KEY,
            name,
            ConeClassLikeTypeImpl(
                ConeClassLookupTagWithFixedSymbol(scope, scopeSymbol),
                emptyArray(),
                isNullable = false
            )
        ) {
            visibility = Visibilities.Local
        }
    }
}
