/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.artifacts.transform.CacheableTransform
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
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJsonDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.isCompatibleArchive
import java.io.File
import java.util.zip.ZipFile

/** Reads the `package.json` of a klib that comes without an [NpmDependencyReport]. */
internal abstract class KlibToNpmDependencyReportTransform : TransformAction<KlibToNpmDependencyReportTransform.Parameters> {

    interface Parameters : TransformParameters {
        @get:Input
        val target: Property<String>
    }

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val klib: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val klib = klib.get().asFile
        val packageJson = klib.packageJsonText() ?: return
        val dependencies = KgpJson.default.decodeFromString<PackageJsonDependencies>(packageJson)

        NpmDependencyReport(
            target = parameters.target.get(),
            compilation = klib.nameWithoutExtension,
            dependencies = dependencies.dependencies,
            peerDependencies = dependencies.peerDependencies,
            optionalDependencies = dependencies.optionalDependencies,
        ).writeTo(outputs.file(NPM_DEPENDENCY_REPORT_FILE_NAME))
    }

    private fun File.packageJsonText(): String? = when {
        isDirectory -> resolve(NpmProject.PACKAGE_JSON).takeIf { it.isFile }?.readText()
        isCompatibleArchive -> ZipFile(this).use { zip ->
            zip.getEntry(NpmProject.PACKAGE_JSON)?.let { zip.getInputStream(it).reader().use { reader -> reader.readText() } }
        }
        else -> null
    }
}
