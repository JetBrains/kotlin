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
        module('root', moduleFilePath: 'root') {
          module('main', moduleFilePath: 'main') {}
          module('test', moduleFilePath: 'test') {}
        }
      }
    }
    applyProjectState([projectNode])
    def projectPath = ExternalSystemApiUtil.normalizePath(projectDir.path)
    def projectInfo = new InternalExternalProjectInfo(systemId, projectPath, projectNode)
    ExternalProjectsManagerImpl.getInstance(project).updateExternalProjectData(projectInfo)
    assertModuleData(root: 'root', main: 'main', test: 'test')
  }

  private def assertModuleData(Map<String, String> modules) {
    def moduleManager = ModuleManager.getInstance(project)
    for (def expectation : modules.entrySet()) {
      def name = expectation.key
      def path = expectation.value
      def module = moduleManager.findModuleByName(name)
      def moduleData = ExternalSystemApiUtil.findModuleData(module, systemId)
      assertNotNull("Module $name isn't exist", moduleData)
      assertEquals(path, moduleData.getData().moduleFileDirectoryPath)
    }
  }
}
