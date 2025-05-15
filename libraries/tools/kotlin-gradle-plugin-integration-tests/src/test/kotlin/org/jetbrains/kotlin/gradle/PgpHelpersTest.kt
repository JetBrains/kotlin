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
import io.ktor.util.pipeline.*
import kotlinx.coroutines.runBlocking
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.awaitInitialization
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.security.Security
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OtherGradlePluginTests
@DisplayName("PGP signing helper tasks")
class PgpHelpersTest : KGPBaseTest() {

    @GradleTest
    @DisplayName("Should complain about missing name parameter")
    internal fun shouldComplainAboutMissingName(gradleVersion: GradleVersion) {
        projectWithJvmPlugin(gradleVersion) {
            buildAndFail("generatePgpKeys", "--password=abc") {
                assertOutputContains("You must provide a value for the '--name' command line option, e.g. --name \"Jane Doe <janedoe@example.com>\"")
            }
        }
    }

    @GradleTest
    @DisplayName("Should complain about missing password parameter")
    internal fun shouldComplainAboutMissingPassword(gradleVersion: GradleVersion) {
        projectWithJvmPlugin(gradleVersion) {
            buildAndFail("generatePgpKeys", "--name='Jane Doe <janedoe@example.com>'") {
                assertOutputContains("You must provide a value for either the '--password' command line option or the 'signing.password' Gradle property")
            }
        }
    }

    @GradleTest
    @DisplayName("Should read password supplied through CLI option")
    internal fun passwordSuppliedThroughOption(gradleVersion: GradleVersion) {
        projectWithJvmPlugin(gradleVersion) {
            build("generatePgpKeys", "--name='Jane Doe <janedoe@example.com>'", "--password=abc") {
                assertPgpKeysWereGenerated()
            }
        }
    }

    @GradleTest
    @DisplayName("Should read password supplied through project property")
    internal fun passwordSuppliedThroughGradleProperty(gradleVersion: GradleVersion) {
        projectWithJvmPlugin(gradleVersion) {
            build("generatePgpKeys", "--name='Jane Doe <janedoe@example.com>'", "-Psigning.password=abc") {
                assertPgpKeysWereGenerated()
            }
        }
    }

    @GradleTest
    @DisplayName("Should not leak BouncyCastle dependency")
    internal fun shouldNotLeakBouncyCastleDependency(gradleVersion: GradleVersion) {
        projectWithJvmPlugin(gradleVersion) {
            val classesLeaked = buildScriptReturn {
                try {
                    this::class.java.classLoader.loadClass("org.bouncycastle.openpgp.operator.PGPContentSignerBuilder")
                    return@buildScriptReturn true
                } catch (_: ClassNotFoundException) {
                    // ignore
                }
                if (Security.getProvider("BC") != null) {
                    return@buildScriptReturn true
                }
                false
            }.buildAndReturn("generatePgpKeys", "--name='Jane Doe <janedoe@example.com>'", "-Psigning.password=abc")

            assertFalse(classesLeaked)
        }
    }

    @GradleTest
    @DisplayName("Should upload public key to server")
    internal fun uploadPublicKeyToServer(gradleVersion: GradleVersion) {
        projectWithJvmPlugin(gradleVersion) {
            build(
                "generatePgpKeys",
                "--name='Jane Doe <janedoe@example.com>'",
                "-Psigning.password=abc",
            )
            val parameters = mutableListOf<Parameters>()

            runWithKtorService(
                {
                    val formParameters: Parameters = call.receiveParameters()
                    parameters += formParameters
                    call.respond(HttpStatusCode.OK)
                }
            ) { port ->
                build(
                    "uploadPublicPgpKey",
                    "--keyring",
                    findGeneratedPublicKeyAsc().absolutePathString(),
                    "--keyserver",
                    "http://localhost:$port",
                )
            }
            assert(parameters.size == 1) { "Exactly one request must be sent to the server, but the number of requests was: ${parameters.size}" }
            val params = parameters.single()
            assertEquals("nm", params["options"])
            assertEquals(findGeneratedPublicKeyAsc().readText(), params["keytext"])
        }
    }

    @GradleTest
    @DisplayName("Should fail task when upload public key to server fails")
    internal fun failedUploadPublicKeyToServer(gradleVersion: GradleVersion) {
        projectWithJvmPlugin(gradleVersion) {
            build(
                "generatePgpKeys",
                "--name='Jane Doe <janedoe@example.com>'",
                "-Psigning.password=abc",
            )
            runWithKtorService(
                {
                    call.respond(HttpStatusCode.BadRequest, "Some reason")
                }
            ) { port ->
                buildAndFail(
                    "uploadPublicPgpKey",
                    "--keyring",
                    findGeneratedPublicKeyAsc().absolutePathString(),
                    "--keyserver",
                    "http://localhost:$port",
                ) {
                    assertOutputContains("Failed to upload public key. Server returned:\nSome reason")
                }
            }
        }
    }

    @GradleTest
    @DisplayName("Use generated key from Gradle signing plugin")
    internal fun useGeneratedKeyInSigningPlugin(gradleVersion: GradleVersion) {
        project("empty", gradleVersion, buildOptions = BuildOptions().disableConfigurationCacheForGradle7(gradleVersion)) {
            plugins {
                kotlin("jvm")
                `maven-publish`
                signing
            }
            var keyId: String? = null
            var keyringPath: String? = null
            build("generatePgpKeys", "--name='Jane Doe <janedoe@example.com>'", "-Psigning.password=abc") {
                assertPgpKeysWereGenerated()
                // need to match key ID from this output: "The key ID of the generated key is 'XXXXXXXX'."
                keyId = output.substringAfter("The key ID of the generated key is '").substringBefore("'")
                keyringPath = "build/pgp/secret_$keyId.gpg"
            }
            assertNotNull(keyId)
            assertNotNull(keyringPath)

            buildScriptInjection {
                project.group = "someGroup"
                project.version = "1.0.0"

                publishing.repositories {
                    it.maven(project.layout.buildDirectory.dir("repo")) {
                        name = "repo"
                    }
                }
                publishing.publications {
                    it.create<MavenPublication>("mavenJava")
                }
                signing.sign(publishing.publications["mavenJava"])
            }
            build("publish", "-Psigning.keyId=$keyId", "-Psigning.password=abc", "-Psigning.secretKeyRingFile=$keyringPath") {
                assertTasksExecuted(":signMavenJavaPublication")
            }
        }
    }

    private fun TestProject.assertPgpKeysWereGenerated() {
        val expectedFileNames =
            listOf("secret_" to ".gpg", "secret_" to ".asc", "public_" to ".gpg", "public_" to ".asc", "example_" to ".properties")
        val actualFileNames = projectPath.resolve("build/pgp").listDirectoryEntries().map { it.fileName.toString() }
        for (expected in expectedFileNames) {
            assertTrue("File ${expected.first}X${expected.second} not found (where X is key ID).") {
                actualFileNames.any { actual -> actual.startsWith(expected.first) && actual.endsWith(expected.second) }
            }
        }
    }

    private fun TestProject.findGeneratedPublicKeyAsc(): Path {
        return projectPath.resolve("build/pgp").listDirectoryEntries()
            .single { it.fileName.toString().startsWith("public_") && it.fileName.toString().endsWith(".asc") }
    }

    private fun runWithKtorService(
        addEndpointAction: PipelineInterceptor<Unit, ApplicationCall>,
        action: (Int) -> Unit,
    ) {
        var server: ApplicationEngine? = null
        try {
            server = embeddedServer(CIO, host = "localhost", port = 0)
            {
                routing {
                    get("/isReady") {
                        call.respond(HttpStatusCode.OK)
                    }
                    post("/pks/add", addEndpointAction)
                }
            }.start()
            val port = runBlocking { server.resolvedConnectors().single().port }
            awaitInitialization(port)
            action(port)
        } finally {
            server?.stop(1000, 1000)
        }
    }

    private fun projectWithJvmPlugin(gradleVersion: GradleVersion, test: TestProject.() -> Unit) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("jvm")
            }
            test()
        }
    }
}