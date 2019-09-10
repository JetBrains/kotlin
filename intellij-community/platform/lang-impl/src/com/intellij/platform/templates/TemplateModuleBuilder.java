// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform.templates;

import com.intellij.application.options.CodeStyle;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.platform.templates.github.ZipUtil;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import org.apache.velocity.exception.VelocityException;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.PathMacroUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipInputStream;

/**
* @author Dmitry Avdeev
*/
public class TemplateModuleBuilder extends ModuleBuilder {
  private final static Logger LOG = Logger.getInstance(TemplateModuleBuilder.class);

  private final ModuleType<?> myType;
  private final List<WizardInputField<?>> myAdditionalFields;
  private final ArchivedProjectTemplate myTemplate;
  private boolean myProjectMode;

  public TemplateModuleBuilder(ArchivedProjectTemplate template, ModuleType<?> moduleType, @NotNull List<WizardInputField<?>> additionalFields) {
    myTemplate = template;
    myType = moduleType;
    myAdditionalFields = additionalFields;
  }

  @Override
  public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
    ModuleBuilder builder = myType.createModuleBuilder();
    return builder.createWizardSteps(wizardContext, modulesProvider);
  }

  @Override
  public ModuleWizardStep[] createFinishingSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
    ModuleBuilder builder = myType.createModuleBuilder();
    return builder.createFinishingSteps(wizardContext, modulesProvider);
  }

  @NotNull
  @Override
  protected List<WizardInputField<?>> getAdditionalFields() {
    return myAdditionalFields;
  }

  @Override
  public Module commitModule(@NotNull final Project project, ModifiableModuleModel model) {
    if (myProjectMode) {
      final Module[] modules = ModuleManager.getInstance(project).getModules();
      if (modules.length > 0) {
        final Module module = modules[0];
        ApplicationManager.getApplication().runWriteAction(() -> {
          try {
            setupModule(module);
          }
          catch (ConfigurationException e) {
            LOG.error(e);
          }
        });

        StartupManager.getInstance(project).registerPostStartupActivity(() -> ApplicationManager.getApplication().runWriteAction(() -> {
          try {
            ModifiableModuleModel modifiableModuleModel = ModuleManager.getInstance(project).getModifiableModel();
            modifiableModuleModel.renameModule(module, module.getProject().getName());
            modifiableModuleModel.commit();
            fixModuleName(module);
          }
          catch (ModuleWithNameAlreadyExists exists) {
            // do nothing
          }
        }));
        return module;
      }
      return null;
    }
    else {
      return super.commitModule(project, model);
    }
  }

  @NotNull
  @Override
  public String getBuilderId() {
    return myTemplate.getName();
  }

  @Override
  public ModuleType<?> getModuleType() {
    return myType;
  }

  @Override
  public Icon getNodeIcon() {
    return myTemplate.getIcon();
  }

  @Override
  public boolean isTemplateBased() {
    return true;
  }

  @NotNull
  @Override
  public Module createModule(@NotNull ModifiableModuleModel moduleModel)
    throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
    final String path = getContentEntryPath();
    final ExistingModuleLoader loader = ExistingModuleLoader.setUpLoader(getModuleFilePath());
    unzip(loader.getName(), path, true, null, true);
    Module module = loader.createModule(moduleModel);
    if (myProjectMode) {
      moduleModel.renameModule(module, module.getProject().getName());
    }
    fixModuleName(module);
    return module;
  }

  private void fixModuleName(@NotNull Module module) {
    ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
    for (WizardInputField<?> field : myAdditionalFields) {
      ProjectTemplateParameterFactory factory = WizardInputField.getFactoryById(field.getId());
      if (factory != null) {
        factory.applyResult(field.getValue(), model);
      }
    }

    applyProjectDefaults(module.getProject());

    for (ProjectTemplateParameterFactory factory : ProjectTemplateParameterFactory.EP_NAME.getExtensionList()) {
      String value = factory.getImmediateValue();
      if (value != null) {
        factory.applyResult(value, model);
      }
    }

    model.commit();

    RunManager runManager = RunManager.getInstance(module.getProject());
    for (RunConfiguration configuration : runManager.getAllConfigurationsList()) {
      if (configuration instanceof ModuleBasedConfiguration) {
        ((ModuleBasedConfiguration<?, ?>)configuration).getConfigurationModule().setModule(module);
      }
    }
  }

  private static void applyProjectDefaults(@NotNull Project project) {
    Project defaultProject = ProjectManager.getInstance().getDefaultProject();
    String charset = EncodingProjectManager.getInstance(defaultProject).getDefaultCharsetName();
    EncodingProjectManager.getInstance(project).setDefaultCharsetName(charset);

    RunnerAndConfigurationSettings selectedConfiguration = RunManager.getInstance(project).getSelectedConfiguration();
    RunManagerImpl.getInstanceImpl(defaultProject).copyTemplatesToProjectFromTemplate(project);
    RunManager.getInstance(project).setSelectedConfiguration(selectedConfiguration);
  }

  @Nullable
  private WizardInputField<?> getBasePackageField() {
    for (WizardInputField<?> field : getAdditionalFields()) {
      if (ProjectTemplateParameterFactory.IJ_BASE_PACKAGE.equals(field.getId())) {
        return field;
      }
    }
    return null;
  }

  private void unzip(final @Nullable String projectName,
                     String path,
                     final boolean isModuleMode,
                     @Nullable ProgressIndicator pI,
                     boolean reportFailuresWithDialog) {
    final WizardInputField<?> basePackage = getBasePackageField();
    try {
      final File dir = new File(path);
      class ExceptionConsumer implements Consumer<VelocityException> {
        private String myPath;
        private String myText;
        private final SmartList<Trinity<String, String, VelocityException>> myFailures = new SmartList<>();

        @Override
        public void consume(VelocityException e) {
          myFailures.add(Trinity.create(myPath, myText, e));
        }

        private void setCurrentFile(String path, String text) {
          myPath = path;
          myText = text;
        }

        private void reportFailures() {
          if (myFailures.isEmpty()) {
            return;
          }

          if(reportFailuresWithDialog) {
            String dialogMessage;
            if (myFailures.size() == 1) {
              dialogMessage = "Failed to decode file \'" + myFailures.get(0).getFirst() + "\'";
            }
            else {
              StringBuilder dialogMessageBuilder = new StringBuilder();
              dialogMessageBuilder.append("Failed to decode files: \n");
              for (Trinity<String, String, VelocityException> failure : myFailures) {
                dialogMessageBuilder.append(failure.getFirst()).append("\n");
              }
              dialogMessage = dialogMessageBuilder.toString();
            }
            Messages.showErrorDialog(dialogMessage, "Decoding Template");
          }

          StringBuilder reportBuilder = new StringBuilder();
          for (Trinity<String, String, VelocityException> failure : myFailures) {
            reportBuilder.append("File: ").append(failure.getFirst()).append("\n");
            reportBuilder.append("Exception:\n").append(ExceptionUtil.getThrowableText(failure.getThird())).append("\n");
            reportBuilder.append("File content:\n\'").append(failure.getSecond()).append("\'\n");
            reportBuilder.append("\n===========================================\n");
          }

          LOG.error("Cannot decode files in template", (Throwable)null, new Attachment("Files in template", reportBuilder.toString()));
        }
      }
      ExceptionConsumer consumer = new ExceptionConsumer();

      List<File> filesToRefresh = new ArrayList<>();
      myTemplate.processStream(new ArchivedProjectTemplate.StreamProcessor<Void>() {
        @Override
        public Void consume(@NotNull ZipInputStream stream) throws IOException {
          ZipUtil.unzip(ProgressManager.getInstance().getProgressIndicator(), dir, stream, path1 -> {
            if (isModuleMode && path1.contains(Project.DIRECTORY_STORE_FOLDER)) {
              return null;
            }
            if (basePackage != null) {
              return path1.replace(getPathFragment(basePackage.getDefaultValue()), getPathFragment(basePackage.getValue()));
            }
            return path1;
          }, new ZipUtil.ContentProcessor() {
            @Override
            public byte[] processContent(byte[] content, File file) throws IOException {
              if(pI != null){
                pI.checkCanceled();
              }
              FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(file.getName());
              String text = new String(content, StandardCharsets.UTF_8);
              consumer.setCurrentFile(file.getName(), text);
              return fileType.isBinary() ? content : processTemplates(projectName, text, file, consumer);
            }
          }, true);

          myTemplate.handleUnzippedDirectories(dir, filesToRefresh);
          return null;
        }
      });

      if (pI != null) {
        pI.setText("Refreshing...");
      }

      String iml = ContainerUtil.find(ObjectUtils.chooseNotNull(dir.list(), ArrayUtilRt.EMPTY_STRING_ARRAY), s -> s.endsWith(".iml"));
      if (isModuleMode) {
        File from = new File(path, Objects.requireNonNull(iml));
        File to = new File(getModuleFilePath());
        if (!from.renameTo(to)) {
          throw new IOException("Can't rename " + from + " to " + to);
        }
      }

      RefreshQueue refreshQueue = RefreshQueue.getInstance();
      LOG.assertTrue(!filesToRefresh.isEmpty());
      for (File file : filesToRefresh) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        if (virtualFile == null) {
          throw new IOException("Can't find " + file);
        }
        refreshQueue.refresh(false, true, null, virtualFile);
      }

      consumer.reportFailures();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  private static String getPathFragment(@NotNull String value) {
    return "/" + value.replace('.', '/') + "/";
  }

  @SuppressWarnings("UseOfPropertiesAsHashtable")
  @Nullable
  private byte[] processTemplates(@Nullable String projectName, String content, File file, Consumer<? super VelocityException> exceptionConsumer)
    throws IOException {
    String patchedContent = content;
    if (!(myTemplate instanceof LocalArchivedTemplate) || ((LocalArchivedTemplate)myTemplate).isEscaped()) {
      for (WizardInputField<?> field : myAdditionalFields) {
        if (!field.acceptFile(file)) {
          return null;
        }
      }
      Properties properties = FileTemplateManager.getDefaultInstance().getDefaultProperties();
      for (WizardInputField<?> field : myAdditionalFields) {
        properties.putAll(field.getValues());
      }
      if (projectName != null) {
        properties.put(ProjectTemplateParameterFactory.IJ_PROJECT_NAME, projectName);
      }
      String merged = FileTemplateUtil.mergeTemplate(properties, content, true, exceptionConsumer);
      patchedContent = merged.replace("\\$", "$").replace("\\#", "#");
    }
    else {
      int i = content.indexOf(SaveProjectAsTemplateAction.FILE_HEADER_TEMPLATE_PLACEHOLDER);
      if (i != -1) {
        final FileTemplate template =
          FileTemplateManager.getDefaultInstance().getDefaultTemplate(SaveProjectAsTemplateAction.getFileHeaderTemplateName());
        Properties properties = FileTemplateManager.getDefaultInstance().getDefaultProperties();
        String templateText = template.getText(properties);
        patchedContent = patchedContent.substring(0, i) +
                         templateText +
                         patchedContent.substring(i + SaveProjectAsTemplateAction.FILE_HEADER_TEMPLATE_PLACEHOLDER.length());
      }
    }
    return StringUtilRt.convertLineSeparators(patchedContent, CodeStyle.getDefaultSettings().getLineSeparator()).
      getBytes(StandardCharsets.UTF_8);
  }

  @Nullable
  @Override
  public Project createProject(String name, @NotNull String path) {
    Path baseDir = Paths.get(path);
    LOG.assertTrue(Files.isDirectory(baseDir));

    List<Path> children;
    try (DirectoryStream<Path> childrenIterator = Files.newDirectoryStream(baseDir)) {
      children = ContainerUtil.collect(childrenIterator.iterator());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    boolean isSomehowOverwriting = children.size() > 1 ||
                                   (children.size() == 1 && !PathMacroUtil.DIRECTORY_STORE_NAME.equals(children.get(0).getFileName().toString()));

    return ProgressManager.getInstance().run(new Task.WithResult<Project, RuntimeException>(null, "Applying Template", true) {
      @Override
      public Project compute(@NotNull ProgressIndicator indicator) {
        try {
          myProjectMode = true;
          unzip(name, path, false, indicator, false);
          return ProjectManagerEx.getInstanceEx().loadProject(baseDir);
        }
        finally {
          cleanup();
          if (indicator.isCanceled() && !isSomehowOverwriting) {
            try {
              FileUtil.delete(baseDir);
            }
            catch (IOException e) {
              LOG.error(e);
            }
          }
        }
      }
    });
  }
}
