// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform.templates;

import com.intellij.CommonBundle;
import com.intellij.configurationStore.StoreUtil;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.fileTemplates.impl.FileTemplateBase;
import com.intellij.ide.util.projectWizard.ProjectTemplateFileProcessor;
import com.intellij.ide.util.projectWizard.ProjectTemplateParameterFactory;
import com.intellij.idea.ActionsBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.project.ProjectKt;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.Compressor;
import com.intellij.util.io.PathKt;
import com.intellij.util.ui.UIUtil;
import gnu.trove.TIntObjectHashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.PathMacroUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dmitry Avdeev
 */
public class SaveProjectAsTemplateAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(SaveProjectAsTemplateAction.class);
  private static final String PROJECT_TEMPLATE_XML = "project-template.xml";

  static final String FILE_HEADER_TEMPLATE_PLACEHOLDER = "<IntelliJ_File_Header>";

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = getEventProject(e);
    assert project != null;
    if (!ProjectKt.isDirectoryBased(project)) {
      Messages.showErrorDialog(project, "Project templates do not support old .ipr (file-based) format.\n" +
                                        "Please convert your project via File->Save as Directory-Based format.", CommonBundle.getErrorTitle());
      return;
    }

    final VirtualFile descriptionFile = getDescriptionFile(project, LocalArchivedTemplate.DESCRIPTION_PATH);
    final SaveProjectAsTemplateDialog dialog = new SaveProjectAsTemplateDialog(project, descriptionFile);

    if (dialog.showAndGet()) {
      final Module moduleToSave = dialog.getModuleToSave();
      final Path file = dialog.getTemplateFile();
      final String description = dialog.getDescription();

      FileDocumentManager.getInstance().saveAllDocuments();

      ProgressManager.getInstance().run(new Task.Backgroundable(project, "Saving Project as Template", true, PerformInBackgroundOption.DEAF) {
        @Override
        public void run(@NotNull final ProgressIndicator indicator) {
          saveProject(project, file, moduleToSave, description, dialog.isReplaceParameters(), indicator, shouldEscape());
        }

        @Override
        public void onSuccess() {
          AnAction newProjectAction = ActionManager.getInstance().getAction(getNewProjectActionId());
          newProjectAction.getTemplatePresentation().setText(ActionsBundle.actionText("NewDirectoryProject"));
          AnAction manageAction = ActionManager.getInstance().getAction("ManageProjectTemplates");
          Notification notification = new Notification("Project Template",
                                                       "Template Created",
                                                       FileUtilRt.getNameWithoutExtension(file.getFileName().toString()) +
                                                       " was successfully created",
                                                       NotificationType.INFORMATION);
          notification.addAction(newProjectAction);
          if (manageAction != null) {
            notification.addAction(manageAction);
          }
          notification.notify(getProject());
        }

        @Override
        public boolean shouldStartInBackground() {
          return true;
        }

        @Override
        public void onCancel() {
          PathKt.delete(file);
        }
      });
    }
  }

  public static VirtualFile getDescriptionFile(Project project, String path) {
    VirtualFile baseDir = project.getBaseDir();
    return baseDir != null ? baseDir.findFileByRelativePath(path) : null;
  }

  public static void saveProject(Project project,
                                 @NotNull Path zipFile,
                                 Module moduleToSave,
                                 String description,
                                 boolean replaceParameters,
                                 ProgressIndicator indicator,
                                 boolean shouldEscape) {
    Map<String, String> parameters = computeParameters(project, replaceParameters);
    indicator.setText("Saving project...");
    StoreUtil.saveSettings(project, true);

    indicator.setText("Processing project files...");
    VirtualFile dir = getDirectoryToSave(project, moduleToSave);
    List<LocalArchivedTemplate.RootDescription> roots = collectStructure(project, moduleToSave);
    LocalArchivedTemplate.RootDescription basePathRoot = findOrAddBaseRoot(roots, dir);
    PathKt.createDirectories(zipFile.getParent());
    try (Compressor stream = new Compressor.Zip(zipFile.toFile())) {
      writeFile(LocalArchivedTemplate.DESCRIPTION_PATH, description, project, basePathRoot.myRelativePath, stream, true);

      if (replaceParameters) {
        String text = getInputFieldsText(parameters);
        writeFile(LocalArchivedTemplate.TEMPLATE_DESCRIPTOR, text, project, basePathRoot.myRelativePath, stream, false);
      }

      String metaDescription = getTemplateMetaText(shouldEscape, roots);
      writeFile(LocalArchivedTemplate.META_TEMPLATE_DESCRIPTOR_PATH, metaDescription, project, basePathRoot.myRelativePath, stream, true);

      FileIndex index = moduleToSave == null
                        ? ProjectRootManager.getInstance(project).getFileIndex()
                        : ModuleRootManager.getInstance(moduleToSave).getFileIndex();
      MyContentIterator iterator = new MyContentIterator(indicator, stream, project, parameters, shouldEscape);
      for (LocalArchivedTemplate.RootDescription root : roots) {
        String prefix = LocalArchivedTemplate.ROOT_FILE_NAME + root.myIndex;
        VirtualFile rootFile = root.myFile;
        iterator.setRootAndPrefix(rootFile, prefix);
        index.iterateContentUnderDirectory(rootFile, iterator);
      }
    }
    catch (ProcessCanceledException ignored) { }
    catch (Exception ex) {
      LOG.error(ex);
      UIUtil.invokeLaterIfNeeded(() -> Messages.showErrorDialog(project, "Can't save project as template", "Internal Error"));
    }
  }

  private static LocalArchivedTemplate.RootDescription findOrAddBaseRoot(List<LocalArchivedTemplate.RootDescription> roots, VirtualFile dirToSave) {
    for (LocalArchivedTemplate.RootDescription root : roots) {
      if(root.myRelativePath.isEmpty()){
        return root;
      }
    }
    LocalArchivedTemplate.RootDescription root = new LocalArchivedTemplate.RootDescription(dirToSave, "", roots.size());
    roots.add(root);
    return root;
  }

  static String getFileHeaderTemplateName() {
    if (PlatformUtils.isIntelliJ()) {
      return FileTemplateBase.getQualifiedName(FileTemplateManager.FILE_HEADER_TEMPLATE_NAME, "java");
    }
    else if (PlatformUtils.isPhpStorm()) {
      return FileTemplateBase.getQualifiedName("PHP File Header", "php");
    } else if (PlatformUtils.isWebStorm()) {
      return FileTemplateBase.getQualifiedName("JavaScript File", "js");
    } else {
      throw new IllegalStateException("Provide file header template for your IDE");
    }
  }

  static String getNewProjectActionId() {
    if (PlatformUtils.isIntelliJ()) return "NewProject";
    if (PlatformUtils.isPhpStorm()) return "NewDirectoryProject";
    if (PlatformUtils.isWebStorm()) return "NewWebStormDirectoryProject";
    throw new IllegalStateException("Provide new project action id for your IDE");
  }

  private static void writeFile(String path, String text, Project project, String prefix, Compressor zip, boolean overwrite) throws IOException {
    VirtualFile descriptionFile = getDescriptionFile(project, path);
    if (descriptionFile == null) {
      zip.addFile(prefix + '/' + path, text.getBytes(StandardCharsets.UTF_8));
    }
    else if (overwrite) {
      Ref<IOException> exceptionRef = Ref.create();
      ApplicationManager.getApplication().invokeAndWait(() -> {
        try {
          WriteAction.run(() -> VfsUtil.saveText(descriptionFile, text));
        }
        catch (IOException e) {
          exceptionRef.set(e);
        }
      });
      IOException e = exceptionRef.get();
      if (e != null) throw e;
    }
  }

  public static Map<String, String> computeParameters(final Project project, boolean replaceParameters) {
    final Map<String, String> parameters = new HashMap<>();
    if (replaceParameters) {
      ApplicationManager.getApplication().runReadAction(() -> {
        for (ProjectTemplateParameterFactory extension : ProjectTemplateParameterFactory.EP_NAME.getExtensionList()) {
          String value = extension.detectParameterValue(project);
          if (value != null) {
            parameters.put(value, extension.getParameterId());
          }
        }
      });
    }
    return parameters;
  }

  public static String getEncodedContent(VirtualFile virtualFile,
                                         Project project,
                                         Map<String, String> parameters) throws IOException {
    return getEncodedContent(virtualFile, project, parameters,
                             FileTemplateBase.getQualifiedName(FileTemplateManager.FILE_HEADER_TEMPLATE_NAME, "java"), true);
  }

  private static String getEncodedContent(VirtualFile virtualFile,
                                          Project project,
                                          Map<String, String> parameters,
                                          String fileHeaderTemplateName,
                                          boolean shouldEscape) throws IOException {
    String text = VfsUtilCore.loadText(virtualFile);
    final FileTemplate template = FileTemplateManager.getInstance(project).getDefaultTemplate(fileHeaderTemplateName);
    final String templateText = template.getText();
    final Pattern pattern = FileTemplateUtil.getTemplatePattern(template, project, new TIntObjectHashMap<>());
    String result = convertTemplates(text, pattern, templateText, shouldEscape);
    result = ProjectTemplateFileProcessor.encodeFile(result, virtualFile, project);
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      result = result.replace(entry.getKey(), "${" + entry.getValue() + "}");
    }
    return result;
  }

  private static VirtualFile getDirectoryToSave(Project project, @Nullable Module module) {
    if (module == null) {
      return project.getBaseDir();
    }
    else {
      VirtualFile moduleFile = module.getModuleFile();
      assert moduleFile != null;
      return moduleFile.getParent();
    }
  }

  @NotNull
  private static List<LocalArchivedTemplate.RootDescription> collectStructure(Project project, Module moduleToSave) {
    List<LocalArchivedTemplate.RootDescription> result = new ArrayList<>();
    if (moduleToSave != null) {
      PathMacroManager macroManager = PathMacroManager.getInstance(moduleToSave);
      ModuleRootManager rootManager = ModuleRootManager.getInstance(moduleToSave);
      int i = 0;
      for (VirtualFile file : rootManager.getContentRoots()) {
        result.add(i, describeRoot(file, i, macroManager));
        i++;
      }
    }
    else {
      PathMacroManager macroManager = PathMacroManager.getInstance(project);
      ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
      int i = 0;
      for (VirtualFile file : rootManager.getContentRoots()) {
        result.add(i, describeRoot(file, i, macroManager));
        i++;
      }
    }
    return result;
  }

  private static LocalArchivedTemplate.RootDescription describeRoot(VirtualFile root, int rootIndex, PathMacroManager pathMacroManager) {
    return new LocalArchivedTemplate.RootDescription(root, getRelativePath(pathMacroManager, root), rootIndex);
  }

  private static String getRelativePath(PathMacroManager pathMacroManager, VirtualFile moduleRoot) {
    String path = pathMacroManager.collapsePath(moduleRoot.getPath());
    path = StringUtil.trimStart(path, "$" + PathMacroUtil.PROJECT_DIR_MACRO_NAME + "$");
    path = StringUtil.trimStart(path, PathMacroUtil.DEPRECATED_MODULE_DIR);
    path = StringUtil.trimStart(path, "/");
    return path;
  }

  public static String convertTemplates(String input, Pattern pattern, String template, boolean shouldEscape) {
    Matcher matcher = pattern.matcher(input);
    int start = matcher.matches() ? matcher.start(1) : -1;
    if(!shouldEscape){
      if(start == -1){
        return input;
      } else {
        return input.substring(0, start) + FILE_HEADER_TEMPLATE_PLACEHOLDER + input.substring(matcher.end(1));
      }
    }
    StringBuilder builder = new StringBuilder(input.length() + 10);
    for (int i = 0; i < input.length(); i++) {
      if (start == i) {
        builder.append(template);
        //noinspection AssignmentToForLoopParameter
        i = matcher.end(1);
      }

      char c = input.charAt(i);
      if (c == '$') {
        builder.append("#[[\\$]]#");
        continue;
      }
      if (c == '#') {
        builder.append('\\');
      }
      builder.append(c);
    }
    return builder.toString();
  }

  private static String getInputFieldsText(Map<String, String> parameters) {
    Element element = new Element(ArchivedProjectTemplate.TEMPLATE);
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      Element field = new Element(ArchivedProjectTemplate.INPUT_FIELD);
      field.setText(entry.getValue());
      field.setAttribute(ArchivedProjectTemplate.INPUT_DEFAULT, entry.getKey());
      element.addContent(field);
    }
    return JDOMUtil.writeElement(element);
  }

  private static String getTemplateMetaText(boolean shouldEncode, List<LocalArchivedTemplate.RootDescription> roots) {
    Element element = new Element(ArchivedProjectTemplate.TEMPLATE);
    element.setAttribute(LocalArchivedTemplate.UNENCODED_ATTRIBUTE, String.valueOf(!shouldEncode));

    LocalArchivedTemplate.RootDescription.writeRoots(element, roots);

    return JDOMUtil.writeElement(element);
  }

  private static boolean shouldEscape() {
    return !PlatformUtils.isPhpStorm();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = getEventProject(e);
    e.getPresentation().setEnabled(project != null && !project.isDefault());
  }

  private static class MyContentIterator implements ContentIterator {
    private static final Set<String> ALLOWED_FILES = ContainerUtil.newHashSet(
      "description.html", PROJECT_TEMPLATE_XML, LocalArchivedTemplate.TEMPLATE_META_XML, "misc.xml", "modules.xml", "workspace.xml");

    private final ProgressIndicator myIndicator;
    private final Compressor myStream;
    private final Project myProject;
    private final Map<String, String> myParameters;
    private final boolean myShouldEscape;
    private VirtualFile myRootDir;
    private String myPrefix;

    MyContentIterator(ProgressIndicator indicator, Compressor stream, Project project, Map<String, String> parameters, boolean shouldEscape) {
      myIndicator = indicator;
      myStream = stream;
      myProject = project;
      myParameters = parameters;
      myShouldEscape = shouldEscape;
    }

    public void setRootAndPrefix(VirtualFile root, String prefix) {
      myRootDir = root;
      myPrefix = prefix;
    }

    @Override
    public boolean processFile(@NotNull VirtualFile virtualFile) {
      myIndicator.checkCanceled();

      if (!virtualFile.isDirectory()) {
        String fileName = virtualFile.getName();
        myIndicator.setText2(fileName);

        String relativePath = VfsUtilCore.getRelativePath(virtualFile, myRootDir, '/');
        if (relativePath == null) {
          throw new RuntimeException("Can't find relative path for " + virtualFile + " in " + myRootDir);
        }

        boolean system = Project.DIRECTORY_STORE_FOLDER.equals(virtualFile.getParent().getName());
        if (!system || ALLOWED_FILES.contains(fileName) || fileName.endsWith(".iml")) {
          String entryName = myPrefix + '/' + relativePath;
          try {
            if (virtualFile.getFileType().isBinary() || PROJECT_TEMPLATE_XML.equals(virtualFile.getName())) {
              myStream.addFile(entryName, new File(virtualFile.getPath()));
            }
            else {
              String result = getEncodedContent(virtualFile, myProject, myParameters, getFileHeaderTemplateName(), myShouldEscape);
              myStream.addFile(entryName, result.getBytes(StandardCharsets.UTF_8));
            }
          }
          catch (IOException e) {
            LOG.error(e);
          }
        }
      }

      return true;
    }
  }
}