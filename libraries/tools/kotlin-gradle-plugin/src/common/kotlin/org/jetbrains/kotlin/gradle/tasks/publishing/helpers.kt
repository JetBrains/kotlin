/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.publishing

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_BOUNCY_CASTLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.io.File

internal fun Project.addPgpSignatureHelpers() {
    val bcConfiguration = maybeCreateBcConfiguration()

    val pgpDirectory = project.layout.buildDirectory.dir("pgp")
    project.tasks.register("generatePgpKeys", GeneratePgpKeys::class.java) {
        it.notCompatibleWithConfigurationCache("Do not cache password.")
        it.outputDirectory.set(pgpDirectory)
        it.password.set(project.providers.gradleProperty("signing.password"))
        it.bouncyCastleClasspath.from(bcConfiguration)
        it.gradleHomePath.set(project.gradle.gradleUserHomeDir.absolutePath)
        it.group = "signing"
        it.description = """
            Generates a new PGP keypair.
            
            Usage: 
            gradlew generatePgpKeys --name "Jane Doe <janedoe@example.com>" --password YOUR_PASSWORD
        """.trimIndent()
    }

    if (!gradle.startParameter.isOffline) {
        project.tasks.register("uploadPublicPgpKey", UploadPgpKeyTask::class.java) {
            it.keyserver.set("https://keyserver.ubuntu.com")
            it.group = "signing"
            it.description = "Uploads the public PGP key to a keyserver"
        }
    }
}

internal fun Project.addSigningValidationHelpers() {
    val bcConfiguration = maybeCreateBcConfiguration()
    val signingTask = project.tasks.register<CheckSigningTask>("checkSigningConfiguration") {
        group = "validation"
        description = "Checks that a signing configuration is set up correctly."
        bouncyCastleClasspath.from(bcConfiguration)
        offlineMode.set(gradle.startParameter.isOffline)
        keyservers.convention(
            listOf(
                "https://keys.openpgp.org",
                "https://keyserver.ubuntu.com",
//              A third server is listed in the Sonatype docs,
//              but it often returns 502 errors when getting keys so we're not going to use it.
//                "https://pgp.mit.edu",
            )
        )
    }
    project.pluginManager.withPlugin("signing") {
        project.getExtension<SigningExtension>("signing")?.let { signing ->
            signingTask.configure { task ->
                task.gradleHomePath.set(project.gradle.gradleUserHomeDir.absolutePath)
                val signatory = try {
                    signing.signatory
                } catch (e: Exception) {
                    logger.warn("Failed to create signatory: ${e.message}")
                    null
                }
                if (signatory != null) {
                    task.signatory.set(signatory)
                }
                if (GradleVersion.current() < GradleVersion.version("8.1")) {
                    task.notCompatibleWithConfigurationCache("checkSigningConfiguration task is not compatible with configuration cache on Gradle versions < 8.1.")
                }
                task.keyId.set(providers.gradleProperty("signing.keyId"))
                task.keyringPath.set(layout.file(providers.gradleProperty("signing.secretKeyRingFile").map { File(it) }))
                task.hasKeyPassword.set(providers.gradleProperty("signing.password").map { true })
                signatory?.keyId?.let { task.signatoryKeyId.set(it) }
            }
        }
        project.pluginManager.withPlugin("maven-publish") {
            project.getExtension<PublishingExtension>("publishing")?.let { publishing ->
                afterEvaluate {
                    val publicationSigning = publishing.publications.filterIsInstance<MavenPublication>().associate {
                        it.name to (tasks.withType<Sign>().findByName("sign${it.name.capitalizeAsciiOnly()}Publication") != null)
                    }
                    signingTask.configure { it.publicationNamesWithSigning.set(publicationSigning) }
                }
            }
        }
    }
}

internal fun Project.addPomValidationHelpers() {
    project.pluginManager.withPlugin("maven-publish") {
        project.getExtension<PublishingExtension>("publishing")?.let { publishing ->
            publishing.publications.withType<MavenPublication>().configureEach { publication ->
                val capitalizedPublicationName = "${publication.name.capitalizeAsciiOnly()}Publication"
                val generatePomTaskName = "generatePomFileFor$capitalizedPublicationName"
                val generatePomTask = tasks.withType<GenerateMavenPom>().named(generatePomTaskName)
                project.tasks.register<CheckPomTask>("checkPomFileFor${publication.name.capitalizeAsciiOnly()}Publication") {
                    dependsOn(generatePomTask)
                    group = "validation"
                    pom.set(generatePomTask.map { it.destination })
                }
            }
        }
    }
}

internal fun keyIdToHex(keyId: Long) =
    keyId.toULong().toString(16).uppercase()
        .padStart(16, '0')


private fun Project.maybeCreateBcConfiguration(): Configuration {
    return project.configurations.maybeCreateResolvable(KOTLIN_BOUNCY_CASTLE_CONFIGURATION_NAME) {
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.JAR))
        description = "Bouncy Castle dependencies used internally for library publishing validation tasks. Not used during compilation."
        defaultDependencies {
            it.add(
                project.dependencies.create("org.bouncycastle:bcpkix-jdk18on:1.80")
            )
            it.add(
                project.dependencies.create("org.bouncycastle:bcpg-jdk18on:1.80")
            )
        }
    }
}