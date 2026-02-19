/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.NSNumberKind
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClassType
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.ClassId.Companion.topLevel
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Will translate [this] type to the corresponding ObjC equivalent.
 * e.g. `kotlin.String` -> `NSString`
 * e.g. `kotlin.collections.List` -> `NSArray`
 *
 * This function will also look through supertypes (e.g., custom implementations of List will still be mapped to NSArray).
 * Returns `null` if the type is not mapped to any ObjC equivalent
 */
internal fun ObjCExportContext.translateToMappedObjCTypeOrNull(type: KaType): ObjCClassType? {
    val allSuperTypes = with(analysisSession) { type.allSupertypes }
    return listOf(type).plus(allSuperTypes).firstNotNullOfOrNull find@{ type ->
        val classId = with(analysisSession) { type.expandedSymbol }?.classId ?: return@find null
        mappedObjCTypeNames[classId]?.let { mappedTypeName ->
            return@find ObjCClassType(mappedTypeName, translateTypeArgumentsToObjC(type))
        }
    }
}

private val mappedObjCTypes = buildSet {
    add(topLevel(FqNames.list))
    add(topLevel(FqNames.mutableList))
    add(topLevel(FqNames.set))
    add(topLevel(FqNames.mutableSet))
    add(topLevel(FqNames.map))
    add(topLevel(FqNames.mutableMap))
    add(topLevel(FqNames.string.toSafe()))

    NSNumberKind.entries.forEach { addIfNotNull(it.mappedKotlinClassId) }
}

internal val ObjCExportContext.mappedObjCTypeNames: Map<ClassId, String>
    get() = exportSession.cached("mappedObjCTypeNames") {
        buildMap {
            mappedObjCTypes.forEach { type ->
                when (type) {
                    topLevel(FqNames.list) -> this[type] = "NSArray"
                    topLevel(FqNames.mutableList) -> this[type] = "NSMutableArray"
                    topLevel(FqNames.set) -> this[type] = "NSSet"
                    topLevel(FqNames.mutableSet) -> this[type] = exportSession.getObjCKotlinStdlibClassOrProtocolName("MutableSet").objCName
                    topLevel(FqNames.map) -> this[type] = "NSDictionary"
                    topLevel(FqNames.mutableMap) -> this[type] =
                        exportSession.getObjCKotlinStdlibClassOrProtocolName("MutableDictionary").objCName
                    topLevel(FqNames.string.toSafe()) -> this[type] = "NSString"
                    else -> {
                        NSNumberKind.entries.firstNotNullOf { numberClassId ->
                            numberClassId.mappedKotlinClassId
                        }.let { clazzId ->
                            this[type] = exportSession.getObjCKotlinStdlibClassOrProtocolName(clazzId.shortClassName.asString()).objCName
                        }
                    }
                }
            }

            NSNumberKind.entries.forEach { number ->
                val numberClassId = number.mappedKotlinClassId
                if (numberClassId != null) {
                    this[numberClassId] =
                        exportSession.getObjCKotlinStdlibClassOrProtocolName(numberClassId.shortClassName.asString()).objCName
                }
            }
        }
    }

internal fun KaSession.isMappedObjCType(type: KaType?): Boolean {
    if (type == null) return false
    return mappedObjCTypes.contains(type.expandedSymbol?.classId)
}