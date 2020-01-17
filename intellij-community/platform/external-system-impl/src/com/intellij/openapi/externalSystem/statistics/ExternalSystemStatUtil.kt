// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.internal.statistic.IdeActivity
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.utils.getPluginInfo
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import java.util.function.Consumer

fun getAnonymizedSystemId(systemId: ProjectSystemId): String {
  val manager = ExternalSystemApiUtil.getManager(systemId) ?: return "undefined.system"
  return if (getPluginInfo(manager.javaClass).isDevelopedByJetBrains()) systemId.readableName else "third.party"
}

fun addExternalSystemId(data: FeatureUsageData,
                        systemId: ProjectSystemId?) {
  data.addData("system_id", systemId?.let { getAnonymizedSystemId(it) } ?: "undefined.system")
}

fun importActivityStarted(project: Project, externalSystemId: ProjectSystemId,
                          dataConsumer: Consumer<FeatureUsageData>): IdeActivity {
  return IdeActivity(project, "project.import").startedWithData(Consumer {
    addExternalSystemId(it, externalSystemId);
    dataConsumer.accept(it)
  })
}