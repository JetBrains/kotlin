import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinProjectModel
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Project.buildIdeaKotlinProjectModel(): IdeaKotlinProjectModel {
    return serviceOf<ToolingModelBuilderRegistry>().getBuilder(IdeaKotlinProjectModel::class.java.name)
        .buildAll(IdeaKotlinProjectModel::class.java.name, this) as IdeaKotlinProjectModel
}

fun createKpmProject(): Pair<ProjectInternal, KotlinPm20ProjectExtension> {
    val project = ProjectBuilder.builder().build() as ProjectInternal
    project.plugins.apply(KotlinPm20PluginWrapper::class.java)
    return project to project.extensions.getByType(KotlinPm20ProjectExtension::class.java)
}
