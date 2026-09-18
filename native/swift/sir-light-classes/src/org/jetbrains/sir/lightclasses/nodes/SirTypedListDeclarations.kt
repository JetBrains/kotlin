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
import org.jetbrains.kotlin.sir.SirExtension
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirFunctionalType
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirScopeDefiningDeclaration
import org.jetbrains.kotlin.sir.SirStruct
import org.jetbrains.kotlin.sir.SirTypeVariance
import org.jetbrains.kotlin.sir.SirTypealias
import org.jetbrains.kotlin.sir.builder.buildExtension
import org.jetbrains.kotlin.sir.builder.buildGetter
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

context(_: KaSession, sirSession: SirSession)
internal fun createSirTypedListDeclarations(
    declaration: SirScopeDefiningDeclaration
): SirTypedListDeclarations? {
    if (declaration !is SirAbstractClassFromKtSymbol && declaration !is SirProtocolFromKtSymbol) return null
    if (!declaration.sirSession.collectionsV2) return null
    val ktSymbol = declaration.ktSymbol
    val [isMutable, elementType] = ktSymbol.calculateListType() ?: return null
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
        return SirTypedListDeclarations.Concrete(
            listProtocol = if (isMutable) KotlinRuntimeSupportModule.mutableList else KotlinRuntimeSupportModule.list,
            elementTypeAlias = elementTypeAlias,
        )
    } else {
        // TODO: Add missing generic to protocol, struct and init KT-88831
        val parent = declaration.parent
        val typedListProtocol = buildProtocol {
            name = "${declaration.name}_Typed"
            primaryAssociatedTypes.add("Element")
            protocols.add(if (isMutable) KotlinRuntimeSupportModule.typedMutableList else KotlinRuntimeSupportModule.typedList)
        }.apply { this.parent = parent }
        val typedListExtension = buildExtension {
            extendedType = SirNominalType(typedListProtocol)
            val declarationType = SirNominalType(declaration)
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
            protocols.add(typedListProtocol)
            buildVariable {
                isConstant = true
                name = "__rawCollection"
                type = SirNominalType(KotlinRuntimeModule.kotlinBase)
            }.also(declarations::add)
            buildVariable {
                isConstant = true
                name = "__conformsTo"
                type = SirFunctionalType(
                    parameterTypes = listOf(SirNominalType(SirSwiftModule.anyClass).optional()),
                    returnType = SirNominalType(SirSwiftModule.bool)
                )
            }.also(declarations::add)
            // TODO: Add init KT-88831
        }.apply {
            declarations.forEach { it.parent = this }
            this.parent = parent
        }
        return SirTypedListDeclarations.Generic(
            typedListProtocol = typedListProtocol,
            typedListExtension = typedListExtension,
            typedListStruct = typedListStruct,
        )
    }
}

internal sealed class SirTypedListDeclarations {
    class Generic(
        val typedListProtocol: SirProtocol,
        val typedListExtension: SirExtension,
        val typedListStruct: SirStruct,
    ) : SirTypedListDeclarations()

    class Concrete(
        val listProtocol: SirProtocol,
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
