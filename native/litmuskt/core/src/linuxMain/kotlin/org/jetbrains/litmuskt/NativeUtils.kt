package org.jetbrains.litmuskt

import platform.posix.sysconf
import platform.posix._SC_NPROCESSORS_ONLN

actual fun cpuCount(): Int = sysconf(_SC_NPROCESSORS_ONLN).toInt()
