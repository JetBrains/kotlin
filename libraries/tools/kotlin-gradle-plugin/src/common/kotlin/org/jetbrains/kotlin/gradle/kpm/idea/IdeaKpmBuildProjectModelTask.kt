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
import org.jetbrains.kotlin.kpm.idea.proto.writeTo
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal fun Project.locateOrRegisterIdeaKpmBuildProjectModelTask(): TaskProvider<IdeaKpmBuildProjectModelTask> {
    return locateOrRegisterTask(IdeaKpmBuildProjectModelTask.defaultTaskName)
}

/**
 * Internal Task used for troubleshooting/debugging/diagnosing IdeaKpm model building.
 */
internal open class IdeaKpmBuildProjectModelTask : DefaultTask() {
    @OutputDirectory
    val outputDirectory = project.buildDir.resolve("ideaKpmProjectModel")

    private val builder = project.pm20Extension.ideaKpmProjectModelBuilder

    init {
        outputs.upToDateWhen { false }
    }

    @OptIn(ExperimentalTime::class)
    @TaskAction
    protected fun buildIdeaKpmProjectModel() {
        outputDirectory.mkdirs()

        val model = builder.buildIdeaKpmProject()
        val serializationContext = builder.buildSerializationContext()
        val textFile = outputDirectory.resolve("model.txt")
        textFile.writeText(model.toString())

        val javaIoSerializableDuration = measureTime {
            val javaIoSerializableBinaryFile = outputDirectory.resolve("model.java.bin")
            javaIoSerializableBinaryFile.writeBytes(ByteArrayOutputStream().use { byteArrayOutputStream ->
                ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream -> objectOutputStream.writeObject(model) }
                byteArrayOutputStream.toByteArray()
            })
        }

        val protoDuration = measureTime {
            val protoBinaryFile = outputDirectory.resolve("model.proto.bin")
            if (protoBinaryFile.exists()) protoBinaryFile.delete()
            protoBinaryFile.outputStream().use { stream ->
                model.writeTo(stream, serializationContext)
            }
        }

        val gsonDuration = measureTime {
            val jsonFile = outputDirectory.resolve("model.json")
            jsonFile.writeText(GsonBuilder().setLenient().setPrettyPrinting().create().toJson(model))
        }

        logger.quiet(
            "java.io.Serializable took $javaIoSerializableDuration\n" +
                    "protobuf took $protoDuration\n" +
                    "Gson took $gsonDuration"
        )
    }

    companion object {
        const val defaultTaskName = "buildIdeaKpmProjectModel"
    }
}
