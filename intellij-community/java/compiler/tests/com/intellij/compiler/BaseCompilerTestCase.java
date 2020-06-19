// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.ProjectTopics;
import com.intellij.compiler.impl.CompileDriver;
import com.intellij.compiler.impl.ExitStatus;
import com.intellij.compiler.server.BuildManager;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.*;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.io.DirectoryContentSpec;
import com.intellij.util.io.DirectoryContentSpecKt;
import com.intellij.util.io.TestFileSystemBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.util.JpsPathUtil;
import org.junit.Assert;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public abstract class BaseCompilerTestCase extends JavaModuleTestCase {
  @Override
  protected void setUpModule() {
  }

  @Override
  protected boolean isCreateProjectFileExplicitly() {
    return false;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myProject.getMessageBus().connect(getTestRootDisposable()).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        //todo[nik] projectOpened isn't called in tests so we need to add this listener manually
        forceFSRescan();
      }
    });
    CompilerTestUtil.enableExternalCompiler();
  }

  protected void forceFSRescan() {
    BuildManager.getInstance().clearState(myProject);
  }

  @NotNull
  @Override
  protected LanguageLevel getProjectLanguageLevel() {
    return LanguageLevel.JDK_1_8;
  }

  @Override
  protected Sdk getTestProjectJdk() {
    return JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      for (Artifact artifact : getArtifactManager().getArtifacts()) {
        final String outputPath = artifact.getOutputPath();
        if (!StringUtil.isEmpty(outputPath)) {
          FileUtil.delete(new File(FileUtil.toSystemDependentName(outputPath)));
        }
      }
      CompilerTestUtil.disableExternalCompiler(getProject());
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  protected ArtifactManager getArtifactManager() {
    return ArtifactManager.getInstance(myProject);
  }

  protected String getProjectBasePath() {
    return myProject.getBasePath();
  }

  protected void copyToProject(String relativePath) {
    File dir = PathManagerEx.findFileUnderProjectHome(relativePath, getClass());
    final File target = new File(FileUtil.toSystemDependentName(getProjectBasePath()));
    try {
      FileUtil.copyDir(dir, target);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    WriteAction.runAndWait(() -> {
      VirtualFile virtualDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(target);
      assertNotNull(target.getAbsolutePath() + " not found", virtualDir);
      virtualDir.refresh(false, true);
    });
  }

  protected Module addModule(String moduleName, @Nullable VirtualFile sourceRoot) {
    return addModule(moduleName, sourceRoot, null);
  }

  protected Module addModule(String moduleName, @Nullable VirtualFile sourceRoot, @Nullable VirtualFile testRoot) {
    return WriteAction.computeAndWait(() -> {
      final Module module = createModule(moduleName);
      if (sourceRoot != null) {
        PsiTestUtil.addSourceContentToRoots(module, sourceRoot, false);
      }
      if (testRoot != null) {
        PsiTestUtil.addSourceContentToRoots(module, testRoot, true);
      }
      ModuleRootModificationUtil.setModuleSdk(module, getTestProjectJdk());
      return module;
    });
  }

  protected VirtualFile createFile(final String path) {
    return createFile(path, "");
  }

  protected VirtualFile createFile(@NotNull String path, final String text) {
    return VfsTestUtil.createFile(getOrCreateProjectBaseDir(), path, text);
  }

  protected CompilationLog make(final Artifact... artifacts) {
    final CompileScope scope = ArtifactCompileScope.createArtifactsScope(myProject, Arrays.asList(artifacts));
    return make(scope);
  }

  protected CompilationLog recompile(final Artifact... artifacts) {
    final CompileScope scope = ArtifactCompileScope.createArtifactsScope(myProject, Arrays.asList(artifacts), true);
    return make(scope);
  }

  protected CompilationLog make(Module... modules) {
    return make(false, false, modules);
  }

  protected CompilationLog makeWithDependencies(final boolean includeRuntimeDependencies, Module... modules) {
    return make(true, includeRuntimeDependencies, modules);
  }

  private CompilationLog make(boolean includeDependentModules, final boolean includeRuntimeDependencies, Module... modules) {
    return make(getCompilerManager().createModulesCompileScope(modules, includeDependentModules, includeRuntimeDependencies));
  }

  protected CompilationLog recompile(Module... modules) {
    return compile(getCompilerManager().createModulesCompileScope(modules, false), true);
  }

  protected CompilerManager getCompilerManager() {
    return CompilerManager.getInstance(myProject);
  }

  protected void assertModulesUpToDate() {
    boolean upToDate = getCompilerManager().isUpToDate(getCompilerManager().createProjectCompileScope(myProject));
    assertTrue("Modules are not up to date", upToDate);
  }

  protected CompilationLog compile(boolean force, VirtualFile... files) {
    return compile(getCompilerManager().createFilesCompileScope(files), force);
  }

  protected CompilationLog make(final CompileScope scope) {
    return compile(scope, false);
  }

  protected CompilationLog compile(final CompileScope scope, final boolean forceCompile) {
    return compile(scope, forceCompile, false);
  }

  protected CompilationLog compile(final CompileScope scope, final boolean forceCompile,
                                   final boolean errorsExpected) {
    return compile(errorsExpected, callback -> {
      final CompilerManager compilerManager = getCompilerManager();
      if (forceCompile) {
        compilerManager.compile(scope, callback);
      }
      else {
        compilerManager.make(scope, callback);
      }
    });
  }

  protected CompilationLog rebuild() {
    return compile(false, compileStatusNotification -> getCompilerManager().rebuild(compileStatusNotification));
  }

  protected CompilationLog compile(final boolean errorsExpected, final Consumer<CompileStatusNotification> action) {
    CompilationLog log = compile(action);
    if (errorsExpected && log.myErrors.length == 0) {
      Assert.fail("compilation finished without errors");
    }
    else if (!errorsExpected && log.myErrors.length > 0) {
      Assert.fail("compilation finished with errors: " + Arrays.toString(log.myErrors));
    }
    return log;
  }

  private CompilationLog compile(final Consumer<CompileStatusNotification> action) {
    final Ref<CompilationLog> result = Ref.create(null);
    final Semaphore semaphore = new Semaphore();
    semaphore.down();
    final List<String> generatedFilePaths = new ArrayList<>();
    myProject.getMessageBus().connect(getTestRootDisposable()).subscribe(CompilerTopics.COMPILATION_STATUS, new CompilationStatusListener() {
      @Override
      public void fileGenerated(@NotNull String outputRoot, @NotNull String relativePath) {
        generatedFilePaths.add(relativePath);
      }
    });
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
      final CompileStatusNotification callback = new CompileStatusNotification() {
        @Override
        public void finished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
          try {
            if (aborted) {
              Assert.fail("compilation aborted");
            }
            ExitStatus status = CompileDriver.getExternalBuildExitStatus(compileContext);
            result.set(new CompilationLog(status == ExitStatus.UP_TO_DATE,
                                          generatedFilePaths,
                                          compileContext.getMessages(CompilerMessageCategory.ERROR),
                                          compileContext.getMessages(CompilerMessageCategory.WARNING)));
          }
          finally {
            semaphore.up();
          }
        }
      };
      PlatformTestUtil.saveProject(myProject);
      CompilerTestUtil.saveApplicationSettings();
      try {
        CompilerTester.enableDebugLogging();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      action.accept(callback);
    });

    try {
      final long start = System.currentTimeMillis();
      while (!semaphore.waitFor(10)) {
        if (!BuildManager.getInstance().isBuildProcessDebuggingEnabled() && System.currentTimeMillis() - start > 5 * 60 * 1000) {
          throw new RuntimeException("timeout");
        }
        if (SwingUtilities.isEventDispatchThread()) {
          UIUtil.dispatchAllInvocationEvents();
        }
      }
      if (SwingUtilities.isEventDispatchThread()) {
        UIUtil.dispatchAllInvocationEvents();
      }
    }
    finally {
      CompilerTester.printBuildLog();
    }

    return result.get();
  }

  protected void changeFile(VirtualFile file) {
    changeFile(file, null);
  }

  protected void changeFile(final VirtualFile file, @Nullable final String newText) {
    try {
      if (newText != null) {
        setFileText(file, newText);
      }
      ((NewVirtualFile)file).setTimeStamp(file.getTimeStamp() + 10);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void deleteFile(final VirtualFile file) {
    try {
      WriteAction.runAndWait(() -> file.delete(this));
    }
    catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  protected void setUpProject() throws Exception {
    super.setUpProject();

    CompilerProjectExtension.getInstance(myProject).setCompilerOutputUrl("file://" + myProject.getBasePath() + "/out");
  }

  @NotNull
  @Override
  protected Module doCreateRealModule(@NotNull String moduleName) {
    //todo[nik] reuse code from PlatformTestCase
    VirtualFile baseDir = getOrCreateProjectBaseDir();
    Path moduleFile = baseDir.toNioPath().resolve(moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION);
    myFilesToDelete.add(moduleFile);
    return WriteAction.computeAndWait(() -> {
      Module module = ModuleManager.getInstance(myProject)
        .newModule(FileUtil.toSystemIndependentName(moduleFile.toString()), getModuleType().getId());
      module.getModuleFile();
      return module;
    });
  }

  protected CompilationLog buildAllModules() {
    return make(getCompilerManager().createProjectCompileScope(myProject));
  }

  protected static void assertOutput(Module module, TestFileSystemBuilder item) {
    assertOutput(module, item, false);
  }

  protected static void assertOutput(Module module, DirectoryContentSpec spec) {
    DirectoryContentSpecKt.assertMatches(getOutputDir(module, false), spec);
  }

  protected static void assertOutput(Module module, TestFileSystemBuilder item, final boolean forTests) {
    File outputDir = getOutputDir(module, forTests);
    Assert.assertTrue((forTests? "Test output" : "Output") +" directory " + outputDir.getAbsolutePath() + " doesn't exist", outputDir.exists());
    item.build().assertDirectoryEqual(outputDir);
  }

  protected static void assertNoOutput(Module module) {
    File dir = getOutputDir(module);
    Assert.assertFalse("Output directory " + dir.getAbsolutePath() + " does exist", dir.exists());
  }

  protected static File getOutputDir(Module module) {
    return getOutputDir(module, false);
  }

  protected static File getOutputDir(Module module, boolean forTests) {
    CompilerModuleExtension extension = CompilerModuleExtension.getInstance(module);
    Assert.assertNotNull(extension);
    String outputUrl = forTests? extension.getCompilerOutputUrlForTests() : extension.getCompilerOutputUrl();
    Assert.assertNotNull((forTests? "Test output" : "Output") +" directory for module '" + module.getName() + "' isn't specified", outputUrl);
    return JpsPathUtil.urlToFile(outputUrl);
  }

  protected static void createFileInOutput(Module m, final String fileName) {
    try {
      boolean created = new File(getOutputDir(m), fileName).createNewFile();
      assertTrue(created);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static void createFileInOutput(Artifact a, final String name)  {
    try {
      boolean created = new File(a.getOutputPath(), name).createNewFile();
      assertTrue(created);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static class CompilationLog {
    private final Set<String> myGeneratedPaths;
    private final boolean myExternalBuildUpToDate;
    private final CompilerMessage[] myErrors;
    private final CompilerMessage[] myWarnings;

    public CompilationLog(boolean externalBuildUpToDate, List<String> generatedFilePaths, CompilerMessage[] errors,
                          CompilerMessage[] warnings) {
      myExternalBuildUpToDate = externalBuildUpToDate;
      myErrors = errors;
      myWarnings = warnings;
      myGeneratedPaths = CollectionFactory.createFilePathSet(generatedFilePaths);
    }

    public void assertUpToDate() {
      assertTrue(myExternalBuildUpToDate);
    }

    public void assertGenerated(String... expected) {
      assertSet("generated", myGeneratedPaths, expected);
    }

    public CompilerMessage[] getErrors() {
      return myErrors;
    }

    public CompilerMessage[] getWarnings() {
      return myWarnings;
    }

    private static void assertSet(String name, Set<String> actual, String[] expected) {
      for (String path : expected) {
        if (!actual.remove(path)) {
          Assert.fail("'" + path + "' is not " + name + ". " + name + ": " + new HashSet<>(actual));
        }
      }
      if (!actual.isEmpty()) {
        Assert.fail("'" + actual.iterator().next() + "' must not be " + name);
      }
    }
  }
}
