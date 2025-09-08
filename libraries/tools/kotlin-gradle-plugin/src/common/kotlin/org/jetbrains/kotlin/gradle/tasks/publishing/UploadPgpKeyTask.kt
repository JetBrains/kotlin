/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@DisableCachingByDefault(because = "Uploading keys to a keyserver is not cacheable. This task is intended for CLI usage.")
abstract class UploadPgpKeyTask internal constructor() : DefaultTask() {
    @get:Input
    @get:Option(
        option = "keyring", description = "The file that contains the public key to upload to the keyserver in armored ASCII format."
    )
    abstract val keyring: Property<String>

    @get:Input
    @get:Option(
        option = "keyserver", description = "The address of the keyserver to upload the key to. Default: 'https://keyserver.ubuntu.com'"
    )
    @get:Optional
    abstract val keyserver: Property<String>

    @TaskAction
    protected fun execute() {
        val publicKeyringFile = File(keyring.get())
        require(publicKeyringFile.isFile) {
            "The provided public keyring file does not exist or cannot be read: ${publicKeyringFile.absolutePath}"
        }
        val publicKeyringContent = publicKeyringFile.readText()
        require(publicKeyringContent.startsWith("-----BEGIN PGP PUBLIC KEY BLOCK-----")) {
            """
                The provided public keyring file does not start with '-----BEGIN PGP PUBLIC KEY BLOCK-----'.
                Please make sure that the provided file contains a valid public key in armored ASCII format.
            """.trimIndent()
        }
        val connection = URI.create("${keyserver.get()}/pks/add").toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            connection.doInput = true
            connection.allowUserInteraction = true

            connection.outputStream.writer().buffered().use {
                it.write("keytext=")
                it.write(URLEncoder.encode(publicKeyringContent, StandardCharsets.UTF_8.toString()))
                // nm stands for "no modification", as described in the HKP protocol documentation:
                // https://www.ietf.org/archive/id/draft-gallagher-openpgp-hkp-04.html#name-the-nm-no-modification-opti
                it.write("&options=nm")
            }

            val result = connection.inputStream.reader().use { it.readText() }
            logger.quiet("Key upload successful. Server returned:\n$result")

        } catch (e: IOException) {
            connection.errorStream?.reader()?.use { it.readText() }?.also { result ->
                logger.error("Failed to upload public key. Server returned:\n$result")
            }
            throw e
        } finally {
            connection.disconnect()
        }
    }
}