/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module

abstract class BuildSystemType {
    object JPS : BuildSystemType()
}

interface BuildSystemTypeDetector {
    fun detectBuildSystemType(module: Module): BuildSystemType?

    companion object {
        val EP_NAME = ExtensionPointName.create<BuildSystemTypeDetector>("org.jetbrains.kotlin.buildSystemTypeDetector")
    }
}

fun Module.getBuildSystemType(): BuildSystemType {
    @Suppress("DEPRECATION")
    for (extension in Extensions.getExtensions(BuildSystemTypeDetector.EP_NAME)) {
        val result = extension.detectBuildSystemType(this)
        if (result != null) {
            return result
        }
    }
    return BuildSystemType.JPS
}
