/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.plugin.test

import org.jetbrains.kotlin.maven.test.isWindowsHost
import org.jetbrains.kotlin.maven.test.loadMavenInvokerPropertiesOrNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
// TODO: KT-83109 Remove beanshell and groovy verification in kotlin-maven-plugin-test
class LegacyIT : KotlinMavenTestBase() {
    private fun verifyWithLegacyBsh(
        projectName: String,
        withDebug: Boolean = true,
        buildOptions: MavenBuildOptions = this.buildOptions,
        disableKotlinDaemonOnWindows: Boolean = false,
    ) {
        val buildOptions = if (disableKotlinDaemonOnWindows && isWindowsHost) {
            buildOptions.copy(useKotlinDaemon = false)
        } else buildOptions

        testProject(projectName, "default", buildOptions) {
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

    @Test
    fun `test-helloworld`() = verifyWithLegacyBsh("test-helloworld",)

    @Test
    fun `test-helloworld-kts`() = verifyWithLegacyBsh("test-helloworld-kts",)

    @Test
    fun `test-accessToInternal`() = verifyWithLegacyBsh("test-accessToInternal",)

    @Test
    fun `test-allopen-simple`() = verifyWithLegacyBsh("test-allopen-simple",)

    @Test
    fun `test-allopen-spring`() = verifyWithLegacyBsh("test-allopen-spring",)

    @Test
    fun `test-apiVersion`() = verifyWithLegacyBsh("test-apiVersion",)

    @Test
    fun `test-bom`() = verifyWithLegacyBsh("test-bom",)

    @Test
    @Disabled // FIXME: KT-83111 Add JavaVersion argument resolver for kotlin-maven-plugin-test
    fun `test-customJdk`() = verifyWithLegacyBsh("test-customJdk",)

    @Test
    fun `test-empty-argument`() = verifyWithLegacyBsh("test-empty-argument",)

    @Test
    fun `test-enable-extensions`() = verifyWithLegacyBsh("test-enable-extensions",)

    @Test
    fun `test-executeKotlinScriptBuildAccess`() = verifyWithLegacyBsh("test-executeKotlinScriptBuildAccess",)

    @Test
    fun `test-executeKotlinScriptCompileError`() = verifyWithLegacyBsh("test-executeKotlinScriptCompileError",)

    @Test
    fun `test-executeKotlinScriptFile`() = verifyWithLegacyBsh("test-executeKotlinScriptFile",)

    @Test
    fun `test-executeKotlinScriptInline`() = verifyWithLegacyBsh("test-executeKotlinScriptInline",)

    @Test
    fun `test-executeKotlinScriptScriptException`() = verifyWithLegacyBsh("test-executeKotlinScriptScriptException",)

    @Test
    fun `test-executeKotlinScriptWithDependencies`() = verifyWithLegacyBsh("test-executeKotlinScriptWithDependencies",)

    @Test
    fun `test-executeKotlinScriptWithTemplate`() = verifyWithLegacyBsh("test-executeKotlinScriptWithTemplate",)

    @Test
    fun `test-extraArguments`() = verifyWithLegacyBsh("test-extraArguments",)

    @Test
    fun `test-jvmTarget`() = verifyWithLegacyBsh("test-jvmTarget",)

    @Test
    fun `test-kapt-annotationProcessorPaths-without-version`() = verifyWithLegacyBsh(
        "test-kapt-annotationProcessorPaths-without-version",
        disableKotlinDaemonOnWindows = true
    )

    @Test
    fun `test-kapt-generateKotlinCode`() = verifyWithLegacyBsh(
        "test-kapt-generateKotlinCode",
        disableKotlinDaemonOnWindows = true
    )

    @Test
    fun `test-kotlin-dataframe`() = verifyWithLegacyBsh("test-kotlin-dataframe",)

    @Test
    fun `test-kotlin-version-in-manifest`() = verifyWithLegacyBsh("test-kotlin-version-in-manifest",)

    @Test
    fun `test-languageVersion`() = verifyWithLegacyBsh("test-languageVersion",)

    @Disabled // requires JDK 17
    @Test
    fun `test-lombok-simple`() = verifyWithLegacyBsh("test-lombok-simple",)

    @Test
    fun `test-lombok-with-kapt`() = verifyWithLegacyBsh("test-lombok-with-kapt",)

    @Test
    fun `test-moduleName`() = verifyWithLegacyBsh("test-moduleName",)

    @Test
    fun `test-moduleNameDefault`() = verifyWithLegacyBsh("test-moduleNameDefault",)

    @Test
    fun `test-multimodule`() = verifyWithLegacyBsh("test-multimodule",)

    @Test
    fun `test-multimodule-in-process`() = verifyWithLegacyBsh("test-multimodule-in-process",)

    @Test
    fun `test-multimodule-srcdir`() = verifyWithLegacyBsh("test-multimodule-srcdir",)

    @Test
    fun `test-multimodule-srcdir-absolute`() = verifyWithLegacyBsh("test-multimodule-srcdir-absolute",)

    @Test
    fun `test-noarg-jpa`() = verifyWithLegacyBsh("test-noarg-jpa",)

    @Test
    fun `test-noarg-simple`() = verifyWithLegacyBsh("test-noarg-simple",)

    @Test
    fun `test-plugins`() = verifyWithLegacyBsh("test-plugins", withDebug = false,)

    @Test
    fun `test-power-assert`() = verifyWithLegacyBsh("test-power-assert",)

    @Test
    fun `test-reflection`() = verifyWithLegacyBsh("test-reflection",)

    @Test
    fun `test-respect-compile-source-root`() = verifyWithLegacyBsh("test-respect-compile-source-root",)

    @Test
    fun `test-sam-with-receiver-simple`() = verifyWithLegacyBsh("test-sam-with-receiver-simple",)

    @Test
    fun `test-suppressWarnings`() = verifyWithLegacyBsh("test-suppressWarnings",)

}