/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.objcexport.NSNumberKind
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClassType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId

/**
 * Will translate [this] type to the corresponding ObjC equivalent.
 * e.g. `kotlin.String` -> `NSString`
 * e.g. `kotlin.collections.List` -> `NSArray`
 *
 * This function will also look through supertypes (e.g., custom implementations of List will still be mapped to NSArray).
 * Returns `null` if the type is not mapped to any ObjC equivalent
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtType.translateToMappedObjCTypeOrNull(): ObjCClassType? {
    return listOf(this).plus(this.getAllSuperTypes()).firstNotNullOfOrNull find@{ type ->
        val classId = type.expandedClassSymbol?.classIdIfNonLocal ?: return@find null
        mappedObjCTypeNames[classId]?.let { mappedTypeName ->
            return@find ObjCClassType(mappedTypeName, type.translateTypeArgumentsToObjC())
        }
    }
}


context(KtAnalysisSession, KtObjCExportSession)
private val mappedObjCTypeNames: Map<ClassId, String>
    get() = cached("mappedObjCTypeNames") {
        buildMap {
            this[ClassId.topLevel(StandardNames.FqNames.list)] = "NSArray"
            this[ClassId.topLevel(StandardNames.FqNames.mutableList)] = "NSMutableArray"
            this[ClassId.topLevel(StandardNames.FqNames.set)] = "NSSet"
            this[ClassId.topLevel(StandardNames.FqNames.mutableSet)] = "MutableSet".getObjCKotlinStdlibClassOrProtocolName().objCName
            this[ClassId.topLevel(StandardNames.FqNames.map)] = "NSDictionary"
            this[ClassId.topLevel(StandardNames.FqNames.mutableMap)] = "MutableDictionary".getObjCKotlinStdlibClassOrProtocolName().objCName
            this[ClassId.topLevel(StandardNames.FqNames.string.toSafe())] = "NSString"


            NSNumberKind.entries.forEach { number ->
                val numberClassId = number.mappedKotlinClassId
                if (numberClassId != null) {
                    this[numberClassId] = numberClassId.shortClassName.asString().getObjCKotlinStdlibClassOrProtocolName().objCName
                }
            }
        }
    }


