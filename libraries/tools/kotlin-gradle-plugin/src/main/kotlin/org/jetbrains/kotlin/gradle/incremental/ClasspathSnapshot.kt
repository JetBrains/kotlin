/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import java.io.*

/** Snapshot of a classpath. It consists of a list of [ClasspathEntrySnapshot]s. */
class ClasspathSnapshot(val classpathEntrySnapshots: List<ClasspathEntrySnapshot>)

/** Snapshot of a classpath entry (directory or jar). It consists of a list of [ClassSnapshot]s. */
class ClasspathEntrySnapshot(

    /**
     * Maps (Unix-like) relative paths of classes to their snapshots. The paths are relative to the containing classpath entry (directory or
     * jar).
     */
    val classSnapshots: LinkedHashMap<String, ClassSnapshot>
) : Serializable {

    companion object {
        private const val serialVersionUID = 0L
    }
}

/**
 * Snapshot of a class. It contains information to compute the source files that need to be recompiled during an incremental run of the
 * `KotlinCompile` task.
 */
class ClassSnapshot : Serializable {

    // TODO WORK-IN-PROGRESS

    companion object {
        private const val serialVersionUID = 0L
    }
}

/** Utility to read/write a [ClasspathSnapshot] from/to a file. */
object ClasspathSnapshotSerializer {

    fun readFromFiles(classpathEntrySnapshotFiles: List<File>): ClasspathSnapshot {
        return ClasspathSnapshot(classpathEntrySnapshotFiles.map { ClasspathEntrySnapshotSerializer.readFromFile(it) })
    }
}

/** Utility to read/write a [ClasspathEntrySnapshot] from/to a file. */
object ClasspathEntrySnapshotSerializer {

    fun readFromFile(classpathEntrySnapshotFile: File): ClasspathEntrySnapshot {
        check(classpathEntrySnapshotFile.isFile) { "`${classpathEntrySnapshotFile.path}` does not exist (or is a directory)." }
        return ObjectInputStream(FileInputStream(classpathEntrySnapshotFile).buffered()).use {
            it.readObject() as ClasspathEntrySnapshot
        }
    }

    fun writeToFile(classpathEntrySnapshot: ClasspathEntrySnapshot, classpathEntrySnapshotFile: File) {
        check(classpathEntrySnapshotFile.parentFile.exists()) { "Parent dir of `${classpathEntrySnapshotFile.path}` does not exist." }
        ObjectOutputStream(FileOutputStream(classpathEntrySnapshotFile).buffered()).use {
            it.writeObject(classpathEntrySnapshot)
        }
    }
}
