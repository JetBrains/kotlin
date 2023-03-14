/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.unambiguousNameInProject
import org.jetbrains.kotlin.gradle.plugin.mpp.sourcesJarTaskNamed
import org.jetbrains.kotlin.gradle.utils.future

interface GradleKpmSourceArchiveTaskConfigurator<in T : GradleKpmVariant> {
    fun registerSourceArchiveTask(variant: T): TaskProvider<*>?
}

object GradleKpmDefaultKotlinSourceArchiveTaskConfigurator : GradleKpmSourceArchiveTaskConfigurator<GradleKpmVariant> {
    override fun registerSourceArchiveTask(variant: GradleKpmVariant): TaskProvider<*> {
        return sourcesJarTaskNamed(
            taskName = variant.sourceArchiveTaskName,
            componentName = variant.name,
            project = variant.project,
            sourceSets = variant.project.future {
                GradleKpmFragmentSourcesProvider().getSourcesFromRefinesClosureAsMap(variant)
                    .entries.associate { it.key.unambiguousNameInProject to it.value.get() }
            },
            artifactNameAppendix = variant.name,
            componentTypeName = "variant",
        )
    }
}
