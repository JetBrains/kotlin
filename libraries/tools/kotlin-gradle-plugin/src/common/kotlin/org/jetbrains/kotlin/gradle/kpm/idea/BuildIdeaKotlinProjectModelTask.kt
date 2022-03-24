/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

internal fun Project.locateOrRegisterBuildIdeaKotlinProjectModelTask(): TaskProvider<BuildIdeaKotlinProjectModelTask> {
    return locateOrRegisterTask(BuildIdeaKotlinProjectModelTask.defaultTaskName)
}

internal open class BuildIdeaKotlinProjectModelTask : DefaultTask() {
    @OutputDirectory
    val outputDirectory = project.buildDir.resolve("ideaKotlinProjectModel")

    private val builder = project.pm20Extension.ideaKotlinProjectModelBuilder

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    protected fun buildIdeaKotlinProjectModel() {
        outputDirectory.mkdirs()

        val model = builder.buildIdeaKotlinProjectModel()
        val textFile = outputDirectory.resolve("model.txt")
        textFile.writeText(model.toString())

        val binaryFile = outputDirectory.resolve("model.bin")
        binaryFile.writeBytes(ByteArrayOutputStream().use { byteArrayOutputStream ->
            ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream -> objectOutputStream.writeObject(model) }
            byteArrayOutputStream.toByteArray()
        })

        val jsonFile = outputDirectory.resolve("model.json")
        jsonFile.writeText(GsonBuilder().setLenient().setPrettyPrinting().create().toJson(model))
    }

    companion object {
        const val defaultTaskName = "buildIdeaKotlinProjectModel"
    }
}
