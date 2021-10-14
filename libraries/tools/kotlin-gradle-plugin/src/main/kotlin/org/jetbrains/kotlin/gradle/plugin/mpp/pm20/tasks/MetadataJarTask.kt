/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.tasks

import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.ProtoTask
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.taskProvider
import org.jetbrains.kotlin.gradle.targets.metadata.filesWithUnpackedArchives
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import java.util.concurrent.Callable

internal object MetadataJarTask : ProtoTask<Jar> {
    override fun registerTask(project: Project) {
        project.pm20Extension.modules.all { module ->
            val allMetadataJar = project.registerTask<Jar>(nameIn(module)) { task ->
                if (module.moduleClassifier != null) {
                    task.archiveClassifier.set(module.moduleClassifier)
                }
                task.archiveAppendix.set("metadata")
                task.from() // TODO: ????
                task.from(module.taskProvider(GeneratePsmTask).map { it.resultFile }) { spec ->
                    spec.into("META-INF")
                        .rename { MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME }
                }
            }

            val registry = project.pm20Extension.metadataCompilationRegistryByModuleId[module.moduleIdentifier]!!

            module.fragments.all { fragment ->
                allMetadataJar.configure { jar ->
                    val metadataOutput = project.files(Callable {
                        val compilationData = registry.byFragment(fragment)
                        project.filesWithUnpackedArchives(compilationData.output.allOutputs, setOf(KLIB_FILE_EXTENSION))
                    })
                    jar.from(metadataOutput) { spec ->
                        spec.into(fragment.fragmentName)
                    }
                }
            }
        }
    }

    override fun nameIn(module: KotlinGradleModule): String =
        lowerCamelCaseName(module.moduleClassifier, "metadataJar")
}
