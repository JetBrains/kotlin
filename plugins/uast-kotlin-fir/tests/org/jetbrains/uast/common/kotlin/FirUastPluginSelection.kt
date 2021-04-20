/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.common.kotlin

interface FirUastPluginSelection {
    // Whether this is FIR UAST plugin or FE 1.0 UAST plugin
    val isFirUastPlugin: Boolean

    val pluginSuffix: String
        get() = if (isFirUastPlugin) ".fir" else ".fe10"
}
