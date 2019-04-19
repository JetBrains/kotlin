// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.ide.projectWizard.NewProjectWizardTestCase;
import com.intellij.ide.projectWizard.ProjectTypeStep;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage;
import com.intellij.openapi.externalSystem.service.ui.SelectExternalProjectDialog;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.view.ProjectNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.RunAll;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleWizardStep;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.openapi.externalSystem.test.ExternalSystemTestCase.collectRootsInside;

/**
 * @author Dmitry Avdeev
 */
public class GradleProjectWizardTest extends NewProjectWizardTestCase {

  protected static final String GRADLE_JDK_NAME = "Gradle JDK";
  private String myJdkHome;

  public void testGradleProject() throws Exception {
    final String projectName = "testProject";
    ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable()).subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(@NotNull Project project) {
        assertNotEmpty(ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID));
        // project save is not called in unit mode, see com.intellij.ide.impl.NewProjectUtil.doCreate
        ExternalProjectsDataStorage.getInstance(project).doSave();
      }
    });

    Project project = createProject(step -> {
      if (step instanceof ProjectTypeStep) {
        assertTrue(((ProjectTypeStep)step).setSelectedTemplate("Gradle", null));
        List<ModuleWizardStep> steps = myWizard.getSequence().getSelectedSteps();
        assertEquals(5, steps.size());
        final ProjectBuilder projectBuilder = myWizard.getProjectBuilder();
        assertInstanceOf(projectBuilder, GradleModuleBuilder.class);
        ((GradleModuleBuilder)projectBuilder).setName(projectName);
      }
    });

    assertEquals(projectName, project.getName());
    Module[] modules = ModuleManager.getInstance(project).getModules();
    assertEquals(1, modules.length);
    final Module module = modules[0];
    assertTrue(ModuleRootManager.getInstance(module).isSdkInherited());
    assertEquals(projectName, module.getName());

    VirtualFile root = ProjectRootManager.getInstance(project).getContentRoots()[0];
    VirtualFile settingsScript = VfsUtilCore.findRelativeFile("settings.gradle", root);
    assertNotNull(settingsScript);
    assertEquals(String.format("rootProject.name = '%s'\n\n", projectName),
                 StringUtil.convertLineSeparators(VfsUtilCore.loadText(settingsScript)));

    VirtualFile buildScript = VfsUtilCore.findRelativeFile("build.gradle", root);
    assertNotNull(buildScript);
    assertEquals("plugins {\n" +
                 "    id 'java'\n" +
                 "}\n\n" +
                 "version '1.0-SNAPSHOT'\n" +
                 "\n" +
                 "sourceCompatibility = 1.8\n" +
                 "\n" +
                 "repositories {\n" +
                 "    mavenCentral()\n" +
                 "}\n" +
                 "\n" +
                 "dependencies {\n" +
                 "    testCompile group: 'junit', name: 'junit', version: '4.12'\n" +
                 "}\n",
                 StringUtil.convertLineSeparators(VfsUtilCore.loadText(buildScript)));

    Module childModule = createModuleFromTemplate("Gradle", null, project, step -> {
      if (step instanceof ProjectTypeStep) {
        List<ModuleWizardStep> steps = myWizard.getSequence().getSelectedSteps();
        assertEquals(5, steps.size());
      }
      else if (step instanceof GradleModuleWizardStep) {
        SelectExternalProjectDialog projectDialog = new SelectExternalProjectDialog(GradleConstants.SYSTEM_ID, project, null);
        Disposer.register(getTestRootDisposable(), projectDialog.getDisposable());
        JComponent component = projectDialog.getPreferredFocusedComponent();
        ProjectNode projectNode = (ProjectNode)((SimpleTree)component).getNodeFor(0);
        assertEquals(projectName, projectNode.getName());
        ((GradleModuleWizardStep)step).setArtifactId("childModule");
      }
    });

    modules = ModuleManager.getInstance(project).getModules();
    assertEquals(2, modules.length);

    assertEquals("childModule", childModule.getName());
    assertEquals(String.format("rootProject.name = '%s'\n" +
                               "include '%s'\n\n", projectName, childModule.getName()),
                 StringUtil.convertLineSeparators(VfsUtilCore.loadText(settingsScript)));
  }

  @Override
  protected Project createProject(Consumer adjuster) throws IOException {
    @SuppressWarnings("unchecked") Project project = super.createProject(adjuster);
    myFilesToDelete.add(ProjectUtil.getExternalConfigurationDir(project).toFile());
    return project;
  }

  @Override
  protected void createWizard(@Nullable Project project) throws IOException {
    Collection linkedProjectsSettings = project == null
                                        ? ContainerUtil.emptyList()
                                        : ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).getLinkedProjectsSettings();
    assertTrue(linkedProjectsSettings.size() <= 1);
    File directory;
    Object settings = ContainerUtil.getFirstItem(linkedProjectsSettings);
    if (settings instanceof ExternalProjectSettings) {
      directory =
        FileUtil.createTempDirectory(new File(((ExternalProjectSettings)settings).getExternalProjectPath()), getName(), "new", false);
    }
    else {
      directory = FileUtil.createTempDirectory(getName(), "new", false);
    }
    myFilesToDelete.add(directory);
    if (myWizard != null) {
      Disposer.dispose(myWizard.getDisposable());
      myWizard = null;
    }
    myWizard = createWizard(project, directory);
    UIUtil.dispatchAllInvocationEvents(); // to make default selection applied
  }

  protected void collectAllowedRoots(final List<String> roots) {
    roots.add(myJdkHome);
    roots.addAll(collectRootsInside(myJdkHome));
    roots.add(PathManager.getConfigPath());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myJdkHome = IdeaTestUtil.requireRealJdkHome();
    List<String> allowedRoots = new ArrayList<>();
    collectAllowedRoots(allowedRoots);
    if (!allowedRoots.isEmpty()) {
      VfsRootAccess.allowRootAccess(getTestRootDisposable(), ArrayUtil.toStringArray(allowedRoots));
    }
    WriteAction.runAndWait(() -> {
      Sdk oldJdk = ProjectJdkTable.getInstance().findJdk(GRADLE_JDK_NAME);
      if (oldJdk != null) {
        ProjectJdkTable.getInstance().removeJdk(oldJdk);
      }
      VirtualFile jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(myJdkHome));
      Sdk jdk = SdkConfigurationUtil.setupSdk(new Sdk[0], jdkHomeDir, SimpleJavaSdkType.getInstance(), true, null, GRADLE_JDK_NAME);
      assertNotNull("Cannot create JDK for " + myJdkHome, jdk);
      ProjectJdkTable.getInstance().addJdk(jdk);
    });
  }

  @Override
  public void tearDown() {
    new RunAll(
      () -> {
        if (myJdkHome != null) {
          Sdk jdk = ProjectJdkTable.getInstance().findJdk(GRADLE_JDK_NAME);
          if (jdk != null) {
            WriteAction.runAndWait(() -> ProjectJdkTable.getInstance().removeJdk(jdk));
          }
        }
      },
      () -> super.tearDown()
    ).run();
  }
}
