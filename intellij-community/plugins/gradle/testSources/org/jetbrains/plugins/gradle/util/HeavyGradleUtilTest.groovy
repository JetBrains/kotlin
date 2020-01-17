// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.test.AbstractExternalSystemTest
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings

import static org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID

class HeavyGradleUtilTest extends AbstractExternalSystemTest {

  @SuppressWarnings(["GrUnresolvedAccess", "GroovyAssignabilityCheck"])
  void 'test find module data'() {
    def projectPath = ExternalSystemApiUtil.normalizePath(projectDir.path)
    def projectNode = buildExternalProjectInfo {
      project(projectPath: projectPath) {
        module('root', moduleFilePath: 'root', externalConfigPath: projectPath) {
          module('root.main', moduleFilePath: 'root/main', externalConfigPath: projectPath) {}
          module('root.test', moduleFilePath: 'root/test', externalConfigPath: projectPath) {}
        }
        module('module', moduleFilePath: 'module', externalConfigPath: "$projectPath/module") {
          module('module.main', moduleFilePath: 'module/main', externalConfigPath: "$projectPath/module") {}
          module('module.test', moduleFilePath: 'module/test', externalConfigPath: "$projectPath/module") {}
        }
      }
    }
    def linkedProjectNode = buildExternalProjectInfo {
      project(projectPath: "$projectPath/linked") {
        module('linked', moduleFilePath: 'linked', externalConfigPath: "$projectPath/linked") {
          module('linked.main', moduleFilePath: 'linked/main', externalConfigPath: "$projectPath/linked") {}
          module('linked.test', moduleFilePath: 'linked/test', externalConfigPath: "$projectPath/linked") {}
        }
      }
    }
    applyProjectState([projectNode, linkedProjectNode])
    assertGradleModuleData(
      root: 'root', 'root.main': 'root', 'root.test': 'root',
      module: 'module', 'module.main': 'module', 'module.test': 'module',
      linked: 'linked', 'linked.main': 'linked', 'linked.test': 'linked'
    )
  }

  @Override
  void applyProjectState(@NotNull List<DataNode<ProjectData>> projects) {
    def linkedProjectsSettings = new ArrayList()
    for (DataNode<ProjectData> node : projects) {
      def externalModulePaths = ExternalSystemApiUtil.findAll(node, ProjectKeys.MODULE)
        .collect { it.getData().getLinkedExternalProjectPath() }
        .toSet()
      def projectPath = node.data.linkedExternalProjectPath
      def projectSettings = new GradleProjectSettings()
      projectSettings.externalProjectPath = projectPath
      projectSettings.setModules(externalModulePaths)
      linkedProjectsSettings.add(projectSettings)
    }
    def settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
    settings.linkedProjectsSettings = linkedProjectsSettings
    def dataManager = ProjectDataManager.getInstance()
    for (DataNode<ProjectData> node : projects) {
      dataManager.importData(node, project, true)
    }
    def projectManager = ExternalProjectsManagerImpl.getInstance(project)
    for (DataNode<ProjectData> node : projects) {
      def projectPath = node.data.linkedExternalProjectPath
      def projectInfo = new InternalExternalProjectInfo(SYSTEM_ID, projectPath, node)
      projectManager.updateExternalProjectData(projectInfo)
    }
  }

  private def assertGradleModuleData(Map<String, String> modules) {
    def moduleManager = ModuleManager.getInstance(project)
    for (def expectation : modules.entrySet()) {
      def name = expectation.key
      def path = expectation.value
      def module = moduleManager.findModuleByName(name)
      assertNotNull("Module '$name' isn't exist", module)
      def moduleData = GradleUtil.findGradleModuleData(module)
      assertNotNull("Data of module '$name' isn't exist", moduleData)
      assertEquals(path, moduleData.getData().moduleFileDirectoryPath)
    }
  }
}
