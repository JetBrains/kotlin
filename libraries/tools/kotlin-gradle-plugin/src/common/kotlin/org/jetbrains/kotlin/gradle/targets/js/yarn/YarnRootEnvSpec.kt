/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootEnvSpec

/**
 * Spec for Yarn - package manager to install NPM dependencies
 */
abstract class YarnRootEnvSpec : BaseYarnRootEnvSpec() {
    companion object : HasPlatformDisambiguator by JsPlatformDisambiguator {
        val YARN: String
            get() = extensionName("yarnSpec")
    }
}