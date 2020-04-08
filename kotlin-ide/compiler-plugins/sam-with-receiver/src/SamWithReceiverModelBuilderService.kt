/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.samWithReceiver.ide

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.annotation.plugin.ide.*

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

class SamWithReceiverProjectResolverExtension : AnnotationBasedPluginProjectResolverExtension<SamWithReceiverModel>() {
    companion object {
        val KEY = Key<SamWithReceiverModel>("SamWithReceiverModel")
    }

    override val modelClass get() = SamWithReceiverModel::class.java
    override val userDataKey get() = KEY
}