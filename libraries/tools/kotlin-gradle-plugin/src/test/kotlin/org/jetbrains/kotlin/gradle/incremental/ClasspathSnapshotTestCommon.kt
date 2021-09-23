/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.readBytes
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.snapshot
import org.jetbrains.kotlin.gradle.util.compileSources
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class ClasspathSnapshotTestCommon {

    companion object {
        val testDataDir = File("libraries/tools/kotlin-gradle-plugin/src/testData/kotlin.incremental.useClasspathSnapshot")
    }

    @get:Rule
    val tmpDir = TemporaryFolder()

    // Use Gson to compare objects
    private val gson by lazy { GsonBuilder().setPrettyPrinting().create() }
    protected fun Any.toGson(): String = gson.toJson(this)

    open class SourceFile(val baseDir: File, relativePath: String) {
        val unixStyleRelativePath: String

        init {
            unixStyleRelativePath = relativePath.replace('\\', '/')
        }

        fun asFile() = File(baseDir, unixStyleRelativePath)
    }

    class KotlinSourceFile(baseDir: File, relativePath: String, val preCompiledClassFiles: List<ClassFile>) :
        SourceFile(baseDir, relativePath) {

        constructor(baseDir: File, relativePath: String, preCompiledClassFile: ClassFile) :
                this(baseDir, relativePath, listOf(preCompiledClassFile))
    }

    /** Same as [SourceFile] but with a [TemporaryFolder] to store the results of operations on the [SourceFile]. */
    open class TestSourceFile(val sourceFile: SourceFile, private val tmpDir: TemporaryFolder) {

        fun replace(oldValue: String, newValue: String, preCompiledKotlinClassFile: ClassFile? = null): TestSourceFile {
            val fileContents = sourceFile.asFile().readText()
            check(fileContents.contains(oldValue)) { "String '$oldValue' not found in file '${sourceFile.asFile().path}'" }

            val newSourceFile =
                preCompiledKotlinClassFile?.let { KotlinSourceFile(tmpDir.newFolder(), sourceFile.unixStyleRelativePath, it) }
                    ?: SourceFile(tmpDir.newFolder(), sourceFile.unixStyleRelativePath)
            newSourceFile.asFile().parentFile.mkdirs()
            newSourceFile.asFile().writeText(fileContents.replace(oldValue, newValue))
            return TestSourceFile(newSourceFile, tmpDir)
        }

        /**
         * Compiles this source file and returns a single generated .class file, or fails if zero or more than one .class file was
         * generated.
         *
         * Alternatively, the caller can call [compileAll] to get all generated .class files.
         */
        fun compile(): ClassFile = compileAll().single()

        /** Compiles this source file and returns all generated .class files. */
        @Suppress("MemberVisibilityCanBePrivate")
        fun compileAll(): List<ClassFile> {
            val filePath = sourceFile.asFile().path
            return when {
                filePath.endsWith(".kt") -> compileKotlin()
                filePath.endsWith(".java") -> compileJava()
                else -> error("Unexpected file name extension: $filePath")
            }
        }

        private fun compileKotlin(): List<ClassFile> {
            sourceFile as KotlinSourceFile

            // TODO: Call Kotlin compiler to generate classes.
            // If <kotlin-repo>/dist/kotlinc/lib/kotlin-compiler.jar is available (e.g., by running ./gradlew dist), we will be able to
            // call the Kotlin compiler to generate classes from here. For example:
//            val classesDir = tmpDir.newFolder()
//            org.jetbrains.kotlin.test.MockLibraryUtil.compileKotlin(sourceFile.asFile().path, classesDir)
//            val classFiles = classesDir.walk()
//                .filter { it.isFile && !it.toRelativeString(classesDir).startsWith("META-INF/") }
//                .map { ClassFile(classesDir, it.toRelativeString(classesDir)) }
//                .sortedBy { it.unixStyleRelativePath.substringBefore(".class") }
//                .toList()
//            kotlin.test.assertEquals(classFiles.size, sourceFile.preCompiledClassFiles.size)
//            sourceFile.preCompiledClassFiles.forEach {
//                File(classesDir, it.unixStyleRelativePath).copyTo(File(it.classRoot, it.unixStyleRelativePath), overwrite = true)
//            }
            // However, kotlin-compiler.jar is currently not available in CI builds, so we need to pre-compile the classes, put them in the
            // test data, and use them here instead.
            return sourceFile.preCompiledClassFiles
        }

        private fun compileJava(): List<ClassFile> {
            val classesDir = tmpDir.newFolder()
            compileSources(listOf(sourceFile.asFile()), classesDir)
            return classesDir.walk().filter { it.isFile }
                .map { ClassFile(classesDir, it.toRelativeString(classesDir)) }
                .sortedBy { it.unixStyleRelativePath.substringBefore(".class") }
                .toList()
        }

        /**
         * Compiles this source file and returns the snapshot of a single generated .class file, or fails if zero or more than one .class
         * file was generated.
         *
         * Alternatively, the caller can call [compileAndSnapshotAll] to get the snapshots of all generated .class files.
         */
        fun compileAndSnapshot() = compile().snapshot()

        /** Compiles this source file and returns the snapshots of all generated .class files. */
        fun compileAndSnapshotAll(): List<ClassSnapshot> {
            val classes = compileAll().map {
                ClassFileWithContents(it, it.readBytes())
            }
            return ClassSnapshotter.snapshot(classes)
        }
    }

    object Util {

        fun ClassFile.readBytes(): ByteArray {
            // The class files in tests are currently in a directory, so we don't need to handle jars
            return File(classRoot, unixStyleRelativePath).readBytes()
        }

        fun ClassFile.snapshot(): ClassSnapshot {
            return ClassSnapshotter.snapshot(listOf(ClassFileWithContents(this, readBytes()))).single()
        }
    }

    abstract class ChangeableTestSourceFile(sourceFile: SourceFile, tmpDir: TemporaryFolder) : TestSourceFile(sourceFile, tmpDir) {

        abstract fun changePublicMethodSignature(): TestSourceFile

        abstract fun changeMethodImplementation(): TestSourceFile
    }

    class SimpleKotlinClass(tmpDir: TemporaryFolder) : ChangeableTestSourceFile(
        KotlinSourceFile(
            baseDir = File(testDataDir, "src/original"),
            relativePath = "com/example/SimpleKotlinClass.kt",
            preCompiledClassFile = ClassFile(File(testDataDir, "classes/original"), "com/example/SimpleKotlinClass.class")
        ), tmpDir
    ) {

        override fun changePublicMethodSignature() = replace(
            "publicMethod()", "changedPublicMethod()",
            preCompiledKotlinClassFile = ClassFile(
                File(testDataDir, "classes/changedPublicMethodSignature"), "com/example/SimpleKotlinClass.class"
            )
        )

        override fun changeMethodImplementation() = replace(
            "I'm in a public method", "This method implementation has changed!",
            preCompiledKotlinClassFile = ClassFile(
                File(testDataDir, "classes/changedMethodImplementation"), "com/example/SimpleKotlinClass.class"
            )
        )
    }

    class SimpleJavaClass(tmpDir: TemporaryFolder) : ChangeableTestSourceFile(
        SourceFile(File(testDataDir, "src/original"), "com/example/SimpleJavaClass.java"), tmpDir
    ) {

        override fun changePublicMethodSignature() = replace("publicMethod()", "changedPublicMethod()")

        override fun changeMethodImplementation() = replace("I'm in a public method", "This method implementation has changed!")
    }

    class JavaClassWithNestedClasses(tmpDir: TemporaryFolder) : ChangeableTestSourceFile(
        SourceFile(File(testDataDir, "src/original"), "com/example/JavaClassWithNestedClasses.java"), tmpDir
    ) {

        /** The source file contains multiple classes, select the one we are mostly interested in. */
        val nestedClassToTest = "com/example/JavaClassWithNestedClasses\$InnerClass"

        override fun changePublicMethodSignature() = replace("publicMethod()", "changedPublicMethod()")

        override fun changeMethodImplementation() = replace("I'm in a public method", "This method implementation has changed!")
    }
}
