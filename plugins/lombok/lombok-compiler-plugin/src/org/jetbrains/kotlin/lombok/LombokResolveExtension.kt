/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.lombok.config.LombokConfig
import org.jetbrains.kotlin.resolve.jvm.SyntheticJavaPartsProvider
import org.jetbrains.kotlin.resolve.jvm.extensions.SyntheticJavaResolveExtension

class LombokResolveExtension(pluginConfig: LombokPluginConfig) : SyntheticJavaResolveExtension {

    private val config = pluginConfig.configFile?.let(LombokConfig::parse) ?: LombokConfig.Empty

    override fun getProvider(): SyntheticJavaPartsProvider = LombokSyntheticJavaPartsProvider(config)
}
