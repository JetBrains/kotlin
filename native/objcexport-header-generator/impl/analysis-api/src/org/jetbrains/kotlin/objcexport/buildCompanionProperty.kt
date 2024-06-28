/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
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
internal fun ObjCExportContext.buildCompanionProperty(classSymbol: KaClassSymbol): ObjCProperty {
    val companion = with(kaSession) {
        classSymbol.staticMemberScope
    }.classifiers.toList().firstOrNull { (it as? KaClassSymbol)?.isCompanion == true }

    val typeName = getObjCClassOrProtocolName(companion as KaClassSymbol)
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
