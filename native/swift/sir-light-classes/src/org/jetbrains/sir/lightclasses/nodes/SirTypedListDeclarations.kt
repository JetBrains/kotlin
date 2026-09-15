/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.containingModule
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.allSupertypes
import org.jetbrains.kotlin.analysis.api.types.defaultType
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirExtension
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirFunctionalType
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirScopeDefiningDeclaration
import org.jetbrains.kotlin.sir.SirStruct
import org.jetbrains.kotlin.sir.SirTypeVariance
import org.jetbrains.kotlin.sir.SirTypealias
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildExtension
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildProtocol
import org.jetbrains.kotlin.sir.builder.buildStruct
import org.jetbrains.kotlin.sir.builder.buildTypealias
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.optional
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.translateType
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.containingModule
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.swiftName
import kotlin.collections.emptyList

context(_: KaSession, sirSession: SirSession)
internal fun createSirTypedListDeclarations(
    declaration: SirScopeDefiningDeclaration
): SirTypedListDeclarations? {
    if (declaration !is SirAbstractClassFromKtSymbol && declaration !is SirProtocolFromKtSymbol) return null
    if (!declaration.sirSession.collectionsV2) return null
    val ktSymbol = declaration.ktSymbol
    val [isMutable, elementType] = ktSymbol.calculateListType() ?: return null
    val typedListProtocols = when (declaration) {
        // TODO: Support classes KT-88831
        is SirProtocolFromKtSymbol -> declaration.translatedProtocols.map { it.typedListDeclarations }
        else -> emptyList()
    }.filterIsInstance<SirTypedListDeclarations.Generic>().let { declarations ->
        buildList {
            if (isMutable && declarations.none { it.isMutable }) {
                add(KotlinRuntimeSupportModule.typedMutableList)
            } else if (declarations.isEmpty()) {
                add(KotlinRuntimeSupportModule.typedList)
            }
            addAll(declarations.map { it.typedListProtocol })
        }
    }
    if (elementType != null) {
        val elementTypeAlias = buildTypealias {
            name = "Element"
            type = elementType.translateType(
                SirTypeVariance.INVARIANT,
                reportErrorType = { error("Failed to translate type: $it") },
                reportUnsupportedType = { error("Failed to translate type: type is not supported") },
                processTypeImports = ktSymbol.containingModule.sirModule()::updateImports
            )
        }.apply { parent = declaration }
        // TODO: Fix and test concrete list types KT-88831
        // We need to add impl for rawCollection and conformsTo, fix multiple matches on size,
        // explicitly add inherited conformances on _KotlinExistential, possibly more
        return SirTypedListDeclarations.Concrete(
            typedListProtocols = typedListProtocols,
            elementTypeAlias = elementTypeAlias,
        )
    } else {
        val parent = declaration.parent
        val typedListProtocol = buildProtocol {
            name = "${declaration.name}_Typed"
            primaryAssociatedTypes.add("Element")
            protocols.addAll(typedListProtocols)
        }.apply { this.parent = parent }
        val declarationType = SirNominalType(declaration)
        val typedListExtension = buildExtension {
            extendedType = SirNominalType(typedListProtocol)
            buildVariable {
                name = "rawList"
                type = declarationType
                getter = buildGetter {
                    body = SirFunctionBody(listOf("return __rawCollection as! ${declarationType.swiftName}"))
                }
            }.apply { getter?.parent = this }.also(declarations::add)
        }.apply {
            declarations.forEach { it.parent = this }
            this.parent = declaration.containingModule()
        }
        val typedListStruct = buildStruct {
            name = "${typedListProtocol.name}Impl"
            typeParameters.add("Element")
            protocols.add(typedListProtocol)
            buildVariable {
                isConstant = true
                name = "__rawCollection"
                type = SirNominalType(KotlinRuntimeModule.kotlinBase)
            }.also(declarations::add)
            val conformsToType = SirFunctionalType(
                parameterTypes = listOf(SirNominalType(SirSwiftModule.anyClass).optional()),
                returnType = SirNominalType(SirSwiftModule.bool)
            )
            buildVariable {
                isConstant = true
                name = "__conformsTo"
                type = conformsToType
            }.also(declarations::add)
            buildInit {
                visibility = SirVisibility.PACKAGE
                isFailable = false
                SirParameter(
                    argumentName = "rawList",
                    type = declarationType
                ).also(parameters::add)
                SirParameter(
                    argumentName = "conformsTo",
                    type = conformsToType.copyAppendingAttributes(SirAttribute.Escaping)
                ).also(parameters::add)
                body = SirFunctionBody(listOf("self.__rawCollection = rawList", "self.__conformsTo = conformsTo"))
            }.also(declarations::add)
        }.apply {
            declarations.forEach { it.parent = this }
            this.parent = parent
        }
        return SirTypedListDeclarations.Generic(
            isMutable = isMutable,
            typedListProtocol = typedListProtocol,
            typedListExtension = typedListExtension,
            typedListStruct = typedListStruct,
        )
    }
}

internal sealed class SirTypedListDeclarations {
    class Generic(
        val isMutable: Boolean,
        val typedListProtocol: SirProtocol,
        val typedListExtension: SirExtension,
        val typedListStruct: SirStruct,
    ) : SirTypedListDeclarations()

    class Concrete(
        val typedListProtocols: List<SirProtocol>,
        val elementTypeAlias: SirTypealias,
    ) : SirTypedListDeclarations()
}

context(_: KaSession)
private fun KaNamedClassSymbol.calculateListType(): Pair<Boolean, KaClassType?>? {
    var isMutableList = false
    var isList = false
    var elementType: KaClassType? = null
    val types = sequence {
        val defaultType = defaultType
        yield(defaultType)
        yieldAll(defaultType.allSupertypes)
    }
    for (type in types) {
        if (type !is KaClassType) continue
        when (type.classId) {
            StandardClassIds.MutableList -> isMutableList = true
            StandardClassIds.List -> isList = true
            else -> continue
        }
        elementType = type.typeArguments.single().type as? KaClassType
        if (isMutableList) break
    }
    return when {
        isMutableList -> true to elementType
        isList -> false to elementType
        else -> null
    }
}
