/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.sourcesJarTaskNamed

interface KotlinSourceArchiveTaskConfigurator<in T : KotlinGradleVariant> {
    fun registerSourceArchiveTask(variant: T): TaskProvider<*>?
}

object DefaultKotlinSourceArchiveTaskConfigurator : KotlinSourceArchiveTaskConfigurator<KotlinGradleVariant> {
    override fun registerSourceArchiveTask(variant: KotlinGradleVariant): TaskProvider<*> {
        return sourcesJarTaskNamed(
            taskName = variant.sourceArchiveTaskName,
            project = variant.project,
            sourceSets = lazy {
                FragmentSourcesProvider().getSourcesFromRefinesClosureAsMap(variant)
                    .entries.associate { it.key.fragmentName to it.value.get() }
            },
            artifactNameAppendix = variant.name
        )
    }
}
