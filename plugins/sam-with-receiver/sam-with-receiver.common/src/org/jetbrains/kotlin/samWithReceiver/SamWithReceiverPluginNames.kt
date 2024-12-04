/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver

object SamWithReceiverPluginNames {
    const val PLUGIN_ID = "org.jetbrains.kotlin.samWithReceiver"
    const val ANNOTATION_OPTION_NAME = "annotation"
    const val PRESET_OPTION_NAME = "preset"

    val SUPPORTED_PRESETS = emptyMap<String, List<String>>()
}
