/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.tools.tests

import org.jetbrains.kotlin.tools.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.util.jar.JarFile

class RuntimePublicAPITest {

    @[Rule JvmField]
    val testName = TestName()

    @Test fun kotlinRuntime() {
        snapshotAPIAndCompare("../../tools/runtime/build/libs", "kotlin-runtime", listOf("../runtime-declarations.json"), listOf("kotlin.jvm.internal"))
    }

    //@Ignore("No more original stdlib jar is produced")
    @Test fun kotlinStdlib() {
        snapshotAPIAndCompare("../../stdlib/build/libs", "original-kotlin-stdlib", listOf("../stdlib-declarations.json"))
    }

    @Test fun kotlinStdlibRuntimeMerged() {
        snapshotAPIAndCompare("../../stdlib/build/libs", "kotlin-stdlib", listOf("../stdlib-declarations.json", "../runtime-declarations.json"), listOf("kotlin.jvm.internal"))
    }

    @Test fun kotlinStdlibJdk7() {
        snapshotAPIAndCompare("../../stdlib/jdk7/build/libs", "kotlin-stdlib-jdk7", listOf("../stdlib-jdk7-declarations.json"))
    }

    @Test fun kotlinStdlibJdk8() {
        snapshotAPIAndCompare("../../stdlib/jdk8/build/libs", "kotlin-stdlib-jdk8", listOf("../stdlib-jdk8-declarations.json"))
    }

    @Test fun kotlinStdlibJre7() {
        snapshotAPIAndCompare("../../stdlib/jre7/build/libs", "kotlin-stdlib-jre7", listOf("../stdlib-jre7-declarations.json"))
    }

    @Test fun kotlinStdlibJre8() {
        snapshotAPIAndCompare("../../stdlib/jre8/build/libs", "kotlin-stdlib-jre8", listOf("../stdlib-jre8-declarations.json"))
    }

    @Test fun kotlinReflect() {
        snapshotAPIAndCompare("../../tools/kotlin-reflect/build/libs", "kotlin-reflect(?!-[-a-z]+)", listOf("../reflect-declarations.json"), nonPublicPackages = listOf("kotlin.reflect.jvm.internal"))
    }


    private fun snapshotAPIAndCompare(basePath: String, jarPattern: String, kotlinJvmMappingsPath: List<String>, publicPackages: List<String> = emptyList(), nonPublicPackages: List<String> = emptyList()) {
        val base = File(basePath).absoluteFile.normalize()
        val jarFile = getJarPath(base, jarPattern, System.getProperty("kotlinVersion"))
        val kotlinJvmMappingsFiles = kotlinJvmMappingsPath.map(base::resolve)

        println("Reading kotlin visibilities from $kotlinJvmMappingsFiles")
        val publicPackagePrefixes = publicPackages.map { it.replace('.', '/') + '/' }
        val visibilities =
                kotlinJvmMappingsFiles
                        .map { readKotlinVisibilities(it).filterKeys { name -> publicPackagePrefixes.none { name.startsWith(it) } } }
                        .reduce { m1, m2 -> m1 + m2 }

        println("Reading binary API from $jarFile")
        val api = getBinaryAPI(JarFile(jarFile), visibilities).filterOutNonPublic(nonPublicPackages)

        val target = File("reference-public-api")
                .resolve(testName.methodName.replaceCamelCaseWithDashedLowerCase() + ".txt")

        api.dumpAndCompareWith(target)
    }

    private fun getJarPath(base: File, jarPattern: String, kotlinVersion: String?): File {
        val versionPattern = kotlinVersion?.let { "-" + Regex.escape(it) } ?: ".+"
        val regex = Regex(jarPattern + versionPattern + "\\.jar")
        val files = (base.listFiles() ?: throw Exception("Cannot list files in $base"))
            .filter { it.name.let {
                    it matches regex
                    && !it.endsWith("-sources.jar")
                    && !it.endsWith("-javadoc.jar") } }

        return files.singleOrNull() ?: throw Exception("No single file matching $regex in $base:\n${files.joinToString("\n")}")
    }

}

