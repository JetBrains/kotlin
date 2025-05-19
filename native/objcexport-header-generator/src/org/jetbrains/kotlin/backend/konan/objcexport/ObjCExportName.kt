/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi

sealed interface ObjCExportName {
    val swiftName: String?
    val objCName: String
}

interface ObjCExportClassOrProtocolName : ObjCExportName {
    val binaryName: String
}

interface ObjCExportPropertyName : ObjCExportName

interface ObjCExportFunctionName : ObjCExportName

interface ObjCExportFileName : ObjCExportName

fun ObjCExportClassOrProtocolName(
    swiftName: String?,
    objCName: String,
    binaryName: String = objCName,
): ObjCExportClassOrProtocolName = ObjCExportClassOrProtocolNameImpl(
    swiftName = swiftName,
    objCName = objCName,
    binaryName = binaryName
)

private data class ObjCExportClassOrProtocolNameImpl(
    override val swiftName: String?,
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

fun ObjCExportFunctionName(
    swiftName: String,
    objCName: String,
): ObjCExportFunctionName = ObjCExportFunctionNameImpl(
    swiftName = swiftName,
    objCName = objCName
)

fun ObjCExportFileName(
    swiftName: String,
    objCName: String,
): ObjCExportFileName = ObjCExportFileNameImpl(
    swiftName = swiftName,
    objCName = objCName
)

private data class ObjCExportPropertyNameImpl(
    override val swiftName: String,
    override val objCName: String,
) : ObjCExportPropertyName

private data class ObjCExportFunctionNameImpl(
    override val swiftName: String,
    override val objCName: String,
) : ObjCExportFunctionName

private data class ObjCExportFileNameImpl(
    override val swiftName: String,
    override val objCName: String,
) : ObjCExportFileName


fun ObjCExportClassOrProtocolName.toNameAttributes(): List<String> = listOfNotNull(
    binaryName.takeIf { it != objCName }?.let { objcRuntimeNameAttribute(it) },
    swiftName.takeIf { it != objCName }?.let { swiftNameAttribute(it) }
)

fun ObjCExportFileName.toNameAttributes(): List<String> = listOfNotNull(
    swiftName.takeIf { it != objCName }?.let { swiftNameAttribute(it) }
)

@InternalKotlinNativeApi
fun swiftNameAttribute(swiftName: String) = "swift_name(\"$swiftName\")"

@InternalKotlinNativeApi
fun objcRuntimeNameAttribute(name: String) = "objc_runtime_name(\"$name\")"

fun ObjCExportName.name(forSwift: Boolean) = swiftName.takeIf { forSwift } ?: objCName

@InternalKotlinNativeApi
fun String.toIdentifier(): String = this.toValidObjCSwiftIdentifier()

internal fun String.toValidObjCSwiftIdentifier(): String {
    if (this.isEmpty()) return "__"

    return this.replace('$', '_') // TODO: handle more special characters.
        .let { if (it.first().isDigit()) "_$it" else it }
        .let { if (it == "_") "__" else it }
}