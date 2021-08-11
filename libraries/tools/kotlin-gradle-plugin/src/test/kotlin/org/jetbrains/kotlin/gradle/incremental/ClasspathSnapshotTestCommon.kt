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

    data class SourceFile(val baseDir: File, val unixStyleRelativePath: String) {
        init {
            check(!unixStyleRelativePath.contains("\\")) { "Unix-style relative path must not contain '\\': $unixStyleRelativePath" }
        }

        fun asFile() = File(baseDir, unixStyleRelativePath)
    }

    /** Same as [SourceFile] but with a [TemporaryFolder] to store the results of operations on the [SourceFile]. */
    open class TestSourceFile(val sourceFile: SourceFile, protected val tmpDir: TemporaryFolder) {

        fun replace(oldValue: String, newValue: String, newBaseDir: File? = null): TestSourceFile {
            val fileContents = sourceFile.asFile().readText()
            check(fileContents.contains(oldValue)) { "String '$oldValue' not found in file '${sourceFile.asFile().path}'" }

            val newSourceFile = SourceFile(newBaseDir ?: tmpDir.newFolder(), sourceFile.unixStyleRelativePath)
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
            // TODO: Call Kotlin compiler to generate classes (see https://github.com/JetBrains/kotlin/pull/4512#discussion_r679432232)
            // Currently, we use the precompiled classes in the test data.
            return listOf(
                ClassFile(
                    File(testDataDir.path + "/classes/" + sourceFile.baseDir.path.substringAfterLast('/')),
                    sourceFile.unixStyleRelativePath.replace(".kt", ".class")
                )
            )
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
        SourceFile(File(testDataDir, "src/original"), "com/example/SimpleKotlinClass.kt"), tmpDir
    ) {

        override fun changePublicMethodSignature() = replace(
            "publicMethod()", "changedPublicMethod()",
            newBaseDir = File(tmpDir.newFolder(), "changedPublicMethodSignature")
        )

        override fun changeMethodImplementation() = replace(
            "I'm in a public method", "This method implementation has changed!",
            newBaseDir = File(tmpDir.newFolder(), "changedMethodImplementation")
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
