/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

sealed interface ObjCExportName {
    val swiftName: String
    val objCName: String
}

interface ObjCExportClassOrProtocolName : ObjCExportName {
    val binaryName: String
}

interface ObjCExportPropertyName : ObjCExportName

fun ObjCExportClassOrProtocolName(
    swiftName: String,
    objCName: String,
    binaryName: String = objCName,
): ObjCExportClassOrProtocolName = ObjCExportClassOrProtocolNameImpl(
    swiftName = swiftName,
    objCName = objCName,
    binaryName = binaryName
)

private data class ObjCExportClassOrProtocolNameImpl(
    override val swiftName: String,
    override val objCName: String,
    override val binaryName: String,
) : ObjCExportClassOrProtocolName

fun ObjCExportPropertyName(
    swiftName: String,
    objCName: String,
): ObjCExportPropertyName = ObjCExportPropertyNameImpl(
    swiftName = swiftName,
    objCName = objCName
)

private data class ObjCExportPropertyNameImpl(
    override val swiftName: String,
    override val objCName: String,
) : ObjCExportPropertyName


fun ObjCExportClassOrProtocolName.toNameAttributes(): List<String> = listOfNotNull(
    binaryName.takeIf { it != objCName }?.let { objcRuntimeNameAttribute(it) },
    swiftName.takeIf { it != objCName }?.let { swiftNameAttribute(it) }
)

private fun swiftNameAttribute(swiftName: String) = "swift_name(\"$swiftName\")"
private fun objcRuntimeNameAttribute(name: String) = "objc_runtime_name(\"$name\")"