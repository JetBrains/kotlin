/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.jps.build;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.io.DirectoryContentSpec;
import com.intellij.util.io.DirectoryContentSpecKt;
import com.intellij.util.io.TestFileSystemBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.api.CanceledStatus;
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl;
import org.jetbrains.jps.builders.impl.BuildRootIndexImpl;
import org.jetbrains.jps.builders.impl.BuildTargetIndexImpl;
import org.jetbrains.jps.builders.impl.BuildTargetRegistryImpl;
import org.jetbrains.jps.builders.logging.BuildLoggingManager;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.cmdline.ClasspathBootstrap;
import org.jetbrains.jps.cmdline.ProjectDescriptor;
import org.jetbrains.jps.incremental.BuilderRegistry;
import org.jetbrains.jps.incremental.FSOperations;
import org.jetbrains.jps.incremental.IncProjectBuilder;
import org.jetbrains.jps.incremental.RebuildRequestedException;
import org.jetbrains.jps.incremental.fs.BuildFSState;
import org.jetbrains.jps.incremental.relativizer.PathRelativizerService;
import org.jetbrains.jps.incremental.storage.BuildDataManager;
import org.jetbrains.jps.incremental.storage.BuildTargetsState;
import org.jetbrains.jps.incremental.storage.ProjectStamps;
import org.jetbrains.jps.indices.ModuleExcludeIndex;
import org.jetbrains.jps.indices.impl.IgnoredFileIndexImpl;
import org.jetbrains.jps.indices.impl.ModuleExcludeIndexImpl;
import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.java.*;
import org.jetbrains.jps.model.java.compiler.JavaCompilers;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerConfiguration;
import org.jetbrains.jps.model.java.impl.JavaModuleIndexImpl;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsSdkReferencesTable;
import org.jetbrains.jps.model.serialization.JpsProjectLoader;
import org.jetbrains.jps.model.serialization.PathMacroUtil;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.kotlin.jps.build.CompileScopeTestBuilder.make;

public abstract class JpsBuildTestCase extends UsefulTestCase {
  private File myProjectDir;
  @NotNull protected JpsProject myProject;
  protected JpsModel myModel;
  private JpsSdk<JpsDummyElement> myJdk;
  protected File myDataStorageRoot;
  private TestProjectBuilderLogger myLogger;

  protected Map<String, String> myBuildParams;

  protected static void rename(String path, String newName) {
    try {
      File file = new File(FileUtil.toSystemDependentName(path));
      assertTrue("File " + file.getAbsolutePath() + " doesn't exist", file.exists());
      final File tempFile = new File(file.getParentFile(), "__" + newName);
      FileUtil.rename(file, tempFile);
      File newFile = new File(file.getParentFile(), newName);
      FileUtil.copyContent(tempFile, newFile);
      FileUtil.delete(tempFile);
      change(newFile.getPath());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myModel = JpsElementFactory.getInstance().createModel();
    myProject = myModel.getProject();
    myDataStorageRoot = FileUtil.createTempDirectory("compile-server-" + getProjectName(), null);
    myLogger = new TestProjectBuilderLogger();
    myBuildParams = new HashMap<>();
  }

  protected static void assertOutput(final String outputPath, TestFileSystemBuilder expected) {
    expected.build().assertDirectoryEqual(new File(FileUtil.toSystemDependentName(outputPath)));
  }

  protected static void assertOutput(final String outputPath, DirectoryContentSpec expected) {
    DirectoryContentSpecKt.assertMatches(new File(outputPath), expected);
  }

  protected static void assertOutput(JpsModule module, TestFileSystemBuilder expected) {
    String outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false);
    assertNotNull(outputUrl);
    assertOutput(JpsPathUtil.urlToPath(outputUrl), expected);
  }

  protected static void assertOutput(JpsModule module, DirectoryContentSpec expected) {
    String outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false);
    assertNotNull(outputUrl);
    assertOutput(JpsPathUtil.urlToPath(outputUrl), expected);
  }

  protected static void change(String filePath) {
    change(filePath, null);
  }

  protected static void change(String filePath, final @Nullable String newContent) {
    try {
      File file = new File(FileUtil.toSystemDependentName(filePath));
      assertTrue("File " + file.getAbsolutePath() + " doesn't exist", file.exists());
      if (newContent != null) {
        FileUtil.writeToFile(file, newContent);
      }
      long oldTimestamp = FSOperations.lastModified(file);
      long time = System.currentTimeMillis();
      setLastModified(file, time);
      if (FSOperations.lastModified(file) <= oldTimestamp) {
        setLastModified(file, time + 1);
        long newTimeStamp = FSOperations.lastModified(file);
        if (newTimeStamp <= oldTimestamp) {
          //Mac OS and some versions of Linux truncates timestamp to nearest second
          setLastModified(file, time + 1000);
          newTimeStamp = FSOperations.lastModified(file);
          assertTrue("Failed to change timestamp for " + file.getAbsolutePath(), newTimeStamp > oldTimestamp);
        }
        sleepUntil(newTimeStamp);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static void sleepUntil(long time) {
    //we need this to ensure that the file won't be treated as changed by user during compilation and therefore marked for recompilation
    long delta;
    while ((delta = time - System.currentTimeMillis()) > 0) {
      TimeoutUtil.sleep(delta);
    }
  }

  private static void setLastModified(File file, long time) {
    boolean updated = file.setLastModified(time);
    assertTrue("Cannot modify timestamp for " + file.getAbsolutePath(), updated);
  }

  protected static void delete(String filePath) {
    File file = new File(FileUtil.toSystemDependentName(filePath));
    assertTrue("File " + file.getAbsolutePath() + " doesn't exist", file.exists());
    final boolean deleted = FileUtil.delete(file);
    assertTrue("Cannot delete file " + file.getAbsolutePath(), deleted);
  }

  protected JpsSdk<JpsDummyElement> addJdk(final String name) {
    try {
      String pathToRtJar = ClasspathBootstrap.getResourcePath(Object.class);
      String path = pathToRtJar == null ? null : FileUtil.toSystemIndependentName(new File(pathToRtJar).getCanonicalPath());
      return addJdk(name, path);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected JpsSdk<JpsDummyElement> addJdk(final String name, @Nullable String jdkClassesRoot) {
    String homePath = System.getProperty("java.home");
    String versionString = System.getProperty("java.version");
    JpsTypedLibrary<JpsSdk<JpsDummyElement>> jdk = myModel.getGlobal().addSdk(name, homePath, versionString, JpsJavaSdkType.INSTANCE);
    if (jdkClassesRoot != null) {
      jdk.addRoot(JpsPathUtil.pathToUrl(jdkClassesRoot), JpsOrderRootType.COMPILED);
    }
    return jdk.getProperties();
  }

  protected String getProjectName() {
    return StringUtil.decapitalize(StringUtil.trimStart(getName(), "test"));
  }

  protected ProjectDescriptor createProjectDescriptor(final BuildLoggingManager buildLoggingManager) {
    try {
      BuildTargetRegistryImpl targetRegistry = new BuildTargetRegistryImpl(myModel);
      ModuleExcludeIndex index = new ModuleExcludeIndexImpl(myModel);
      IgnoredFileIndexImpl ignoredFileIndex = new IgnoredFileIndexImpl(myModel);
      BuildDataPaths dataPaths = new BuildDataPathsImpl(myDataStorageRoot);
      BuildRootIndexImpl buildRootIndex = new BuildRootIndexImpl(targetRegistry, myModel, index, dataPaths, ignoredFileIndex);
      BuildTargetIndexImpl targetIndex = new BuildTargetIndexImpl(targetRegistry, buildRootIndex);
      BuildTargetsState targetsState = new BuildTargetsState(dataPaths, myModel, buildRootIndex);
      PathRelativizerService relativizer = new PathRelativizerService(myModel.getProject());
      ProjectStamps projectStamps = new ProjectStamps(myDataStorageRoot, targetsState, relativizer);
      BuildDataManager dataManager = new BuildDataManager(dataPaths, targetsState, relativizer);
      return new ProjectDescriptor(myModel, new BuildFSState(true), projectStamps, dataManager, buildLoggingManager, index,
                                   targetIndex, buildRootIndex, ignoredFileIndex);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void loadProject(String projectPath) {
    loadProject(projectPath, Collections.emptyMap());
  }

  protected void loadProject(String projectPath,
                             Map<String, String> pathVariables) {
    try {
      String testDataRootPath = getTestDataRootPath();
      String fullProjectPath = FileUtil.toSystemDependentName(testDataRootPath != null ? testDataRootPath + "/" + projectPath : projectPath);
      Map<String, String> allPathVariables = new HashMap<>(pathVariables.size() + 1);
      allPathVariables.putAll(pathVariables);
      allPathVariables.put(PathMacroUtil.APPLICATION_HOME_DIR, PathManager.getHomePath());
      allPathVariables.putAll(getAdditionalPathVariables());
      JpsProjectLoader.loadProject(myProject, allPathVariables, Paths.get(fullProjectPath));
      final JpsJavaCompilerConfiguration config = JpsJavaExtensionService.getInstance().getCompilerConfiguration(myProject);
      config.getCompilerOptions(JavaCompilers.JAVAC_ID).PREFER_TARGET_JDK_COMPILER = false;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  protected Map<String, String> getAdditionalPathVariables() {
    return Collections.emptyMap();
  }

  @Nullable
  protected String getTestDataRootPath() {
    return null;
  }

  protected <T extends JpsElement> JpsModule addModule(@NotNull String moduleName,
                                                       String @NotNull [] srcPaths,
                                                       @Nullable String outputPath,
                                                       @Nullable String testOutputPath,
                                                       @Nullable JpsSdk<T> sdk) {
    JpsModule module = myProject.addModule(moduleName, JpsJavaModuleType.INSTANCE);
    if (sdk != null) {
      setupModuleSdk(module, sdk);
    }
    if (srcPaths.length > 0 || outputPath != null) {
      for (String srcPath : srcPaths) {
        module.getContentRootsList().addUrl(JpsPathUtil.pathToUrl(srcPath));
        module.addSourceRoot(JpsPathUtil.pathToUrl(srcPath), JavaSourceRootType.SOURCE);
      }
      JpsJavaModuleExtension extension = JpsJavaExtensionService.getInstance().getOrCreateModuleExtension(module);
      if (outputPath != null) {
        extension.setOutputUrl(JpsPathUtil.pathToUrl(outputPath));
        if (!StringUtil.isEmpty(testOutputPath)) {
          extension.setTestOutputUrl(JpsPathUtil.pathToUrl(testOutputPath));
        }
        else {
          extension.setTestOutputUrl(extension.getOutputUrl());
        }
      }
      else {
        extension.setInheritOutput(true);
      }
    }
    return module;
  }

  protected  <T extends JpsElement> void setupModuleSdk(@NotNull JpsModule module, @NotNull JpsSdk<T> sdk) {
    final JpsSdkType<T> sdkType = sdk.getSdkType();
    final JpsSdkReferencesTable sdkTable = module.getSdkReferencesTable();
    sdkTable.setSdkReference(sdkType, sdk.createReference());

    if (sdkType instanceof JpsJavaSdkTypeWrapper) {
      final JpsSdkReference<T> wrapperRef = sdk.createReference();
      sdkTable.setSdkReference(JpsJavaSdkType.INSTANCE, JpsJavaExtensionService.
        getInstance().createWrappedJavaSdkReference((JpsJavaSdkTypeWrapper)sdkType, wrapperRef));
    }
    // ensure jdk entry is the first one in dependency list
    module.getDependenciesList().clear();
    module.getDependenciesList().addSdkDependency(sdkType);
    module.getDependenciesList().addModuleSourceDependency();
  }

  protected void rebuildAllModules() {
    doBuild(CompileScopeTestBuilder.rebuild().allModules()).assertSuccessful();
  }

  /**
   * Invoked forced rebuild for all targets in the project. May lead to unpredictable results if some plugins add targets your test doesn't expect.
   * @deprecated use {@link #rebuildAllModules()} instead or directly add required target types to the scope via {@link CompileScopeTestBuilder#targetTypes}
   */
  @Deprecated
  protected void rebuildAll() {
    doBuild(CompileScopeTestBuilder.rebuild().all()).assertSuccessful();
  }

  protected BuildResult buildAllModules() {
    return doBuild(make().allModules());
  }

  /**
   * Invoked incremental build for all targets in the project. May lead to unpredictable results if some plugins add targets your test doesn't expect.
   *
   * @deprecated use {@link #buildAllModules()} instead or directly add required target types to the scope via {@link CompileScopeTestBuilder#targetTypes}
   */
  @Deprecated
  protected BuildResult makeAll() {
    return doBuild(make().all());
  }

  protected BuildResult doBuild(CompileScopeTestBuilder scope) {
    ProjectDescriptor descriptor = createProjectDescriptor(new BuildLoggingManager(myLogger));
    try {
      myLogger.clearFilesData();
      return doBuild(descriptor, scope);
    }
    finally {
      descriptor.release();
    }
  }

  protected void clearBuildLog() {
    myLogger.clearLog();
  }

  public void assertCompiled(String builderName, String... paths) {
    myLogger.assertCompiled(builderName, new File[]{myProjectDir, myDataStorageRoot}, paths);
  }

  public void checkFullLog(File expectedLogFile) {
    assertSameLinesWithFile(expectedLogFile.getAbsolutePath(), myLogger.getFullLog(myProjectDir, myDataStorageRoot));
  }

  protected void assertDeleted(String... paths) {
    myLogger.assertDeleted(new File[]{myProjectDir, myDataStorageRoot}, paths);
  }

  protected BuildResult doBuild(final ProjectDescriptor descriptor, CompileScopeTestBuilder scopeBuilder) {
    IncProjectBuilder builder = new IncProjectBuilder(descriptor, BuilderRegistry.getInstance(), myBuildParams, CanceledStatus.NULL, true);
    BuildResult result = new BuildResult();
    builder.addMessageHandler(result);
    try {
      beforeBuildStarted(descriptor);
      builder.build(scopeBuilder.build(), false);
      result.storeMappingsDump(descriptor);
    }
    catch (RebuildRequestedException | IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      // the following code models module index reload after each make session
      JavaModuleIndex moduleIndex = JpsJavaExtensionService.getInstance().getJavaModuleIndex(descriptor.getProject());
      if (moduleIndex instanceof JavaModuleIndexImpl) {
        ((JavaModuleIndexImpl)moduleIndex).dropCache();
      }
    }
    return result;
  }

  protected void beforeBuildStarted(@NotNull ProjectDescriptor descriptor) {
  }

  protected void deleteFile(String relativePath) {
    delete(new File(getOrCreateProjectDir(), relativePath).getAbsolutePath());
  }

  protected void changeFile(String relativePath) {
    changeFile(relativePath, null);
  }

  protected void changeFile(String relativePath, String newContent) {
    change(new File(getOrCreateProjectDir(), relativePath).getAbsolutePath(), newContent);
  }

  protected String createFile(String relativePath) {
    return createFile(relativePath, "");
  }

  protected String createDir(String relativePath) {
    File dir = new File(getOrCreateProjectDir(), relativePath);
    boolean created = dir.mkdirs();
    if (!created && !dir.isDirectory()) {
      fail("Cannot create " + dir.getAbsolutePath() + " directory");
    }
    return FileUtil.toSystemIndependentName(dir.getAbsolutePath());
  }

  public String createFile(String relativePath, final String text) {
    try {
      File file = new File(getOrCreateProjectDir(), relativePath);
      FileUtil.writeToFile(file, text);
      return FileUtil.toSystemIndependentName(file.getAbsolutePath());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected String copyToProject(String relativeSourcePath, String relativeTargetPath) {
    File source = findFindUnderProjectHome(relativeSourcePath);
    String fullTargetPath = getAbsolutePath(relativeTargetPath);
    File target = new File(fullTargetPath);
    try {
      FileUtil.copyFileOrDir(source, target);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return fullTargetPath;
  }

  protected File findFindUnderProjectHome(String relativeSourcePath) {
    return PathManagerEx.findFileUnderProjectHome(relativeSourcePath, getClass());
  }

  public File getOrCreateProjectDir() {
    if (myProjectDir == null) {
      try {
        myProjectDir = doGetProjectDir();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return myProjectDir;
  }

  protected File doGetProjectDir() throws IOException {
    return FileUtil.createTempDirectory("prj", null);
  }

  public String getAbsolutePath(final String pathRelativeToProjectRoot) {
    return FileUtil.toSystemIndependentName(new File(getOrCreateProjectDir(), pathRelativeToProjectRoot).getAbsolutePath());
  }

  public @NotNull String getUrl(@NotNull String pathRelativeToProjectRoot) {
    return JpsPathUtil.pathToUrl(getAbsolutePath(pathRelativeToProjectRoot));
  }

  public JpsModule addModule(String moduleName, String... srcPaths) {
    return addModule(moduleName, srcPaths, getAbsolutePath(getModuleOutputRelativePath(moduleName)), null, getJdk());
  }

  protected final JpsSdk<JpsDummyElement> getJdk() {
    if (myJdk == null) {
      myJdk = addJdk("1.6");
    }
    return myJdk;
  }

  @NotNull
  protected static File getModuleOutput(JpsModule module) {
    String outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false);
    return JpsPathUtil.urlToFile(outputUrl);
  }

  @NotNull
  protected String getModuleOutputRelativePath(JpsModule module) {
    return getModuleOutputRelativePath(module.getName());
  }

  @NotNull
  protected String getModuleOutputRelativePath(String moduleName) {
    return "out/production/" + moduleName;
  }

  protected void checkMappingsAreSameAfterRebuild(BuildResult makeResult) {
    String makeDump = makeResult.getMappingsDump();
    BuildResult rebuildResult = doBuild(CompileScopeTestBuilder.rebuild().allModules());
    rebuildResult.assertSuccessful();
    String rebuildDump = rebuildResult.getMappingsDump();
    assertEquals(rebuildDump, makeDump);
  }
}
