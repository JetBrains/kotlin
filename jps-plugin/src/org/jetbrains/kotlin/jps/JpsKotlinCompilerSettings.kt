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

package org.jetbrains.kotlin.jps

import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.ex.JpsElementBase
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.module.JpsModuleDependency
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyBean
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.jps.model.kotlinFacetExtension

class JpsKotlinCompilerSettings : JpsElementBase<JpsKotlinCompilerSettings>() {
    private var commonCompilerArguments: CommonCompilerArguments = CommonCompilerArguments.DummyImpl()
    private var k2JvmCompilerArguments = K2JVMCompilerArguments()
    private var k2JsCompilerArguments = K2JSCompilerArguments()
    private var compilerSettings = CompilerSettings()

    override fun createCopy(): JpsKotlinCompilerSettings {
        val copy = JpsKotlinCompilerSettings()
        copy.commonCompilerArguments = this.commonCompilerArguments
        copy.k2JvmCompilerArguments = this.k2JvmCompilerArguments
        copy.k2JsCompilerArguments = this.k2JsCompilerArguments
        copy.compilerSettings = this.compilerSettings
        return copy
    }

    override fun applyChanges(modified: JpsKotlinCompilerSettings) {
        // do nothing
    }

    companion object {
        internal val ROLE = JpsElementChildRoleBase.create<JpsKotlinCompilerSettings>("Kotlin Compiler Settings")

        fun getSettings(project: JpsProject) = project.container.getChild(ROLE) ?: JpsKotlinCompilerSettings()

        fun getOrCreateSettings(project: JpsProject): JpsKotlinCompilerSettings {
            var settings = project.container.getChild(ROLE)
            if (settings == null) {
                settings = JpsKotlinCompilerSettings()
                project.container.setChild(ROLE, settings)
            }
            return settings
        }

        fun getCommonCompilerArguments(module: JpsModule): CommonCompilerArguments {
            val defaultArguments = getSettings(module.project).commonCompilerArguments
            val facetSettings = module.kotlinFacetExtension?.settings ?: return defaultArguments
            val (languageLevel, apiLevel) = facetSettings.versionInfo
            val facetArguments = facetSettings.compilerInfo.commonCompilerArguments ?: return defaultArguments
            return copyBean(facetArguments).apply {
                if (facetSettings.useProjectSettings) {
                    languageVersion = defaultArguments.languageVersion
                    apiVersion = defaultArguments.apiVersion
                    multiPlatform = defaultArguments.multiPlatform
                }
                else {
                    languageVersion = languageLevel?.description
                    apiVersion = apiLevel?.description
                    multiPlatform = module
                            .dependenciesList
                            .dependencies
                            .any { (it as? JpsModuleDependency)?.module?.targetPlatform == TargetPlatformKind.Common }
                }
            }
        }

        fun setCommonCompilerArguments(project: JpsProject, commonCompilerSettings: CommonCompilerArguments) {
            getOrCreateSettings(project).commonCompilerArguments = commonCompilerSettings
        }

        fun getK2JvmCompilerArguments(module: JpsModule): K2JVMCompilerArguments {
            val defaultArguments = getSettings(module.project).k2JvmCompilerArguments
            val facetSettings = module.kotlinFacetExtension?.settings ?: return defaultArguments
            if (facetSettings.useProjectSettings) return defaultArguments
            val targetPlatform = facetSettings.versionInfo.targetPlatformKind as? TargetPlatformKind.Jvm ?: return defaultArguments
            return copyBean(defaultArguments).apply {
                jvmTarget = targetPlatform.version.description
            }
        }

        fun setK2JvmCompilerArguments(project: JpsProject, k2JvmCompilerArguments: K2JVMCompilerArguments) {
            getOrCreateSettings(project).k2JvmCompilerArguments = k2JvmCompilerArguments
        }

        fun getK2JsCompilerArguments(module: JpsModule): K2JSCompilerArguments {
            val defaultArguments = getSettings(module.project).k2JsCompilerArguments
            val facetSettings = module.kotlinFacetExtension?.settings ?: return defaultArguments
            if (facetSettings.useProjectSettings) return defaultArguments
            return facetSettings.compilerInfo.k2jsCompilerArguments ?: defaultArguments
        }

        fun setK2JsCompilerArguments(project: JpsProject, k2JsCompilerArguments: K2JSCompilerArguments) {
            getOrCreateSettings(project).k2JsCompilerArguments = k2JsCompilerArguments
        }

        fun getCompilerSettings(module: JpsModule): CompilerSettings {
            val defaultSettings = getSettings(module.project).compilerSettings
            val facetSettings = module.kotlinFacetExtension?.settings ?: return defaultSettings
            if (facetSettings.useProjectSettings) return defaultSettings
            return facetSettings.compilerInfo.compilerSettings ?: defaultSettings
        }

        fun setCompilerSettings(project: JpsProject, compilerSettings: CompilerSettings) {
            getOrCreateSettings(project).compilerSettings = compilerSettings
        }
    }
}

val JpsModule.targetPlatform: TargetPlatformKind<*>?
    get() = kotlinFacetExtension?.settings?.versionInfo?.targetPlatformKind