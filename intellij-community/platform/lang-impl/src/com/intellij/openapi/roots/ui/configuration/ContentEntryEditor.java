/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.DirectoryIndexExcludePolicy;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class ContentEntryEditor implements ContentRootPanel.ActionCallback {
  private boolean myIsSelected;
  private ContentRootPanel myContentRootPanel;
  private JPanel myMainPanel;
  protected EventDispatcher<ContentEntryEditorListener> myEventDispatcher;
  private final String myContentEntryUrl;
  private final List<ModuleSourceRootEditHandler<?>> myEditHandlers;

  public interface ContentEntryEditorListener extends EventListener{

    void editingStarted(@NotNull ContentEntryEditor editor);
    void beforeEntryDeleted(@NotNull ContentEntryEditor editor);
    void sourceFolderAdded(@NotNull ContentEntryEditor editor, SourceFolder folder);
    void sourceFolderRemoved(@NotNull ContentEntryEditor editor, VirtualFile file);
    void folderExcluded(@NotNull ContentEntryEditor editor, VirtualFile file);
    void folderIncluded(@NotNull ContentEntryEditor editor, String fileUrl);
    void navigationRequested(@NotNull ContentEntryEditor editor, VirtualFile file);
    void sourceRootPropertiesChanged(@NotNull ContentEntryEditor editor, @NotNull SourceFolder folder);
  }

  public ContentEntryEditor(String url, List<ModuleSourceRootEditHandler<?>> editHandlers) {
    myContentEntryUrl = url;
    myEditHandlers = editHandlers;
  }

  protected final List<ModuleSourceRootEditHandler<?>> getEditHandlers() {
    return myEditHandlers;
  }

  public String getContentEntryUrl() {
    return myContentEntryUrl;
  }

  public void initUI() {
    myMainPanel = new JPanel(new BorderLayout());
    myMainPanel.setOpaque(false);
    myMainPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        myEventDispatcher.getMulticaster().editingStarted(ContentEntryEditor.this);
      }
      @Override
      public void mouseEntered(MouseEvent e) {
        if (!myIsSelected) {
          highlight(true);
        }
      }
      @Override
      public void mouseExited(MouseEvent e) {
        if (!myIsSelected) {
          highlight(false);
        }
      }
    });
    myEventDispatcher = EventDispatcher.create(ContentEntryEditorListener.class);
    setSelected(false);
    update();
  }

  @Nullable
  protected ContentEntry getContentEntry() {
    final ModifiableRootModel model = getModel();
    if (model != null) {
      final ContentEntry[] entries = model.getContentEntries();
      for (ContentEntry entry : entries) {
        if (entry.getUrl().equals(myContentEntryUrl)) return entry;
      }
    }

    return null;
  }

  protected abstract ModifiableRootModel getModel();

  @Override
  public void deleteContentEntry() {
    final String path = FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(myContentEntryUrl));
    final int answer = Messages.showYesNoDialog(ProjectBundle.message("module.paths.remove.content.prompt", path),
                                                ProjectBundle.message("module.paths.remove.content.title"), Messages.getQuestionIcon());
    if (answer != Messages.YES) { // no
      return;
    }
    myEventDispatcher.getMulticaster().beforeEntryDeleted(this);
    final ContentEntry entry = getContentEntry();
    if (entry != null) {
      getModel().removeContentEntry(entry);
    }
  }

  @Override
  public void deleteContentFolder(ContentEntry contentEntry, ContentFolder folder) {
    if (folder instanceof SourceFolder) {
      removeSourceFolder((SourceFolder)folder);
      update();
    }
    else if (folder instanceof ExcludeFolder) {
      removeExcludeFolder(folder.getUrl());
      update();
    }

  }

  @Override
  public void navigateFolder(ContentEntry contentEntry, ContentFolder contentFolder) {
    final VirtualFile file = contentFolder.getFile();
    if (file != null) { // file can be deleted externally
      myEventDispatcher.getMulticaster().navigationRequested(this, file);
    }
  }

  @Override
  public void onSourceRootPropertiesChanged(@NotNull SourceFolder folder) {
    update();
    myEventDispatcher.getMulticaster().sourceRootPropertiesChanged(this, folder);
  }

  public void addContentEntryEditorListener(ContentEntryEditorListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeContentEntryEditorListener(ContentEntryEditorListener listener) {
    myEventDispatcher.removeListener(listener);
  }

  public void setSelected(boolean isSelected) {
    if (myIsSelected != isSelected) {
      highlight(isSelected);
      myIsSelected = isSelected;
    }
  }

  private void highlight(boolean selected) {
    if (myContentRootPanel != null) {
      myContentRootPanel.setSelected(selected);
    }
  }

  public JComponent getComponent() {
    return myMainPanel;
  }

  public void update() {
    if (myContentRootPanel != null) {
      myMainPanel.remove(myContentRootPanel);
    }
    myContentRootPanel = createContentRootPane();
    myContentRootPanel.initUI();
    myContentRootPanel.setSelected(myIsSelected);
    myMainPanel.add(myContentRootPanel, BorderLayout.CENTER);
    myMainPanel.revalidate();
  }

  protected ContentRootPanel createContentRootPane() {
    return new ContentRootPanel(this, myEditHandlers) {
      @Override
      protected ContentEntry getContentEntry() {
        return ContentEntryEditor.this.getContentEntry();
      }
    };
  }

  @Nullable
  public SourceFolder addSourceFolder(@NotNull final VirtualFile file, boolean isTestSource, String packagePrefix) {
    return addSourceFolder(file, isTestSource ? JavaSourceRootType.TEST_SOURCE : JavaSourceRootType.SOURCE,
                           JpsJavaExtensionService.getInstance().createSourceRootProperties(packagePrefix));
  }

  @Nullable
  public <P extends JpsElement> SourceFolder addSourceFolder(@NotNull final VirtualFile file, final JpsModuleSourceRootType<P> rootType,
                                                             final P properties) {
    final ContentEntry contentEntry = getContentEntry();
    if (contentEntry != null) {
      final SourceFolder sourceFolder = contentEntry.addSourceFolder(file, rootType, properties);
      myEventDispatcher.getMulticaster().sourceFolderAdded(this, sourceFolder);
      update();
      return sourceFolder;
    }

    return null;
  }

  @Nullable
  protected SourceFolder doAddSourceFolder(@NotNull final VirtualFile file, final boolean isTestSource) {
    final ContentEntry contentEntry = getContentEntry();
    return contentEntry != null ? contentEntry.addSourceFolder(file, isTestSource) : null;
  }

  public void removeSourceFolder(@NotNull final SourceFolder sourceFolder) {
    try {
      doRemoveSourceFolder(sourceFolder);
    }
    finally {
      myEventDispatcher.getMulticaster().sourceFolderRemoved(this, sourceFolder.getFile());
      update();
    }
  }

  protected void doRemoveSourceFolder(@NotNull final SourceFolder sourceFolder) {
    final ContentEntry contentEntry = getContentEntry();
    if (contentEntry != null) contentEntry.removeSourceFolder(sourceFolder);
  }

  @Nullable
  public ExcludeFolder addExcludeFolder(@NotNull final VirtualFile file) {
    try {
      return doAddExcludeFolder(file);
    }
    finally {
      myEventDispatcher.getMulticaster().folderExcluded(this, file);
      update();
    }
  }

  @Nullable
  protected ExcludeFolder doAddExcludeFolder(@NotNull final VirtualFile file) {
    final ContentEntry contentEntry = getContentEntry();
    return contentEntry != null ? contentEntry.addExcludeFolder(file) : null;
  }

  public void removeExcludeFolder(@NotNull final String excludeRootUrl) {
    try {
      doRemoveExcludeFolder(excludeRootUrl);
    }
    finally {
      myEventDispatcher.getMulticaster().folderIncluded(this, excludeRootUrl);
      update();
    }
  }

  protected void doRemoveExcludeFolder(@NotNull final String excludeRootUrl) {
    final ContentEntry contentEntry = getContentEntry();
    if (contentEntry != null) {
      contentEntry.removeExcludeFolder(excludeRootUrl);
    }
  }

  @Nullable
  public JpsModuleSourceRootType<?> getRootType(@NotNull VirtualFile file) {
    SourceFolder folder = getSourceFolder(file);
    return folder != null ? folder.getRootType() : null;
  }

  public boolean isExcludedOrUnderExcludedDirectory(@NotNull VirtualFile file) {
    ModifiableRootModel model = getModel();
    if (model == null) {
      throw new AssertionError(getClass() + ".getModel() returned null unexpectedly");
    }
    Project project = model.getProject();
    ContentEntry contentEntry = getContentEntry();
    if (contentEntry == null) {
      return false;
    }
    return isExcludedOrUnderExcludedDirectory(project, contentEntry, file);
  }

  public static boolean isExcludedOrUnderExcludedDirectory(@Nullable Project project,
                                                           @NotNull ContentEntry entry,
                                                           @NotNull VirtualFile file) {
    Set<String> excludedUrls = new HashSet<>(entry.getExcludeFolderUrls());
    if (project != null) {
      for (DirectoryIndexExcludePolicy policy : DirectoryIndexExcludePolicy.getExtensions(project)) {
        ContainerUtil.addAll(excludedUrls, policy.getExcludeUrlsForProject());
      }
    }
    Set<VirtualFile> sourceRoots = ContainerUtil.set(entry.getSourceFolderFiles());
    VirtualFile parent = file;
    while (parent != null) {
      if (excludedUrls.contains(parent.getUrl())) return true;
      if (sourceRoots.contains(parent)) return false;
      parent = parent.getParent();
    }
    return false;
  }

  @Nullable
  public SourceFolder getSourceFolder(@NotNull final VirtualFile file) {
    final ContentEntry contentEntry = getContentEntry();
    if (contentEntry == null) {
      return null;
    }
    for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
      final VirtualFile f = sourceFolder.getFile();
      if (f != null && f.equals(file)) {
        return sourceFolder;
      }
    }
    return null;
  }
}
