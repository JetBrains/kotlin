/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.idea.codeInsight.codevision

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.settings.InlayHintsConfigurable
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.openapi.project.Project
import com.intellij.ui.layout.panel
import org.jetbrains.kotlin.idea.KotlinBundle
import javax.swing.JPanel


// FIX ME WHEN BUNCH 192 REMOVED
typealias CodeVisionInlayHintsConfigurable = InlayHintsConfigurable

fun logUsageStatistics(project: Project?, groupId: String, eventId: String) =
    FUCounterUsageLogger.getInstance().logEvent(project, groupId, eventId)

fun logUsageStatistics(project: Project?, groupId: String, eventId: String, data: FeatureUsageData) =
    FUCounterUsageLogger.getInstance().logEvent(project, groupId, eventId, data)

fun createImmediateConfigurable(settings: KotlinCodeVisionProvider.KotlinCodeVisionSettings): ImmediateConfigurable {
    return object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JPanel {
            return panel {}
        }

        override val cases: List<ImmediateConfigurable.Case>
            get() = listOf(
                ImmediateConfigurable.Case(
                    KotlinBundle.message("hints.title.codevision.usages"),
                    "usages",
                    settings::showUsages
                ),
                ImmediateConfigurable.Case(
                    KotlinBundle.message("hints.title.codevision.inheritors"),
                    "inheritors",
                    settings::showInheritors
                )
            )

        override val mainCheckboxText: String
            get() = KotlinBundle.message("hints.title.codevision.show.hints.for")
    }
}