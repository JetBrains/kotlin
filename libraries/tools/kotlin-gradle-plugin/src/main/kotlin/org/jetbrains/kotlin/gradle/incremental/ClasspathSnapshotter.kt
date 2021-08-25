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

        val relativePathsToSnapshotsMap = classes.map { it.classFile.unixStyleRelativePath }.zipToMap(snapshots)
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
        val kotlinClassSnapshots: Map<ClassFileWithContents, KotlinClassSnapshot?> = classes.associateWith {
            trySnapshotKotlinClass(it)
        }

        // Snapshot the remaining Java classes in one invocation
        val javaClasses: List<ClassFileWithContents> = classes.filter { kotlinClassSnapshots[it] == null }
        val snapshots: List<JavaClassSnapshot> = snapshotJavaClasses(javaClasses)
        val javaClassSnapshots: Map<ClassFileWithContents, JavaClassSnapshot> = javaClasses.zipToMap(snapshots)

        return classes.map { kotlinClassSnapshots[it] ?: javaClassSnapshots[it]!! }
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
        val classNames: List<JavaClassName> = classes.map { JavaClassName.compute(it.contents) }
        val classNameToClassFile: LinkedHashMap<JavaClassName, ClassFileWithContents> = classNames.zipToMap(classes)

        // We divide classes into 2 categories:
        //   - Special classes, which includes local, anonymous, or synthetic classes, and their nested classes. These classes can't be
        //     referenced from other source files, so any changes in these classes will not cause recompilation of other source files.
        //     Therefore, the snapshots of these classes are empty.
        //   - Regular classes: Any classes that do not belong to the above category.
        val specialClasses = getSpecialClasses(classNames).toSet()

        // Snapshot special classes first
        val specialClassSnapshots: Map<JavaClassName, JavaClassSnapshot?> = classNames.associateWith {
            if (it in specialClasses) {
                EmptyJavaClassSnapshot
            } else null
        }

        // Snapshot the remaining regular classes in one invocation
        val regularClasses: List<JavaClassName> = classNames.filter { specialClassSnapshots[it] == null }
        val regularClassIds: List<ClassId> = computeJavaClassIds(regularClasses)
        val regularClassesContents: List<ByteArray> = regularClasses.map { classNameToClassFile[it]!!.contents }

        val snapshots: List<RegularJavaClassSnapshot> = JavaClassDescriptorCreator.create(regularClassIds, regularClassesContents).map {
            RegularJavaClassSnapshot(it.toSerializedJavaClass())
        }
        val regularClassSnapshots: LinkedHashMap<JavaClassName, JavaClassSnapshot> = regularClasses.zipToMap(snapshots)

        return classNames.map { specialClassSnapshots[it] ?: regularClassSnapshots[it]!! }
    }

    /** Returns local, anonymous, or synthetic classes, and their nested classes. */
    private fun getSpecialClasses(classNames: List<JavaClassName>): List<JavaClassName> {
        val specialClasses: MutableMap<JavaClassName, Boolean> = HashMap(classNames.size)
        val nameToClassName: Map<String, JavaClassName> = classNames.associateBy { it.name }

        fun JavaClassName.isSpecial(): Boolean {
            specialClasses[this]?.let { return it }

            return if (isAnonymous || isSynthetic) {
                true
            } else when (this) {
                is TopLevelClass -> false
                is NestedNonLocalClass -> nameToClassName[outerName]?.isSpecial() ?: error("Class name not found: $outerName")
                is LocalClass -> true
            }.also {
                specialClasses[this] = it
            }
        }

        return classNames.filter { it.isSpecial() }
    }
}

/** Utility to read the contents of a directory or jar. */
private object DirectoryOrJarContentsReader {

    /**
     * Returns a map from Unix-style relative paths of entries to their contents. The paths are relative to the given container (directory
     * or jar).
     *
     * The map entries need to satisfy the given filter.
     *
     * The map entries are sorted based on their Unix-style relative paths (to ensure deterministic results across filesystems).
     *
     * Note: If a jar has duplicate entries, only one of them will be used (there is no guarantee which one will be used, but the selection
     * will be deterministic).
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

/**
 * Combines two lists of the same size into a map.
 *
 * This method is more efficient than calling `[Iterable.zip].toMap()` as it doesn't create short-lived intermediate [Pair]s as done by
 * [Iterable.zip].
 */
private fun <K, V> List<K>.zipToMap(other: List<V>): LinkedHashMap<K, V> {
    check(this.size == other.size)
    val map = LinkedHashMap<K, V>(size)
    indices.forEach { index ->
        map[this[index]] = other[index]
    }
    return map
}
