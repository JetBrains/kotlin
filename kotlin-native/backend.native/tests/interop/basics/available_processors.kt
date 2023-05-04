/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.native.*
import kotlin.test.*
import cstdlib.*
import kotlinx.cinterop.*


@OptIn(kotlin.ExperimentalStdlibApi::class)
fun main(args: Array<String>) {
    require(args.size == 1) {
        "An expected anount of processors should be specified as program argument"
    }
    val x: Int = Platform.getAvailableProcessors()
    println("Got available processors: $x")
    assertTrue(x > 0)
    if (!(Platform.osFamily == OsFamily.LINUX && (Platform.cpuArchitecture == CpuArchitecture.ARM32 ||
                    Platform.cpuArchitecture == CpuArchitecture.ARM64))) {
        assertEquals(args[0].trim().toInt(), x)
    }

    setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", "12345", 1)
    assertEquals(Platform.getAvailableProcessors(), 12345)
    setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", Long.MAX_VALUE.toString(), 1)
    assertFailsWith<IllegalStateException> { Platform.getAvailableProcessors() }
    setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", "-1", 1)
    assertFailsWith<IllegalStateException> { Platform.getAvailableProcessors() }
    setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", "0", 1)
    assertFailsWith<IllegalStateException> { Platform.getAvailableProcessors() }
    // windows doesn't support empty env variables
    if (Platform.osFamily != OsFamily.WINDOWS) {
        setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", "", 1)
        assertFailsWith<IllegalStateException> { Platform.getAvailableProcessors() }
    }
    setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", "123aaaa", 1)
    assertFailsWith<IllegalStateException> { Platform.getAvailableProcessors() }
    unsetenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS")
    assertEquals(Platform.getAvailableProcessors(), x)
}
