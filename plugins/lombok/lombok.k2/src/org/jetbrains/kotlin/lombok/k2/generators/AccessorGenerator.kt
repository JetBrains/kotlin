/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Accessors
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Getter
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Setter
import org.jetbrains.kotlin.lombok.k2.config.LombokService
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.utils.AccessorNames
import org.jetbrains.kotlin.lombok.utils.capitalize
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.collections.orEmpty

@OptIn(DirectDeclarationsAccess::class)
class AccessorGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val lombokService: LombokService
        get() = session.lombokService

    private val cache: FirCache<Pair<FirClassSymbol<*>, FirClassDeclaredMemberScope?>, Map<Name, List<FirJavaMethod>>?, Nothing?> =
        session.firCachesFactory.createCache(uncurry(::createAccessors))

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        return cache.getValue(classSymbol to context.declaredScope)?.keys ?: emptySet()
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner
        if (owner == null || !owner.isSuitableJavaClass()) return emptyList()
        val accessor = cache.getValue(owner to context.declaredScope)?.get(callableId.callableName) ?: return emptyList()
        return accessor.map { it.symbol }
    }

    private fun createAccessors(
        classSymbol: FirClassSymbol<*>,
        declaredScope: FirClassDeclaredMemberScope?
    ): Map<Name, List<FirJavaMethod>>? {
        val fieldsWithAccessor = computeFieldsWithAccessors(classSymbol) ?: return null
        val globalAccessors = lombokService.getAccessors(classSymbol)
        val explicitlyDeclaredFunctions = declaredScope?.collectAllFunctions()?.associateBy { it.name }.orEmpty()
        return buildMap {
            fieldsWithAccessor.forEach { (field, getter, setter) ->
                val dispatchReceiverType = runIf(!field.isStatic) { classSymbol.defaultType() }

                val getterName = getter?.let { computeGetterName(field, it, globalAccessors) }

                if (getterName != null && explicitlyDeclaredFunctions[getterName]?.valueParameterSymbols?.isEmpty() != true) {
                    val function = classSymbol.createJavaMethod(
                        name = getterName,
                        valueParameters = emptyList(),
                        returnTypeRef = field.returnTypeRef,
                        visibility = getter.visibility.toVisibility(),
                        modality = Modality.OPEN,
                        dispatchReceiverType = dispatchReceiverType,
                        isStatic = field.isStatic,
                    )

                    getOrPut(getterName) { mutableListOf() }.add(function)
                }

                val accessors = lombokService.getAccessorsIfAnnotated(field.symbol) ?: globalAccessors
                val setterName = setter?.let { computeSetterName(field, it, accessors) }
                if (setterName != null && explicitlyDeclaredFunctions[setterName].let { it?.valueParameterSymbols?.size != 1 }) {
                    val returnTypeRef = if (accessors.chain) {
                        buildResolvedTypeRef { coneType = classSymbol.defaultType() }
                    } else {
                        session.builtinTypes.unitType
                    }

                    val function = classSymbol.createJavaMethod(
                        name = setterName,
                        valueParameters = listOf(ConeLombokValueParameter(field.name, field.returnTypeRef)),
                        returnTypeRef = returnTypeRef,
                        visibility = setter.visibility.toVisibility(),
                        modality = Modality.OPEN,
                        dispatchReceiverType = dispatchReceiverType,
                        isStatic = field.isStatic,
                    )

                    getOrPut(setterName) { mutableListOf() }.add(function)
                }
            }
        }.takeIf { it.isNotEmpty() }
    }

    private data class FieldWithAccessors(
        val field: FirJavaField,
        val getter: Getter?,
        val setter: Setter?,
    )

    @OptIn(SymbolInternals::class)
    private fun computeFieldsWithAccessors(classSymbol: FirClassSymbol<*>): List<FieldWithAccessors>? {
        val data = lombokService.getData(classSymbol)

        val classGetter = lombokService.getGetter(classSymbol)
            ?: data?.asGetter()
            ?: lombokService.getValue(classSymbol)?.asGetter()

        val classSetter = lombokService.getSetter(classSymbol)
            ?: data?.asSetter()

        val hasValueAnnotation = lombokService.getValue(classSymbol) != null

        return buildList {
            for (declaration in classSymbol.fir.declarations) {
                if (declaration !is FirJavaField) continue

                val getter = lombokService.getGetter(declaration.symbol) ?: runIf(!declaration.isStatic) { classGetter }

                val setter = runIf(declaration.isVar && !hasValueAnnotation) {
                    lombokService.getSetter(declaration.symbol) ?: runIf(!declaration.isStatic) { classSetter }
                }

                if (getter != null || setter != null) {
                    add(FieldWithAccessors(declaration, getter, setter))
                }
            }
        }.takeIf { it.isNotEmpty() }
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