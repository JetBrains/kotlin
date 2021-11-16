/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.external

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

val KotlinSourceSet.ideDependencyConfigurationNames: Set<String>
    get() {
        return setOf(
            compileOnlyMetadataConfigurationName,
            runtimeOnlyMetadataConfigurationName,
            implementationMetadataConfigurationName,
            apiMetadataConfigurationName
        )
    }

fun KotlinSourceSet.ideDependencyConfigurations(project: Project): Set<Configuration> {
    return ideDependencyConfigurationNames.map { name ->
        project.configurations.getByName(name)
    }.toSet()
}

fun KotlinSourceSet.addIdeImplementationDependency(project: Project, dependencyNotation: Any) {
    project.dependencies.add(
        implementationMetadataConfigurationName, dependencyNotation
    )
}

fun KotlinSourceSet.dependsOnCommonMain(project: Project) {
    dependsOn(project.multiplatformExtension.sourceSets.getByName("commonMain"))
}

fun KotlinSourceSet.dependsOnCommonTest(project: Project) {
    dependsOn(project.multiplatformExtension.sourceSets.getByName("commonTest"))
}
