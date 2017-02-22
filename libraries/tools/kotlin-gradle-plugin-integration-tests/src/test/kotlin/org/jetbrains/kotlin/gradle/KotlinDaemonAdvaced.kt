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

import org.jetbrains.kotlin.compilerRunner.COULD_NOT_CONNECT_TO_DAEMON_MESSAGE
import org.jetbrains.kotlin.compilerRunner.CREATED_SESSION_FILE_PREFIX
import org.jetbrains.kotlin.compilerRunner.DELETED_SESSION_FILE_PREFIX
import org.jetbrains.kotlin.compilerRunner.EXISTING_SESSION_FILE_PREFIX
import org.junit.Assert
import org.junit.Test

// todo: test client file creation/deletion
// todo: test daemon start (does not start every build)
// todo: test daemon shutdown when gradle daemon dies
class KotlinDaemonAdvaced : BaseGradleIT() {
    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

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

        val project = Project("multiprojectWithDependency", GRADLE_VERSION)
        val strategyCLIArg = "-Dkotlin.compiler.execution.strategy=daemon"

        fun checkAfterNonIncrementalBuild(output: String) {
            val createdSessions = output.findAllStringsPrefixed(CREATED_SESSION_FILE_PREFIX)
            assert(createdSessions.size == 1) { "Created multiple sessions per build ${createdSessions.joinToString()}" }

            val existingSessions = output.findAllStringsPrefixed(EXISTING_SESSION_FILE_PREFIX)
            Assert.assertArrayEquals("Existing sessions don't match created sessions for two module projects", createdSessions, existingSessions)

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
}