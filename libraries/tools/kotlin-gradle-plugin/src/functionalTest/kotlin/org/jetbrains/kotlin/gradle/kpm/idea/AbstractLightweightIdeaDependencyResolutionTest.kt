/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.Project
import org.gradle.api.artifacts.verification.DependencyVerificationMode
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.enableDefaultStdlibDependency

abstract class AbstractLightweightIdeaDependencyResolutionTest {

    fun buildProject(builder: ProjectBuilder.() -> Unit = {}): ProjectInternal {
        return (ProjectBuilder.builder().also(builder).build()).also { project ->
            project.gradle.startParameter.dependencyVerificationMode = DependencyVerificationMode.OFF
            project.enableDefaultStdlibDependency(false)
            project.repositories.mavenLocal()
            project.repositories.maven { it.setUrl("https://cache-redirector.jetbrains.com/maven-central") }
        } as ProjectInternal
    }

    val Project.konanDistribution get() = KonanDistribution(project.konanHome)
}
