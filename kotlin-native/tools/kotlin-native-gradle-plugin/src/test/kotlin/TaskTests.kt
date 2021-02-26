/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin.test

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskTests {
    val tmpFolder = TemporaryFolder()
        @Rule get

    val projectDirectory: File
        get() = tmpFolder.root

    @Test
    fun `Plugin should support separate run tasks for different binaries`() {
        val project = KonanProject.createEmpty(projectDirectory).apply {
            buildFile.appendText("""
                konanArtifacts {
                    program('foo') {
                        srcDir 'src/foo/kotlin'
                    }
                    program('bar') {
                        srcDir 'src/bar/kotlin'
                    }
                }
            """.trimIndent())
        }
        project.generateSrcFile(
                listOf("src", "foo", "kotlin"),
                "main.kt",
                "fun main(args: Array<String>) = println(\"Run Foo: \${args[0]}, \${args[1]}\")")
        project.generateSrcFile(
                listOf("src", "bar", "kotlin"),
                "main.kt",
                "fun main(args: Array<String>) = println(\"Run Bar: \${args[0]}, \${args[1]}\")")
        val resultFoo = project.createRunner()
                .withArguments("runFoo", "-PrunArgs=arg1 arg2")
                .build()
        val resultAll = project.createRunner()
                .withArguments("run", "-PrunArgs=arg1 arg2")
                .build()

        assertTrue(resultFoo.output.contains("Run Foo: arg1, arg2"), "No Foo output for 'runFoo'")
        assertFalse(resultFoo.output.contains("Run Bar: "), "There is Bar output for 'runFoo'")
        assertTrue(resultAll.output.contains("Run Foo: arg1, arg2"), "No Foo output for 'run'")
        assertTrue(resultAll.output.contains("Run Bar: arg1, arg2"), "No Bar output for 'run'")
    }
}