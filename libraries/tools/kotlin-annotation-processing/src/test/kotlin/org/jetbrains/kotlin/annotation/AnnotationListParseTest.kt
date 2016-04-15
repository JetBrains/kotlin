package org.jetbrains.kotlin.annotation

import org.junit.Assert
import org.junit.Test
import java.io.File
import org.junit.Assert.*
import java.io.IOException

public class AnnotationListParseTest {

    @Test
    fun testAnnotatedGettersSetters() = doTest("annotatedGettersSetters")

    @Test
    fun testAnnotationInSameFile() = doTest("annotationInSameFile")

    @Test
    fun testAnonymousClasses() = doTest("anonymousClasses")

    @Test
    fun testClassAnnotations() = doTest("classAnnotations")

    @Test
    fun testConstructors() = doTest("constructors")

    @Test
    fun testDefaultPackage() = doTest("defaultPackage")

    @Test
    fun testFieldAnnotations() = doTest("fieldAnnotations")

    @Test
    fun testLocalClasses() = doTest("localClasses")

    @Test
    fun testMethodAnnotations() = doTest("methodAnnotations")

    @Test
    fun testNestedClasses() = doTest("nestedClasses")

    @Test
    fun testSimple() = doTest("simple")

    @Test
    fun testDeclarations() = doTest("classDeclarations")


    private val resourcesRootFile = File("src/test/resources/parse")

    private fun doTest(testName: String) {
        val annotationsFile = File(resourcesRootFile, "$testName/annotations.txt")
        val expectedFile = File(resourcesRootFile, "$testName/parsed.txt")

        assertTrue(annotationsFile.absolutePath + " does not exist.", annotationsFile.exists())

        val annotationProvider = KotlinAnnotationProvider(annotationsFile)
        val parsedAnnotations = annotationProvider.annotatedKotlinElements

        val actualAnnotations = StringBuilder()
        parsedAnnotations.forEach {
            for (element in it.value) {
                actualAnnotations.append(it.key).append(' ').append(element.classFqName)
                when (element) {
                    is AnnotatedElementDescriptor.Method -> actualAnnotations.append(' ').append(element.methodName)
                    is AnnotatedElementDescriptor.Field -> actualAnnotations.append(' ').append(element.fieldName)
                    is AnnotatedElementDescriptor.Constructor -> actualAnnotations.append(" ${AnnotatedElementDescriptor.Constructor.METHOD_NAME}")
                    is AnnotatedElementDescriptor.Class -> {}
                }
                actualAnnotations.append('\n')
            }
        }

        val actualAnnotationsSorted = actualAnnotations.toString().lines().filter { it.isNotEmpty() }.sorted()
        val classDeclarationsSorted = annotationProvider.kotlinClasses.sorted()

        val fileContents = (actualAnnotationsSorted + classDeclarationsSorted).joinToString("\n")
        assertEqualsToFile(expectedFile, fileContents)
    }

    // KotlinTestUtils.assertEqualsToFile() is not reachable from here
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