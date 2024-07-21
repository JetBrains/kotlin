package org.jetbrains.litmuskt

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.windows.GetSystemInfo
import platform.windows.SYSTEM_INFO

actual val affinityManager: AffinityManager? = null

@OptIn(ExperimentalForeignApi::class)
actual fun cpuCount(): Int = memScoped {
    val systemInfo = alloc<SYSTEM_INFO>()
    GetSystemInfo(systemInfo.ptr)
    systemInfo.dwNumberOfProcessors.toInt()
}
