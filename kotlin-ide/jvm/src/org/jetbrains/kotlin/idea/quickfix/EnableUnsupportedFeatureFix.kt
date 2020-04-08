/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.configuration.BuildSystemType
import org.jetbrains.kotlin.idea.configuration.findApplicableConfigurator
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.core.isInTestSourceContentKotlinAware
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.getRuntimeLibraryVersion
import org.jetbrains.kotlin.idea.roots.invalidateProjectRoots
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.versions.findKotlinRuntimeLibrary
import org.jetbrains.kotlin.idea.versions.updateLibraries
import org.jetbrains.kotlin.psi.KtFile

sealed class EnableUnsupportedFeatureFix(
    element: PsiElement,
    protected val feature: LanguageFeature,
    protected val apiVersionOnly: Boolean,
    protected val isModule: Boolean,
) : KotlinQuickFixAction<PsiElement>(element) {
    override fun getFamilyName() = KotlinJvmBundle.message(
        "enable.feature.family",
        0.takeIf { isModule } ?: 1,
        0.takeIf { apiVersionOnly } ?: 1
    )

    override fun getText() = KotlinJvmBundle.message(
        "enable.feature.text",
        0.takeIf { isModule } ?: 1,
        0.takeIf { apiVersionOnly } ?: 1,
        if (apiVersionOnly) feature.sinceApiVersion.versionString else feature.sinceVersion?.versionString.toString()
    )

    class InModule(element: PsiElement, feature: LanguageFeature, apiVersionOnly: Boolean) :
        EnableUnsupportedFeatureFix(element, feature, apiVersionOnly, isModule = true) {
        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return

            val facetSettings = KotlinFacetSettingsProvider.getInstance(project)?.getInitializedSettings(module)
            val targetApiLevel = facetSettings?.apiLevel?.let { apiLevel ->
                if (ApiVersion.createByLanguageVersion(apiLevel) < feature.sinceApiVersion)
                    feature.sinceApiVersion.versionString
                else
                    null
            }
            val forTests = ModuleRootManager.getInstance(module).fileIndex.isInTestSourceContentKotlinAware(file.virtualFile)

            findApplicableConfigurator(module).updateLanguageVersion(
                module,
                if (apiVersionOnly) null else feature.sinceVersion!!.versionString,
                targetApiLevel,
                feature.sinceApiVersion,
                forTests
            )
        }
    }

    class InProject(element: PsiElement, feature: LanguageFeature, apiVersionOnly: Boolean) :
        EnableUnsupportedFeatureFix(element, feature, apiVersionOnly, isModule = false) {
        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val targetVersion = feature.sinceVersion!!

            KotlinCommonCompilerArgumentsHolder.getInstance(project).update {
                val parsedApiVersion = apiVersion?.let { ApiVersion.parse(it) }
                if (parsedApiVersion != null && feature.sinceApiVersion > parsedApiVersion) {
                    if (!checkUpdateRuntime(project, feature.sinceApiVersion)) return@update
                    apiVersion = feature.sinceApiVersion.versionString
                }

                if (!apiVersionOnly) {
                    languageVersion = targetVersion.versionString
                }
            }
            project.invalidateProjectRoots()
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): EnableUnsupportedFeatureFix? {
            val (feature, languageFeatureSettings) = Errors.UNSUPPORTED_FEATURE.cast(diagnostic).a

            val sinceVersion = feature.sinceVersion ?: return null
            val apiVersionOnly = sinceVersion <= languageFeatureSettings.languageVersion &&
                    feature.sinceApiVersion > languageFeatureSettings.apiVersion

            if (!sinceVersion.isStable && !ApplicationManager.getApplication().isInternal) {
                return null
            }

            val module = ModuleUtilCore.findModuleForPsiElement(diagnostic.psiElement) ?: return null
            if (module.getBuildSystemType() == BuildSystemType.JPS) {
                val facetSettings = KotlinFacet.get(module)?.configuration?.settings
                if (facetSettings == null || facetSettings.useProjectSettings) return InProject(
                    diagnostic.psiElement,
                    feature,
                    apiVersionOnly
                )
            }
            return InModule(diagnostic.psiElement, feature, apiVersionOnly)
        }
    }
}

fun checkUpdateRuntime(project: Project, requiredVersion: ApiVersion): Boolean {
    val modulesWithOutdatedRuntime = project.allModules().filter { module ->
        val parsedModuleRuntimeVersion = getRuntimeLibraryVersion(module)?.let { version ->
            ApiVersion.parse(version.substringBefore("-"))
        }
        parsedModuleRuntimeVersion != null && parsedModuleRuntimeVersion < requiredVersion
    }
    if (modulesWithOutdatedRuntime.isNotEmpty()) {
        if (!askUpdateRuntime(project, requiredVersion,
                              modulesWithOutdatedRuntime.mapNotNull { findKotlinRuntimeLibrary(it) })
        ) return false
    }
    return true
}

fun askUpdateRuntime(project: Project, requiredVersion: ApiVersion, librariesToUpdate: List<Library>): Boolean {
    if (!ApplicationManager.getApplication().isUnitTestMode) {
        val rc = Messages.showOkCancelDialog(
            project,
            KotlinJvmBundle.message(
                "this.language.feature.requires.version.0.or.later.of.the.kotlin.runtime.library.would.you.like.to.update.the.runtime.library.in.your.project",
                requiredVersion
            ),
            KotlinJvmBundle.message("update.runtime.library"),
            Messages.getQuestionIcon()
        )
        if (rc != Messages.OK) return false
    }

    updateLibraries(project, librariesToUpdate)
    return true
}

fun askUpdateRuntime(module: Module, requiredVersion: ApiVersion): Boolean {
    val library = findKotlinRuntimeLibrary(module) ?: return true
    return askUpdateRuntime(module.project, requiredVersion, listOf(library))
}
