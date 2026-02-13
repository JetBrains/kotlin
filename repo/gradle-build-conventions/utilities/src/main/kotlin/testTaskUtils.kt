/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.sun.management.OperatingSystemMXBean
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitivity
import java.lang.management.ManagementFactory

private val reservedMemoryMb = 9000 // system processes, gradle daemon, kotlin daemon, etc ...

val totalMaxMemoryForTestsMb: Int
    get() {
        val mxbean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val availableMemoryMb = (mxbean.totalPhysicalMemorySize / 1048576 - reservedMemoryMb).toInt()
        return availableMemoryMb - (availableMemoryMb % 1024)
    }

fun Project.ideaHomePathForTests(): Provider<Directory> = rootProject.layout.buildDirectory.dir("ideaHomeForTests")

fun <T : Task> T.attachIdeaHomeInput() {
    val buildTxtProvider = project.rootProject.tasks.named("createIdeaHomeForTests")
        .map { task -> task.outputs.files.singleFile.resolve("build.txt") }

    inputs.file(buildTxtProvider)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPathSensitivity(PathSensitivity.RELATIVE)
}