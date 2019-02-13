/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import groovy.lang.Closure
import org.gradle.api.tasks.JavaExec
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import javax.inject.Inject
import java.io.File

open class RunJvmTask: JavaExec() {
    var outputFileName: String? = null
    @Input
    @Option(option = "filter", description = "filter")
    var filter: String = ""

    override fun configure(configureClosure: Closure<Any>): Task {
        return super.configure(configureClosure)
    }

    private fun executeTask(output: java.io.OutputStream? = null) {
        val filterArgs = filter.split("\\s*,\\s*".toRegex())
                .map{ if (it.isNotEmpty()) listOf("-f", it) else listOf(null) }.flatten().filterNotNull()
        args(filterArgs)
        exec()
    }

    @TaskAction
    fun run() {
        if (outputFileName != null)
            File(outputFileName).outputStream().use { output ->  executeTask(output)}
        else
            executeTask()
    }
}
