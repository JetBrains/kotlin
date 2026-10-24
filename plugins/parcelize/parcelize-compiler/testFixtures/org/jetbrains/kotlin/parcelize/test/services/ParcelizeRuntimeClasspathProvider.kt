/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.services

import org.hamcrest.BaseDescription
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.PARCELIZE_COMPILER_PLUGIN_CLASSPATH
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeDirectives.ENABLE_PARCELIZE
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import org.junit.runner.JUnitCore
import java.io.File
import kotlin.reflect.KClass

class ParcelizeRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    companion object {
        val layoutlibJar: File by lazy { getLayoutLibFile("layoutLib.path") }
        val layoutlibApiJar: File by lazy { getLayoutLibFile("layoutLibApi.path") }

        private fun getLayoutLibFile(property: String): File {
            val layoutLibFile = File(System.getProperty(property))
            if (!layoutLibFile.isFile) {
                error("Can't find jar file in $property system property")
            }
            return layoutLibFile
        }
    }

    override fun runtimeClassPaths(module: TestModule): List<File> {
        if (ENABLE_PARCELIZE !in module.directives) return emptyList()
        val kotlinRuntimeJar = testServices.standardLibrariesPathProvider.runtimeJarForTests()

        val robolectricClasspath = System.getProperty("robolectric.classpath")
            ?: throw RuntimeException("Unable to get a valid classpath from 'robolectric.classpath' property, please set it accordingly")
        val robolectricJars = robolectricClasspath.split(File.pathSeparator)
            .map { File(it) }
            .sortedBy { it.nameWithoutExtension }

        val parcelizeRuntimeJars = System.getProperty(PARCELIZE_COMPILER_PLUGIN_CLASSPATH)?.split(File.pathSeparator)?.map(::File)
            ?: error("Unable to get a valid classpath from '$PARCELIZE_COMPILER_PLUGIN_CLASSPATH' property")

        val hamcrestJar = getJarFor(BaseDescription::class)
        val junitJar = getJarFor(JUnitCore::class)

        return buildList {
            add(kotlinRuntimeJar)
            add(layoutlibJar)
            add(layoutlibApiJar)
            addAll(robolectricJars)
            add(junitJar)
            add(hamcrestJar)
            addAll(parcelizeRuntimeJars)
        }
    }

    /**
     * Returns jar file where `clazz` is located by using `clazz`'s classloader.
     */
    private fun getJarFor(clazz: KClass<out Any>): File {
        val resourceName = clazz.java.name.replace('.', '/') + ".class"
        return File(
            clazz.java.classLoader.getResource(resourceName)!!.file
                .substringAfter("file:")
                .substringBeforeLast('!')
        )
    }
}
