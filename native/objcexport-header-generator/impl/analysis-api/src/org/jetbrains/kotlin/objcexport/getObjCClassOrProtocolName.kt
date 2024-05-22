/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportClassOrProtocolName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

/**
 * Constructs the name of the given [KtClassLikeSymbol] by composing
 * ```
 * {Current Framework Name}{Additional Module Prefix}{ObjC Class or Protocol Name}
 *         (1)                      (2)                         (3)
 * ```
 *
 * 1) Current Framework Name: See [KtObjCExportConfiguration.frameworkName]
 * 2) Additional Module Prefix: Dependency modules that are not directly exported will add the prefix to distinguish from which
 * dependency module a symbol came from. Note: This module name can be abbreviated e.g. `LongModuleName` will be `LMN`
 * 3) ObjC Class or Protocol Name: This is either the name of the symbol, or the defined name in the `ObjCName` annotation
 * (see [resolveObjCNameAnnotation]): e.g.
 * ```
 * @ObjCName("ObjCFoo")
 * class Foo
 * ```
 * will use `ObjCFoo` instead of the class name `Foo`
 *
 * @param bareName if `true`, the symbol name will not be prefixed with module/framework/parent name
 *
 */
context(KtAnalysisSession, KtObjCExportSession)
fun KtClassLikeSymbol.getObjCClassOrProtocolName(bareName: Boolean = false): ObjCExportClassOrProtocolName {
    val resolvedObjCNameAnnotation = resolveObjCNameAnnotation()

    return ObjCExportClassOrProtocolName(
        objCName = getObjCName(resolvedObjCNameAnnotation, bareName),
        swiftName = getSwiftName(resolvedObjCNameAnnotation, bareName)
    )
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassLikeSymbol.getObjCName(
    resolvedObjCNameAnnotation: KtResolvedObjCNameAnnotation? = resolveObjCNameAnnotation(),
    bareName: Boolean = false,
): String {
    val objCName = (resolvedObjCNameAnnotation?.objCName ?: nameOrAnonymous.asString()).toValidObjCSwiftIdentifier()

    if (bareName || resolvedObjCNameAnnotation != null && resolvedObjCNameAnnotation.isExact) {
        return objCName
    }

    getContainingSymbol()?.let { it as? KtClassLikeSymbol }?.let { containingClass ->
        return containingClass.getObjCName() + objCName.capitalizeAsciiOnly()
    }

    return buildString {
        configuration.frameworkName?.let(::append)
        getObjCModuleNamePrefix()?.let(::append)
        append(objCName)
    }
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassLikeSymbol.getSwiftName(
    resolvedObjCNameAnnotation: KtResolvedObjCNameAnnotation? = resolveObjCNameAnnotation(),
    bareName: Boolean = false,
): String {
    val swiftName = (resolvedObjCNameAnnotation?.swiftName ?: nameOrAnonymous.asString()).toValidObjCSwiftIdentifier()
    if (bareName || resolvedObjCNameAnnotation != null && resolvedObjCNameAnnotation.isExact) {
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

    return buildString {
        getObjCModuleNamePrefix()?.let(::append)
        append(swiftName)
    }
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

context(KtAnalysisSession, KtObjCExportSession)
private fun KtSymbol.getObjCModuleNamePrefix(): String? {
    val module = getContainingModule()
    val moduleName = module.getObjCKotlinModuleName() ?: return null
    if (moduleName == "stdlib" || moduleName == "kotlin-stdlib-common") return "Kotlin"
    if (isExported(module)) return null
    return abbreviateModuleName(moduleName)
}

/**
 * 'MyModuleName' -> 'MMN'
 * 'someLibraryFoo' -> 'SLF'
 */
internal fun abbreviateModuleName(name: String): String {
    val normalizedName = name
        .capitalizeAsciiOnly()
        .replace("[-.]".toRegex(), "_")

    val uppers = normalizedName.filter { character -> character.isUpperCase() }
    if (uppers.length >= 3) return uppers

    return normalizedName
}