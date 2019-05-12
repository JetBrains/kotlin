/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.kotlin.idea.KotlinFileType
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

abstract class LazyScriptDefinitionProvider : ScriptDefinitionProvider {

    protected val lock = ReentrantReadWriteLock()

    protected abstract val currentDefinitions: Sequence<ScriptDefinition>

    protected open fun getScriptingHostConfiguration(): ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration

    override fun getDefaultDefinition(): ScriptDefinition =
        ScriptDefinition.FromLegacy(getScriptingHostConfiguration(), StandardScriptDefinition)

    private var _cachedDefinitions: Sequence<ScriptDefinition>? = null
    private val cachedDefinitions: Sequence<ScriptDefinition>
        get() {
            assert(lock.readLockCount > 0) { "cachedDefinitions should only be used under the read lock" }
            if (_cachedDefinitions == null) lock.write {
                _cachedDefinitions = CachingSequence(currentDefinitions.constrainOnce())
            }
            return _cachedDefinitions!!
        }

    protected fun clearCache() {
        lock.write {
            _cachedDefinitions = null
        }
    }

    protected open fun nonScriptFileName(fileName: String) = nonScriptFilenameSuffixes.any {
        fileName.endsWith(it, ignoreCase = true)
    }

    override fun findDefinition(fileName: String): ScriptDefinition? =
        if (nonScriptFileName(fileName)) null
        else lock.read {
            cachedDefinitions.firstOrNull { it.isScript(fileName) }
        }

    override fun findScriptDefinition(fileName: String): KotlinScriptDefinition? = findDefinition(fileName)?.legacyDefinition

    override fun isScript(fileName: String) = findDefinition(fileName) != null

    override fun getKnownFilenameExtensions(): Sequence<String> = lock.read {
        cachedDefinitions.map { it.fileExtension }
    }

    override fun getDefaultScriptDefinition(): KotlinScriptDefinition = getDefaultDefinition().legacyDefinition

    companion object {
        // TODO: find a common place for storing kotlin-related extensions and reuse values from it everywhere
        protected val nonScriptFilenameSuffixes = arrayOf(".${KotlinFileType.EXTENSION}", ".${JavaFileType.DEFAULT_EXTENSION}")
    }
}

private class CachingSequence<T>(from: Sequence<T>) : Sequence<T> {

    private val lock = ReentrantReadWriteLock()
    private val sequenceIterator = from.iterator()
    private val cache = arrayListOf<T>()

    private inner class CachingIterator : Iterator<T> {

        private var cacheCursor = 0

        override fun hasNext(): Boolean =
            lock.read { cacheCursor < cache.size }
                    // iterator's hasNext can mutate the iterator's state, therefore write lock is needed
                    || lock.write { cacheCursor < cache.size || sequenceIterator.hasNext() }

        override fun next(): T {
            lock.read {
                if (cacheCursor < cache.size) return cache[cacheCursor++]
            }
            // lock.write is not an upgrade but retake, therefore - one more check needed
            lock.write {
                return if (cacheCursor < cache.size) cache[cacheCursor++]
                else sequenceIterator.next().also { cache.add(it) }
            }
        }
    }

    override fun iterator(): Iterator<T> = CachingIterator()
}
