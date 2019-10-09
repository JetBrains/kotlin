// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.externalSystem.importing.ImportSpec;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter;
import com.intellij.openapi.externalSystem.test.ExternalSystemImportingTestCase;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.impl.JavaHomeFinder;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.RunAll;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.lang.JavaVersion;
import org.gradle.StartParameter;
import org.gradle.util.GradleVersion;
import org.gradle.wrapper.GradleWrapperMain;
import org.gradle.wrapper.PathAssembler;
import org.gradle.wrapper.WrapperConfiguration;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JdkVersionDetector;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSystemSettings;
import org.jetbrains.plugins.gradle.tooling.VersionMatcherRule;
import org.jetbrains.plugins.gradle.tooling.builder.AbstractModelBuilderTest;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleUtil;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static org.jetbrains.plugins.gradle.tooling.builder.AbstractModelBuilderTest.DistributionLocator;
import static org.jetbrains.plugins.gradle.tooling.builder.AbstractModelBuilderTest.SUPPORTED_GRADLE_VERSIONS;
import static org.junit.Assume.assumeThat;

@RunWith(value = Parameterized.class)
public abstract class GradleImportingTestCase extends ExternalSystemImportingTestCase {
  public static final String BASE_GRADLE_VERSION = AbstractModelBuilderTest.BASE_GRADLE_VERSION;
  protected static final String GRADLE_JDK_NAME = "Gradle JDK";
  private static final int GRADLE_DAEMON_TTL_MS = 10000;

  @Rule public TestName name = new TestName();

  @Rule public VersionMatcherRule versionMatcherRule = new VersionMatcherRule();
  @NotNull
  @org.junit.runners.Parameterized.Parameter()
  public String gradleVersion;
  private GradleProjectSettings myProjectSettings;
  private String myJdkHome;

  @Override
  public void setUp() throws Exception {
    assumeThat(gradleVersion, versionMatcherRule.getMatcher());
    myJdkHome = requireRealJdkHome();
    super.setUp();
    WriteAction.runAndWait(() -> {
      Sdk oldJdk = ProjectJdkTable.getInstance().findJdk(GRADLE_JDK_NAME);
      if (oldJdk != null) {
        ProjectJdkTable.getInstance().removeJdk(oldJdk);
      }
      VirtualFile jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(myJdkHome));
      JavaSdk javaSdk = JavaSdk.getInstance();
      SdkType javaSdkType = javaSdk == null ? SimpleJavaSdkType.getInstance() : javaSdk;
      Sdk jdk = SdkConfigurationUtil.setupSdk(new Sdk[0], jdkHomeDir, javaSdkType, true, null, GRADLE_JDK_NAME);
      assertNotNull("Cannot create JDK for " + myJdkHome, jdk);
      ProjectJdkTable.getInstance().addJdk(jdk);
    });
    myProjectSettings = new GradleProjectSettings().withQualifiedModuleNames();
    System.setProperty(ExternalSystemExecutionSettings.REMOTE_PROCESS_IDLE_TTL_IN_MS_KEY, String.valueOf(GRADLE_DAEMON_TTL_MS));
    PathAssembler.LocalDistribution distribution = WriteAction.computeAndWait(() -> configureWrapper());

    List<String> allowedRoots = new ArrayList<>();
    collectAllowedRoots(allowedRoots, distribution);
    if (!allowedRoots.isEmpty()) {
      VfsRootAccess.allowRootAccess(myTestFixture.getTestRootDisposable(), ArrayUtilRt.toStringArray(allowedRoots));
    }
  }

  @NotNull
  protected GradleVersion getCurrentGradleVersion() {
    return GradleVersion.version(gradleVersion);
  }

  @NotNull
  protected GradleVersion getCurrentGradleBaseVersion() {
    return GradleVersion.version(gradleVersion).getBaseVersion();
  }

  protected void assumeTestJavaRuntime(@NotNull JavaVersion javaRuntimeVersion) {
    Assume.assumeFalse("Skip integration tests running on JDK 9+ for Gradle < 3.0",
                       javaRuntimeVersion.feature > 9 && getCurrentGradleBaseVersion().compareTo(GradleVersion.version("3.0")) < 0);
  }

  @NotNull
  private String requireRealJdkHome() {
    JavaVersion javaRuntimeVersion = JavaVersion.current();
    assumeTestJavaRuntime(javaRuntimeVersion);
    GradleVersion baseVersion = GradleVersion.version(gradleVersion).getBaseVersion();
    if (javaRuntimeVersion.feature > 9 && baseVersion.compareTo(GradleVersion.version("4.8")) < 0) {
      List<String> paths = JavaHomeFinder.suggestHomePaths();
      for (String path : paths) {
        if (JdkUtil.checkForJdk(path)) {
          JdkVersionDetector.JdkVersionInfo jdkVersionInfo = JdkVersionDetector.getInstance().detectJdkVersionInfo(path);
          if (jdkVersionInfo == null) continue;
          int feature = jdkVersionInfo.version.feature;
          if (feature > 6 && feature < 9) {
            return path;
          }
        }
      }
      Assume.assumeTrue("Cannot find JDK for Gradle, checked paths: " + paths, false);
      return null;
    }
    else {
      return IdeaTestUtil.requireRealJdkHome();
    }
  }

  protected void collectAllowedRoots(final List<String> roots, PathAssembler.LocalDistribution distribution) {
  }

  @Override
  public void tearDown() throws Exception {
    if (myJdkHome == null) {
      //super.setUp() wasn't called
      return;
    }
    new RunAll(
      () -> {
        Sdk jdk = ProjectJdkTable.getInstance().findJdk(GRADLE_JDK_NAME);
        if (jdk != null) {
          WriteAction.runAndWait(() -> ProjectJdkTable.getInstance().removeJdk(jdk));
        }
      },
      () -> {
        Messages.setTestDialog(TestDialog.DEFAULT);
        deleteBuildSystemDirectory();
      },
      super::tearDown
    ).run();
  }

  @Override
  protected void collectAllowedRoots(final List<String> roots) {
    super.collectAllowedRoots(roots);
    roots.add(myJdkHome);
    roots.addAll(collectRootsInside(myJdkHome));
    roots.add(PathManager.getConfigPath());
  }

  @Override
  public String getName() {
    return name.getMethodName() == null ? super.getName() : FileUtil.sanitizeFileName(name.getMethodName());
  }

  @Parameterized.Parameters(name = "{index}: with Gradle-{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(SUPPORTED_GRADLE_VERSIONS);
  }

  @Override
  protected String getTestsTempDir() {
    return "tmp";
  }

  @Override
  protected String getExternalSystemConfigFileName() {
    return "build.gradle";
  }

  protected void importProjectUsingSingeModulePerGradleProject() {
    getCurrentExternalProjectSettings().setResolveModulePerSourceSet(false);
    importProject();
  }

  @Override
  protected void importProject() {
    ExternalSystemApiUtil.subscribe(myProject, GradleConstants.SYSTEM_ID, new ExternalSystemSettingsListenerAdapter() {
      @Override
      public void onProjectsLinked(@NotNull Collection settings) {
        super.onProjectsLinked(settings);
        final Object item = ContainerUtil.getFirstItem(settings);
        if (item instanceof GradleProjectSettings) {
          ((GradleProjectSettings)item).setGradleJvm(GRADLE_JDK_NAME);
        }
      }
    });
    super.importProject();
  }

  protected void importProjectUsingSingeModulePerGradleProject(@NonNls @Language("Groovy") String config) throws IOException {
    getCurrentExternalProjectSettings().setResolveModulePerSourceSet(false);
    importProject(config);
  }

  @Override
  protected void importProject(@NonNls @Language("Groovy") String config) throws IOException {
    config = injectRepo(config);
    super.importProject(config);
  }

  @Override
  protected ImportSpec createImportSpec() {
    ImportSpecBuilder importSpecBuilder = new ImportSpecBuilder(super.createImportSpec());
    importSpecBuilder.withArguments("--stacktrace");
    return importSpecBuilder.build();
  }

  @NotNull
  protected String injectRepo(@NonNls @Language("Groovy") String config) {
    config = "allprojects {\n" +
             "  repositories {\n" +
             "    maven {\n" +
             "        url 'http://maven.labs.intellij.net/repo1'\n" +
             "    }\n" +
             "  }" +
             "}\n" + config;
    return config;
  }

  @Override
  protected GradleProjectSettings getCurrentExternalProjectSettings() {
    return myProjectSettings;
  }

  @Override
  protected ProjectSystemId getExternalSystemId() {
    return GradleConstants.SYSTEM_ID;
  }

  protected VirtualFile createSettingsFile(@NonNls @Language("Groovy") String content) throws IOException {
    return createProjectSubFile("settings.gradle", content);
  }

  protected boolean isGradle40orNewer() {
    return GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("4.0")) >= 0;
  }

  private PathAssembler.LocalDistribution configureWrapper() throws IOException, URISyntaxException {

    final URI distributionUri = new DistributionLocator().getDistributionFor(GradleVersion.version(gradleVersion));

    myProjectSettings.setDistributionType(DistributionType.DEFAULT_WRAPPED);
    final VirtualFile wrapperJarFrom = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(wrapperJar());
    assert wrapperJarFrom != null;

    final VirtualFile wrapperJarFromTo = createProjectSubFile("gradle/wrapper/gradle-wrapper.jar");
    WriteAction.runAndWait(() -> wrapperJarFromTo.setBinaryContent(wrapperJarFrom.contentsToByteArray()));


    Properties properties = new Properties();
    properties.setProperty("distributionBase", "GRADLE_USER_HOME");
    properties.setProperty("distributionPath", "wrapper/dists");
    properties.setProperty("zipStoreBase", "GRADLE_USER_HOME");
    properties.setProperty("zipStorePath", "wrapper/dists");
    properties.setProperty("distributionUrl", distributionUri.toString());

    StringWriter writer = new StringWriter();
    properties.store(writer, null);

    createProjectSubFile("gradle/wrapper/gradle-wrapper.properties", writer.toString());

    WrapperConfiguration wrapperConfiguration = GradleUtil.getWrapperConfiguration(getProjectPath());
    PathAssembler.LocalDistribution localDistribution = new PathAssembler(
      StartParameter.DEFAULT_GRADLE_USER_HOME).getDistribution(wrapperConfiguration);

    File zip = localDistribution.getZipFile();
    try {
      if (zip.exists()) {
        ZipFile zipFile = new ZipFile(zip);
        zipFile.close();
      }
    }
    catch (ZipException e) {
      e.printStackTrace();
      System.out.println("Corrupted file will be removed: " + zip.getPath());
      FileUtil.delete(zip);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return localDistribution;
  }

  @NotNull
  private static File wrapperJar() {
    return new File(PathUtil.getJarPathForClass(GradleWrapperMain.class));
  }

  protected void assertMergedModuleCompileLibDepScope(String moduleName, String depName) {
    if (isGradleOlderThen_3_4() || isGradleNewerThen_4_5()) {
      assertModuleLibDepScope(moduleName, depName, DependencyScope.COMPILE);
    }
    else {
      assertModuleLibDepScope(moduleName, depName, DependencyScope.PROVIDED, DependencyScope.TEST, DependencyScope.RUNTIME);
    }
  }

  protected void assertMergedModuleCompileModuleDepScope(String moduleName, String depName) {
    if (isGradleOlderThen_3_4() || isGradleNewerThen_4_5()) {
      assertModuleModuleDepScope(moduleName, depName, DependencyScope.COMPILE);
    }
    else {
      assertModuleModuleDepScope(moduleName, depName, DependencyScope.PROVIDED, DependencyScope.TEST, DependencyScope.RUNTIME);
    }
  }

  protected boolean isGradleOlderThen_3_3() {
    return GradleVersion.version(gradleVersion).getBaseVersion().compareTo(GradleVersion.version("3.3")) < 0;
  }

  protected boolean isGradleOlderThen_3_4() {
    return GradleVersion.version(gradleVersion).getBaseVersion().compareTo(GradleVersion.version("3.4")) < 0;
  }

  protected boolean isGradleOlderThen_4_0() {
    return GradleVersion.version(gradleVersion).getBaseVersion().compareTo(GradleVersion.version("4.0")) < 0;
  }

  protected boolean isGradleNewerThen_4_5() {
    return GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("4.5")) > 0;
  }

  protected boolean isGradleOlderThen_5_2() {
    return GradleVersion.version(gradleVersion).getBaseVersion().compareTo(GradleVersion.version("5.2")) < 0;
  }

  protected boolean isGradleOlderThen_4_8() {
    return GradleVersion.version(gradleVersion).getBaseVersion().compareTo(GradleVersion.version("4.8")) < 0;
  }

  protected boolean isGradleNewerOrSameThen_5_0() {
    return GradleVersion.version(gradleVersion).getBaseVersion().compareTo(GradleVersion.version("5.0")) >= 0;
  }

  protected String getExtraPropertiesExtensionFqn() {
    return isGradleOlderThen_5_2() ? "org.gradle.api.internal.plugins.DefaultExtraPropertiesExtension"
                                   : "org.gradle.internal.extensibility.DefaultExtraPropertiesExtension";
  }

  protected void enableGradleDebugWithSuspend() {
    GradleSystemSettings.getInstance().setGradleVmOptions("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
  }
}
