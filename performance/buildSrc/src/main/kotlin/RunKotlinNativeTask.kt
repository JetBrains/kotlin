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
import java.io.File

open class RunKotlinNativeTask @Inject constructor(
        private val curTarget: KotlinTarget
): DefaultTask() {

    var buildType = "RELEASE"
    var workingDir: Any = project.projectDir
    var outputFileName: String? = null
    private var curArgs: List<String> = emptyList()
    private val curEnvironment: MutableMap<String, Any> = mutableMapOf()

    fun args(vararg args: Any) {
        curArgs = args.map { it.toString() }
    }

    fun environment(map: Map<String, Any>) {
        curEnvironment += map
    }

    override fun configure(configureClosure: Closure<Any>): Task {
        val task = super.configure(configureClosure)
        this.dependsOn += curTarget.compilations.main.linkTaskName("EXECUTABLE", buildType)
        return task
    }

    fun depends(taskName: String) {
        this.dependsOn += taskName
    }

    private fun executeTask(output: java.io.OutputStream? = null) {
        project.exec {
            it.executable = curTarget.compilations.main.getBinary("EXECUTABLE", buildType).toString()
            it.args = curArgs
            it.environment = curEnvironment
            it.workingDir(workingDir)
            if (output != null)
                it.standardOutput = output
        }
    }

    @TaskAction
    fun run() {
        if (outputFileName != null)
            File(outputFileName).outputStream().use { output ->  executeTask(output)}
        else
            executeTask()
    }

    internal fun emptyConfigureClosure() = object : Closure<Any>(this) {
        override fun call(): RunKotlinNativeTask {
            return this@RunKotlinNativeTask
        }
    }
}
