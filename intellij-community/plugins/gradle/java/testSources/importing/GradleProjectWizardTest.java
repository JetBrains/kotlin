// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.ide.projectWizard.NewProjectWizardTestCase;
import com.intellij.ide.projectWizard.ProjectTypeStep;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
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
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.RunAll;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.wizard.AbstractGradleModuleBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleStructureWizardStep;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
        assertEquals(3, steps.size());
        final ProjectBuilder projectBuilder = myWizard.getProjectBuilder();
        assertInstanceOf(projectBuilder, AbstractGradleModuleBuilder.class);
        AbstractGradleModuleBuilder gradleProjectBuilder = (AbstractGradleModuleBuilder)projectBuilder;
        gradleProjectBuilder.setName(projectName);
        gradleProjectBuilder.setProjectId(new ProjectId("", null, null));
      }
    });
    CountDownLatch latch = new CountDownLatch(1);
    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(ProjectDataImportListener.TOPIC, path -> latch.countDown());
    while (latch.getCount() == 1) {
      UIUtil.invokeAndWaitIfNeeded((Runnable)() -> PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue());
      Thread.yield();
    }

    assertEquals(projectName, project.getName());
    assertModules(project, projectName, projectName + ".main", projectName + ".test");
    Module[] modules = ModuleManager.getInstance(project).getModules();
    final Module module = ContainerUtil.find(modules, it -> it.getName().equals(projectName));
    assertTrue(ModuleRootManager.getInstance(module).isSdkInherited());

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
        assertEquals(3, steps.size());
      }
      else if (step instanceof GradleStructureWizardStep) {
        GradleStructureWizardStep gradleStructureWizardStep = (GradleStructureWizardStep)step;
        assertEquals(projectName, gradleStructureWizardStep.getParentData().getExternalName());
        gradleStructureWizardStep.setArtifactId("childModule");
        gradleStructureWizardStep.setGroupId("");
      }
    });
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue());

    assertModules(project, projectName, projectName + ".main", projectName + ".test",
                  projectName + ".childModule", projectName + ".childModule.main", projectName + ".childModule.test");

    assertEquals("childModule", childModule.getName());
    assertEquals(String.format("rootProject.name = '%s'\n" +
                               "include '%s'\n\n", projectName, childModule.getName()),
                 StringUtil.convertLineSeparators(VfsUtilCore.loadText(settingsScript)));
  }

  private static void assertModules(@NotNull Project project, @NotNull String... expectedNames) {
    Module[] actual = ModuleManager.getInstance(project).getModules();
    Collection<String> actualNames = ContainerUtil.map(actual, it -> it.getName());
    assertEquals(ContainerUtil.newHashSet(expectedNames), new HashSet<>(actualNames));
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
      directory = new File(((ExternalProjectSettings)settings).getExternalProjectPath());
      FileUtil.createDirectory(directory);
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
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
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
      VfsRootAccess.allowRootAccess(getTestRootDisposable(), ArrayUtilRt.toStringArray(allowedRoots));
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
