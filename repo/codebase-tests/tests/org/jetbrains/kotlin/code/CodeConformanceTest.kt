/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.config.LanguageFeature
import java.io.File
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashSet

class CodeConformanceTest : TestCase() {
    companion object {
        private val JAVA_FILE_PATTERN = Pattern.compile(".+\\.java")
        private val SOURCES_FILE_PATTERN = Pattern.compile(".+\\.(java|kt|js)")
        private const val MAX_STEPS_COUNT = 100
        private val nonSourcesMatcher = FileMatcher(
            File("."),
            listOf(
                ".git",
                ".idea",
                "build/js",
                "build/tmp",
                "buildSrc",
                "compiler/build",
                "compiler/fir/lightTree/testData",
                "compiler/testData/psi/kdoc",
                "compiler/util/src/org/jetbrains/kotlin/config/MavenComparableVersion.java",
                "dependencies",
                "dependencies/protobuf/protobuf-relocated/build",
                "dist",
                "idea/testData/codeInsight/renderingKDoc",
                "intellij",
                "js/js.tests/.gradle",
                "js/js.tests/build",
                "js/js.translator/qunit/qunit.js",
                "js/js.translator/testData/node_modules",
                "local",
                "libraries/kotlin.test/js/it/.gradle",
                "libraries/kotlin.test/js/it/node_modules",
                "libraries/reflect/api/src/java9/java/kotlin/reflect/jvm/internal/impl",
                "libraries/reflect/build",
                "libraries/stdlib/jdk8/moduleTest/NonExportedPackagesTest.kt",
                "libraries/stdlib/js-ir/.gradle",
                "libraries/stdlib/js-ir/build",
                "libraries/stdlib/js-ir-minimal-for-test/.gradle",
                "libraries/stdlib/js-ir-minimal-for-test/build",
                "libraries/stdlib/js-v1/.gradle",
                "libraries/stdlib/js-v1/build",
                "libraries/tools/binary-compatibility-validator/src/main/kotlin/org.jetbrains.kotlin.tools",
                "libraries/tools/kotlin-gradle-plugin-core/gradle_api_jar/build/tmp",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/build",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/out",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/native/NativeDownloadAndPlatformLibsIT.kt",
                "libraries/tools/kotlin-js-tests/src/test/web/qunit.js",
                "libraries/tools/kotlin-maven-plugin/target",
                "libraries/tools/kotlin-source-map-loader/.gradle",
                "libraries/tools/kotlin-test-js-runner/.gradle",
                "libraries/tools/kotlin-test-js-runner/lib",
                "libraries/tools/kotlin-test-js-runner/node_modules",
                "libraries/tools/kotlin-test-nodejs-runner/.gradle",
                "libraries/tools/kotlin-test-nodejs-runner/node_modules",
                "libraries/tools/kotlinp/src",
                "libraries/tools/new-project-wizard/new-project-wizard-cli/build",
                "out",
                "repo/codebase-tests/tests/org/jetbrains/kotlin/code/CodeConformanceTest.kt",
                "kotlin-native/build",
                "kotlin-native/performance",
                "kotlin-native/samples"
            )
        )

        private val COPYRIGHT_EXCLUDED_FILES_AND_DIRS_MATCHER = FileMatcher(
            File("."),
            listOf(
                "build",
                "buildSrc/build/generated-sources",
                "buildSrc/prepare-deps/build",
                "compiler/ir/serialization.web/build/fullRuntime",
                "compiler/ir/serialization.web/build/reducedRuntime/src/libraries/stdlib/js-ir/runtime/longjs.kt",
                "dependencies",
                "dependencies/android-sdk/build",
                "dependencies/protobuf/protobuf-relocated/build",
                "dist",
                "idea/idea-jvm/src/org/jetbrains/kotlin/idea/copyright",
                "intellij",
                "js/js.tests/.gradle",
                "js/js.tests/build",
                "js/js.translator/testData/node_modules",
                "libraries/examples/browser-example/target",
                "libraries/examples/browser-example-with-library/target",
                "libraries/examples/js-example/target",
                "libraries/kotlin.test/js/it/.gradle",
                "libraries/kotlin.test/js/it/node_modules",
                "libraries/stdlib/common/build",
                "libraries/stdlib/js-ir/.gradle",
                "libraries/stdlib/js-ir/build",
                "libraries/stdlib/js-ir/build/",
                "libraries/stdlib/js-ir/runtime/longjs.kt",
                "libraries/stdlib/js-ir-minimal-for-test/.gradle",
                "libraries/stdlib/js-ir-minimal-for-test/build",
                "libraries/stdlib/js-v1/.gradle",
                "libraries/stdlib/js-v1/build",
                "libraries/stdlib/js-v1/node_modules",
                "libraries/stdlib/wasm/build",
                "libraries/tools/atomicfu/build",
                "libraries/tools/gradle/android-test-fixes/build",
                "libraries/tools/gradle/gradle-warnings-detector/build",
                "libraries/tools/kotlin-allopen/build",
                "libraries/tools/kotlin-assignment/build",
                "libraries/tools/kotlin-gradle-build-metrics/build",
                "libraries/tools/kotlin-gradle-plugin/build",
                "libraries/tools/kotlin-gradle-plugin-api/build",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/build",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/.testKitDir",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/out",
                "libraries/tools/kotlin-gradle-plugin-model/build",
                "libraries/tools/kotlin-gradle-statistics/build",
                "libraries/tools/kotlin-lombok/build",
                "libraries/tools/kotlin-maven-plugin-test/target",
                "libraries/tools/kotlin-noarg/build",
                "libraries/tools/kotlin-test-js-runner/.gradle",
                "libraries/tools/kotlin-test-js-runner/lib",
                "libraries/tools/kotlin-test-js-runner/node_modules",
                "libraries/tools/kotlin-test-nodejs-runner/.gradle",
                "libraries/tools/kotlin-test-nodejs-runner/node_modules",
                "libraries/tools/kotlin-sam-with-receiver/build",
                "libraries/tools/kotlin-serialization/build",
                "libraries/tools/kotlin-source-map-loader/.gradle",
                "kotlin-native", "libraries/stdlib/native-wasm", // Have a separate licences manager
                "out",
                "repo/codebase-tests/tests/org/jetbrains/kotlin/code/CodeConformanceTest.kt",
            )
        )
    }

    fun testParserCode() {
        val pattern = Pattern.compile("assert.*?\\b[^_]at.*?$", Pattern.MULTILINE)

        for (sourceFile in FileUtil.findFilesByMask(JAVA_FILE_PATTERN, File("compiler/frontend/src/org/jetbrains/kotlin/parsing"))) {
            val matcher = pattern.matcher(sourceFile.readText())
            if (matcher.find()) {
                fail("An at-method with side-effects is used inside assert: ${matcher.group()}\nin file: $sourceFile")
            }
        }
    }

    private fun isCorrectExtension(filename: String, extensions: Set<String>): Boolean {
        val additionalExtensions = listOf(
            "after", "new", "before", "expected",
            "todo", "delete", "touch", "prefix", "postfix", "map",
            "fragment", "after2", "result", "log", "messages", "conflicts", "match", "imports", "txt", "xml"
        )
        val possibleAdditionalExtensions = extensions.plus(additionalExtensions)
        val fileExtensions = filename.split("\\.").drop(1)
        if (fileExtensions.size < 2) {
            return true
        }
        val extension = fileExtensions.last()

        return !((extension !in possibleAdditionalExtensions && (extension.toIntOrNull() ?: MAX_STEPS_COUNT) >= MAX_STEPS_COUNT))
    }

    fun testForgottenBunchDirectivesAndFiles() {
        val sourceBunchFilePattern = Pattern.compile("(.+\\.java|.+\\.kt|.+\\.js)(\\.\\w+)?")
        val root = nonSourcesMatcher.root
        val extensions = File(root, ".bunch").readLines().map { it.split("_") }.flatten().toSet()
        val failBuilder = mutableListOf<String>()
        nonSourcesMatcher.excludeWalkTopDown(sourceBunchFilePattern).forEach { sourceFile ->
            val matches = Regex("BUNCH (\\w+)")
                .findAll(sourceFile.readText())
                .map { it.groupValues[1] }
                .toSet()
                .filterNot { it in extensions }
            for (bunch in matches) {
                val filename = FileUtil.toSystemIndependentName(sourceFile.toRelativeString(root))
                failBuilder.add("$filename has unregistered $bunch bunch directive")
            }

            if (!isCorrectExtension(sourceFile.name, extensions)) {
                val filename = FileUtil.toSystemIndependentName(sourceFile.toRelativeString(root))
                failBuilder.add("$filename has unknown bunch extension")
            }
        }

        if (failBuilder.isNotEmpty()) {
            fail("\n" + failBuilder.joinToString("\n"))
        }
    }

    fun testNoBadSubstringsInProjectCode() {
        class TestData(val message: String, val filter: (File, String) -> Boolean) {
            val result: MutableList<File> = ArrayList()
        }

        val atAuthorPattern = Pattern.compile("/\\*.+@author.+\\*/", Pattern.DOTALL)

        val tests = listOf(
            TestData(
                "%d source files contain @author javadoc tag.\nPlease remove them or exclude in this test:\n%s"
            ) { _, source ->
                // substring check is an optimization
                "@author" in source && atAuthorPattern.matcher(source).find() &&
                        "ASM: a very small and fast Java bytecode manipulation framework" !in source &&
                        "package org.jetbrains.kotlin.tools.projectWizard.settings.version.maven" !in source
            },
            TestData(
                "%d source files use something from com.beust.jcommander.internal package.\n" +
                        "This code won't work when there's no TestNG in the classpath of our IDEA plugin, " +
                        "because there's only an optional dependency on testng.jar.\n" +
                        "Most probably you meant to use Guava's Lists, Maps or Sets instead. " +
                        "Please change references in these files to com.google.common.collect:\n%s"
            ) { _, source ->
                "com.beust.jcommander.internal" in source
            },
            TestData(
                "%d source files contain references to package org.jetbrains.jet.\n" +
                        "Package org.jetbrains.jet is deprecated now in favor of org.jetbrains.kotlin. " +
                        "Please consider changing the package in these files:\n%s"
            ) { _, source ->
                "org.jetbrains.jet" in source
            },
            TestData(
                "%d source files contain references to package kotlin.reflect.jvm.internal.impl.\n" +
                        "This package contains internal reflection implementation and is a result of a " +
                        "post-processing of kotlin-reflect.jar by jarjar.\n" +
                        "Most probably you meant to use classes from org.jetbrains.kotlin.**.\n" +
                        "Please change references in these files or exclude them in this test:\n%s"
            ) { _, source ->
                "kotlin.reflect.jvm.internal.impl" in source
            },
            TestData(
                "%d source files contain references to package org.objectweb.asm.\n" +
                        "Package org.jetbrains.org.objectweb.asm should be used instead to avoid troubles with different asm versions in classpath. " +
                        "Please consider changing the package in these files:\n%s"
            ) { _, source ->
                " org.objectweb.asm" in source
            }
        )

        nonSourcesMatcher.excludeWalkTopDown(SOURCES_FILE_PATTERN).forEach { sourceFile ->
            val source = sourceFile.readText()
            for (test in tests) {
                if (test.filter(sourceFile, source)) test.result.add(sourceFile)
            }
        }

        if (tests.flatMap { it.result }.isNotEmpty()) {
            fail(buildString {
                for (test in tests) {
                    if (test.result.isNotEmpty()) {
                        append(test.message.format(test.result.size, test.result.joinToString("\n")))
                        appendLine()
                        appendLine()
                    }
                }
            })
        }
    }

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

        fun matchExact(file: File): Boolean {
            return file.relativeTo(root).invariantSeparatorsPath in paths
        }

        fun matchWithContains(file: File): Boolean {
            if (matchExact(file)) return true
            val relativePath = file.relativeTo(root).invariantSeparatorsPath
            return relativePaths.any { relativePath.startsWith(it) }
        }
    }

    private fun FileMatcher.excludeWalkTopDown(filePattern: Pattern): Sequence<File> {
        return root.walkTopDown()
            .onEnter { dir ->
                !matchExact(dir) // Don't enter to ignored dirs
            }
            .filter { file -> !matchExact(file) } // filter ignored files
            .filter { file -> filePattern.matcher(file.name).matches() }
            .filter { file -> file.isFile }
    }

    fun testRepositoriesAbuse() {
        class RepoAllowList(val repo: String, root: File, allowList: Set<String>, val exclude: String? = null) {
            val matcher = FileMatcher(root, allowList)
        }

        val root = nonSourcesMatcher.root

        val repoCheckers = listOf(
            RepoAllowList(
                // Please use cache-redirector for importing in tests
                "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev", root,
                setOf("repo/scripts/cache-redirector.settings.gradle.kts")
            ),
            RepoAllowList(
                "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev", root,
                setOf("repo/scripts/cache-redirector.settings.gradle.kts")
            ),
            RepoAllowList(
                // Please use cache-redirector for importing in tests
                "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/eap", root,
                setOf("repo/scripts/cache-redirector.settings.gradle.kts")
            ),
            RepoAllowList(
                "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev", root,
                setOf("repo/scripts/cache-redirector.settings.gradle.kts")
            ),
            RepoAllowList(
                "kotlin/ktor", root,
                setOf("repo/scripts/cache-redirector.settings.gradle.kts")
            ),
            RepoAllowList(
                "bintray.com", root,
                setOf("repo/scripts/cache-redirector.settings.gradle.kts"),
                exclude = "jcenter.bintray.com"
            )
        )

        data class RepoOccurance(val repo: String, val file: File)
        data class RepoOccurrences(val repo: String, val files: Collection<File>)

        val extensionsPattern = Pattern.compile(".+\\.(java|kt|gradle|kts|xml)(\\.\\w+)?")
        val repoOccurrences: List<RepoOccurrences> = nonSourcesMatcher.excludeWalkTopDown(extensionsPattern)
            .flatMap { file ->
                val checkers = repoCheckers.filter { checker ->
                    !checker.matcher.matchWithContains(file)
                }

                if (checkers.isNotEmpty()) {
                    val occurrences = ArrayList<RepoOccurance>()
                    file.useLines { lines ->
                        for (line in lines) {
                            for (checker in checkers) {
                                if (line.contains(checker.repo) && (checker.exclude == null || !line.contains(checker.exclude))) {
                                    occurrences.add(RepoOccurance(checker.repo, file))
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
            .map { (repo, occurrences) -> RepoOccurrences(repo, occurrences.mapTo(HashSet()) { it.file }) }

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

    fun testLanguageFeatureOrder() {
        val values = enumValues<LanguageFeature>()
        val enabledFeatures = values.filter { it.sinceVersion != null }

        if (enabledFeatures.sortedBy { it.sinceVersion!! } != enabledFeatures) {
            val (a, b) = enabledFeatures.zipWithNext().first { (a, b) -> a.sinceVersion!! > b.sinceVersion!! }
            fail(
                "Please make sure LanguageFeature entries are sorted by sinceVersion to improve readability & reduce confusion.\n" +
                        "The feature $b is out of order; its sinceVersion is ${b.sinceVersion}, yet it comes after $a, whose " +
                        "sinceVersion is ${a.sinceVersion}.\n"
            )
        }
    }
}

private fun String.ensureFileOrEndsWithSlash() =
    if (endsWith("/") || "." in substringAfterLast('/')) this else "$this/"
