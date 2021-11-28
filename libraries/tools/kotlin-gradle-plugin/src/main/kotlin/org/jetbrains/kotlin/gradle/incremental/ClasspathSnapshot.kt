/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.SerializedJavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

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
class KotlinClassSnapshot(
    val classInfo: KotlinClassInfo,
    val supertypes: List<JvmClassName>
) : ClassSnapshot()

/** [ClassSnapshot] of a Java class. */
sealed class JavaClassSnapshot : ClassSnapshot()

/** [JavaClassSnapshot] of a typical Java class. */
class RegularJavaClassSnapshot(

    /** [ClassId] of the class. It is part of the class's ABI ([classAbiExcludingMembers]). */
    val classId: ClassId,

    /** The superclass and interfaces of the class. It is part of the class's ABI ([classAbiExcludingMembers]). */
    val supertypes: List<JvmClassName>,

    /** [AbiSnapshot] of the class excluding its fields and methods. */
    val classAbiExcludingMembers: AbiSnapshot,

    /** [AbiSnapshot]s of the class's fields. */
    val fieldsAbi: List<AbiSnapshot>,

    /** [AbiSnapshot]s of the class's methods. */
    val methodsAbi: List<AbiSnapshot>

) : JavaClassSnapshot() {

    val className by lazy {
        JvmClassName.byClassId(classId).also {
            check(it == JvmClassName.byInternalName(classAbiExcludingMembers.name))
        }
    }
}

/** The ABI snapshot of a Java element (e.g., class, field, or method). */
open class AbiSnapshot(

    /** The name of the Java element. It is part of the Java element's ABI. */
    val name: String,

    /** The hash of the Java element's ABI. */
    val abiHash: Long
)

/** TEST-ONLY: An [AbiSnapshot] that is used for testing only and must not be used in production code. */
class AbiSnapshotForTests(
    name: String,
    abiHash: Long,

    /** The Java element's ABI, captured in a [String]. */
    @Suppress("unused") val abiValue: String

) : AbiSnapshot(name, abiHash)

/** [JavaClassSnapshot] of a typical Java class which uses protos internally. */
class ProtoBasedJavaClassSnapshot(val serializedJavaClass: SerializedJavaClass) : JavaClassSnapshot()

/**
 * [JavaClassSnapshot] of a Java class where there is nothing to capture.
 *
 * For example, the snapshot of a local class is empty as a local class can't be referenced from other source files and therefore any
 * changes in a local class will not cause recompilation of other source files.
 */
object EmptyJavaClassSnapshot : JavaClassSnapshot()

/**
 * [JavaClassSnapshot] of a Java class where a proper snapshot can't be created for some reason, so we use the hash of the class contents as
 * the snapshot instead, so that at least it's still correct when used as an input of the `KotlinCompile` task (when the class contents have
 * changed, this snapshot will also change).
 */
class ContentHashJavaClassSnapshot(val contentHash: Long) : JavaClassSnapshot()
