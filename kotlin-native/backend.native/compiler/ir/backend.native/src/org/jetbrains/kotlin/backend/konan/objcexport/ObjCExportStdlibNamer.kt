/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

interface ObjCExportStdlibNamer {

    val stdlibTopLevelPrefix: String

    fun numberBoxName(classId: ClassId): ObjCExportNamer.ClassOrProtocolName

    val kotlinAnyName: ObjCExportNamer.ClassOrProtocolName
    val mutableSetName: ObjCExportNamer.ClassOrProtocolName
    val mutableMapName: ObjCExportNamer.ClassOrProtocolName
    val kotlinNumberName: ObjCExportNamer.ClassOrProtocolName

    companion object {
        fun create(stdlibTopLevelPrefix: String): ObjCExportStdlibNamer =
                ObjCExportStdlibNamerImpl(stdlibTopLevelPrefix)
    }
}

private class ObjCExportStdlibNamerImpl(override val stdlibTopLevelPrefix: String) : ObjCExportStdlibNamer {

    private fun String.toSpecialStandardClassOrProtocolName() = ObjCExportNamer.ClassOrProtocolName(
            swiftName = "Kotlin$this",
            objCName = "${stdlibTopLevelPrefix}$this"
    )

    override val kotlinAnyName = "Base".toSpecialStandardClassOrProtocolName()

    override val mutableSetName = "MutableSet".toSpecialStandardClassOrProtocolName()
    override val mutableMapName = "MutableDictionary".toSpecialStandardClassOrProtocolName()

    override fun numberBoxName(classId: ClassId): ObjCExportNamer.ClassOrProtocolName =
            classId.shortClassName.asString().toSpecialStandardClassOrProtocolName()

    override val kotlinNumberName = "Number".toSpecialStandardClassOrProtocolName()
}