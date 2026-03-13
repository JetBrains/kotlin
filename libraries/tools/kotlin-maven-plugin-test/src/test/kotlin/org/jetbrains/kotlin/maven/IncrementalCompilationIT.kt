/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.KotlinMavenTestBase
import org.jetbrains.kotlin.maven.test.MavenTest
import org.jetbrains.kotlin.maven.test.TestVersions
import org.jetbrains.kotlin.maven.test.assertBuildLogContains
import org.jetbrains.kotlin.maven.test.assertCompiledKotlin
import org.jetbrains.kotlin.maven.test.assertFilesExist

class IncrementalCompilationIT : KotlinMavenTestBase() {

    private fun kotlinSimpleOutputPaths() = arrayOf(
        "target/classes/test.properties",
        "target/classes/A.class",
        "target/classes/UseAKt.class",
        "target/classes/Dummy.class",
        "target/classes/JavaUtil.class",
        "target/classes/JavaAUser.class"
    )

    private fun withJavaOutputPaths() = arrayOf(
        "target/classes/SomeMain.class",
        "target/test-classes/SomeTests.class"
    )

    @MavenTest
    fun testSimpleCompile(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package", "-X") {
                assertFilesExist(*kotlinSimpleOutputPaths())
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/useA.kt", "src/main/kotlin/Dummy.kt")
            }
        }
    }

    @MavenTest
    fun testNoChanges(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package")
            build("package", "-X") {
                assertFilesExist(*kotlinSimpleOutputPaths())
                assertCompiledKotlin()
            }
        }
    }

    @MavenTest
    fun testCompileError(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package")

            val aKt = workDir.resolve("src/main/kotlin/A.kt")
            val original = "class A"
            val replacement = "private class A"
            aKt.replaceFirstInFile(original, replacement)

            build("package", expectedToFail = true) {
                assertBuildLogContains("Cannot access 'class A : Any': it is private in file")
            }

            aKt.replaceFirstInFile(replacement, original)
            build("package", "-X") {
                assertFilesExist(*kotlinSimpleOutputPaths())
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    fun testFunctionVisibilityChanged(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package")

            val aKt = workDir.resolve("src/main/kotlin/A.kt")
            aKt.replaceFirstInFile("fun foo", "internal fun foo")

            build("package", "-X") {
                assertFilesExist(*kotlinSimpleOutputPaths())
                assertCompiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    fun testJavaChanged(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package")

            val javaUtil = workDir.resolve("src/main/java/JavaUtil.java")
            javaUtil.replaceFirstInFile("CONST = 0", "CONST = 1")

            build("package", "-X") {
                assertFilesExist(*kotlinSimpleOutputPaths())
                assertCompiledKotlin("src/main/kotlin/A.kt")
            }
        }
    }

    @MavenTest
    fun secondRunWithTests(mavenVersion: TestVersions.Maven) {
        testProject("kotlinWithTests", mavenVersion) {
            build("package")
            build("package", "-X") {
                assertFilesExist(*withJavaOutputPaths())
                assertCompiledKotlin()
            }
        }
    }

    @MavenTest
    fun removeUsedClass(mavenVersion: TestVersions.Maven) {
        testProject("kotlinSimple", mavenVersion) {
            build("package")

            workDir.resolve("src/main/kotlin/A.kt").deleteFile()

            build("package", "-X", expectedToFail = true) {
                assertBuildLogContains("Unresolved reference 'A'")
                assertCompiledKotlin("src/main/kotlin/useA.kt")
            }
        }
    }

    @MavenTest
    fun renameUsedClassInTest(mavenVersion: TestVersions.Maven) {
        testProject("kotlinWithTests", mavenVersion) {
            build("package")

            workDir.resolve("src/test/kotlin/BaseTests.kt").replaceFirstInFile("BaseTests", "MyBaseTests")

            build("package", "-X", expectedToFail = true) {
                assertBuildLogContains("Unresolved reference 'BaseTests'")
                assertCompiledKotlin("src/test/kotlin/BaseTests.kt", "src/test/kotlin/SomeTests.kt")
            }
        }
    }
}
