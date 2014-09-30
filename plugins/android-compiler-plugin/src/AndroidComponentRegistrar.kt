/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android

import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.jet.config.CompilerConfiguration
import com.intellij.mock.MockProject
import org.jetbrains.jet.config.CompilerConfigurationKey
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.jet.extensions.ExternalDeclarationsProvider
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.android.*
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.jet.utils.emptyOrSingletonList
import com.intellij.openapi.project.Project

public object AndroidConfigurationKeys {

    public val ANDROID_RES_PATH: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("android resources search path")

    public val ANDROID_MANIFEST: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("android manifest file")
}

public class AndroidCommandLineProcessor : CommandLineProcessor {
    class object {
        public val ANDROID_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.android"

        public val RESOURCE_PATH_OPTION: CliOption = CliOption("androidRes", "<path>", "Android resources path")
        public val MANIFEST_FILE_OPTION: CliOption = CliOption("androidManifest", "<path>", "Android manifest file")
    }

    override val pluginId: String = ANDROID_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(RESOURCE_PATH_OPTION, MANIFEST_FILE_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            RESOURCE_PATH_OPTION -> configuration.put(AndroidConfigurationKeys.ANDROID_RES_PATH, value)
            MANIFEST_FILE_OPTION -> configuration.put(AndroidConfigurationKeys.ANDROID_MANIFEST, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}

public class AndroidDeclarationsProvider(private val project: Project) : ExternalDeclarationsProvider {
    override fun getExternalDeclarations(): Collection<JetFile> {
        val parser = ServiceManager.getService<AndroidUIXmlProcessor>(project, javaClass<AndroidUIXmlProcessor>())
        return emptyOrSingletonList(parser?.parseToPsi(project))
    }
}

public class AndroidComponentRegistrar : ComponentRegistrar {

    public override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val androidResPath = configuration.get(AndroidConfigurationKeys.ANDROID_RES_PATH)
        val androidManifest = configuration.get(AndroidConfigurationKeys.ANDROID_MANIFEST)
        project.registerService(javaClass<AndroidUIXmlProcessor>(), CliAndroidUIXmlProcessor(project, androidResPath, androidManifest))

        ExternalDeclarationsProvider.registerExtension(project, AndroidDeclarationsProvider(project))
    }
}