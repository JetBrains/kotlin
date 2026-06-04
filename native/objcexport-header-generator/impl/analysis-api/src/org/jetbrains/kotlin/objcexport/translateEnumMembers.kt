/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.Name

/**
 * Note: At the time of writing this function (and comment) we found it easiest
 * to construct the functions manually. Potentially, there is a way to get those functions from
 * the Analysis API by requesting the combined member scope and looking for [KtSymbolOrigin.SOURCE_MEMBER_GENERATED].
 */
internal fun ObjCExportContext.translateEnumMembers(symbol: KaClassSymbol): List<ObjCExportStub> {
    if (symbol.classKind != KaClassKind.ENUM_CLASS) return emptyList()
    return getEnumEntries(symbol) + listOf(getEnumValuesMethod(symbol), getEnumEntriesProperty(symbol))
}

private fun ObjCExportContext.getEnumEntries(symbol: KaClassSymbol): List<ObjCProperty> {

    val staticMembers = with(analysisSession) { symbol.staticDeclaredMemberScope }.callables.toList()
    return staticMembers.filterIsInstance<KaEnumEntrySymbol>().map { entry ->

        val entryName = getEnumEntryName(entry, false)
        val swiftName = getEnumEntryName(entry, true)
        ObjCProperty(
            name = entryName,
            comment = null,
            origin = null,
            type = mapToReferenceTypeIgnoringNullability(entry.returnType),
            propertyAttributes = listOf("class", "readonly"),
            declarationAttributes = listOf(swiftNameAttribute(swiftName))
        )
    }
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildEnumValuesMethod]
 */
private fun ObjCExportContext.getEnumValuesMethod(symbol: KaClassSymbol): ObjCMethod {
    val valuesFunctionSymbol = with(analysisSession) { symbol.staticMemberScope }.callables(Name.identifier("values")).firstOrNull()
    val returnType = valuesFunctionSymbol?.returnType
    return ObjCMethod(
        comment = null,
        isInstanceMethod = false,
        returnType = if (returnType == null) ObjCIdType else translateToObjCReferenceType(returnType),
        selectors = listOf("values"),
        parameters = emptyList(),
        attributes = listOf(swiftNameAttribute("values()")),
        origin = null
    )
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildEnumEntriesProperty]
 */
private fun ObjCExportContext.getEnumEntriesProperty(symbol: KaClassSymbol): ObjCProperty {
    val entriesSymbol = with(analysisSession) { symbol.staticMemberScope }.callables(Name.identifier("entries")).firstOrNull()

    val returnType = entriesSymbol?.returnType
    return ObjCProperty(
        name = "entries",
        comment = null,
        type = if (returnType == null) ObjCIdType else translateToObjCReferenceType(returnType),
        propertyAttributes = listOf("class", "readonly"),
        declarationAttributes = listOf(swiftNameAttribute("entries")),
        origin = null,
        setterName = null,
        getterName = null
    )
}

internal fun ObjCExportContext.getNSEnumEntryName(symbol: KaEnumEntrySymbol, forSwift: Boolean): String {
    val objCEnumEntryNameAnnotation = symbol.resolveObjCEnumEntryNameAnnotation()
    val name = (if (forSwift) objCEnumEntryNameAnnotation?.swiftName?.ifEmpty { null } else null) ?: objCEnumEntryNameAnnotation?.objCName
    return name?.ifEmpty { null } ?: getEnumEntryName(symbol, forSwift)
}

/**
 * See K1 implementation as [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getEnumEntryName]
 */
internal fun ObjCExportContext.getEnumEntryName(symbol: KaEnumEntrySymbol, forSwift: Boolean): String {
    val name = getObjCPropertyName(symbol) {
        it.split('_').mapIndexed { index, s ->
            // This is the transformation block that'd run if the @ObjCName annotation was not present.
            // If present, we'd use the user provided name as-is.
            //
            // FOO_BAR_BAZ -> fooBarBaz:
            val lower = s.lowercase()
            if (index == 0) lower else lower.replaceFirstChar(Char::uppercaseChar)
        }.joinToString("").toIdentifier()
    }.run {
        when {
            // In case no @ObjCName annotation was present, both swiftName and objCName point to the resultant
            // string from the transformation run above.
            forSwift -> this.swiftName
            else -> this.objCName
        }
    }

    return if (name in objCSpecialNames) name.handleSpecialNames("the")
    else if (name.isReservedPropertyName) name + "_"
    else name
}
