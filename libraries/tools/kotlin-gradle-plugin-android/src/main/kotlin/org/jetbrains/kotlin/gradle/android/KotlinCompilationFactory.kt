/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.api.BaseVariant
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.external.DefaultSourceSetNameOption
import org.jetbrains.kotlin.gradle.targets.external.KotlinExternalTargetHandle
import org.jetbrains.kotlin.gradle.targets.external.addIdeImplementationDependency

fun KotlinExternalTargetHandle.createKotlinCompilation(
    variant: BaseVariant, commonSourceSet: KotlinSourceSet
) {
    val kotlinCompilation = createCompilation(
        name = variant.name,
        defaultSourceSetNameOption = DefaultSourceSetNameOption.KotlinConvention,
        classesOutputDirectory = project.layout.buildDirectory.dir("kotlin/android/classes/${variant.name}")
    )

    kotlinCompilation.defaultSourceSet.dependsOn(commonSourceSet)
    kotlinCompilation.defaultSourceSet.setupAndroidArtifactTypeForIde(project)
    kotlinCompilation.defaultSourceSet.addIdeImplementationDependency(project, project.getAndroidRuntimeJars())

    // Register compiled Kotlin Bytecode into variant
    val compiledKotlinKey = variant.registerPreJavacGeneratedBytecode(kotlinCompilation.classesDirs)

    setUpDependencyResolution(variant, kotlinCompilation)

    kotlinCompilation.addCompileDependenciesFiles(
        project.files(
            variant.getCompileClasspath(compiledKotlinKey),
            project.getAndroidRuntimeJars()
        )
    )
}
