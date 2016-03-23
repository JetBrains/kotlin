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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.util.jar.JarFile

class RuntimePublicAPITest {

    @[Rule JvmField]
    val testName = TestName()

    @Test fun kotlinRuntime() {
        snapshotAPIAndCompare("../../tools/runtime/target", "kotlin-runtime-0.1-SNAPSHOT.jar", "runtime-declarations.json")
    }

    @Test fun kotlinStdlib() {
        snapshotAPIAndCompare("../../stdlib/target", "kotlin-stdlib-0.1-SNAPSHOT.jar", "stdlib-declarations.json")
    }

    private fun snapshotAPIAndCompare(basePath: String, jarPath: String, kotlinJvmMappingsPath: String) {
        val base = File(basePath).absoluteFile
        val visibilities = readKotlinVisibilities(base.resolve(kotlinJvmMappingsPath))

        // TODO: List jars since version can differ
        // TODO: Excluded package list
        val api = getBinaryAPI(JarFile(base.resolve(jarPath)), visibilities).filterOutNonPublic()

        val target = File("src/test/resources/output")
                .resolve(testName.methodName.replaceCamelCaseWithDashedLowerCase() + ".txt")

        api.dumpAndCompareWith(target)
    }

}

