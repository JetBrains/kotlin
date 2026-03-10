/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class ComputeLocalPackageDependencyInputFiles : DefaultTask() {

    @get:Input
    val localPackages: SetProperty<File> = project.objects.setProperty(File::class.java)

    /**
     * Recompute if the manifests change
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val manifests get() = localPackages.map { it.map { it.resolve("Package.swift") } }

    @get:OutputFile
    val filesToTrackFromLocalPackages: RegularFileProperty = project.objects.fileProperty().convention(
        project.layout.buildDirectory.file("kotlin/swiftImportFilesToTrackFromLocalPackages")
    )

    @get:Inject
    protected abstract val execOps: ExecOperations

    @TaskAction
    fun generateSwiftPMSyntheticImportProjectAndFetchPackages() {
        // FIXME: KT-84800 Fingerprint transitive local packages
        val localPackageFiles = localPackages.get().flatMap { packageRoot ->
            listOf(
                packageRoot.resolve("Package.swift")
            ) + findLocalPackageSources(packageRoot)
        }.map {
            it.path
        }
        filesToTrackFromLocalPackages.getFile().writeText(
            localPackageFiles.joinToString("\n")
        )
    }

    @Serializable
    data class PackageDescription(
        val targets: List<PackageTarget>
    ) {
        @Serializable
        data class PackageTarget(
            val path: String,
            val type: String,
            @kotlinx.serialization.SerialName("module_type") val moduleType: String,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun findLocalPackageSources(path: File): List<File> {
        val jsonBuffer = ByteArrayOutputStream()
        execOps.exec { exec ->
            exec.workingDir(path)
            exec.standardOutput = jsonBuffer
            exec.commandLine("swift", "package", "describe", "--type", "json")
            exec.environment.keys.filter {
                // Swift CLIs try to compile the manifest for iphonesimulator... with these envs
                it.startsWith("SDK")
            }.forEach {
                exec.environment.remove(it)
            }
        }
        val packageDescription = packageDescriptionJson.decodeFromStream<PackageDescription>(ByteArrayInputStream(jsonBuffer.toByteArray()))

        return packageDescription.targets.filter {
            (it.moduleType == "SwiftTarget" || it.moduleType == "ClangTarget") && it.type != "test"
        }.map {
            path.resolve(it.path)
        }
    }

    companion object {
        const val TASK_NAME = "computeLocalPackageDependencyInputFiles"
        private val packageDescriptionJson = Json {
            ignoreUnknownKeys = true
        }
    }
}