/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.allOverriddenSymbols
import org.jetbrains.kotlin.analysis.api.symbols.containingSymbol
import org.jetbrains.kotlin.name.render
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.sir.util.swiftIdentifier
import org.jetbrains.kotlin.sir.util.swiftName
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal fun SirClassMemberDeclaration.needsReverseBridge(): Boolean {
    if (!isInstance) return false
    if (isUnavailable) return false
    return when (val containingDecl = parent) {
        is SirClass -> modality == SirModality.OPEN && containingDecl.modality == SirModality.OPEN && !containingDecl.isUnavailable
        is SirProtocol -> !containingDecl.isUnavailable
        else -> false
    }
}

internal fun SirClassMemberDeclaration.needsNonVirtualForwardBridge(): Boolean = needsReverseBridge() && parent is SirClass

internal fun SirClassMemberDeclaration.effectiveReverseBridgeDeprecation(): SirAttribute.Available? {
    fun SirDeclaration.deprecatedAttr(): SirAttribute.Available? =
        attributes.firstOrNull { it is SirAttribute.Available && it.deprecated } as? SirAttribute.Available
    return this.deprecatedAttr()
        ?: (parent as? SirClass)?.deprecatedAttr()
        ?: (parent as? SirProtocol)?.deprecatedAttr()
}

internal fun SirFunction.reverseBridgeSwiftCall(selfExpr: String, paramExprs: List<String>): String {
    val params = listOfNotNull(contextParameter, extensionReceiverParameter) + parameters
    check(paramExprs.size == params.size) { "Parameter expression count doesn't match parameter count" }
    val methodName = name.swiftIdentifier
    val tryPrefix = when {
        isAsync -> "try await "
        errorType != SirType.never -> "try "
        else -> ""
    }
    return if (params.any { it.isVariadic }) {
        val destType = SirFunctionalType(
            parameterTypes = params.map { if (it.isVariadic) SirArrayType(it.type) else it.type },
            isAsync = isAsync,
            returnType = returnType,
            errorType = errorType,
        ).swiftName
        val labels = params.joinToString("") { param ->
            "${param.argumentName?.takeIf { it.isNotEmpty() }?.swiftIdentifier ?: "_"}:"
        }
        "${tryPrefix}unsafeBitCast($selfExpr.$methodName($labels), to: ($destType).self)(${paramExprs.joinToString(", ")})"
    } else {
        val args = params
            .zip(paramExprs)
            .joinToString(", ") { [param, expr] ->
                param.argumentName?.takeIf { it.isNotEmpty() }?.let { "${it.swiftIdentifier}: $expr" } ?: expr
            }
        "$tryPrefix$selfExpr.$methodName($args)"
    }
}

context(_: SirSession)
internal fun SirClassMemberDeclaration.forwardBridgedBody(
    proxy: BridgeFunctionProxy,
    isAbstractKotlinMember: Boolean,
    memberKind: String,
    memberName: String,
): SirFunctionBody {
    if (!needsNonVirtualForwardBridge()) {
        return SirFunctionBody(proxy.createSwiftInvocation { "return $it" })
    }
    val wrapperFqName = (parent as SirClass).swiftFqName
    val virtualLines = proxy.createSwiftInvocation { "return $it" }
    val fallbackLines = if (isAbstractKotlinMember) {
        listOf("fatalError(\"Cannot invoke the inherited implementation of abstract $memberKind '$wrapperFqName.$memberName': a Swift subclass must override it and must not call super.\")")
    } else {
        proxy.createSwiftInvocation(useDirectDispatch = true) { "return $it" }
    }
    return SirFunctionBody(buildList {
        add("if Self.self == $wrapperFqName.self {")
        virtualLines.forEach { add("    $it") }
        add("} else {")
        fallbackLines.forEach { add("    $it") }
        add("}")
    })
}

/** The fully qualified name of the Kotlin class declaring [this] member, the target of its reverse bridge. */
context(_: KaSession)
internal val KaCallableSymbol.reverseBridgeTargetClassFqName: String
    get() = (containingSymbol as? KaNamedClassSymbol)?.classId?.asSingleFqName()?.render() ?: ""

context(_: KaSession, sirSession: SirSession)
internal fun SirDeclaration.interfaceSelfType(ktSymbol: KaCallableSymbol): SirType? {
    (parent as? SirProtocol)?.let { return SirExistentialType(it) }

    val containingClass = (parent as? SirClass)?.kaSymbolOrNull<KaClassSymbol>() ?: return null
    if (!containingClass.hasFBoundedTypeParameters()) return null

    val overriddenInterfaceMember = ktSymbol.allOverriddenSymbols.firstOrNull { overridden ->
        val containingSymbol = overridden.containingSymbol
        containingSymbol is KaClassSymbol && containingSymbol.classKind == KaClassKind.INTERFACE
    } ?: return null

    val interfaceSymbol = overriddenInterfaceMember.containingSymbol as? KaNamedClassSymbol ?: return null

    val sirProtocol = with(sirSession) { interfaceSymbol.toSir().allDeclarations }.firstIsInstanceOrNull<SirProtocol>()
        ?: return null

    return SirExistentialType(sirProtocol)
}
