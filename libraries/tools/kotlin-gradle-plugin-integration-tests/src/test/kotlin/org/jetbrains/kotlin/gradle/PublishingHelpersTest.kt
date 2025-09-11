/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.tasks.publishing.CheckSigningTask
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.awaitInitialization
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

@OtherGradlePluginTests
@DisplayName("Library publishing helper tasks")
class PublishingHelpersTest : KGPBaseTest() {
    @GradleTest
    @DisplayName("Verify generated pom.xml")
    internal fun verifyGeneratedPom(gradleVersion: GradleVersion) {
        projectWithJvmPlugin(gradleVersion) {
            buildScriptInjection {
                project.group = "someGroup"
                project.version = "1.0.0"

                publishing.repositories {
                    it.maven(project.layout.buildDirectory.dir("repo")) {
                        name = "repo"
                    }
                }
                publishing.publications.create<MavenPublication>("mavenJava") {
                    pom {
                        it.name.set("My Library")
                        it.description.set("A concise description of my library")
                        it.url.set("http://www.example.com/library")
                        it.licenses { licenses ->
                            licenses.license { license ->
                                license.name.set("The Apache License, Version 2.0")
                                license.url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        it.developers { developers ->
                            developers.developer { developer ->
                                developer.name.set("John Doe")
                                developer.email.set("john.doe@example.com")
                                developer.organization.set("Example")
                                developer.organizationUrl.set("http://example.com")
                            }
                        }
                        it.scm { scm ->
                            scm.connection.set("scm:git:git://example.com/my-library.git")
                            scm.developerConnection.set("scm:git:ssh://example.com/my-library.git")
                            scm.url.set("https://example.com/my-library/")
                        }
                    }
                }
            }
            build("checkPomFileForMavenJavaPublication") {
                assertTasksExecuted(":generatePomFileForMavenJavaPublication")
            }
        }
    }

    @GradleTest
    @DisplayName("Should fail verification of pom.xml with missing tags")
    internal fun shouldFailVerificationGeneratedPom(gradleVersion: GradleVersion) {
        projectWithJvmPlugin(gradleVersion) {
            buildScriptInjection {
                project.group = "someGroup"
                project.version = "1.0.0"

                publishing.repositories {
                    it.maven(project.layout.buildDirectory.dir("repo")) {
                        name = "repo"
                    }
                }
                publishing.publications.create<MavenPublication>("mavenJava")
            }
            buildAndFail("checkPomFileForMavenJavaPublication") {
                assertTasksFailed(":checkPomFileForMavenJavaPublication")
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.PomMisconfigured,
                    """
                    Missing tags in POM:
                    * <name>
                    * <description>
                    * <url>
                    * <licenses>
                    * <licenses> - <license>
                    * <licenses> - <license> - <name>
                    * <licenses> - <license> - <url>
                    * <developers>
                    * <developers> - <developer>
                    * <developers> - <developer> - <name>
                    * <developers> - <developer> - <email>
                    * <developers> - <developer> - <organization>
                    * <developers> - <developer> - <organizationUrl>
                    * <scm>
                    * <scm> - <connection>
                    * <scm> - <developerConnection>
                    * <scm> - <url>
                """.trimIndent()
                )
            }
        }
    }

    @GradleTest
    @DisplayName("Verify signing is configured")
    internal fun verifySigningConfigured(gradleVersion: GradleVersion) {
        checkSigningConfigurationTest(gradleVersion) { buildArguments ->
            buildScriptInjection {
                signing.sign(publishing.publications)
            }
            build(*buildArguments) {
                assertTasksExecuted(":checkSigningConfiguration")
            }
        }
    }

    @GradleTest
    @DisplayName("Signing check should fail when no publications are signed")
    internal fun shouldFailSigningCheckWhenNoPublicationsSigned(gradleVersion: GradleVersion) {
        checkSigningConfigurationTest(gradleVersion) { buildArguments ->
            buildAndFail(*buildArguments) {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.SigningMisconfigured,
                    "No publications are signed. Publishing to Maven Central will fail validation."
                )
            }
        }
    }

    @GradleTest
    @DisplayName("Signing check warn when some publications are not signed")
    internal fun shouldWarnWhenSomePublicationsNotSigned(gradleVersion: GradleVersion) {
        checkSigningConfigurationTest(gradleVersion) { buildArguments ->
            buildScriptInjection {
                signing.sign(publishing.publications["mavenJava"])
                publishing.publications.create<MavenPublication>("abc")
            }

            build(*buildArguments) {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.SomePublicationsNotSigned, """
                    Configure signing for the following publications if you plan to publish them to Maven Central: abc
                """.trimIndent()
                )
            }
        }
    }

    @GradleTest
    @DisplayName("Should show info about missing key ID")
    internal fun shouldShowInfoAboutMissingKeyId(gradleVersion: GradleVersion) {
        checkSigningConfigurationTest(gradleVersion) { buildArguments ->
            buildScriptInjection {
                signing.sign(publishing.publications)
            }
            buildAndFail(*buildArguments.filter { !it.startsWith("-Psigning.keyId") }.toTypedArray()) {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.SigningMisconfigured,
                    "'signing.keyId' is not set. Please ensure you have the 'signing.keyId' property set to your key's ID."
                )
            }
        }
    }

    @GradleTest
    @DisplayName("Should show info about missing keyring path")
    internal fun shouldShowInfoAboutMissingKeyringPath(gradleVersion: GradleVersion) {
        checkSigningConfigurationTest(gradleVersion) { buildArguments ->
            buildScriptInjection {
                signing.sign(publishing.publications)
            }
            buildAndFail(*buildArguments.filter { !it.startsWith("-Psigning.secretKeyRingFile") }.toTypedArray()) {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.SigningMisconfigured,
                    "* 'signing.secretKeyRingFile' is not set. Please ensure you have the 'signing.secretKeyRingFile' property set to your keyring's file path."
                )
            }
        }
    }

    @GradleTest
    @DisplayName("Should fail signing check when key not uploaded")
    internal fun shouldFailSigningCheckWhenKeyNotUploaded(gradleVersion: GradleVersion) {
        checkSigningConfigurationTest(gradleVersion) { buildArguments ->
            buildScriptInjection {
                signing.sign(publishing.publications)
                project.tasks.named<CheckSigningTask>("checkSigningConfiguration").configure {
                    it.keyservers.set(emptyList())
                }
            }
            buildAndFail(*buildArguments) {
                assertOutputContains("None of the keyservers contain the public key with id:")
            }
        }
    }

    @GradleTest
    @Disabled("For manual testing only: needs external GnuPG program and key setup (replace keyName and passphrase in test)")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_1],
    )
    @DisplayName("Signing check works with configuration cache and GPG signatory")
    internal fun signingCheckWorksWithConfigurationCacheAndGpg(gradleVersion: GradleVersion) {
        checkSigningConfigurationTest(gradleVersion) { buildArguments ->
            buildScriptInjection {
                signing.useGpgCmd()
                signing.sign(publishing.publications)
                project.tasks.named<CheckSigningTask>("checkSigningConfiguration").configure {
                    it.keyservers.set(emptyList())
                }
            }
            fun buildWithConfCacheCheck(firstRun: Boolean) {
                buildAndFail(
                    *buildArguments,
                    "-Psigning.gnupg.keyName=48153D94",
                    "-Psigning.gnupg.passphrase=testtesttest",
                ) {
                    if (gradleVersion >= GradleVersion.version("8.1")) {
                        if (firstRun) {
                            assertConfigurationCacheStored()
                        } else {
                            assertConfigurationCacheReused()
                        }
                    }
                    assertOutputDoesNotContain("Plugin 'org.jetbrains.kotlin.jvm': external process started 'gpg")
                    assertOutputContains("None of the keyservers contain the public key with id: 48153D94")
                }
            }
            buildWithConfCacheCheck(firstRun = true)
            buildWithConfCacheCheck(firstRun = false)
        }
    }

    @GradleTest
    @DisplayName("Signing check works with configuration cache and without GPG signatory")
    internal fun signingCheckWorksWithConfigurationCacheAndWithoutGpg(gradleVersion: GradleVersion) {
        checkSigningConfigurationTest(gradleVersion) { buildArguments ->
            buildScriptInjection {
                signing.sign(publishing.publications)
                project.tasks.named<CheckSigningTask>("checkSigningConfiguration").configure {
                    it.keyservers.set(emptyList())
                }
            }
            buildAndFail(
                *buildArguments,
            ) {
                if (gradleVersion >= GradleVersion.version("8.1")) {
                    assertConfigurationCacheStored()
                }
                assertOutputContains("None of the keyservers contain the public key with id:")
            }
        }
    }


    private fun checkSigningConfigurationTest(
        gradleVersion: GradleVersion,
        buildScriptCode: TestProject.(buildArguments: Array<String>) -> Unit,
    ) {
        projectWithJvmPlugin(gradleVersion) {
            build("generatePgpKeys", "--name='Jane Doe <janedoe@example.com>'", "--password=abc")
            val keyring = findGeneratedKey("secret_", ".gpg")
            val keyId = keyring.fileName.toString().substringAfter("secret_").substringBefore(".gpg")

            runWithKtorService(
                routingSetup = {
                    get("/pks/lookup") {
                        if (call.request.queryParameters["op"] == "get" && call.request.queryParameters["search"]?.startsWith("0x") == true) {
                            call.respond(HttpStatusCode.OK, "<ASCII KEY TEXT HERE>")
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "Wrong parameters for getting key.")
                        }
                    }
                }) { port ->
                buildScriptInjection {
                    publishing.publications.create<MavenPublication>("mavenJava")
                    project.tasks.named<CheckSigningTask>("checkSigningConfiguration").configure {
                        it.keyservers.set(listOf("http://localhost:$port"))
                    }
                }
                buildScriptCode(
                    arrayOf(
                        "checkSigningConfiguration",
                        "-Psigning.keyId=$keyId",
                        "-Psigning.secretKeyRingFile=$keyring",
                        "-Psigning.password=abc"
                    )
                )
            }
        }
    }

    private fun TestProject.findGeneratedKey(prefix: String = "public_", suffix: String = ".asc"): Path {
        return projectPath.resolve("build/pgp").listDirectoryEntries()
            .single { it.fileName.toString().startsWith(prefix) && it.fileName.toString().endsWith(suffix) }
    }

    private fun runWithKtorService(
        routingSetup: Routing.() -> Unit,
        action: (Int) -> Unit,
    ) {
        var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
        try {
            server = embeddedServer(CIO, host = "localhost", port = 0) {
                routing {
                    get("/isReady") {
                        call.respond(HttpStatusCode.OK)
                    }
                    routingSetup()
                }
            }.start()
            val port = runBlocking { server.engine.resolvedConnectors().single().port }
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
                `maven-publish`
                signing
            }
            test()
        }
    }
}