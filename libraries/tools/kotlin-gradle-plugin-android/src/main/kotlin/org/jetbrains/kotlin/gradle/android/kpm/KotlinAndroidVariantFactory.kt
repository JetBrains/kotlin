/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.android.kpm

import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.attributes.Attribute
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.android.multiplatform.getAndroidRuntimeJars
import org.jetbrains.kotlin.gradle.android.multiplatform.setJavaTargetEnvironmentAttributeIfSupported
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinJvmVariant

fun KotlinGradleModule.createKotlinAndroidVariant(androidVariant: BaseVariant) {
    val kotlinVariant = fragments.create<KotlinJvmVariant>("android${androidVariant.buildType.name.capitalize()}")
    kotlinVariant.refines(androidCommon)

    kotlinVariant.compileDependencyFiles = androidVariant.compileConfiguration.apply {
        setJavaTargetEnvironmentAttributeIfSupported(kotlinVariant.project, "android")
        extendsFrom(kotlinVariant.project.configurations.getByName(kotlinVariant.compileDependencyConfigurationName))
    }

    kotlinVariant.runtimeDependencyFiles = androidVariant.runtimeConfiguration.apply {
        setJavaTargetEnvironmentAttributeIfSupported(kotlinVariant.project, "android")
        extendsFrom(kotlinVariant.project.configurations.getByName(kotlinVariant.runtimeDependencyConfigurationName))
    }

    val mainBytecodeKey = androidVariant.registerPreJavacGeneratedBytecode(
        kotlinVariant.compilationOutputs.classesDirs
    )

    kotlinVariant.compileDependencyFiles += project.files(
        androidVariant.getCompileClasspath(mainBytecodeKey),
        project.getAndroidRuntimeJars()
    )

    project.configurations.getByName(kotlinVariant.apiElementsConfigurationName).apply {
        setJavaTargetEnvironmentAttributeIfSupported(project, "android")
        val buildTypeAttrValue = project.objects.named(BuildTypeAttr::class.java, androidVariant.buildType.name)
        attributes.attribute(Attribute.of(BuildTypeAttr::class.java), buildTypeAttrValue)
    }
}
