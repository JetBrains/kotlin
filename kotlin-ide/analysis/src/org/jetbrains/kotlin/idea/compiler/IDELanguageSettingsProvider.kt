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

package org.jetbrains.kotlin.idea.compiler

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.LanguageSettingsProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.common.arguments.JavaTypeEnhancementStateParser
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.core.script.scriptRelatedModuleName
import org.jetbrains.kotlin.idea.project.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.TargetPlatformVersion
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.subplatformsOfType
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState

object IDELanguageSettingsProvider : LanguageSettingsProvider {
    override fun getLanguageVersionSettings(
        moduleInfo: ModuleInfo,
        project: Project,
        isReleaseCoroutines: Boolean?
    ): LanguageVersionSettings =
        when (moduleInfo) {
            is ModuleSourceInfo -> moduleInfo.module.languageVersionSettings
            is LibraryInfo -> project.getLanguageVersionSettings(
                javaTypeEnhancementState = computeJavaTypeEnhancementState(project), isReleaseCoroutines = isReleaseCoroutines
            )
            is ScriptModuleInfo -> {
                getLanguageSettingsForScripts(
                    project,
                    moduleInfo.scriptFile,
                    moduleInfo.scriptDefinition
                ).languageVersionSettings
            }

            is ScriptDependenciesInfo.ForFile ->
                getLanguageSettingsForScripts(
                    project,
                    moduleInfo.scriptFile,
                    moduleInfo.scriptDefinition
                ).languageVersionSettings
            is PlatformModuleInfo -> moduleInfo.platformModule.module.languageVersionSettings
            else -> project.getLanguageVersionSettings()
        }

    private fun computeJavaTypeEnhancementState(project: Project): JavaTypeEnhancementState? {
        var result: JavaTypeEnhancementState? = null
        for (module in ModuleManager.getInstance(project).modules) {
            val settings = KotlinFacetSettingsProvider.getInstance(project)?.getSettings(module) ?: continue
            val compilerArguments = settings.mergedCompilerArguments as? K2JVMCompilerArguments ?: continue

            result = JavaTypeEnhancementStateParser(MessageCollector.NONE).parse(
                compilerArguments.jsr305,
                compilerArguments.supportCompatqualCheckerFrameworkAnnotations,
                compilerArguments.jspecifyAnnotations
            )

        }
        return result
    }

    // TODO(dsavvinov): get rid of this method; instead store proper instance of TargetPlatformVersion in platform-instance
    override fun getTargetPlatform(moduleInfo: ModuleInfo, project: Project): TargetPlatformVersion =
        when (moduleInfo) {
            is ModuleSourceInfo ->
                moduleInfo.module.platform?.subplatformsOfType<JdkPlatform>()?.firstOrNull()?.targetVersion
                    ?: TargetPlatformVersion.NoVersion
            is ScriptModuleInfo,
            is ScriptDependenciesInfo.ForFile -> detectDefaultTargetPlatformVersion(moduleInfo.platform)
            else -> TargetPlatformVersion.NoVersion
        }
}

private data class ScriptLanguageSettings(
    val languageVersionSettings: LanguageVersionSettings,
    val targetPlatformVersion: TargetPlatformVersion
)

private val SCRIPT_LANGUAGE_SETTINGS = Key.create<CachedValue<ScriptLanguageSettings>>("SCRIPT_LANGUAGE_SETTINGS")

fun getTargetPlatformVersionForScript(project: Project, file: VirtualFile, scriptDefinition: ScriptDefinition): TargetPlatformVersion {
    return getLanguageSettingsForScripts(project, file, scriptDefinition).targetPlatformVersion
}

private fun detectDefaultTargetPlatformVersion(platform: TargetPlatform?): TargetPlatformVersion {
    return platform?.subplatformsOfType<JdkPlatform>()?.firstOrNull()?.targetVersion ?: TargetPlatformVersion.NoVersion
}

private fun getLanguageSettingsForScripts(project: Project, file: VirtualFile, scriptDefinition: ScriptDefinition): ScriptLanguageSettings {
    val scriptModule = file.let {
        it.scriptRelatedModuleName?.let { module -> ModuleManager.getInstance(project).findModuleByName(module) }
            ?: ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(it)
    }

    val environmentCompilerOptions = scriptDefinition.defaultCompilerOptions
    val args = scriptDefinition.compilerOptions
    return if (environmentCompilerOptions.none() && args.none()) {
        ScriptLanguageSettings(
            project.getLanguageVersionSettings(contextModule = scriptModule),
            detectDefaultTargetPlatformVersion(scriptModule?.platform)
        )
    } else {
        val settings = scriptDefinition.getUserData(SCRIPT_LANGUAGE_SETTINGS) ?: createCachedValue(project) {
            val compilerArguments = K2JVMCompilerArguments()
            parseCommandLineArguments(environmentCompilerOptions.toList(), compilerArguments)
            parseCommandLineArguments(args.toList(), compilerArguments)
            // TODO: reporting
            val verSettings = compilerArguments.toLanguageVersionSettings(MessageCollector.NONE)
            val jvmTarget =
                compilerArguments.jvmTarget?.let { JvmTarget.fromString(it) } ?: detectDefaultTargetPlatformVersion(scriptModule?.platform)

            val languageVersionSettings = project.getLanguageVersionSettings(contextModule = scriptModule)
            val versionSettings = if (languageVersionSettings.languageVersion.isLess(verSettings.languageVersion)) {
                languageVersionSettings
            } else {
                verSettings
            }

            ScriptLanguageSettings(versionSettings, jvmTarget)
        }.also { scriptDefinition.putUserData(SCRIPT_LANGUAGE_SETTINGS, it) }
        settings.value
    }
}

private fun LanguageVersion.isLess(languageVersion: LanguageVersion): Boolean =
    if (major < languageVersion.major) {
        true
    } else {
        minor < languageVersion.minor
    }

private inline fun createCachedValue(
    project: Project,
    crossinline body: () -> ScriptLanguageSettings
): CachedValue<ScriptLanguageSettings> {
    return CachedValuesManager
        .getManager(project)
        .createCachedValue(
            {
                CachedValueProvider.Result(
                    body(),
                    ProjectRootModificationTracker.getInstance(project), ModuleManager.getInstance(project)
                )
            }, false
        )
}
