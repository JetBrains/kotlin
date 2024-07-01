/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.jetbrains.kotlin.backend.common.serialization.cityHash64String
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinCInteropLibraryDirectoryForIde
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropOutput
import org.jetbrains.kotlin.gradle.targets.native.internal.getPlatformCinteropDependenciesOrEmpty
import org.jetbrains.kotlin.gradle.targets.native.internal.getPlatformCinteropOutputsOrEmpty
import java.io.File

internal object IdePlatformCinteropDependencyResolver : IdeDependencyResolver, IdeDependencyResolver.WithBuildDependencies {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()
        val project = sourceSet.project

        val cinteropFiles = project.getPlatformCinteropOutputsOrEmpty(sourceSet).map { cinteropOutput ->
            project.copyCInteropFileForIdeIfNecessary(cinteropOutput)
        }

        return project.resolveCInteropDependencies(cinteropFiles)
    }

    /**
     * Copies the file into a directory specifically for the IDE, so it survives ./gradlew clean
     */
    private fun Project.copyCInteropFileForIdeIfNecessary(cinteropOutput: CInteropOutput): File {
        val klibFile = cinteropOutput.klibLocation.singleFile
        if (!klibFile.exists()) return klibFile

        val relativeFilePath = with(cinteropOutput) {
            val separator = File.separator
            val projectDir = (buildPath + separator + projectPath).replace(":", separator).trimStart('/')
            val hash = cinteropOutput.key.cityHash64String()
            val relativeFilePath = "$projectDir$separator$targetName-$compilationName-$cinteropName-$hash"
            if (klibFile.isFile) "$relativeFilePath.klib" else relativeFilePath
        }

        val outputFile = kotlinCInteropLibraryDirectoryForIde.resolve(relativeFilePath)
        if (outputFile.exists()) project.delete(outputFile)

        project.copy {
            it.from(klibFile)
            it.into(outputFile)
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
