package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.Name

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtClassOrObjectSymbol.translateEnumMembers(): List<ObjCExportStub> {
    if (classKind != KtClassKind.ENUM_CLASS) return emptyList()
    return getEnumEntries() + listOf(getEnumValuesMethod(), getEnumEntriesProperty())
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassOrObjectSymbol.getEnumEntries(): List<ObjCProperty> {
    val staticMembers = this.getStaticDeclaredMemberScope().getCallableSymbols().toList()
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
    val valuesFunctionSymbol = getStaticMemberScope().getCallableSymbols(Name.identifier("values")).firstOrNull()
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
    val entriesSymbol = getStaticMemberScope().getCallableSymbols(Name.identifier("entries")).firstOrNull()

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