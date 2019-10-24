// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.externalSystem.importing.ImportSpec
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.view.ExternalProjectsViewImpl
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.ProjectNode
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.ToolWindowHeadlessManagerImpl
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.PsiTestUtil
import org.junit.Test
import org.junit.runners.Parameterized

class GradleToolWindowTest extends GradleImportingTestCase {
  ToolWindowHeadlessManagerImpl.MockToolWindow toolWindow
  ExternalProjectsViewImpl view
  boolean isPreview

  @Parameterized.Parameters(name = "with Gradle-{0}")
  static Collection<Object[]> data() {
    return [["5.0"] as Object[]]
  }

  @Override
  void setUp() throws Exception {
    super.setUp()
    toolWindow = new ToolWindowHeadlessManagerImpl.MockToolWindow(myProject)
    view = new ExternalProjectsViewImpl(myProject, toolWindow, getExternalSystemId())
    ExternalProjectsManagerImpl.getInstance(myProject).registerView(view)
    view.initStructure()
    isPreview = false
  }

  @Test
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
  void testDotInModuleName() {
    createSettingsFile("""
rootProject.name='rooot.dot'
include ':child1'
include ':child2'
include ':child2:dot.child'
"""
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

    createProjectSubFile "../child2/dot.child/build.gradle", """
group 'test'
version '1.0-SNAPSHOT'
"""

    doTest()
  }

  @Test
  void testBuildSrc() {
    createSettingsFile("""
rootProject.name='rooot'
include ':child1'
"""
    )

    createProjectSubFile "build.gradle", """
group 'test'
version '1.0-SNAPSHOT'
"""

    createProjectSubFile "../child1/build.gradle", """
group 'test'
version '1.0-SNAPSHOT'
def foo = new testBuildSrcClassesUsages.BuildSrcClass().sayHello()
"""

    createProjectSubFile "buildSrc/src/main/groovy/testBuildSrcClassesUsages/BuildSrcClass.groovy", """
package testBuildSrcClassesUsages;
public class BuildSrcClass {   
  public String sayHello() { 'Hello!' }
}
"""

    doTest()
  }


  @Test
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
  void testWithExistedRootModule() {
    createMainModule("project")

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
    isPreview = true
    importProject()
    isPreview = false

    doTest()
    assert ModuleManager.getInstance(myProject).getModules().length == 3
  }

  @Test
  void testWithExistedRootModuleWithoutPreviewImport() {
    createMainModule("project")

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
    assert ModuleManager.getInstance(myProject).getModules().length == 3
  }

  Module createMainModule(String name) {
    Module module = null
    WriteAction.runAndWait {
      VirtualFile f = createProjectSubFile(name + ".iml")
      module = ModuleManager.getInstance(myProject).newModule(f.getPath(), JavaModuleType.getModuleType().getName())
      PsiTestUtil.addContentRoot(module, f.getParent())
    }
    return module
  }

  @Test
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

  @Test
  void testDuplicatingProjectLeafNames() {
    createSettingsFile("""
rootProject.name = 'rootProject'
include 'p1', 'p2', 'p1:sub:sp1', 'p2:p2sub:sub:sp2'
include 'p1:leaf', 'p2:leaf'
""")

    doTest()
  }

  @Override
  protected ImportSpec createImportSpec() {
    ImportSpecBuilder importSpecBuilder = new ImportSpecBuilder(super.createImportSpec())
    if (isPreview) {
      importSpecBuilder.usePreviewMode()
    }
    return importSpecBuilder.build()
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

    String path = getPath()
    assert new File(path).exists(), "File $path doesn't exist"

    assertSameLinesWithFile(path, sw.toString())
  }

  protected String getPath() {
    def communityPath = PlatformTestUtil.getCommunityPath().replace(File.separatorChar, '/'.charAt(0))
    def testName = getTestName(true)
    testName = testName.substring(0, testName.indexOf("_"))
    testName = getTestName(testName, true)
    return "$communityPath/plugins/gradle/java/testData/toolWindow/${testName}.test"
  }

  static Node buildTree(String name, List<ExternalSystemNode<?>> nodes, Node parent) {
    def node = new Node(parent, name)
    nodes.each {
      buildTree(it.name, it.children.toList(), node)
    }
    return node
  }
}
