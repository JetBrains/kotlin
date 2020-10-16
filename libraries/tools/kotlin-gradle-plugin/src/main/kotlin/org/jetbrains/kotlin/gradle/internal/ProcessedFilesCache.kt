/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Cache for preventing processing some files twice.
 * Files are precessed to some subdirectories of [targetDir] called `target`.
 * [getOrCompute] returns path to directory with files produced for given input file.
 * `compute` will be called only for source files that was changed since last processing.
 *
 * See [org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModuleBuilder] for example.
 *
 * @param version When updating logic in `compute`, `version` should be increased to invalidate cache
 */
// CHECK
internal open class ProcessedFilesCache(
    val logger: Logger,
//    val hasher: FileHasher,
    val projectDir: File,
    val targetDir: File,
    stateFileName: String,
    val version: String
) : AutoCloseable {

    private fun readFrom(json: JsonReader): State? {
        val result = State()

        json.obj {
            check(json.nextName() == "version")
            val version = json.nextString()
            if (version != this.version) return null

            check(json.nextName() == "items")
            json.obj {
                while (json.peek() == JsonToken.NAME) {
                    val key = json.nextName()
                    json.beginObject()
                    check(json.nextName() == "src")
                    val src = json.nextString()

                    var target: String? = null
                    if (json.peek() == JsonToken.NAME) {
                        check(json.nextName() == "target")
                        if (json.peek() != JsonToken.NULL) {
                            target = json.nextString()
                        }
                    }
                    json.endObject()

                    result[decodeHexString(key)] = Element(src, target)
                }
            }
        }

        return result
    }

    private fun State.writeTo(json: JsonWriter) {
        json.obj {
            json.name("version").value(version)
            json.name("items")
            json.obj {
                byHash.forEach {
                    json.name(it.key.toHex())
                    json.obj {
                        json.name("src").value(it.value.src)
                        json.name("target")
                        if (it.value.target == null) json.nullValue() else json.value(it.value.target)
                    }
                }
            }

        }
    }

    private inline fun JsonReader.obj(body: () -> Unit) {
        beginObject()
        body()
        endObject()
    }

    private inline fun JsonWriter.obj(body: () -> Unit) {
        beginObject()
        body()
        endObject()
    }

    fun Int.toHex(): String {
        return toString(16)
    }

    private fun decodeHexString(hexString: String): Int {
        return Integer.parseInt(hexString, 16)
    }

    private fun hexToByte(a: Char, b: Char): Byte = ((a.toDigit() shl 4) + b.toDigit()).toByte()

    private fun Char.toDigit(): Int = Character.digit(this, 16).also { check(it != -1) }

    class ByteArrayWrapper(val contents: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ByteArrayWrapper

            if (!contents.contentEquals(other.contents)) return false

            return true
        }

        override fun hashCode(): Int {
            return contents.contentHashCode()
        }
    }

    private class State {
        val byHash = mutableMapOf<Int, Element>()
        val byTarget = mutableMapOf<String, Element>()

        operator fun get(elementHash: Int) = byHash[elementHash]

        operator fun set(elementHash: Int, element: Element) {
            byHash[elementHash] = element
            val target = element.target
            if (target != null) {
                byTarget[target] = element
            }
        }

        fun remove(element: Element) {
            if (element.target != null) {
                byTarget.remove(element.target)
            }

            byHash.values.removeIf { it == element }
        }
    }

    data class Element(
        val src: String,
        val target: String?
    )

    private val stateFile = targetDir.resolve(stateFileName)
    private val state: State

    init {
        targetDir.mkdirs()

        state = (if (stateFile.exists()) {
            try {
                GsonBuilder().setPrettyPrinting().create().newJsonReader(stateFile.reader()).use { readFrom(it) }
            } catch (e: Throwable) {
                logger.warn("Cannot read $stateFile", e)
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }
                null
            }
        } else null) ?: State()
    }

    internal fun getOrCompute(
        file: File,
        compute: () -> File?
    ): File? = getOrComputeKey(file, compute)?.let { File(targetDir, it) }

    private fun getOrComputeKey(
        file: File,
        compute: () -> File?
    ): String? {
        val hash = file.hashCode()
        val old = state[hash]

        if (old != null) {
            if (checkTarget(old.target)) return old.target
            else logger.warn("Cannot find ${File(targetDir.relativeTo(projectDir), old.target!!)}, rebuilding")
        }

        val key = compute()?.relativeTo(targetDir)?.toString()
        val existedTarget = state.byTarget[key]
        if (key != null && existedTarget != null) {
            if (!File(existedTarget.src).exists()) {
                logger.warn("Removing cache for removed source `${existedTarget.src}`")
                state.remove(existedTarget)
            }
        }
        state[hash] = Element(file.canonicalPath, key)

        return key
    }

    private fun checkTarget(target: String?): Boolean {
        if (target == null) return true
        return targetDir.resolve(target).exists()
    }

    override fun close() {
        stateFile.parentFile.mkdirs()
        GsonBuilder().setPrettyPrinting().create().newJsonWriter(stateFile.writer()).use {
            state.writeTo(it)
        }
    }
}