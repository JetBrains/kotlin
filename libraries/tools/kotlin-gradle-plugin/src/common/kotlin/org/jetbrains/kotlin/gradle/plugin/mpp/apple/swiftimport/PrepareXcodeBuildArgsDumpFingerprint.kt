/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class PrepareXcodeBuildArgsDumpFingerprint : DefaultTask() {
    @get:Input
    abstract val xcodebuildPlatform: Property<String>

    @get:Input
    abstract val xcodebuildSdk: Property<String>

    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    @get:Input
    val additionalXcodeArgs: ListProperty<String> = project.objects.listProperty(String::class.java)
        .convention(emptyList())

    @get:Input
    abstract val packageResolvedSynchronization: Property<String>

    @get:Input
    abstract val directSwiftPMDependencies: SetProperty<SwiftPMDependency>

    @get:Input
    abstract val transitiveSwiftPMDependencies: Property<TransitiveSwiftPMDependencies>

    @get:Input
    abstract val buildSettingsFingerprint: Property<String>

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resolvedPackagesState: ConfigurableFileCollection

    @get:Internal
    abstract val packageResolvedFile: RegularFileProperty

    @get:OutputFile
    abstract val fingerprintsFile: RegularFileProperty

    @TaskAction
    fun prepareFingerprint() {
        val identifierDepsFingerprint = normalizedXcodeDumpTaskFingerprintByIdentifierDeps(
            packageResolvedSynchronization.get(),
            xcodebuildPlatform.get(),
            xcodebuildSdk.get(),
            architectures.get(),
            additionalXcodeArgs.get(),
            directSwiftPMDependencies.get(),
            transitiveSwiftPMDependencies.get(),
            buildSettingsFingerprint.get(),
        )
        val packageResolvedFingerprint = packageResolvedFile.orNull
            ?.asFile
            ?.takeIf { it.exists() }
            ?.let {
                normalizedXcodeDumpTaskFingerprintByPackageResolvedFile(
                    packageResolvedFile = it,
                    xcodebuildPlatform = xcodebuildPlatform.get(),
                    xcodebuildSdk = xcodebuildSdk.get(),
                    architectures = architectures.get(),
                    additionalXcodeArgs = additionalXcodeArgs.get(),
                    buildSettingsFingerprint = buildSettingsFingerprint.get(),
                )
            }

        val output = fingerprintsFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            dumpTaskFingerprintJson.encodeToString(
                XcodeDumpSharingFingerprints(
                    packageResolvedHash = packageResolvedFingerprint,
                    identifierDepsHash = identifierDepsFingerprint,
                )
            )
        )
    }

    companion object {
        const val TASK_NAME = "prepareXcodebuildArgsDumpFingerprint"
    }
}
