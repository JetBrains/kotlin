/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinCInteropLibraryDirectoryForIde
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.internal.getPlatformCinteropDependenciesOrEmpty
import org.jetbrains.kotlin.gradle.utils.crc32ChecksumString
import java.io.File

internal object IdePlatformCinteropDependencyResolver : IdeDependencyResolver, IdeDependencyResolver.WithBuildDependencies {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()
        val project = sourceSet.project

        val cinteropFiles = project.getPlatformCinteropDependenciesOrEmpty(sourceSet).map { file ->
            project.copyCInteropFileForIdeIfNecessary(file)
        }

        return project.resolveCInteropDependencies(cinteropFiles)
    }

    /**
     * Copies the file into a directory specifically for the IDE, so it survives ./gradlew clean
     */
    private fun Project.copyCInteropFileForIdeIfNecessary(file: File): File {
        if (!file.exists()) return file
        val newFileName = "${file.nameWithoutExtension}-${file.crc32ChecksumString()}.${file.extension}"
        val outputFile = kotlinCInteropLibraryDirectoryForIde.resolve(newFileName)

        /* Copy only if really necessary */
        if (!outputFile.exists()) {
            file.copyTo(outputFile)
        }

        return outputFile
    }

    override fun dependencies(project: Project): Iterable<Any> {
        return project.kotlinExtension.sourceSets.mapNotNull { sourceSet ->
            if (sourceSet !is DefaultKotlinSourceSet) return@mapNotNull null
            if (!IdeMultiplatformImport.SourceSetConstraint.isNative(sourceSet)) return@mapNotNull null
            project.getPlatformCinteropDependenciesOrEmpty(sourceSet)
        }
    }
}
