/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.components.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirEnum
import org.jetbrains.kotlin.sir.SirEnumCase
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirInit
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirVariable
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildFunctionCopy
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildInitCopy
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.implicitlyUnwrappedOptional
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.extractDeclarations
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.baseBridgeName
import org.jetbrains.sir.lightclasses.utils.bridgeFqName
import org.jetbrains.sir.lightclasses.utils.relocatedDeclarationNamePrefix
import org.jetbrains.sir.lightclasses.utils.translatedAttributes

internal fun createSirEnumFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession,
): SirEnum = SirEnumFromKtSymbol(
    ktSymbol,
    sirSession
)

private class SirEnumFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
) : SirEnum(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val origin: KotlinSource by lazy {
        KotlinSource(ktSymbol)
    }
    override val visibility: SirVisibility by lazy {
        SirVisibility.PUBLIC
    }
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override val name: String by lazyWithSessions {
        (this@SirEnumFromKtSymbol.relocatedDeclarationNamePrefix() ?: "") + ktSymbol.sirDeclarationName()
    }
    override val protocols: List<SirProtocol> by lazyWithSessions {
        listOf(
            KotlinRuntimeSupportModule.kotlinBridgeable,
            SirSwiftModule.caseIterable,
            SirSwiftModule.losslessStringConvertible,
            SirSwiftModule.rawRepresentable
        )
    }
    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit
    override val declarations: MutableList<SirDeclaration> by lazyWithSessions {
        mutableListOf<SirDeclaration>().apply {
            addAll(childDeclarations)
            addAll(syntheticDeclarations())
        }
    }
    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
    private val cases: List<SirEnumCaseFromKtSymbol> get() = childDeclarations.filterIsInstance<SirEnumCaseFromKtSymbol>()
    private val childDeclarations: List<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope
            .extractDeclarations()
            .toList()
    }

    private fun syntheticDeclarations(): List<SirDeclaration> = listOf(
        kotlinBaseInitDeclaration(),
        kotlinBridgeableExternalRcRef(),
        description(),
        failableInitFromString(),
        rawValue(),
        failableInitFromInteger(),
    )

    private fun kotlinBaseInitDeclaration(): SirDeclaration = buildInitCopy(KotlinRuntimeModule.kotlinBaseDesignatedInit) {
        origin = SirOrigin.KotlinBaseInitOverride(`for` = KotlinSource(ktSymbol))
        parameters[0] = SirParameter(
            argumentName = "__externalRCRefUnsafe",
            type = unsafeMutableRawPointerFlexibleType()
        )
        val separator = "\n                    "
        val caseSelector = cases.joinToString(separator = separator) {
            "case ${it.nativeCaseRepresentation(ktSymbol)}: self = .${it.name}"
        } + defaultBranch(separator)
        body = SirFunctionBody(
            listOf(
                """
                    switch __externalRCRefUnsafe {
                    $caseSelector
                    }
                """.trimIndent()
            )
        )
    }.also { it.parent = this }

    private fun kotlinBridgeableExternalRcRef(): SirFunction = buildFunctionCopy(KotlinRuntimeSupportModule.kotlinBridgeableExternalRcRef) {
        origin = SirOrigin.KotlinBridgeableExternalRcRefOverride(`for` = KotlinSource(ktSymbol))
        returnType = unsafeMutableRawPointerFlexibleType()
        val separator = "\n                    "
        val caseSelector = cases.joinToString(separator = separator) {
            "case .${it.name}: ${it.nativeCaseRepresentation(ktSymbol)}"
        } + defaultBranch(separator)
        body = SirFunctionBody(
            listOf(
                """
                    return switch self {
                    $caseSelector
                    }
                """.trimIndent()
            )
        )
    }.also { it.parent = this }

    private fun defaultBranch(separator: String): String =
        (if (cases.isNotEmpty()) separator else "") + "default: fatalError()"

    private fun description(): SirVariable = buildVariable {
        name = "description"
        type = SirNominalType(SirSwiftModule.string)
        val separator = "\n                        "
        getter = buildGetter {
            val caseSelector = cases.joinToString(separator = separator) {
                "case .${it.name}: \"${it.name}\""
            } + defaultBranch(separator)
            body = SirFunctionBody(
                listOf(
                    """
                        switch self {
                        $caseSelector
                        }
                    """.trimIndent()
                )
            )
        }
    }.also { it.parent = this }

    private fun failableInitFromString(): SirInit = buildInit {
        isFailable = true
        parameters.add(
            SirParameter(
                parameterName = "description",
                type = SirNominalType(SirSwiftModule.string),
            )
        )
        val caseSelector = cases.joinToString(separator = "\n                        ") {
            """case "${it.name}": self = .${it.name}"""
        }
        body = SirFunctionBody(
            listOf(
                """
                        switch description {
                        $caseSelector
                        default: return nil
                        }
                    """.trimIndent()
            )
        )
    }.also { it.parent = this }

    private fun rawValue(): SirVariable = buildVariable {
        name = "rawValue"
        type = SirNominalType(SirSwiftModule.int32)
        val separator = "\n                        "
        var index = 0
        getter = buildGetter {
            val caseSelector = cases.joinToString(separator = separator) {
                "case .${it.name}: ${index++}"
            } + defaultBranch(separator)
            body = SirFunctionBody(
                listOf(
                    """
                        switch self {
                        $caseSelector
                        }
                    """.trimIndent()
                )
            )
        }
    }.also { it.parent = this }

    private fun failableInitFromInteger(): SirInit = buildInit {
        isFailable = true
        parameters.add(
            SirParameter(
                argumentName = "rawValue",
                type = SirNominalType(SirSwiftModule.int32),
            )
        )
        body = SirFunctionBody(
            listOf(
                """
                    guard 0..<${cases.size} ~= rawValue else { return nil }
                    self = $name.allCases[Int(rawValue)]
                """.trimIndent()
            )
        )
    }.also { it.parent = this }

    private fun unsafeMutableRawPointerFlexibleType(): SirNominalType =
        SirNominalType(SirSwiftModule.unsafeMutableRawPointer).implicitlyUnwrappedOptional()
}

internal fun createSirEnumCaseFromKtSymbol(
    ktSymbol: KaEnumEntrySymbol,
    sirSession: SirSession,
): SirEnumCase = SirEnumCaseFromKtSymbol(
    ktSymbol,
    sirSession
)

private class SirEnumCaseFromKtSymbol(
    override val ktSymbol: KaEnumEntrySymbol,
    override val sirSession: SirSession,
) : SirEnumCase(), SirFromKtSymbol<KaEnumEntrySymbol> {
    override val name: String = ktSymbol.name.asString()
    override val origin: SirOrigin = KotlinSource(ktSymbol)

    override val visibility: SirVisibility
        get() = SirVisibility.PUBLIC
    override val documentation: String?
        get() = (parent as SirEnum).documentation
    override var parent: SirDeclarationParent = sirSession.withSessions { ktSymbol.getSirParent() as SirEnum }
        set(arg) {
            if (arg === field) return
            error("Changing SirEnumCase.parent is prohibited")
        }
    override val attributes: List<SirAttribute>
        get() = emptyList()

    fun nativeCaseRepresentation(enumSymbol: KaNamedClassSymbol): String =
        "${enumSymbol.classId!!.underscoredRepresentation()}_$name()"

    private fun ClassId.underscoredRepresentation(): String = buildString {
        if (!packageFqName.isRoot) {
            appendUnderscoredRepresentation(packageFqName)
            append("_")
        }
        appendUnderscoredRepresentation(relativeClassName)
    }

    private fun StringBuilder.appendUnderscoredRepresentation(fqName: FqName) {
        if (fqName.isRoot) return
        val parent = fqName.parent()
        if (!parent.isRoot) {
            appendUnderscoredRepresentation(parent)
            append("_")
        }
        append(fqName.shortName().asString())
    }

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        val fqName = bridgeFqName ?: return@lazyWithSessions null
        val baseName = fqName.baseBridgeName
        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = emptyList(),
            returnType = SirType.any,
            kotlinFqName = fqName,
            selfParameter = null,
            contextParameters = emptyList(),
            extensionReceiverParameter = null,
            errorParameter = null,
            isAsync = false,
        )
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        bridgeProxy?.createSirBridges {
            buildCall("")
        }.orEmpty()
    }
}
