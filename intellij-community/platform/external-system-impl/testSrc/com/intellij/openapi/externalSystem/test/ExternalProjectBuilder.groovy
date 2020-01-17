/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.externalSystem.test

import com.intellij.externalSystem.JavaProjectData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.roots.DependencyScope
import com.intellij.util.BooleanFunction
import org.jetbrains.annotations.NotNull

import static com.intellij.openapi.externalSystem.test.ExternalSystemTestUtil.TEST_EXTERNAL_SYSTEM_ID
/**
 * @author Denis Zhdanov
 */
class ExternalProjectBuilder extends BuilderSupport {
  
  File projectDir
  DataNode<ProjectData> projectNode
  
  @Override
  protected void setParent(Object parent, Object child) {
  }

  @Override
  protected Object createNode(Object name) {
    createNode(name, [:])
  }

  @Override
  protected Object createNode(Object name, Object value) {
    createNode(name, [name: value])
  }

  @Override
  protected Object createNode(Object name, Map attributes, Object value) {
    createNode(name, [name: value] + attributes)
  }

  @Override
  protected Object createNode(Object name, Map attributes) {
    def projectPath = ExternalSystemApiUtil.normalizePath(projectDir.path)
    switch (name) {
      case 'project':
        ProjectSystemId projectSystemId = attributes.projectSystemId ?: TEST_EXTERNAL_SYSTEM_ID
        String externalProjectPath = attributes.projectPath ?: projectPath
        ProjectData projectData = new ProjectData(projectSystemId, attributes.name ?: 'project', projectPath, externalProjectPath)
        projectNode = new DataNode<ProjectData>(ProjectKeys.PROJECT, projectData, null)
        return projectNode
      case 'javaProject':
        ProjectSystemId projectSystemId = attributes.projectSystemId ?: TEST_EXTERNAL_SYSTEM_ID
        String jdk = attributes.jdk ?: ""
        String languageLevel = attributes.languageLevel ?: ""
        JavaProjectData javaProjectData = new JavaProjectData(projectSystemId, "")
        javaProjectData.setJdkVersion(jdk)
        javaProjectData.setLanguageLevel(languageLevel)
        return (current as DataNode).createChild(JavaProjectData.KEY, javaProjectData)
      case 'module':
        ProjectSystemId projectSystemId = attributes.projectSystemId ?: TEST_EXTERNAL_SYSTEM_ID
        String moduleFilePath = attributes.moduleFilePath ?: projectPath
        String externalConfigPath = attributes.externalConfigPath ?: projectPath
        ModuleData moduleData = new ModuleData(attributes.name ?: name as String,
                                               projectSystemId,
                                               ModuleTypeId.JAVA_MODULE,
                                               attributes.name ?: name as String,
                                               moduleFilePath,
                                               externalConfigPath)
        return (current as DataNode).createChild(ProjectKeys.MODULE, moduleData)
      case 'lib':
        DataNode<ModuleData> parentNode = current as DataNode
        LibraryDependencyData data = new LibraryDependencyData(parentNode.data,
                                                               getLibrary(attributes.name, attributes),
                                                               getLevel(attributes))
        data.scope = getScope(attributes)
        return parentNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, data)
      case 'extraModel':
        DataNode<ModuleData> parentNode = current as DataNode
        return parentNode.createChild(attributes.key, attributes.model)
      case 'task':
        DataNode<ExternalConfigPathAware> parentNode = current as DataNode
        ProjectSystemId projectSystemId = attributes.projectSystemId ?: TEST_EXTERNAL_SYSTEM_ID
        TaskData data = new TaskData(projectSystemId, attributes.name, parentNode.data.linkedExternalProjectPath, null)
        return parentNode.createChild(ProjectKeys.TASK, data)
      case 'contentRoot':
        DataNode<ModuleData> parentNode = current as DataNode
        ContentRootData data = new ContentRootData(TEST_EXTERNAL_SYSTEM_ID, attributes.name)
        return parentNode.createChild(ProjectKeys.CONTENT_ROOT, data)
      case 'folder':
        DataNode<ContentRootData> parentNode = current as DataNode
        ContentRootData data = parentNode.data
        data.storePath(attributes.type, attributes.path, attributes.packagePrefix)
        return null
        
      default: throw new IllegalArgumentException("Unexpected entry: $name")
    }
  }

  @NotNull
  private LibraryData getLibrary(@NotNull String name, @NotNull Map attributes) {
    DataNode<LibraryData> existing = ExternalSystemApiUtil.find(projectNode, ProjectKeys.LIBRARY, {
      DataNode<LibraryData> node -> node.data.externalName == name
    } as BooleanFunction)
    if (existing != null) {
      return existing.data
    }
    LibraryData result = new LibraryData(TEST_EXTERNAL_SYSTEM_ID, name, attributes.unresolved as boolean)
    ['bin': LibraryPathType.BINARY, 'src': LibraryPathType.SOURCE, 'doc': LibraryPathType.DOC].each {
      key, type -> attributes[key]?.each { result.addPath(type, it as String) }
    }
    if (attributes.level != 'module') {
      projectNode.createChild(ProjectKeys.LIBRARY, result)
    }
    result
  }

  @NotNull
  private static LibraryLevel getLevel(@NotNull Map attributes) {
    try {
      return LibraryLevel.valueOf(attributes.level.toUpperCase())
    }
    catch (Exception ignored) {
      return LibraryLevel.PROJECT
    }
  }
  
  @NotNull
  private static DependencyScope getScope(@NotNull Map attributes) {
    try {
      return DependencyScope.valueOf(attributes.scope.toUpperCase())
    }
    catch (Exception ignored) {
      return DependencyScope.COMPILE
    }
  }
}
