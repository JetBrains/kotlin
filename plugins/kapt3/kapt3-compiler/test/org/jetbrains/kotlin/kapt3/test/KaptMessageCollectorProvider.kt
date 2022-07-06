/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test

import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class KaptMessageCollectorProvider(private val testServices: TestServices) : TestService {
    private class StreamAndCollector {
        val outputStream = ByteArrayOutputStream()
        val messageCollector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, false)
    }

    private val cache: MutableMap<TestModule, StreamAndCollector> = mutableMapOf()

    private fun getStreamAndCollector(module: TestModule): StreamAndCollector {
        return cache.getOrPut(module) { StreamAndCollector() }
    }

    fun getCollector(module: TestModule): PrintingMessageCollector {
        return getStreamAndCollector(module).messageCollector
    }

    fun getErrorStream(module: TestModule): ByteArrayOutputStream {
        return getStreamAndCollector(module).outputStream
    }
}

val TestServices.messageCollectorProvider: KaptMessageCollectorProvider by TestServices.testServiceAccessor()

