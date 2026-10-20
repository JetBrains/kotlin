/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test

/**
 * A deprecation warning that the detector deliberately does not report.
 *
 * @param [pattern] regex to match the warning description
 * @param [fromClass] - optional stacktrace [Regex]. Note that Gradle will only capture first 50 warnings stacktrace and skip doing it
 * for the rest.
 */
internal class IgnoredWarning(
    val reason: String,
    val pattern: Regex,
    val fromClass: Regex? = null,
)

/**
 * Deprecation warnings caused by third-party Gradle plugins, which this repository can neither fix nor act on.
 *
 * To learn the exact wording of a warning, run the build with `--warning-mode=all`: Gradle prints the full message
 * itself, and `-Dorg.gradle.deprecation.trace=true` adds the stack trace that identifies the caller. Note that some
 * warnings are only emitted once per Gradle daemon, so a warm daemon may stay silent.
 */
internal val KNOWN_THIRD_PARTY_WARNINGS: List<IgnoredWarning> = listOf(
    IgnoredWarning(
        reason = "AGP declares Boolean 'is-' properties: https://issuetracker.google.com/issues/399393875",
        pattern = """'is-' (?:property|method) with a Boolean type""".toRegex(),
        fromClass = """\bcom\.android\.""".toRegex(),
    ),
    IgnoredWarning(
        reason = "AGP looks up null attribute keys while writing the lint model: " +
                "https://issuetracker.google.com/issues/408334529",
        pattern = """Retrieving attribute with a null key\.""".toRegex(),
        fromClass = """\bcom\.android\.""".toRegex(),
    ),
    IgnoredWarning(
        reason = "AGP calls deprecated Configuration.setVisible(boolean): " +
                "https://issuetracker.google.com/issues/560282299",
        pattern = """The Configuration\.setVisible\(boolean\) method has been deprecated\.""".toRegex(),
        fromClass = """\bcom\.android\.""".toRegex(),
    ),
    IgnoredWarning(
        reason = "KSP calls deprecated Configuration.setVisible(boolean): " +
                "https://github.com/google/ksp/issues/3234",
        pattern = """The Configuration\.setVisible\(boolean\) method has been deprecated\.""".toRegex(),
        fromClass = """\bcom\.google\.devtools\.ksp\.""".toRegex(),
    ),
    IgnoredWarning(
        reason = "Dokka calls deprecated Configuration.setVisible(boolean): " +
                "https://github.com/Kotlin/dokka/issues/4600",
        pattern = """The Configuration\.setVisible\(boolean\) method has been deprecated\.""".toRegex(),
        fromClass = """\borg\.jetbrains\.dokka\.""".toRegex(),
    ),
)

internal fun List<IgnoredWarning>.firstMatching(
    description: String,
    stackTraceClassNames: List<String> = emptyList(),
): IgnoredWarning? = firstOrNull { ignoredWarning ->
    ignoredWarning.pattern.containsMatchIn(description) &&
            ignoredWarning.fromClass.let { fromClass ->
                fromClass == null ||
                        stackTraceClassNames.any { fromClass.containsMatchIn(it) }
            }
}
