package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.Name

/**
 * Note: At the time of writing this function (and comment) we found it easiest
 * to construct the functions manually. Potentially, there is a way to get those functions from
 * the Analysis API by requesting the combined member scope and looking for [KtSymbolOrigin.SOURCE_MEMBER_GENERATED].
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtClassOrObjectSymbol.translateEnumMembers(): List<ObjCExportStub> {
    if (classKind != KtClassKind.ENUM_CLASS) return emptyList()
    return getEnumEntries() + listOf(getEnumValuesMethod(), getEnumEntriesProperty())
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassOrObjectSymbol.getEnumEntries(): List<ObjCProperty> {
    val staticMembers = this.getStaticDeclaredMemberScope().callables().toList()
    return staticMembers.filterIsInstance<KtEnumEntrySymbol>().map { entry ->

        val entryName = entry.getEnumEntryName(false)
        val swiftName = entry.getEnumEntryName(true)
        ObjCProperty(
            name = entryName,
            comment = null,
            origin = null,
            type = entry.returnType.mapToReferenceTypeIgnoringNullability(),
            propertyAttributes = listOf("class", "readonly"),
            declarationAttributes = listOf(swiftNameAttribute(swiftName))
        )
    }
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildEnumValuesMethod]
 */
context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassOrObjectSymbol.getEnumValuesMethod(): ObjCMethod {
    val valuesFunctionSymbol = getStaticMemberScope().callables(Name.identifier("values")).firstOrNull()
    return ObjCMethod(
        comment = null,
        isInstanceMethod = false,
        returnType = valuesFunctionSymbol?.returnType?.translateToObjCReferenceType() ?: ObjCIdType,
        selectors = listOf("values"),
        parameters = emptyList(),
        attributes = listOf(swiftNameAttribute("values()")),
        origin = null
    )
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildEnumEntriesProperty]
 */
context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassOrObjectSymbol.getEnumEntriesProperty(): ObjCProperty {
    val entriesSymbol = getStaticMemberScope().callables(Name.identifier("entries")).firstOrNull()

    return ObjCProperty(
        name = "entries",
        comment = null,
        type = entriesSymbol?.returnType?.translateToObjCReferenceType() ?: ObjCIdType,
        propertyAttributes = listOf("class", "readonly"),
        declarationAttributes = listOf(swiftNameAttribute("entries")),
        origin = null,
        setterName = null,
        getterName = null
    )
}

context(KtAnalysisSession)
private fun KtEnumEntrySymbol.getEnumEntryName(forSwift: Boolean): String {

    val propertyName: String = getObjCPropertyName().run {
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