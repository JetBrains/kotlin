/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.config.LanguageFeature
import java.io.File
import java.util.regex.Pattern

class CodeConformanceTest : TestCase() {
    companion object {
        private val JAVA_FILE_PATTERN = Pattern.compile(".+\\.java")
        private val KOTLIN_FILE_PATTERN = Pattern.compile(".+\\.kt")
        private val ARGS_FILE_PATTERN = Pattern.compile(".+\\.args")
        private val SOURCES_FILE_PATTERN = Pattern.compile(".+\\.(java|kt|js)")

        @Suppress("SpellCheckingInspection")
        private val nonSourcesMatcher = FileMatcher(
            File("."),
            listOf(
                ".git",
                ".idea",
                "build/js",
                "build/tmp",
                "compiler/build",
                "compiler/fir/lightTree/testData",
                "compiler/psi/psi-impl/testData/psi/kdoc/TwoTags.kt",
                "core/language.version-settings/src/org/jetbrains/kotlin/config/MavenComparableVersion.java",
                "dependencies",
                "dependencies/protobuf/protobuf-relocated/build",
                "dist",
                "idea/testData/codeInsight/renderingKDoc",
                "intellij",
                "js/js.tests/.gradle",
                "js/js.tests/build",
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
                "libraries/tools/kotlin-test-nodejs-runner/.gradle",
                "libraries/tools/kotlin-test-nodejs-runner/node_modules",
                "libraries/tools/kotlinp/src",
                "libraries/tools/new-project-wizard/new-project-wizard-cli/build",
                "out",
                "repo/codebase-tests/tests/org/jetbrains/kotlin/code/CodeConformanceTest.kt",
                "repo/gradle-build-conventions/test-federation-convention/build",
                "kotlin-native/build",
                "kotlin-native/performance",
                "kotlin-native/samples",
                "wasm/wasm.debug.browsers/node_modules",
                "wasm/wasm.debug.browsers/.gradle",
            )
        )

        // Files excluded from testNoHardcodedPathAccess.
        // Split into two clearly marked sections (rec 3):
        //   • Production code — the path strings serve a legitimate runtime purpose.
        //   • Test / test-data — the path strings are expected *values* under test, not real file accesses.
        private val UNDECLARED_INPUT_ACCESS_EXCLUDED_FILES_AND_DIRS = FileMatcher(
            File("."),
            listOf(
                // ---- Production code with legitimate path patterns ----

                // macOS JDK layout: ../Classes/classes.jar is a standard macOS SDK path component
                "compiler/util/src/org/jetbrains/kotlin/utils/JavaSdkUtil.java",
                "libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/utils/fileUtils.kt",
                "repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/jarEmbedding.kt",

                // macOS binary paths: @executable_path/../Frameworks (rpath) and xcode-select shell command
                "native/utils/src/org/jetbrains/kotlin/konan/target/Linker.kt",
                "native/utils/src/org/jetbrains/kotlin/konan/target/Xcode.kt",

                // Help / error-message strings that mention "dist/klib" as a human-readable path example
                "compiler/arguments/src/org/jetbrains/kotlin/arguments/description/NativeCompilerArguments.kt",
                "compiler/cli/cli-base/gen/org/jetbrains/kotlin/cli/common/arguments/K2NativeCompilerArguments.kt",
                "kotlin-native/Interop/StubGenerator/src/org/jetbrains/kotlin/native/interop/gen/jvm/CommandLine.kt",

                // Path-manipulation utilities: ../ is a path component being constructed or decomposed
                "build-common/src/org/jetbrains/kotlin/incremental/storage/RelocatableFileToPathConverter.kt",
                "compiler/ir/backend.js/src/org/jetbrains/kotlin/ir/backend/js/transformers/irToJs/JsIrProgramFragment.kt",
                "js/js.sourcemap/src/org/jetbrains/kotlin/js/sourceMap/RelativePathCalculator.kt",
                "libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/npm/NpmProjectModules.kt",
                "libraries/stdlib/jdk7/src/kotlin/io/path/PathRecursiveFunctions.kt",
                "libraries/stdlib/jvm/src/kotlin/io/files/Utils.kt",

                // npm package "kotlin-web-helpers/dist/…": dist/ belongs to the npm package, not the Kotlin dist/
                "libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/karma/KotlinKarma.kt",
                "libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/mocha/KotlinMocha.kt",
                "libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/webpack/KotlinWebpackConfig.kt",

                // Karma stack-trace processor: ../ is part of the stack-trace format string being parsed
                "libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/karma/KarmaStackTraceProcessor.kt",

                // SwiftPM API: "../MySwiftPackage" is a documented example value, not a filesystem access
                "libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/apple/swiftimport/SwiftPMImportExtension.kt",

                // Code generator that writes to a sibling output directory — not a test
                "compiler/fir/tree/tree-generator/src/org/jetbrains/kotlin/fir/tree/generator/Main.kt",

                // dukat build scripts reference sibling stdlib/idl directories
                "libraries/tools/dukat/",

                // Build conventions: dist/ is a Gradle build output path, not a test input
                "repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/ComposeDependencies.kt",

                // Compiler tooling: "dist/" is the distribution base-path constant
                "compiler/util/src/org/jetbrains/kotlin/utils/PathUtil.kt",

                // ---- Test / test-data files with legitimate path patterns ----

                // macOS JDK path (../Classes/classes.jar) used in analysis and build-tools test infra
                "analysis/analysis-api-impl-base/src/org/jetbrains/kotlin/analysis/api/impl/base/util/JdkClassFinder.java",
                "compiler/build-tools/kotlin-build-tools-api-tests/src/main/kotlin/compilation/assertions/compilationAssertions.kt",

                // Tests whose subject *is* path strings: ../ appears as an expected assertion value
                "build-common/test/org/jetbrains/kotlin/incremental/storage/RelocatableFileToPathConverterTest.kt",
                "compiler/incremental-compilation-impl/tests/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathSnapshotterTest.kt",
                "compiler/tests-integration/tests/org/jetbrains/kotlin/cli/LauncherScriptTest.kt",
                "compiler/tests-integration/tests/org/jetbrains/kotlin/integration/FilePathNormalizationTest.kt",
                "compiler/util-io/test/org/jetbrains/kotlin/cli/klib/UnzipTest.kt",
                "compiler/util-io/test/org/jetbrains/kotlin/cli/klib/ZipTest.kt",
                "compiler/util-klib/testFixtures/org/jetbrains/kotlin/library/AbstractKlibLoaderTest.kt",
                "libraries/stdlib/jdk7/test/PathExtensionsTest.kt",
                "libraries/stdlib/jdk7/test/PathRecursiveFunctionsZipTest.kt",
                "libraries/stdlib/jvm/test/io/Files.kt",

                // npm package test: 'kotlin-web-helpers/dist/…' is an npm path being verified
                "libraries/tools/kotlin-gradle-plugin/src/test/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/karma/KotlinKarmaTest.kt",

                // Karma stack-trace parsing tests: ../ is part of the expected trace-format string
                "libraries/tools/kotlin-gradle-plugin/src/test/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/karma/KarmaStackTraceProcessorKtTest.kt",

                // Source-map path rewriting: "../../../../src/main/kotlin/main.kt" is the expected rewritten path
                "libraries/tools/kotlin-gradle-plugin/src/test/kotlin/org/jetbrains/kotlin/gradle/org/jetbrains/kotlin/gradle/targets/js/internal/RewriteSourceMapFilterReaderTest.kt",

                // SwiftPM tests: "../localPackage" strings are SPM relative-path identifiers under test
                "libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/unitTests/SwiftPMImportExtensionTests.kt",
                "libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/unitTests/SwiftPMImportUnitTests.kt",
                "libraries/tools/kotlin-gradle-plugin/src/testFixtures/kotlin/org/jetbrains/kotlin/gradle/testing/XcodeProjectSerializationFixtures.kt",

                // KGP integration tests: ../ paths are cross-project Gradle/Xcode/CocoaPods configuration values
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/apple/",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/BuildCacheRelocationIT.kt",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/IncrementalCompilationMultiProjectIT.kt",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/KaptIT.kt",
                // "build/dist/js/…" is the JS/Wasm plugin's own build output, not the Kotlin compiler dist/
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/Kotlin2JsGradlePluginIT.kt",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/KotlinWasmGradlePluginIT.kt",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/WasmPackageManagerGradlePluginIT.kt",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/native/CocoaPodsGitIT.kt",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/native/CocoaPodsPodspecIT.kt",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/testDsl.kt",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/testHelpers.kt",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/uklibs/KmpGradlePublicationMetadataIT.kt",

                // Maven plugin test infrastructure
                "libraries/tools/kotlin-maven-plugin-test/src/test/kotlin/org/jetbrains/kotlin/maven/test/MavenTestExecutionContext.kt",

                // JS translator testData: "../module.mjs" strings are JavaScript @JsModule identifiers
                "js/js.translator/testData/incremental/invalidation/",

                // C interop testData: "../lib1/lib1.h" is a C header path relative to the .def file
                "native/native.tests/testData/codegen/cinterop/kt64105.kt",
            )
        )

        @Suppress("SpellCheckingInspection")
        private val COPYRIGHT_EXCLUDED_FILES_AND_DIRS_MATCHER = FileMatcher(
            File("."),
            listOf(
                "build",
                "compiler/ir/serialization.js/build/fullRuntime",
                "compiler/ir/serialization.js/build/reducedRuntime/src/libraries/stdlib/js-ir/runtime/boxedLong.kt",
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
                "libraries/kotlin.test/js/it/build",
                "libraries/kotlin.test/js/it/node_modules",
                "libraries/kotlin.test/js-ir/it/.gradle",
                "libraries/kotlin.test/js-ir/it/build",
                "libraries/kotlin.test/js-ir/it/node_modules",
                "libraries/stdlib/build",
                "libraries/stdlib/common/build",
                "libraries/stdlib/js-ir/.gradle",
                "libraries/stdlib/js-ir/build",
                "libraries/stdlib/js-ir/build/",
                "libraries/stdlib/js-ir/runtime/boxedLong.kt",
                "libraries/stdlib/js-ir-minimal-for-test/.gradle",
                "libraries/stdlib/js-ir-minimal-for-test/build",
                "libraries/stdlib/js-v1/.gradle",
                "libraries/stdlib/js-v1/build",
                "libraries/stdlib/js-v1/node_modules",
                "libraries/stdlib/jvm/build",
                "libraries/stdlib/jvm-minimal-for-test/build",
                "libraries/stdlib/wasm/build",
                "libraries/tools/atomicfu/build",
                "libraries/tools/gradle/android-test-fixes/build",
                "libraries/tools/gradle/gradle-warnings-detector/build",
                "libraries/tools/gradle/kotlin-compiler-args-properties/build",
                "libraries/tools/gradle/documentation/build",
                "libraries/tools/kotlin-allopen/build",
                "libraries/tools/kotlin-assignment/build",
                "libraries/tools/kotlin-gradle-build-metrics/build",
                "libraries/tools/kotlin-gradle-plugin/build",
                "libraries/tools/kotlin-gradle-plugin-api/build",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/build",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/.testKitDir",
                "libraries/tools/kotlin-gradle-plugin-integration-tests/out",
                "libraries/tools/kotlin-gradle-statistics/build",
                "libraries/tools/kotlin-lombok/build",
                "libraries/tools/kotlin-maven-plugin-test/target",
                "libraries/tools/kotlin-noarg/build",
                "libraries/tools/kotlin-test-nodejs-runner/.gradle",
                "libraries/tools/kotlin-test-nodejs-runner/node_modules",
                "libraries/tools/kotlin-sam-with-receiver/build",
                "libraries/tools/kotlin-serialization/build",
                "libraries/tools/kotlin-source-map-loader/.gradle",
                "kotlin-native", "libraries/stdlib/native-wasm", // Have a separate licences manager
                "out",
                "repo/codebase-tests/tests/org/jetbrains/kotlin/code/CodeConformanceTest.kt",
                "repo/gradle-settings-conventions/kotlin-bootstrap/build/generated-sources",
                "repo/gradle-settings-conventions/cache-redirector/build/generated-sources",
                "repo/gradle-settings-conventions/jvm-toolchain-provisioning/build/generated-sources",
                "repo/gradle-settings-conventions/develocity/build/generated-sources",
                "repo/gradle-settings-conventions/kotlin-daemon-config/build/generated-sources",
                "repo/gradle-build-conventions/buildsrc-compat/build/generated-sources",
                "repo/gradle-build-conventions/generators/build/generated-sources",
                "repo/gradle-build-conventions/project-tests-convention/build/generated-sources",
                "repo/gradle-build-conventions/test-data-manager-convention/build/generated-sources",
                "repo/gradle-build-conventions/android-sdk-provisioner/build/generated-sources",
                "repo/gradle-build-conventions/asm-deprecating-transformer/build/generated-sources",
                "repo/gradle-build-conventions/binaryen-configuration/build/generated-sources",
                "repo/gradle-build-conventions/d8-configuration/build/generated-sources",
                "repo/gradle-build-conventions/nodejs-configuration/build/generated-sources",
                "repo/gradle-build-conventions/gradle-plugins-common/build/generated-sources",
                "repo/gradle-build-conventions/gradle-plugins-documentation/build/generated-sources",
                "repo/gradle-build-conventions/test-federation-convention/build/",
                "wasm/wasm.debug.browsers/node_modules",
                "wasm/wasm.debug.browsers/.gradle",
                ".gradle/expanded",
            )
        )
    }

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

    fun testParserCode() {
        val pattern = Pattern.compile("assert.*?\\b[^_]at.*?$", Pattern.MULTILINE)

        for (sourceFile in FileUtil.findFilesByMask(JAVA_FILE_PATTERN, File("compiler/frontend/src/org/jetbrains/kotlin/parsing"))) {
            val matcher = pattern.matcher(sourceFile.readText())
            if (matcher.find()) {
                fail("An at-method with side-effects is used inside assert: ${matcher.group()}\nin file: $sourceFile")
            }
        }
    }

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
                val (allowed, notAllowed) = (testCaseToMatchedFiles[test] ?: error("Should be added during initialization")).partition {
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

    fun testNoHardcodedPathAccess() {
        val violations = mutableListOf<String>()

        // Separate patterns for double- and single-quoted strings so the other quote type is
        // allowed inside each (rec 1). Friedl unrolled loop [^X\\\n]*(?:\\.[^X\\\n]*)* avoids
        // O(string_length) JVM regex stack recursion. (?<!\.)\.\./ skips ellipsis (...) and
        // matches both Unix (/) and Windows (\) path separators (rec 4).
        val quotedStringPattern = Pattern.compile(
            // Double-quoted strings: single quotes are allowed inside
            """["][^"\\\n]*(?:\\.[^"\\\n]*)*(?:(?<!\.)\.\./|dist/)[^"\\\n]*(?:\\.[^"\\\n]*)*["]""" +
            // Single-quoted strings: double quotes are allowed inside
            """|'[^'\\\n]*(?:\\.[^'\\\n]*)*(?:(?<!\.)\.\./|dist/)[^'\\\n]*(?:\\.[^'\\\n]*)*'"""
        )
        // Shared pattern used for triple-quoted strings and .args files (rec 2).
        val pathPattern = Pattern.compile("""(?:(?<!\.)\.\./|dist/)""")

        nonSourcesMatcher.excludeWalkTopDown(SOURCES_FILE_PATTERN).forEach { sourceFile ->
            if (sourceFile.extension == "js") return@forEach
            if (UNDECLARED_INPUT_ACCESS_EXCLUDED_FILES_AND_DIRS.matchWithContains(sourceFile)) return@forEach
            val content = sourceFile.readText()

            // Check single/double-quoted string literals (single-line, escaped-quote aware)
            val matcher = quotedStringPattern.matcher(content)
            while (matcher.find()) {
                violations.add("${sourceFile}: ${matcher.group()}")
            }

            // Check triple-quoted strings by splitting on """ — avoids regex recursion on multiline content
            val parts = content.split("\"\"\"")
            for (i in parts.indices) {
                if (i % 2 == 1) { // odd-indexed parts are inside triple-quoted strings
                    val m = pathPattern.matcher(parts[i])
                    if (m.find()) {
                        violations.add("${sourceFile}: [triple-quoted] ${m.group()}")
                    }
                }
            }
        }

        // Check testData .args files line by line using the same pathPattern (rec 2),
        // so ellipsis and Windows separators are handled consistently.
        nonSourcesMatcher.excludeWalkTopDown(ARGS_FILE_PATTERN).forEach { argsFile ->
            argsFile.readLines().forEachIndexed { index, line ->
                if (pathPattern.matcher(line).find()) {
                    violations.add("${argsFile}:${index + 1}: $line")
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail(buildString {
                appendLine("${violations.size} file(s) contain references to undeclared test inputs (../ or dist/).")
                appendLine("Tests must not access parent directories (../) or build artifacts (dist/).")
                appendLine("Violations found:")
                violations.forEach { appendLine("  $it") }
            })
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

    /**
     * Verify that no hardcoded File.pathSeparator is used in SSoT
     * Valid patterns:
     * - \${File.pathSeparator} in regular strings
     * - ${File.pathSeparator} in raw string literals ($$"...")
     * Invalid:
     * - ${File.pathSeparator} in regular strings (without \ or $$ prefix)
     */
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
