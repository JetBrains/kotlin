/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts.uklibsPublication

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.Uklib
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.Module
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext
import org.jetbrains.kotlin.gradle.tasks.locateTask

internal suspend fun Project.archiveUklibTask(): TaskProvider<ArchiveUklibTask> {
    val taskName = "archiveUklib"
    tasks.locateTask<ArchiveUklibTask>(taskName)?.let { return it }

    val archiveUklib = tasks.register(taskName, ArchiveUklibTask::class.java)

    val kgpFragments = multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments()

    kgpFragments.forEach { fragment ->
        archiveUklib.configure {
            // outputFile might be a directory or a file
            it.inputs.files(fragment.outputFile)
            // FIXME: some outputFiles are derived from a project.provider, use explicit task dependency as a workaround (remap output files from the task)
            it.dependsOn(fragment.providingTask)
        }
    }

    archiveUklib.configure {
        it.model.set(
            Uklib(
                module = Module(
                    fragments = kgpFragments.map {
                        it.fragment
                    }.toHashSet(),
                ),
                manifestVersion = Uklib.CURRENT_UMANIFEST_VERSION,
            )
        )
    }

    return archiveUklib
}

// FIXME: Project dependencies?
// FIXME: Look at the (unresolved?) dependencies of all configurations that contribute to the POM (UsageComponents?) and set compile/runtime accordingly
internal class UklibPomDependenciesRewriter {
    data class DependencyGA(
        val group: String?,
        val artifact: String,
    )

    data class TargetDep(
        val ga: UklibPomDependenciesRewriter.DependencyGA,
        val scope: KotlinUsageContext.MavenScope?,
    )

    fun makeAllDependenciesCompile(
        pomXml: XmlProvider,
        mapping: Map<DependencyGA, TargetDep>,
    ) {
        val dependenciesNode = (pomXml.asNode().get("dependencies") as NodeList).filterIsInstance<Node>().singleOrNull() ?: return
        val dependencyNodes = (dependenciesNode.get("dependency") as? NodeList).orEmpty().filterIsInstance<Node>()

        dependencyNodes.forEach { dependencyNode ->
//            fun Node.getSingleChildValueOrNull(childName: String): String? =
//                ((get(childName) as NodeList?)?.singleOrNull() as Node?)?.text()
            val group = ((dependencyNode.get("groupId") as NodeList).singleOrNull() as Node?)?.text() ?: return@forEach
            val artifact = ((dependencyNode.get("artifactId") as NodeList).singleOrNull() as Node?)?.text() ?: return@forEach
            val scope = ((dependencyNode.get("scope") as NodeList).singleOrNull() as Node?)

            // Leave if it's already compile
            if (scope?.text() == "compile") return@forEach
            mapping[DependencyGA(group, artifact)]?.let {
                when (it.scope) {
                    KotlinUsageContext.MavenScope.COMPILE -> scope?.setValue("compile")
                    KotlinUsageContext.MavenScope.RUNTIME -> scope?.setValue("runtime")
                }
            }
            // When do we actually need to map dependencies?
        }
    }
}