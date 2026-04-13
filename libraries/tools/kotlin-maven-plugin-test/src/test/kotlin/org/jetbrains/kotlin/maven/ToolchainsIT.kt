/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.jetbrains.kotlin.maven.test.TestVersions.Java.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

@DisplayName("Maven Toolchains support")
class ToolchainsIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("Toolchain JDK is used when kotlin.compiler.jdkHome is not set")
    fun testToolchainUsedWhenJdkHomeNotSet(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            addMavenToolchainsPlugin("21")

            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21)
                assertToolchainAppliedToGoal("test-compile", jdk21)
                assertFilesExist(*mainOutputPaths())
                assertFilesExist(*testOutputPaths())
                assertClassFileMajorVersion("target/classes/hogwarts/Wizard.class", 52)
                assertClassFileMajorVersion("target/test-classes/hogwarts/WizardTest.class", 52)
                assertTestsPassed(3)
            }
        }
    }

    @MavenTest
    @DisplayName("Toolchain provides JDK 21 API for compile when JAVA_HOME is JDK 17")
    fun testToolchainProvidesHigherApiForCompile(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            workDir.resolve("src/main/kotlin/hogwarts/Jdk21Api.kt").toFile().writeText(
                """
                package hogwarts
                fun firstSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent()
            )
            addMavenToolchainsPlugin("21")

            build(
                "compile",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21)
                assertFileExists("target/classes/hogwarts/Jdk21ApiKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Toolchain provides JDK 21 API for test-compile when JAVA_HOME is JDK 17")
    fun testToolchainProvidesHigherApiForTestCompile(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            workDir.resolve("src/test/kotlin/hogwarts/Jdk21TestApi.kt").toFile().writeText(
                """
                package hogwarts
                fun firstTestSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent()
            )
            addMavenToolchainsPlugin("21")

            build(
                "test-compile",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("test-compile", jdk21)
                assertFileExists("target/test-classes/hogwarts/Jdk21TestApiKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Toolchain is ignored when kotlin.compiler.jdkHome is explicitly set")
    fun testToolchainIgnoredWhenJdkHomeSet(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            workDir.resolve("src/main/kotlin/hogwarts/Jdk21Api.kt").toFile().writeText(
                """
                package hogwarts
                fun firstSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent()
            )
            workDir.resolve("src/test/kotlin/hogwarts/Jdk21TestApi.kt").toFile().writeText(
                """
                package hogwarts
                fun firstTestSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent()
            )
            addMavenToolchainsPlugin("17")

            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17),
                    extraMavenProperties = mapOf("kotlin.compiler.jdkHome" to jdk21)
                )
            ) {
                assertToolchainIgnoredByGoal("compile", jdk21)
                assertToolchainIgnoredByGoal("test-compile", jdk21)
                assertFileExists("target/classes/hogwarts/Jdk21ApiKt.class")
                assertFileExists("target/test-classes/hogwarts/Jdk21TestApiKt.class")
                assertTestsPassed(3)
            }
        }
    }

    @MavenTest
    @DisplayName("jdkHome overrides toolchain for compilation, but surefire still runs on toolchain JDK")
    fun testJdkHomeOverrideDoesNotAffectSurefireRuntime(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            workDir.resolve("src/main/kotlin/hogwarts/Jdk21Api.kt").toFile().writeText(
                """
                package hogwarts
                fun firstSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent()
            )
            workDir.resolve("src/test/kotlin/hogwarts/Jdk21ApiTest.kt").toFile().writeText(
                """
                package hogwarts
                import org.junit.Test
                import org.junit.Assert.*
                class Jdk21ApiTest {
                    @Test fun firstSpellWorks() {
                        val spells = java.util.LinkedHashSet<Spell>()
                        spells.add(Spell("Lumos", 1))
                        assertEquals(Spell("Lumos", 1), firstSpell(spells))
                    }
                }
            """.trimIndent()
            )
            addMavenToolchainsPlugin("17")

            build(
                "test",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17),
                    extraMavenProperties = mapOf("kotlin.compiler.jdkHome" to jdk21)
                )
            ) {
                assertToolchainIgnoredByGoal("compile", jdk21)
                assertToolchainIgnoredByGoal("test-compile", jdk21)
                assertFileExists("target/classes/hogwarts/Jdk21ApiKt.class")
                assertFileExists("target/test-classes/hogwarts/Jdk21ApiTest.class")
                assertBuildLogContains("NoClassDefFoundError: java/util/SequencedCollection")
            }
        }
    }

    @MavenTest
    @DisplayName("Toolchain JDK 17 restricts classpath — JDK 21 API causes compilation failure")
    fun testToolchainRestrictsClasspath(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            workDir.resolve("src/main/kotlin/hogwarts/Jdk21Api.kt").toFile().writeText(
                """
                package hogwarts
                fun firstSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent()
            )
            addMavenToolchainsPlugin("17")

            build(
                "compile",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17)
                )
            ) {
                assertCompilationFailed()
                assertToolchainAppliedToGoal("compile", jdk17)
                assertGoalLogContains("compile", "Unresolved reference 'SequencedCollection'")
                assertFileNotExists("target/classes/hogwarts/Jdk21ApiKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Toolchain JDK 17 restricts test-compile classpath — JDK 21 API causes failure")
    fun testToolchainRestrictsTestCompileClasspath(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            workDir.resolve("src/test/kotlin/hogwarts/Jdk21TestApi.kt").toFile().writeText(
                """
                package hogwarts
                fun firstTestSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent()
            )
            addMavenToolchainsPlugin("17")

            build(
                "test-compile",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17)
                )
            ) {
                assertCompilationFailed()
                assertToolchainAppliedToGoal("test-compile", jdk17)
                assertGoalLogContains("test-compile", "Unresolved reference 'SequencedCollection'")
                assertFileNotExists("target/test-classes/hogwarts/Jdk21TestApiKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("jdkToolchain does not affect test runtime — surefire uses JAVA_HOME")
    fun testJdkToolchainDoesNotAffectSurefireRuntime(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            workDir.resolve("src/main/kotlin/hogwarts/Jdk21Magic.kt").toFile().writeText(
                """
                package hogwarts
                fun reversedSpells(spells: java.util.SequencedCollection<Spell>) = spells.reversed()
            """.trimIndent()
            )
            workDir.resolve("src/test/kotlin/hogwarts/Jdk21MagicTest.kt").toFile().writeText(
                """
                package hogwarts
                import org.junit.Test
                import org.junit.Assert.*
                class Jdk21MagicTest {
                    @Test fun reversedSpellsWork() {
                        val spells = java.util.LinkedHashSet<Spell>()
                        spells.add(Spell("Lumos", 1))
                        val reversed = reversedSpells(spells)
                        assertNotNull(reversed)
                    }
                }
            """.trimIndent()
            )
            configureJdkToolchain("21")

            build(
                "test",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21)
                assertToolchainAppliedToGoal("test-compile", jdk21)
                assertFileExists("target/classes/hogwarts/Jdk21MagicKt.class")
                assertFileExists("target/test-classes/hogwarts/Jdk21MagicTest.class")
                assertBuildLogContains("NoClassDefFoundError: java/util/SequencedCollection")
            }
        }
    }

    @MavenTest
    @DisplayName("maven-toolchains-plugin affects surefire runtime — tests pass on toolchain JDK")
    fun testMavenToolchainsPluginAffectsSurefireRuntime(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            workDir.resolve("src/main/kotlin/hogwarts/Jdk21Magic.kt").toFile().writeText(
                """
                package hogwarts
                fun reversedSpells(spells: java.util.SequencedCollection<Spell>) = spells.reversed()
            """.trimIndent()
            )
            workDir.resolve("src/test/kotlin/hogwarts/Jdk21MagicTest.kt").toFile().writeText(
                """
                package hogwarts
                import org.junit.Test
                import org.junit.Assert.*
                class Jdk21MagicTest {
                    @Test fun reversedSpellsWork() {
                        val spells = java.util.LinkedHashSet<Spell>()
                        spells.add(Spell("Lumos", 1))
                        val reversed = reversedSpells(spells)
                        assertNotNull(reversed)
                    }
                }
            """.trimIndent()
            )
            addMavenToolchainsPlugin("21")

            build(
                "test",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21)
                assertToolchainAppliedToGoal("test-compile", jdk21)
                assertFileExists("target/classes/hogwarts/Jdk21MagicKt.class")
                assertFileExists("target/test-classes/hogwarts/Jdk21MagicTest.class")
                assertTestsPassed(4)
            }
        }
    }

    @MavenTest
    @DisplayName("Toolchain with matching jvmTarget produces correct bytecode version")
    fun testToolchainWithMatchingJvmTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            addMavenToolchainsPlugin("17")

            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17),
                    extraMavenProperties = mapOf("kotlin.compiler.jvmTarget" to "17")
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk17)
                assertToolchainAppliedToGoal("test-compile", jdk17)
                assertClassFileMajorVersion("target/classes/hogwarts/Wizard.class", 61)
                assertClassFileMajorVersion("target/test-classes/hogwarts/WizardTest.class", 61)
            }
        }
    }

    @MavenTest
    @DisplayName("Toolchain with lower jvmTarget produces correct bytecode version")
    fun testToolchainWithLowerJvmTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            addMavenToolchainsPlugin("17")

            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17),
                    extraMavenProperties = mapOf("kotlin.compiler.jvmTarget" to "11")
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk17)
                assertToolchainAppliedToGoal("test-compile", jdk17)
                assertClassFileMajorVersion("target/classes/hogwarts/Wizard.class", 55)
                assertClassFileMajorVersion("target/test-classes/hogwarts/WizardTest.class", 55)
                assertTestsPassed(3)
            }
        }
    }

    @MavenTest
    @DisplayName("jvmTarget higher than toolchain JDK causes javac failure on Kotlin bytecode")
    fun testToolchainWithHigherJvmTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            addMavenToolchainsPlugin("17")

            // Kotlin compiles to bytecode 65 (JDK 21), but maven-compiler-plugin on toolchain JDK 17
            // cannot read these class files, causing "cannot access Wizard".
            build(
                "package",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17),
                    extraMavenProperties = mapOf("kotlin.compiler.jvmTarget" to "21")
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk17)
                assertClassFileMajorVersion("target/classes/hogwarts/Wizard.class", 65)
                assertBuildLogContains("error: cannot access Wizard")
            }
        }
    }

    @MavenTest
    @DisplayName("Toolchain from Maven profile with maven-toolchains-plugin is used")
    fun testToolchainFromMavenProfile(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            addMavenToolchainsPlugin("21", profileId = "toolchain-jdk21")

            build(
                "-Ptoolchain-jdk21",
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21)
                assertToolchainAppliedToGoal("test-compile", jdk21)
                assertFilesExist(*mainOutputPaths())
                assertFilesExist(*testOutputPaths())
                assertClassFileMajorVersion("target/classes/hogwarts/Wizard.class", 52)
                assertClassFileMajorVersion("target/test-classes/hogwarts/WizardTest.class", 52)
                assertTestsPassed(3)
            }
        }
    }

    @MavenTest
    @DisplayName("jdkToolchain in plugin config is used")
    fun testJdkToolchainInPluginConfig(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            configureJdkToolchain("21")

            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21)
                assertToolchainAppliedToGoal("test-compile", jdk21)
                assertFilesExist(*mainOutputPaths())
                assertFilesExist(*testOutputPaths())
                assertTestsPassed(3)
            }
        }
    }

    @MavenTest
    @DisplayName("jdkToolchain JDK 17 restricts compile classpath — JDK 21 API causes failure")
    fun testJdkToolchainRestrictsCompileClasspath(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            configureJdkToolchain("17")

            workDir.resolve("src/main/kotlin/hogwarts/Jdk21Api.kt").toFile().writeText("""
                package hogwarts
                fun firstSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent())

            build(
                "compile",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17)
                )
            ) {
                assertCompilationFailed()
                assertToolchainAppliedToGoal("compile", jdk17)
                assertGoalLogContains("compile", "Unresolved reference 'SequencedCollection'")
                assertFileNotExists("target/classes/hogwarts/Jdk21ApiKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("jdkToolchain JDK 17 restricts test-compile classpath — JDK 21 API causes failure")
    fun testJdkToolchainRestrictsTestCompileClasspath(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            configureJdkToolchain("17")

            workDir.resolve("src/test/kotlin/hogwarts/Jdk21TestApi.kt").toFile().writeText("""
                package hogwarts
                fun firstTestSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent())

            build(
                "test-compile",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17)
                )
            ) {
                assertToolchainAppliedToGoal("test-compile", jdk17)
                assertGoalLogContains("test-compile", "Unresolved reference 'SequencedCollection'")
                assertFileNotExists("target/test-classes/hogwarts/Jdk21TestApiKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("maven-toolchains-plugin selects correct toolchain from multiple entries in toolchains.xml")
    fun testMavenToolchainsPluginSelectedFromMultipleEntries(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            addMavenToolchainsPlugin("21")

            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_17, JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21)
                assertToolchainAppliedToGoal("test-compile", jdk21)
                assertTestsPassed(3)
            }
        }
    }

    @MavenTest
    @DisplayName("jdkToolchain selects correct toolchain from multiple entries in toolchains.xml")
    fun testJdkToolchainSelectedFromMultipleEntries(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            configureJdkToolchain("21")

            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_17, JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21)
                assertToolchainAppliedToGoal("test-compile", jdk21)
            }
        }
    }

    @MavenTest
    @DisplayName("No toolchain override when requested jdkToolchain is not found")
    fun testJdkToolchainNotFound(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            configureJdkToolchain("99")

            build(
                "package", "-X",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17)
                )
            ) {
                assertBuildLogDoesNotContain("Overriding JDK home path with toolchain JDK:")
                assertCompilerArgsDoNotContain("-jdk-home")
                assertFilesExist(*mainOutputPaths())
                assertFilesExist(*testOutputPaths())
                assertTestsPassed(3)
            }
        }
    }

    @MavenTest
    @DisplayName("jdkToolchain with version range selects matching toolchain")
    fun testJdkToolchainWithVersionRange(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            configureJdkToolchain("[21,)")

            workDir.resolve("src/main/kotlin/hogwarts/Jdk21Api.kt").toFile().writeText("""
                package hogwarts
                fun firstSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent())

            build(
                "compile",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_17, JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21)
                assertFileExists("target/classes/hogwarts/Jdk21ApiKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("jdkToolchain with version range restricts classpath to matched toolchain")
    fun testJdkToolchainWithVersionRangeRestrictsClasspath(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains", mavenVersion) {
            configureJdkToolchain("[11,17]")

            workDir.resolve("src/main/kotlin/hogwarts/Jdk21Api.kt").toFile().writeText("""
                package hogwarts
                fun firstSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent())

            build(
                "compile",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_21,
                    toolchains = listOf(JDK_17, JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk17)
                assertGoalLogContains("compile", "Unresolved reference 'SequencedCollection'")
                assertFileNotExists("target/classes/hogwarts/Jdk21ApiKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Toolchain applies to all modules in multi-module project")
    fun testToolchainInMultiModuleProject(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains-multimodule", mavenVersion) {
            addMavenToolchainsPlugin("21")

            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21, module = "test-toolchains-lib")
                assertToolchainAppliedToGoal("compile", jdk21, module = "test-toolchains-app")
                assertFileExists("lib/target/classes/hogwarts/Spell.class")
                assertFileExists("app/target/classes/hogwarts/Wizard.class")
            }
        }
    }

    @MavenTest
    @DisplayName("jdkToolchain in child module overrides parent maven-toolchains-plugin")
    fun testJdkToolchainOverridesParentMavenToolchain(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains-multimodule", mavenVersion) {
            addMavenToolchainsPlugin("17")
            modifyPomXml("app/pom.xml") {
                val build = documentElement.getOrCreateChild("build")
                val plugins = build.getOrCreateChild("plugins")
                plugins.appendXmlFragment(
                    $$"""
                    <plugin>
                        <artifactId>kotlin-maven-plugin</artifactId>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <version>${kotlin.version}</version>
                        <configuration>
                            <jdkToolchain><version>21</version></jdkToolchain>
                        </configuration>
                    </plugin>
                """.trimIndent())
            }

            workDir.resolve("app/src/main/kotlin/hogwarts/Jdk21Api.kt").toFile().writeText("""
                package hogwarts
                fun firstSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent())

            build(
                "compile",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_17, JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21, module = "test-toolchains-app")
                assertToolchainAppliedToGoal("compile", jdk17, module = "test-toolchains-lib")
                assertFileExists("app/target/classes/hogwarts/Jdk21ApiKt.class")
                assertFileExists("lib/target/classes/hogwarts/Spell.class")
                assertFileExists("app/target/classes/hogwarts/Wizard.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Module on parent toolchain cannot use JDK 21 API from lib compiled with jdkToolchain 21")
    fun testParentToolchainRestrictsModuleUsingHigherApiFromLib(mavenVersion: TestVersions.Maven) {
        testProject("test-toolchains-multimodule", mavenVersion) {
            addMavenToolchainsPlugin("17")
            modifyPomXml("lib/pom.xml") {
                val build = documentElement.getOrCreateChild("build")
                val plugins = build.getOrCreateChild("plugins")
                plugins.appendXmlFragment(
                    $$"""
                    <plugin>
                        <artifactId>kotlin-maven-plugin</artifactId>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <version>${kotlin.version}</version>
                        <configuration>
                            <jdkToolchain><version>21</version></jdkToolchain>
                        </configuration>
                    </plugin>
                """.trimIndent())
            }

            workDir.resolve("lib/src/main/kotlin/hogwarts/Jdk21Api.kt").toFile().writeText("""
                package hogwarts
                fun firstSpell(spells: java.util.SequencedCollection<Spell>): Spell = spells.first
            """.trimIndent())
            workDir.resolve("app/src/main/kotlin/hogwarts/Wizard.kt").toFile().writeText(
                $$"""
                package hogwarts
                class Wizard(val name: String) {
                    fun cast(spell: Spell): String = "${name} casts ${spell.incantation}!"
                    fun firstFromSet(): Spell = firstSpell(java.util.LinkedHashSet())
                }
            """.trimIndent())

            build(
                "compile",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_17, JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21, module = "test-toolchains-lib")
                assertToolchainAppliedToGoal("compile", jdk17, module = "test-toolchains-app")
                assertFileExists("lib/target/classes/hogwarts/Jdk21ApiKt.class")
                assertModuleGoalLogContains("test-toolchains-app", "compile", "Cannot access class 'java.util.SequencedCollection'")
                assertFileNotExists("app/target/classes/hogwarts/Wizard.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Toolchain is applied to kapt and test-kapt goals")
    fun testToolchainWithKapt(mavenVersion: TestVersions.Maven) {
        testProject("test-kapt-toolchains", mavenVersion) {
            addMavenToolchainsPlugin("21")

            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("compile", jdk21, module = "spellbook-processor")
                assertToolchainAppliedToGoal("kapt", jdk21, module = "hogwarts-app")
                assertToolchainAppliedToGoal("compile", jdk21, module = "hogwarts-app")
                assertToolchainAppliedToGoal("test-kapt", jdk21, module = "hogwarts-app")
                assertToolchainAppliedToGoal("test-compile", jdk21, module = "hogwarts-app")

                assertFileExists("app/target/generated-sources/kapt/compile/app/WizardSpellBook.java")
                assertFileExists("app/target/generated-sources/kaptKotlin/compile/WizardExtensions.kt")
                assertFileExists("app/target/generated-sources/kapt/test/app/WizardTestSpellBook.java")
                assertFileExists("app/target/generated-sources/kaptKotlin/test/WizardTestExtensions.kt")
                assertTestsPassed(3)
            }
        }
    }

    @MavenTest
    @Disabled("KT-79897: kapt loads annotation processor on JAVA_HOME JVM, not toolchain JDK")
    @DisplayName("kapt should run annotation processor on toolchain JDK")
    fun testToolchainAffectsKaptProcessorRuntime(mavenVersion: TestVersions.Maven) {
        testProject("test-kapt-toolchains", mavenVersion) {
            addMavenToolchainsPlugin("21")

            workDir.resolve("app/src/main/kotlin/app/Wizard.kt").toFile().writeText(
                """
                package app
                import example.RequiresJdk21Runtime
                @RequiresJdk21Runtime
                class Wizard {
                    fun name(): String = "Harry Potter"
                }
            """.trimIndent()
            )

            build(
                "compile",
                expectedToFail = false,
                // KT-71048: daemon retains JDK from first client — disable to avoid interference
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    useKotlinDaemon = false,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("kapt", jdk21, module = "hogwarts-app")
                assertFileExists("app/target/generated-sources/kapt/compile/app/WizardJdk21Generated.java")
            }
        }
    }

    @MavenTest
    @Disabled("KT-79897: kapt loads annotation processor on JAVA_HOME JVM, not toolchain JDK")
    @DisplayName("test-kapt should run annotation processor on toolchain JDK")
    fun testToolchainAffectsTestKaptProcessorRuntime(mavenVersion: TestVersions.Maven) {
        testProject("test-kapt-toolchains", mavenVersion) {
            addMavenToolchainsPlugin("21")

            workDir.resolve("app/src/test/kotlin/app/WizardTest.kt").toFile().writeText(
                """
                package app
                import example.RequiresJdk21Runtime
                @RequiresJdk21Runtime
                class WizardTest
            """.trimIndent()
            )

            build(
                "test-compile",
                expectedToFail = false,
                // KT-71048: daemon retains JDK from first client — disable to avoid interference
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_17,
                    useKotlinDaemon = false,
                    toolchains = listOf(JDK_21)
                )
            ) {
                assertToolchainAppliedToGoal("test-kapt", jdk21, module = "hogwarts-app")
                assertFileExists("app/target/generated-sources/kapt/test/app/WizardTestJdk21Generated.java")
            }
        }
    }

    @MavenTest
    @Disabled("KT-79897: kapt's javac on pre-modular JDK cannot resolve modular boot classpath")
    @DisplayName("kapt with JAVA_HOME JDK 8 should use toolchain JDK 17")
    fun testKaptPreModularJdkCannotResolveModularBootClasspath(mavenVersion: TestVersions.Maven) {
        testProject("test-kapt-toolchains", mavenVersion) {
            addMavenToolchainsPlugin("17")

            build(
                "compile",
                expectedToFail = false,
                // KT-71048: daemon retains JDK from first client — disable to avoid interference
                buildOptions = buildOptions.copy(
                    javaVersion = JDK_1_8,
                    useKotlinDaemon = false,
                    toolchains = listOf(JDK_17)
                )
            ) {
                assertToolchainAppliedToGoal("kapt", jdk17, module = "hogwarts-app")
                assertToolchainAppliedToGoal("compile", jdk17, module = "hogwarts-app")
            }
        }
    }

    private fun mainOutputPaths() = arrayOf(
        "target/classes/hogwarts/Wizard.class",
        "target/classes/hogwarts/Spell.class",
    )

    private fun testOutputPaths() = arrayOf(
        "target/test-classes/hogwarts/WizardTest.class",
        "target/test-classes/hogwarts/SpellTest.class",
    )

}
