/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageParserCallback
import org.junit.Test
import org.slf4j.event.EventRecodingLogger
import org.slf4j.event.SubstituteLoggingEvent
import org.slf4j.helpers.SubstituteLogger
import java.text.ParseException
import java.util.concurrent.ArrayBlockingQueue
import kotlin.test.assertEquals

class TCServiceMessageOutputStreamHandlerTest {
    private val client = Mock()
    private val logEvents = ArrayBlockingQueue<SubstituteLoggingEvent>(10)
    private val log = EventRecodingLogger(SubstituteLogger("", logEvents, false), logEvents)
    private val handler = TCServiceMessageOutputStreamHandler(client, {}, log, 35)

    private val clientCalls get() = client.log.toString()
    private val logString get() = logEvents.map { it.message }.toString()
    
    @Test
    fun testLines() {
        handler.write("Test1\n".toByteArray())
        handler.write("Test2".toByteArray())
        handler.close()
        assertEquals(
            "TEXT: `Test1\n`\n" +
                    "TEXT: `Test2`\n",
            clientCalls
        )
        assertEquals("[]", logString)
    }

    @Test
    fun testLines2() {
        handler.write("Test1\n\r".toByteArray())
        handler.write("Test2".toByteArray())
        handler.close()
        assertEquals(
            "TEXT: `Test1\n`\n" +
                    "TEXT: `\rTest2`\n",
            clientCalls
        )
        assertEquals("[]", logString)
    }

    @Test
    fun testLines3() {
        handler.write("Test1\n\r\n".toByteArray())
        handler.write("Test2".toByteArray())
        handler.close()
        assertEquals(
            "TEXT: `Test1\n`\n" +
                    "TEXT: `\r\n`\n" +
                    "TEXT: `Test2`\n",
            clientCalls
        )
        assertEquals("[]", logString)
    }

    @Test
    fun testFlush() {
        //testStarted key correspond to [jetbrains.buildServer.messages.serviceMessages.TestStarted] class and requires "name" attribute
        handler.write("xxx##teamc".toByteArray())
        handler.flush()
        handler.write("ity[testStarted name='test']\n".toByteArray())
        handler.write("yyy##teamcity[testStarted name='test']".toByteArray())
        handler.close()
        assertEquals(
            "TEXT: `xxx`\n" +
                    "MESSAGE: `##teamcity[testStarted name='test']`\n" +
                    "TEXT: `\n`\n" + // this will be ignore in org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient.regularText
                    "TEXT: `yyy`\n" +
                    "MESSAGE: `##teamcity[testStarted name='test']`\n",
            clientCalls
        )
        assertEquals("[]", logString)
    }

    @Test
    fun testMessage() {
        //testStarted key correspond to [jetbrains.buildServer.messages.serviceMessages.TestStarted] class and requires "name" attribute
        handler.write("xxx##teamcity[testStarted name='test']\n".toByteArray())
        handler.write("yyy##teamcity[testStarted name='test']".toByteArray())
        handler.close()
        assertEquals(
            "TEXT: `xxx`\n" +
                    "MESSAGE: `##teamcity[testStarted name='test']`\n" +
                    "TEXT: `\n`\n" + // this will be ignore in org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient.regularText
                    "TEXT: `yyy`\n" +
                    "MESSAGE: `##teamcity[testStarted name='test']`\n",
            clientCalls
        )
        assertEquals("[]", logString)
    }

    @Test
    fun testMessageSplit() {
        //testStarted key correspond to [jetbrains.buildServer.messages.serviceMessages.TestStarted] class and requires "name" attribute
        handler.write("xxx##teamc".toByteArray())
        handler.write("ity[testStarted name='test']\n".toByteArray())
        handler.write("yyy##teamcity[testStarted name='test']".toByteArray())
        handler.close()
        assertEquals(
            "TEXT: `xxx`\n" +
                    "MESSAGE: `##teamcity[testStarted name='test']`\n" +
                    "TEXT: `\n`\n" + // this will be ignore in org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient.regularText
                    "TEXT: `yyy`\n" +
                    "MESSAGE: `##teamcity[testStarted name='test']`\n",
            clientCalls
        )
        assertEquals("[]", logString)
    }

    @Test
    fun testNoOverflowNL() {
        // "##teamcity[]".length = 12
        // 35 - 12 = 23
        //                                     | <- limit here
        handler.write("012345##teamcity[1234567890123456789012]\n".toByteArray())
        handler.close()
        assertEquals(
            "TEXT: `012345`\n" +
                    "MESSAGE: `##teamcity[1234567890123456789012]`\n" +
                    "TEXT: `\n`\n",
            clientCalls
        )
        assertEquals("[]", logString)
    }


    @Test
    fun testNoOverflowNoNL() {
        // "##teamcity[]".length = 12
        // 35 - 12 = 23
        //                                          | <- limit here
        handler.write("012345##teamcity[12345678901234567890123]\n".toByteArray())
        handler.close()
        assertEquals(
            "TEXT: `012345`\n" +
                    "MESSAGE: `##teamcity[12345678901234567890123]`\n" +
                    "TEXT: `\n`\n",
            clientCalls
        )
        assertEquals("[]", logString)
    }

    @Test
    fun testOverflow() {
        // "##teamcity[]".length = 12
        // 35 - 12 = 23
        //                                         | <- limit here
        handler.write("012345##teamcity[123456789012345678901234]\n".toByteArray())
        handler.close()
        assertEquals(
            "TEXT: `012345`\n" +
                    "EXCEPTION: `java.text.ParseException: The service message is invalid because it does not end with ] character: ##teamcity[123456789012345678901234`, `##teamcity[123456789012345678901234`\n" +
                    "TEXT: `]\n`\n",
            clientCalls
        )
        assertEquals(
            "[Cannot process process output: too long teamcity service message (more then 1Mb). Event was lost. See stdout for more details.]",
            logString
        )
    }

    class Mock : ServiceMessageParserCallback {
        val log = StringBuffer()

        override fun parseException(p0: ParseException, p1: String) {
            log.append("EXCEPTION: `$p0`, `$p1`\n")
        }

        override fun serviceMessage(p0: ServiceMessage) {
            log.append("MESSAGE: `$p0`\n")
        }

        override fun regularText(p0: String) {
            log.append("TEXT: `$p0`\n")
        }
    }
}