/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

val KAPT_LITE_OPTIONS = CompilerConfigurationKey.create<KaptLiteOptions.Builder>("Kapt-lite options")

class KaptLiteOptions(val stubsOutputDir: File) {
    class Builder {
        var stubsOutputDir: File? = null

        fun build(): KaptLiteOptions? {
            val stubsOutputDir = this.stubsOutputDir ?: return null
            return KaptLiteOptions(stubsOutputDir)
        }
    }
}