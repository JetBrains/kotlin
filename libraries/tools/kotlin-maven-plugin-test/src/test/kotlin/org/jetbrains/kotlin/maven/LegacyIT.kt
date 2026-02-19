/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.plugin.test

import org.jetbrains.kotlin.maven.test.KotlinMavenTestBase
import org.jetbrains.kotlin.maven.test.MavenBuildOptions
import org.jetbrains.kotlin.maven.test.MavenTest
import org.jetbrains.kotlin.maven.test.MavenVersions
import org.jetbrains.kotlin.maven.test.TestVersions
import org.jetbrains.kotlin.maven.test.assertBuildLogContains
import org.jetbrains.kotlin.maven.test.isWindowsHost
import org.jetbrains.kotlin.maven.test.loadMavenInvokerPropertiesOrNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.io.path.absolutePathString

@Execution(ExecutionMode.CONCURRENT)
// TODO: KT-83109 Remove beanshell and groovy verification in kotlin-maven-plugin-test
class LegacyIT : KotlinMavenTestBase() {
    private fun verifyWithLegacyBsh(
        projectName: String,
        mavenVersion: TestVersions.Maven,
        withDebug: Boolean = true,
        buildOptions: MavenBuildOptions = this.buildOptions,
        disableKotlinDaemonOnWindows: Boolean = false,
    ) {
        val buildOptions = if (disableKotlinDaemonOnWindows && isWindowsHost) {
            buildOptions.copy(useKotlinDaemon = false)
        } else buildOptions

        testProject(projectName, mavenVersion, buildOptions) {
            val args = mutableListOf<String>()

            // enable debug output by default, as some verification scripts are checking for dependencies hits
            // that are not printed in INFO mode when re-used from local repo.
            if (withDebug) args += listOf("-X")

            val mavenInvokerProperties = loadMavenInvokerPropertiesOrNull(workDir.resolve("invoker.properties").toFile())
            val invokerGoals = mavenInvokerProperties?.goals
            if (invokerGoals != null) args += invokerGoals else args += "package"

            build(
                *args.toTypedArray(),
                expectedToFail = mavenInvokerProperties?.failureExpected == true
            )
            runVerifyScript()
        }
    }

    @MavenTest
    fun `test-helloworld`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-helloworld", mavenVersion)

    @MavenTest
    fun `test-helloworld-kts`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-helloworld-kts", mavenVersion)

    @MavenTest
    fun `test-accessToInternal`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-accessToInternal", mavenVersion)

    @MavenTest
    fun `test-allopen-simple`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-allopen-simple", mavenVersion)

    @MavenTest
    fun `test-allopen-spring`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-allopen-spring", mavenVersion)

    @MavenTest
    fun `test-apiVersion`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-apiVersion", mavenVersion)

    @MavenTest
    fun `test-bom`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-bom", mavenVersion)

    @MavenTest
    fun `test-customJdk-1_8`(mavenVersion: TestVersions.Maven) {
        testProject("test-customJdk", mavenVersion) {
            build(
                "package",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.javaHomeProvider(TestVersions.Java.JDK_1_8).absolutePathString())
                )
            ) {
                assertBuildLogContains(
                    "[INFO] BUILD FAILURE",
                    "[INFO] Overriding JDK home path with",
                    "Unresolved reference 'StackWalker'"
                )
            }
        }
    }

    @MavenTest
    fun `test-customJdk-17`(mavenVersion: TestVersions.Maven) {
        testProject("test-customJdk", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_17,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.javaHomeProvider(TestVersions.Java.JDK_17).absolutePathString())
                )
            ) {
                assertBuildLogContains("[INFO] BUILD SUCCESS")
            }
        }
    }

    @MavenTest
    fun `test-empty-argument`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-empty-argument", mavenVersion)

    @MavenTest
    fun `test-enable-extensions`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-enable-extensions", mavenVersion)

    @MavenTest
    fun `test-executeKotlinScriptBuildAccess`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-executeKotlinScriptBuildAccess", mavenVersion)

    @MavenTest
    fun `test-executeKotlinScriptCompileError`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-executeKotlinScriptCompileError", mavenVersion)

    @MavenTest
    fun `test-executeKotlinScriptFile`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-executeKotlinScriptFile", mavenVersion)

    @MavenTest
    fun `test-executeKotlinScriptInline`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-executeKotlinScriptInline", mavenVersion)

    @MavenTest
    fun `test-executeKotlinScriptScriptException`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-executeKotlinScriptScriptException", mavenVersion)

    @MavenTest
    fun `test-executeKotlinScriptWithDependencies`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-executeKotlinScriptWithDependencies", mavenVersion)

    @MavenTest
    fun `test-executeKotlinScriptWithTemplate`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-executeKotlinScriptWithTemplate", mavenVersion)

    @MavenTest
    fun `test-extraArguments`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-extraArguments", mavenVersion)

    @MavenTest
    fun `test-jvmTarget`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-jvmTarget", mavenVersion)

    @MavenTest
    fun `test-kapt-annotationProcessorPaths-without-version`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh(
        "test-kapt-annotationProcessorPaths-without-version",
        mavenVersion,
        disableKotlinDaemonOnWindows = true
    )

    @MavenTest
    fun `test-kapt-generateKotlinCode`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh(
        "test-kapt-generateKotlinCode",
        mavenVersion,
        disableKotlinDaemonOnWindows = true
    )

    @MavenTest
    fun `test-kotlin-dataframe`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-kotlin-dataframe", mavenVersion)

    @MavenTest
    fun `test-kotlin-version-in-manifest`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-kotlin-version-in-manifest", mavenVersion)

    @MavenTest
    fun `test-languageVersion`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-languageVersion", mavenVersion)

    @MavenTest
    fun `test-lombok-simple`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-lombok-simple", mavenVersion, buildOptions = buildOptions.copy(javaVersion = TestVersions.Java.JDK_17))

    @MavenTest
    fun `test-lombok-with-kapt`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-lombok-with-kapt", mavenVersion)

    @MavenTest
    fun `test-moduleName`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-moduleName", mavenVersion)

    @MavenTest
    fun `test-moduleNameDefault`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-moduleNameDefault", mavenVersion)

    @MavenTest
    fun `test-multimodule`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-multimodule", mavenVersion)

    @MavenTest
    fun `test-multimodule-in-process`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-multimodule-in-process", mavenVersion)

    @MavenTest
    fun `test-multimodule-srcdir`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-multimodule-srcdir", mavenVersion)

    @MavenTest
    fun `test-multimodule-srcdir-absolute`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-multimodule-srcdir-absolute", mavenVersion)

    @MavenTest
    fun `test-noarg-jpa`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-noarg-jpa", mavenVersion)

    @MavenTest
    fun `test-noarg-simple`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-noarg-simple", mavenVersion)

    @MavenTest
    @MavenVersions(max = TestVersions.Maven.MAVEN_3_6_3) // since maven 3.7+ they've introduced custom "goalPrefix" that breaks expected build log
    fun `test-plugins`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-plugins", mavenVersion, withDebug = false)

    @MavenTest
    fun `test-power-assert`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-power-assert", mavenVersion)

    @MavenTest
    fun `test-reflection`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-reflection", mavenVersion)

    @MavenTest
    fun `test-respect-compile-source-root`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-respect-compile-source-root", mavenVersion)

    @MavenTest
    fun `test-sam-with-receiver-simple`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-sam-with-receiver-simple", mavenVersion)

    @MavenTest
    fun `test-suppressWarnings`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-suppressWarnings", mavenVersion)

    @MavenTest
    fun `test-smart-defaults-custom-source-dirs`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-smart-defaults-custom-source-dirs", mavenVersion)

    @MavenTest
    fun `test-smart-defaults-disabled`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-smart-defaults-disabled", mavenVersion)

    @MavenTest
    fun `test-smart-defaults-disabled-via-property`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-smart-defaults-disabled-via-property", mavenVersion)

    @MavenTest
    fun `test-smart-defaults-enabled`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-smart-defaults-enabled", mavenVersion)

    @MavenTest
    fun `test-smart-defaults-source-roots`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-smart-defaults-source-roots", mavenVersion)

    @MavenTest
    fun `test-smart-defaults-stdlib`(mavenVersion: TestVersions.Maven) = verifyWithLegacyBsh("test-smart-defaults-stdlib", mavenVersion)

    @MavenTest
    fun `test-smart-defaults-stdlib-exists`(mavenVersion: TestVersions.Maven) =
        verifyWithLegacyBsh("test-smart-defaults-stdlib-exists", mavenVersion)

}