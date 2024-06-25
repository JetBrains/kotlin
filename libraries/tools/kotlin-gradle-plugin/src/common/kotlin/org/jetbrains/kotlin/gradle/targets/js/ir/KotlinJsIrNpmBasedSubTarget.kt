/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

abstract class KotlinJsIrNpmBasedSubTarget(
    target: KotlinJsIrTarget,
    disambiguationClassifier: String,
) : KotlinJsIrSubTarget(target, disambiguationClassifier) {

    override fun binaryInputFile(binary: JsIrBinary): Provider<RegularFile> {
        return binary.npmProjectMainFileSyncPath()
    }

    override fun binarySyncTaskName(binary: JsIrBinary): String {
        return binary.npmProjectLinkSyncTaskName()
    }

    override fun binarySyncOutput(binary: JsIrBinary): Provider<Directory> {
        return binary.compilation.npmProject.dist
    }
}
