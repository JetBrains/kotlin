/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.kotlin.bitcode.CompileToBitcodePlugin

/**
 * Depend on [CompileToBitcodePlugin]'s module named [moduleName] defined in [dependency].
 */
fun DependencyHandler.module(dependency: ProjectDependency, moduleName: String): ProjectDependency = dependency.copy().apply {
    capabilities {
        requireCapability(CppConsumerPlugin.moduleCapability(dependencyProject, moduleName))
    }
}

/**
 * Depend on [CompileToBitcodePlugin]'s module (testFixtures part) named [moduleName] defined in [dependency].
 */
fun DependencyHandler.moduleTestFixtures(dependency: ProjectDependency, moduleName: String): ProjectDependency = dependency.copy().apply {
    capabilities {
        requireCapability(CppConsumerPlugin.moduleTestFixturesCapability(dependencyProject, moduleName))
    }
}

/**
 * Depend on [CompileToBitcodePlugin]'s module (test part) named [moduleName] defined in [dependency].
 */
fun DependencyHandler.moduleTest(dependency: ProjectDependency, moduleName: String): ProjectDependency = dependency.copy().apply {
    capabilities {
        requireCapability(CppConsumerPlugin.moduleTestCapability(dependencyProject, moduleName))
    }
}

// TODO: Remove when .gradle is gone from K/N build.
fun DependencyHandler.module(project: Project, moduleName: String): ProjectDependency = module(
        dependency = this.project(mapOf("path" to project.path)) as ProjectDependency,
        moduleName
)