/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.mock.MockProject
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.lang.resolve.android.*
import org.jetbrains.kotlin.psi.JetFile

public object AndroidConfigurationKeys {

    public val ANDROID_RES_PATH: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("android resources search path")

    public val ANDROID_MANIFEST: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("android manifest file")
}

public class AndroidCommandLineProcessor : CommandLineProcessor {
    default object {
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

public class CliAndroidDeclarationsProvider(private val project: Project) : ExternalDeclarationsProvider {
    override fun getExternalDeclarations(moduleInfo: ModuleInfo?): Collection<JetFile> {
        val parser = ServiceManager.getService<AndroidUIXmlProcessor>(project, javaClass<AndroidUIXmlProcessor>())
        return parser.parseToPsi() ?: listOf()
    }
}

public class AndroidComponentRegistrar : ComponentRegistrar {

    public override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val androidResPath = configuration.get(AndroidConfigurationKeys.ANDROID_RES_PATH)
        val androidManifest = configuration.get(AndroidConfigurationKeys.ANDROID_MANIFEST)

        if (androidResPath != null && androidManifest != null) {
            project.registerService(javaClass<AndroidUIXmlProcessor>(), CliAndroidUIXmlProcessor(project, androidManifest, androidResPath))
            project.registerService(javaClass<AndroidResourceManager>(), CliAndroidResourceManager(project, androidManifest, androidResPath))

            ExternalDeclarationsProvider.registerExtension(project, CliAndroidDeclarationsProvider(project))
            ExpressionCodegenExtension.registerExtension(project, AndroidExpressionCodegenExtension())
            Extensions.getArea(project).getExtensionPoint(
                    PsiTreeChangePreprocessor.EP_NAME).registerExtension(AndroidPsiTreeChangePreprocessor())
        }
    }
}