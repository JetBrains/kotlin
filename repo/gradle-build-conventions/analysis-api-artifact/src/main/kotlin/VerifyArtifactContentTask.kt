/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.util.zip.ZipFile

/**
 * Verifies that the [artifact] JAR is either non-empty or empty, as configured by [expectNonEmpty].
 *
 * The archive "content" is everything except directory entries and the `META-INF` directory, so an "empty"
 * JAR is one that contains nothing but metadata (the manifest, service descriptors, etc.).
 */
@DisableCachingByDefault(because = "The verification is cheap and not worth caching")
abstract class VerifyArtifactContentTask : DefaultTask() {
    init {
        group = "verification"
    }

    /**
     * The JAR artifact to verify.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val artifact: RegularFileProperty

    /**
     * A human-readable name of the verified artifact, used in error messages.
     */
    @get:Input
    abstract val artifactName: Property<String>

    /**
     * `true` if the [artifact] is expected to contain content, `false` if it is expected to be empty.
     */
    @get:Input
    abstract val expectNonEmpty: Property<Boolean>

    @TaskAction
    fun verify() {
        val artifactFile = artifact.get().asFile

        val contentEntries = ZipFile(artifactFile).use { zipFile ->
            zipFile.entries().asSequence()
                .filterNot { it.isDirectory }
                .map { it.name }
                .filterNot { it.equals("META-INF", ignoreCase = true) || it.startsWith("META-INF/", ignoreCase = true) }
                .toList()
        }

        val artifactName = artifactName.get()
        if (expectNonEmpty.get()) {
            if (contentEntries.isEmpty()) {
                val message = "$artifactName ('${artifactFile.name}') is expected to be non-empty, but contains nothing besides 'META-INF'"
                throw GradleException(message)
            }
        } else {
            if (contentEntries.isNotEmpty()) {
                val firstEntries = contentEntries.sorted().take(10).joinToString(separator = "\n") { "    $it" }
                val message = buildString {
                    append(artifactName)
                    append(" ('").append(artifactFile.name).append("') is expected to be empty, but contains ")
                    append(contentEntries.size).appendLine(" entries:").append(firstEntries)
                }
                throw GradleException(message)
            }
        }
    }
}
