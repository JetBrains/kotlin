package org.jetbrains.kotlin.test

import java.io.File

object TestMetadataUtil {
    @JvmStatic
    fun getTestData(testClass: Class<*>): File? {
        val testRoot = getTestRoot(testClass) ?: return null
        val testMetadataAnnotation = testClass.getAnnotation(TestMetadata::class.java) ?: return null
        return File(testRoot, testMetadataAnnotation.value)
    }

    @JvmStatic
    fun getTestDataPath(testClass: Class<*>): String {
        val path = getTestData(testClass)?.absolutePath ?: KotlinTestUtils.getHomeDirectory()
        return if (path.endsWith(File.separator)) path else path + File.separator
    }

    @JvmStatic
    fun getFile(testClass: Class<*>, path: String): File {
        return File(getTestData(testClass), path)
    }

    @JvmStatic
    fun getTestRoot(testClass: Class<*>): File? {
        var current = testClass
        while (true) {
            current = current.enclosingClass ?: break
        }

        val testRootAnnotation = current.getAnnotation(TestRoot::class.java) ?: return null
        return File(KotlinTestUtils.getHomeDirectory(), testRootAnnotation.value)
    }
}