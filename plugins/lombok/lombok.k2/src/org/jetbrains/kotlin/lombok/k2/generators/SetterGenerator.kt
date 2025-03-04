/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.buildJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameter
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Accessors
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Setter
import org.jetbrains.kotlin.lombok.k2.config.LombokService
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.utils.AccessorNames
import org.jetbrains.kotlin.lombok.utils.capitalize
import org.jetbrains.kotlin.lombok.utils.collectWithNotNull
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

@OptIn(DirectDeclarationsAccess::class)
class SetterGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val lombokService: LombokService
        get() = session.lombokService

    private val cache: FirCache<Pair<FirClassSymbol<*>, FirClassDeclaredMemberScope?>, Map<Name, FirJavaMethod>?, Nothing?> =
        session.firCachesFactory.createCache(uncurry(::createSetters))

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableForSetters()) return emptySet()
        return cache.getValue(classSymbol to context.declaredScope)?.keys ?: emptySet()
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner
        if (owner == null || !owner.isSuitableForSetters()) return emptyList()
        val getter = cache.getValue(owner to context.declaredScope)?.get(callableId.callableName) ?: return emptyList()
        return listOf(getter.symbol)
    }

    private fun FirClassSymbol<*>.isSuitableForSetters(): Boolean {
        return isSuitableJavaClass() && classKind != ClassKind.ENUM_CLASS
    }

    private fun createSetters(classSymbol: FirClassSymbol<*>, declaredScope: FirClassDeclaredMemberScope?): Map<Name, FirJavaMethod>? {
        val fieldsWithSetter = computeFieldsWithSetters(classSymbol) ?: return null
        val globalAccessors = lombokService.getAccessors(classSymbol)
        val explicitlyDeclaredFunctions = declaredScope?.collectAllFunctions()?.associateBy { it.name }.orEmpty()
        return fieldsWithSetter.mapNotNull { (field, setterInfo) ->
            val accessors = lombokService.getAccessorsIfAnnotated(field.symbol) ?: globalAccessors
            val setterName = computeSetterName(field, setterInfo, accessors) ?: return@mapNotNull null
            val existing = explicitlyDeclaredFunctions[setterName]
            if (existing != null && existing.valueParameterSymbols.size == 1) {
                return@mapNotNull null
            }
            val function = buildJavaMethod {
                containingClassSymbol = classSymbol
                moduleData = field.moduleData
                returnTypeRef = if (accessors.chain) {
                    buildResolvedTypeRef {
                        coneType = classSymbol.defaultType()
                    }
                } else {
                    session.builtinTypes.unitType
                }

                dispatchReceiverType = classSymbol.defaultType()
                name = setterName
                symbol = FirNamedFunctionSymbol(CallableId(classSymbol.classId, setterName))
                val visibility = setterInfo.visibility.toVisibility()
                status = FirResolvedDeclarationStatusImpl(visibility, Modality.OPEN, visibility.toEffectiveVisibility(classSymbol))

                valueParameters += buildJavaValueParameter {
                    moduleData = field.moduleData
                    containingDeclarationSymbol = this@buildJavaMethod.symbol
                    returnTypeRef = field.returnTypeRef
                    name = field.name
                    isVararg = false
                    isFromSource = true
                }

                isStatic = false
                isFromSource = true
            }
            setterName to function
        }.toMap()
    }

    @OptIn(SymbolInternals::class)
    private fun computeFieldsWithSetters(classSymbol: FirClassSymbol<*>): List<Pair<FirJavaField, Setter>>? {
        val classSetter = lombokService.getSetter(classSymbol)
            ?: lombokService.getData(classSymbol)?.asSetter()

        return classSymbol.fir.declarations
            .filterIsInstance<FirJavaField>()
            .filter { it.isVar }
            .collectWithNotNull { lombokService.getSetter(it.symbol) ?: classSetter }
            .takeIf { it.isNotEmpty() }
    }

    private fun computeSetterName(field: FirJavaField, setterInfo: Setter, accessors: Accessors): Name? {
        if (setterInfo.visibility == AccessLevel.NONE) return null
        val propertyName = field.toAccessorBaseName(accessors) ?: return null
        val functionName = if (accessors.fluent) {
            propertyName
        } else {
            AccessorNames.SET + propertyName.capitalize()
        }
        return Name.identifier(functionName)
    }
}
