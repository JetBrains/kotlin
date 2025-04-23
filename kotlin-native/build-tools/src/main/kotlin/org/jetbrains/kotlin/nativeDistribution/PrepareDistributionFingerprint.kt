/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest
import javax.inject.Inject

open class PrepareDistributionFingerprint @Inject constructor(
        objectFactory: ObjectFactory,
) : DefaultTask() {
    /**
     * Which files to fingerprint.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE) // manually computed relative to output
    val input: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * Where to place the fingerprint.
     */
    @get:OutputFile
    val output: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Which algorithm to use for fingerprinting.
     *
     * See [MessageDigest] for the list of supported algorithms.
     */
    @get:Input
    val fingerprintAlgorithm = objectFactory.property(String::class.java).convention("SHA-256")

    @get:Input
    @Suppress("unused") // used by Gradle via reflection.
    protected val inputRelativePaths = output.locationOnly.zip(input.elements) { base, files ->
        files.map {
            it.asFile.toRelativeString(base.asFile)
        }
    }

    @TaskAction
    fun fingerprint() {
        val output = output.asFile.get()
        val md = MessageDigest.getInstance(fingerprintAlgorithm.get())
        sequence {
            val queue = input.files.toMutableList()
            while (queue.isNotEmpty()) {
                val file = queue.removeLast()
                when {
                    file.isDirectory -> queue.addAll(file.listFiles())
                    file.isFile -> yield(file to file.toRelativeString(output))
                    else -> error("Unexpected file: $file")
                }
            }
        }.sortedBy {
            it.second
        }.forEach { (file, path) ->
            md.update(path.encodeToByteArray())
            file.forEachBlock { buffer, size ->
                md.update(buffer, 0, size)
            }
        }
        output.writeText(md.digest().joinToString(separator = "") { it.toUByte().toString(16) })
    }
}