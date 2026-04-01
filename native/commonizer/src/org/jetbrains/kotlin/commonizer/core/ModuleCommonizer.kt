/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import kotlinx.metadata.klib.KlibMetadataVersion
import org.jetbrains.kotlin.commonizer.cir.CirModule
import org.jetbrains.kotlin.commonizer.cir.CirName

class ModuleCommonizer : AbstractStandardCommonizer<CirModule, CirModule>() {
    private lateinit var name: CirName
    private lateinit var metadataVersion: KlibMetadataVersion

    override fun commonizationResult() = CirModule.create(name = name, metadataVersion = metadataVersion)

    override fun initialize(first: CirModule) {
        name = first.name
        metadataVersion = first.metadataVersion
    }

    override fun doCommonizeWith(next: CirModule): Boolean {
        metadataVersion = minOf(metadataVersion, next.metadataVersion)
        return true
    }
}
