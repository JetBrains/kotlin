package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.RegexTaskToFriendTaskMapper
import org.jetbrains.kotlin.gradle.plugin.TaskToFriendTaskMapper
import org.jetbrains.kotlin.gradle.plugin.mapKotlinTaskProperties

internal open class KotlinTasksProvider {
    fun createKotlinJVMTask(project: Project, name: String, sourceSetName: String): KotlinCompile {
        return project.tasks.create(name, KotlinCompile::class.java).apply {
            this.sourceSetName = sourceSetName
            friendTaskName = taskToFriendTaskMapper[this]
            mapKotlinTaskProperties(project, this)
            outputs.upToDateWhen { isCacheFormatUpToDate }
        }
    }

    fun createKotlinJSTask(project: Project, name: String, sourceSetName: String): Kotlin2JsCompile =
            project.tasks.create(name, Kotlin2JsCompile::class.java)

    protected open val taskToFriendTaskMapper: TaskToFriendTaskMapper =
            RegexTaskToFriendTaskMapper.Default()
}

internal class AndroidTasksProvider : KotlinTasksProvider() {
    override val taskToFriendTaskMapper: TaskToFriendTaskMapper =
            RegexTaskToFriendTaskMapper.Android()
}