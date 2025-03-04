/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

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
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Accessors
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Getter
import org.jetbrains.kotlin.lombok.k2.config.LombokService
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.utils.AccessorNames
import org.jetbrains.kotlin.lombok.utils.capitalize
import org.jetbrains.kotlin.lombok.utils.collectWithNotNull
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

@OptIn(DirectDeclarationsAccess::class)
class GetterGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val lombokService: LombokService
        get() = session.lombokService

    private val cache: FirCache<Pair<FirClassSymbol<*>, FirClassDeclaredMemberScope?>, Map<Name, FirJavaMethod>?, Nothing?> =
        session.firCachesFactory.createCache(uncurry(::createGetters))

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        return cache.getValue(classSymbol to context.declaredScope)?.keys ?: emptySet()
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner
        if (owner == null || !owner.isSuitableJavaClass()) return emptyList()
        val getter = cache.getValue(owner to context.declaredScope)?.get(callableId.callableName) ?: return emptyList()
        return listOf(getter.symbol)
    }

    private fun createGetters(classSymbol: FirClassSymbol<*>, declaredScope: FirClassDeclaredMemberScope?): Map<Name, FirJavaMethod>? {
        val fieldsWithGetter = computeFieldsWithGetter(classSymbol) ?: return null
        val globalAccessors = lombokService.getAccessors(classSymbol)
        val explicitlyDeclaredFunctions = declaredScope?.collectAllFunctions()?.associateBy { it.name }.orEmpty()
        return fieldsWithGetter.mapNotNull { (field, getterInfo) ->
            val getterName = computeGetterName(field, getterInfo, globalAccessors) ?: return@mapNotNull null
            if (explicitlyDeclaredFunctions[getterName]?.valueParameterSymbols?.isEmpty() == true) {
                return@mapNotNull null
            }
            val function = buildJavaMethod {
                containingClassSymbol = classSymbol
                moduleData = field.moduleData
                returnTypeRef = field.returnTypeRef
                dispatchReceiverType = classSymbol.defaultType()
                name = getterName
                symbol = FirNamedFunctionSymbol(CallableId(classSymbol.classId, getterName))
                val visibility = getterInfo.visibility.toVisibility()
                status = FirResolvedDeclarationStatusImpl(visibility, Modality.OPEN, visibility.toEffectiveVisibility(classSymbol))
                isStatic = false
                isFromSource = true
            }
            getterName to function
        }.toMap()
    }

    @OptIn(SymbolInternals::class)
    private fun computeFieldsWithGetter(classSymbol: FirClassSymbol<*>): List<Pair<FirJavaField, Getter>>? {
        val classGetter = lombokService.getGetter(classSymbol)
            ?: lombokService.getData(classSymbol)?.asGetter()
            ?: lombokService.getValue(classSymbol)?.asGetter()

        return classSymbol.fir.declarations
            .filterIsInstance<FirJavaField>()
            .collectWithNotNull { lombokService.getGetter(it.symbol) ?: classGetter }
            .takeIf { it.isNotEmpty() }
    }

    private fun computeGetterName(field: FirJavaField, getterInfo: Getter, globalAccessors: Accessors): Name? {
        if (getterInfo.visibility == AccessLevel.NONE) return null
        val accessors = lombokService.getAccessorsIfAnnotated(field.symbol) ?: globalAccessors
        val propertyName = field.toAccessorBaseName(accessors) ?: return null
        val functionName = if (accessors.fluent) {
            propertyName
        } else {
            val prefix = if (field.returnTypeRef.isPrimitiveBoolean() && !accessors.noIsPrefix) AccessorNames.IS else AccessorNames.GET
            prefix + propertyName.capitalize()
        }
        return Name.identifier(functionName)
    }
}
