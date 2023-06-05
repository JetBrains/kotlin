/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories

internal interface UnameExecutor {
    val unameExecResult: Provider<String>

    interface UnameExecutorVariantFactory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(project: Project): UnameExecutor
    }
}

internal class DefaultUnameExecutorVariantFactory : UnameExecutor.UnameExecutorVariantFactory {
    override fun getInstance(project: Project): UnameExecutor = DefaultUnameExecutor(project.providers)
}

internal class DefaultUnameExecutor(
    private val providerFactory: ProviderFactory,
) : UnameExecutor {
    override val unameExecResult: Provider<String>
        get() {
            val cmd = providerFactory.exec {
                it.executable = "uname"
                it.args = listOf("-m")
            }

            return cmd.standardOutput.asText.map { it.trim() }
        }
}