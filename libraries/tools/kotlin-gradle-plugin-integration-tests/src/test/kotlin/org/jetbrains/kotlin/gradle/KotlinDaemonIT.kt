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

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.compilerRunner.*
import org.junit.Assert
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

// todo: test client file creation/deletion
// todo: test daemon start (does not start every build)
// todo: test daemon shutdown when gradle daemon dies
class KotlinDaemonIT : BaseGradleIT() {

    @Test
    fun testDaemonMultiproject() {
        fun String.findAllStringsPrefixed(prefix: String): Array<String> {
            val result = arrayListOf<String>()
            val regex = ("$prefix([^\\r\\n]*)").toRegex()
            for (match in regex.findAll(this)) {
                result.add(match.groupValues[1])
            }
            return result.toTypedArray()
        }

        val project = Project("multiprojectWithDependency")
        val strategyCLIArg = "-Dkotlin.compiler.execution.strategy=daemon"

        fun checkAfterNonIncrementalBuild(output: String) {
            val createdSessions = output.findAllStringsPrefixed(CREATED_SESSION_FILE_PREFIX)
            assert(createdSessions.size == 1) { "Created multiple sessions per build ${createdSessions.joinToString()}" }

            val existingSessions = output.findAllStringsPrefixed(EXISTING_SESSION_FILE_PREFIX)
            Assert.assertArrayEquals(
                "Existing sessions don't match created sessions for two module projects",
                createdSessions,
                existingSessions
            )

            val deletedSessions = output.findAllStringsPrefixed(DELETED_SESSION_FILE_PREFIX)
            Assert.assertArrayEquals("Deleted sessions don't match created sessions", createdSessions, deletedSessions)
        }

        project.build("build", strategyCLIArg) {
            assertNotContains(COULD_NOT_CONNECT_TO_DAEMON_MESSAGE)
            checkAfterNonIncrementalBuild(output)
        }

        project.build("clean", "build", strategyCLIArg) {
            assertNotContains(COULD_NOT_CONNECT_TO_DAEMON_MESSAGE)
            checkAfterNonIncrementalBuild(output)
        }

        project.build("build", strategyCLIArg) {
            val createdSessions = output.findAllStringsPrefixed(CREATED_SESSION_FILE_PREFIX)
            Assert.assertArrayEquals("Sessions should not be created (incremental build)", createdSessions, emptyArray())

            val existingSessions = output.findAllStringsPrefixed(EXISTING_SESSION_FILE_PREFIX)
            Assert.assertArrayEquals("Sessions should not be existing (incremental build)", existingSessions, emptyArray())

            val deletedSessions = output.findAllStringsPrefixed(DELETED_SESSION_FILE_PREFIX)
            Assert.assertArrayEquals("Sessions should not be deleted (incremental build)", deletedSessions, emptyArray())
        }
    }

    @Test
    fun testClientFileIsDeletedOnExit() {
        val project = Project("kotlinProject")
        val options = defaultBuildOptions().copy(withDaemon = false)

        project.build("assemble", options = options) {
            val regex = Regex("(?m)($CREATED_CLIENT_FILE_PREFIX|$EXISTING_CLIENT_FILE_PREFIX)(.+)$")
            val clientFiles = regex.findAll(output).toList().map { File(it.groupValues[2]) }
            assert(clientFiles.isNotEmpty()) { "No client files in log" }
            clientFiles.forEach { clientFile ->
                assert(!clientFile.exists()) { "Client file $clientFile is expected to be deleted!" }
            }
        }
    }

    @Test
    fun testGradleBuildClasspathShouldNotBeLeakedIntoDaemonClasspath() {
        val testProject = Project("kotlinProject")
        testProject.setupWorkingDir()

        testProject.build("assemble") {
            assertGradleClasspathNotLeaked()
        }
    }

    private fun CompiledProject.assertGradleClasspathNotLeaked() {
        assertContains("Kotlin compiler classpath:")
        val daemonClasspath = output.lineSequence().find {
            it.contains("Kotlin compiler classpath:")
        }!!
        assertTrue("Daemon classpath contains embeddable daemon jar leaked from Gradle dist classpath: $daemonClasspath") {
            !daemonClasspath.contains(".gradle/wrapper/dists")
        }
    }
}