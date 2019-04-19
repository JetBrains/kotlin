/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.stats.completion

import com.intellij.stats.completion.events.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*


object Fixtures {

    val userId = UUID.randomUUID().toString()

    private val relevance = mapOf(Pair("sort", 1.0.toString()), Pair("proximity", 2.0.toString()))
    
    val lookupList = listOf(
            LookupEntryInfo(0, 5, relevance),
            LookupEntryInfo(1, 9, relevance),
            LookupEntryInfo(2, 7, relevance)
    )

    val initialState = LookupState(lookupList.map { it.id }, lookupList, emptyList(), 0)

    val userFactors: Map<String, String> = mapOf(
            "avgTimeToType" to "0.6",
            "maxSelecterItem" to "10",
            "explicitSelectCountToday" to "100"
    )

    val history = mapOf(10 to ElementPositionHistory(listOf(StagePosition(0, 1))))
    
}

class EventSerializeDeserializeTest {

    private fun serializeDeserializeAndCheck(event: LogEvent) {
        val logLine = LogEventSerializer.toString(event)
        val eventFromString = LogEventSerializer.fromString(logLine).event!!
        assertEquals(logLine, LogEventSerializer.toString(eventFromString))
    }

    @Test
    fun `completion started event`() {
        val event = CompletionStartedEvent("", "", "", Fixtures.userId,
                                           "xx", "Java", true, 1, Fixtures.initialState,
                                           Fixtures.userFactors, 0, System.currentTimeMillis())
        serializeDeserializeAndCheck(event)
    }

    @Test
    fun `up down pressed event`() {
        val state = Fixtures.initialState
        var event: LogEvent = UpPressedEvent(Fixtures.userId, "xx", state, System.currentTimeMillis())
        serializeDeserializeAndCheck(event)

        event = DownPressedEvent(Fixtures.userId, "xx", state.withSelected(1), System.currentTimeMillis())
        serializeDeserializeAndCheck(event)
    }
    
    @Test
    fun `up down pressed event no additional items`() {
        val state = Fixtures.initialState.withoutNewItems()
        var event: LogEvent = UpPressedEvent(Fixtures.userId, "xx", state, System.currentTimeMillis())
        serializeDeserializeAndCheck(event)

        event = DownPressedEvent(Fixtures.userId, "xx", state.withSelected(1), System.currentTimeMillis())
        serializeDeserializeAndCheck(event)
    }
    
    @Test
    fun `completion cancelled event`() {
        val event = CompletionCancelledEvent(Fixtures.userId, "xx", System.currentTimeMillis())
        serializeDeserializeAndCheck(event)
    }
    
    @Test
    fun `item selected by typing event`() {
        val event = TypedSelectEvent(Fixtures.userId, "xx", Fixtures.initialState.withoutNewItems(),
                                     0, Fixtures.history, System.currentTimeMillis())
        serializeDeserializeAndCheck(event)
    }
    
    @Test
    fun `explicit select event`() {
        val state = Fixtures.initialState.withoutNewItems()
        val event = ExplicitSelectEvent(Fixtures.userId, "xx", state, 0,
                                        Fixtures.history, System.currentTimeMillis())
        serializeDeserializeAndCheck(event)
    }

    @Test
    fun `backspace event`() {
        val event: LogEvent = BackspaceEvent(Fixtures.userId, "xx",
                                             Fixtures.initialState.withoutNewItems(), 1, System.currentTimeMillis())
        serializeDeserializeAndCheck(event)
    }
    
    
    @Test
    fun `type event`() {
        val event = TypeEvent(Fixtures.userId, "xx", Fixtures.initialState, 2, System.currentTimeMillis())
        serializeDeserializeAndCheck(event)
    }

    @Test
    fun `deserialization with info`() {
        val json = JsonSerializer.toJson(First())
        val obj: DeserializationResult<Second> = JsonSerializer.fromJson(json, Second::class.java)

        assertThat(obj.absentFields).hasSize(2)
        assertThat(obj.absentFields).contains("absent_field0")
        assertThat(obj.absentFields).contains("absent_field1")

        assertThat(obj.unknownFields).hasSize(1)
        assertThat(obj.unknownFields).contains("unknown_field")
    }

    @Test
    fun `ignore static fields`() {
        val obj = JsonSerializer.fromJson(JsonSerializer.toJson(WithStaticField()), WithStaticField::class.java)
        assertThat(obj.absentFields).hasSize(0)
        assertThat(obj.unknownFields).hasSize(0)
    }
}


@Suppress("PropertyName", "unused")
private class First {
    val just_field: String = ""
    val unknown_field: Int = 0
}

@Suppress("PropertyName", "unused")
class Second {
    val just_field: String = ""
    val absent_field0: Double = 1.0
    val absent_field1: Double = 1.0
}

@Suppress("unused")
class WithStaticField {
    companion object {
        @JvmStatic
        val STATIC_FIELD = 10
    }
}

