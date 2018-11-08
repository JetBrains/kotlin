/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.usagestats

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsageTriggerCollector
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsageTriggerCollector
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

private val COMMON_GROUP_ID = "statistics.kotlin.ide"

abstract class KotlinProjectScopeStatsReporter(private val statsName: String) : ProjectUsagesCollector() {

    override fun getGroupId() = "$COMMON_GROUP_ID.$statsName"

}

class GradleLibrariesStatsReporter : KotlinProjectScopeStatsReporter("libraries") {
    override fun getUsages(p0: Project): MutableSet<UsageDescriptor> =
        LibraryTablesRegistrar
            .getInstance()
            .getLibraryTable(p0)
            .libraries
            .map { UsageDescriptor(it.name ?: "unknown", 1) }
            .toMutableSet()
}

abstract class KotlinStatsTriggerReporter(val statsName: String) : ApplicationUsageTriggerCollector() {
    override fun getGroupId() = "$COMMON_GROUP_ID.$statsName"
}

class KotlinVersionStatsReporter : KotlinStatsTriggerReporter("kotlin_version")

class GradlePluginsStatsReporter : KotlinStatsTriggerReporter("gradle_plugins")