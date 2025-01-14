/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.parallel.Isolated

@Isolated // to prevent system properties conflicts
class HostManagerTest {

    @Test
    fun hostManagerWorksInUnknownOs() {
        withModifiedSystemProperties("os.name" to "FreeBSD") {
            assertDoesNotThrow { HostManager() }
            assertThrows<TargetSupportException> { HostManager.host }
            assertNull(HostManager.hostOrNull)
            assertFalse(HostManager.hostIsMac)
            assertFalse(HostManager.hostIsMingw)
            assertFalse(HostManager.hostIsLinux)
        }
    }

    @Synchronized
    private fun withModifiedSystemProperties(vararg pairs: Pair<String, String>, block: () -> Unit) {
        val backupValues = pairs.toMap().mapValues { (key, _) -> System.getProperty(key) }

        try {
            pairs.forEach { (key, value) -> System.setProperty(key, value) }
            block()
        } finally {
            backupValues.forEach { (key, value) ->
                if (value == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, value)
                }
            }
        }
    }
}