/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.plainBuildSystem

import org.jetbrains.kotlin.project.modelx.FragmentId
import org.jetbrains.kotlin.project.modelx.ModuleId
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Describes Kotlin Project Model files structure for [PlainCompilerFacade]
 */
interface KpmFileStructure {
    val kpmFilePath: Path
    val sourcesRoot: Path
    val variantOutputDir: Path
    val metadataOutputDir: Path

    val metadataExtractDir: Path

    fun jsOutputDir(variantId: FragmentId): Path
    fun jvmOutputDir(variantId: FragmentId): Path

    fun metadataArtifact(moduleId: ModuleId): Path
    fun jvmArtifact(moduleId: ModuleId, variantId: FragmentId): Path
    fun jsArtifact(moduleId: ModuleId, variantId: FragmentId): Path

    fun kpmVariantInfoFilePath(variantId: FragmentId): Path
}

class DefaultKpmFileStructure(basePath: Path = Paths.get(".")) : KpmFileStructure {
    override val kpmFilePath = basePath.resolve("out/metadata/META-INF/kpm.json")
    override val sourcesRoot = basePath.resolve("src")
    override val variantOutputDir = basePath.resolve("out/variants")
    override val metadataOutputDir = basePath.resolve("out/metadata")

    val artifactsDir = basePath.resolve("out/artifacts")

    override val metadataExtractDir = basePath.resolve("out/fragmentMetadata/")

    override fun metadataArtifact(moduleId: ModuleId) =
        artifactsDir.resolve("$moduleId-metadata.jar")

    override fun jsOutputDir(variantId: FragmentId) =
        variantOutputDir.resolve(variantId)
    override fun jvmOutputDir(variantId: FragmentId) =
        variantOutputDir.resolve(variantId)

    override fun jvmArtifact(moduleId: ModuleId, variantId: FragmentId): Path =
        artifactsDir.resolve("$moduleId-$variantId.jar")
    override fun jsArtifact(moduleId: ModuleId, variantId: FragmentId): Path =
        artifactsDir.resolve("$moduleId-$variantId.jar")

    override fun kpmVariantInfoFilePath(variantId: FragmentId): Path =
        variantOutputDir.resolve("$variantId/META-INF/kpm-variant.json")
}

