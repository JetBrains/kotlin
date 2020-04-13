/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver.ide

import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedPluginModel
import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedPluginModelBuilderService
import org.jetbrains.kotlin.annotation.plugin.ide.DumpedPluginModel
import org.jetbrains.kotlin.annotation.plugin.ide.DumpedPluginModelImpl

interface SamWithReceiverModel : AnnotationBasedPluginModel {
    override fun dump(): DumpedPluginModel {
        return DumpedPluginModelImpl(SamWithReceiverModelImpl::class.java, annotations.toList(), presets.toList())
    }
}

class SamWithReceiverModelImpl(
    override val annotations: List<String>,
    override val presets: List<String>
) : SamWithReceiverModel


class SamWithReceiverModelBuilderService : AnnotationBasedPluginModelBuilderService<SamWithReceiverModel>() {
    override val gradlePluginNames get() = listOf("org.jetbrains.kotlin.plugin.sam.with.receiver", "kotlin-sam-with-receiver")
    override val extensionName get() = "samWithReceiver"
    override val modelClass get() = SamWithReceiverModel::class.java

    override fun createModel(annotations: List<String>, presets: List<String>, extension: Any?): SamWithReceiverModelImpl {
        return SamWithReceiverModelImpl(annotations, presets)
    }
}

