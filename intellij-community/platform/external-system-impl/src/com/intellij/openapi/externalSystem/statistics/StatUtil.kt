// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.internal.statistic.utils.getPluginInfo
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil

fun getAnonymizedSystemId(systemId: ProjectSystemId): String {
  val manager = ExternalSystemApiUtil.getManager(systemId) ?: return "undefined.system"
  return if (getPluginInfo(manager.javaClass).isDevelopedByJetBrains()) systemId.readableName else "third.party"
}