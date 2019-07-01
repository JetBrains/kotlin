/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency

/**
 * Info about NPM projects inside particular gradle [project].
 */
class NpmProjects(
    val project: Project,

    val npmProjects: List<NpmProjectPackage>,
    val npmProjectsByCompilation: Map<KotlinJsCompilation, NpmProjectPackage>,
    val npmProjectsByNpmDependency: Map<NpmDependency, NpmProjectPackage>,

    val taskRequirements: Map<RequiresNpmDependencies, Collection<RequiredKotlinJsDependency>>
)