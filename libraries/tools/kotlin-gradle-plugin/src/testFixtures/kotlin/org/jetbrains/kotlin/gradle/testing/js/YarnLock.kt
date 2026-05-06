/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.js

/**
 * Minimal representation of a `yarn.lock` file.
 *
 * Only properties relevant for tests are defined
 * to reduce the complexity and maintenance.
 *
 * The Yarn lock file format is a custom format,
 * thus it must be decoded manually.
 */
internal data class YarnLock(
    val entries: List<Entry>,
) {

    data class Entry(
        val name: String,
        val versions: List<String>,
    )

    companion object {
        fun decodeFrom(content: String): YarnLock {
            return YarnLock(
                entries = content
                    // Yarn lock entries are separated by a blank line
                    .split("\n\n")
                    .asSequence()
                    // skip entries that are comments or whitespace only
                    .filter { entry ->
                        entry.lines()
                            .map { it.trim() }
                            .any {
                                !it.startsWith("#") && it.isNotBlank()
                            }
                    }
                    .map { it.trim() }
                    .map { entry ->
                        decodeEntry(entry)
                    }
                    .toList()
            )
        }

        private fun decodeEntry(content: String): Entry {
            // the first line of an entry is all versions of the package
            val requestedPkgs = content.lines().first()

            val entries =
                requestedPkgs
                    .removeSuffix(":")
                    .split(", ")
                    .map { it.removeSurrounding("\"") }
                    .map { entryPackage ->
                        fun invalid(reason: String): Nothing = error("invalid yarn lock entry: $reason. $entryPackage")
                        val r = yarnPackageEntryPattern.matchEntire(entryPackage)
                            ?: invalid("failed to parse")
                        val name = r.groups["name"]?.value ?: invalid("missing name")
                        val version = r.groups["version"]?.value ?: invalid("missing version")
                        name to version
                    }
                    .groupBy({ it.first }, { it.second })
                    .map { (name, versions) ->
                        Entry(
                            name = name,
                            versions = versions.sorted(),
                        )
                    }

            return entries.singleOrNull()
                ?: error("Expected a single entry, but got ${entries.size}. Entry:\n$content")
        }

        /**
         * Each entry in the yarn lock file is of the form:
         * ```text
         * <package>@<version>
         * ```
         * Or, if `package` is an alias:
         * ```text
         * <package>@<target>@<version>
         * ```
         */
        private val yarnPackageEntryPattern = Regex(
            """
                    |^(?<name>@?[^@]+)@(?:(?<target>\w+:@?[^@]+)@)?(?<version>.+)$
                    """.trimMargin()
        )
    }
}
