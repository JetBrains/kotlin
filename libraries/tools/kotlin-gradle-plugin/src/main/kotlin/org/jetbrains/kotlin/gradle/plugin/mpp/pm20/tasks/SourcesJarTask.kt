/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.tasks

import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentSourcesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.ProtoTask
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.sourcesJarTaskNamed
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName

object SourcesJarTask : ProtoTask<Jar> {
    override fun registerTask(project: Project) {
        project.pm20Extension.modules.all { module ->
            val sourcesArtifactAppendix = dashSeparatedName(module.moduleClassifier, "all", "sources")

            // not really nice, but can't remove sourcesJarTaskNamed because of other clients, and don't want to duplicate code either
            sourcesJarTaskNamed(
                module.disambiguateName("allSourcesJar"),
                project,
                lazy { FragmentSourcesProvider().getAllFragmentSourcesAsMap(module).entries.associate { it.key.fragmentName to it.value.get() } },
                sourcesArtifactAppendix
            )
        }
    }

    override fun nameIn(module: KotlinGradleModule): String {
        TODO("Not yet implemented")
    }
}
