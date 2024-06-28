/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.plugins.JavaBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction

internal val ApplyJavaBasePluginSetupAction = KotlinProjectSetupAction {
    project.plugins.apply(JavaBasePlugin::class.java)
}