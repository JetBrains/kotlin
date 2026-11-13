/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.w3c.dom.Element
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.reader
import kotlin.io.path.readText
import kotlin.io.path.writeText

@DisplayName("JDK compatibility")
class JdkCompatibilityIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("Compilation fails on JDK 17 when code uses JDK 21+ API")
    fun testCustomJdkFailure(mavenVersion: TestVersions.Maven) {
        testProject("test-customJdk", mavenVersion) {
            build(
                "package",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_17,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_17))
                )
            ) {
                assertCompilationFailed()
                assertBuildLogContains(
                    "[INFO] Overriding JDK home path with",
                    "Unresolved reference 'SequencedCollection'"
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Compilation succeeds on JDK 21 with JDK 21+ API")
    fun testCustomJdkSuccess(mavenVersion: TestVersions.Maven) {
        testProject("test-customJdk", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_21,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_21))
                )
            ) {
                assertJarExistsAndNotEmpty("target/test-customJdk-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Compilation with JDK 17 internal classpath dependencies succeeds")
    fun testJavaClasspath(mavenVersion: TestVersions.Maven) {
        testProject("java8/test-classpath", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_17,
                    extraMavenProperties = mapOf(
                        "kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_17),
                        "kotlin.compiler.jdkHome" to context.getJavaHomeString(TestVersions.Java.JDK_1_8),
                    )
                )
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-classpath-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Runtime image via jlink includes all required Kotlin and app modules")
    fun testJava9JlinkModularArtifacts(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-jlink-modular-artifacts", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("jlinked/target/maven-jlink/release")

                val releaseFile = Path(basedir).resolve("jlinked/target/maven-jlink/release")
                val props = java.util.Properties()
                releaseFile.reader().use { props.load(it) }

                var modules = props.getProperty("MODULES")
                    ?: fail("MODULES property missing from release file")

                if (modules.startsWith("\"") && modules.endsWith("\"")) {
                    modules = modules.substring(1, modules.length - 1)
                }

                val moduleSet = modules.split(" ").toSet()
                for (module in listOf(
                    "java.base",
                    "kotlin.stdlib",
                    "kotlin.stdlib.jdk7",
                    "kotlin.stdlib.jdk8",
                    "org.test.modularApp"
                )) {
                    assertTrue(module in moduleSet) {
                        "Expected to find $module in image modules: $modules"
                    }
                }
            }
        }
    }

    @MavenTest
    @DisplayName("Mixed Kotlin/Java source roots in Java 9 module compile correctly")
    fun testJava9SourceRootsRegisteredSeveralTimes(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertJarExistsAndNotEmpty("target/test-sourceRootsRegisteredSeveralTimes-1.0-SNAPSHOT.jar")
                assertFileExists("target/classes/foo/bar/Foo.class")
                assertFileExists("target/classes/koo/bar/Koo.class")
                assertFileExists("target/classes/module-info.class")
            }
        }
    }

    @MavenTest
    @DisplayName("KT-88152: Relative source roots in Java 9 module compile correctly")
    fun testJava9RelativeSourceRoots(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            relativizeSourceDirs()

            build(
                "package",
                expectedToFail = false
            ) {
                assertJarExistsAndNotEmpty("target/test-sourceRootsRegisteredSeveralTimes-1.0-SNAPSHOT.jar")
                assertFileExists("target/classes/foo/bar/Foo.class")
                assertFileExists("target/classes/koo/bar/Koo.class")
                assertFileExists("target/classes/module-info.class")
            }
        }
    }

    @MavenTest
    @DisplayName("KT-88152: Relative source roots with a leading .. in Java 9 module compile correctly")
    fun testJava9RelativeSourceRootsWithLeadingParent(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            relativizeSourceDirs("../${workDir.fileName}/")

            build(
                "package",
                expectedToFail = false
            ) {
                assertJarExistsAndNotEmpty("target/test-sourceRootsRegisteredSeveralTimes-1.0-SNAPSHOT.jar")
                assertFileExists("target/classes/foo/bar/Foo.class")
                assertFileExists("target/classes/koo/bar/Koo.class")
                assertFileExists("target/classes/module-info.class")
            }
        }
    }

    @MavenTest
    @DisplayName("KT-88152: Kotlin file in a relative Java source root isn't reported as a duplicate")
    fun testJava9RelativeSourceRootsWithKotlinInJavaRoot(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            relativizeSourceDirs()
            workDir.resolve("src/main/java/foo/bar/InJavaRoot.kt").writeText("package foo.bar\n\nclass InJavaRoot\n")

            // The warning is printed only without incremental compilation, which is turned on in this fixture
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(extraMavenProperties = mapOf("kotlin.compiler.incremental" to "false"))
            ) {
                assertBuildLogDoesNotContain("Duplicate source root")
                assertJarExistsAndNotEmpty("target/test-sourceRootsRegisteredSeveralTimes-1.0-SNAPSHOT.jar")
                assertFileExists("target/classes/foo/bar/InJavaRoot.class")
                assertFileExists("target/classes/module-info.class")
            }
        }
    }

    @MavenTest
    @DisplayName("KT-88152: Nested source roots in Java 9 module compile correctly")
    fun testJava9NestedSourceRoots(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            // src/main contains src/main/java, which is also the compile source root
            modifySourceDirs("compile") { listOf("src/main") }

            build(
                "package",
                expectedToFail = false
            ) {
                assertJarExistsAndNotEmpty("target/test-sourceRootsRegisteredSeveralTimes-1.0-SNAPSHOT.jar")
                assertFileExists("target/classes/foo/bar/Foo.class")
                assertFileExists("target/classes/koo/bar/Koo.class")
                assertFileExists("target/classes/module-info.class")
            }
        }
    }

    @MavenTest
    @DisplayName("KT-88152: Test source root listed twice in Java 9 module compiles correctly")
    fun testJava9TestSourceRootListedTwice(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            // javac would need a test module that requires the main one, and only the Kotlin step is under test
            modifySourceDirs("test-compile") { listOf("src/test/kotlin", "src/test/java", "${BASEDIR}src/test/java") }
            modifyPomXml {
                findOrCreateMavenCompilerPlugin().findExecution("java-test-compile").setChildText("phase", "none")
            }
            workDir.resolve("src/test/java/module-info.java").also { it.parent.createDirectories() }
                .writeText("module foo.bar.test {\n  requires kotlin.stdlib;\n}\n")
            workDir.resolve("src/test/kotlin/koo/bar/KooTest.kt").also { it.parent.createDirectories() }
                .writeText("package koo.bar\n\nclass KooTest\n")

            build(
                "test-compile",
                expectedToFail = false
            ) {
                assertFileExists("target/test-classes/koo/bar/KooTest.class")
            }
        }
    }

    @MavenTest
    @DisabledOnOs(OS.WINDOWS)
    @DisplayName("KT-88152: A class is still generated for a symlinked Kotlin file")
    fun testJava9SymlinkedKotlinFile(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            val kotlinDir = workDir.resolve("src/main/kotlin/koo/bar")
            kotlinDir.resolve("A.kt").writeText("package koo.bar\n\nprivate fun helper() = 1\n")
            Files.createSymbolicLink(kotlinDir.resolve("B.kt"), Path("A.kt"))

            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("target/classes/koo/bar/AKt.class")
                assertFileExists("target/classes/koo/bar/BKt.class")
                assertFileExists("target/classes/module-info.class")
            }
        }
    }

    @MavenTest
    @DisabledOnOs(OS.WINDOWS)
    @DisplayName("KT-88152: Symlinked module-info.java with another target name is passed to the Kotlin compiler")
    fun testJava9SymlinkedModuleInfoWithOtherName(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            val moduleInfo = workDir.resolve("src/main/java/module-info.java")
            val descriptors = workDir.resolve("descriptors").createDirectories()
            descriptors.resolve("Descriptor.java").writeText(moduleInfo.readText())
            moduleInfo.deleteExisting()
            Files.createSymbolicLink(moduleInfo, Path("../../../descriptors/Descriptor.java"))
            // The directory with the link target is walked first
            modifySourceDirs("compile") { listOf("${BASEDIR}descriptors") + it }

            // javac rejects a module-info.java that links to a file with another name, so only the Kotlin step runs
            build(
                "compile",
                expectedToFail = false,
                buildOptions = buildOptions.copy(extraMavenProperties = mapOf("maven.main.skip" to "true"))
            ) {
                assertFileExists("target/classes/koo/bar/Koo.class")
            }
        }
    }

    @MavenTest
    @DisabledOnOs(OS.WINDOWS)
    @DisplayName("KT-88152: Symlinked module-info.java in another source root is passed to the Kotlin compiler once")
    fun testJava9SymlinkedModuleInfoInAnotherRoot(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            val alias = workDir.resolve("src/alias").createDirectories()
            Files.createSymbolicLink(alias.resolve("module-info.java"), Path("../main/java/module-info.java"))
            modifySourceDirs("compile") { it + "${BASEDIR}src/alias" }

            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("target/classes/koo/bar/Koo.class")
                assertFileExists("target/classes/module-info.class")
            }
        }
    }

    @MavenTest
    @DisplayName("KT-88152: Relative source roots with extensions enabled compile a Java 9 module")
    fun testJava9RelativeSourceRootsWithExtensions(mavenVersion: TestVersions.Maven) {
        val buildOptions = if (isWindowsHost) {
            buildOptions.withoutKotlinDaemon("KT-90859: the extensions bind kapt, and its classloaders are not cleaned up after the execution in Maven")
        } else {
            buildOptions
        }
        testProject("java9/test-relativeSourceRootsWithExtensions", mavenVersion, buildOptions) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertJarExistsAndNotEmpty("target/test-relativeSourceRootsWithExtensions-1.0-SNAPSHOT.jar")
                assertFileExists("target/classes/org/example/app/Main.class")
                assertFileExists("target/classes/org/example/k/Greeter.class")
                assertFileExists("target/classes/module-info.class")
            }
        }
    }

    @MavenTest
    @DisplayName("KT-55709: Incremental compilation with many files does not fail on JDK 17")
    fun testJava17FileSnapshotMapOverflow(mavenVersion: TestVersions.Maven) {
        testProject("java17/test-fileSnapshotMap-overflow", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(javaVersion = TestVersions.Java.JDK_17)
            ) {
                assertJarExistsAndNotEmpty("app/target/test-fileSnapshotMap-overflow-app-1.0-SNAPSHOT.jar")
            }
        }
    }

    private fun MavenTestProject.relativizeSourceDirs(prefix: String = "") {
        for (executionId in listOf("compile", "test-compile")) {
            modifySourceDirs(executionId) { dirs -> dirs.map { prefix + it.removePrefix(BASEDIR) } }
        }
    }

    /** Fails when the execution has no `<sourceDirs>` or when [transform] returns the directories unchanged. */
    private fun MavenTestProject.modifySourceDirs(executionId: String, transform: (List<String>) -> List<String>) {
        modifyPomXml {
            val sourceDirs = findKotlinPlugin().findExecution(executionId).getElementsByTagName("sourceDirs").item(0) as? Element
                ?: error("Execution '$executionId' has no <sourceDirs>")
            val before = (0 until sourceDirs.childNodes.length).map { sourceDirs.childNodes.item(it) }
                .filterIsInstance<Element>().map { it.textContent }
            val after = transform(before)
            check(after != before) { "<sourceDirs> of execution '$executionId' is unchanged: $before" }
            while (sourceDirs.hasChildNodes()) sourceDirs.removeChild(sourceDirs.firstChild)
            after.forEach { sourceDirs.appendXmlFragment("<sourceDir>$it</sourceDir>") }
        }
    }

    private fun Element.setChildText(tagName: String, text: String) {
        val child = getElementsByTagName(tagName).item(0) ?: error("No <$tagName> in <$nodeName>")
        child.textContent = text
    }

    private companion object {
        const val BASEDIR = $$"${project.basedir}/"
    }
}
