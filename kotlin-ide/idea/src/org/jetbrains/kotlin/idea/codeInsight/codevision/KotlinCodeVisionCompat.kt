/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.codevision

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.openapi.project.Project
import com.intellij.ui.layout.*
import javax.swing.JPanel


fun logUsageStatistics(project: Project?, groupId: String, eventId: String) =
    FUCounterUsageLogger.getInstance().logEvent(project, groupId, eventId)

fun logUsageStatistics(project: Project?, groupId: String, eventId: String, data: FeatureUsageData) =
    FUCounterUsageLogger.getInstance().logEvent(project, groupId, eventId, data)

fun createImmediateConfigurable(): ImmediateConfigurable {
    return object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JPanel = panel {}

        override val cases: List<ImmediateConfigurable.Case> = emptyList()

        override val mainCheckboxText: String = ""
    }
}