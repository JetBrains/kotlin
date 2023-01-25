/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.tooling.core.WeakInterner
import java.io.File
import java.io.Serializable

fun IdeaKotlinClasspath(files: Iterable<File>): IdeaKotlinClasspath = IdeaKotlinClasspath.from(files)

fun IdeaKotlinClasspath(vararg files: File): IdeaKotlinClasspath = IdeaKotlinClasspath.from(files.toList())

fun IdeaKotlinClasspath(): IdeaKotlinClasspath = IdeaKotlinClasspath.empty()

@IdeaKotlinModel
class IdeaKotlinClasspath private constructor(private val files: MutableSet<File> = mutableSetOf()) : MutableSet<File>, Serializable {
    override val size: Int
        get() = files.size

    override fun add(element: File): Boolean {
        return files.add(normalise(element))
    }

    fun addAll(classpath: IdeaKotlinClasspath): Boolean {
        return files.addAll(classpath.files)
    }

    override fun addAll(elements: Collection<File>): Boolean {
        return files.addAll(elements.map(::normalise))
    }

    override fun clear() {
        files.clear()
    }

    override fun contains(element: File): Boolean {
        return files.contains(normalise(element))
    }

    override fun containsAll(elements: Collection<File>): Boolean {
        return files.containsAll(elements.map(::normalise).toSet())
    }

    override fun isEmpty(): Boolean {
        return files.isEmpty()
    }

    override fun iterator(): MutableIterator<File> {
        return files.iterator()
    }

    override fun remove(element: File): Boolean {
        return files.remove(normalise(element))
    }

    override fun removeAll(elements: Collection<File>): Boolean {
        return files.removeAll(elements.map(::normalise).toSet())
    }

    override fun retainAll(elements: Collection<File>): Boolean {
        return files.retainAll(elements.map(::normalise).toSet())
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IdeaKotlinClasspath) return false
        return files == other.files
    }

    override fun hashCode(): Int {
        return files.hashCode()
    }

    override fun toString(): String {
        return "IdeaKotlinClasspath($files)"
    }

    internal companion object {
        private const val serialVersionUID = 0L

        private val interner = WeakInterner()

        fun normalise(file: File): File {
            val normalized = file.absoluteFile.normalize()
            return interner.getOrPut(normalized)
        }

        fun from(files: Iterable<File>): IdeaKotlinClasspath {
            return IdeaKotlinClasspath(files.map(::normalise).toMutableSet())
        }

        fun from(file: File): IdeaKotlinClasspath {
            return IdeaKotlinClasspath(mutableSetOf(normalise(file)))
        }

        fun empty() = IdeaKotlinClasspath(mutableSetOf())
    }
}
