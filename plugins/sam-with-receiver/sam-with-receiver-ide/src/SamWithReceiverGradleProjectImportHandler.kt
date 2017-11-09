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

import org.jetbrains.kotlin.annotation.plugin.ide.AbstractGradleImportHandler
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverCommandLineProcessor
import org.jetbrains.kotlin.utils.PathUtil

class SamWithReceiverGradleProjectImportHandler : AbstractGradleImportHandler() {
    override val compilerPluginId = SamWithReceiverCommandLineProcessor.PLUGIN_ID
    override val pluginName = "sam-with-receiver"
    override val annotationOptionName = SamWithReceiverCommandLineProcessor.ANNOTATION_OPTION.name
    override val dataStorageTaskName = "samWithReceiverDataStorageTask"
    override val pluginJarFileFromIdea = PathUtil.kotlinPathsForIdeaPlugin.allOpenPluginJarPath

    override fun getAnnotationsForPreset(presetName: String): List<String> {
        for ((name, annotations) in SamWithReceiverCommandLineProcessor.SUPPORTED_PRESETS.entries) {
            if (presetName == name) {
                return annotations
            }
        }

        return super.getAnnotationsForPreset(presetName)
    }
}
