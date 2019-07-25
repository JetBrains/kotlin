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
package com.intellij.openapi.externalSystem.service.project

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.test.AbstractExternalSystemTest
import com.intellij.openapi.externalSystem.test.ExternalSystemTestUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.util.ArrayUtil

import static com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.*
import static com.intellij.openapi.externalSystem.test.ExternalSystemTestCase.collectRootsInside

class ExternalProjectServiceTest extends AbstractExternalSystemTest {

  void 'test module names deduplication'() {
    DataNode<ProjectData> projectNode = buildExternalProjectInfo {
      project {
        module('root', externalConfigPath: 'root') {}
        module('root', externalConfigPath: 'root/1') {}
        module('root', externalConfigPath: 'root/2') {}
        module('root', externalConfigPath: 'root/3') {}
        module('root', externalConfigPath: 'another/root') {}
        module('root', externalConfigPath: 'another/notRoot') {}
        module('root', externalConfigPath: 'root/root/root') {}
        module('root', externalConfigPath: 'root/root/root/root') {}
        module('root', externalConfigPath: 'yetanother/root/root') {}
        module('group-root', externalConfigPath: 'root') {}
        module('group-root', externalConfigPath: 'root/group/root') {}
        module('group-root', externalConfigPath: 'root/my/group/root') {}
        module('group-root', externalConfigPath: 'root/my-group/root') {}
      }
    }

    def modelsProvider = new IdeModelsProviderImpl(project)
    applyProjectState([projectNode])
    def expectedNames = [
      'root', '1-root', '2-root', '3-root', 'another-root', 'notRoot-root', 'root-root', 'root-root-root', 'yetanother-root-root',
      'group-root', 'root-group-root', 'my-group-root', 'my-group-group-root'
    ]
    assertOrderedEquals(modelsProvider.getModules().collect { it.name }, expectedNames)

    // check reimport with the same data
    applyProjectState([projectNode])
    assertOrderedEquals(modelsProvider.getModules().collect { it.name }, expectedNames)
  }

  void 'test no duplicate library dependency is added on subsequent refresh when there is an unresolved library'() {
    DataNode<ProjectData> projectNode = buildExternalProjectInfo {
      project {
        module('module') {
          lib('lib1')
          lib('lib2', unresolved: true)
        }
      }
    }

    applyProjectState([projectNode, projectNode])

    def modelsProvider = new IdeModelsProviderImpl(project)
    def module = modelsProvider.findIdeModule('module')
    assertNotNull(module)

    def entries = modelsProvider.getOrderEntries(module)
    def dependencies = [:].withDefault { 0 }
    for (OrderEntry entry : entries) {
      if (entry instanceof LibraryOrderEntry) {
        def name = (entry as LibraryOrderEntry).libraryName
        dependencies[name]++
      }
    }
    ExternalSystemTestUtil.assertMapsEqual(['Test_external_system_id: lib1': 1, 'Test_external_system_id: lib2': 1], dependencies)
  }

  void 'test changes in a project layout (content roots) could be detected on Refresh'() {

    String rootPath = ExternalSystemApiUtil.toCanonicalPath(project.basePath)

    def contentRoots = [
      (TEST)    : ['src/test/resources', '/src/test/java', 'src/test/groovy'],
      (SOURCE)  : ['src/main/resources', 'src/main/java', 'src/main/groovy'],
      (EXCLUDED): ['.gradle', 'build']
    ]

    def projectBaseFile = new File(project.basePath)
    (contentRoots[TEST] + contentRoots[SOURCE]).forEach {
      FileUtil.createDirectory(new File(projectBaseFile, it))
    }

    def projectRootBuilder = {
      buildExternalProjectInfo {
        project {
          module {
            contentRoot(rootPath) {
              contentRoots.each { key, values -> values.each { folder(type: key, path: "$rootPath/$it") } }
            }
          }
        }
      }
    }

    DataNode<ProjectData> projectNodeInitial = projectRootBuilder()

    contentRoots[(SOURCE)].remove(0)
    contentRoots[(TEST)].remove(0)
    DataNode<ProjectData> projectNodeRefreshed = projectRootBuilder()

    applyProjectState([projectNodeInitial, projectNodeRefreshed])

    def modelsProvider = new IdeModelsProviderImpl(project)
    def module = modelsProvider.findIdeModule('module')
    assertNotNull(module)
    def entries = modelsProvider.getOrderEntries(module)
    def folders = [:].withDefault { 0 }
    for (OrderEntry entry : entries) {
      if (entry instanceof ModuleSourceOrderEntry) {
        def contentEntry = (entry as ModuleSourceOrderEntry).getRootModel().getContentEntries().first()
        folders['source'] += contentEntry.sourceFolders.length
        folders['excluded'] += contentEntry.excludeFolders.length
      }
    }
    ExternalSystemTestUtil.assertMapsEqual(['source': 4, 'excluded': 2], folders)
  }

  void 'test library dependency with sources path added on subsequent refresh'() {

    def libBinPath = new File(projectDir, "bin_path")
    def libSrcPath = new File(projectDir, "source_path")
    def libDocPath = new File(projectDir, "doc_path")

    FileUtil.createDirectory(libBinPath)
    FileUtil.createDirectory(libSrcPath)
    FileUtil.createDirectory(libDocPath)

    applyProjectState([
      buildExternalProjectInfo {
        project {
          module('module') {
            lib('lib1', level: 'module', bin: [libBinPath.absolutePath])
          }
        }
      },
      buildExternalProjectInfo {
        project {
          module('module') {
            lib('lib1', level: 'module', bin: [libBinPath.absolutePath], src: [libSrcPath.absolutePath])
          }
        }
      },
      buildExternalProjectInfo {
        project {
          module('module') {
            lib('lib1', level: 'module', bin: [libBinPath.absolutePath], src: [libSrcPath.absolutePath], doc: [libDocPath.absolutePath])
          }
        }
      }
    ])

    def modelsProvider = new IdeModelsProviderImpl(project)
    def module = modelsProvider.findIdeModule('module')
    assertNotNull(module)

    def entries = modelsProvider.getOrderEntries(module)
    def dependencies = [:].withDefault { 0 }
    entries.each { OrderEntry entry ->
      if (entry instanceof LibraryOrderEntry) {
        def name = (entry as LibraryOrderEntry).libraryName
        dependencies[name]++
        if ("Test_external_system_id: lib1".equals(name)) {
          def classesUrls = entry.getUrls(OrderRootType.CLASSES)
          assertEquals(1, classesUrls.length)
          assertTrue(classesUrls[0].endsWith("bin_path"))
          def sourceUrls = entry.getUrls(OrderRootType.SOURCES)
          assertEquals(1, sourceUrls.length)
          assertTrue(sourceUrls[0].endsWith("source_path"))
          def docUrls = entry.getUrls(JavadocOrderRootType.instance)
          assertEquals(1, docUrls.length)
          assertTrue(docUrls[0].endsWith("doc_path"))
        }
        else {
          fail()
        }
      }
    }
    ExternalSystemTestUtil.assertMapsEqual(['Test_external_system_id: lib1': 1], dependencies)
  }

  void 'test excluded directories merge'() {
    String rootPath = ExternalSystemApiUtil.toCanonicalPath(project.basePath)
    def contentRoots = [
      (EXCLUDED): ['.gradle', 'build']
    ]

    def projectRootBuilder = {
      buildExternalProjectInfo {
        project {
          module {
            contentRoot(rootPath) {
              contentRoots.each { key, values -> values.each { folder(type: key, path: "$rootPath/$it") } }
            }
          }
        }
      }
    }

    DataNode<ProjectData> projectNodeInitial = projectRootBuilder()

    contentRoots[(EXCLUDED)].remove(0)
    contentRoots[(EXCLUDED)].add("newExclDir")

    DataNode<ProjectData> projectNodeRefreshed = projectRootBuilder()
    applyProjectState([projectNodeInitial, projectNodeRefreshed])

    def modelsProvider = new IdeModelsProviderImpl(project)
    def module = modelsProvider.findIdeModule('module')
    assertNotNull(module)
    def folders = []
    for (OrderEntry entry : modelsProvider.getOrderEntries(module)) {
      if (entry instanceof ModuleSourceOrderEntry) {
        def contentEntry = (entry as ModuleSourceOrderEntry).getRootModel().getContentEntries().first()
        folders = contentEntry.excludeFolders.collect { new File(it.url).name }
      }
    }
    assertEquals(new HashSet<>(folders), new HashSet<>([".gradle", "build", "newExclDir"]))
  }

  void 'test project SDK configuration import'() {
    String myJdkName = "My JDK"
    String myJdkHome = IdeaTestUtil.requireRealJdkHome()

    List<String> allowedRoots = new ArrayList<String>()
    allowedRoots.add(myJdkHome)
    allowedRoots.addAll(collectRootsInside(myJdkHome))
    VfsRootAccess.allowRootAccess(testRootDisposable, ArrayUtil.toStringArray(allowedRoots))

    WriteAction.run {
      Sdk oldJdk = ProjectJdkTable.getInstance().findJdk(myJdkName)
      if (oldJdk != null) {
        ProjectJdkTable.getInstance().removeJdk(oldJdk)
      }
      VirtualFile jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(myJdkHome))
      Sdk jdk = SdkConfigurationUtil.setupSdk(new Sdk[0], jdkHomeDir, JavaSdk.getInstance(), true, null, myJdkName)
      assertNotNull("Cannot create JDK for " + myJdkHome, jdk)
      ProjectJdkTable.getInstance().addJdk(jdk, testFixture.project)
    }

    DataNode<ProjectData> projectNode = buildExternalProjectInfo {
      project {
        javaProject(jdk: '1.7', languageLevel: '1.7') {
        }
      }
    }

    applyProjectState([projectNode])

    ProjectRootManager rootManager = ProjectRootManager.getInstance(project)
    Sdk sdk = rootManager.getProjectSdk()
    assertNotNull(sdk)
    LanguageLevelProjectExtension languageLevelExtension = LanguageLevelProjectExtension.getInstance(project)
    assertEquals(LanguageLevel.JDK_1_7, languageLevelExtension.languageLevel)
  }
}
