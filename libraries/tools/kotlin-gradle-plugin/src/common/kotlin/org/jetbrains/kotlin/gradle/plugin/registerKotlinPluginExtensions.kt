/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.AndroidMainSourceSetConventionsChecker
import org.jetbrains.kotlin.gradle.dsl.IosSourceSetConventionChecker
import org.jetbrains.kotlin.gradle.dsl.PlatformSourceSetConventionsChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.AndroidPluginWithoutAndroidTargetChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.AndroidSourceSetLayoutV1SourceSetsNotFoundChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.CommonMainOrTestWithDependsOnChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.DeprecatedKotlinNativeTargetsChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.DisabledCinteropCommonizationInHmppProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.DisabledNativeTargetsChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.ExperimentalK2UsageChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.InternalGradlePropertiesUsageChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.JsEnvironmentChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.KotlinSourceSetTreeDependsOnMismatchChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.KotlinTargetAlreadyDeclaredChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.MissingNativeStdlibChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.NoKotlinTargetsDeclaredChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.PreHmppDependenciesUsageChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.UnusedSourceSetsChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.WasmSourceSetsNotFoundChecker

/**
 * Active Extensions (using the [KotlinGradlePluginExtensionPoint] infrastructure) will be registered here by the Kotlin Gradle Plugin.
 */
internal fun Project.registerKotlinPluginExtensions() {
    KotlinGradleProjectChecker.extensionPoint.apply {
        register(project, CommonMainOrTestWithDependsOnChecker)
        register(project, DeprecatedKotlinNativeTargetsChecker)
        register(project, MissingNativeStdlibChecker)
        register(project, UnusedSourceSetsChecker)
        register(project, AndroidSourceSetLayoutV1SourceSetsNotFoundChecker)
        register(project, AndroidPluginWithoutAndroidTargetChecker)
        register(project, NoKotlinTargetsDeclaredChecker)
        register(project, DisabledCinteropCommonizationInHmppProjectChecker)
        register(project, DisabledNativeTargetsChecker)
        register(project, JsEnvironmentChecker)
        register(project, PreHmppDependenciesUsageChecker)
        register(project, ExperimentalK2UsageChecker)
        register(project, KotlinSourceSetTreeDependsOnMismatchChecker)
        register(project, PlatformSourceSetConventionsChecker)
        register(project, AndroidMainSourceSetConventionsChecker)
        register(project, IosSourceSetConventionChecker)
        register(project, KotlinTargetAlreadyDeclaredChecker)
        register(project, InternalGradlePropertiesUsageChecker)
        register(project, WasmSourceSetsNotFoundChecker)
    }
}
