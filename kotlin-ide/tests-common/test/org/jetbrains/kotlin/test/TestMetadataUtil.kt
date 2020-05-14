package org.jetbrains.kotlin.test

import java.io.File

object TestMetadataUtil {
    @JvmStatic
    fun getTestData(testClass: Class<*>): File {
        val testRoot = getTestRoot(testClass)
        val testMetadataAnnotation = testClass.getAnnotation(TestMetadata::class.java)
                ?: error("@${TestMetadata::class.java.simpleName} annotation was not found on ${testClass.name}")

        return File(testRoot, testMetadataAnnotation.value)
    }

    @JvmStatic
    fun getFile(testClass: Class<*>, path: String): File {
        return File(getTestData(testClass), path)
    }

    @JvmStatic
    fun getTestRoot(testClass: Class<*>): File {
        var current = testClass
        while (true) {
            current = current.enclosingClass ?: break
        }

        val testRootAnnotation = current.getAnnotation(TestRoot::class.java)
            ?: error("@${TestRoot::class.java.simpleName} annotation was not found on ${current.name}")

        return File(KotlinTestUtils.getHomeDirectory(), testRootAnnotation.value)
    }
}