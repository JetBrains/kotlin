/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.name.ClassId
import java.io.File
import java.util.zip.ZipInputStream

/** Computes a [ClasspathEntrySnapshot] of a classpath entry (directory or jar). */
@Suppress("SpellCheckingInspection")
object ClasspathEntrySnapshotter {

    private val DEFAULT_CLASS_FILTER = { unixStyleRelativePath: String, isDirectory: Boolean ->
        !isDirectory
                && unixStyleRelativePath.endsWith(".class", ignoreCase = true)
                && !unixStyleRelativePath.endsWith("module-info.class", ignoreCase = true)
                && !unixStyleRelativePath.startsWith("meta-inf", ignoreCase = true)
    }

    fun snapshot(classpathEntry: File): ClasspathEntrySnapshot {
        val classes =
            DirectoryOrJarContentsReader.read(classpathEntry, DEFAULT_CLASS_FILTER)
                .map { (unixStyleRelativePath, contents) ->
                    ClassFileWithContents(ClassFile(classpathEntry, unixStyleRelativePath), contents)
                }

        val snapshots = ClassSnapshotter.snapshot(classes)

        val relativePathsToSnapshotsMap =
            classes.map { it.classFile.unixStyleRelativePath }.zip(snapshots).toMap(LinkedHashMap())
        return ClasspathEntrySnapshot(relativePathsToSnapshotsMap)
    }
}

/** Creates [ClassSnapshot]s of classes. */
@Suppress("SpellCheckingInspection")
object ClassSnapshotter {

    /**
     * Creates [ClassSnapshot]s of the given classes.
     *
     * Note that for Java (non-Kotlin) classes, creating a [ClassSnapshot] for a nested class will require accessing the outer class (and
     * possibly vice versa). Therefore, outer classes and nested classes must be passed together in one invocation of this method.
     */
    fun snapshot(classes: List<ClassFileWithContents>): List<ClassSnapshot> {
        // Snapshot Kotlin classes first
        val kotlinClassSnapshots: Map<ClassFile, KotlinClassSnapshot?> = classes.associate {
            it.classFile to trySnapshotKotlinClass(it)
        }

        // Snapshot Java classes in one invocation
        val javaClasses: List<ClassFileWithContents> = classes.filter { kotlinClassSnapshots[it.classFile] == null }
        val snapshots: List<JavaClassSnapshot> = snapshotJavaClasses(javaClasses)
        val javaClassSnapshots: Map<ClassFile, JavaClassSnapshot> = javaClasses.map { it.classFile }.zip(snapshots).toMap()

        // Return a snapshot for each class
        return classes.map { kotlinClassSnapshots[it.classFile] ?: javaClassSnapshots[it.classFile]!! }
    }

    /** Creates [KotlinClassSnapshot] of the given class, or returns `null` if the class is not a Kotlin class. */
    private fun trySnapshotKotlinClass(clazz: ClassFileWithContents): KotlinClassSnapshot? {
        return KotlinClassInfo.tryCreateFrom(clazz.contents)?.let {
            KotlinClassSnapshot(it)
        }
    }

    /**
     * Creates [JavaClassSnapshot]s of the given Java classes.
     *
     * Note that creating a [JavaClassSnapshot] for a nested class will require accessing the outer class (and possibly vice versa).
     * Therefore, outer classes and nested classes must be passed together in one invocation of this method.
     */
    private fun snapshotJavaClasses(classes: List<ClassFileWithContents>): List<JavaClassSnapshot> {
        val classFiles = classes.map { it.classFile }
        val classesContents = classes.map { it.contents }
        val classNames = classesContents.map { JavaClassName.compute(it) }
        val classIds = computeJavaClassIds(classNames)

        // Snapshot special cases first
        // Map a class index to its snapshot, or `null` if it will be created later
        val specialCaseSnapshots: Map<Int, JavaClassSnapshot?> = classFiles.indices.associateWith { index ->
            val className = classNames[index]
            val classId = classIds[index]
            if (classId.isLocal) {
                // A local class can't be referenced from other source files, so any changes in a local class will not cause recompilation
                // of other source files. Therefore, the snapshot of a local class is empty.
                // In that regard, a nested class of a local class is also considered local (which matches the definition of
                // ClassId.isLocal, see ClassId's kdoc). Therefore, we checked `classId.isLocal`, which is a super set of `className is
                // LocalClass`.
                EmptyJavaClassSnapshot
            } else if (className is NestedNonLocalClass && (className.isAnonymous || className.isSynthetic)) {
                // An anonymous or synthetic class also can't be referenced from other source files, so its snapshot is also empty.
                EmptyJavaClassSnapshot
            } else {
                null
            }
        }

        // Snapshot the remaining classes in one invocation
        val remainingClassesIndices: List<Int> = classFiles.indices.filter { specialCaseSnapshots[it] == null }
        val remainingClassIds: List<ClassId> = remainingClassesIndices.map { classIds[it] }
        val remainingClassesContents: List<ByteArray> = remainingClassesIndices.map { classes[it].contents }

        val snapshots: List<JavaClassSnapshot> = JavaClassDescriptorCreator.create(remainingClassIds, remainingClassesContents).map {
            RegularJavaClassSnapshot(it.toSerializedJavaClass())
        }
        val remainingSnapshots: Map<Int, JavaClassSnapshot> /* maps a class index to its snapshot */ =
            remainingClassesIndices.zip(snapshots).toMap()

        // Return a snapshot for each class
        return classFiles.indices.map { specialCaseSnapshots[it] ?: remainingSnapshots[it]!! }
    }
}

/** Utility to read the contents of a directory or jar. */
private object DirectoryOrJarContentsReader {

    /**
     * Returns a map from Unix-style relative paths of entries to their contents. The paths are relative to the container (directory or
     * jar).
     *
     * The map entries need to satisfy the given filter.
     *
     * The map entries are sorted based on their Unix-style relative paths (to ensure deterministic results across filesystems).
     *
     * Note: If a jar has duplicate entries, only one of them will be used (there is no guarantee which one will be used).
     */
    fun read(
        directoryOrJar: File,
        entryFilter: ((unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean)? = null
    ): LinkedHashMap<String, ByteArray> {
        return if (directoryOrJar.isDirectory) {
            readDirectory(directoryOrJar, entryFilter)
        } else {
            check(directoryOrJar.isFile && directoryOrJar.path.endsWith(".jar", ignoreCase = true))
            readJar(directoryOrJar, entryFilter)
        }
    }

    private fun readDirectory(
        directory: File,
        entryFilter: ((unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean)? = null
    ): LinkedHashMap<String, ByteArray> {
        val relativePathsToContents: MutableList<Pair<String, ByteArray>> = mutableListOf()
        directory.walk().forEach { file ->
            val unixStyleRelativePath = file.relativeTo(directory).invariantSeparatorsPath
            if (entryFilter == null || entryFilter(unixStyleRelativePath, file.isDirectory)) {
                relativePathsToContents.add(unixStyleRelativePath to file.readBytes())
            }
        }
        return relativePathsToContents.sortedBy { it.first }.toMap(LinkedHashMap())
    }

    private fun readJar(
        jarFile: File,
        entryFilter: ((unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean)? = null
    ): LinkedHashMap<String, ByteArray> {
        val relativePathsToContents: MutableList<Pair<String, ByteArray>> = mutableListOf()
        ZipInputStream(jarFile.inputStream().buffered()).use { zipInputStream ->
            while (true) {
                val entry = zipInputStream.nextEntry ?: break
                val unixStyleRelativePath = entry.name
                if (entryFilter == null || entryFilter(unixStyleRelativePath, entry.isDirectory)) {
                    relativePathsToContents.add(unixStyleRelativePath to zipInputStream.readBytes())
                }
            }
        }
        return relativePathsToContents.sortedBy { it.first }.toMap(LinkedHashMap())
    }
}
