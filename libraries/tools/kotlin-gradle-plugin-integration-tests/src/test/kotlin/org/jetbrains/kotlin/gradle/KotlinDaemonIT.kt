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

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.tooling.internal.consumer.ConnectorServices
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths
import kotlin.test.assertTrue

// todo: test client file creation/deletion
// todo: test daemon start (does not start every build)
// todo: test daemon shutdown when gradle daemon dies
@DisplayName("Kotlin daemon base behaviour")
class KotlinDaemonIT : KGPDaemonsBaseTest() {

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions
        .copy(
            logLevel = LogLevel.DEBUG
        )

    @DisplayName("Kotlin daemon is reused in multiproject")
    @GradleTest
    fun testDaemonMultiproject(gradleVersion: GradleVersion) {
        project("multiprojectWithDependency", gradleVersion) {
            gradleProperties.append(
                "\nkotlin.compiler.execution.strategy=daemon"
            )

            build("build") {
                assertOutputDoesNotContain(COULD_NOT_CONNECT_TO_DAEMON_MESSAGE)
                assertKotlinDaemonReusesOnlyOneSession()
            }

            build("clean", "build") {
                assertOutputDoesNotContain(COULD_NOT_CONNECT_TO_DAEMON_MESSAGE)
                assertKotlinDaemonReusesOnlyOneSession()
            }

            build("build") {
                assert(output.findAllStringsPrefixed(CREATED_SESSION_FILE_PREFIX).isEmpty()) {
                    "Sessions should not be created (incremental build)"
                }

                assert(output.findAllStringsPrefixed(EXISTING_SESSION_FILE_PREFIX).isEmpty()) {
                    "Sessions should not be existing (incremental build)"
                }

                assert(output.findAllStringsPrefixed(DELETED_SESSION_FILE_PREFIX).isEmpty()) {
                    "Sessions should not be deleted (incremental build)"
                }
            }
        }
    }

    @DisplayName("Client file is deleted on exit")
    @GradleTest
    fun testClientFileIsDeletedOnExit(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            build("assemble") {
                val regex = "(?m)($CREATED_CLIENT_FILE_PREFIX|$EXISTING_CLIENT_FILE_PREFIX)(.+)$".toRegex()
                val clientFiles = regex.findAll(output).map { Paths.get(it.groupValues[2]) }.toList()
                assert(clientFiles.isNotEmpty()) { "No client files in log" }

                // Stop daemons
                ConnectorServices.reset()
                Thread.sleep(2000) // Kotlin daemon stops itself after 1 sec of Gradle daemon
                clientFiles.forEach { clientFile ->
                    assertFileNotExists(clientFile)
                }
            }
        }
    }

    @DisplayName("Gradle build classpath should not leak into Kotlin daemon")
    @GradleTest
    fun testGradleBuildClasspathShouldNotBeLeakedIntoDaemonClasspath(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            build("assemble") {
                assertGradleClasspathNotLeaked()
            }
        }
    }

    private fun BuildResult.assertGradleClasspathNotLeaked() {
        assertOutputContains("Kotlin compiler classpath:")
        val daemonClasspath = output
            .lineSequence()
            .first {
                it.contains("Kotlin compiler classpath:")
            }
        assertTrue("Daemon classpath contains embeddable daemon jar leaked from Gradle dist classpath: $daemonClasspath") {
            !daemonClasspath.contains(".gradle/wrapper/dists")
        }
    }

    private fun BuildResult.assertKotlinDaemonReusesOnlyOneSession() {
        val createdSessions = output.findAllStringsPrefixed(CREATED_SESSION_FILE_PREFIX)
        assert(createdSessions.size == 1) {
            """
            |${printBuildOutput()}
            |
            |Created multiple sessions per build: ${createdSessions.joinToString()}
            """.trimMargin()
        }

        val existingSessions = output.findAllStringsPrefixed(EXISTING_SESSION_FILE_PREFIX)
        assert(createdSessions == createdSessions) {
            """
            |${printBuildOutput()}
            |
            |Existing sessions don't match created sessions for two module projects.
            |Created sessions:
            |${createdSessions.joinToString(separator = "\n")}
            |Existing sessions:
            |${existingSessions.joinToString(separator = "\n")}
            """.trimMargin()
        }

        val deletedSessions = output.findAllStringsPrefixed(DELETED_SESSION_FILE_PREFIX)
        assert(createdSessions == deletedSessions) {
            """
            |${printBuildOutput()}
            |
            |Deleted sessions don't match created sessions.
            |Created sessions:
            |${createdSessions.joinToString(separator = "\n")}
            |Deleted sessions:
            |${deletedSessions.joinToString(separator = "\n")}
            """.trimMargin()
        }
    }

    private fun String.findAllStringsPrefixed(prefix: String): List<String> =
        ("$prefix([^\\r\\n]*)").toRegex().findAll(this).map { it.groupValues[1] }.toList()
}