/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.util.jar.JarFile

class RuntimePublicAPITest {

    @[Rule JvmField]
    val testName = TestName()

    @Test fun kotlinRuntime() {
        snapshotAPIAndCompare("../../tools/runtime/target", "kotlin-runtime", listOf("runtime-declarations.json"), listOf("kotlin.jvm.internal"))
    }

    @Test fun kotlinStdlib() {
        snapshotAPIAndCompare("../../stdlib/target", "original-kotlin-stdlib", listOf("stdlib-declarations.json"))
    }

    @Test fun kotlinStdlibRuntimeMerged() {
        snapshotAPIAndCompare("../../stdlib/target", "kotlin-stdlib", listOf("stdlib-declarations.json", "../../tools/runtime/target/runtime-declarations.json"), listOf("kotlin.jvm.internal"))
    }

/*
    @Test fun kotlinReflect() {
        // requires declaration mapping JSON from kotlin-reflect which isn't built by maven build
        snapshotAPIAndCompare("../../tools/kotlin-reflect/target", "kotlin-reflect", "../../../../dist/declarations/reflect-declarations.json ")
    }
*/

    private fun snapshotAPIAndCompare(basePath: String, jarPrefix: String, kotlinJvmMappingsPath: List<String>, publicPackages: List<String> = emptyList()) {
        val base = File(basePath).absoluteFile.normalize()
        val jarFile = getJarPath(base, jarPrefix)
        val kotlinJvmMappingsFiles = kotlinJvmMappingsPath.map(base::resolve)

        println("Reading kotlin visibilities from $kotlinJvmMappingsFiles")
        val publicPackagePrefixes = publicPackages.map { it.replace('.', '/') + '/' }
        val visibilities =
                kotlinJvmMappingsFiles
                        .map { readKotlinVisibilities(it).filterKeys { name -> publicPackagePrefixes.none { name.startsWith(it) } } }
                        .reduce { m1, m2 -> m1 + m2 }

        println("Reading binary API from $jarFile")
        val api = getBinaryAPI(JarFile(jarFile), visibilities).filterOutNonPublic()

        val target = File("reference-public-api")
                .resolve(testName.methodName.replaceCamelCaseWithDashedLowerCase() + ".txt")

        api.dumpAndCompareWith(target)
    }

    private fun getJarPath(base: File, jarPrefix: String): File {
        val files = (base.listFiles() ?: throw Exception("Cannot list files in $base"))
            .filter { it.name.let {
                it.startsWith(jarPrefix) && it.endsWith(".jar")
                    && !it.endsWith("-sources.jar")
                    && !it.endsWith("-javadoc.jar") }}

        return files.singleOrNull() ?: throw Exception("No single file matching $jarPrefix in $base: $files")
    }

}

