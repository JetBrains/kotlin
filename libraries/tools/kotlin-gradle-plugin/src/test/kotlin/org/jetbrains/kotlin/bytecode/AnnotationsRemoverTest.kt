package org.jetbrains.kotlin.bytecode

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil.createTempDirectory
import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.gradle.util.checkBytecodeNotContains
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import java.io.*
import kotlin.properties.Delegates

class AnnotationsRemoverTest {
    private var workingDir: File by Delegates.notNull()

    @Before
    fun setUp() {
        workingDir = createTempDirectory(AnnotationsRemoverTest::class.java.simpleName, null)
    }

    @After
    fun tearDown() {
        workingDir.deleteRecursively()
    }

    @Test
    fun testRemoveAnnotations() {
        // initial build
        val sourceDir = File(workingDir, "src").apply { mkdirs() }
        val annotationsKt = File(sourceDir, "annotations.kt").apply {
            writeText("""
                package foo

                annotation class Ann1
                annotation class Ann2
                annotation class Ann3
                annotation class Ann4
                annotation class Ann5
                annotation class NotRemovableAnn1
                annotation class NotRemovableAnn2
                annotation class NotRemovableAnn3
                annotation class NotRemovableAnn4
                annotation class NotRemovableAnn5
            """.trimIndent())
        }
        File(sourceDir, "A.kt").apply {
            writeText("""
                import foo.*

                @Ann1
                @NotRemovableAnn1
                class A {
                    @get:Ann2
                    @field:Ann3
                    @get:NotRemovableAnn2
                    @field:NotRemovableAnn3
                    val i = 10

                    @Ann4
                    @NotRemovableAnn4
                    fun m(@Ann5 @NotRemovableAnn5 x: Int) {}
                }
            """.trimIndent())
        }
        val annClassRegex = "annotation class (Ann\\d)".toRegex()
        val annotationsToRemove = annClassRegex.findAll(annotationsKt.readText()).toList().map { "foo/${it.groupValues[1]}" }
        assertEquals(5, annotationsToRemove.size)

        val notRemovableAnnClassRegex = "annotation class (NotRemovableAnn\\d)".toRegex()
        val notRemovableAnns = notRemovableAnnClassRegex.findAll(annotationsKt.readText()).toList().map { "foo/${it.groupValues[1]}" }
        assertEquals(5, notRemovableAnns.size)

        val outDir = File(workingDir, "out").apply { mkdirs() }
        compileAll(sourceDir, outDir)
        val aClass = File(outDir, "A.class")
        assert(aClass.exists()) { "$aClass does not exist" }
        checkBytecodeContains(aClass, annotationsToRemove)

        // remove annotations
        val transformedOut = File(workingDir, "transformed").apply { mkdirs() }
        val aTransformedClass = File(transformedOut, "A.class")
        val remover = AnnotationsRemover(annotationsToRemove)
        remover.transformClassFile(aClass, aTransformedClass)
        checkBytecodeNotContains(aTransformedClass, annotationsToRemove)
        checkBytecodeContains(aTransformedClass, notRemovableAnns)
    }

    private fun compileAll(inputDir: File, outputDir: File) {
        val ktFiles = inputDir.walk()
                .filter { it.isFile && it.extension.toLowerCase() == "kt" }
                .map { it.absolutePath }
                .toList().toTypedArray()

        val byteOut = ByteArrayOutputStream()
        val exitCode = PrintStream(byteOut).use { err ->
            K2JVMCompiler().exec(err, *ktFiles, "-d", outputDir.absolutePath)
        }

        if (exitCode != ExitCode.OK) {
            System.err.print(byteOut.toString())
        }

        assertEquals(ExitCode.OK, exitCode)
    }
}