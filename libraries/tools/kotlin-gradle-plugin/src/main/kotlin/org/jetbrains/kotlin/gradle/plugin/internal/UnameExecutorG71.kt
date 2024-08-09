/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.nodejs.UnameExecutor
import java.io.ByteArrayOutputStream

internal class UnameExecutorG71(
    private val project: Project,
) : UnameExecutor {
    override val unameExecResult: Provider<String>
        get() {
            return project.provider {
                val out = ByteArrayOutputStream()
                val cmd = project.exec {
                    it.executable = "uname"
                    it.args = listOf("-m")
                    it.standardOutput = out
                }

                cmd.assertNormalExitValue()
                out.toString().trim()
            }
        }

    internal class UnameExecutorVariantFactoryG71 : UnameExecutor.UnameExecutorVariantFactory {
        override fun getInstance(project: Project): UnameExecutor = UnameExecutorG71(project)
    }
}

