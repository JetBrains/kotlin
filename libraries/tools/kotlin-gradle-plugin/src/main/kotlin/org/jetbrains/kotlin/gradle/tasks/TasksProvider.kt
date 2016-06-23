package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.Task
import org.gradle.api.Project
import org.gradle.api.tasks.SourceTask
import org.jetbrains.kotlin.gradle.plugin.RegexTaskToFriendTaskMapper
import org.jetbrains.kotlin.gradle.plugin.TaskToFriendTaskMapper
import org.jetbrains.kotlin.gradle.plugin.friendTaskName
import java.net.URLClassLoader

/**
 * Tasks provider to be used wrapper
 * Created by Nikita.Skvortsov
 * date: 17.12.2014.
 */

open class KotlinTasksProvider(val tasksLoader: ClassLoader) {
    val kotlinJVMCompileTaskClass: Class<AbstractCompile> =
            tasksLoader.loadClass("org.jetbrains.kotlin.gradle.tasks.KotlinCompile") as Class<AbstractCompile>

    val kotlinJSCompileTaskClass: Class<AbstractCompile> =
            tasksLoader.loadClass("org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile") as Class<AbstractCompile>

    val kotlinJVMOptionsClass: Class<Any> =
            tasksLoader.loadClass("org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments") as Class<Any>

    fun createKotlinJVMTask(project: Project, name: String): AbstractCompile {
        return project.tasks.create(name, kotlinJVMCompileTaskClass).apply {
            friendTaskName = taskToFriendTaskMapper[this]
        }
    }

    fun createKotlinJSTask(project: Project, name: String): AbstractCompile {
        return project.tasks.create(name, kotlinJSCompileTaskClass)
    }

    protected open val taskToFriendTaskMapper: TaskToFriendTaskMapper =
            RegexTaskToFriendTaskMapper.Default()
}

class AndroidTasksProvider(tasksLoader: ClassLoader) : KotlinTasksProvider(tasksLoader) {
    override val taskToFriendTaskMapper: TaskToFriendTaskMapper =
            RegexTaskToFriendTaskMapper.Android()
}