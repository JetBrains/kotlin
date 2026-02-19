/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

/**
 * [org.gradle.api.tasks.JavaExec] task has some useful parameters introduced in later then
 * Gradle 6.7.3 versions.
 */
internal interface JavaExecTaskParametersCompatibility {
    fun setJvmArgumentsConvention(jvmArgs: ListProperty<String>)

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(
            javaExecTask: JavaExec
        ): JavaExecTaskParametersCompatibility
    }
}

internal class DefaultJavaExecTaskParametersCompatibility(
    private val javaExecTask: JavaExec
) : JavaExecTaskParametersCompatibility {

    override fun setJvmArgumentsConvention(jvmArgs: ListProperty<String>) {
        javaExecTask.jvmArguments.convention(jvmArgs)
    }

    internal class Factory : JavaExecTaskParametersCompatibility.Factory {
        override fun getInstance(javaExecTask: JavaExec): JavaExecTaskParametersCompatibility =
            DefaultJavaExecTaskParametersCompatibility(javaExecTask)
    }
}


internal fun JavaExec.compatibilityWrapper() =
    project.variantImplementationFactory<JavaExecTaskParametersCompatibility.Factory>()
        .getInstance(this)
