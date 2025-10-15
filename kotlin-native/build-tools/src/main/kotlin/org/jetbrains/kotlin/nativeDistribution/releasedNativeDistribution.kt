/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.konan.target.HostManager
import javax.inject.Inject

private enum class ArchiveType(val extension: String) {
    TAR_GZ("tar.gz"),
    ZIP("zip");

    override fun toString() = extension

    companion object {
        val HOST_DEFAULT = if (HostManager.hostIsMingw) ZIP else TAR_GZ
    }
}

private abstract class ExtractNativeDistribution : TransformAction<ExtractNativeDistribution.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val archiveType: Property<ArchiveType>
    }

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    override fun transform(outputs: TransformOutputs) {
        fileSystemOperations.sync {
            val fileTree = when (parameters.archiveType.get()) {
                ArchiveType.ZIP -> archiveOperations.zipTree(inputArtifact)
                ArchiveType.TAR_GZ -> archiveOperations.tarTree(inputArtifact)
            }
            from(fileTree) {
                // Drop the top-level directory during extraction.
                eachFile {
                    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                }
                includeEmptyDirs = false
            }
            into(outputs.dir(inputArtifact.get().asFile.name))
        }
    }
}

fun Project.releasedNativeDistributionConfiguration(version: String): Configuration {
    val name = "releasedNativeDistributionV${version}"
    return try {
        configurations.create(name) {
            isTransitive = false
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
            }
        }.also {
            dependencies {
                // declared to be included in verification-metadata.xml
                "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$version:macos-aarch64@tar.gz")
                "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$version:macos-x86_64@tar.gz")
                "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$version:linux-x86_64@tar.gz")
                "implicitDependencies"("org.jetbrains.kotlin:kotlin-native-prebuilt:$version:windows-x86_64@zip")
                it("org.jetbrains.kotlin:kotlin-native-prebuilt:$version:${HostManager.platformName()}@${ArchiveType.HOST_DEFAULT}")
                registerTransform(ExtractNativeDistribution::class) {
                    from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArchiveType.HOST_DEFAULT.extension)
                    to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
                    parameters {
                        archiveType.set(ArchiveType.HOST_DEFAULT)
                    }
                }
            }
        }
    } catch (_: InvalidUserDataException) {
        configurations.getByName(name)
    }
}
