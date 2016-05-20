package org.jetbrains.kotlin.annotation

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

open class AnnotationListParseTest {
    companion object {
        const val ANNOTATIONS_FILE_NAME = "annotations.txt"
        const val PARSED_FILE_NAME = "parsed.txt"
    }

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
        doTest(testDir = File(resourcesRootFile, testName))
    }

    protected open fun doTest(testDir: File) {
        val annotationsFile = File(testDir, ANNOTATIONS_FILE_NAME)
        val expectedFile = File(testDir, PARSED_FILE_NAME)

        assertTrue(annotationsFile.absolutePath + " does not exist.", annotationsFile.exists())

        val annotationProvider = KotlinAnnotationProvider(annotationsFile)
        val parsedAnnotations = annotationProvider.annotatedKotlinElements

        val actualAnnotations = StringBuilder()
        parsedAnnotations.forEach {
            for (element in it.value) {
                actualAnnotations.append(it.key).append(' ').append(element.classFqName)
                when (element) {
                    is AnnotatedElement.Method ->
                        actualAnnotations.append(' ').append(element.methodName)
                    is AnnotatedElement.Field ->
                        actualAnnotations.append(' ').append(element.fieldName)
                    is AnnotatedElement.Constructor ->
                        actualAnnotations.append(" ${AnnotatedElement.Constructor.METHOD_NAME}")
                    is AnnotatedElement.Class -> {}
                }
                actualAnnotations.append('\n')
            }
        }

        val actualAnnotationsSorted = actualAnnotations.toString().lines().filter { it.isNotEmpty() }.sorted()
        val classDeclarationsSorted = annotationProvider.kotlinClasses.sorted()

        val fileContents = (actualAnnotationsSorted + classDeclarationsSorted).joinToString("\n")
        assertEqualsToFile(expectedFile, fileContents)
    }
}