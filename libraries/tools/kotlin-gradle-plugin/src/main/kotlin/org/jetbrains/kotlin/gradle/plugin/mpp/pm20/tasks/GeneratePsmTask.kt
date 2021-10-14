/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.tasks

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.mpp.GenerateProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.buildProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.ProtoTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

object GeneratePsmTask : ProtoTask<GenerateProjectStructureMetadata> {
    override fun registerTask(project: Project) {
        project.pm20Extension.modules.all { module ->
            project.registerTask<GenerateProjectStructureMetadata>(nameIn(module)) { task ->
                task.lazyKotlinProjectStructureMetadata = lazy { buildProjectStructureMetadata(module) }
            }
        }
    }

    override fun nameIn(module: KotlinGradleModule): String =
        lowerCamelCaseName("generate", module.moduleClassifier, "ProjectStructureMetadata")
}

// TODO: move the task itself closer?
