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
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.idea.formatter.KotlinFormatterUsageCollector.KotlinFormatterKind.*

class KotlinFormatterUsageCollector : ProjectUsagesCollector() {
    override fun getGroupId() = "kotlin.ide.formatter"
    override fun getVersion(): Int = 2

    override fun getMetrics(project: Project): Set<MetricEvent> {
        val usedFormatter = getKotlinFormatterKind(project)

        val data = FeatureUsageData().addData("kind", usedFormatter.name)

        return setOf(newMetric("settings", data))
    }

    companion object {
        private val KOTLIN_OFFICIAL_CODE_STYLE: CodeStyleSettings by lazy {
            CodeStyleSettingsManager.getInstance().cloneSettings(CodeStyle.getDefaultSettings()).also(KotlinStyleGuideCodeStyle::apply)
        }

        private val KOTLIN_OBSOLETE_CODE_STYLE: CodeStyleSettings by lazy {
            CodeStyleSettingsManager.getInstance().cloneSettings(CodeStyle.getDefaultSettings()).also(KotlinObsoleteCodeStyle::apply)
        }

        private fun codeStylesIsEquals(lhs: CodeStyleSettings, rhs: CodeStyleSettings): Boolean =
            lhs.kotlinCustomSettings == rhs.kotlinCustomSettings && lhs.kotlinCommonSettings == rhs.kotlinCommonSettings

        fun getKotlinFormatterKind(project: Project): KotlinFormatterKind {
            val isProject = CodeStyle.usesOwnSettings(project)
            val currentSettings = CodeStyle.getSettings(project)

            return when (currentSettings.kotlinCodeStyleDefaults()) {
                KotlinStyleGuideCodeStyle.CODE_STYLE_ID -> {
                    if (codeStylesIsEquals(currentSettings, KOTLIN_OFFICIAL_CODE_STYLE))
                        paired(IDEA_OFFICIAL_KOTLIN, isProject)
                    else
                        paired(IDEA_OFFICIAL_KOTLIN_WITH_CUSTOM, isProject)
                }

                KotlinObsoleteCodeStyle.CODE_STYLE_ID -> {
                    if (codeStylesIsEquals(currentSettings, KOTLIN_OBSOLETE_CODE_STYLE))
                        paired(IDEA_OBSOLETE_KOTLIN, isProject)
                    else
                        paired(IDEA_OBSOLETE_KOTLIN_WITH_CUSTOM, isProject)
                }

                else -> paired(IDEA_CUSTOM, isProject)
            }
        }

        private fun paired(kind: KotlinFormatterKind, isProject: Boolean): KotlinFormatterKind {
            if (!isProject) return kind

            return when (kind) {
                IDEA_CUSTOM -> PROJECT_CUSTOM
                IDEA_OFFICIAL_KOTLIN -> PROJECT_OFFICIAL_KOTLIN
                IDEA_OFFICIAL_KOTLIN_WITH_CUSTOM -> PROJECT_OFFICIAL_KOTLIN_WITH_CUSTOM
                IDEA_OBSOLETE_KOTLIN -> PROJECT_OBSOLETE_KOTLIN
                IDEA_OBSOLETE_KOTLIN_WITH_CUSTOM -> PROJECT_OBSOLETE_KOTLIN_WITH_CUSTOM
                else -> kind
            }
        }
    }

    enum class KotlinFormatterKind {
        IDEA_CUSTOM, PROJECT_CUSTOM,

        IDEA_OFFICIAL_KOTLIN, PROJECT_OFFICIAL_KOTLIN,
        IDEA_OFFICIAL_KOTLIN_WITH_CUSTOM, PROJECT_OFFICIAL_KOTLIN_WITH_CUSTOM,

        IDEA_OBSOLETE_KOTLIN, PROJECT_OBSOLETE_KOTLIN,
        IDEA_OBSOLETE_KOTLIN_WITH_CUSTOM, PROJECT_OBSOLETE_KOTLIN_WITH_CUSTOM,
    }
}