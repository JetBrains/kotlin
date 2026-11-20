/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.gradle.process.ExecOperations
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBacked
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBackedSerializer
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.StringOrStringList
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

@Serializable
internal class XcprojProject(
    val targets: List<XcprojTarget> = emptyList(),
    val packages: MutableList<XcprojPackageLocation> = mutableListOf(),
) : JsonElementBacked() {
    val nativeTargets: List<XcprojNativeTarget> get() = targets.filterIsInstance<XcprojNativeTarget>()
}

private val xcprojJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    explicitNulls = false
    allowComments = true
    allowTrailingComma = true
    prettyPrint = true
}

private val xcprojSerializer =
    JsonElementBackedSerializer(XcprojProject.serializer())

internal fun deserializeXcprojProject(file: File): XcprojProject =
    file.inputStream().use { xcprojJson.decodeFromStream(xcprojSerializer, it) }

internal fun deserializeXcprojProject(
    @Language("JSON5")
    content: String,
): XcprojProject = xcprojJson.decodeFromString(xcprojSerializer, content)

internal fun XcprojProject.serializeXcprojProject(output: OutputStream) = xcprojJson.encodeToStream(xcprojSerializer, this, output)

internal fun XcprojProject.writeXcprojProject(file: File, execOps: ExecOperations) {
    val serialized = ByteArrayOutputStream().apply { serializeXcprojProject(this) }
    val formatted = ByteArrayOutputStream()
    execOps.exec {
        it.commandLine("xcrun", "xcprojformatter")
        it.standardInput = ByteArrayInputStream(serialized.toByteArray())
        it.standardOutput = formatted
    }
    file.writeBytes(formatted.toByteArray())
}

internal fun File.xcodeProjectFile(): File = resolve("project.xcproj").takeIf { it.isFile }
    ?: resolve("project.pbxproj")

internal fun File.isXcproj(): Boolean = extension == "xcproj"

internal fun XcprojProject.hasLinkagePackageReference(): Boolean = nativeTargets.any { target ->
    target.packageProductMembers.any { it.hasLinkageProductName() }
}

private fun XcprojSwiftPackageProductMember.hasLinkageProductName(): Boolean =
    productName == GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME

internal class MissingTargetsForEmbedAndSignIntegration : Error("Couldn't find targets to insert embedAndSign integration")
internal class MissingForLinkagePackageIntegration : Error("Couldn't find targets with embedAndSign integration to integrate the SwiftPM package")

/** Returns false when the integration is already present */
internal fun XcprojProject.integrateEmbedAndSign(script: String): Boolean {
    if (nativeTargets.any { it.hasEmbedAndSignIntegration() }) return false
    if (nativeTargets.isEmpty()) throw MissingTargetsForEmbedAndSignIntegration()
    nativeTargets.forEach { target ->
        target.buildPhases.add(
            0,
            XcprojScriptBuildPhase(
                name = "Compile Kotlin Framework",
                shell = "/bin/sh",
                script = StringOrStringList.StringValue(script),
                runOnEveryBuild = true
            )
        )
        target.buildSettings["ENABLE_USER_SCRIPT_SANDBOXING"] =
            XcprojBuildSettingString("NO")
    }
    return true
}

internal fun XcprojProject.integrateLinkagePackage(): Boolean {
    if (hasLinkagePackageReference()) return false
    val integrationTargets = nativeTargets.filter { it.hasEmbedAndSignIntegration() }
    if (integrationTargets.isEmpty()) throw MissingForLinkagePackageIntegration()
    val productName = GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
    if (packages.none { (it as? XcprojPackageLocationLocal)?.path == productName }) {
        packages.add(
            XcprojPackageLocationLocal(
                path = productName
            )
        )
    }
    integrationTargets.forEach { target ->
        target.dependencies.add(XcprojPackageTargetDependency(productName = productName))
        target.buildPhases.filterIsInstance<XcprojFrameworkBuildPhase>().forEach { phase ->
            target.packageProductMembers.add(
                XcprojSwiftPackageProductMember(
                    productName = productName,
                    buildFile = XcprojTargetBuildFile(
                        buildPhase = phase.reference
                    )
                )
            )
        }
    }
    return true
}

// This is crude approximation of platforms; we should use "xcodebuild -showBuildSettings" or something similar in the future
internal fun platforms(target: XcprojNativeTarget): Set<String> {
    fun setting(name: String): List<String>? {
        fun values(settings: Map<String, XcprojBuildSettingValue>): List<String>? =
            settings[name]?.values?.flatMap { it.split(Regex("\\s+")) }?.filter { it.isNotBlank() }
        return values(target.buildSettings)
    }
    return (setting("SUPPORTED_PLATFORMS") ?: setting("SDKROOT"))?.toSet() ?: emptySet()
}
