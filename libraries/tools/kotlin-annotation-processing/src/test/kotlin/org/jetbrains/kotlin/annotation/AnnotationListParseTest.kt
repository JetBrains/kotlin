package org.jetbrains.kotlin.annotation

import org.junit.Test
import java.io.File
import org.junit.Assert.*

public class AnnotationListParseTest {

    Test fun testClassAnnotations() = doTest("classAnnotations")

    Test fun testDefaultPackage() = doTest("defaultPackage")

    Test fun testFieldAnnotations() = doTest("fieldAnnotations")

    Test fun testLocalClasses() = doTest("localClasses")

    Test fun testMethodAnnotations() = doTest("methodAnnotations")

    Test fun testNestedClasses() = doTest("nestedClasses")

    Test fun testSimple() = doTest("simple")


    private val resourcesRootFile = File("src/test/resources/parse")

    private fun doTest(testName: String) {
        val annotationsFile = File(resourcesRootFile, "$testName/annotations.txt")
        val expectedFile = File(resourcesRootFile, "$testName/parsed.txt")

        assertTrue(annotationsFile.getAbsolutePath() + " does not exist.", annotationsFile.exists())
        assertTrue(expectedFile.getAbsolutePath() + " does not exist.", expectedFile.exists())

        val expectedParseResult = expectedFile.readText()

        val annotationProvider = FileKotlinAnnotationProvider(annotationsFile)
        val parsedAnnotations = annotationProvider.annotatedKotlinElements

        val actualAnnotations = StringBuilder()
        parsedAnnotations.forEach {
            for (element in it.getValue()) {
                actualAnnotations.append(it.getKey()).append(' ').append(element.classFqName)
                when (element) {
                    is AnnotatedMethodDescriptor -> actualAnnotations.append(' ').append(element.methodName)
                    is AnnotatedFieldDescriptor -> actualAnnotations.append(' ').append(element.fieldName)
                }
                actualAnnotations.append('\n')
            }
        }

        val actualAnnotationsSorted = actualAnnotations.toString()
                .lines().filter { it.isNotEmpty() }.sort().joinToString("\n")

        assertEquals(expectedParseResult, actualAnnotationsSorted)
    }

    public class FileKotlinAnnotationProvider(val annotationsFile: File): KotlinAnnotationProvider() {
        override val serializedAnnotations: String
            get() = annotationsFile.readText()
    }

}