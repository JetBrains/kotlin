/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.PipelineInterceptor
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.awaitInitialization
import org.jetbrains.kotlin.gradle.util.getEmptyPort
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.collections.plusAssign
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.test.assertEquals

@OtherGradlePluginTests
@DisplayName("PGP signing helper tasks")
class PgpHelpersTest : KGPBaseTest() {

    @GradleTest
    @DisplayName("Should complain about missing name parameter")
    internal fun shouldComplainAboutMissingName(gradleVersion: GradleVersion) {
        project(
            projectName = "signingHelpers",
            gradleVersion = gradleVersion,
        ) {
            buildAndFail("generatePgpKeys", "--password=abc") {
                assertOutputContains("You must provide a value for the '--name' command line option, e.g. --name \"Jane Doe <janedoe@example.com>\"")
            }
        }
    }

    @GradleTest
    @DisplayName("Should complain about missing password parameter")
    internal fun shouldComplainAboutMissingPassword(gradleVersion: GradleVersion) {
        project(
            projectName = "signingHelpers",
            gradleVersion = gradleVersion,
        ) {
            buildAndFail("generatePgpKeys", "--name='Jane Doe <janedoe@example.com>'") {
                assertOutputContains("You must provide a value for either the '--password' command line option or the 'signing.password' Gradle property")
            }
        }
    }

    @GradleTest
    @DisplayName("Should read password supplied through CLI option")
    internal fun passwordSuppliedThroughOption(gradleVersion: GradleVersion) {
        project(
            projectName = "signingHelpers",
            gradleVersion = gradleVersion,
        ) {
            build("generatePgpKeys", "--name='Jane Doe <janedoe@example.com>'", "--password=abc") {
                assertPgpKeysWereGenerated(projectPath)
            }
        }
    }

    @GradleTest
    @DisplayName("Should read password supplied through project property")
    internal fun passwordSuppliedThroughGradleProperty(gradleVersion: GradleVersion) {
        project(
            projectName = "signingHelpers",
            gradleVersion = gradleVersion,
        ) {
            build("generatePgpKeys", "--name='Jane Doe <janedoe@example.com>'", "-Psigning.password=abc") {
                assertPgpKeysWereGenerated(projectPath)
            }
        }
    }

    @GradleTest
    @DisplayName("Should not leak BouncyCastle dependency")
    internal fun shouldNotLeakBouncyCastleDependency(gradleVersion: GradleVersion) {
        project(
            projectName = "signingHelpersClassloaderIsolation",
            gradleVersion = gradleVersion,
        ) {
            build("generatePgpKeys", "--name='Jane Doe <janedoe@example.com>'", "-Psigning.password=abc") {
                assertOutputContains("PGPContentSignerBuilder class not found")
                assertOutputContains("Security provider is: null")
            }
        }
    }

    @GradleTest
    @DisplayName("Should upload public key to server")
    internal fun uploadPublicKeyToServer(gradleVersion: GradleVersion) {
        project(
            projectName = "signingHelpers",
            gradleVersion = gradleVersion,
        ) {
            build(
                "generatePgpKeys",
                "--name='Jane Doe <janedoe@example.com>'",
                "-Psigning.password=abc",
            )
            val parameters = mutableListOf<Parameters>()

            runWithKtorService({
                                   val formParameters: Parameters = call.receiveParameters()
                                   parameters += formParameters
                                   call.respond(HttpStatusCode.OK)
                               }) { port ->
                build(
                    "uploadPublicPgpKey",
                    "--keyServer",
                    "http://localhost:$port",
                )
            }
            assert(parameters.size == 1) { "Exactly one request must be sent to the server, but the number of requests was: ${parameters.size}" }
            val params = parameters.single()
            assertEquals("nm", params["options"])
            assertEquals(projectPath.resolve("build/pgp/public.asc").readText(), params["keytext"])
        }
    }

    @GradleTest
    @DisplayName("Should fail task when upload public key to server fails")
    internal fun failedUploadPublicKeyToServer(gradleVersion: GradleVersion) {
        project(
            projectName = "signingHelpers",
            gradleVersion = gradleVersion,
        ) {
            build(
                "generatePgpKeys",
                "--name='Jane Doe <janedoe@example.com>'",
                "-Psigning.password=abc",
            )
            runWithKtorService({
                                   call.respond(HttpStatusCode.BadRequest, "Some reason")
                               }) { port ->
                buildAndFail(
                    "uploadPublicPgpKey",
                    "--keyServer",
                    "http://localhost:$port",
                ) {
                    assertOutputContains("Failed to upload public key. Server returned:\nSome reason")
                }
            }
        }
    }

    private fun BuildResult.assertJdkHomeIsUsingJdk(
        javaexecPath: String,
    ) = assertOutputContains("[KOTLIN] Kotlin compilation 'jdkHome' argument: $javaexecPath")


    private fun BuildResult.assertPgpKeysWereGenerated(projectPath: Path) {
        val expectedFileNames = setOf("secret.gpg", "secret.asc", "public.gpg", "public.asc", "example.properties")
        val actualFileNames = projectPath.resolve("build/pgp").listDirectoryEntries().map { it.fileName.toString() }.toSet()
        assertEquals(expectedFileNames, actualFileNames)
    }

    companion object {
        private fun runWithKtorService(
            addEndpointAction: PipelineInterceptor<Unit, ApplicationCall>,
            action: (Int) -> Unit,
        ) {
            var server: ApplicationEngine? = null
            try {
                val port = getEmptyPort().localPort
                server = embeddedServer(CIO, host = "localhost", port = port)
                {
                    routing {
                        get("/isReady") {
                            call.respond(HttpStatusCode.OK)
                        }
                        post("/pks/add", addEndpointAction)
                    }
                }.start()
                awaitInitialization(port)
                action(port)
            } finally {
                server?.stop(1000, 1000)
            }
        }
    }
}