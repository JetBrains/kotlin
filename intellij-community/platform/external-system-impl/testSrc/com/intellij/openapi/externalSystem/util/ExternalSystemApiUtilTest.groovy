// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.test.AbstractExternalSystemTest
import com.intellij.openapi.externalSystem.test.ExternalSystemTestUtil
import com.intellij.openapi.module.ModuleManager

class ExternalSystemApiUtilTest extends AbstractExternalSystemTest {
  private def systemId = ExternalSystemTestUtil.TEST_EXTERNAL_SYSTEM_ID

  void 'test find module data'() {
    def projectNode = buildExternalProjectInfo {
      project {
        module('root', moduleFilePath: 'root') {}
        module('main', moduleFilePath: 'main') {}
        module('test', moduleFilePath: 'test') {}
      }
    }
    applyProjectState([projectNode])
    def projectPath = ExternalSystemApiUtil.normalizePath(projectDir.path)
    def projectInfo = new InternalExternalProjectInfo(systemId, projectPath, projectNode)
    ExternalProjectsManagerImpl.getInstance(project).updateExternalProjectData(projectInfo)
    def moduleManager = ModuleManager.getInstance(project)
    def rootModule = moduleManager.findModuleByName('root')
    def rootModuleData = ExternalSystemApiUtil.findModuleData(rootModule, systemId)
    def mainModule = moduleManager.findModuleByName('main')
    def mainModuleData = ExternalSystemApiUtil.findModuleData(mainModule, systemId)
    def testModule = moduleManager.findModuleByName('test')
    def testModuleData = ExternalSystemApiUtil.findModuleData(testModule, systemId)
    assertEquals("root", rootModuleData.getData().externalName)
    assertEquals("main", mainModuleData.getData().externalName)
    assertEquals("test", testModuleData.getData().externalName)
  }
}
