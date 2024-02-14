package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClassType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute
import org.jetbrains.kotlin.objcexport.KtObjCExportSession
import org.jetbrains.kotlin.objcexport.ObjCPropertyNames
import org.jetbrains.kotlin.objcexport.getObjCClassOrProtocolName

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslator.needCompanionObjectProperty]
 */
context(KtAnalysisSession)
internal val KtClassOrObjectSymbol.needsCompanionProperty: Boolean
    get() {
        return this.getStaticMemberScope().getClassifierSymbols()
            .any { (it as? KtClassOrObjectSymbol)?.isCompanion == true }
    }

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtClassOrObjectSymbol.buildCompanionProperty(): ObjCProperty {

    val companion = this.getStaticMemberScope().getClassifierSymbols().toList()
        .firstOrNull { (it as? KtClassOrObjectSymbol)?.isCompanion == true }
    val typeName = (companion as KtClassOrObjectSymbol).getObjCClassOrProtocolName()
    val propertyName = ObjCPropertyNames.companionObjectPropertyName

    return ObjCProperty(
        propertyName,
        null,
        null,
        ObjCClassType(typeName.objCName, classId = companion.classIdIfNonLocal),
        listOf("class", "readonly"),
        getterName = propertyName,
        declarationAttributes = listOf(swiftNameAttribute(propertyName))
    )
}

internal val KtClassOrObjectSymbol.isCompanion: Boolean
    get() = classKind == KtClassKind.COMPANION_OBJECT