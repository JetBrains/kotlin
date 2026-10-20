/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBacked
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBackedSerializer

@KeepGeneratedSerializer
@Serializable(with = XcprojSwiftPackageProductMemberSerializer::class)
internal data class XcprojSwiftPackageProductMember(
    @SerialName("product-name") val productName: String,
    @SerialName("build-phase") val buildFile: XcprojTargetBuildFile,
) : JsonElementBacked()

internal object XcprojSwiftPackageProductMemberSerializer :
    JsonElementBackedSerializer<XcprojSwiftPackageProductMember>(XcprojSwiftPackageProductMember.generatedSerializer())
