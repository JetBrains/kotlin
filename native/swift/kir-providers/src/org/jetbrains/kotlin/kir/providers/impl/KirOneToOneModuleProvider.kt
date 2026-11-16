/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.impl

import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.kir.builder.buildModule
import org.jetbrains.kotlin.kir.providers.KirModuleProvider
import org.jetbrains.kotlin.sir.SirModule

/**
 * A module provider implementation that generates a [KirModule] for each given [SirModule].
 */
public class KirOneToOneModuleProvider : KirModuleProvider {

    private val moduleCache: MutableMap<SirModule, KirModule> = mutableMapOf()

    override fun SirModule.kirModule(): KirModule = moduleCache.getOrPut(this) {
        buildModule {}
    }
}
