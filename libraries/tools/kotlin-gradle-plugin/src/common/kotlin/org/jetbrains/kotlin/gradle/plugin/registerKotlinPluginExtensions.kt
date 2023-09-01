/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.AndroidMainSourceSetConventionsChecker
import org.jetbrains.kotlin.gradle.dsl.IosSourceSetConventionChecker
import org.jetbrains.kotlin.gradle.dsl.PlatformSourceSetConventionsChecker
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.GeneralKotlinExtensionParameterSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.MultiplatformSetupAdditionalCompilerArgumentsAction

internal fun Project.registerKotlinPluginExtensions() {
    KotlinProjectAction.extensionPoint.apply {
        register(project, GeneralKotlinExtensionParameterSetupAction)

        if (multiplatformExtensionOrNull != null) {
            register(project, MultiplatformSetupAdditionalCompilerArgumentsAction)
        }
    }

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
    }
}