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
sealed class JavaClassSnapshot : ClassSnapshot()

/** [JavaClassSnapshot] of a typical Java class. */
class PlainJavaClassSnapshot(
    val serializedJavaClass: SerializedJavaClass
) : JavaClassSnapshot()

/**
 * [JavaClassSnapshot] of a Java class where there is nothing to capture (e.g., the snapshot of a local class is empty as a local class
 * can't be compiled against and any changes in a local class will not cause recompilation of other classes).
 */
object EmptyJavaClassSnapshot : JavaClassSnapshot()

/** [JavaClassSnapshot] of a Java class where a [PlainJavaClassSnapshot] can't be computed. */
class FallBackJavaClassSnapshot(
    /** The hash of the class contents. */
    val contentHash: ByteArray
) : JavaClassSnapshot()
