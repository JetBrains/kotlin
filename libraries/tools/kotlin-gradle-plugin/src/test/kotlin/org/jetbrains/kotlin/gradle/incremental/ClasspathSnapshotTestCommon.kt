/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import com.google.gson.GsonBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class ClasspathSnapshotTestCommon {

    companion object {
        val testDataDir = File("libraries/tools/kotlin-gradle-plugin/src/testData/kotlin.incremental.useClasspathSnapshot")
    }

    @get:Rule
    val tmpDir = TemporaryFolder()

    abstract class RelativeFile(val baseDir: File, val relativePath: String) {
        fun asFile() = File(baseDir, relativePath)
    }

    class SourceFile(baseDir: File, relativePath: String) : RelativeFile(baseDir, relativePath) {
        init {
            check(relativePath.endsWith(".kt") || relativePath.endsWith(".java"))
        }
    }

    class ClassFile(baseDir: File, relativePath: String) : RelativeFile(baseDir, relativePath) {
        init {
            check(relativePath.endsWith(".class"))
        }

        fun snapshot() = ClassSnapshotter.snapshot(asFile().readBytes())
    }

    open class TestSourceFile(val sourceFile: SourceFile, val tmpDir: TemporaryFolder) {

        fun replace(oldValue: String, newValue: String): TestSourceFile {
            val fileContents = sourceFile.asFile().readText()
            check(fileContents.contains(oldValue)) { "String '$oldValue' not found in file '${sourceFile.asFile().path}'" }

            val newSourceFile = SourceFile(tmpDir.newFolder(), sourceFile.relativePath).also { it.asFile().parentFile.mkdirs() }
            newSourceFile.asFile().writeText(fileContents.replace(oldValue, newValue))
            return TestSourceFile(newSourceFile, tmpDir)
        }

        fun compile(): ClassFile {
            return when {
                sourceFile.relativePath.endsWith(".kt") -> compileKotlin()
                else -> error("Unexpected file name extension: '${sourceFile.asFile().path}'")
            }
        }

        private fun compileKotlin(): ClassFile {
            // TODO: Call Kotlin compiler to generate classes (see https://github.com/JetBrains/kotlin/pull/4512#discussion_r679432232)
            return ClassFile(
                File(sourceFile.baseDir.path.substringBeforeLast("src") + "classes" + sourceFile.baseDir.path.substringAfterLast("src")),
                sourceFile.relativePath.substringBeforeLast('.') + ".class"
            )
        }

        fun compileAndSnapshot(): ClassSnapshot = compile().snapshot()
    }

    abstract class ChangeableTestSourceFile(sourceFile: SourceFile, tmpDir: TemporaryFolder) : TestSourceFile(sourceFile, tmpDir) {

        abstract fun changePublicMethodSignature(): TestSourceFile

        abstract fun changeMethodImplementation(): TestSourceFile
    }

    class SimpleKotlinClass(tmpDir: TemporaryFolder) : ChangeableTestSourceFile(
        SourceFile(File(testDataDir, "src/original"), "com/example/SimpleKotlinClass.kt"), tmpDir
    ) {

        override fun changePublicMethodSignature(): TestSourceFile {
            return TestSourceFile(
                SourceFile(
                    File(sourceFile.baseDir.path.replace("original", "changedPublicMethodSignature")),
                    sourceFile.relativePath
                ), tmpDir
            )
        }

        override fun changeMethodImplementation(): TestSourceFile {
            return TestSourceFile(
                SourceFile(
                    File(sourceFile.baseDir.path.replace("original", "changedMethodImplementation")),
                    sourceFile.relativePath
                ), tmpDir
            )
        }
    }

    // Use Gson to compare objects
    private val gson by lazy { GsonBuilder().setPrettyPrinting().create() }
    protected fun Any.toGson(): String = gson.toJson(this)
}
