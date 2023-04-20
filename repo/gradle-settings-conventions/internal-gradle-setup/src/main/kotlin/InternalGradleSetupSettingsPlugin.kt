/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

abstract class InternalGradleSetupSettingsPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {
    }
}