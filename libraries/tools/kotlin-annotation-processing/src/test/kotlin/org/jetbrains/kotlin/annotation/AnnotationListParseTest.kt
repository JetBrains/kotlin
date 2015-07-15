package org.jetbrains.kotlin.annotation

import org.junit.Assert
import org.junit.Test
import java.io.File
import org.junit.Assert.*
import java.io.IOException

public class AnnotationListParseTest {

    Test fun testAnnotatedGettersSetters() = doTest("annotatedGettersSetters")

    Test fun testAnnotationInSameFile() = doTest("annotationInSameFile")

    Test fun testAnonymousClasses() = doTest("anonymousClasses")

    Test fun testClassAnnotations() = doTest("classAnnotations")

    Test fun testConstructors() = doTest("constructors")

    Test fun testDefaultPackage() = doTest("defaultPackage")

    Test fun testFieldAnnotations() = doTest("fieldAnnotations")

    Test fun testLocalClasses() = doTest("localClasses")

    Test fun testMethodAnnotations() = doTest("methodAnnotations")

    Test fun testNestedClasses() = doTest("nestedClasses")

    Test fun testPlatformStatic() = doTest("platformStatic")

    Test fun testSimple() = doTest("simple")

    Test fun testDeclarations() = doTest("classDeclarations")


    private val resourcesRootFile = File("src/test/resources/parse")

    private fun doTest(testName: String) {
        val annotationsFile = File(resourcesRootFile, "$testName/annotations.txt")
        val expectedFile = File(resourcesRootFile, "$testName/parsed.txt")

        assertTrue(annotationsFile.getAbsolutePath() + " does not exist.", annotationsFile.exists())

        val annotationProvider = FileKotlinAnnotationProvider(annotationsFile)
        val parsedAnnotations = annotationProvider.annotatedKotlinElements

        val actualAnnotations = StringBuilder()
        parsedAnnotations.forEach {
            for (element in it.getValue()) {
                actualAnnotations.append(it.getKey()).append(' ').append(element.classFqName)
                when (element) {
                    is AnnotatedMethodDescriptor -> actualAnnotations.append(' ').append(element.methodName)
                    is AnnotatedFieldDescriptor -> actualAnnotations.append(' ').append(element.fieldName)
                    is AnnotatedConstructorDescriptor -> actualAnnotations.append(" <init>")
                    is AnnotatedClassDescriptor -> {}
                    else -> Assert.fail("Unknown element type: $element")
                }
                actualAnnotations.append('\n')
            }
        }

        val actualAnnotationsSorted = actualAnnotations.toString().lines().filter { it.isNotEmpty() }.sort()
        val classDeclarationsSorted = annotationProvider.kotlinClasses.sort()

        val fileContents = (actualAnnotationsSorted + classDeclarationsSorted).joinToString("\n")
        assertEqualsToFile(expectedFile, fileContents)
    }

    // JetTestUtils.assertEqualsToFile() is not reachable from here
    public fun assertEqualsToFile(expectedFile: File, actual: String) {
        val lineSeparator = System.getProperty("line.separator")
        val actualText = actual.replace(lineSeparator, "\n").trim('\n', ' ', '\t')

        if (!expectedFile.exists()) {
            expectedFile.writeText(actualText.replace("\n", lineSeparator))
            Assert.fail("Expected data file did not exist. Generating: " + expectedFile)
        }

        val expectedText = expectedFile.readText().replace(lineSeparator, "\n")

        assertEquals(expectedText, actualText)
    }

}