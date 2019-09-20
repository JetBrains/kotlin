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

import com.google.gson.GsonBuilder
import com.google.gson.internal.LinkedTreeMap
import com.intellij.stats.completion.events.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object JsonSerializer {
    private val gson = GsonBuilder().serializeNulls().create()
    private val ignoredFields = setOf(
      "recorderId", "timestamp", "sessionUid", "actionType", "userUid", "bucket", "recorderVersion"
    )

    fun toJson(obj: Any): String = gson.toJson(obj)

    fun <T> fromJson(json: String, clazz: Class<T>): DeserializationResult<T> {
        val declaredFields = allFields(clazz).map { it.name }.filter { it !in ignoredFields }
        val jsonFields = gson.fromJson(json, LinkedTreeMap::class.java).keys.map { it.toString() }.toSet()
        val value = gson.fromJson(json, clazz)

        val unknownFields = jsonFields.subtract(declaredFields)
        val absentFields = declaredFields.subtract(jsonFields)

        return DeserializationResult(value, unknownFields, absentFields)
    }

    private fun <T> allFields(clazz: Class<T>): List<Field> {
        val fields: List<Field> = clazz.declaredFields.asSequence()
            .filter { !Modifier.isStatic(it.modifiers) }
            .toList()
        if (clazz.superclass != null) {
            return fields + allFields(clazz.superclass)
        }
        return fields
    }
}


class DeserializationResult<out T>(val value: T, val unknownFields: Set<String>, val absentFields: Set<String>)


object LogEventSerializer {


    private val actionClassMap: Map<Action, Class<out LogEvent>> = mapOf(
        Action.COMPLETION_STARTED to CompletionStartedEvent::class.java,
        Action.TYPE to TypeEvent::class.java,
        Action.DOWN to DownPressedEvent::class.java,
        Action.UP to UpPressedEvent::class.java,
        Action.BACKSPACE to BackspaceEvent::class.java,
        Action.COMPLETION_CANCELED to CompletionCancelledEvent::class.java,
        Action.EXPLICIT_SELECT to ExplicitSelectEvent::class.java,
        Action.TYPED_SELECT to TypedSelectEvent::class.java,
        Action.CUSTOM to CustomMessageEvent::class.java
    )


    fun toString(event: LogEvent): String {
        return "${event.timestamp}\t" +
                "${event.recorderId}\t" +
                "${event.recorderVersion}\t" +
                "${event.userUid}\t" +
                "${event.sessionUid}\t" +
                "${event.bucket}\t" +
                "${event.actionType}\t" +
                JsonSerializer.toJson(event)
    }


    fun fromString(line: String): DeserializedLogEvent {
        val parseResult = parseTabSeparatedLine(line, 7) ?: return DeserializedLogEvent(null, emptySet(), emptySet())
        val elements = parseResult.elements
        val endOffset = parseResult.endOffset

        val timestamp = elements[0].toLong()
        val recorderId = elements[1]
        val recorderVersion = elements[2]

        val userUid = elements[3]
        val sessionUid = elements[4]
        val bucket = elements[5]
        val actionType = Action.valueOf(elements[6])

        val clazz = actionClassMap[actionType] ?: return DeserializedLogEvent(null, emptySet(), emptySet())

        val json = line.substring(endOffset + 1)
        val result = JsonSerializer.fromJson(json, clazz)

        val event = result.value

        event.userUid = userUid
        event.timestamp = timestamp
        event.recorderId = recorderId
        event.recorderVersion = recorderVersion
        event.sessionUid = sessionUid
        event.bucket = bucket
        event.actionType = actionType

        return DeserializedLogEvent(event, result.unknownFields, result.absentFields)
    }

    private fun parseTabSeparatedLine(line: String, elementsCount: Int): TabSeparatedParseResult? {
        val items = mutableListOf<String>()
        var start = -1
        return try {
            for (i in 0 until elementsCount) {
                val nextSpace = line.indexOf('\t', start + 1)
                val newItem = line.substring(start + 1, nextSpace)
                items.add(newItem)
                start = nextSpace
            }
            TabSeparatedParseResult(items, start)
        }
        catch (e: Exception) {
            null
        }
    }

}


class TabSeparatedParseResult(val elements: List<String>, val endOffset: Int)


class DeserializedLogEvent(
        val event: LogEvent?,
        val unknownEventFields: Set<String>,
        val absentEventFields: Set<String>
)