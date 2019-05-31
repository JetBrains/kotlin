/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.daemon.common.toHexString
import java.io.File

/**
 * Cache for preventing processing some files twice.
 * Files are precessed some subdirectories of [targetDir] called `target`.
 * For each target [getOrCompute] should be called.
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

    private class State {
        var version: String? = null
        val byHash = mutableMapOf<String, Element>()
        val byTarget = mutableMapOf<String, Element>()

        operator fun get(elementHash: ByteArray) =
            byHash[elementHash.toHexString()]

        operator fun set(elementHash: ByteArray, element: Element) {
            byHash[elementHash.toHexString()] = element
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
            stateFile.reader().use {
                gson.fromJson(it, State::class.java)
            }.let {
                if (it.version != version) {
                    targetDir.deleteRecursively()
                    null
                } else it
            }
        } catch (e: JsonParseException) {
            project.logger.warn("Cannot read $stateFile", e)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            null
        } else null

        old = state ?: State()
    }

    private val new = State()

    init {
        new.version = version
    }

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

        stateFile.writer().use {
            gson.toJson(new, it)
        }
    }
}