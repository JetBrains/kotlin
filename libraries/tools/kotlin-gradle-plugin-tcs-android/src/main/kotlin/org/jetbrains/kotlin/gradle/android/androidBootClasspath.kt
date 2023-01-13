/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.android.AndroidKotlinSourceSet.Companion.android
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import java.util.concurrent.Callable

internal fun Project.androidBootClasspath(): FileCollection {
    return project.files(Callable { project.extensions.getByType<BaseExtension>().bootClasspath })
}

internal class AndroidBootClasspathIdeDependencyResolver(private val project: Project) : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (sourceSet.android == null) return emptySet()
        return setOf(
            IdeaKotlinResolvedBinaryDependency(
                binaryType = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
                classpath = IdeaKotlinClasspath(project.androidBootClasspath().files),
                extras = mutableExtrasOf(),
                coordinates = IdeaKotlinBinaryCoordinates("com.android", "sdk", "7.4")
            )
        )
    }
}
