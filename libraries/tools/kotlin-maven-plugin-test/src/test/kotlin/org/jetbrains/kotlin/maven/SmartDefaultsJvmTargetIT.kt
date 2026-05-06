/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.jetbrains.kotlin.maven.test.TestVersions.Java.JDK_21
import org.junit.jupiter.api.DisplayName

@DisplayName("Auto-align jvmTarget/jdkRelease with the project's Java level")
class SmartDefaultsJvmTargetIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("Properties maven.compiler.source/target → jvmTarget auto-aligned")
    fun testAutoAlignFromTargetProperty(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.source", "17")
            setMavenProperty("maven.compiler.target", "17")

            build("package", "-X") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertJvmTargetAutoAlignedFromTarget("17", "17")
            }
        }
    }

    @MavenTest
    @DisplayName("Property maven.compiler.release → jvmTarget and jdkRelease auto-aligned")
    fun testAutoAlignFromReleaseProperty(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.release", "17")

            build("package", "-X") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertJvmTargetAutoAlignedFromRelease("17", "17")
            }
        }
    }

    @MavenTest
    @DisplayName("Properties target + release → release wins")
    fun testReleasePropertyWinsOverTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.target", "11")
            setMavenProperty("maven.compiler.release", "17")

            build("package") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertBuildLogJvmTargetDerivedFromMvnRelease("17", "17")
            }
        }
    }

    @MavenTest
    @DisplayName("Compiler plugin config source/target → jvmTarget auto-aligned")
    fun testAutoAlignFromCompilerPluginTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            addMavenCompilerPluginConfiguration("<source>17</source><target>17</target>")

            build("package", "-X") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertJvmTargetAutoAlignedFromTarget("17", "17")
            }
        }
    }

    @MavenTest
    @DisplayName("Compiler plugin config release → jvmTarget and jdkRelease auto-aligned")
    fun testAutoAlignFromCompilerPluginRelease(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            addMavenCompilerPluginConfiguration("<release>17</release>")

            build("package", "-X") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertJvmTargetAutoAlignedFromRelease("17", "17")
            }
        }
    }

    @MavenTest
    @DisplayName("Compiler plugin config release takes priority over maven.compiler.release property")
    fun testCompilerPluginConfigWinsOverProperty(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.release", "11")
            addMavenCompilerPluginConfiguration("<release>17</release>")

            build("package", "-X") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertJvmTargetAutoAlignedFromRelease("17", "17")
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit kotlin.compiler.jvmTarget is not overridden by maven.compiler.target")
    fun testKotlinJvmTargetPropertyOverridesTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.source", "11")
            setMavenProperty("maven.compiler.target", "11")
            setMavenProperty("kotlin.compiler.jvmTarget", "17")

            build("package") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertBuildLogJvmTargetNotDerivedFrom("maven.compiler.target")
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit kotlin.compiler.jvmTarget is not overridden by maven.compiler.release")
    fun testKotlinJvmTargetPropertyOverridesRelease(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.release", "11")
            setMavenProperty("kotlin.compiler.jvmTarget", "17")

            build("package") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertBuildLogJvmTargetNotDerivedFrom("maven.compiler.release")
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit kotlin.compiler.jdkRelease is not overridden by maven.compiler.release")
    fun testKotlinJdkReleasePropertyOverridesRelease(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.release", "11")
            setMavenProperty("kotlin.compiler.jdkRelease", "17")

            build("package", "-X") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertCompilerArgsContain("-Xjdk-release=17")
                assertBuildLogJvmTargetNotDerivedFrom("maven.compiler.release")
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit jvmTarget in kotlin plugin config is not overridden by maven.compiler.target")
    fun testKotlinPluginConfigJvmTargetOverridesTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.source", "17")
            setMavenProperty("maven.compiler.target", "17")
            addKotlinPluginLevelConfiguration("<jvmTarget>11</jvmTarget>")

            build("package") {
                assertClassFilesMajorVersion(JVM_11_MAJOR_VERSION, *allKotlinOutputPaths())
                assertBuildLogJvmTargetNotDerivedFrom("maven.compiler.target")
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit jvmTarget in kotlin plugin config is not overridden by maven.compiler.release")
    fun testKotlinPluginConfigJvmTargetOverridesRelease(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.release", "17")
            addKotlinPluginLevelConfiguration("<jvmTarget>11</jvmTarget>")

            build("package") {
                assertClassFilesMajorVersion(JVM_11_MAJOR_VERSION, *allKotlinOutputPaths())
                assertBuildLogJvmTargetNotDerivedFrom("maven.compiler.release")
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit jdkRelease in kotlin plugin config is not overridden by maven.compiler.release")
    fun testKotlinPluginConfigJdkReleaseOverrides(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.release", "17")
            addKotlinPluginLevelConfiguration("<jdkRelease>11</jdkRelease>")

            build("package", "-X") {
                assertClassFilesMajorVersion(JVM_11_MAJOR_VERSION, *allKotlinOutputPaths())
                assertCompilerArgsContain("-Xjdk-release=11")
                assertBuildLogJvmTargetNotDerivedFrom("maven.compiler.release")
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit kotlin.compiler.jvmTarget is not overridden by compiler plugin config target")
    fun testKotlinPropertyOverridesCompilerPluginTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            addMavenCompilerPluginConfiguration("<source>11</source><target>11</target>")
            setMavenProperty("kotlin.compiler.jvmTarget", "17")

            build("package") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertBuildLogJvmTargetNotDerivedFrom("maven.compiler.target")
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit kotlin.compiler.jdkRelease is not overridden by compiler plugin config release")
    fun testKotlinPropertyOverridesCompilerPluginRelease(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            addMavenCompilerPluginConfiguration("<release>11</release>")
            setMavenProperty("kotlin.compiler.jdkRelease", "17")

            build(
                "package", "-X",
                buildOptions = buildOptions.copy(javaVersion = JDK_21)
            ) {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
                assertCompilerArgsContain("-Xjdk-release=17")
                assertBuildLogJvmTargetNotDerivedFrom("maven.compiler.release")
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit jvmTarget in kotlin plugin config is not overridden by compiler plugin config target")
    fun testKotlinPluginConfigOverridesCompilerPluginTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            addMavenCompilerPluginConfiguration("<source>11</source><target>11</target>")
            addKotlinPluginLevelConfiguration("<jvmTarget>21</jvmTarget>")

            build("package", buildOptions = buildOptions.copy(javaVersion = JDK_21)) {
                assertClassFilesMajorVersion(JVM_21_MAJOR_VERSION, *allKotlinOutputPaths())
                assertBuildLogJvmTargetNotDerivedFrom("maven.compiler.target")
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit jdkRelease in kotlin plugin config is not overridden by compiler plugin config release")
    fun testKotlinPluginConfigJdkReleaseOverridesCompilerPluginRelease(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            addMavenCompilerPluginConfiguration("<release>11</release>")
            addKotlinPluginLevelConfiguration("<jdkRelease>21</jdkRelease>")

            build(
                "package", "-X",
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    extraMavenProperties = mapOf("kotlin.compiler.jdkHome" to jdk21)
                )
            ) {
                assertClassFilesMajorVersion(JVM_21_MAJOR_VERSION, *allKotlinOutputPaths())
                assertCompilerArgsContain("-Xjdk-release=21")
                assertBuildLogJvmTargetNotDerivedFrom("maven.compiler.release")
            }
        }
    }

    @MavenTest
    @DisplayName("Without maven.compiler.* properties Kotlin defaults to bytecode 8")
    fun testDefaultBytecodeWithNoProperties(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            build("package") {
                assertClassFilesMajorVersion(JVM_8_MAJOR_VERSION, *allKotlinOutputPaths())
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit kotlin.compiler.jvmTarget without maven.compiler.* produces correct bytecode")
    fun testExplicitJvmTargetWithoutMavenProperties(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("kotlin.compiler.jvmTarget", "21")

            build(
                "package",
                buildOptions = buildOptions.copy(javaVersion = JDK_21)
            ) {
                assertClassFilesMajorVersion(JVM_21_MAJOR_VERSION, *allKotlinOutputPaths())
            }
        }
    }

    @MavenTest
    @DisplayName("Explicit kotlin.compiler.jdkRelease without maven.compiler.* produces correct bytecode")
    fun testExplicitJdkReleaseWithoutMavenProperties(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("kotlin.compiler.jdkRelease", "17")

            build("package") {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *allKotlinOutputPaths())
            }
        }
    }

    @MavenTest
    @DisplayName("Auto-alignment from target does NOT restrict API - SequencedCollection compiles")
    fun testTargetAutoAlignDoesNotRestrictApi(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.source", "17")
            setMavenProperty("maven.compiler.target", "17")

            workDir.resolve("src/main/kotlin/org/jetbrains/Jdk21Api.kt").toFile().writeText(
                """
                package org.jetbrains
                fun firstSign(signs: java.util.SequencedCollection<Sign>): Sign = signs.first
                """.trimIndent()
            )

            // KT-71048: Kotlin daemon doesn't discriminate by JVM version, so jdkHome is needed
            // to ensure the compiler sees JDK 21 classpath regardless of the daemon's JDK.
            build(
                "compile",
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    extraMavenProperties = mapOf("kotlin.compiler.jdkHome" to jdk21)
                )
            ) {
                assertClassFilesMajorVersion(JVM_17_MAJOR_VERSION, *mainKotlinOutputPaths())
                assertBuildLogJvmTargetDerivedFromMvnTarget("17", "17")
                assertFileExists("target/classes/org/jetbrains/Jdk21ApiKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Auto-alignment from release does restrict API - SequencedCollection fails")
    fun testReleaseAutoAlignRestrictsApi(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.release", "17")

            workDir.resolve("src/main/kotlin/org/jetbrains/Jdk21Api.kt").toFile().writeText(
                """
                package org.jetbrains
                fun firstSign(signs: java.util.SequencedCollection<Sign>): Sign = signs.first
                """.trimIndent()
            )

            // KT-71048: jdkHome needed to ensure the compiler sees JDK 21 classpath
            build(
                "compile",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    extraMavenProperties = mapOf("kotlin.compiler.jdkHome" to jdk21)
                )
            ) {
                assertFileNotExists("target/classes/org/jetbrains/Jdk21ApiKt.class")
                assertBuildLogJvmTargetDerivedFromMvnRelease("17", "17")
                assertBuildLogContains("Unresolved reference 'SequencedCollection'")
            }
        }
    }

    @MavenTest
    @DisplayName("maven.compiler.target=1.7 is unsupported — warning logged, Kotlin defaults to bytecode 8")
    fun testUnsupportedTargetVersion(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.source", "1.7")
            setMavenProperty("maven.compiler.target", "1.7")

            build(
                "package",
                buildOptions = buildOptions.copy(javaVersion = TestVersions.Java.JDK_11)
                    .withoutKotlinDaemon("KT-71048: prevents JDK11 daemon contamination of subsequent JDK17 tests")
            ) {
                assertBuildLogContains("[WARNING] maven.compiler.target=1.7 is not supported as a Kotlin jvmTarget")
                assertClassFilesMajorVersion(JVM_8_MAJOR_VERSION, *allKotlinOutputPaths())
            }
        }
    }

    @MavenTest
    @DisplayName("maven.compiler.release=7 is unsupported — warning logged, Kotlin defaults to bytecode 8")
    fun testUnsupportedReleaseVersion(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.release", "7")

            build(
                "package",
                buildOptions = buildOptions.copy(javaVersion = TestVersions.Java.JDK_11)
                    .withoutKotlinDaemon("KT-71048: prevents JDK11 daemon contamination of subsequent JDK17 tests")
            ) {
                assertBuildLogContains("[WARNING] maven.compiler.release=7 is not supported as a Kotlin jvmTarget")
                assertClassFilesMajorVersion(JVM_8_MAJOR_VERSION, *allKotlinOutputPaths())
            }
        }
    }

    @MavenTest
    @DisplayName("Non-existent JDK release version is forwarded to Kotlin compiler which rejects it")
    fun testUnsupportedHighReleaseVersion(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.release", "99")

            build("compile", expectedToFail = true) {
                assertBuildLogJvmTargetDerivedFromMvnRelease("99", "99")
                assertBuildLogContains("Unknown -Xjdk-release value: 99")
            }
        }
    }

    @MavenTest
    @DisplayName("Multi-project setup: parent release=17, child inherits → bytecode 17 in both subprojects")
    fun testMultiModuleParentReleaseInherited(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-jvm-target-multimodule", mavenVersion) {
            setMavenProperty("maven.compiler.release", "17")

            build("package") {
                assertClassFilesMajorVersion(
                    JVM_17_MAJOR_VERSION,
                    "lib/target/classes/witcher/Sign.class",
                    "lib/target/classes/witcher/Potion.class",
                    "app/target/classes/witcher/Witcher.class",
                    "app/target/classes/witcher/WitcherSchool.class",
                )
                assertBuildLogLineCount("[INFO] Using jvmTarget=17 (derived from maven.compiler.release=17)", 3)
            }
        }
    }

    @MavenTest
    @DisplayName("Multi-project setup: parent release=17, child overrides kotlin.compiler.jvmTarget=21 → child Kotlin bytecode 21")
    fun testMultiModuleChildOverridesParent(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-jvm-target-multimodule", mavenVersion) {
            setMavenProperty("maven.compiler.release", "17")
            setMavenProperty("kotlin.compiler.jvmTarget", "21", "app/pom.xml")

            build(
                "package",
                buildOptions = buildOptions.copy(javaVersion = JDK_21)
            ) {
                assertBuildLogLineCount("[INFO] Using jvmTarget=17 (derived from maven.compiler.release=17)", 2)
                assertClassFilesMajorVersion(
                    JVM_17_MAJOR_VERSION,
                    "lib/target/classes/witcher/Sign.class",
                    "lib/target/classes/witcher/Potion.class",
                )
                assertClassFilesMajorVersion(
                    JVM_21_MAJOR_VERSION,
                    "app/target/classes/witcher/Witcher.class",
                )
                // Java in app still compiles with release=17 from parent
                assertClassFilesMajorVersion(
                    JVM_17_MAJOR_VERSION,
                    "app/target/classes/witcher/WitcherSchool.class",
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Multi-project setup:: parent target=11, child overrides maven.compiler.release=17 → child bytecode 17")
    fun testMultiModuleChildOverridesParentTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-jvm-target-multimodule", mavenVersion) {
            setMavenProperty("maven.compiler.source", "11")
            setMavenProperty("maven.compiler.target", "11")
            setMavenProperty("maven.compiler.release", "17", "app/pom.xml")

            build("package") {
                assertBuildLogLineCount("[INFO] Using jvmTarget=11 (derived from maven.compiler.target=11)", 2)
                assertClassFilesMajorVersion(
                    JVM_11_MAJOR_VERSION,
                    "lib/target/classes/witcher/Sign.class",
                    "lib/target/classes/witcher/Potion.class",
                )
                assertClassFilesMajorVersion(
                    JVM_17_MAJOR_VERSION,
                    "app/target/classes/witcher/Witcher.class",
                    "app/target/classes/witcher/WitcherSchool.class",
                )
            }
        }
    }

    @MavenTest
    @DisplayName("maven.compiler.target=8 → jvmTarget auto-aligned to 1.8")
    fun testAutoAlignFromTarget8(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.source", "8")
            setMavenProperty("maven.compiler.target", "8")

            build("package", "-X") {
                assertClassFilesMajorVersion(JVM_8_MAJOR_VERSION, *allKotlinOutputPaths())
                assertJvmTargetAutoAlignedFromTarget("1.8", "8")
            }
        }
    }

    @MavenTest
    @DisplayName("maven.compiler.release=8 → jvmTarget and jdkRelease auto-aligned to 1.8")
    fun testAutoAlignFromRelease8(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            setMavenProperty("maven.compiler.release", "8")

            build("package", "-X") {
                assertClassFilesMajorVersion(JVM_8_MAJOR_VERSION, *allKotlinOutputPaths())
                assertJvmTargetAutoAlignedFromRelease("1.8", "8")
            }
        }
    }

    @MavenTest
    @DisplayName("Smart defaults disabled via property - jvmTarget is NOT auto-aligned from maven.compiler.release")
    fun testSmartDefaultsDisabledNoJvmTargetAlignment(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-disabled-via-property", mavenVersion) {
            setMavenProperty("maven.compiler.release", "17")

            build("package") {
                assertSmartDefaultsNotEnabled()
                assertClassFilesMajorVersion(JVM_8_MAJOR_VERSION, "target/classes/test/MainKt.class")
            }
        }
    }

    companion object {
        private const val JVM_8_MAJOR_VERSION = 52
        private const val JVM_11_MAJOR_VERSION = 55
        private const val JVM_17_MAJOR_VERSION = 61
        private const val JVM_21_MAJOR_VERSION = 65
    }

    private fun mainKotlinOutputPaths() = arrayOf(
        "target/classes/org/jetbrains/Witcher.class",
        "target/classes/org/jetbrains/Sign.class",
    )

    private fun testKotlinOutputPaths() = arrayOf(
        "target/test-classes/org/jetbrains/Bestiary.class",
        "target/test-classes/org/jetbrains/WitcherKotlinTest.class",
    )

    private fun allKotlinOutputPaths() = mainKotlinOutputPaths() + testKotlinOutputPaths()
}
