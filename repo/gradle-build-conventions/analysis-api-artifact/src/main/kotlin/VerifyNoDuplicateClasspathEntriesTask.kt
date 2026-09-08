/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.zip.ZipFile

/**
 * Verifies that no entry is present in more than one archive of [classpath].
 *
 * Fat JARs in this repository are assembled with `DuplicatesStrategy.EXCLUDE`, so a class which ends up in several JARs is silently
 * resolved by the classpath order. For the Analysis API that is a correctness issue: patched IntelliJ classes must not be shadowed by
 * their unpatched originals.
 *
 * Per-archive metadata which unavoidably collides, and `META-INF/services` descriptors, which the `ServiceLoader` reads from every
 * archive, are not compared. Everything else is, including `*.kotlin_module` files and XML declarations, as losing those to the
 * classpath order is a bug as well. Known and accepted collisions are listed in [permittedDuplicatesFile].
 */
abstract class VerifyNoDuplicateClasspathEntriesTask : DefaultTask() {
    init {
        group = "verification"
    }

    /**
     * The archives to verify against each other. Non-archive files are ignored.
     */
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    /**
     * A file listing entry names which are allowed to be present in more than one archive, one per line.
     * Blank lines and lines starting with `#` are ignored, so every exception can be commented on.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val permittedDuplicatesFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val ownersByEntry = mutableMapOf<String, MutableSet<String>>()

        for (file in classpath.files) {
            if (!file.isFile || !file.isArchive()) continue

            ZipFile(file).use { zipFile ->
                zipFile.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .map { it.name }
                    .filterNot(::isIgnoredEntry)
                    .forEach { ownersByEntry.getOrPut(it, ::mutableSetOf).add(file.name) }
            }
        }

        val permittedDuplicatesFile = permittedDuplicatesFile.get().asFile
        val permittedDuplicates = permittedDuplicatesFile.readPermittedDuplicates()

        val duplicates = ownersByEntry
            .filterValues { it.size > 1 }
            .filterKeys { it !in permittedDuplicates }

        if (duplicates.isNotEmpty()) {
            val message = renderFailure(duplicates, permittedDuplicatesFile)
            throw GradleException(message)
        }
    }

    private fun File.isArchive(): Boolean {
        val lowercasedExtension = extension.lowercase()
        return lowercasedExtension == "jar" || lowercasedExtension == "zip"
    }

    private fun File.readPermittedDuplicates(): Set<String> =
        useLines { lines ->
            lines
                .map { it.trim() }
                .filterNot { it.isEmpty() || it.startsWith('#') }
                .toSet()
        }

    private fun renderFailure(duplicates: Map<String, Set<String>>, permittedDuplicatesFile: File): String = buildString {
        val subject = if (duplicates.size == 1) "entry is" else "entries are"
        append(duplicates.size).append(' ').append(subject).append(" present in more than one archive of the '")
        append(path).appendLine("' classpath:")

        val maxReportedDuplicates = 500

        duplicates.entries
            .sortedBy { it.key }
            .take(maxReportedDuplicates)
            .forEach { (entry, owners) ->
                append("    ").append(entry).append(": ").appendLine(owners.sorted().joinToString())
            }

        val notReportedCount = duplicates.size - maxReportedDuplicates
        if (notReportedCount > 0) {
            append("    (and ").append(notReportedCount).appendLine(" more)")
        }

        appendLine()
        appendLine("Every entry must be provided by exactly one archive, otherwise the classpath order decides which one wins.")
        append("Fix the dependency declarations, or add the entry to '").append(permittedDuplicatesFile.name)
        appendLine("' if the duplication is intentional.")
    }
}

private const val META_INF_PREFIX = "META-INF/"
private const val MODULE_INFO = "module-info.class"
private val SIGNATURE_EXTENSIONS = setOf("SF", "DSA", "RSA", "EC")
private val LEGAL_FILE_PREFIXES = listOf("LICENSE", "NOTICE", "COPYRIGHT")

private fun isIgnoredEntry(name: String): Boolean {
    if (name == MODULE_INFO || LEGAL_FILE_PREFIXES.any { name.startsWith(it) }) return true
    if (!name.startsWith(META_INF_PREFIX, ignoreCase = true)) return false

    val path = name.substring(META_INF_PREFIX.length)
    return when {
        path.startsWith("services/") -> true
        path.startsWith("maven/") -> true
        path.startsWith("versions/") -> path.endsWith("/$MODULE_INFO")
        path.contains('/') -> false
        path == "MANIFEST.MF" || path == "INDEX.LIST" || path == "DEPENDENCIES" -> true
        LEGAL_FILE_PREFIXES.any { path.startsWith(it) } -> true
        path == "AL2.0" || path == "LGPL2.1" -> true
        else -> path.substringAfterLast('.', missingDelimiterValue = "") in SIGNATURE_EXTENSIONS
    }
}
