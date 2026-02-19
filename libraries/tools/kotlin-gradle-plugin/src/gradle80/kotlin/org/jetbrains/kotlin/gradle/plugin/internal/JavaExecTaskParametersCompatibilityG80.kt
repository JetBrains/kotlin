/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.JavaExec

internal class JavaExecTaskParametersCompatibilityG80(
    private val javaExecTask: JavaExec
) : JavaExecTaskParametersCompatibility {

    override fun setJvmArgumentsConvention(jvmArgs: ListProperty<String>) {
        javaExecTask.jvmArgumentProviders.add { jvmArgs.get() }
    }

    internal class Factory : JavaExecTaskParametersCompatibility.Factory {
        override fun getInstance(javaExecTask: JavaExec): JavaExecTaskParametersCompatibility =
            JavaExecTaskParametersCompatibilityG80(javaExecTask)
    }
}
