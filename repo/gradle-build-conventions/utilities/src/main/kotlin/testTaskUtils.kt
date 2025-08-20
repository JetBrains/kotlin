/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.sun.management.OperatingSystemMXBean
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import java.lang.management.ManagementFactory

private val reservedMemoryMb = 9000 // system processes, gradle daemon, kotlin daemon, etc ...

val totalMaxMemoryForTestsMb: Int
    get() {
        val mxbean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        return (mxbean.totalPhysicalMemorySize / 1048576 - reservedMemoryMb).toInt()
    }

fun Project.ideaHomePathForTests(): Provider<Directory> = rootProject.layout.buildDirectory.dir("ideaHomeForTests")
