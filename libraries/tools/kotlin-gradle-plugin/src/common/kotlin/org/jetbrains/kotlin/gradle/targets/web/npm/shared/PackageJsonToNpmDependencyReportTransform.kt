/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJsonDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.isCompatibleArchive
import java.io.File
import java.util.zip.ZipFile

/** Reads the `package.json` of a klib, an unpacked klib or a bare `package.json` that comes without an [NpmDependencyReport]. */
@DisableCachingByDefault(because = "Reading package.json and converting it to an npm dependency report is not worth caching")
internal abstract class PackageJsonToNpmDependencyReportTransform : TransformAction<PackageJsonToNpmDependencyReportTransform.Parameters> {

    interface Parameters : TransformParameters {
        @get:Input
        val target: Property<String>
    }

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val file = input.get().asFile
        val dependencies = file.readPackageJsonDependencies() ?: return

        NpmDependencyReport(
            target = parameters.target.get(),
            compilation = file.nameWithoutExtension,
            dependencies = dependencies.dependencies,
            peerDependencies = dependencies.peerDependencies,
            optionalDependencies = dependencies.optionalDependencies,
        ).writeTo(outputs.file(NPM_DEPENDENCY_REPORT_FILE_NAME))
    }
}

internal fun File.readPackageJsonDependencies(): PackageJsonDependencies? =
    packageJsonText()?.let { KgpJson.default.decodeFromString<PackageJsonDependencies>(it) }

private fun File.packageJsonText(): String? = when {
    isFile && name == NpmProject.PACKAGE_JSON -> readText()
    isDirectory -> resolve(NpmProject.PACKAGE_JSON).takeIf { it.isFile }?.readText()
    isCompatibleArchive -> ZipFile(this).use { zip ->
        zip.getEntry(NpmProject.PACKAGE_JSON)?.let { zip.getInputStream(it).reader().use { reader -> reader.readText() } }
    }
    else -> null
}
