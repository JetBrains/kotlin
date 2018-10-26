/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import javax.inject.Inject

open class RunKotlinNativeTask @Inject constructor(
        private val myTarget: KotlinTarget
): DefaultTask() {

    var buildType = "RELEASE"
    var workingDir: Any = project.projectDir
    private var myArgs: List<String> = emptyList()
    private val myEnvironment: MutableMap<String, Any> = mutableMapOf()

    fun args(vararg args: Any) {
        myArgs = args.map { it.toString() }
    }

    fun environment(map: Map<String, Any>) {
        myEnvironment += map
    }

    override fun configure(configureClosure: Closure<Any>): Task {
        val task = super.configure(configureClosure)
        this.dependsOn += myTarget.compilations.main.linkTaskName("EXECUTABLE", buildType)
        return task
    }

    @TaskAction
    fun run() {
        project.exec {
            it.executable = myTarget.compilations.main.getBinary("EXECUTABLE", buildType).toString()
            it.args = myArgs
            it.environment = myEnvironment
            it.workingDir(workingDir)
        }
    }

    internal fun emptyConfigureClosure() = object : Closure<Any>(this) {
        override fun call(): RunKotlinNativeTask {
            return this@RunKotlinNativeTask
        }
    }
}
