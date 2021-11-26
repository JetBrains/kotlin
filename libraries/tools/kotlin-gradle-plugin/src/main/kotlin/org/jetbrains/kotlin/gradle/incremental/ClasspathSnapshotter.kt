/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.supertypes
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File
import java.util.zip.ZipInputStream

/** Computes a [ClasspathEntrySnapshot] of a classpath entry (directory or jar). */
object ClasspathEntrySnapshotter {

    private val DEFAULT_CLASS_FILTER = { unixStyleRelativePath: String, isDirectory: Boolean ->
        !isDirectory
                && unixStyleRelativePath.endsWith(".class", ignoreCase = true)
                && !unixStyleRelativePath.endsWith("module-info.class", ignoreCase = true)
                && !unixStyleRelativePath.startsWith("meta-inf", ignoreCase = true)
    }

    fun snapshot(classpathEntry: File, protoBased: Boolean? = null): ClasspathEntrySnapshot {
        val classes =
            DirectoryOrJarContentsReader.read(classpathEntry, DEFAULT_CLASS_FILTER)
                .map { (unixStyleRelativePath, contents) ->
                    ClassFileWithContents(ClassFile(classpathEntry, unixStyleRelativePath), contents)
                }

        val snapshots = try {
            ClassSnapshotter.snapshot(classes, protoBased)
        } catch (e: Throwable) {
            if (isKnownProblematicClasspathEntry(classpathEntry)) {
                classes.map { ContentHashJavaClassSnapshot(it.contents.md5()) }
            } else throw e
        }

        val relativePathsToSnapshotsMap = classes.map { it.classFile.unixStyleRelativePath }.zipToMap(snapshots)
        return ClasspathEntrySnapshot(relativePathsToSnapshotsMap)
    }

    /** Returns `true` if it is known that the snapshot of the given classpath entry can't be created for some reason. */
    private fun isKnownProblematicClasspathEntry(classpathEntry: File): Boolean {
        if (classpathEntry.name.startsWith("tools-jar-api")) {
            // [FAULTY JAR] kotlin/dependencies/tools-jar-api/build/libs/tools-jar-api-1.6.255-SNAPSHOT.jar contains class
            // com/sun/tools/javac/comp/Infer$GraphStrategy$NodeNotFoundException, but doesn't contain its outer class
            // com/sun/tools/javac/comp/Infer$GraphStrategy.
            // This happens with a few other similar classes in this jar.
            // Therefore, this is a faulty jar, and our snapshotting logic cannot process it.
            return true
        }
        if (classpathEntry.name.startsWith("platform-impl")) {
            // ~/.gradle/kotlin-build-dependencies/repo/kotlin.build/ideaIC/203.8084.24/artifacts/lib/platform-impl.jar contains class
            // com/intellij/application/options/codeStyle/OptionTableWithPreviewPanel.IntOption. When processing that class,
            // BinaryJavaAnnotation$Companion.computeTargetType$resolution_common_jvm in Annotations.kt requires the targetType to be
            // JavaClassifierType, but the actual type is PlainJavaPrimitiveType.
            // TODO: It's likely that this requirement is incorrect, but let's fix it later.
            return true
        }
        return false
    }
}

/** Creates [ClassSnapshot]s of classes. */
object ClassSnapshotter {

    /**
     * Creates [ClassSnapshot]s of the given classes.
     *
     * Note that for Java (non-Kotlin) classes, creating a [ClassSnapshot] for a nested class will require accessing the outer class (and
     * possibly vice versa). Therefore, outer classes and nested classes must be passed together in one invocation of this method.
     */
    fun snapshot(
        classes: List<ClassFileWithContents>,
        protoBased: Boolean? = null,
        includeDebugInfoInSnapshot: Boolean? = null
    ): List<ClassSnapshot> {
        // Snapshot Kotlin classes first
        val kotlinClassSnapshots: Map<ClassFileWithContents, KotlinClassSnapshot?> = classes.associateWith {
            trySnapshotKotlinClass(it)
        }

        // Snapshot the remaining Java classes in one invocation
        val javaClasses: List<ClassFileWithContents> = classes.filter { kotlinClassSnapshots[it] == null }
        val snapshots: List<JavaClassSnapshot> = snapshotJavaClasses(javaClasses, protoBased, includeDebugInfoInSnapshot)
        val javaClassSnapshots: Map<ClassFileWithContents, JavaClassSnapshot> = javaClasses.zipToMap(snapshots)

        return classes.map { kotlinClassSnapshots[it] ?: javaClassSnapshots[it]!! }
    }

    /** Creates [KotlinClassSnapshot] of the given class, or returns `null` if the class is not a Kotlin class. */
    private fun trySnapshotKotlinClass(clazz: ClassFileWithContents): KotlinClassSnapshot? {
        return KotlinClassInfo.tryCreateFrom(clazz.contents)?.let {
            KotlinClassSnapshot(it, computeSupertypes(it, clazz.contents))
        }
    }

    // TODO: Find a faster way to get supertypes without loading protos (e.g., attach to an existing ASM visitor)
    private fun computeSupertypes(classInfo: KotlinClassInfo, classContents: ByteArray): List<JvmClassName> {
        return try {
            val (nameResolver, proto) = JvmProtoBufUtil.readClassDataFrom(classInfo.classHeaderData, classInfo.classHeaderStrings)
            val supertypeClassIds = proto.supertypes(TypeTable(proto.typeTable)).map { nameResolver.getClassId(it.className) }
            supertypeClassIds.map { JvmClassName.byClassId(it) }
        } catch (e: Exception) {
            // The above method call currently fails on a few classes for some reason:
            //   - org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException: Message was missing required fields.
            //     (Lite runtime could not determine which fields were missing) for SomeClassKt.class
            //   - java.lang.NullPointerException: parseDelimitedFrom(this, EXTENSION_REGISTRY) must not be null for
            //     kotlin-stdlib-1.6.255-SNAPSHOT.jar
            // Fall back to ASM visitor to get the supertypes.
            val supertypeClassNames = mutableListOf<String>()
            ClassReader(classContents).accept(object : ClassVisitor(Opcodes.API_VERSION) {
                override fun visit(
                    version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String>?
                ) {
                    superName?.let { supertypeClassNames.add(it) }
                    interfaces?.let { supertypeClassNames.addAll(it) }
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG)
            supertypeClassNames.map { JvmClassName.byInternalName(it) }
        }
    }

    /**
     * Creates [JavaClassSnapshot]s of the given Java classes.
     *
     * Note that creating a [JavaClassSnapshot] for a nested class will require accessing the outer class (and possibly vice versa).
     * Therefore, outer classes and nested classes must be passed together in one invocation of this method.
     */
    private fun snapshotJavaClasses(
        classes: List<ClassFileWithContents>,
        protoBased: Boolean? = null,
        includeDebugInfoInSnapshot: Boolean? = null
    ): List<JavaClassSnapshot> {
        val classNames: List<JavaClassName> = classes.map { JavaClassName.compute(it.contents) }
        val classNameToClassFile: Map<JavaClassName, ClassFileWithContents> = classNames.zipToMap(classes)

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

        // Snapshot the remaining regular classes
        val regularClasses: List<JavaClassName> = classNames.filter { specialClassSnapshots[it] == null }
        val regularClassIds = computeJavaClassIds(regularClasses)
        val regularClassFiles: List<ClassFileWithContents> = regularClasses.map { classNameToClassFile[it]!! }

        val snapshots: List<JavaClassSnapshot> = if (protoBased ?: protoBasedDefaultValue) {
            snapshotJavaClassesProtoBased(regularClassIds, regularClassFiles)
        } else {
            regularClassIds.mapIndexed { index, classId ->
                JavaClassSnapshotter.snapshot(classId, regularClassFiles[index].contents, includeDebugInfoInSnapshot)
            }
        }
        val regularClassSnapshots: Map<JavaClassName, JavaClassSnapshot> = regularClasses.zipToMap(snapshots)

        return classNames.map { specialClassSnapshots[it] ?: regularClassSnapshots[it]!! }
    }

    private fun snapshotJavaClassesProtoBased(
        classIds: List<ClassId>,
        classFilesWithContents: List<ClassFileWithContents>
    ): List<JavaClassSnapshot> {
        val classesContents = classFilesWithContents.map { it.contents }
        val descriptors: List<JavaClassDescriptor?> = JavaClassDescriptorCreator.create(classIds, classesContents)
        val snapshots: List<JavaClassSnapshot> = descriptors.mapIndexed { index, descriptor ->
            val classFileWithContents = classFilesWithContents[index]
            if (descriptor != null) {
                try {
                    ProtoBasedJavaClassSnapshot(descriptor.toSerializedJavaClass())
                } catch (e: Throwable) {
                    if (isKnownExceptionWhenReadingDescriptor(e)) {
                        ContentHashJavaClassSnapshot(classFileWithContents.contents.md5())
                    } else throw e
                }
            } else {
                if (isKnownProblematicClass(classFileWithContents.classFile)) {
                    ContentHashJavaClassSnapshot(classFileWithContents.contents.md5())
                } else {
                    error(
                        "Failed to create JavaClassDescriptor for class '${classFileWithContents.classFile.unixStyleRelativePath}'" +
                                " in '${classFileWithContents.classFile.classRoot.path}'"
                    )
                }
            }
        }
        return snapshots
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
                is NestedNonLocalClass -> {
                    nameToClassName[outerName]?.isSpecial() ?: error("Can't find outer class '$outerName' of class '$name'")
                }
                is LocalClass -> true
            }.also {
                specialClasses[this] = it
            }
        }

        return classNames.filter { it.isSpecial() }
    }

    /** Returns `true` if it is known that the given exception can be thrown when calling [JavaClassDescriptor.toSerializedJavaClass]. */
    private fun isKnownExceptionWhenReadingDescriptor(throwable: Throwable): Boolean {
        // When building the Kotlin repo with `./gradlew publish -Pbootstrap.local=true
        // -Pbootstrap.local.path=/path/to/kotlin/build/repo -Pkotlin.incremental.useClasspathSnapshot=true`, the build can fail with:
        //   org.gradle.api.internal.artifacts.transform.TransformException: Execution failed for ClasspathEntrySnapshotTransform: ~/.gradle/wrapper/dists/gradle-6.9-bin/2ecsmyp3bolyybemj56vfn4mt/gradle-6.9/lib/kotlin-reflect-1.4.20.jar
        //   Caused by: java.lang.IncompatibleClassChangeError: Expected static method 'java.lang.Object org.jetbrains.kotlin.utils.DFS.dfsFromNode(java.lang.Object, org.jetbrains.kotlin.utils.DFS$Neighbors, org.jetbrains.kotlin.utils.DFS$Visited, org.jetbrains.kotlin.utils.DFS$NodeHandler)'
        //     at org.jetbrains.kotlin.builtins.FunctionTypesKt.isTypeOrSubtypeOf(functionTypes.kt:31)
        //     (... at JavaClassDescriptor.toSerializedJavaClass)
        // The reason is that:
        //   - org/jetbrains/kotlin/builtins/FunctionTypesKt.class is located in ~/.gradle/caches/jars-8/66425fb82fd14126e9aa07dcd3100b42/kotlin-compiler-embeddable-1.6.255-20210909.213620-55.jar
        //   - org/jetbrains/kotlin/utils/DFS.class is located in ~/.gradle/caches/jars-8/c716e2e2d26b16f6f1462e59ba44cf3b/buildSrc.jar
        // And somehow the two classes are incompatible (probably similar to the NoSuchMethodError documented at JavaClassDescriptorCreatorKt.createBinaryJavaClass).
        // This happens to a few other jars inside gradle-6.9/lib.
        // However, outside the Kotlin repo build, we don't have this issue (org/jetbrains/kotlin/builtins/FunctionTypesKt.class and
        // org/jetbrains/kotlin/utils/DFS.class will be located in the same kotlin-compiler-embeddable.jar).
        // Therefore, we special-case the Kotlin repo build below.
        // TODO: See how we can address this issue.
        return (throwable is IncompatibleClassChangeError &&
                DFS::class.java.classLoader.getResource(DFS::class.java.name.replace('.', '/') + ".class")
                    ?.path?.contains("buildSrc.jar") == true
                )
    }

    /** Returns `true` if it is known that the snapshot of the given class can't be created for some reason. */
    private fun isKnownProblematicClass(classFile: ClassFile): Boolean {
        if (classFile.classRoot.name.startsWith("groovy")
            && classFile.unixStyleRelativePath.endsWith("\$CollectorHelper.class")
        ) {
            // [FAULTY JAR] In groovy-all-1.3-2.5.12.jar and groovy-2.5.11.jar, the bytecode of
            // groovy/cli/OptionField\$CollectorHelper.class indicates that its outer class is groovy/cli/OptionField, but the bytecode of
            // groovy/cli/OptionField.class does not list any nested classes.
            // This happens with a few other CollectorHelper classes in these jars.
            // Therefore, these are faulty jars, and our snapshotting logic cannot process it.
            return true
        }
        if (classFile.classRoot.name.startsWith("gradle-api")
            && classFile.unixStyleRelativePath.startsWith("org/gradle/internal/impldep/META-INF/versions")
        ) {
            // [FAULTY JAR] gradle-api-6.9.jar has the following entries:
            //   - org/gradle/internal/impldep/org/junit/platform/commons/util/ModuleUtils.class
            //   - org/gradle/internal/impldep/META-INF/versions/9/org/junit/platform/commons/util/ModulesUtils.class
            //   - org/gradle/internal/impldep/META-INF/versions/9/org/junit/platform/commons/util/ModuleUtils$ModuleReferenceScanner.class
            // The META-INF directories are located not at the top level (which is not expected), and those directories escaped our filter
            // which filters out top-level META-INF directories. We then failed to snapshot ModuleUtils$ModuleReferenceScanner.class as
            // there are 2 versions of ModuleUtils.class, and the one outside the META-INF directory doesn't have any nested classes.
            // Therefore, this is a faulty jar, and our snapshotting logic cannot process it.
            return true
        }
        return false
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
     * Note: If a jar has duplicate entries after filtering, only the first one is retained.
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
        val relativePathsToContents = mutableMapOf<String, ByteArray>()
        directory.walk().forEach { file ->
            val unixStyleRelativePath = file.relativeTo(directory).invariantSeparatorsPath
            if (entryFilter == null || entryFilter(unixStyleRelativePath, file.isDirectory)) {
                relativePathsToContents[unixStyleRelativePath] = file.readBytes()
            }
        }
        return relativePathsToContents.toSortedMap().toMap(LinkedHashMap())
    }

    private fun readJar(
        jarFile: File,
        entryFilter: ((unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean)? = null
    ): LinkedHashMap<String, ByteArray> {
        val relativePathsToContents = mutableMapOf<String, ByteArray>()
        ZipInputStream(jarFile.inputStream().buffered()).use { zipInputStream ->
            while (true) {
                val entry = zipInputStream.nextEntry ?: break
                val unixStyleRelativePath = entry.name
                if (entryFilter == null || entryFilter(unixStyleRelativePath, entry.isDirectory)) {
                    relativePathsToContents.computeIfAbsent(unixStyleRelativePath) { zipInputStream.readBytes() }
                }
            }
        }
        return relativePathsToContents.toSortedMap().toMap(LinkedHashMap())
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

private const val protoBasedDefaultValue = false