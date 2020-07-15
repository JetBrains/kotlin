/*
 * Copyright 2000-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.CodeStyle
import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newMetric
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.idea.formatter.KotlinFormatterUsageCollector.KotlinFormatterKind.*
import org.jetbrains.kotlin.idea.util.isDefaultIntellijObsoleteCodeStyle

class KotlinFormatterUsageCollector : ProjectUsagesCollector() {

    override fun getGroupId() = "kotlin.ide.formatter"
    override fun getVersion(): Int = 1

    override fun getMetrics(project: Project): Set<MetricEvent> {
        val usedFormatter = getKotlinFormatterKind(project)

        val data = FeatureUsageData()
            .addData("kind", usedFormatter.name)
            .addData("defaults", getDefaultCodeStyle(project))

        return setOf(
            newMetric("settings", data)
        )
    }

    private fun getDefaultCodeStyle(project: Project): String {

        val settings = CodeStyle.getSettings(project)
        val kotlinCommonSettings = settings.kotlinCommonSettings
        val kotlinCustomSettings = settings.kotlinCustomSettings

        val defaults = kotlinCustomSettings.CODE_STYLE_DEFAULTS ?: kotlinCommonSettings.CODE_STYLE_DEFAULTS

        return defaults ?: "ide_defaults"
    }


    companion object {

        private val KOTLIN_DEFAULT_COMMON = CodeStyle.getDefaultSettings().kotlinCommonSettings.also {
            KotlinStyleGuideCodeStyle.applyToCommonSettings(it)
        }

        private val KOTLIN_DEFAULT_CUSTOM by lazy {
            CodeStyle.getDefaultSettings().kotlinCustomSettings.cloneSettings().also {
                KotlinStyleGuideCodeStyle.applyToKotlinCustomSettings(it)
            }
        }

        private val KOTLIN_OBSOLETE_DEFAULT_COMMON = KotlinLanguageCodeStyleSettingsProvider().defaultCommonSettings.also {
            KotlinObsoleteCodeStyle.applyToCommonSettings(it)
        }

        private val KOTLIN_OBSOLETE_DEFAULT_CUSTOM by lazy {
            CodeStyle.getDefaultSettings().kotlinCustomSettings.cloneSettings().also {
                KotlinObsoleteCodeStyle.applyToKotlinCustomSettings(it)
            }
        }

        fun getKotlinFormatterKind(project: Project): KotlinFormatterKind {
            val isProject = CodeStyleSettingsManager.getInstance(project).USE_PER_PROJECT_SETTINGS

            val settings = CodeStyle.getSettings(project)
            val kotlinCommonSettings = settings.kotlinCommonSettings
            val kotlinCustomSettings = settings.kotlinCustomSettings
            val isDefaultIntellijObsoleteCodeStyle = kotlinCommonSettings.isDefaultIntellijObsoleteCodeStyle && kotlinCustomSettings.isDefaultIntellijObsoleteCodeStyle

            val isDefaultKotlinCommonSettings = kotlinCommonSettings == CodeStyle.getDefaultSettings().kotlinCommonSettings
            val isDefaultKotlinCustomSettings = kotlinCustomSettings == CodeStyle.getDefaultSettings().kotlinCustomSettings

            if (isDefaultKotlinCommonSettings && isDefaultKotlinCustomSettings) {
                return if (isDefaultIntellijObsoleteCodeStyle) {
                    paired(IDEA_OFFICIAL_DEFAULT, isProject)
                } else {
                    paired(IDEA_DEFAULT, isProject)
                }
            }

            if (kotlinCommonSettings == KOTLIN_OBSOLETE_DEFAULT_COMMON && kotlinCustomSettings == KOTLIN_OBSOLETE_DEFAULT_CUSTOM) {
                return paired(IDEA_OBSOLETE_KOTLIN, isProject)
            }

            if (kotlinCommonSettings == KOTLIN_DEFAULT_COMMON && kotlinCustomSettings == KOTLIN_DEFAULT_CUSTOM) {
                return paired(IDEA_KOTLIN, isProject)
            }

            val isKotlinOfficialLikeSettings = settings == CodeStyleSettingsManager.getInstance(project).cloneSettings(settings).also {
                KotlinStyleGuideCodeStyle.apply(it)
            }
            if (isKotlinOfficialLikeSettings) {
                return paired(IDEA_OFFICIAL_KOTLIN_WITH_CUSTOM, isProject)
            }

            val isKotlinObsoleteLikeSettings = settings == CodeStyleSettingsManager.getInstance(project).cloneSettings(settings).also {
                KotlinObsoleteCodeStyle.apply(it)
            }
            if (isKotlinObsoleteLikeSettings) {
                return paired(IDEA_KOTLIN_WITH_CUSTOM, isProject)
            }

            return paired(IDEA_CUSTOM, isProject)
        }

        private fun paired(kind: KotlinFormatterKind, isProject: Boolean): KotlinFormatterKind {
            if (!isProject) return kind

            return when (kind) {
                IDEA_DEFAULT -> PROJECT_DEFAULT
                IDEA_OFFICIAL_DEFAULT -> PROJECT_OFFICIAL_DEFAULT
                IDEA_CUSTOM -> PROJECT_CUSTOM
                IDEA_KOTLIN_WITH_CUSTOM -> PROJECT_KOTLIN_WITH_CUSTOM
                IDEA_KOTLIN -> PROJECT_KOTLIN
                IDEA_OBSOLETE_KOTLIN -> PROJECT_OBSOLETE_KOTLIN
                IDEA_OFFICIAL_KOTLIN_WITH_CUSTOM -> PROJECT_OBSOLETE_KOTLIN_WITH_CUSTOM
                else -> kind
            }
        }
    }

    enum class KotlinFormatterKind {
        IDEA_DEFAULT,
        IDEA_CUSTOM,
        IDEA_KOTLIN_WITH_CUSTOM,
        IDEA_KOTLIN,

        PROJECT_DEFAULT,
        PROJECT_CUSTOM,
        PROJECT_KOTLIN_WITH_CUSTOM,
        PROJECT_KOTLIN,

        IDEA_OFFICIAL_DEFAULT,
        IDEA_OBSOLETE_KOTLIN,
        IDEA_OFFICIAL_KOTLIN_WITH_CUSTOM,
        PROJECT_OFFICIAL_DEFAULT,
        PROJECT_OBSOLETE_KOTLIN,
        PROJECT_OBSOLETE_KOTLIN_WITH_CUSTOM
    }
}