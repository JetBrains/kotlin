/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.repoTestFixtures.isGitIgnored
import org.junit.jupiter.api.Test
import kotlin.test.fail
import java.io.File
import java.util.regex.Pattern

class CodeConformanceTest {
    companion object {
        private val JAVA_FILE_PATTERN = Pattern.compile(".+\\.java")
        private val KOTLIN_FILE_PATTERN = Pattern.compile(".+\\.kt")
        private val SOURCES_FILE_PATTERN = Pattern.compile(".+\\.(java|kt|js)")

        private val nonSourcesMatcher = FileMatcher(
            File("."),
            listOf(
                ".idea",
                "compiler/fir/lightTree/testData",
                "compiler/psi/psi-impl/testData/psi/kdoc/TwoTags.kt",
                "core/language.version-settings/src/org/jetbrains/kotlin/config/MavenComparableVersion.java",
                "dependencies",
                "idea/testData/codeInsight/renderingKDoc",
                "libraries/reflect/api/src/java9/java/kotlin/reflect/jvm/internal/impl",
                "libraries/stdlib/jdk8/moduleTest/NonExportedPackagesTest.kt",
                "libraries/tools/binary-compatibility-validator/src/main/kotlin/org.jetbrains.kotlin.tools",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/native/NativeDownloadAndPlatformLibsIT.kt",
                "libraries/tools/kotlin-js-tests/src/test/web/qunit.js",
                "libraries/tools/kotlin-maven-plugin/target",
                "libraries/tools/kotlinp/src",
                "repo/codebase-tests/tests/org/jetbrains/kotlin/code/CodeConformanceTest.kt",
                "kotlin-native/performance",
                "kotlin-native/samples",
            )
        )

        private val COPYRIGHT_EXCLUDED_FILES_AND_DIRS_MATCHER = FileMatcher(
            File("."),
            listOf(
                "dependencies",
                "kotlin-native", "libraries/stdlib/native-wasm", // Have a separate licences manager
                "repo/codebase-tests/tests/org/jetbrains/kotlin/code/CodeConformanceTest.kt",
            )
        )
    }

    @Test
    fun testNotUsingCanonicalFileApi() {
        val canonicalPattern = Pattern.compile("\\.canonical(Path|File)", Pattern.MULTILINE)

        // find KGP modules except the modules of internal test infrastructure
        val kgpDirs = File("libraries/tools").walkTopDown()
            .maxDepth(2)
            .filter { it.isDirectory && it.name.startsWith("kotlin-gradle-") && "test" !in it.name }
            // Check only `src` directory
            .flatMap { it.walkTopDown().maxDepth(1).filter { file -> file.isDirectory && file.name == "src" } }
            .map { it.path }
            .toList()

        val targetDirs = kgpDirs + listOf(
            "build-common/src",
            "compiler/build-tools/kotlin-build-statistics/src",
            "compiler/build-tools/kotlin-build-tools-api/src",
            "compiler/build-tools/kotlin-build-tools-impl/src",
            "compiler/build-tools/kotlin-build-tools-compat/src",
            "compiler/build-tools/kotlin-build-tools-jdk-utils/src",
            "compiler/daemon/daemon-client/src",
            "compiler/daemon/daemon-common/src",
            "compiler/daemon/src",
            "compiler/incremental-compilation-impl/src",
            "jps/jps-common/src",
            "jps/jps-plugin/src",
        )

        targetDirs.map {
            FileUtil.findFilesByMask(KOTLIN_FILE_PATTERN, File(it))
        }.flatten().forEach { sourceFile ->
            val matcher = canonicalPattern.matcher(sourceFile.readText())
            if (matcher.find()) {
                fail("KT-69613 canonicalPath and canonicalFile apis should not be used: ${matcher.group()}\nin file: $sourceFile")
            }
        }
    }

    @Test
    fun testNoDirectPathToStringConversion() {
        val absolutePathStringPattern = Pattern.compile("\\.absolutePathString\\(\\)", Pattern.MULTILINE)

        val targetDirs = listOf(
            "compiler/build-tools/kotlin-build-tools-api/src",
            "compiler/build-tools/kotlin-build-tools-api/gen",
            "compiler/build-tools/kotlin-build-tools-impl/src",
            "compiler/build-tools/kotlin-build-tools-impl/gen",
            "compiler/build-tools/kotlin-build-tools-compat/src",
            "compiler/build-tools/kotlin-build-tools-compat/gen",
            "compiler/build-tools/kotlin-build-tools-cri-impl/src",
        )

        targetDirs.map {
            FileUtil.findFilesByMask(KOTLIN_FILE_PATTERN, File(it))
        }.flatten().forEach { sourceFile ->
            val matcher = absolutePathStringPattern.matcher(sourceFile.readText())
            if (matcher.find()) {
                fail("KT-83715 absolutePathString should not be used as it loses information about FileSystem: ${matcher.group()}\nin file: $sourceFile")
            }
        }
    }

    @Test
    fun testParserCode() {
        val pattern = Pattern.compile("assert.*?\\b[^_]at.*?$", Pattern.MULTILINE)

        for (sourceFile in FileUtil.findFilesByMask(JAVA_FILE_PATTERN, File("compiler/frontend/src/org/jetbrains/kotlin/parsing"))) {
            val matcher = pattern.matcher(sourceFile.readText())
            if (matcher.find()) {
                fail("An at-method with side-effects is used inside assert: ${matcher.group()}\nin file: $sourceFile")
            }
        }
    }

    @Test
    fun testNoBadSubstringsInProjectCode() {
        class FileTestCase(val message: String, allowedFiles: List<String> = emptyList(), val filter: (File, String) -> Boolean) {
            val allowedMatcher = FileMatcher(File("."), allowedFiles)
        }

        val atAuthorPattern = Pattern.compile("/\\*.+@author.+\\*/", Pattern.DOTALL)

        @Suppress("SpellCheckingInspection") val tests = listOf(
            FileTestCase(
                "%d source files contain @author javadoc tag.\nPlease remove them or exclude in this test:\n%s"
            ) { _, source ->
                // substring check is an optimization
                "@author" in source && atAuthorPattern.matcher(source).find() &&
                        "ASM: a very small and fast Java bytecode manipulation framework" !in source &&
                        "package org.jetbrains.kotlin.tools.projectWizard.settings.version.maven" !in source
            },
            FileTestCase(
                "%d source files use something from com.beust.jcommander.internal package.\n" +
                        "This code won't work when there's no TestNG in the classpath of our IDEA plugin, " +
                        "because there's only an optional dependency on testng.jar.\n" +
                        "Most probably you meant to use Guava's Lists, Maps or Sets instead. " +
                        "Please change references in these files to com.google.common.collect:\n%s"
            ) { _, source ->
                "com.beust.jcommander.internal" in source
            },
            FileTestCase(
                "%d source files contain references to package org.jetbrains.jet.\n" +
                        "Package org.jetbrains.jet is deprecated now in favor of org.jetbrains.kotlin. " +
                        "Please consider changing the package in these files:\n%s"
            ) { _, source ->
                "org.jetbrains.jet" in source
            },
            FileTestCase(
                message = "%d source files contain references to package kotlin.reflect.jvm.internal.impl.\n" +
                        "This package contains internal reflection implementation and is a result of a " +
                        "post-processing of kotlin-reflect.jar by jarjar.\n" +
                        "Most probably you meant to use classes from org.jetbrains.kotlin.**.\n" +
                        "Please change references in these files or exclude them in this test:\n%s",
                allowedFiles = listOf(
                    "libraries/tools/jdk-api-validator/src/test/JdkApiUsageTest.kt"
                )
            ) { _, source ->
                "kotlin.reflect.jvm.internal.impl" in source
            },
            FileTestCase(
                "%d source files contain references to package org.objectweb.asm.\n" +
                        "Package org.jetbrains.org.objectweb.asm should be used instead to avoid troubles with different asm versions in classpath. " +
                        "Please consider changing the package in these files:\n%s",
                allowedFiles = listOf(
                    "plugins/compose/group-mapping/"
                )
            ) { _, source ->
                " org.objectweb.asm" in source
            },
            FileTestCase(
                message = "%d source files contain references to package gnu.trove.\n" +
                        "Please avoid using trove library in new use cases. " +
                        "These files are affected:\n%s",
            ) { _, source ->
                "gnu.trove" in source
            },
        )

        val testCaseToMatchedFiles: Map<FileTestCase, MutableList<File>> = mutableMapOf<FileTestCase, MutableList<File>>()
            .apply {
                tests.forEach { testCase -> this[testCase] = mutableListOf() }
            }

        nonSourcesMatcher.excludeWalkTopDown(SOURCES_FILE_PATTERN).forEach { sourceFile ->
            val source = sourceFile.readText()
            for (test in tests) {
                if (test.filter(sourceFile, source)) {
                    (testCaseToMatchedFiles[test] ?: error("Should be added during initialization")).add(sourceFile)
                }
            }
        }

        val failureStr = buildString {
            for (test in tests) {
                val [allowed, notAllowed] = (testCaseToMatchedFiles[test] ?: error("Should be added during initialization")).partition {
                    test.allowedMatcher.matchWithContains(it)
                }

                if (notAllowed.isNotEmpty()) {
                    append(test.message.format(notAllowed.size, notAllowed.joinToString("\n")))
                    appendLine()
                    appendLine()
                }

                val unmatched = test.allowedMatcher.unmatched(allowed)
                if (unmatched.isNotEmpty()) {
                    val testMessage = test.message.format(unmatched.size, "NONE")
                    append(
                        "Unused \"allowed files\" for test:\n" +
                                "`$testMessage`\n" +
                                "Remove exceptions for the test list:${unmatched.joinToString("\n", prefix = "\n")}"
                    )
                    appendLine()
                    appendLine()
                }
            }
        }

        if (failureStr.isNotEmpty()) {
            fail(failureStr)
        }
    }

    @Test
    fun testThirdPartyCopyrights() {
        val filesWithUnlistedCopyrights = mutableListOf<String>()
        val knownThirdPartyCode = loadKnownThirdPartyCodeList()
        val copyrightRegex = Regex("""\bCopyright\b""")
        val root = COPYRIGHT_EXCLUDED_FILES_AND_DIRS_MATCHER.root

        COPYRIGHT_EXCLUDED_FILES_AND_DIRS_MATCHER.excludeWalkTopDown(SOURCES_FILE_PATTERN)
            .filter { sourceFile ->
                val relativePath = FileUtil.toSystemIndependentName(sourceFile.toRelativeString(root))
                !knownThirdPartyCode.any { relativePath.startsWith(it) }
            }
            .forEach { sourceFile ->
                sourceFile.useLines { lineSequence ->
                    for (line in lineSequence) {
                        if (copyrightRegex in line && "JetBrains" !in line) {
                            val relativePath = FileUtil.toSystemIndependentName(sourceFile.toRelativeString(root))
                            filesWithUnlistedCopyrights.add("$relativePath: $line")
                        }
                    }
                }
            }

        if (filesWithUnlistedCopyrights.isNotEmpty()) {
            fail(
                "The following files contain third-party copyrights and no license information. " +
                        "Please update license/README.md accordingly:\n${filesWithUnlistedCopyrights.joinToString("\n")}"
            )
        }
    }

    private class FileMatcher(val root: File, paths: Collection<String>) {
        private val files = paths.map { File(it) }
        private val paths = files.mapTo(HashSet()) { it.invariantSeparatorsPath }
        private val relativePaths = files.filterTo(ArrayList()) { it.isDirectory }.mapTo(HashSet()) { it.invariantSeparatorsPath + "/" }

        private fun File.invariantRelativePath() = relativeTo(root).invariantSeparatorsPath

        fun matchExact(file: File): Boolean {
            return file.invariantRelativePath() in paths
        }

        fun matchWithContains(file: File): Boolean {
            if (matchExact(file)) return true
            val relativePath = file.invariantRelativePath()
            return relativePaths.any { relativePath.startsWith(it) }
        }

        fun unmatched(files: List<File>): Set<String> {
            val filePaths = files.map { it.invariantRelativePath() }.toSet()
            val relativePaths = paths.filter { p -> filePaths.any { it.startsWith(p) } }.toSet()
            return paths - filePaths - relativePaths
        }
    }

    private fun FileMatcher.excludeWalkTopDown(filePattern: Pattern): Sequence<File> {
        return root.walkTopDown()
            .onEnter { dir ->
                !matchExact(dir) && !dir.toPath().isGitIgnored() // Don't enter to ignored dirs
            }
            .filter { file -> !matchExact(file) } // filter ignored files
            .filter { file -> filePattern.matcher(file.name).matches() }
            .filter { file -> file.isFile }
    }

    @Test
    fun testRepositoriesAbuse() {
        class RepoAllowList(val repo: String, root: File, allowList: Set<String>, val exclude: String? = null) {
            val matcher = FileMatcher(root, allowList)
        }

        val root = nonSourcesMatcher.root

        val repoCheckers = listOf(
            RepoAllowList(
                // Please use redirector for importing in tests
                "https://redirector.kotlinlang.org/maven/dev", root,
                setOf("repo/gradle-settings-conventions/cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts")
            ),
            RepoAllowList(
                "kotlin/ktor", root,
                setOf("repo/gradle-settings-conventions/cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts")
            ),
            RepoAllowList(
                "bintray.com", root,
                setOf("repo/gradle-settings-conventions/cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts"),
                exclude = "jcenter.bintray.com"
            )
        )

        data class RepoOccurrence(val repo: String, val file: File)
        data class RepoOccurrences(val repo: String, val files: Collection<File>)

        val extensionsPattern = Pattern.compile(".+\\.(java|kt|gradle|kts|xml)(\\.\\w+)?")
        val repoOccurrences: List<RepoOccurrences> = nonSourcesMatcher.excludeWalkTopDown(extensionsPattern)
            .flatMap { file ->
                val checkers = repoCheckers.filter { checker ->
                    !checker.matcher.matchWithContains(file)
                }

                if (checkers.isNotEmpty()) {
                    val occurrences = ArrayList<RepoOccurrence>()
                    file.useLines { lines ->
                        for (line in lines) {
                            for (checker in checkers) {
                                if (line.contains(checker.repo) && (checker.exclude == null || !line.contains(checker.exclude))) {
                                    occurrences.add(RepoOccurrence(checker.repo, file))
                                }
                            }
                        }
                    }
                    occurrences
                } else {
                    listOf()
                }
            }
            .groupBy { it.repo }
            .map { [repo, occurrences] -> RepoOccurrences(repo, occurrences.mapTo(HashSet()) { it.file }) }

        if (repoOccurrences.isNotEmpty()) {
            val repoOccurrencesStableOrder = repoOccurrences
                .map { occurrence -> RepoOccurrences(occurrence.repo, occurrence.files.sortedBy { file -> file.path }) }
                .sortedBy { it.repo }
            fail(
                buildString {
                    appendLine("The following files use repositories and not listed in the correspondent allow lists")
                    for ((repo, files) in repoOccurrencesStableOrder) {
                        appendLine(repo)
                        for (file in files) {
                            appendLine("  ${file.relativeTo(root).invariantSeparatorsPath}")
                        }
                    }
                }
            )
        }
    }

    private fun loadKnownThirdPartyCodeList(): List<String> {
        File("license/README.md").useLines { lineSequence ->
            return lineSequence
                .filter { it.startsWith(" - Path: ") }
                .map { it.removePrefix(" - Path: ").trim().ensureFileOrEndsWithSlash() }
                .toList()

        }
    }

    @Test
    fun testLanguageFeatureOrder() {
        val values = enumValues<LanguageFeature>()
        val enabledFeatures = values.filter { it.sinceVersion != null }

        if (enabledFeatures.sortedBy { it.sinceVersion!! } != enabledFeatures) {
            val [a, b] = enabledFeatures.zipWithNext().first { [a, b] -> a.sinceVersion!! > b.sinceVersion!! }
            fail(
                "Please make sure LanguageFeature entries are sorted by sinceVersion to improve readability & reduce confusion.\n" +
                        "The feature $b is out of order; its sinceVersion is ${b.sinceVersion}, yet it comes after $a, whose " +
                        "sinceVersion is ${a.sinceVersion}.\n"
            )
        }
    }

    /**
     * Verify that no hardcoded File.pathSeparator is used in SSoT
     * Valid patterns:
     * - \${File.pathSeparator} in regular strings
     * - ${File.pathSeparator} in raw string literals ($$"...")
     * Invalid:
     * - ${File.pathSeparator} in regular strings (without \ or $$ prefix)
     */
    @Test
    fun testNoHardcodedPathSeparatorInSSOT() {
        val pattern = Pattern.compile("""(?<![\\$])\$\{File\.pathSeparator\}""")
        val targetDirs = listOf(
            "compiler/arguments/src/org/jetbrains/kotlin/arguments/dsl/types"
        )

        targetDirs.flatMap {
            FileUtil.findFilesByMask(KOTLIN_FILE_PATTERN, File(it))
        }.forEach { sourceFile ->
            val content = sourceFile.readText()
            val matcher = pattern.matcher(content)

            while (matcher.find()) {
                val matchPos = matcher.start()
                val beforeMatch = content.substring(0, matchPos)
                val quoteIdx = findPrecedingUnescapedQuote(beforeMatch)

                if (isInsideRawStringLiteral(beforeMatch, quoteIdx)) {
                    continue
                }

                fail(
                    "[KT-84449] Platform-specific File.pathSeparator must be escaped for runtime evaluation. " +
                            "Use \\${'$'}{File.pathSeparator} or raw string literals.\nin file: $sourceFile"
                )
            }
        }
    }
}

private fun findPrecedingUnescapedQuote(text: String): Int {
    var i = text.length - 1
    while (i >= 0) {
        if (text[i] == '"') {
            // Count preceding backslashes
            var backslashCount = 0
            var j = i - 1
            while (j >= 0 && text[j] == '\\') {
                backslashCount++
                j--
            }
            // Quote is unescaped if even number of backslashes before it
            if (backslashCount % 2 == 0) {
                return i
            }
        }
        i--
    }
    return -1
}

private fun isInsideRawStringLiteral(text: String, quoteIndex: Int): Boolean {
    return quoteIndex >= 2 && text.substring(quoteIndex - 2, quoteIndex) == "$$" + ""
}

private fun String.ensureFileOrEndsWithSlash() =
    if (endsWith("/") || "." in substringAfterLast('/')) this else "$this/"
