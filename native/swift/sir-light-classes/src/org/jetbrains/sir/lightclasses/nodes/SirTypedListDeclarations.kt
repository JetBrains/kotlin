/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import com.intellij.util.containers.addIfNotNull
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
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildExtension
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildProtocol
import org.jetbrains.kotlin.sir.builder.buildStruct
import org.jetbrains.kotlin.sir.builder.buildTypealias
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.nonOptional
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
import org.jetbrains.sir.lightclasses.utils.superClassDeclaration
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
        is SirAbstractClassFromKtSymbol -> buildList {
            addIfNotNull((declaration.superClassDeclaration as? SirAbstractClassFromKtSymbol)?.typedListDeclarations)
            addAll(declaration.translatedProtocols.map { it.typedListDeclarations })
        }
        is SirProtocolFromKtSymbol -> declaration.translatedProtocols.map { it.typedListDeclarations }
        else -> emptyList()
    }.filterIsInstance<SirTypedListDeclarations.Generic>().let { declarations ->
        buildList {
            if (isMutable && declarations.none { it.isMutable }) {
                add(KotlinRuntimeSupportModule.typedMutableList)
            } else if (declarations.isEmpty()) {
                // TODO: class doesn't conform to List protocol (generic interfaces are ignored) KT-88831
                add(KotlinRuntimeSupportModule.typedList)
            }
            addAll(declarations.map { it.typedListProtocol })
        }
    }
    val declarationType = SirNominalType(declaration)
    val kotlinBaseType = SirNominalType(KotlinRuntimeModule.kotlinBase)
    val conformsToType = SirFunctionalType(
        parameterTypes = listOf(SirNominalType(SirSwiftModule.anyClass).optional()),
        returnType = SirNominalType(SirSwiftModule.bool)
    )
    if (elementType != null) {
        val translatedElementType = elementType.translateType(
            SirTypeVariance.INVARIANT,
            reportErrorType = { error("Failed to translate type: $it") },
            reportUnsupportedType = { error("Failed to translate type: type is not supported") },
            processTypeImports = ktSymbol.containingModule.sirModule()::updateImports
        )
        val typedListExtension = buildExtension {
            extendedType = declarationType
            protocols.addAll(typedListProtocols)
            buildTypealias {
                name = "Element"
                type = translatedElementType
            }.also(declarations::add)
            buildVariable {
                name = "__rawCollection"
                type = kotlinBaseType
                getter = buildGetter {
                    body = SirFunctionBody(listOf("return self"))
                }
            }.apply { getter?.parent = this }.also(declarations::add)
            buildVariable {
                name = "__conformsTo"
                type = conformsToType
                getter = buildGetter {
                    val elementFqName = translatedElementType.nonOptional().swiftName
                    body = SirFunctionBody(listOf("return { $0 is $elementFqName }"))
                }
            }.apply { getter?.parent = this }.also(declarations::add)
        }.apply {
            declarations.forEach { it.parent = this }
            this.parent = declaration.containingModule()
        }
        return SirTypedListDeclarations.Concrete(typedListExtension)
    } else {
        val parent = declaration.parent
        val typedListProtocol = buildProtocol {
            name = "${declaration.name}_Typed"
            primaryAssociatedTypes.add("Element")
            protocols.addAll(typedListProtocols)
        }.apply { this.parent = parent }
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
                type = kotlinBaseType
            }.also(declarations::add)
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

    abstract fun asTriple(): Triple<SirProtocol?, SirExtension, SirStruct?>

    class Generic(
        val isMutable: Boolean,
        val typedListProtocol: SirProtocol,
        val typedListExtension: SirExtension,
        val typedListStruct: SirStruct,
    ) : SirTypedListDeclarations() {
        override fun asTriple(): Triple<SirProtocol?, SirExtension, SirStruct?> =
            Triple(typedListProtocol, typedListExtension, typedListStruct)
    }

    class Concrete(
        val typedListExtension: SirExtension,
    ) : SirTypedListDeclarations() {
        override fun asTriple(): Triple<SirProtocol?, SirExtension, SirStruct?> =
            Triple(null, typedListExtension, null)
    }
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
