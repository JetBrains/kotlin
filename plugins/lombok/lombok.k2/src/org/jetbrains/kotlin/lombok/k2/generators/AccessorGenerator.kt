/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
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
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Accessors
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Getter
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Setter
import org.jetbrains.kotlin.lombok.k2.config.LombokService
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.utils.AccessorNames
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.lombok.utils.decapitalize
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.collections.orEmpty

@OptIn(DirectDeclarationsAccess::class)
class AccessorGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val lombokService: LombokService
        get() = session.lombokService

    private val cache: FirCache<Pair<FirClassSymbol<*>, FirClassDeclaredMemberScope?>, Map<Name, List<FirJavaMethod>>?, Nothing?> =
        session.firCachesFactory.createCache(::createMethods)

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

    private fun createMethods(key: Pair<FirClassSymbol<*>, FirClassDeclaredMemberScope?>): Map<Name, List<FirJavaMethod>>? {
        val (classSymbol, declaredScope) = key
        val data = lombokService.getData(classSymbol)
        val fieldsWithAccessor = computeFieldsWithAccessors(classSymbol, data) ?: return null
        val globalAccessors = lombokService.getAccessors(classSymbol)
        val explicitlyDeclaredFunctions = declaredScope?.collectAllFunctions()?.associateBy { it.name }.orEmpty()
        return buildMap {
            fieldsWithAccessor.forEach { (field, getter, setter) ->
                val dispatchReceiverType = runIf(!field.isStatic) { classSymbol.defaultType() }
                val localAccessors = lombokService.getAccessorsIfAnnotated(field.symbol)

                val getterName = getter?.let { computeAccessorName(field, it, localAccessors, globalAccessors) }

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

                val setterName = setter?.let { computeAccessorName(field, it, localAccessors, globalAccessors) }

                if (setterName != null && explicitlyDeclaredFunctions[setterName].let { it?.valueParameterSymbols?.size != 1 }) {
                    val returnTypeRef = if (
                        localAccessors?.chain ?: globalAccessors.chain ?: false ||
                        localAccessors?.fluent ?: globalAccessors.fluent ?: false
                    ) {
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

            if (data != null) {
                getOrPut(LombokNames.CAN_EQUAL) { mutableListOf() }.add(
                    classSymbol.createJavaMethod(
                        name = LombokNames.CAN_EQUAL,
                        valueParameters = listOf(ConeLombokValueParameter(Name.identifier("other"), session.builtinTypes.nullableAnyType)),
                        returnTypeRef = session.builtinTypes.booleanType,
                        visibility = JavaVisibilities.ProtectedAndPackage,
                        modality = Modality.OPEN,
                    )
                )
            }
        }.takeIf { it.isNotEmpty() }
    }

    private data class FieldWithAccessors(
        val field: FirJavaField,
        val getter: Getter?,
        val setter: Setter?,
    )

    @OptIn(SymbolInternals::class)
    private fun computeFieldsWithAccessors(classSymbol: FirClassSymbol<*>, data: ConeLombokAnnotations.Data?): List<FieldWithAccessors>? {
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

    private fun computeAccessorName(
        field: FirJavaField,
        accessorInfo: ConeLombokAnnotations.AbstractAccessor,
        localAccessors: Accessors?,
        globalAccessors: Accessors,
    ): Name? {
        if (accessorInfo.visibility == AccessLevel.NONE) return null

        val prefixes = localAccessors?.prefix ?: globalAccessors.prefix ?: emptyList()
        // Don't generate the accessor if the field doesn't match any provided prefix
        val propertyName = field.extractPropertyNameOrNull(prefixes) ?: return null

        val isPrimitiveBoolean = field.returnTypeRef.isPrimitiveBoolean()
        val functionName = if (localAccessors?.fluent ?: globalAccessors.fluent ?: false) {
            propertyName
        } else {
            val prefix = when (accessorInfo) {
                is Getter -> {
                    if (isPrimitiveBoolean && !globalAccessors.noIsPrefix)
                        AccessorNames.IS
                    else
                        AccessorNames.GET
                }
                is Setter -> {
                    AccessorNames.SET
                }
            }

            prefix + propertyName.normalizeAndCapitalize(isPrimitiveBoolean)
        }
        return Name.identifier(functionName)
    }

    private fun FirJavaField.extractPropertyNameOrNull(prefixes: List<String>): String? {
        val fieldId = name.identifier
        if (prefixes.isEmpty()) {
            return fieldId
        }

        return prefixes.firstNotNullOfOrNull {
            if (it.isEmpty()) {
                return@firstNotNullOfOrNull fieldId
            }
            if (fieldId.isPrefixed(it))
                fieldId.removePrefix(it).decapitalize()
            else
                null
        }
    }
}