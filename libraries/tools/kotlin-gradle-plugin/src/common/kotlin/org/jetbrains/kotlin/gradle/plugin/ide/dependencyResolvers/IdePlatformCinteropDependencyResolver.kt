/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.native.internal.getAllCInteropOutputFiles
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object IdePlatformCinteropDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()
        val project = sourceSet.project

        val nativeCompilation = sourceSet.internal.compilations.singleOrNull().safeAs<KotlinNativeCompilation>() ?: return emptySet()
        val cinteropFiles = project.getAllCInteropOutputFiles(nativeCompilation)

        return resolveCinteropDependencies(project, cinteropFiles)
    }
}
