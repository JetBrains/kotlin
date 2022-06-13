/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.testUtils

import org.gradle.api.Project
import org.gradle.api.artifacts.verification.DependencyVerificationMode
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProject
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProjectBinaryContainer
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProjectContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Project.buildIdeaKpmProject(): IdeaKpmProject {
    return serviceOf<ToolingModelBuilderRegistry>().getBuilder(IdeaKpmProject::class.java.name)
        .buildAll(IdeaKpmProject::class.java.name, this) as IdeaKpmProject
}

fun Project.buildIdeaKpmProjectBinary(): IdeaKpmProjectBinaryContainer {
    return serviceOf<ToolingModelBuilderRegistry>().getBuilder(IdeaKpmProjectContainer::class.java.name)
        .buildAll(IdeaKpmProjectContainer::class.java.name, this) as IdeaKpmProjectBinaryContainer
}

fun createKpmProject(): Pair<ProjectInternal, KotlinPm20ProjectExtension> {
    val project = ProjectBuilder.builder().build() as ProjectInternal
    project.plugins.apply(KotlinPm20PluginWrapper::class.java)
    project.gradle.startParameter.dependencyVerificationMode = DependencyVerificationMode.OFF
    project.repositories.mavenLocal()
    project.repositories.maven { it.setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    return project to project.extensions.getByType(KotlinPm20ProjectExtension::class.java)
}
