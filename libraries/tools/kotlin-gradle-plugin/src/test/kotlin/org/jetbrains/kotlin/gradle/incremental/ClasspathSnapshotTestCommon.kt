/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.SourceFile.JavaSourceFile
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.SourceFile.KotlinSourceFile
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.compile
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.compileAll
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.snapshot
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.snapshotAll
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class ClasspathSnapshotTestCommon {

    companion object {
        val testDataDir =
            File("libraries/tools/kotlin-gradle-plugin/src/testData/org/jetbrains/kotlin/gradle/incremental/ClasspathSnapshotTestCommon")
    }

    @get:Rule
    val tmpDir = TemporaryFolder()

    // Use Gson to compare objects
    private val gson by lazy { GsonBuilder().setPrettyPrinting().create() }
    protected fun Any.toGson(): String = gson.toJson(this)

    sealed class SourceFile(val baseDir: File, relativePath: String) {
        val unixStyleRelativePath: String

        init {
            unixStyleRelativePath = relativePath.replace('\\', '/')
        }

        fun asFile() = File(baseDir, unixStyleRelativePath)

        class KotlinSourceFile(baseDir: File, relativePath: String, val preCompiledClassFiles: List<ClassFile>) :
            SourceFile(baseDir, relativePath) {

            constructor(baseDir: File, relativePath: String, preCompiledClassFile: ClassFile) :
                    this(baseDir, relativePath, listOf(preCompiledClassFile))
        }

        class JavaSourceFile(baseDir: File, relativePath: String) : SourceFile(baseDir, relativePath)
    }

    /** Same as [SourceFile] but with a [TemporaryFolder] to store the results of operations on the [SourceFile]. */
    open class TestSourceFile(val sourceFile: SourceFile, private val tmpDir: TemporaryFolder) {

        fun replace(oldValue: String, newValue: String, preCompiledKotlinClassFile: ClassFile? = null): TestSourceFile {
            val fileContents = sourceFile.asFile().readText()
            check(fileContents.contains(oldValue)) { "String '$oldValue' not found in file '${sourceFile.asFile().path}'" }

            val newSourceFile = when (sourceFile) {
                is KotlinSourceFile -> KotlinSourceFile(tmpDir.newFolder(), sourceFile.unixStyleRelativePath, preCompiledKotlinClassFile!!)
                is JavaSourceFile -> JavaSourceFile(tmpDir.newFolder(), sourceFile.unixStyleRelativePath)
            }
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
        fun compileAll(): List<ClassFile> = sourceFile.compile(tmpDir)

        /**
         * Compiles this source file and returns the snapshot of a single generated .class file, or fails if zero or more than one .class
         * file was generated.
         *
         * Alternatively, the caller can call [compileAndSnapshotAll] to get the snapshots of all generated .class files.
         */
        fun compileAndSnapshot() = compile().snapshot()

        /** Compiles this source file and returns the snapshots of all generated .class files. */
        fun compileAndSnapshotAll(): List<ClassSnapshot> = compileAll().snapshotAll()
    }

    object Util {

        /** Compiles the source files in the given directory and returns all generated .class files. */
        fun compileAll(srcDir: File, classpath: List<File>, tmpDir: TemporaryFolder): List<ClassFile> {
            val kotlinClasses = compileKotlin(srcDir, classpath, tmpDir)

            val javaClasspath = classpath + listOfNotNull(kotlinClasses.firstOrNull()?.classRoot)
            val javaClasses = compileJava(srcDir, javaClasspath, tmpDir)

            return kotlinClasses + javaClasses
        }

        private fun compileKotlin(srcDir: File, classpath: List<File>, tmpDir: TemporaryFolder): List<ClassFile> {
            val preCompiledKotlinClassesDir = srcDir.path.let {
                File(it.substringBeforeLast("src") + "classes" + it.substringAfterLast("src"))
            }
            preCompileKotlinFilesIfNecessary(srcDir, preCompiledKotlinClassesDir, classpath, tmpDir)
            return getClassFilesInDir(preCompiledKotlinClassesDir)
        }

        private val preCompiledKotlinClassesDirs = mutableSetOf<File>()

        /**
         * If <kotlin-repo>/dist/kotlinc/lib/kotlin-compiler.jar is available (e.g., by running ./gradlew dist), we will be able to call the
         * Kotlin compiler to generate classes. However, kotlin-compiler.jar is currently not available in CI builds, so we need to
         * pre-compile the classes locally and put them in the test data to check in.
         */
        @Synchronized // To safe-guard shared variable preCompiledKotlinClassesDirs
        private fun preCompileKotlinFilesIfNecessary(
            srcDir: File,
            preCompiledKotlinClassesDir: File,
            classpath: List<File>,
            tmpDir: TemporaryFolder,
            preCompile: Boolean = false // Set to `true` to pre-compile Kotlin class files locally (DO NOT check in with preCompile = true)
        ) {
            if (preCompile) {
                if (!preCompiledKotlinClassesDirs.contains(preCompiledKotlinClassesDir)) {
                    val classFiles = doCompileKotlin(srcDir, classpath, tmpDir)
                    preCompiledKotlinClassesDir.deleteRecursively()
                    for (classFile in classFiles) {
                        File(preCompiledKotlinClassesDir, classFile.unixStyleRelativePath).apply {
                            parentFile.mkdirs()
                            classFile.asFile().copyTo(this)
                        }
                    }
                    preCompiledKotlinClassesDirs.add(preCompiledKotlinClassesDir)
                }
            }
        }

        fun SourceFile.compile(tmpDir: TemporaryFolder): List<ClassFile> {
            return if (this is KotlinSourceFile) {
                preCompiledClassFiles.forEach {
                    preCompileKotlinFilesIfNecessary(baseDir, it.classRoot, classpath = emptyList(), tmpDir)
                }
                preCompiledClassFiles
            } else {
                val srcDir = tmpDir.newFolder()
                asFile().copyTo(File(srcDir, unixStyleRelativePath))
                compileAll(srcDir, classpath = emptyList(), tmpDir)
            }
        }

        private fun doCompileKotlin(srcDir: File, classpath: List<File>, tmpDir: TemporaryFolder): List<ClassFile> {
            if (srcDir.walk().none { it.path.endsWith(".kt") }) {
                return emptyList()
            }

            val classesDir = tmpDir.newFolder()
            org.jetbrains.kotlin.test.MockLibraryUtil.compileKotlin(
                srcDir.path,
                classesDir,
                extraClasspath = classpath.map { it.path }.toTypedArray()
            )
            return getClassFilesInDir(classesDir)
        }

        private fun compileJava(srcDir: File, classpath: List<File>, tmpDir: TemporaryFolder): List<ClassFile> {
            val javaFiles = srcDir.walk().toList().filter { it.path.endsWith(".java") }
            if (javaFiles.isEmpty()) {
                return emptyList()
            }

            val classesDir = tmpDir.newFolder()
            val classpathOption =
                if (classpath.isNotEmpty()) listOf("-classpath", classpath.joinToString(File.pathSeparator)) else emptyList()

            KotlinTestUtils.compileJavaFiles(javaFiles, listOf("-d", classesDir.path) + classpathOption)
            return getClassFilesInDir(classesDir)
        }

        private fun getClassFilesInDir(classesDir: File): List<ClassFile> {
            return classesDir.walk().toList()
                .filter { it.isFile && it.path.endsWith(".class") }
                .map { ClassFile(classesDir, it.toRelativeString(classesDir)) }
                .sortedBy { it.unixStyleRelativePath.substringBefore(".class") }
        }

        // `ClassFile`s in production code could be in a jar, but the `ClassFile`s in tests are currently in a directory, so converting it
        // to a File is possible.
        @Suppress("MemberVisibilityCanBePrivate")
        fun ClassFile.asFile() = File(classRoot, unixStyleRelativePath)

        @Suppress("MemberVisibilityCanBePrivate")
        fun ClassFile.readBytes() = asFile().readBytes()

        fun ClassFile.snapshot(protoBased: Boolean? = null): ClassSnapshot = listOf(this).snapshotAll(protoBased).single()

        fun List<ClassFile>.snapshotAll(protoBased: Boolean? = null): List<ClassSnapshot> {
            val classFilesWithContents = this.map { ClassFileWithContents(it, it.readBytes()) }
            return ClassSnapshotter.snapshot(classFilesWithContents, protoBased, includeDebugInfoInSnapshot = true)
        }
    }

    abstract class ChangeableTestSourceFile(sourceFile: SourceFile, tmpDir: TemporaryFolder) : TestSourceFile(sourceFile, tmpDir) {

        abstract fun changePublicMethodSignature(): TestSourceFile

        abstract fun changeMethodImplementation(): TestSourceFile
    }

    class SimpleKotlinClass(tmpDir: TemporaryFolder) : ChangeableTestSourceFile(
        KotlinSourceFile(
            baseDir = File(testDataDir, "src/kotlin"), relativePath = "com/example/SimpleKotlinClass.kt",
            preCompiledClassFile = ClassFile(File(testDataDir, "classes/kotlin/original"), "com/example/SimpleKotlinClass.class")
        ), tmpDir
    ) {

        override fun changePublicMethodSignature() = replace(
            "publicFunction()", "publicFunction(newParam: Int)",
            preCompiledKotlinClassFile = ClassFile(
                File(testDataDir, "classes/kotlin/changedPublicMethodSignature"), "com/example/SimpleKotlinClass.class"
            )
        )

        override fun changeMethodImplementation() = replace(
            "I'm in a public function", "This function's implementation has changed!",
            preCompiledKotlinClassFile = ClassFile(
                File(testDataDir, "classes/kotlin/changedMethodImplementation"), "com/example/SimpleKotlinClass.class"
            )
        )
    }

    class SimpleJavaClass(tmpDir: TemporaryFolder) : ChangeableTestSourceFile(
        JavaSourceFile(File(testDataDir, "src/java"), "com/example/SimpleJavaClass.java"), tmpDir
    ) {

        override fun changePublicMethodSignature() = replace("publicMethod()", "publicMethod(int newParam)")

        override fun changeMethodImplementation() = replace("I'm in a public method", "This method's implementation has changed!")
    }

    class JavaClassWithNestedClasses(tmpDir: TemporaryFolder) : ChangeableTestSourceFile(
        JavaSourceFile(File(testDataDir, "src/java"), "com/example/JavaClassWithNestedClasses.java"), tmpDir
    ) {

        /** The source file contains multiple classes, select the one that we want to test. */
        val nestedClassToTest = "com/example/JavaClassWithNestedClasses\$InnerClass"

        override fun changePublicMethodSignature() = replace("publicMethod()", "publicMethod(int newParam)")

        override fun changeMethodImplementation() = replace("I'm in a public method", "This method's implementation has changed!")
    }
}
