/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.KOTLIN_STDLIB_JDK7_MODULE_NAME
import org.jetbrains.kotlin.gradle.internal.KOTLIN_STDLIB_JS_MODULE_NAME
import org.jetbrains.kotlin.gradle.internal.KOTLIN_STDLIB_MODULE_NAME
import org.jetbrains.kotlin.gradle.internal.KOTLIN_STDLIB_WASM_JS_MODULE_NAME
import org.jetbrains.kotlin.gradle.internal.KOTLIN_STDLIB_WASM_WASI_MODULE_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.*
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmTargetDsl
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import kotlin.test.Test

private val KotlinToolingVersion.advanceMajor: KotlinToolingVersion
    get() = KotlinToolingVersion(major + 1, minor, patch, classifier)

class StdlibVersionAlignmentTest : SourceSetDependenciesResolution() {
    private val jvmProject by lazy {
        buildProjectWithJvm {
            configureRepositoriesForTests()
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    private val kmpProject
        get() = buildProjectWithMPP {
            configureRepositoriesForTests()
            kotlin {
                jvm()
                linuxX64()
                js { browser() }
                wasmJs { browser() }
                wasmWasi { nodejs() }
            }
        }

    private fun MavenRepositoryMockDsl.stdlib(version: String): MavenRepositoryMock.Module {
        val stdlibJs = module("$KOTLIN_MODULE_GROUP:$KOTLIN_STDLIB_JS_MODULE_NAME:$version")
        val stdlibWasmJs = module("$KOTLIN_MODULE_GROUP:$KOTLIN_STDLIB_WASM_JS_MODULE_NAME:$version")
        val stdlibWasmWasi = module("$KOTLIN_MODULE_GROUP:$KOTLIN_STDLIB_WASM_WASI_MODULE_NAME:$version")
        val annotations = module("org.jetbrains:annotations:13.0")
        return module("$KOTLIN_MODULE_GROUP:$KOTLIN_STDLIB_MODULE_NAME:$version") {
            variantDependencies = {
                when (platformType) {
                    jvm -> setOf(annotations)
                    js -> setOf(stdlibJs)
                    wasm -> {
                        this as KotlinWasmTargetDsl
                        when (wasmTargetType!!) {
                            KotlinWasmTargetType.JS -> setOf(stdlibWasmJs)
                            KotlinWasmTargetType.WASI -> setOf(stdlibWasmWasi)
                        }
                    }
                    else -> emptySet()
                }
            }
        }
    }

    private fun MavenRepositoryMockDsl.stdlibJdk7(version: String): MavenRepositoryMock.Module =
        module("$KOTLIN_MODULE_GROUP:$KOTLIN_STDLIB_JDK7_MODULE_NAME:$version").also {
            it dependsOn module("$KOTLIN_MODULE_GROUP:$KOTLIN_STDLIB_MODULE_NAME:$version")
        }

    private fun MavenRepositoryMockDsl.stdlibJdk8(version: String): MavenRepositoryMock.Module =
        module("$KOTLIN_MODULE_GROUP:$KOTLIN_STDLIB_JDK8_MODULE_NAME:$version").also {
            it dependsOn module("$KOTLIN_MODULE_GROUP:$KOTLIN_STDLIB_MODULE_NAME:$version")
        }

    private val currentKotlinVersion get() = jvmProject.kotlinToolingVersion.toString()
    private val oldKotlinVersion = "1.9.0"
    private val futureKotlinVersion get() = jvmProject.kotlinToolingVersion.advanceMajor.toString()

    private fun String.sanitizer(): String = this
        .replace(currentKotlinVersion, "<CURRENT_KOTLIN_VERSION>")
        .replace(futureKotlinVersion, "<FUTURE_KOTLIN_VERSION>")
        .replace("""org.jetbrains:annotations:(\S+)""".toRegex(), "org.jetbrains:annotations")

    @Test
    fun jvmHigherVersion() =
        assertSourceSetDependenciesResolution(
            "jvmHigherVersion.txt",
            sanitize = { sanitizer() },
            withProject = jvmProject
        ) {
            mavenRepositoryMock {
                module("test:lib:1.0") dependsOn stdlib(futureKotlinVersion)
            }
            api("main", "test:lib:1.0")
        }

    @Test
    fun jvmLowerVersion() =
        assertSourceSetDependenciesResolution(
            "jvmLowerVersion.txt",
            sanitize = { sanitizer() },
            withProject = jvmProject
        ) {
            mavenRepositoryMock {
                module("test:lib:1.0") dependsOn stdlib(oldKotlinVersion)
            }
            api("main", "test:lib:1.0")
        }

    @Test
    fun jvmHigherAndLowerVersion() =
        assertSourceSetDependenciesResolution(
            "jvmHigherAndLowerVersion.txt",
            sanitize = { sanitizer() },
            withProject = jvmProject
        ) {
            mavenRepositoryMock {
                module("test:lib:1.0") dependsOn stdlib(futureKotlinVersion)
                module("test:lib:1.0") dependsOn module("test:lib2:1.0") dependsOn stdlib(oldKotlinVersion)
            }
            api("main", "test:lib:1.0")
        }

    @Test
    fun jvmMainHigherVersion() = assertSourceSetDependenciesResolution(
        "jvmMainHigherVersion.txt",
        sanitize = { sanitizer() },
        withProject = jvmProject
    ) {
        mavenRepositoryMock {
            module("test:lib:1.0") dependsOn stdlib(currentKotlinVersion)
            module("test:lib:2.0") dependsOn stdlib(futureKotlinVersion)
        }
        api("main", "test:lib:2.0")
        api("test", "test:lib:1.0")
    }

    @Test
    fun jvmTestHigherVersion() = assertSourceSetDependenciesResolution(
        "jvmTestHigherVersion.txt",
        sanitize = { sanitizer() },
        withProject = jvmProject
    ) {
        mavenRepositoryMock {
            module("test:lib:1.0") dependsOn stdlib(currentKotlinVersion)
            module("test:lib:2.0") dependsOn stdlib(futureKotlinVersion)
        }
        api("main", "test:lib:1.0")
        api("test", "test:lib:2.0")
    }

    @Test
    fun jvmNoStdlib() = assertSourceSetDependenciesResolution(
        "jvmNoStdLib.txt",
        withProject = buildProjectWithJvm(preApplyCode = { enableDefaultStdlibDependency(false) })
    ) {
        mavenRepositoryMock {
            module("test:javaLib:1.0")
        }

        api("main", "test:javaLib:1.0")
    }

    @Test
    fun jvmNoDefaultStdlibButFromTransitiveDependencies() = assertSourceSetDependenciesResolution(
        "jvmNoDefaultStdlibButFromTransitiveDependencies.txt",
        sanitize = { sanitizer() },
        withProject = buildProjectWithJvm(preApplyCode = { enableDefaultStdlibDependency(false) })
    ) {
        it.configureRepositoriesForTests()
        mavenRepositoryMock {
            module("test:lib:1.0") dependsOn stdlib(oldKotlinVersion)
            module("test:lib2:1.0") dependsOn stdlib(futureKotlinVersion)
        }

        api("main", "test:lib:1.0")
        api("test", "test:lib2:1.0")
    }

    /**
     * TODO: Fix in KT-58999, check jvmJdkVariantsAlignment.txt for the reference
     */
    @Test
    fun jvmJdkVariantsAlignment() = assertSourceSetDependenciesResolution(
        "jvmJdkVariantsAlignment.txt",
        sanitize = { sanitizer() },
        withProject = jvmProject
    ) {
        mavenRepositoryMock {
            module("test:lib:1.0") dependsOn stdlibJdk7(oldKotlinVersion)
            module("test:lib2:1.0") dependsOn stdlibJdk8(futureKotlinVersion)
        }
        api("main", "test:lib:1.0")
        api("test", "test:lib2:1.0")
    }

    @Test
    fun kmpHigherVersion() =
        assertSourceSetDependenciesResolution(
            "kmpHigherVersion.txt",
            sanitize = { sanitizer() },
            withProject = kmpProject
        ) {
            mavenRepositoryMock {
                module("test:lib:1.0") dependsOn stdlib(futureKotlinVersion)
            }
            api("commonMain", "test:lib:1.0")
        }

    @Test
    fun kmpLowerVersion() =
        assertSourceSetDependenciesResolution(
            "kmpLowerVersion.txt",
            sanitize = { sanitizer() },
            withProject = kmpProject
        ) {
            mavenRepositoryMock {
                module("test:lib:1.0") dependsOn stdlib(oldKotlinVersion)
            }
            api("commonMain", "test:lib:1.0")
        }

    @Test
    fun kmpHigherAndLowerVersion() =
        assertSourceSetDependenciesResolution(
            "kmpHigherAndLowerVersion.txt",
            sanitize = { sanitizer() },
            withProject = kmpProject
        ) {
            mavenRepositoryMock {
                module("test:lib:1.0") dependsOn stdlib(futureKotlinVersion)
                module("test:lib:1.0") dependsOn module("test:lib2:1.0") dependsOn stdlib(oldKotlinVersion)
            }
            api("commonMain", "test:lib:1.0")
        }

    @Test
    fun kmpCommonMainHigherVersion() = assertSourceSetDependenciesResolution(
        "kmpCommonMainHigherVersion.txt",
        sanitize = { sanitizer() },
        withProject = kmpProject
    ) {
        mavenRepositoryMock {
            module("test:lib:1.0") dependsOn stdlib(currentKotlinVersion)
            module("test:lib:2.0") dependsOn stdlib(futureKotlinVersion)
        }
        api("commonMain", "test:lib:2.0")
        api("commonTest", "test:lib:1.0")
    }

    @Test
    fun kmpCommonTestHigherVersion() = assertSourceSetDependenciesResolution(
        "kmpCommonTestHigherVersion.txt",
        sanitize = { sanitizer() },
        withProject = kmpProject
    ) {
        mavenRepositoryMock {
            module("test:lib:1.0") dependsOn stdlib(currentKotlinVersion)
            module("test:lib:2.0") dependsOn stdlib(futureKotlinVersion)
        }
        api("commonMain", "test:lib:1.0")
        api("commonTest", "test:lib:2.0")
    }

    @Test
    fun kmpNoDefaultStdlibButFromTransitiveDependencies() {
        val kmpProject = buildProjectWithMPP(preApplyCode = { enableDefaultStdlibDependency(false) }) {
            @OptIn(ExperimentalWasmDsl::class)
            kotlin {
                jvm()
                linuxX64()
                js { browser() }
                wasmJs { browser() }
                wasmWasi { nodejs() }
            }
        }
        assertSourceSetDependenciesResolution(
            "kmpNoDefaultStdlibButFromTransitiveDependencies.txt",
            sanitize = { sanitizer() },
            withProject = kmpProject
        ) {
            it.configureRepositoriesForTests()
            mavenRepositoryMock {
                module("test:lib:1.0") dependsOn stdlib(oldKotlinVersion)
                module("test:lib2:1.0") dependsOn stdlib(futureKotlinVersion)
            }

            api("commonMain", "test:lib:1.0")
            api("commonTest", "test:lib2:1.0")
        }
    }

}



