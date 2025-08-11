/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.tests

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.AbiToolsFactory
import org.jetbrains.kotlin.abi.tools.api.v2.AbiToolsV2
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.util.ServiceLoader

class RuntimePublicAPITest {

    @[Rule JvmField]
    val testName = TestName()

    @JvmField
    val abiTools: AbiToolsV2 = ServiceLoader<AbiToolsFactory>.load(AbiToolsFactory::class.java).single().get().v2

    @Test fun kotlinStdlibRuntimeMerged() {
        snapshotAPIAndCompare("../../stdlib/build/libs", "kotlin-stdlib", listOf("kotlin.jvm.internal"))
    }

    @Test fun kotlinStdlibJdk7() {
        snapshotAPIAndCompare("../../stdlib/jdk7/build/libs", "kotlin-stdlib-jdk7")
    }

    @Test fun kotlinStdlibJdk8() {
        snapshotAPIAndCompare("../../stdlib/jdk8/build/libs", "kotlin-stdlib-jdk8")
    }

    @Test fun kotlinReflect() {
        snapshotAPIAndCompare("../../reflect/build/libs", "kotlin-reflect(?!-[-a-z]+)", nonPublicPackages = listOf("kotlin.reflect.jvm.internal"))
    }

    private fun snapshotAPIAndCompare(
        basePath: String,
        jarPattern: String,
        publicPackages: List<String> = emptyList(),
        nonPublicPackages: List<String> = emptyList(),
        nonPublicAnnotations: List<String> = emptyList()
    ) {
        val base = File(basePath).absoluteFile.normalize()
        val jarFile = getJarFile(base, jarPattern, System.getProperty("kotlinVersion"))

        val internalToPublicPackages = publicPackages
        val excludedClasses = nonPublicPackages.map { packageName -> "$packageName.**" }.toSet()
        val excludedAnnotatedWith = nonPublicAnnotations.toSet()

        val filters = AbiFilters(emptySet(), excludedClasses, emptySet(), excludedAnnotatedWith)

        val dump: (Appendable) -> Unit = { writer ->
            abiTools.printJvmDump(writer, filters, jarFiles = listOf(jarFile)) { packageName, _ ->
                internalToPublicPackages.any { packageName.startsWith(it) && (packageName.length == it.length || packageName[it.length] == '.') }
            }
        }

        val target = File("reference-public-api")
            .resolve(testName.methodName.replaceCamelCaseWithDashedLowerCase() + ".txt")

        dumpAndCompareWith(dump, target)
    }

    private fun getJarFile(base: File, jarPattern: String, kotlinVersion: String?): File =
        getLibFile(base, jarPattern, kotlinVersion, "jar")

}
