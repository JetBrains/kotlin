/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.noarg.ide

import org.jetbrains.kotlin.annotation.plugin.ide.AbstractGradleImportHandler
import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedCompilerPluginSetup
import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedCompilerPluginSetup.PluginOption
import org.jetbrains.kotlin.noarg.NoArgCommandLineProcessor
import org.jetbrains.kotlin.utils.PathUtil

class NoArgGradleProjectImportHandler : AbstractGradleImportHandler<NoArgModel>() {
    override val compilerPluginId = NoArgCommandLineProcessor.PLUGIN_ID
    override val pluginName = "noarg"
    override val annotationOptionName = NoArgCommandLineProcessor.ANNOTATION_OPTION.name
    override val pluginJarFileFromIdea = PathUtil.kotlinPathsForIdeaPlugin.noArgPluginJarPath
    override val modelKey = NoArgProjectResolverExtension.KEY

    override fun getAdditionalOptions(model: NoArgModel): List<PluginOption> {
        return listOf(PluginOption(
                NoArgCommandLineProcessor.INVOKE_INITIALIZERS_OPTION.name,
                model.invokeInitializers.toString()))
    }

    override fun getAnnotationsForPreset(presetName: String): List<String> {
        for ((name, annotations) in NoArgCommandLineProcessor.SUPPORTED_PRESETS.entries) {
            if (presetName == name) {
                return annotations
            }
        }

        return super.getAnnotationsForPreset(presetName)
    }
}
