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

package org.jetbrains.kotlin.android.synthetic.idea

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.util.Key
import org.gradle.api.Project
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.ANDROID_COMPILER_PLUGIN_ID
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.DEFAULT_CACHE_IMPL_OPTION
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.ENABLED_OPTION
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.EXPERIMENTAL_OPTION
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.idea.configuration.GradleProjectImportHandler
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.util.NotNullableCopyableDataNodeUserDataProperty
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.Serializable
import java.lang.Exception

var DataNode<ModuleData>.hasAndroidExtensionsPlugin: Boolean
        by NotNullableCopyableDataNodeUserDataProperty(Key.create<Boolean>("HAS_ANDROID_EXTENSIONS_PLUGIN"), false)

var DataNode<ModuleData>.isExperimental: Boolean
        by NotNullableCopyableDataNodeUserDataProperty(Key.create<Boolean>("ANDROID_EXTENSIONS_IS_EXPERIMENTAL"), false)

private const val DEFAULT_CACHE_IMPLEMENTATION_DEFAULT_VALUE = "hashMap"

var DataNode<ModuleData>.defaultCacheImplementation: String
        by NotNullableCopyableDataNodeUserDataProperty(Key.create<String>("ANDROID_EXTENSIONS_DEFAULT_CACHE_IMPL"),
                                                       DEFAULT_CACHE_IMPLEMENTATION_DEFAULT_VALUE)

interface AndroidExtensionsGradleModel : Serializable {
    val hasAndroidExtensionsPlugin: Boolean
    val isExperimental: Boolean
    val defaultCacheImplementation: String
}

class AndroidExtensionsGradleModelImpl(
        override val hasAndroidExtensionsPlugin: Boolean,
        override val isExperimental: Boolean,
        override val defaultCacheImplementation: String
) : AndroidExtensionsGradleModel

@Suppress("unused")
class AndroidExtensionsProjectResolverExtension : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses() = setOf(AndroidExtensionsGradleModel::class.java)
    override fun getToolingExtensionsClasses() = setOf(AndroidExtensionsModelBuilderService::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val androidExtensionsModel = resolverCtx.getExtraProject(gradleModule, AndroidExtensionsGradleModel::class.java) ?: return

        ideModule.hasAndroidExtensionsPlugin = androidExtensionsModel.hasAndroidExtensionsPlugin
        ideModule.isExperimental = androidExtensionsModel.isExperimental
        ideModule.defaultCacheImplementation = androidExtensionsModel.defaultCacheImplementation

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}

class AndroidExtensionsModelBuilderService : ModelBuilderService {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
                .withDescription("Unable to build Android Extensions plugin configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == AndroidExtensionsGradleModel::class.java.name

    override fun buildAll(modelName: String?, project: Project): Any {
        val androidExtensionsPlugin = project.plugins.findPlugin("kotlin-android-extensions")

        val androidExtensionsExtension = project.extensions.findByName("androidExtensions")

        val isExperimental = androidExtensionsExtension?.let { ext ->
            val isExperimentalMethod = ext::class.java.methods
                    .firstOrNull { it.name == "isExperimental" && it.parameterCount == 0 }
                    ?: return@let false

            isExperimentalMethod.invoke(ext) as? Boolean
        } ?: false

        val defaultCacheImplementation = androidExtensionsExtension?.let { ext ->
            val defaultCacheImplementationMethod = ext::class.java.methods.firstOrNull {
                it.name == "getDefaultCacheImplementation" && it.parameterCount == 0
            } ?: return@let DEFAULT_CACHE_IMPLEMENTATION_DEFAULT_VALUE

            val enumValue = defaultCacheImplementationMethod.invoke(ext) ?: return@let DEFAULT_CACHE_IMPLEMENTATION_DEFAULT_VALUE

            val optionNameMethod = enumValue::class.java.methods.firstOrNull { it.name == "getOptionName" && it.parameterCount == 0 }
                                   ?: return@let DEFAULT_CACHE_IMPLEMENTATION_DEFAULT_VALUE

            optionNameMethod.invoke(enumValue) as? String
        } ?: DEFAULT_CACHE_IMPLEMENTATION_DEFAULT_VALUE

        return AndroidExtensionsGradleModelImpl(androidExtensionsPlugin != null, isExperimental, defaultCacheImplementation)
    }
}

@Suppress("unused")
class AndroidExtensionsGradleImportHandler : GradleProjectImportHandler {
    override fun importBySourceSet(facet: KotlinFacet, sourceSetNode: DataNode<GradleSourceSetData>) {
        val module = ExternalSystemApiUtil.findParent(sourceSetNode, ProjectKeys.MODULE) ?: return
        importByModule(facet, module)
    }

    override fun importByModule(facet: KotlinFacet, moduleNode: DataNode<ModuleData>) {
        val facetSettings = facet.configuration.settings
        val commonArguments = facetSettings.compilerArguments ?: CommonCompilerArguments.DummyImpl()

        fun makePluginOption(key: String, value: String) = "plugin:$ANDROID_COMPILER_PLUGIN_ID:$key=$value"

        val newPluginOptions = (commonArguments.pluginOptions ?: emptyArray())
                .filterTo(mutableListOf()) { !it.startsWith("plugin:$ANDROID_COMPILER_PLUGIN_ID:") } // Filter out old options

        if (moduleNode.hasAndroidExtensionsPlugin) {
            newPluginOptions += makePluginOption(EXPERIMENTAL_OPTION.optionName, moduleNode.isExperimental.toString())
            newPluginOptions += makePluginOption(ENABLED_OPTION.optionName, moduleNode.hasAndroidExtensionsPlugin.toString())
            newPluginOptions += makePluginOption(DEFAULT_CACHE_IMPL_OPTION.optionName, moduleNode.defaultCacheImplementation)
        }

        commonArguments.pluginOptions = newPluginOptions.toTypedArray()
        facetSettings.compilerArguments = commonArguments
    }
}