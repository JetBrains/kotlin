/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportClassOrProtocolName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

context(KtAnalysisSession, KtObjCExportSession)
fun KtClassLikeSymbol.getObjCClassOrProtocolName(): ObjCExportClassOrProtocolName {
    val resolvedObjCNameAnnotation = resolveObjCNameAnnotation()

    return ObjCExportClassOrProtocolName(
        objCName = getObjCName(resolvedObjCNameAnnotation),
        swiftName = getSwiftName(resolvedObjCNameAnnotation)
    )
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassLikeSymbol.getObjCName(
    resolvedObjCNameAnnotation: KtResolvedObjCNameAnnotation? = resolveObjCNameAnnotation(),
): String {
    val objCName = (resolvedObjCNameAnnotation?.objCName ?: nameOrAnonymous.asString()).toValidObjCSwiftIdentifier()

    if (resolvedObjCNameAnnotation != null && resolvedObjCNameAnnotation.isExact) {
        return objCName
    }

    getContainingSymbol()?.let { it as? KtClassLikeSymbol }?.let { containingClass ->
        return containingClass.getObjCName() + objCName.capitalizeAsciiOnly()
    }

    // KT-65670: Append module specific prefixes?
    return configuration.frameworkName.orEmpty() + objCName
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassLikeSymbol.getSwiftName(
    resolvedObjCNameAnnotation: KtResolvedObjCNameAnnotation? = resolveObjCNameAnnotation(),
): String {
    val swiftName = (resolvedObjCNameAnnotation?.swiftName ?: nameOrAnonymous.asString()).toValidObjCSwiftIdentifier()
    if (resolvedObjCNameAnnotation != null && resolvedObjCNameAnnotation.isExact) {
        return swiftName
    }

    getContainingSymbol()?.let { it as? KtClassLikeSymbol }?.let { containingClass ->
        val containingClassSwiftName = containingClass.getSwiftName()
        return buildString {
            if (canBeInnerSwift()) {
                append(containingClassSwiftName)
                if ("." !in this && containingClass.canBeOuterSwift()) {
                    // AB -> AB.C
                    append(".")
                    append(mangleSwiftNestedClassName(swiftName))
                } else {
                    // AB -> ABC
                    // A.B -> A.BC
                    append(swiftName.capitalizeAsciiOnly())
                }
            } else {
                append(containingClassSwiftName.replaceFirst(".", ""))
                append(swiftName.capitalizeAsciiOnly())
            }
        }
    }

    // KT-65670: Append module specific prefixes?
    return swiftName

}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassLikeSymbol.canBeInnerSwift(): Boolean {
    if (configuration.objcGenerics && this.typeParameters.isNotEmpty()) {
        // Swift compiler doesn't seem to handle this case properly.
        // See https://bugs.swift.org/browse/SR-14607.
        // This behaviour of Kotlin is reported as https://youtrack.jetbrains.com/issue/KT-46518.
        return false
    }

    if (this is KtClassOrObjectSymbol && this.classKind == KtClassKind.INTERFACE) {
        // Swift doesn't support nested protocols.
        return false
    }

    return true
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassLikeSymbol.canBeOuterSwift(): Boolean {
    if (configuration.objcGenerics && this.typeParameters.isNotEmpty()) {
        // Swift nested classes are static but capture outer's generics.
        return false
    }

    if (this is KtClassOrObjectSymbol && this.classKind == KtClassKind.INTERFACE) {
        // Swift doesn't support outer protocols.
        return false
    }

    return true
}

private fun mangleSwiftNestedClassName(name: String): String = when (name) {
    "Type" -> "${name}_" // See https://github.com/JetBrains/kotlin-native/issues/3167
    else -> name
}