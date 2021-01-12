// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.ScopeToolState;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.ExternalStorageConfigurationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.EdtTestUtilKt;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.settings.TestRunner;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.executeOnEdt;

/**
 * @author Vladislav.Soroka
 */
public class GradleProjectOpenProcessorTest extends GradleImportingTestCase {
  /**
   * Needed only to reuse stuff in GradleImportingTestCase#setUp().
   */
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{{BASE_GRADLE_VERSION}});
  }

  @Override
  protected void collectAllowedRoots(List<String> roots) {
    super.collectAllowedRoots(roots);
    for (String javaHome : JavaSdk.getInstance().suggestHomePaths()) {
      roots.add(javaHome);
      roots.addAll(collectRootsInside(javaHome));
    }
  }

  @Test
  public void testGradleSettingsFileModification() throws Exception {
    VirtualFile foo = createProjectSubDir("foo");
    createProjectSubFile("foo/build.gradle", "apply plugin: 'java'");
    createProjectSubFile("foo/.idea/modules.xml",
                         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                         "<project version=\"4\">\n" +
                         "  <component name=\"ProjectModuleManager\">\n" +
                         "    <modules>\n" +
                         "      <module fileurl=\"file://$PROJECT_DIR$/foo.iml\" filepath=\"$PROJECT_DIR$/foo.iml\" />\n" +
                         "      <module fileurl=\"file://$PROJECT_DIR$/bar.iml\" filepath=\"$PROJECT_DIR$/bar.iml\" />\n" +
                         "    </modules>\n" +
                         "  </component>\n" +
                         "</project>");
    createProjectSubFile("foo/foo.iml",
                         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                         "<module type=\"JAVA_MODULE\" version=\"4\">\n" +
                         "  <component name=\"NewModuleRootManager\" inherit-compiler-output=\"true\">\n" +
                         "    <content url=\"file://$MODULE_DIR$\">\n" +
                         "    </content>\n" +
                         "  </component>\n" +
                         "</module>");
    createProjectSubFile("foo/bar.iml",
                         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                         "<module type=\"JAVA_MODULE\" version=\"4\">\n" +
                         "  <component name=\"NewModuleRootManager\" inherit-compiler-output=\"true\">\n" +
                         "  </component>\n" +
                         "</module>");

    Project fooProject = PlatformTestUtil.loadAndOpenProject(foo.toNioPath());
    AutoImportProjectTracker.getInstance(fooProject).enableAutoImportInTests();

    try {
      EdtTestUtil.runInEdtAndWait(() -> PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue());
      assertModules(fooProject, "foo", "bar");

      AsyncPromise<?> promise = new AsyncPromise<>();
      final MessageBusConnection myBusConnection = fooProject.getMessageBus().connect();
      myBusConnection.subscribe(ProjectDataImportListener.TOPIC, path -> promise.setResult(null));
      createProjectSubFile("foo/.idea/gradle.xml",
                           "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                           "<project version=\"4\">\n" +
                           "  <component name=\"GradleSettings\">\n" +
                           "    <option name=\"linkedExternalProjectsSettings\">\n" +
                           "      <GradleProjectSettings>\n" +
                           "        <option name=\"distributionType\" value=\"DEFAULT_WRAPPED\" />\n" +
                           "        <option name=\"externalProjectPath\" value=\"$PROJECT_DIR$\" />\n" +
                           "        <option name=\"gradleJvm\" value=\"" + GRADLE_JDK_NAME + "\" />\n" +
                           "        <option name=\"modules\">\n" +
                           "          <set>\n" +
                           "            <option value=\"$PROJECT_DIR$\" />\n" +
                           "          </set>\n" +
                           "        </option>\n" +
                           "        <option name=\"resolveModulePerSourceSet\" value=\"false\" />\n" +
                           "      </GradleProjectSettings>\n" +
                           "    </option>\n" +
                           "  </component>\n" +
                           "</project>");
      edt(() -> UIUtil.dispatchAllInvocationEvents());
      edt(() -> PlatformTestUtil.saveProject(fooProject));
      edt(() -> PlatformTestUtil.waitForPromise(promise, TimeUnit.MINUTES.toMillis(1)));
      assertTrue("The module has not been linked",
                 ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, getModule(fooProject, "foo")));
    }
    finally {
      edt(() -> closeProject(fooProject));
    }
    assertFalse(fooProject.isOpen());
    assertTrue(fooProject.isDisposed());
  }

  @Test
  public void testDefaultGradleSettings() throws IOException {
    VirtualFile foo = createProjectSubDir("foo");
    createProjectSubFile("foo/build.gradle", "apply plugin: 'java'");
    createProjectSubFile("foo/settings.gradle", "");
    Project fooProject = executeOnEdt(() -> ProjectUtil.openOrImport(foo.toNioPath()));

    try {
      assertTrue(fooProject.isOpen());
      edt(() -> UIUtil.dispatchAllInvocationEvents());
      assertTrue("The module has not been linked",
                 ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, getModule(fooProject, "foo")));
      assertTrue(ExternalStorageConfigurationManager.getInstance(fooProject).isEnabled());
      assertTrue(GradleSettings.getInstance(fooProject).getStoreProjectFilesExternally());
      GradleProjectSettings fooSettings = GradleSettings.getInstance(fooProject).getLinkedProjectSettings(foo.getPath());
      assertTrue(fooSettings.isResolveModulePerSourceSet());
      assertTrue(fooSettings.isResolveExternalAnnotations());
      assertTrue(fooSettings.getDelegatedBuild());
      assertEquals(TestRunner.GRADLE, fooSettings.getTestRunner());
      assertTrue(fooSettings.isUseQualifiedModuleNames());
    }
    finally {
      edt(() -> closeProject(fooProject));
    }
    assertFalse(fooProject.isOpen());
    assertTrue(fooProject.isDisposed());
  }

  @Test
  public void testOpenAndImportProjectInHeadlessMode() throws Exception {
    VirtualFile foo = createProjectSubDir("foo");
    createProjectSubFile("foo/build.gradle", "apply plugin: 'java'");
    createProjectSubFile("foo/.idea/inspectionProfiles/myInspections.xml",
                         "<component name=\"InspectionProjectProfileManager\">\n" +
                         "  <profile version=\"1.0\">\n" +
                         "    <option name=\"myName\" value=\"myInspections\" />\n" +
                         "    <option name=\"myLocal\" value=\"true\" />\n" +
                         "    <inspection_tool class=\"MultipleRepositoryUrls\" enabled=\"true\" level=\"ERROR\" enabled_by_default=\"true\" />\n" +
                         "  </profile>\n" +
                         "</component>");
    createProjectSubFile("foo/.idea/inspectionProfiles/profiles_settings.xml",
                         "<component name=\"InspectionProjectProfileManager\">\n" +
                         "  <settings>\n" +
                         "    <option name=\"PROJECT_PROFILE\" value=\"myInspections\" />\n" +
                         "    <version value=\"1.0\" />\n" +
                         "  </settings>\n" +
                         "</component>");
    FileUtil.copyDir(new File(getProjectPath(), "gradle"), new File(getProjectPath(), "foo/gradle"));

    Project fooProject = null;
    try {
      fooProject = EdtTestUtilKt.runInEdtAndGet(() -> {
        final Project project = ProjectUtil.openOrImport(foo.toNioPath());
        ProjectInspectionProfileManager.getInstance(project).forceLoadSchemes();
        UIUtil.dispatchAllInvocationEvents();
        return project;
      });
      assertTrue(fooProject.isOpen());
      InspectionProfileImpl currentProfile = getCurrentProfile(fooProject);
      assertEquals("myInspections", currentProfile.getName());
      ScopeToolState toolState = currentProfile.getToolDefaultState("MultipleRepositoryUrls", fooProject);
      assertEquals(HighlightDisplayLevel.ERROR, toolState.getLevel());

      // Gradle import will fail because of classloading limitation of the test mode since wrong guava pollute the classpath
      // assertModules(fooProject, "foo", "foo_main", "foo_test");
    }
    finally {
      if (fooProject != null) {
        Project finalFooProject = fooProject;
        edt(() -> closeProject(finalFooProject));
      }
    }
    assertFalse(fooProject.isOpen());
    assertTrue(fooProject.isDisposed());
  }

  @NotNull
  private static InspectionProfileImpl getCurrentProfile(Project fooProject) {
    InspectionProfileImpl currentProfile = InspectionProfileManager.getInstance(fooProject).getCurrentProfile();
    if (!currentProfile.wasInitialized()) {
      boolean oldValue = InspectionProfileImpl.INIT_INSPECTIONS;
      try {
        InspectionProfileImpl.INIT_INSPECTIONS = true;
        currentProfile.initInspectionTools(fooProject);
      }
      finally {
        InspectionProfileImpl.INIT_INSPECTIONS = oldValue;
      }
    }
    return currentProfile;
  }

  private static void closeProject(final Project project) {
    if (project != null && !project.isDisposed()) {
      PlatformTestUtil.forceCloseProjectWithoutSaving(project);
    }
  }
}