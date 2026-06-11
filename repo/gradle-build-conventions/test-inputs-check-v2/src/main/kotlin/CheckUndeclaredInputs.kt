/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import jdk.jfr.consumer.RecordingFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.file.Paths
import kotlin.math.min

@CacheableTask
abstract class CheckUndeclaredInputs : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jfrFile: ConfigurableFileCollection

    @get:OutputFile
    abstract val undeclaredInputsFile: RegularFileProperty

    @get:Input
    abstract val verificationTasksDisabled: Property<Boolean>

    @get:Input
    abstract val teamcityBuild: Property<Boolean>

    @TaskAction
    fun execute() {
        if (verificationTasksDisabled.get()) {
            logger.warn("Skipping undeclared inputs checking because `kotlin.build.disable.verification.tasks` is true")
            return
        }
        val jfrFile = jfrFile.singleFile

        val undeclaredInputs = buildSet {
            RecordingFile(jfrFile.toPath()).use { recording ->
                while (recording.hasMoreEvents()) {
                    val event = recording.readEvent()
                    if (event.eventType.name !in listOf("jetbrains.UndeclaredInput")) continue
                    val path = event.getString("path")?.let(Paths::get) ?: continue
                    add(path)
                }
            }
        }
        undeclaredInputsFile.get().asFile.writeText(undeclaredInputs.joinToString("\n"))

        if (undeclaredInputs.isNotEmpty()) {
            error(buildString {
                appendLine("Undeclared test inputs found! (${undeclaredInputs.size})")
                if (!teamcityBuild.get()) {
                    appendLine("Open the JFR snapshot in IDEA, then go to: Events | Uncategorized | jetbrains.UndeclaredInput")
                    appendLine("You can find your ${jfrFile.name} here -> ${jfrFile.parentFile.absolutePath}")
                }
                appendLine("Displaying ${min(undeclaredInputs.size, 100)}/${undeclaredInputs.size} elements:")
                if (undeclaredInputs.size > 100) {
                    appendLine("See the full list here -> ${undeclaredInputsFile.get().asFile.absolutePath}")
                }
                undeclaredInputs.take(100).forEach { appendLine(it) }
            })
        }
    }
}
