/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.STORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.UPGRADE_YARN_LOCK
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.YARN_LOCK_MISMATCH_MESSAGE
import org.jetbrains.kotlin.gradle.tasks.USING_JS_IR_BACKEND_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.TestVersions.Gradle.G_7_6
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledIf
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.streams.toList
import kotlin.test.*

@MppGradlePluginTests
class KotlinWasmGradlePluginIT : KGPBaseTest() {

    @DisplayName("Check wasi target")
    @GradleTest
    fun wasiTarget(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-test", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            buildAndFail(":wasmWasiTest") {
                assertTasksExecuted(":compileKotlinWasmWasi")
                assertTasksFailed(":wasmWasiNodeTest")
            }
        }
    }
}
