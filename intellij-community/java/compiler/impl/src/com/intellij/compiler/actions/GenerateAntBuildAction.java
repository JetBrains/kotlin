// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.actions;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.compiler.ant.*;
import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.configurationStore.StoreUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GenerateAntBuildAction extends CompileActionBase {
  @NonNls private static final String XML_EXTENSION = ".xml";

  @Override
  protected void doAction(DataContext dataContext, final Project project) {
    ((CompilerConfigurationImpl)CompilerConfiguration.getInstance(project)).convertPatterns();
    final GenerateAntBuildDialog dialog = new GenerateAntBuildDialog(project);
    if (dialog.showAndGet()) {
      final String[] names = dialog.getRepresentativeModuleNames();
      final GenerationOptionsImpl[] genOptions = new GenerationOptionsImpl[1];
      if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
        (Runnable)() -> genOptions[0] = ReadAction
          .compute(() -> new GenerationOptionsImpl(project, dialog.isGenerateSingleFileBuild(), dialog.isFormsCompilationEnabled(),
                                                   dialog.isBackupFiles(), dialog.isForceTargetJdk(), dialog.isRuntimeClasspathInlined(),
                                                   dialog.isIdeaHomeGenerated(), names, dialog.getOutputFileName())), "Analyzing project structure...", true, project)) {
        return;
      }
      if (!validateGenOptions(project, genOptions[0])) {
        return;
      }
      generate(project, genOptions[0]);
    }
  }

  /**
   * Validate generation options and notify user about possible problems
   *
   * @param project    a context project
   * @param genOptions a generation optiosn
   * @return true if the generator should proceed with current options or if there is not conflict.
   */
  private static boolean validateGenOptions(Project project, GenerationOptionsImpl genOptions) {
    final Collection<String> EMPTY = Collections.emptyList();
    Collection<String> conflicts = EMPTY;
    for (ModuleChunk chunk : genOptions.getModuleChunks()) {
      final ChunkCustomCompilerExtension[] customeCompilers = chunk.getCustomCompilers();
      if (customeCompilers.length > 1) {
        if (conflicts == EMPTY) {
          conflicts = new LinkedList<>();
        }
        conflicts.add(chunk.getName());
      }
    }
    if (!conflicts.isEmpty()) {
      StringBuilder msg = new StringBuilder();
      for (String conflictingChunk : conflicts) {
        msg.append(CompilerBundle.message("generate.ant.build.custom.compiler.conflict.message.row", conflictingChunk));
      }
      int rc = Messages
        .showOkCancelDialog(project, CompilerBundle.message("generate.ant.build.custom.compiler.conflict.message", msg.toString()),
                            CompilerBundle.message("generate.ant.build.custom.compiler.conflict.title"), Messages.getErrorIcon());
      if (rc != Messages.OK) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabled(e.getProject() != null);
  }

  private void generate(@NotNull Project project, final GenerationOptions genOptions) {
    StoreUtil.saveDocumentsAndProjectSettings(project);
    final List<File> filesToRefresh = new ArrayList<>();
    final IOException[] _ex = new IOException[]{null};
    final List<File> _generated = new ArrayList<>();

    try {
      if (genOptions.generateSingleFile) {
        final File projectBuildFileDestDir = VfsUtilCore.virtualToIoFile(project.getBaseDir());
        final File destFile = new File(projectBuildFileDestDir, genOptions.getBuildFileName());
        final File propertiesFile = new File(projectBuildFileDestDir, genOptions.getPropertiesFileName());

        ensureFilesWritable(project, new File[]{destFile, propertiesFile});
      }
      else {
        final List<File> allFiles = new ArrayList<>();

        final File projectBuildFileDestDir = VfsUtilCore.virtualToIoFile(project.getBaseDir());
        allFiles.add(new File(projectBuildFileDestDir, genOptions.getBuildFileName()));
        allFiles.add(new File(projectBuildFileDestDir, genOptions.getPropertiesFileName()));

        final ModuleChunk[] chunks = genOptions.getModuleChunks();
        for (final ModuleChunk chunk : chunks) {
          final File chunkBaseDir = BuildProperties.getModuleChunkBaseDir(chunk);
          allFiles.add(new File(chunkBaseDir, BuildProperties.getModuleChunkBuildFileName(chunk) + XML_EXTENSION));
        }

        ensureFilesWritable(project, allFiles.toArray(new File[0]));
      }

      new Task.Modal(project, CompilerBundle.message("generate.ant.build.title"), false) {
        @Override
        public void run(@NotNull final ProgressIndicator indicator) {
          indicator.setIndeterminate(true);
          indicator.setText(CompilerBundle.message("generate.ant.build.progress.message"));
          try {
            final File[] generated;
            if (genOptions.generateSingleFile) {
              generated = generateSingleFileBuild(project, genOptions, filesToRefresh);
            }
            else {
              generated = generateMultipleFileBuild(project, genOptions, filesToRefresh);
            }
            if (generated != null) {
              ContainerUtil.addAll(_generated, generated);
            }
          }
          catch (IOException e) {
            _ex[0] = e;
          }
        }
      }.queue();

    }
    catch (IOException e) {
      _ex[0] = e;
    }

    if (_ex[0] != null) {
      Messages.showErrorDialog(project, CompilerBundle.message("error.ant.files.generate.failed", _ex[0].getMessage()),
                               CompilerBundle.message("generate.ant.build.title"));
    }
    else {
      StringBuilder filesString = new StringBuilder();
      for (int idx = 0; idx < _generated.size(); idx++) {
        final File file = _generated.get(idx);
        if (idx > 0) {
          filesString.append(",\n");
        }
        filesString.append(file.getPath());
      }
      Messages.showInfoMessage(project, CompilerBundle.message("message.ant.files.generated.ok", filesString.toString()),
                               CompilerBundle.message("generate.ant.build.title"));
    }

    if (filesToRefresh.size() > 0) {
      CompilerUtil.refreshIOFiles(filesToRefresh);
    }
  }

  private boolean backup(final File file, final Project project, GenerationOptions genOptions, List<? super File> filesToRefresh) {
    if (!genOptions.backupPreviouslyGeneratedFiles || !file.exists()) {
      return true;
    }
    final String path = file.getPath();
    final int extensionIndex = path.lastIndexOf(".");
    final String extension = path.substring(extensionIndex);
    //noinspection HardCodedStringLiteral
    final String backupPath = path.substring(0, extensionIndex) +
                              "_" +
                              new Date(file.lastModified()).toString().replaceAll("\\s+", "_").replaceAll(":", "-") +
                              extension;
    final File backupFile = new File(backupPath);
    boolean ok;
    try {
      FileUtil.rename(file, backupFile);
      ok = true;
    }
    catch (IOException e) {
      Messages.showErrorDialog(project, CompilerBundle.message("error.ant.files.backup.failed", path),
                               CompilerBundle.message("generate.ant.build.title"));
      ok = false;
    }
    filesToRefresh.add(backupFile);
    return ok;
  }

  private File[] generateSingleFileBuild(Project project, GenerationOptions genOptions, List<? super File> filesToRefresh) throws IOException {
    final File projectBuildFileDestDir = VfsUtilCore.virtualToIoFile(project.getBaseDir());
    projectBuildFileDestDir.mkdirs();
    final File destFile = new File(projectBuildFileDestDir, genOptions.getBuildFileName());
    final File propertiesFile = new File(projectBuildFileDestDir, genOptions.getPropertiesFileName());

    if (!backup(destFile, project, genOptions, filesToRefresh)) {
      return null;
    }
    if (!backup(propertiesFile, project, genOptions, filesToRefresh)) {
      return null;
    }

    generateSingleFileBuild(project, genOptions, destFile, propertiesFile);

    filesToRefresh.add(destFile);
    filesToRefresh.add(propertiesFile);
    return new File[]{destFile, propertiesFile};
  }

  public static void generateSingleFileBuild(final Project project,
                                             final GenerationOptions genOptions,
                                             final File buildxmlFile,
                                             final File propertiesFile) throws IOException {
    FileUtil.createIfDoesntExist(buildxmlFile);
    FileUtil.createIfDoesntExist(propertiesFile);
    try (PrintWriter dataOutput = makeWriter(buildxmlFile)) {
      new SingleFileProjectBuild(project, genOptions).generate(dataOutput);
    }
    try (PrintWriter propertiesOut = makeWriter(propertiesFile)) {
      new PropertyFileGeneratorImpl(project, genOptions).generate(propertiesOut);
    }
  }

  /**
   * Create print writer over file with UTF-8 encoding
   *
   * @param buildxmlFile a file to write to
   * @return a created print writer
   * @throws UnsupportedEncodingException if endcoding not found
   * @throws FileNotFoundException        if file not found
   */
  private static PrintWriter makeWriter(final File buildxmlFile) throws UnsupportedEncodingException, FileNotFoundException {
    return new PrintWriter(new OutputStreamWriter(new FileOutputStream(buildxmlFile), StandardCharsets.UTF_8));
  }

  private void ensureFilesWritable(Project project, File[] files) throws IOException {
    final List<VirtualFile> toCheck = new ArrayList<>(files.length);
    final LocalFileSystem lfs = LocalFileSystem.getInstance();
    for (File file : files) {
      final VirtualFile vFile = lfs.findFileByIoFile(file);
      if (vFile != null) {
        toCheck.add(vFile);
      }
    }
    final ReadonlyStatusHandler.OperationStatus status =
      ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(toCheck);
    if (status.hasReadonlyFiles()) {
      throw new IOException(status.getReadonlyFilesMessage());
    }
  }

  public File[] generateMultipleFileBuild(Project project, GenerationOptions genOptions, List<? super File> filesToRefresh) throws IOException {
    final File projectBuildFileDestDir = VfsUtilCore.virtualToIoFile(project.getBaseDir());
    projectBuildFileDestDir.mkdirs();
    final List<File> generated = new ArrayList<>();
    final File projectBuildFile = new File(projectBuildFileDestDir, genOptions.getBuildFileName());
    final File propertiesFile = new File(projectBuildFileDestDir, genOptions.getPropertiesFileName());
    final ModuleChunk[] chunks = genOptions.getModuleChunks();

    final File[] chunkFiles = new File[chunks.length];
    for (int idx = 0; idx < chunks.length; idx++) {
      final ModuleChunk chunk = chunks[idx];
      final File chunkBaseDir = BuildProperties.getModuleChunkBaseDir(chunk);
      chunkFiles[idx] = new File(chunkBaseDir, BuildProperties.getModuleChunkBuildFileName(chunk) + XML_EXTENSION);
    }

    if (!backup(projectBuildFile, project, genOptions, filesToRefresh)) {
      return null;
    }
    if (!backup(propertiesFile, project, genOptions, filesToRefresh)) {
      return null;
    }

    FileUtil.createIfDoesntExist(projectBuildFile);
    try (PrintWriter mainDataOutput = makeWriter(projectBuildFile)) {
      final MultipleFileProjectBuild build = new MultipleFileProjectBuild(project, genOptions);
      build.generate(mainDataOutput);
      generated.add(projectBuildFile);

      // the sequence in which modules are imported is important cause output path properties for dependent modules should be defined first

      for (int idx = 0; idx < chunks.length; idx++) {
        final ModuleChunk chunk = chunks[idx];
        final File chunkBuildFile = chunkFiles[idx];
        final File chunkBaseDir = chunkBuildFile.getParentFile();
        if (chunkBaseDir != null) {
          chunkBaseDir.mkdirs();
        }
        final boolean moduleBackupOk = backup(chunkBuildFile, project, genOptions, filesToRefresh);
        if (!moduleBackupOk) {
          return null;
        }

        FileUtil.createIfDoesntExist(chunkBuildFile);
        try (PrintWriter out = makeWriter(chunkBuildFile)) {
          new ModuleChunkAntProject(project, chunk, genOptions).generate(out);
          generated.add(chunkBuildFile);
        }
      }
    }
    // properties
    try (PrintWriter propertiesOut = makeWriter(propertiesFile)) {
      new PropertyFileGeneratorImpl(project, genOptions).generate(propertiesOut);
      generated.add(propertiesFile);
    }

    filesToRefresh.addAll(generated);
    return generated.toArray(new File[0]);
  }

}
