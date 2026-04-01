/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

class ICCompilerArgumentsTrackingIT : KotlinMavenTestBase() {
    @MavenTest
    @DisplayName("Adding compiler language version triggers full rebuild")
    fun rebuildOnNewLanguageVersion(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package", "-X") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
            }

            build("package", "-X", "-Dkotlin.compiler.languageVersion=$FirstSupportedLanguageVersion") {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertBuildLogContains("-language-version $FirstSupportedLanguageVersion")
                assertFilesExist(*kotlinSimpleOutputPaths())
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/Dummy.kt", "src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    @DisplayName("Removing compiler language version argument triggers full rebuild")
    fun rebuildOnRemovedLanguageVersion(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package", "-X", "-Dkotlin.compiler.languageVersion=$FirstSupportedLanguageVersion") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
                assertBuildLogContains("-language-version $FirstSupportedLanguageVersion")
            }

            build("package", "-X") {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertBuildLogDoesNotContain("-language-version $FirstSupportedLanguageVersion")
                assertFilesExist(*kotlinSimpleOutputPaths())
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/Dummy.kt", "src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    @Disabled("KT-85329: Tracking state is lost after compilation error. Enable when fixed.")
    @DisplayName("Changed compiler arguments are detected after compilation error")
    fun rebuildAfterCompilationError(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build(
                "package", "-X",
                "-Dkotlin.compiler.apiVersion=$FirstSupportedLanguageVersion",
                "-Dkotlin.compiler.languageVersion=$FirstSupportedLanguageVersion",
            ) {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
                assertBuildLogContains("-api-version $FirstSupportedLanguageVersion")
                assertBuildLogContains("-language-version $FirstSupportedLanguageVersion")
            }

            build(
                "package",
                "-Dkotlin.compiler.apiVersion=$NonDeprecatedLanguageVersion",
                "-Dkotlin.compiler.languageVersion=$FirstSupportedLanguageVersion",
                expectedToFail = true,
            ) {
                assertCompilationFailed()
                assertBuildLogContains("-api-version ($NonDeprecatedLanguageVersion) cannot be greater than -language-version ($FirstSupportedLanguageVersion)")
            }

            build(
                "package", "-X",
                "-Dkotlin.compiler.apiVersion=$NonDeprecatedLanguageVersion",
                "-Dkotlin.compiler.languageVersion=$NonDeprecatedLanguageVersion",
            ) {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
                assertBuildLogContains("-api-version $NonDeprecatedLanguageVersion")
                assertBuildLogContains("-language-version $NonDeprecatedLanguageVersion")
                assertFilesExist(*kotlinSimpleOutputPaths())
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/Dummy.kt", "src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    @DisplayName("Disabling inputs tracking prevents full rebuild on argument adding")
    fun noRebuildWhenDisabled(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package", DISABLE_INPUTS_TRACKING)

            build(
                "package",
                "-X",
                "-Dkotlin.compiler.languageVersion=$FirstSupportedLanguageVersion",
                DISABLE_INPUTS_TRACKING
            ) {
                assertBuildLogDoesNotContain(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertFilesExist(*kotlinSimpleOutputPaths())
                assertCompiledKotlin()
            }
        }
    }

    @MavenTest
    @DisplayName("Changing jvmTarget triggers full rebuild and produces correct bytecode version")
    fun rebuildOnJvmTargetChange(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package", "-X", "-Dkotlin.compiler.jvmTarget=11") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
                assertClassFileMajorVersion("target/classes/A.class", JVM_11_MAJOR_VERSION)
            }

            build("package", "-X", "-Dkotlin.compiler.jvmTarget=17") {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertClassFileMajorVersion("target/classes/A.class", JVM_17_MAJOR_VERSION)
                assertFilesExist(*kotlinSimpleOutputPaths())
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/Dummy.kt", "src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    @DisplayName("Disabling tracking ignores previously tracked state, producing stale bytecode")
    fun noRebuildOnJvmTargetWhenDisabled(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package", "-X", "-Dkotlin.compiler.jvmTarget=11") {
                assertClassFileMajorVersion("target/classes/A.class", JVM_11_MAJOR_VERSION)
            }

            build("package", "-X", DISABLE_INPUTS_TRACKING, "-Dkotlin.compiler.jvmTarget=17") {
                assertBuildLogDoesNotContain(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertClassFileMajorVersion("target/classes/A.class", JVM_11_MAJOR_VERSION)
                assertCompiledKotlin()
            }
        }
    }

    @MavenTest
    @DisplayName("Adding jvmTarget via XML configuration triggers full rebuild")
    fun rebuildOnPomXmlJvmTarget(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package", "-X") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
            }

            addPluginLevelConfiguration("<jvmTarget>17</jvmTarget>")

            build("package", "-X") {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertClassFileMajorVersion("target/classes/A.class", JVM_17_MAJOR_VERSION)
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/Dummy.kt", "src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    @DisplayName("Adding extra args via XML configuration triggers full rebuild")
    fun rebuildOnPomXmlExtraArgs(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            // Add inline function without lambda params to produce warning
            workDir.resolve("src/main/kotlin/Dummy.kt").replaceFirstInFile(
                "class Dummy",
                "inline fun nothingToInline() = 42\n\nclass Dummy"
            )

            build("package", "-X") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
            }

            addPluginLevelConfiguration(
                """
                <args>
                    <arg>-Werror</arg>
                </args>
                """.trimIndent()
            )

            build("package", "-X", expectedToFail = true) {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertCompilationFailed()
            }
        }
    }

    @MavenTest
    @DisplayName("Adding compiler plugin options triggers full rebuild")
    fun rebuildOnPluginOptions(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimpleWithAllOpen", mavenVersion) {
            build("package", "-X") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
                assertClassFinal("test.OpenClass", isFinal = true)
            }

            addPluginOptions("all-open:annotation=test.AllOpen")

            build("package", "-X") {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertClassFinal("test.OpenClass", isFinal = false)
                assertCompiledKotlin("src/main/kotlin/test.kt")
            }
        }
    }

    @MavenTest
    @DisplayName("Adding argfile reference triggers full rebuild")
    fun rebuildOnArgfile(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package", "-X") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
            }

            workDir.resolve("compiler-warnings.argfile").toFile().writeText(
                "-Xwarning-level=USELESS_CAST:error\n"
            )

            addPluginLevelConfiguration(
                """
                <args>
                    <arg>@compiler-warnings.argfile</arg>
                </args>
                """.trimIndent()
            )

            build("package", "-X") {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertCompilerArgsContain("-Xwarning-level=USELESS_CAST:error")
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/Dummy.kt", "src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    @DisplayName("Adding args to compile execution triggers rebuild of main sources only")
    fun rebuildMainOnlyOnCompileExecutionChange(mavenVersion: TestVersions.Maven) {
        testProject("kotlinWithTests", mavenVersion) {
            build("compile", "-X") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
            }

            addToCompileExecutionConfiguration(
                """
                <args>
                    <arg>-Xjvm-default=all</arg>
                </args>
                """.trimIndent()
            )

            build("compile", "-X") {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertCompiledKotlin("src/main/kotlin/SomeMain.kt")
            }
        }
    }

    @MavenTest
    @DisplayName("Adding args to test-compile execution triggers rebuild of test sources only")
    fun rebuildTestOnlyOnTestCompileExecutionChange(mavenVersion: TestVersions.Maven) {
        testProject("kotlinWithTests", mavenVersion) {
            build("test-compile", "-X") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
            }

            addToTestCompileExecutionConfiguration(
                """
                <args>
                    <arg>-Xno-call-assertions</arg>
                </args>
                """.trimIndent()
            )

            build("test-compile", "-X") {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertCompiledKotlin("src/test/kotlin/BaseTests.kt", "src/test/kotlin/SomeTests.kt")
            }
        }
    }

    @MavenTest
    @DisplayName("Activating Maven profile with different jvmTarget triggers full rebuild")
    fun rebuildOnProfileActivation(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            addMavenProfile("17", "kotlin.compiler.jvmTarget" to "17")

            build("package", "-X") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
            }

            build("package", "-X", "-P17") {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertClassFileMajorVersion("target/classes/A.class", JVM_17_MAJOR_VERSION)
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/Dummy.kt", "src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    @DisplayName("Adding javaParameters configuration property triggers full rebuild")
    fun rebuildOnJavaParameters(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package", "-X") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
            }

            addToCompileExecutionConfiguration("<javaParameters>true</javaParameters>")

            build("package", "-X") {
                assertBuildLogContains(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertCompilerArgsContain("-java-parameters")
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/Dummy.kt", "src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    @DisplayName("Argument with affectsCompilationOutcome=false does not trigger rebuild")
    fun noRebuildOnNowarn(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            // Add inline function without lambda params to produce warning
            workDir.resolve("src/main/kotlin/Dummy.kt").replaceFirstInFile(
                "class Dummy",
                "inline fun nothingToInline() = 42\n\nclass Dummy"
            )

            build("package", "-X") {
                assertBuildLogContains(UNKNOWN_INPUTS_CHANGES_MESSAGE)
                assertBuildLogContains("Expected performance impact from inlining is insignificant")
            }

            // nowarn has affectsCompilationOutcome=false, so adding it alone does not trigger recompilation
            addToCompileExecutionConfiguration("<nowarn>true</nowarn>")

            build("package", "-X") {
                assertBuildLogDoesNotContain(COMPILER_ARGUMENTS_CHANGED_MESSAGE)
                assertCompiledKotlin()
            }

            // Trigger incremental recompilation via source change - warning should be suppressed
            workDir.resolve("src/main/kotlin/Dummy.kt").replaceFirstInFile("class Dummy", "class Dummy { val x = 1 }")

            build("package", "-X") {
                assertBuildLogDoesNotContain("Expected performance impact from inlining is insignificant")
                assertCompiledKotlin("src/main/kotlin/Dummy.kt")
            }
        }
    }

    private fun kotlinSimpleOutputPaths() = arrayOf(
        "target/classes/test.properties",
        "target/classes/A.class",
        "target/classes/UseAKt.class",
        "target/classes/Dummy.class",
        "target/classes/JavaUtil.class",
        "target/classes/JavaAUser.class"
    )

    companion object {
        private val FirstSupportedLanguageVersion = org.jetbrains.kotlin.config.LanguageVersion.FIRST_SUPPORTED
        private val NonDeprecatedLanguageVersion = org.jetbrains.kotlin.config.LanguageVersion.FIRST_NON_DEPRECATED

        private const val UNKNOWN_INPUTS_CHANGES_MESSAGE =
            "Non-incremental compilation will be performed: Unknown inputs changes"
        private const val COMPILER_ARGUMENTS_CHANGED_MESSAGE =
            "Non-incremental compilation will be performed: Compiler arguments changed"

        private const val DISABLE_INPUTS_TRACKING = "-Dkotlin.compiler.incremental.inputs.track=false"

        private const val JVM_11_MAJOR_VERSION = 55
        private const val JVM_17_MAJOR_VERSION = 61
    }
}
