// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.ide.projectWizard.NewProjectWizardTestCase;
import com.intellij.ide.projectWizard.ProjectTypeStep;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.environment.Environment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
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
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.wizard.AbstractGradleModuleBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleStructureWizardStep;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static com.intellij.openapi.externalSystem.test.ExternalSystemTestCase.collectRootsInside;

/**
 * @author Dmitry Avdeev
 */
public class GradleProjectWizardTest extends NewProjectWizardTestCase {
  private static final String GRADLE_JDK_NAME = "Gradle JDK";
  private final List<Sdk> removedSdks = new SmartList<>();
  private String myJdkHome;

  public void testGradleProject() throws Exception {
    final String projectName = "testProject";
    Project project = GradleCreateProjectTestCase.waitForProjectReload(true, () -> {
      return createProject(step -> {
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
    });

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

  private static void assertModules(@NotNull Project project, String @NotNull ... expectedNames) {
    Module[] actual = ModuleManager.getInstance(project).getModules();
    Collection<String> actualNames = ContainerUtil.map(actual, it -> it.getName());
    assertEquals(ContainerUtil.newHashSet(expectedNames), new HashSet<>(actualNames));
  }

  @Override
  protected Project createProject(Consumer adjuster) throws IOException {
    @SuppressWarnings("unchecked")
    Project project = super.createProject(adjuster);
    myFilesToDelete.add(ProjectUtil.getExternalConfigurationDir(project).toFile());
    return project;
  }

  @Override
  protected void createWizard(@Nullable Project project) throws IOException {
    Collection<?> linkedProjectsSettings = project == null
                                        ? Collections.emptyList()
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

  private void collectAllowedRoots(final List<String> roots) {
    roots.add(myJdkHome);
    roots.addAll(collectRootsInside(myJdkHome));
    roots.add(PathManager.getConfigPath());
    String javaHome = Environment.getVariable("JAVA_HOME");
    if (javaHome != null) roots.add(javaHome);
  }

  @Override
  protected void setUp() throws Exception {
    myJdkHome = IdeaTestUtil.requireRealJdkHome();
    super.setUp();
    removedSdks.clear();
    WriteAction.runAndWait(() -> {
      for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
        ProjectJdkTable.getInstance().removeJdk(sdk);
        if (GRADLE_JDK_NAME.equals(sdk.getName())) continue;
        removedSdks.add(sdk);
      }
      VirtualFile jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(myJdkHome));
      JavaSdk javaSdk = JavaSdk.getInstance();
      SdkType javaSdkType = javaSdk == null ? SimpleJavaSdkType.getInstance() : javaSdk;
      Sdk jdk = SdkConfigurationUtil.setupSdk(new Sdk[0], jdkHomeDir, javaSdkType, true, null, GRADLE_JDK_NAME);
      assertNotNull("Cannot create JDK for " + myJdkHome, jdk);
      ProjectJdkTable.getInstance().addJdk(jdk);
    });
    List<String> allowedRoots = new ArrayList<>();
    collectAllowedRoots(allowedRoots);
    if (!allowedRoots.isEmpty()) {
      VfsRootAccess.allowRootAccess(getTestRootDisposable(), ArrayUtilRt.toStringArray(allowedRoots));
    }
  }

  @Override
  public void tearDown() {
    if (myJdkHome == null) {
      //super.setUp() wasn't called
      return;
    }
    new RunAll(
      () -> {
        WriteAction.runAndWait(() -> {
          Arrays.stream(ProjectJdkTable.getInstance().getAllJdks()).forEach(ProjectJdkTable.getInstance()::removeJdk);
          for (Sdk sdk : removedSdks) {
            SdkConfigurationUtil.addSdk(sdk);
          }
          removedSdks.clear();
        });
      },
      () -> {
        Messages.setTestDialog(TestDialog.DEFAULT);
      },
      super::tearDown
    ).run();
  }
}
