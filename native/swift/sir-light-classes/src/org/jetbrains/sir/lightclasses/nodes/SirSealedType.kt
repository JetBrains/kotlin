/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.components.expandedSymbol
import org.jetbrains.kotlin.analysis.api.components.sealedClassInheritors
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirEnum
import org.jetbrains.kotlin.sir.SirEnumCase
import org.jetbrains.kotlin.sir.SirFixity
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirInit
import org.jetbrains.kotlin.sir.SirModality
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirScopeDefiningDeclaration
import org.jetbrains.kotlin.sir.SirStruct
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirVariable
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildEnumCase
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.sirAvailability
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.toSir
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.sir.util.swiftIdentifier
import org.jetbrains.kotlin.sir.visibility
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.utils.enumCaseName
import org.jetbrains.sir.lightclasses.utils.superClassDeclaration

context(_: KaSession, sirSession: SirSession)
internal fun createSirSealedType(
    declaration: SirScopeDefiningDeclaration
): SirScopeDefiningDeclaration? {
    if (declaration !is SirAbstractClassFromKtSymbol && declaration !is SirProtocolFromKtSymbol) return null
    if (declaration.ktSymbol.isSealed) {
        SirSealedTypeEnum(declaration.ktSymbol, sirSession).takeUnless { it.isEmpty }?.let {
            it.parent = declaration.parent
            return it
        }
    }
    if (declaration.ktSymbol.superTypes.any { it.expandedSymbol.isSealed }) {
        return SirSealedTypeStruct(declaration.ktSymbol, sirSession, declaration).apply { parent = declaration.parent }
    }
    return null
}

private val SirScopeDefiningDeclaration.sealedType: SirScopeDefiningDeclaration?
    get() = when (this) {
        is SirAbstractClassFromKtSymbol -> sealedType
        is SirProtocolFromKtSymbol -> sealedType
        else -> null
    }

context(sirSession: SirSession)
internal fun createSirSealedTypeFunctions(
    declaration: SirScopeDefiningDeclaration,
): List<SirFunction> {
    require(declaration is SirAbstractClassFromKtSymbol || declaration is SirProtocolFromKtSymbol) {
        "Sealed type functions are only available for SirClasses and SirProtocols from KtSymbols"
    }
    val sealedType = declaration.sealedType ?: return emptyList()
    return buildList {
        val isSealedType = sealedType is SirSealedTypeEnum
        if (isSealedType) {
            add(SirSealedTypeFunction.Sealed(declaration.ktSymbol, sirSession, sealedType))
        }
        val superSealedType = declaration.superClassDeclaration?.sealedType as? SirSealedTypeEnum
        if (superSealedType != null) {
            add(SirSealedTypeFunction.Leaf(declaration.ktSymbol, sirSession, superSealedType, isSealedType, true))
        }
        for (protocol in declaration.protocols) {
            if (protocol !is SirProtocolFromKtSymbol) continue
            val sealedType = protocol.sealedType as? SirSealedTypeEnum ?: continue
            add(SirSealedTypeFunction.Leaf(declaration.ktSymbol, sirSession, sealedType, isSealedType, false))
        }
    }
}

private class SirSealedTypeEnum(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
) : SirEnum(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val name: String = ktSymbol.name.asString() + "_SealedType"
    override val origin: SirOrigin get() = SirOrigin.SealedType(KotlinSource(ktSymbol))
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? = null
    override lateinit var parent: SirDeclarationParent
    override val attributes: List<SirAttribute> get() = sealedDeclaration.propagatedAttributes
    override val protocols: List<SirProtocol>
        get() = listOf(KotlinRuntimeSupportModule.sealedType)

    private val sealedDeclaration: SirScopeDefiningDeclaration by lazyWithSessions {
        ktSymbol.toSir().primaryDeclaration as? SirScopeDefiningDeclaration
            ?: error("Failed to get declaration for sealed type ($ktSymbol)")
    }

    private val sealedInheritors: List<KaNamedClassSymbol> by lazyWithSessions {
        ktSymbol.sealedClassInheritors
    }

    val isEmpty: Boolean get() = sealedInheritors.isEmpty()

    val cases: List<Pair<ClassId, SirEnumCase?>> by lazyWithSessions {
        val names = createUniqueCaseNames(sealedInheritors)
        sealedInheritors.map { ktSymbol ->
            val classId = ktSymbol.classId ?: error("Sealed type ($ktSymbol) must have a classId")

            val visibility = ktSymbol.sirAvailability().visibility ?: SirVisibility.PRIVATE
            if (visibility <= SirVisibility.INTERNAL) return@map classId to null

            val declaration = ktSymbol.toSir().primaryDeclaration as? SirScopeDefiningDeclaration
                ?: error("Failed to get declaration for sealed type ($ktSymbol)")
            if (declaration.isUnavailable) return@map classId to null

            val sealedType = declaration.sealedType ?: error("Leaf declaration ($declaration) must have a SealedType")
            val case = buildEnumCase {
                name = names.getValue(ktSymbol).enumCaseName
                associatedValueTypes.add(SirNominalType(sealedType))
                attributes.addAll(declaration.propagatedAttributes)
            }.apply { parent = this@SirSealedTypeEnum }
            classId to case
        }
    }

    val unknownType: SirStruct? by lazyWithSessions {
        if (cases.none { it.second == null }) return@lazyWithSessions null
        SirSealedTypeStruct(ktSymbol, sirSession, sealedDeclaration, "Unknown").apply { parent = this@SirSealedTypeEnum }
    }

    val unknownCase: SirEnumCase? by lazyWithSessions {
        val type = SirNominalType(unknownType ?: return@lazyWithSessions null)
        buildEnumCase {
            name = "unknown"
            associatedValueTypes.add(type)
        }.apply { parent = this@SirSealedTypeEnum }
    }

    private val valueVariable: SirVariable by lazy {
        buildVariable {
            name = "value"
            type = SirNominalType(sealedDeclaration)
            getter = buildGetter {
                val statement = buildList {
                    cases.forEach { addIfNotNull(it.second) }
                    addIfNotNull(unknownCase)
                }.joinToString(prefix = "switch self {\n", separator = "\n", postfix = "\n}") { case ->
                    "case let .${case.name.swiftIdentifier}(type): type.value"
                }
                body = SirFunctionBody(listOf(statement))
            }
        }.apply {
            parent = this@SirSealedTypeEnum
            getter?.parent = this
        }
    }

    override val declarations: List<SirDeclaration> by lazyWithSessions {
        buildList {
            cases.forEach { addIfNotNull(it.second) }
            addIfNotNull(unknownCase)
            addIfNotNull(unknownType)
            add(valueVariable)
        }
    }
}

private class SirSealedTypeStruct(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
    private val declaration: SirScopeDefiningDeclaration,
    name: String? = null,
) : SirStruct(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val name: String = name ?: (declaration.name + "_SealedType")
    override val origin: SirOrigin get() = SirOrigin.SealedType(KotlinSource(ktSymbol))
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? = null
    override lateinit var parent: SirDeclarationParent
    override val attributes: List<SirAttribute> get() = declaration.propagatedAttributes
    override val protocols: List<SirProtocol>
        get() = listOf(KotlinRuntimeSupportModule.sealedType)

    private val valueType = SirNominalType(declaration)

    private val valueVariable: SirVariable by lazy {
        buildVariable {
            isConstant = true
            this.name = "value"
            type = valueType
        }.apply { parent = this@SirSealedTypeStruct }
    }

    private val init: SirInit by lazy {
        buildInit {
            visibility = SirVisibility.INTERNAL
            isFailable = false
            parameters += SirParameter(null, "value", valueType)
            body = SirFunctionBody(listOf("self.value = value"))
        }.apply { parent = this@SirSealedTypeStruct }
    }

    override val declarations: List<SirDeclaration> by lazy {
        listOf(valueVariable, init)
    }
}

private sealed class SirSealedTypeFunction : SirFunction(), SirFromKtSymbol<KaNamedClassSymbol> {

    protected abstract val sealedType: SirSealedTypeEnum
    override val returnType: SirType get() = SirNominalType(sealedType)

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val origin: SirOrigin get() = SirOrigin.SealedType(KotlinSource(ktSymbol))
    override val name: String = "sealedType"
    override val contextParameter: SirParameter? = null
    override val extensionReceiverParameter: SirParameter? = null
    override val parameters: List<SirParameter> get() = emptyList()
    override val documentation: String? = null
    override lateinit var parent: SirDeclarationParent
    override val isInstance: Boolean = true
    override val fixity: SirFixity? = null
    override val errorType: SirType get() = SirType.never
    override val isAsync: Boolean = false
    override val bridges: List<SirBridge> get() = emptyList()

    class Sealed(
        override val ktSymbol: KaNamedClassSymbol,
        override val sirSession: SirSession,
        override val sealedType: SirSealedTypeEnum,
    ) : SirSealedTypeFunction() {
        override val isOverride: Boolean = false
        override val modality: SirModality = SirModality.OPEN
        override val attributes: List<SirAttribute> get() = sealedType.propagatedAttributes
        override var body: SirFunctionBody?
            set(_) = Unit
            get() {
                val statement = when (val unknownCase = sealedType.unknownCase) {
                    null -> """fatalError("must implement sealedType in subclass")"""
                    else -> ".${unknownCase.name.swiftIdentifier}(.init(self))"
                }
                return SirFunctionBody(listOf(statement))
            }
    }

    class Leaf(
        override val ktSymbol: KaNamedClassSymbol,
        override val sirSession: SirSession,
        override val sealedType: SirSealedTypeEnum,
        private val isSealedType: Boolean,
        override val isOverride: Boolean,
    ) : SirSealedTypeFunction() {
        override val modality: SirModality = SirModality.FINAL
        private val enumCase: SirEnumCase by lazy {
            val classId = ktSymbol.classId
            sealedType.cases.firstOrNull { it.first == classId }?.let {
                it.second ?: sealedType.unknownCase
            } ?: error("Leaf class ($classId) must have a SealedType case")
        }
        override val attributes: List<SirAttribute> by lazy {
            buildList {
                addAll(sealedType.propagatedAttributes)
                if (isSealedType) add(SirAttribute.DisfavoredOverload)
            }
        }
        override var body: SirFunctionBody?
            set(_) = Unit
            get() {
                val type = if (isSealedType && enumCase != sealedType.unknownCase) "${name.swiftIdentifier}()" else ".init(self)"
                val statement = ".${enumCase.name.swiftIdentifier}($type)"
                return SirFunctionBody(listOf(statement))
            }
    }
}

private val KaClassSymbol?.isSealed: Boolean
    get() = this?.modality == KaSymbolModality.SEALED

private val SirDeclaration.propagatedAttributes: List<SirAttribute>
    get() = attributes.filter { it is SirAttribute.Available || it is SirAttribute.SPI }

context(_: SirSession, _: KaSession)
private fun createUniqueCaseNames(
    symbols: List<KaNamedClassSymbol>
): Map<KaNamedClassSymbol, String> {
    val fqNames = symbols.associateWith { symbol ->
        buildList {
            var symbol: KaDeclarationSymbol? = symbol
            while (symbol != null) {
                add(symbol.sirDeclarationName())
                symbol = symbol.containingDeclaration
            }
        }.asReversed()
    }
    val names = mutableMapOf("" to symbols.toMutableList())
    var index = 1
    do {
        val conflicts = names.filter { it.key.isEmpty() || it.value.size > 1 }
        for (conflict in conflicts) {
            for (symbol in conflict.value) {
                val fqName = fqNames.getValue(symbol).takeLast(index).joinToString(".")
                if (fqName == conflict.key) error("Failed to create unique SealedType enum case name for $symbol")
                names.getOrPut(fqName) { mutableListOf() }.add(symbol)
            }
            names.remove(conflict.key)
        }
        index++
    } while (conflicts.isNotEmpty())
    return buildMap { names.forEach { put(it.value.single(), it.key) } }
}
