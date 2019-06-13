// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.icons.AllIcons;
import com.intellij.ide.FileIconProvider;
import com.intellij.ide.navigationToolbar.AbstractNavBarModelExtension;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.PerFileMappings;
import com.intellij.lang.PerFileMappingsBase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessExtension;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;
import com.intellij.psi.LanguageSubstitutors;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UseScopeEnlarger;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.IndexableSetContributor;
import com.intellij.util.indexing.LightDirectoryIndex;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

@State(name = "ScratchFileService", storages = @Storage(value = "scratches.xml", roamingType = RoamingType.DISABLED))
public class ScratchFileServiceImpl extends ScratchFileService implements PersistentStateComponent<Element>, Disposable {
  private static final RootType NO_ROOT_TYPE = new RootType("", "NO_ROOT_TYPE") {};

  private final LightDirectoryIndex<RootType> myIndex;
  private final MyLanguages myScratchMapping = new MyLanguages();

  protected ScratchFileServiceImpl() {
    Disposer.register(this, myScratchMapping);
    myIndex = new LightDirectoryIndex<>(ApplicationManager.getApplication(), NO_ROOT_TYPE, index -> {
      LocalFileSystem fileSystem = LocalFileSystem.getInstance();
      for (RootType r : RootType.getAllRootTypes()) {
        index.putInfo(fileSystem.findFileByPath(getRootPath(r)), r);
      }
    });
    initFileOpenedListener();
  }

  @NotNull
  @Override
  public String getRootPath(@NotNull RootType rootType) {
    return getScratchesPath() + "/" + rootType.getId();
  }

  @Nullable
  @Override
  public RootType getRootType(@Nullable VirtualFile file) {
    if (file == null || !file.isInLocalFileSystem()) return null;
    VirtualFile directory = file.isDirectory() ? file : file.getParent();
    RootType result = myIndex.getInfoForFile(directory);
    return result == NO_ROOT_TYPE ? null : result;
  }

  private void initFileOpenedListener() {
    final FileEditorManagerListener editorListener = new FileEditorManagerListener() {
      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (!isEditable(file)) return;
        RootType rootType = getRootType(file);
        if (rootType == null) return;
        rootType.fileOpened(file, source);
      }

      @Override
      public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (Boolean.TRUE.equals(file.getUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN))) return;
        if (!isEditable(file)) return;

        RootType rootType = getRootType(file);
        if (rootType == null) return;
        rootType.fileClosed(file, source);
      }

      boolean isEditable(@NotNull VirtualFile file) {
        return FileDocumentManager.getInstance().getDocument(file) != null;
      }
    };

    // handle all previously opened projects (as we are service, lazily created)
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      FileEditorManager editorManager = FileEditorManager.getInstance(project);
      for (VirtualFile virtualFile : editorManager.getOpenFiles()) {
        editorListener.fileOpened(editorManager, virtualFile);
      }
    }
    ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, editorListener);
  }

  @NotNull
  @SystemIndependent
  static String getScratchesPath() {
    return FileUtil.toSystemIndependentName(PathManager.getScratchPath());
  }

  @NotNull
  @Override
  public PerFileMappings<Language> getScratchesMapping() {
    return myScratchMapping;
  }

  @Nullable
  @Override
  public Element getState() {
    return myScratchMapping.getState();
  }

  @Override
  public void loadState(@NotNull Element state) {
    myScratchMapping.loadState(state);
  }

  @Override
  public void dispose() {
  }

  private static class MyLanguages extends PerFileMappingsBase<Language> {

    @Override
    public List<Language> getAvailableValues() {
      return LanguageUtil.getFileLanguages();
    }

    @Nullable
    @Override
    protected String serialize(Language language) {
      return language.getID();
    }

    @Nullable
    @Override
    protected Language handleUnknownMapping(VirtualFile file, String value) {
      return PlainTextLanguage.INSTANCE;
    }
  }

  public static class Substitutor extends LanguageSubstitutor {
    @Nullable
    @Override
    public Language getLanguage(@NotNull VirtualFile file, @NotNull Project project) {
      return substituteLanguage(project, file);
    }

    @Nullable
    public static Language substituteLanguage(@NotNull Project project, @NotNull VirtualFile file) {
      RootType rootType = ScratchFileService.getInstance().getRootType(file);
      if (rootType == null) return null;
      Language language = rootType.substituteLanguage(project, file);
      Language adjusted = language != null ? language : getLanguageByFileName(file);
      return adjusted != null && adjusted != ScratchFileType.INSTANCE.getLanguage() ?
             LanguageSubstitutors.INSTANCE.substituteLanguage(adjusted, file, project) : adjusted;
    }
  }

  public static class Highlighter implements SyntaxHighlighterProvider {
    @Override
    @Nullable
    public SyntaxHighlighter create(@NotNull FileType fileType, @Nullable Project project, @Nullable VirtualFile file) {
      if (project == null || file == null || !(fileType instanceof ScratchFileType)) return null;

      Language language = LanguageUtil.getLanguageForPsi(project, file);
      return language == null ? null : SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, file);
    }
  }

  public static class FilePresentation implements FileIconProvider, EditorTabTitleProvider, DumbAware {

    @Nullable
    @Override
    public Icon getIcon(@NotNull VirtualFile file, @Iconable.IconFlags int flags, @Nullable Project project) {
      if (project == null || file.isDirectory()) return null;
      RootType rootType = ScratchFileService.getInstance().getRootType(file);
      if (rootType == null) return null;
      return rootType.substituteIcon(project, file);
    }

    @Nullable
    @Override
    public String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file) {
      RootType rootType = ScratchFileService.getInstance().getRootType(file);
      if (rootType == null) return null;
      return rootType.substituteName(project, file);
    }
  }

  public static class AccessExtension implements NonProjectFileWritingAccessExtension {

    @Override
    public boolean isWritable(@NotNull VirtualFile file) {
      return FileTypeRegistry.getInstance().isFileOfType(file, ScratchFileType.INSTANCE);
    }
  }

  public static class NavBarExtension extends AbstractNavBarModelExtension {

    @Nullable
    @Override
    public Icon getIcon(Object object) {
      VirtualFile file = object instanceof PsiFileSystemItem ? ((PsiFileSystemItem)object).getVirtualFile() : null;
      if (file == null) return null;
      RootType rootType = ScratchFileService.getInstance().getRootType(file);
      if (rootType == null) return null;
      Icon icon = rootType.substituteIcon(((PsiFileSystemItem)object).getProject(), file);
      return icon == null && file.isDirectory() ? AllIcons.Nodes.Folder : icon;
    }

    @Nullable
    @Override
    public String getPresentableText(Object object) {
      if (!(object instanceof PsiElement)) return null;
      Project project = ((PsiElement)object).getProject();
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile((PsiElement)object);
      if (virtualFile == null || !virtualFile.isValid()) return null;
      RootType rootType = ScratchFileService.getInstance().getRootType(virtualFile);
      if (rootType == null) return null;
      if (virtualFile.isDirectory() && additionalRoots(project).contains(virtualFile)) {
        return rootType.getDisplayName();
      }
      return rootType.substituteName(project, virtualFile);
    }

    @NotNull
    @Override
    public Collection<VirtualFile> additionalRoots(Project project) {
      Set<VirtualFile> result = new LinkedHashSet<>();
      LocalFileSystem fileSystem = LocalFileSystem.getInstance();
      ScratchFileService app = ScratchFileService.getInstance();
      for (RootType r : RootType.getAllRootTypes()) {
        ContainerUtil.addIfNotNull(result, fileSystem.findFileByPath(app.getRootPath(r)));
      }
      return result;
    }
  }

  @Override
  public VirtualFile findFile(@NotNull RootType rootType, @NotNull String pathName, @NotNull Option option) throws IOException {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    String fullPath = getRootPath(rootType) + "/" + pathName;
    if (option != Option.create_new_always) {
      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(fullPath);
      if (file != null && !file.isDirectory()) return file;
      if (option == Option.existing_only) return null;
    }
    String ext = PathUtil.getFileExtension(pathName);
    String fileNameExt = PathUtil.getFileName(pathName);
    String fileName = StringUtil.trimEnd(fileNameExt, ext == null ? "" : "." + ext);
    return WriteAction.compute(() -> {
      VirtualFile dir = VfsUtil.createDirectories(PathUtil.getParentPath(fullPath));
      if (option == Option.create_new_always) {
        return VfsUtil.createChildSequent(LocalFileSystem.getInstance(), dir, fileName, StringUtil.notNullize(ext));
      }
      else {
        return dir.findOrCreateChildData(LocalFileSystem.getInstance(), fileNameExt);
      }
    });
  }

  @Nullable
  private static Language getLanguageByFileName(@Nullable VirtualFile file) {
    return file == null ? null : LanguageUtil.getFileTypeLanguage(FileTypeManager.getInstance().getFileTypeByFileName(file.getNameSequence()));
  }

  public static class UseScopeExtension extends UseScopeEnlarger {
    @Nullable
    @Override
    public SearchScope getAdditionalUseScope(@NotNull PsiElement element) {
      SearchScope useScope = element.getUseScope();
      if (useScope instanceof LocalSearchScope) return null;
      return ScratchesSearchScope.getScratchesScope(element.getProject());
    }
  }

  public static class UsageTypeExtension implements UsageTypeProvider {
    private static final ConcurrentMap<RootType, UsageType> ourUsageTypes =
      ConcurrentFactoryMap.createMap(key -> new UsageType("Usage in " + key.getDisplayName()));

    @Nullable
    @Override
    public UsageType getUsageType(PsiElement element) {
      VirtualFile file = PsiUtilCore.getVirtualFile(element);
      RootType rootType = file != null && FileTypeRegistry.getInstance().isFileOfType(file, ScratchFileType.INSTANCE) ?
                          ScratchFileService.getInstance().getRootType(file) : null;
      return rootType == null ? null : ourUsageTypes.get(rootType);
    }
  }

  public static class IndexSetContributor extends IndexableSetContributor {

    @NotNull
    @Override
    public Set<VirtualFile> getAdditionalRootsToIndex() {
      ScratchFileService instance = ScratchFileService.getInstance();
      LocalFileSystem fileSystem = LocalFileSystem.getInstance();
      HashSet<VirtualFile> result = new HashSet<>();
      for (RootType rootType : RootType.getAllRootTypes()) {
        if (rootType.isHidden()) continue;
        ContainerUtil.addIfNotNull(result, fileSystem.findFileByPath(instance.getRootPath(rootType)));
      }
      return result;
    }
  }
}