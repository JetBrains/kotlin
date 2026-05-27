/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.tests

import kotlinx.validation.api.filterOutAnnotated
import kotlinx.validation.api.filterOutNonPublic
import kotlinx.validation.api.loadApiFromJvmClasses
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.util.jar.JarFile

class RuntimePublicAPITest {

    @[Rule JvmField]
    val testName = TestName()

    @Test fun kotlinStdlibRuntimeMerged() {
        snapshotAPIAndCompare("kotlin-stdlib", listOf("kotlin.jvm.internal"))
    }

    @Test fun kotlinStdlibJdk7() {
        snapshotAPIAndCompare("kotlin-stdlib-jdk7")
    }

    @Test fun kotlinStdlibJdk8() {
        snapshotAPIAndCompare("kotlin-stdlib-jdk8")
    }

    @Test fun kotlinReflect() {
        snapshotAPIAndCompare("kotlin-reflect(?!-[-a-z]+)", nonPublicPackages = listOf("kotlin.reflect.jvm.internal"))
    }

    private fun snapshotAPIAndCompare(
        jarPattern: String,
        publicPackages: List<String> = emptyList(),
        nonPublicPackages: List<String> = emptyList(),
        nonPublicAnnotations: List<String> = emptyList()
    ) {
        val artifactPaths = System.getProperty("testArtifacts").split(File.pathSeparator)
        val jarFiles = artifactPaths.map { File(it) }
        val jarFile = getJarFile(jarFiles, jarPattern, System.getProperty("kotlinVersion"))

        val publicPackagePrefixes = publicPackages.map { it.replace('.', '/') + '/' }
        val publicPackageFilter = { className: String -> publicPackagePrefixes.none { className.startsWith(it) } }

        val api = JarFile(jarFile).loadApiFromJvmClasses(publicPackageFilter)
            .filterOutNonPublic(nonPublicPackages)
            .filterOutAnnotated(nonPublicAnnotations.toSet())

        val target = ForTestCompileRuntime.getFileFromProperty("reference-public-api")
            .resolve(testName.methodName.replaceCamelCaseWithDashedLowerCase() + ".txt")

        api.dumpAndCompareWith(target)
    }

    private fun getJarFile(files: List<File>, jarPattern: String, kotlinVersion: String?): File =
        getLibFile(files, jarPattern, kotlinVersion, "jar")

}
