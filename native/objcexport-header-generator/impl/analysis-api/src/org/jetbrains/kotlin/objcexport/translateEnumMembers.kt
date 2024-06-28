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

    val staticMembers = with(kaSession) { symbol.staticDeclaredMemberScope }.callables.toList()
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
    val valuesFunctionSymbol = with(kaSession) { symbol.staticMemberScope }.callables(Name.identifier("values")).firstOrNull()
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
    val entriesSymbol = with(kaSession) { symbol.staticMemberScope }.callables(Name.identifier("entries")).firstOrNull()

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

private fun ObjCExportContext.getEnumEntryName(symbol: KaEnumEntrySymbol, forSwift: Boolean): String {

    val propertyName: String = getObjCPropertyName(symbol).run {
        when {
            forSwift -> this.swiftName
            else -> this.objCName
        }
    }

    // FOO_BAR_BAZ -> fooBarBaz:
    val name = propertyName.split('_').mapIndexed { index, s ->
        val lower = s.lowercase()
        if (index == 0) lower else lower.replaceFirstChar(Char::uppercaseChar)
    }.joinToString("").toIdentifier()
    return name
}
