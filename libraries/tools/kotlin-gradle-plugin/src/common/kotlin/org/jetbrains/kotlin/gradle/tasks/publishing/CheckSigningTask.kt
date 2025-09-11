/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.publishing

import org.bouncycastle.bcpg.BCPGInputStream
import org.bouncycastle.openpgp.PGPSignature
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.plugins.signing.signatory.Signatory
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import javax.inject.Inject

@DisableCachingByDefault(because = "Result relies on a server call and has no outputs.")
abstract class CheckSigningTask @Inject internal constructor(private val workerExecutor: WorkerExecutor) : DefaultTask(),
    UsesKotlinToolingDiagnostics {

    // on-disk signatory properties

    @get:Input
    @get:Optional
    abstract val keyId: Property<String>

    @get:Input
    @get:Optional
    abstract val hasKeyPassword: Property<Boolean>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val keyringPath: RegularFileProperty

    // other properties

    @get:Input
    abstract val publicationNamesWithSigning: MapProperty<String, Boolean>

    @get:Input
    abstract val gradleHomePath: Property<String>

    /**
     * This is the short key ID used by the currently configured signatory.
     */
    @get:Input
    @get:Optional
    abstract val signatoryKeyId: Property<String>

    @get:Input
    @get:Optional
    abstract val signatory: Property<Signatory>

    @get:Input
    abstract val keyservers: ListProperty<String>

    @get:Internal
    abstract val bouncyCastleClasspath: ConfigurableFileCollection

    @get:Internal
    abstract val offlineMode: Property<Boolean>

    internal interface CheckKeyserversParameters : WorkParameters {
        val signature: Property<String>
        val keyservers: ListProperty<String>
    }

    internal abstract class CheckKeyservers : WorkAction<CheckKeyserversParameters> {
        private val PGPSignature.keyIdHex get() = keyIdToHex(keyID)

        override fun execute() {
            val longKeyId = BCPGInputStream(
                Base64.getDecoder().decode(parameters.signature.get()).inputStream()
            ).use {
                PGPSignature(it).keyIdHex
            }
            val keyFound = parameters.keyservers.get().map { URI.create("$it/pks/lookup?op=get&search=0x$longKeyId").toURL() }.any { url ->
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.inputStream.reader().use {
                        logger.quiet("Public key found on keyserver: '${connection.url.host}'")
                    }
                    return@any true
                } catch (e: IOException) {
                    if (connection.responseCode == 404) {
                        return@any false
                    } else {
                        throw e
                    }
                } finally {
                    connection.disconnect()
                }
            }
            check(keyFound) {
                """
                None of the keyservers contain the public key with id: ${longKeyId.takeLast(8)}
                Keyservers checked: ${parameters.keyservers.get().joinToString()}
                
                You can upload your key to a keyserver with:
                
                gradlew uploadPublicPgpKey --keyring=PATH/TO/ARMORED_ASCII_PUBLIC_KEY.asc
                
                See https://kotl.in/sonatype-distributing-public-key for more details.
            """.trimIndent()
            }
        }

        private companion object {
            private val logger: Logger = Logging.getLogger(CheckKeyservers::class.java)
        }
    }

    @TaskAction
    protected fun execute() {
        checkSignatoryConfigured()

        checkPublicKeyUploaded()

        checkPublicationsHaveSigningEnabled()
    }

    private fun checkPublicationsHaveSigningEnabled() {
        val (signedPublications, unsignedPublications) = publicationNamesWithSigning.get().entries.partition { (_, signed) -> signed }
        if (signedPublications.isEmpty()) {
            reportDiagnostic(
                KotlinToolingDiagnostics.SigningMisconfigured(
                    """
                    No publications are signed. Publishing to Maven Central will fail validation.
                    
                    To sign all publications you can add this to your build script:
                    
                    signing {
                        sign(publishing.publications)
                    }
                """.trimIndent(), "Please double check the settings used for signing.", "https://kotl.in/gradle-signing-plugin-what-to-sign"
                )
            )
            return
        } else if (unsignedPublications.isNotEmpty()) {
            reportDiagnostic(KotlinToolingDiagnostics.SomePublicationsNotSigned(unsignedPublications.map { it.key }))
        }
    }

    private fun checkPublicKeyUploaded() {
        if (offlineMode.get()) {
            logger.warn("Skipping verification of public key with PGP keyservers because Gradle is running in offline mode.")
            return
        }

        val workQueue: WorkQueue = workerExecutor.classLoaderIsolation {
            it.classpath.from(bouncyCastleClasspath)
        }

        workQueue.submit(CheckKeyservers::class.java) { parameters ->
            val signature = signatory.get().sign("example".byteInputStream())
            parameters.signature.set(Base64.getEncoder().encode(signature).decodeToString())
            parameters.keyservers.set(keyservers)
        }
    }

    private fun checkSignatoryConfigured() {
        if (!signatory.isPresent) {
            if (keyId.isPresent || keyringPath.isPresent) {
                val missingProperties = mutableListOf<String>()
                val presentProperties = mutableListOf<String>()

                if (keyId.isPresent) {
                    presentProperties += "* 'signing.keyId': ${keyId.get()}"
                } else {
                    missingProperties += "* 'signing.keyId' is not set. Please ensure you have the 'signing.keyId' property set to your key's ID."
                }
                if (keyringPath.isPresent) {
                    presentProperties += "* 'signing.secretKeyRingFile': ${keyringPath.get()}"
                    if (!keyringPath.get().asFile.isFile) {
                        missingProperties += "* Looks like the path defined in 'signing.secretKeyRingFile' doesn't exist or cannot be read as a file."
                    }
                } else {
                    missingProperties += "* 'signing.secretKeyRingFile' is not set. Please ensure you have the 'signing.secretKeyRingFile' property set to your keyring's file path."
                }
                if (hasKeyPassword.get()) {
                    presentProperties += "* 'signing.password' is set"
                } else {
                    missingProperties += "* 'signing.password' is not set. Please ensure you have the 'signing.password' property set to your secret key's password."
                }
                val errorMessage = buildString {
                    appendLine("Looks like you are trying to load the PGP key from disk:")
                    presentProperties.forEach { appendLine(it) }
                    appendLine()
                    appendLine("Some problems were found with the configuration:")
                    missingProperties.forEach { appendLine(it) }
                    appendLine()
                    appendLine("Ensure that you have the missing properties set, for example by putting them into ${gradleHomePath.get()}${File.separator}gradle.properties:")
                    appendLine(
                        """
                        signing.keyId=${keyId.getOrElse("<YOUR_KEY_ID>")}
                        signing.password=<YOUR_PASSWORD>
                        signing.secretKeyRingFile=${if (keyringPath.isPresent) keyringPath.get().asFile else "<YOUR_KEYRING_FILE_PATH>"}
                    """.trimIndent()
                    )
                }
                reportDiagnostic(
                    KotlinToolingDiagnostics.SigningMisconfigured(
                        errorMessage,
                        "Please double check the settings used for signing.",
                        "https://kotl.in/gradle-signing-signatory-credentials"
                    )
                )
                return
            }

            reportDiagnostic(
                KotlinToolingDiagnostics.SigningMisconfigured(
                    """
                    Could not detect a signing configuration.
                    
                    Please note: if you do not have a signing key, you can generate one by running the 'generatePgpKeys' task.
                """.trimIndent(),
                    "Please ensure you provide the required Gradle properties pointing to your signing key or configure an in-memory key.",
                    "https://kotl.in/gradle-signing-signatory-credentials"
                )
            )
        }
    }
}