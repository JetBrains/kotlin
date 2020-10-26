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
import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.file.*

class ExecClang(private val project: Project) {

    private val platformManager = project.rootProject.findProperty("platformManager") as PlatformManager

    private fun konanArgs(target: KonanTarget): List<String> {
        return platformManager.platform(target).clang.clangArgsForKonanSources.asList()
    }

    fun konanArgs(targetName: String?): List<String> {
        val target = platformManager.targetManager(targetName).target
        return konanArgs(target)
    }

    fun resolveExecutable(executableOrNull: String?): String {
        val executable = executableOrNull ?: "clang"

        if (listOf("clang", "clang++").contains(executable)) {
            val llvmDir = project.findProperty("llvmDir")
            return "${llvmDir}/bin/$executable"
        } else {
            throw GradleException("unsupported clang executable: $executable")
        }
    }

    // The bare ones invoke clang with system default sysroot.

    fun execBareClang(action: Action<in ExecSpec>): ExecResult {
        return this.execClang(emptyList<String>(), action)
    }

    fun execBareClang(closure: Closure<in ExecSpec>): ExecResult {
        return this.execClang(emptyList<String>(), closure)
    }

    // The konan ones invoke clang with konan provided sysroots.
    // So they require a target or assume it to be the host.
    // The target can be specified as KonanTarget or as a
    // (nullable, which means host) target name.

    fun execKonanClang(target: String?, action: Action<in ExecSpec>): ExecResult {
        return this.execClang(konanArgs(target), action)
    }

    fun execKonanClang(target: KonanTarget, action: Action<in ExecSpec>): ExecResult {
        return this.execClang(konanArgs(target), action)
    }

    fun execKonanClang(target: String?, closure: Closure<in ExecSpec>): ExecResult {
        return this.execClang(konanArgs(target), closure)
    }

    fun execKonanClang(target: KonanTarget, closure: Closure<in ExecSpec>): ExecResult {
        return this.execClang(konanArgs(target), closure)
    }

    // These ones are private, so one has to choose either Bare or Konan.

    private fun execClang(defaultArgs: List<String>, closure: Closure<in ExecSpec>): ExecResult {
        return this.execClang(defaultArgs, ConfigureUtil.configureUsing(closure))
    }

    private fun execClang(defaultArgs: List<String>, action: Action<in ExecSpec>): ExecResult {
        val extendedAction = object : Action<ExecSpec> {
            override fun execute(execSpec: ExecSpec) {
                action.execute(execSpec)

                execSpec.apply {
                    executable = resolveExecutable(executable)

                    val hostPlatform = project.findProperty("hostPlatform") as Platform
                    environment["PATH"] = project.files(hostPlatform.clang.clangPaths).asPath +
                            java.io.File.pathSeparator + environment["PATH"]
                    args(defaultArgs)
                }
            }
        }
        return project.exec(extendedAction)
    }
}
