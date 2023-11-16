/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.konan.target.*
import java.io.File
import javax.inject.Inject

abstract class ExecClang @Inject constructor(
        private val platformManager: PlatformManager,
) {

    @get:Inject
    protected abstract val fileOperations: FileOperations
    @get:Inject
    protected abstract val execOperations: ExecOperations

    private fun clangArgsForCppRuntime(target: KonanTarget): List<String> {
        return platformManager.platform(target).clang.clangArgsForKonanSources.asList()
    }

    fun clangArgsForCppRuntime(targetName: String?): List<String> {
        val target = platformManager.targetManager(targetName).target
        return clangArgsForCppRuntime(target)
    }

    fun resolveExecutable(executableOrNull: String?): String {
        val executable = executableOrNull ?: "clang"

        if (listOf("clang", "clang++").contains(executable)) {
            return "${platformManager.hostPlatform.absoluteLlvmHome}/bin/$executable"
        } else {
            throw GradleException("unsupported clang executable: $executable")
        }
    }

    fun resolveToolchainExecutable(target: KonanTarget, executableOrNull: String?): String {
        val executable = executableOrNull ?: "clang"

        if (listOf("clang", "clang++").contains(executable)) {
            // TODO: This is copied from `BitcodeCompiler`. Consider sharing the code instead.
            val platform = platformManager.platform(target)
            return "${platform.absoluteTargetToolchain}/bin/$executable"
        } else {
            throw GradleException("unsupported clang executable: $executable")
        }
    }

    // The bare ones invoke clang with system default sysroot.

    fun execBareClang(action: Action<in ExecSpec>): ExecResult {
        return this.execClang(emptyList(), action)
    }

    // The konan ones invoke clang with konan provided sysroots.
    // So they require a target or assume it to be the host.
    // The target can be specified as KonanTarget or as a
    // (nullable, which means host) target name.

    fun execKonanClang(target: String?, action: Action<in ExecSpec>): ExecResult {
        return this.execClang(clangArgsForCppRuntime(target), action)
    }

    fun execKonanClang(target: KonanTarget, action: Action<in ExecSpec>): ExecResult {
        return this.execClang(clangArgsForCppRuntime(target), action)
    }

    /**
     * Execute Clang the way that produced object file is compatible with
     * the one that produced by Kotlin/Native for given [target]. It means:
     * 1. We pass flags that set sysroot.
     * 2. We call Clang from toolchain in case of Apple target.
     */
    fun execClangForCompilerTests(target: KonanTarget, action: Action<in ExecSpec>): ExecResult {
        val defaultArgs = platformManager.platform(target).clang.clangArgs.toList()
        return execOperations.exec {
            action.execute(this)
            executable = if (target.family.isAppleFamily) {
                resolveToolchainExecutable(target, executable)
            } else {
                resolveExecutable(executable)
            }
            args = defaultArgs + args
        }
    }

    // The toolchain ones execute clang from the toolchain.

    fun execToolchainClang(target: String?, action: Action<in ExecSpec>): ExecResult {
        return this.execToolchainClang(platformManager.targetManager(target).target, action)
    }

    fun execToolchainClang(target: KonanTarget, action: Action<in ExecSpec>): ExecResult {
        val extendedAction = Action<ExecSpec> {
            action.execute(this)
            executable = resolveToolchainExecutable(target, executable)
        }
        return execOperations.exec(extendedAction)
    }

    private fun execClang(defaultArgs: List<String>, action: Action<in ExecSpec>): ExecResult {
        val extendedAction = Action<ExecSpec> {
            action.execute(this)
            executable = resolveExecutable(executable)

            val hostPlatform = platformManager.hostPlatform
            environment["PATH"] = fileOperations.configurableFiles(hostPlatform.clang.clangPaths).asPath +
                    File.pathSeparator + environment["PATH"]
            args = args + defaultArgs
        }
        return execOperations.exec(extendedAction)
    }

    companion object {
        @JvmStatic
        fun create(project: Project): ExecClang = create(
                project.objects,
                project.project(":kotlin-native").findProperty("platformManager") as PlatformManager,
        )

        @JvmStatic
        fun create(objects: ObjectFactory, platformManager: PlatformManager) =
                objects.newInstance(ExecClang::class.java, platformManager)
    }
}
