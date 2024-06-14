package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClassType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isCompanion
import org.jetbrains.kotlin.objcexport.extras.objCTypeExtras
import org.jetbrains.kotlin.objcexport.extras.originClassId
import org.jetbrains.kotlin.objcexport.extras.requiresForwardDeclaration

/**
 * If object class has companion object it needs to have property which returns this companion.
 * To check whether class has companion object see [needsCompanionProperty]
 */
context(KaSession, KtObjCExportSession)
internal fun KaClassOrObjectSymbol.buildCompanionProperty(): ObjCProperty {
    val companion = this.staticMemberScope.classifiers.toList()
        .firstOrNull { (it as? KaClassOrObjectSymbol)?.isCompanion == true }

    val typeName = (companion as KaClassOrObjectSymbol).getObjCClassOrProtocolName()
    val propertyName = ObjCPropertyNames.companionObjectPropertyName

    return ObjCProperty(
        name = propertyName,
        comment = null,
        origin = null,
        type = ObjCClassType(typeName.objCName, extras = objCTypeExtras {
            requiresForwardDeclaration = true
            originClassId = companion.classId
        }),
        propertyAttributes = listOf("class", "readonly"),
        getterName = propertyName,
        declarationAttributes = listOf(swiftNameAttribute(propertyName))
    )
}