/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.SerializedJavaClass

/** Snapshot of a classpath. It consists of a list of [ClasspathEntrySnapshot]s. */
class ClasspathSnapshot(val classpathEntrySnapshots: List<ClasspathEntrySnapshot>)

/**
 * Snapshot of a classpath entry (directory or jar). It consists of a list of [ClassSnapshot]s.
 *
 * NOTE: It's important that the path to the classpath entry is not part of this snapshot. The reason is that classpath entries produced by
 * different builds or on different machines but having the same contents should be considered the same for better build performance.
 */
class ClasspathEntrySnapshot(

    /**
     * Maps (Unix-style) relative paths of classes to their snapshots. The paths are relative to the containing classpath entry (directory
     * or jar).
     */
    val classSnapshots: LinkedHashMap<String, ClassSnapshot>
)

/**
 * Snapshot of a class. It contains minimal information about a class to compute the source files that need to be recompiled during an
 * incremental run of the `KotlinCompile` task.
 *
 * It's important that this class contain only the minimal required information, as it will be part of the classpath snapshot of the
 * `KotlinCompile` task and the task needs to support compile avoidance. For example, this class should contain public method signatures,
 * and should not contain private method signatures, or method implementations.
 */
sealed class ClassSnapshot

/** [ClassSnapshot] of a Kotlin class. */
class KotlinClassSnapshot(val classInfo: KotlinClassInfo) : ClassSnapshot()

/** [ClassSnapshot] of a Java class. */
class JavaClassSnapshot(

    /** The [SerializedJavaClass], or `null` if it can't be computed (in which case [contentHash] is used instead). */
    val serializedJavaClass: SerializedJavaClass?,

    /** The hash of the class contents in case [serializedJavaClass] can't be computed, or `null` if [serializedJavaClass] is not-null. */
    val contentHash: ByteArray?
) : ClassSnapshot() {
    init {
        check((serializedJavaClass != null) xor (contentHash != null))
    }
}
