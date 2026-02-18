/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.testbase.GradleProjectBuildScriptInjectionContext
import org.jetbrains.kotlin.gradle.uklibs.applyJvm

/**
 * Selects compiler version for BTA via [org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension.compilerVersion]
 * and forces compiler plugin artifacts to be of the same version [btaCompilerVersion].
 * A workaround for the tests until we provide a general solution via KT-81629
 */
@OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
fun GradleProjectBuildScriptInjectionContext.useCompilerVersion(btaCompilerVersion: String) {
    project.applyJvm {
        compilerVersion.value(btaCompilerVersion)
    }
    project.configurations.named(PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main") {
        it.resolutionStrategy { strategy ->
            strategy.eachDependency { dep ->
                if (dep.requested.group == "org.jetbrains.kotlin") {
                    dep.useVersion(btaCompilerVersion)
                }
            }
        }
    }
}
