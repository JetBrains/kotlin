package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.*

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtClassOrObjectSymbol.getEnumMembers(): List<ObjCExportStub> {
    if (classKind != KtClassKind.ENUM_CLASS) return emptyList()
    /**
     * TODO: apparently clone, isEqual, hash and description should be added
     */
    return getEnumEntries() + listOf(getEnumValuesMethod(), getEnumEntriesProperty())
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassOrObjectSymbol.getEnumEntries(): List<ObjCProperty> {
    val staticMembers = this.getStaticDeclaredMemberScope().getCallableSymbols().toList()
    return staticMembers.filterIsInstance<KtEnumEntrySymbol>().map { entry ->

        val entryName = entry.getEnumEntryName(false)
        val swiftName = entry.getEnumEntryName(true)
        ObjCProperty(
            entryName,
            null,
            null,
            entry.returnType.mapToReferenceTypeIgnoringNullability(),
            listOf("class", "readonly"),
            declarationAttributes = listOf(swiftNameAttribute(swiftName))
        )
    }
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildEnumValuesMethod]
 */
context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassOrObjectSymbol.getEnumValuesMethod(): ObjCMethod {
    return ObjCMethod(
        comment = null,
        isInstanceMethod = false,
        returnType = ObjCIdType, //TODO: add proper type
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
    return ObjCProperty(
        name = "entries",
        comment = null,
        type = ObjCIdType, //TODO: add proper type
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