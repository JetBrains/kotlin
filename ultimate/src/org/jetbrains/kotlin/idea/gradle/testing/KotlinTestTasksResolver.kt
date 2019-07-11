/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.gradle.testing

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.Consumer
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModelBuilder
import org.jetbrains.kotlin.idea.configuration.getMppModel
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@Order(Int.MIN_VALUE)
open class KotlinTestTasksResolver : AbstractProjectResolverExtension() {
    companion object {
        private const val ENABLED_REGISTRY_KEY = "kotlin.gradle.testing.enabled"
    }

    private val LOG by lazy { Logger.getInstance(KotlinTestTasksResolver::class.java) }

    override fun getToolingExtensionsClasses(): Set<Class<out Any>> {
        return setOf(KotlinMPPGradleModelBuilder::class.java, Unit::class.java)
    }

    override fun populateModuleTasks(
        gradleModule: IdeaModule,
        ideModule: DataNode<ModuleData>,
        ideProject: DataNode<ProjectData>
    ): MutableCollection<TaskData> {
        if (!Registry.`is`(ENABLED_REGISTRY_KEY))
            return super.populateModuleTasks(gradleModule, ideModule, ideProject)

        val mppModel = resolverCtx.getMppModel(gradleModule)
            ?: return super.populateModuleTasks(gradleModule, ideModule, ideProject)

        return postprocessTaskData(mppModel, ideModule, nextResolver.populateModuleTasks(gradleModule, ideModule, ideProject))
    }

    private fun postprocessTaskData(
        mppModel: KotlinMPPGradleModel,
        ideModule: DataNode<ModuleData>,
        originalTaskData: MutableCollection<TaskData>
    ): MutableCollection<TaskData> {
        val testTaskNames = mutableSetOf<String>().apply {
            mppModel.targets.forEach { target ->
                target.testTasks.forEach { testTaskModel ->
                    add(testTaskModel.taskName)
                }
            }
        }

        fun buildNewTaskDataMarkedAsTest(original: TaskData): TaskData =
            TaskData(original.owner, original.name, original.linkedExternalProjectPath, original.description).apply {
                group = original.group
                type = original.type
                isInherited = original.isInherited

                isTest = true
            }

        val replacementMap: Map<TaskData, TaskData> = mutableMapOf<TaskData, TaskData>().apply {
            originalTaskData.forEach {
                if (it.name in testTaskNames && !it.isTest) {
                    put(it, buildNewTaskDataMarkedAsTest(it))
                }
            }
        }

        ideModule.children.filter { it.data in replacementMap }.forEach { it.clear(true) }
        replacementMap.values.forEach { ideModule.createChild(ProjectKeys.TASK, it) }

        return originalTaskData.mapTo(arrayListOf<TaskData>()) { replacementMap[it] ?: it }
    }

    override fun enhanceTaskProcessing(taskNames: MutableList<String>, jvmAgentSetup: String?, initScriptConsumer: Consumer<String>) {
        if (!Registry.`is`(ENABLED_REGISTRY_KEY))
            return

        try {
            val testLoggerScript = javaClass
                .getResourceAsStream("/org/jetbrains/kotlin/idea/gradle/testing/KotlinMppTestLogger.groovy")
                .bufferedReader()
                .readText()

            initScriptConsumer.consume(testLoggerScript)
        } catch (e: Exception) {
            LOG.error(e)
        }
    }
}