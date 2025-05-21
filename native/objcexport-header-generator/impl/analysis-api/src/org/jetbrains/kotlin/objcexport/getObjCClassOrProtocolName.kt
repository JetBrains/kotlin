/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
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
fun ObjCExportContext.getObjCClassOrProtocolName(
    classSymbol: KaClassLikeSymbol,
    bareName: Boolean = false,
): ObjCExportClassOrProtocolName {
    val resolvedObjCNameAnnotation = classSymbol.resolveObjCNameAnnotation()

    return ObjCExportClassOrProtocolName(
        objCName = getObjCName(classSymbol, resolvedObjCNameAnnotation, bareName),
        swiftName = getSwiftName(classSymbol, resolvedObjCNameAnnotation, bareName)
    )
}

private fun ObjCExportContext.getObjCName(
    symbol: KaClassLikeSymbol,
    resolvedObjCNameAnnotation: KtResolvedObjCNameAnnotation? = symbol.resolveObjCNameAnnotation(),
    bareName: Boolean = false,
): String {
    val objCName =
        exportSession.exportSessionSymbolNameOrObjCName(symbol, resolvedObjCNameAnnotation?.objCName).toValidObjCSwiftIdentifier()

    if (bareName || resolvedObjCNameAnnotation != null && resolvedObjCNameAnnotation.isExact) {
        return objCName
            .handleSpecialNames("get")
    }

    with(analysisSession) {
        symbol.containingDeclaration?.let { it as? KaClassLikeSymbol }?.let { containingClass ->
            return getObjCName(containingClass) + objCName.capitalizeAsciiOnly()
        }
    }

    return buildString {
        exportSession.configuration.frameworkName?.let(::append)
        getObjCModuleNamePrefix(symbol)?.let(::append)
        append(objCName)
    }
}

/**
 * See K1 implementation at [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getClassOrProtocolSwiftName]
 */
private fun ObjCExportContext.getSwiftName(
    classSymbol: KaClassLikeSymbol,
    resolvedObjCNameAnnotation: KtResolvedObjCNameAnnotation? = classSymbol.resolveObjCNameAnnotation(),
    bareName: Boolean = false,
): String? {

    val swiftName = resolvedObjCNameAnnotation?.swiftName
        ?: exportSession.exportSessionSymbolNameOrObjCName(classSymbol, resolvedObjCNameAnnotation?.objCName).toValidObjCSwiftIdentifier()
    if (bareName || resolvedObjCNameAnnotation != null && resolvedObjCNameAnnotation.isExact) {
        return swiftName
    }

    with(analysisSession) {
        classSymbol.containingDeclaration?.let { it as? KaClassLikeSymbol }?.let { containingClass ->
            val containingClassSwiftName = getSwiftName(containingClass)
            return buildString {
                if (canBeInnerSwift(classSymbol)) {
                    append(containingClassSwiftName)
                    if ("." !in this && canBeOuterSwift(containingClass)) {
                        // AB -> AB.C
                        append(".")
                        append(mangleSwiftNestedClassName(swiftName))
                    } else {
                        // AB -> ABC
                        // A.B -> A.BC
                        append(swiftName.capitalizeAsciiOnly())
                    }
                } else {
                    append(containingClassSwiftName?.replaceFirst(".", ""))
                    append(swiftName.capitalizeAsciiOnly())
                }
            }
        }
    }

    return buildString {
        getObjCModuleNamePrefix(classSymbol)?.let(::append)
        append(swiftName)
    }
}

private fun ObjCExportContext.canBeInnerSwift(symbol: KaClassLikeSymbol): Boolean {
    @OptIn(KaExperimentalApi::class)
    if (exportSession.configuration.objcGenerics && symbol.typeParameters.isNotEmpty()) {
        // Swift compiler doesn't seem to handle this case properly.
        // See https://bugs.swift.org/browse/SR-14607.
        // This behaviour of Kotlin is reported as https://youtrack.jetbrains.com/issue/KT-46518.
        return false
    }

    if (symbol is KaClassSymbol && symbol.classKind == KaClassKind.INTERFACE) {
        // Swift doesn't support nested protocols.
        return false
    }

    return true
}

private fun ObjCExportContext.canBeOuterSwift(symbol: KaClassLikeSymbol): Boolean {
    @OptIn(KaExperimentalApi::class)
    if (exportSession.configuration.objcGenerics && symbol.typeParameters.isNotEmpty()) {
        // Swift nested classes are static but capture outer's generics.
        return false
    }

    if (symbol is KaClassSymbol && symbol.classKind == KaClassKind.INTERFACE) {
        // Swift doesn't support outer protocols.
        return false
    }

    return true
}

private fun mangleSwiftNestedClassName(name: String): String = when (name) {
    "Type" -> "${name}_" // See https://github.com/JetBrains/kotlin-native/issues/3167
    else -> name
}

private fun ObjCExportContext.getObjCModuleNamePrefix(symbol: KaSymbol): String? {
    val module = with(analysisSession) { symbol.containingModule }
    val moduleName = getObjCKotlinModuleName(module) ?: return null
    val isExported = with(exportSession) { isExported(module) }
    if (moduleName == "stdlib" || moduleName == "kotlin-stdlib-common") return "Kotlin"
    if (isExported) return null
    return normalizeAndAbbreviateModuleName(moduleName)
}

/**
 * Replaces chars which can't be used in ObjC/Swift identifiers with '_'
 * And abbreviates the name if it's too long and contains too many capitals
 *
 * 'MyModuleName' -> 'MMN'
 * 'someLibraryFoo' -> 'SLF'
 */
internal fun normalizeAndAbbreviateModuleName(name: String): String {
    val normalizedName = name
        .capitalizeAsciiOnly()
        .replace(invalidModuleNameChars, "_")

    val uppers = normalizedName.filter { character -> character.isUpperCase() }
    if (uppers.length >= 3) return uppers

    return normalizedName
}

private val invalidModuleNameChars = "[^a-zA-Z0-9]".toRegex()