/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory

private val reservedMemoryMb = 9000 // system processes, gradle daemon, kotlin daemon, etc ...

val totalMaxMemoryForTestsMb: Int
    get() {
        val mxbean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val availableMemoryMb = (mxbean.totalMemorySize / 1048576 - reservedMemoryMb).toInt()
        return availableMemoryMb - (availableMemoryMb % 1024)
    }
