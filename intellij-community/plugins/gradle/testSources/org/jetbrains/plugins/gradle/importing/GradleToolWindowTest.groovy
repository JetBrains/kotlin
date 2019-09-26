// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.view.ExternalProjectsViewImpl
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.ProjectNode
import com.intellij.openapi.wm.impl.ToolWindowHeadlessManagerImpl
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Test

class GradleToolWindowTest extends GradleImportingTestCase {
  private ToolWindowHeadlessManagerImpl.MockToolWindow toolWindow
  private ExternalProjectsViewImpl view

  @Override
  void setUp() throws Exception {
    super.setUp()
    toolWindow = new ToolWindowHeadlessManagerImpl.MockToolWindow(myProject)
    view = new ExternalProjectsViewImpl(myProject, toolWindow, getExternalSystemId())
    ExternalProjectsManagerImpl.getInstance(myProject).registerView(view)
    view.initStructure()
  }

  @Test
  @TargetVersions("5.0")
  void testSimpleBuild() {
    createSettingsFile("""
rootProject.name='rooot'
include ':child1'
include ':child2'"""
    )

    createProjectSubFile "build.gradle", """
group 'test'
version '1.0-SNAPSHOT'
"""

    createProjectSubFile "../child1/build.gradle", """
group 'test'
version '1.0-SNAPSHOT'
"""
    createProjectSubFile "../child2/build.gradle", """
group 'test'
version '1.0-SNAPSHOT'
"""

    doTest()
  }

  @Test
  @TargetVersions("5.0")
  void testSimpleBuildWithoutGrouping() {
    createSettingsFile("""
rootProject.name='rooot'
include ':child1'
include ':child2'"""
    )

    createProjectSubFile "build.gradle", """
group 'test'
version '1.0-SNAPSHOT'
"""

    createProjectSubFile "../child1/build.gradle", """
group 'test'
version '1.0-SNAPSHOT'
"""
    createProjectSubFile "../child2/build.gradle", """
group 'test'
version '1.0-SNAPSHOT'
"""
    view.setGroupModules(false)
    doTest()
  }

  @Test
  @TargetVersions("5.0")
  void testBasicCompositeBuild() throws Exception {
    createSettingsFile("""
rootProject.name='adhoc'
includeBuild '../my-app'
includeBuild '../my-utils'"""
    )

    createProjectSubFile("../my-app/settings.gradle", "rootProject.name = 'my-app'\n")
    createProjectSubFile("../my-app/build.gradle",
                         """
apply plugin: 'java'
group 'org.sample'
version '1.0'

dependencies {
  compile 'org.sample:number-utils:1.0'
  compile 'org.sample:string-utils:1.0'
}
""")

    createProjectSubFile("../my-utils/settings.gradle",
                         """
rootProject.name = 'my-utils'
include 'number-utils', 'string-utils' """
    )

    createProjectSubFile("../my-utils/build.gradle",
                         injectRepo(
                           """
subprojects {
  apply plugin: 'java'

  group 'org.sample'
  version '1.0'
}

project(':string-utils') {
  dependencies {
    compile 'org.apache.commons:commons-lang3:3.4'
  }
} """
                         )
    )

    doTest()
  }

  def doTest() {
    importProject()
    checkToolWindowState()
  }

  private void checkToolWindowState() {
    def data = ProjectDataManager.getInstance().getExternalProjectsData(myProject, getExternalSystemId()).collect {
      it.externalProjectStructure
    }

    def rootNodes = data.collect {
      new ProjectNode(view, it)
    }

    def tree = buildTree("View root", rootNodes, null)
    def sw = new StringWriter()
    def writer = new PrintWriter(sw)
    tree.print(writer)

    assertSameLinesWithFile(getPath(), sw.toString())
  }

  private String getPath() {
    def communityPath = PlatformTestUtil.getCommunityPath().replace(File.separatorChar, '/'.charAt(0))
    def testName = getTestName(true)
    testName = testName.substring(0, testName.indexOf("_"))
    testName = getTestName(testName, true)
    return "$communityPath/plugins/gradle/testData/toolWindow/${testName}.test"
  }

  static Node buildTree(String name, List<ExternalSystemNode<?>> nodes, Node parent) {
    def node = new Node(parent, name)
    nodes.each {
      buildTree(it.name, it.children.toList(), node)
    }
    return node
  }
}
