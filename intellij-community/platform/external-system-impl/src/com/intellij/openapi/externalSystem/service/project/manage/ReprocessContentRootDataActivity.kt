// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class ReprocessContentRootDataActivity : StartupActivity.Background {
  private val LOG = Logger.getInstance(ReprocessContentRootDataActivity::class.java)

  override fun runActivity(project: Project) {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      return
    }
    if (ExternalSystemUtil.isNewProject(project)) {
      LOG.info("Ignored reprocess of content root data service for new projects")
      return
    }

    val instance = SourceFolderManager.getInstance(project) as SourceFolderManagerImpl
    instance.rescanAndUpdateSourceFolders()
  }
}
