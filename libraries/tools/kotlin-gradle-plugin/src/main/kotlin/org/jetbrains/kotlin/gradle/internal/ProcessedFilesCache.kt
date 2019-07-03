/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.daemon.common.toHexString
import java.io.File

/**
 * Cache for preventing processing some files twice.
 * Files are precessed to some subdirectories of [targetDir] called `target`.
 * [getOrCompute] returns path to directory with files produced for given input file.
 * Absent target directories will be deleted on [close].
 * `compute` will be called only for source files that was changed since last processing.
 *
 * See [org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModuleBuilder] for example.
 *
 * @param version When updating logic in `compute`, `version` should be increased to invalidate cache
 */
internal open class ProcessedFilesCache(
    val project: Project,
    val targetDir: File,
    stateFileName: String,
    val version: String
) : AutoCloseable {
    private val hasher = (project as ProjectInternal).services.get(FileHasher::class.java)
    private val gson = GsonBuilder().setPrettyPrinting().create()

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

    fun ByteArray.toHex(): String {
        val result = CharArray(size * 2) { ' ' }
        var i = 0
        forEach {
            val n = it.toInt()
            result[i++] = Character.forDigit(n shr 4 and 0xF, 16)
            result[i++] = Character.forDigit(n and 0xF, 16)
        }
        return String(result)
    }

    private fun decodeHexString(hexString: String): ByteArray {
        check(hexString.length % 2 == 0)
        val bytes = ByteArray(hexString.length / 2)
        var i = 0
        var o = 0
        while (i < hexString.length) {
            bytes[o++] = hexToByte(hexString[i++], hexString[i++])
        }
        return bytes
    }

    private fun hexToByte(a: Char, b: Char): Byte = ((a.toDigit() shl 4) + b.toDigit()).toByte()

    private fun Char.toDigit(): Int = Character.digit(this, 16).also { check(it != -1) }

    private class State {
        val byHash = mutableMapOf<ByteArray, Element>()
        val byTarget = mutableMapOf<String, Element>()

        operator fun get(elementHash: ByteArray) = byHash[elementHash]

        operator fun set(elementHash: ByteArray, element: Element) {
            elementHash.toHexString()
            byHash[elementHash] = element
            val target = element.target
            if (target != null) {
                byTarget[target] = element
            }
        }
    }

    class Element(
        val src: String,
        val target: String?
    )

    private val stateFile = targetDir.resolve(stateFileName)
    private val old: State

    init {
        targetDir.mkdirs()

        val state: State? = if (stateFile.exists()) try {
            gson.newJsonReader(stateFile.reader()).use {
                readFrom(it)
            }
        } catch (e: Throwable) {
            project.logger.warn("Cannot read $stateFile", e)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            null
        } else null

        old = state ?: State()
    }

    private val new = State()

    internal fun getOrCompute(
        file: File,
        compute: () -> String?
    ): String? {
        val hash = hasher.hash(file).toByteArray()
        val old = old[hash]
        if (old != null) {
            new[hash] = old
            return old.target
        } else {
            val key = compute()
            val existedTarget = new.byTarget[key]
            if (key != null && existedTarget != null) {
                if (existedTarget.src != file.canonicalPath) {
                    reportTargetClash(key, file, File(existedTarget.src))
                }
            }
            new[hash] = Element(file.canonicalPath, key)
            return key
        }
    }

    fun deleteTarget(key: String) {
        targetDir.resolve(key).deleteRecursively()
    }

    val targets: Collection<String>
        get() = new.byHash.mapNotNullTo(mutableSetOf()) { it.value.target }

    protected open fun reportTargetClash(target: String, existedSrc: File, newSrc: File): Nothing =
        error("Both `$existedSrc` and `$newSrc` produces `$target`")

    private fun getDeletedTargets(): MutableSet<String> {
        val result = mutableSetOf<String>()

        old.byHash.forEach {
            val target = it.value.target
            if (target != null) {
                result.add(target)
            }
        }

        new.byHash.forEach {
            val target = it.value.target
            if (target != null) {
                result.remove(target)
            }
        }
        return result
    }

    override fun close() {
        getDeletedTargets().forEach {
            deleteTarget(it)
        }

        stateFile.parentFile.mkdirs()
        gson.newJsonWriter(stateFile.writer()).use {
            new.writeTo(it)
        }
    }
}