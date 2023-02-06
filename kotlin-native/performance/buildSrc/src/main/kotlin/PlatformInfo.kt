/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin

import org.jetbrains.kotlin.konan.target.*

object PlatformInfo {
    @JvmStatic
    fun isMac() = HostManager.hostIsMac

    @JvmStatic
    fun isWindows() = HostManager.hostIsMingw

    @JvmStatic
    fun isLinux() = HostManager.hostIsLinux

    @JvmStatic
    val hostName: String
        get() = HostManager.hostName
}