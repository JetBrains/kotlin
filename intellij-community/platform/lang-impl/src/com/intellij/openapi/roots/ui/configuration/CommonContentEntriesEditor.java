// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.ex.FileChooserKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.roots.ui.componentsList.layout.VerticalStackLayout;
import com.intellij.openapi.roots.ui.configuration.actions.IconWithTextAction;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileManagerListener;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.roots.ToolbarPanel;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 */
public class CommonContentEntriesEditor extends ModuleElementsEditor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.ui.configuration.ContentEntriesEditor");
  public static final String NAME = ProjectBundle.message("module.paths.title");

  protected ContentEntryTreeEditor myRootTreeEditor;
  private MyContentEntryEditorListener myContentEntryEditorListener;
  protected JPanel myEditorsPanel;
  protected final Map<String, ContentEntryEditor> myEntryToEditorMap = new HashMap<>();
  private String mySelectedEntryUrl;

  private VirtualFile myLastSelectedDir = null;
  private final String myModuleName;
  private final ModulesProvider myModulesProvider;
  private final boolean myWithBorders;
  private final ModuleConfigurationState myState;
  private final List<ModuleSourceRootEditHandler<?>> myEditHandlers = new ArrayList<>();

  public CommonContentEntriesEditor(String moduleName, final ModuleConfigurationState state, boolean withBorders,
                                    JpsModuleSourceRootType<?>... rootTypes) {
    super(state);
    myState = state;
    myModuleName = moduleName;
    myModulesProvider = state.getModulesProvider();
    myWithBorders = withBorders;
    for (JpsModuleSourceRootType<?> type : rootTypes) {
      ContainerUtil.addIfNotNull(myEditHandlers, ModuleSourceRootEditHandler.getEditHandler(type));
    }
    final VirtualFileManagerListener fileManagerListener = new VirtualFileManagerListener() {
      @Override
      public void afterRefreshFinish(boolean asynchronous) {
        if (state.getProject().isDisposed()) {
          return;
        }
        final Module module = getModule();
        if (module == null || module.isDisposed()) return;
        for (final ContentEntryEditor editor : myEntryToEditorMap.values()) {
          editor.update();
        }
      }
    };
    final VirtualFileManager fileManager = VirtualFileManager.getInstance();
    fileManager.addVirtualFileManagerListener(fileManagerListener);
    registerDisposable(new Disposable() {
      @Override
      public void dispose() {
        fileManager.removeVirtualFileManagerListener(fileManagerListener);
      }
    });
  }

  public CommonContentEntriesEditor(String moduleName, final ModuleConfigurationState state, JpsModuleSourceRootType<?>... rootTypes) {
    this(moduleName, state, false, rootTypes);
  }

  @Override
  protected ModifiableRootModel getModel() {
    return myState.getRootModel();
  }

  @Override
  public String getHelpTopic() {
    return "projectStructure.modules.sources";
  }

  @Override
  public String getDisplayName() {
    return NAME;
  }

  protected final List<ModuleSourceRootEditHandler<?>> getEditHandlers() {
    return myEditHandlers;
  }

  @Override
  public void disposeUIResources() {
    if (myRootTreeEditor != null) {
      myRootTreeEditor.setContentEntryEditor(null);
    }

    myEntryToEditorMap.clear();
    super.disposeUIResources();
  }

  @Override
  public JPanel createComponentImpl() {
    final Module module = getModule();
    final Project project = module.getProject();

    myContentEntryEditorListener = new MyContentEntryEditorListener();

    final JPanel mainPanel = new JPanel(new BorderLayout());

    addAdditionalSettingsToPanel(mainPanel);

    final DefaultActionGroup group = new DefaultActionGroup();
    final AddContentEntryAction action = new AddContentEntryAction();
    action.registerCustomShortcutSet(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK, mainPanel);
    group.add(action);

    myEditorsPanel = new ScrollablePanel(new VerticalStackLayout());
    myEditorsPanel.setBackground(UIUtil.getListBackground());
    JScrollPane myScrollPane = ScrollPaneFactory.createScrollPane(myEditorsPanel, true);
    final ToolbarPanel toolbarPanel = new ToolbarPanel(myScrollPane, group);
    int border = myWithBorders ? 1 : 0;
    toolbarPanel.setBorder(new CustomLineBorder(1, 0, border, border));

    final JBSplitter splitter = new OnePixelSplitter(false);
    splitter.setProportion(0.6f);
    splitter.setHonorComponentsMinimumSize(true);

    myRootTreeEditor = createContentEntryTreeEditor(project);
    final JComponent component = myRootTreeEditor.createComponent();
    component.setBorder(new CustomLineBorder(1, border, border, 0));

    splitter.setFirstComponent(component);
    splitter.setSecondComponent(toolbarPanel);
    JPanel contentPanel = new JPanel(new GridBagLayout());
    final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("ProjectStructureContentEntries", myRootTreeEditor.getEditingActionsGroup(), true);
    contentPanel.add(new JLabel("Mark as:"),
                     new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, 0, JBInsets.create(0, 10), 0, 0));
    contentPanel.add(actionToolbar.getComponent(),
                     new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                                            JBUI.emptyInsets(), 0, 0));
    contentPanel.add(splitter,
                     new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                                            JBUI.emptyInsets(), 0, 0));

    mainPanel.add(contentPanel, BorderLayout.CENTER);


    final JPanel innerPanel = createBottomControl(module);
    if (innerPanel != null) {
      mainPanel.add(innerPanel, BorderLayout.SOUTH);
    }

    final ModifiableRootModel model = getModel();
    if (model != null) {
      final ContentEntry[] contentEntries = model.getContentEntries();
      if (contentEntries.length > 0) {
        for (final ContentEntry contentEntry : contentEntries) {
          addContentEntryPanel(contentEntry.getUrl());
        }
        selectContentEntry(contentEntries[0].getUrl(), false);
      }
    }

    return mainPanel;
  }

  @Nullable
  protected JPanel createBottomControl(Module module) {
    return null;
  }

  protected ContentEntryTreeEditor createContentEntryTreeEditor(Project project) {
    return new ContentEntryTreeEditor(project, myEditHandlers);
  }

  protected void addAdditionalSettingsToPanel(final JPanel mainPanel) {
  }

  protected Module getModule() {
    return myModulesProvider.getModule(myModuleName);
  }

  protected void addContentEntryPanel(final String contentEntry) {
    final ContentEntryEditor contentEntryEditor = createContentEntryEditor(contentEntry);
    contentEntryEditor.initUI();
    contentEntryEditor.addContentEntryEditorListener(myContentEntryEditorListener);
    registerDisposable(new Disposable() {
      @Override
      public void dispose() {
        contentEntryEditor.removeContentEntryEditorListener(myContentEntryEditorListener);
      }
    });
    myEntryToEditorMap.put(contentEntry, contentEntryEditor);
    Border border = BorderFactory.createEmptyBorder(2, 2, 0, 2);
    final JComponent component = contentEntryEditor.getComponent();
    final Border componentBorder = component.getBorder();
    if (componentBorder != null) {
      border = BorderFactory.createCompoundBorder(border, componentBorder);
    }
    component.setBorder(new EmptyBorder(0, 0, 0, 0));
    myEditorsPanel.add(component);
  }

  protected ContentEntryEditor createContentEntryEditor(String contentEntryUrl) {
    return new ContentEntryEditor(contentEntryUrl, myEditHandlers) {
      @Override
      protected ModifiableRootModel getModel() {
        return CommonContentEntriesEditor.this.getModel();
      }
    };
  }

  void selectContentEntry(final String contentEntryUrl, boolean requestFocus) {
    if (mySelectedEntryUrl != null && mySelectedEntryUrl.equals(contentEntryUrl)) {
      if (requestFocus) {
        myRootTreeEditor.requestFocus();
      }
      return;
    }
    try {
      if (mySelectedEntryUrl != null) {
        ContentEntryEditor editor = myEntryToEditorMap.get(mySelectedEntryUrl);
        if (editor != null) {
          editor.setSelected(false);
        }
      }

      if (contentEntryUrl != null) {
        ContentEntryEditor editor = myEntryToEditorMap.get(contentEntryUrl);
        if (editor != null) {
          editor.setSelected(true);
          final JComponent component = editor.getComponent();
          final JComponent scroller = (JComponent)component.getParent();
          SwingUtilities.invokeLater(() -> scroller.scrollRectToVisible(component.getBounds()));
          myRootTreeEditor.setContentEntryEditor(editor);
          if (requestFocus) {
            myRootTreeEditor.requestFocus();
          }
        }
      }
    }
    finally {
      mySelectedEntryUrl = contentEntryUrl;
    }
  }

  @Override
  public void moduleStateChanged() {
    if (myRootTreeEditor != null) { //in order to update exclude output root if it is under content root
      myRootTreeEditor.update();
    }
  }

  @Nullable
  private String getNextContentEntry(final String contentEntryUrl) {
    return getAdjacentContentEntry(contentEntryUrl, 1);
  }

  @Nullable
  private String getAdjacentContentEntry(final String contentEntryUrl, int delta) {
    final ContentEntry[] contentEntries = getModel().getContentEntries();
    for (int idx = 0; idx < contentEntries.length; idx++) {
      ContentEntry entry = contentEntries[idx];
      if (contentEntryUrl.equals(entry.getUrl())) {
        int nextEntryIndex = (idx + delta) % contentEntries.length;
        if (nextEntryIndex < 0) {
          nextEntryIndex += contentEntries.length;
        }
        return nextEntryIndex == idx ? null : contentEntries[nextEntryIndex].getUrl();
      }
    }
    return null;
  }

  protected List<ContentEntry> addContentEntries(final VirtualFile[] files) {
    List<ContentEntry> contentEntries = new ArrayList<>();
    for (final VirtualFile file : files) {
      if (isAlreadyAdded(file)) {
        continue;
      }
      final ContentEntry contentEntry = getModel().addContentEntry(file);
      contentEntries.add(contentEntry);
    }
    return contentEntries;
  }

  private boolean isAlreadyAdded(VirtualFile file) {
    final VirtualFile[] contentRoots = getModel().getContentRoots();
    for (VirtualFile contentRoot : contentRoots) {
      if (contentRoot.equals(file)) {
        return true;
      }
    }
    return false;
  }

  protected void addContentEntryPanels(ContentEntry[] contentEntriesArray) {
    for (ContentEntry contentEntry : contentEntriesArray) {
      addContentEntryPanel(contentEntry.getUrl());
    }
    myEditorsPanel.revalidate();
    myEditorsPanel.repaint();
    selectContentEntry(contentEntriesArray[contentEntriesArray.length - 1].getUrl(), false);
  }

  private final class MyContentEntryEditorListener extends ContentEntryEditorListenerAdapter {
    @Override
    public void sourceFolderAdded(@NotNull ContentEntryEditor editor, SourceFolder folder) {
      fireConfigurationChanged();
    }

    @Override
    public void sourceFolderRemoved(@NotNull ContentEntryEditor editor, VirtualFile file) {
      fireConfigurationChanged();
    }

    @Override
    public void folderExcluded(@NotNull ContentEntryEditor editor, VirtualFile file) {
      fireConfigurationChanged();
    }

    @Override
    public void folderIncluded(@NotNull ContentEntryEditor editor, String fileUrl) {
      fireConfigurationChanged();
    }

    @Override
    public void sourceRootPropertiesChanged(@NotNull ContentEntryEditor editor, @NotNull SourceFolder folder) {
      fireConfigurationChanged();
    }

    @Override
    public void editingStarted(@NotNull ContentEntryEditor editor) {
      selectContentEntry(editor.getContentEntryUrl(), true);
    }

    @Override
    public void beforeEntryDeleted(@NotNull ContentEntryEditor editor) {
      final String entryUrl = editor.getContentEntryUrl();
      if (mySelectedEntryUrl != null && mySelectedEntryUrl.equals(entryUrl)) {
        myRootTreeEditor.setContentEntryEditor(null);
      }
      final String nextContentEntryUrl = getNextContentEntry(entryUrl);
      removeContentEntryPanel(entryUrl);
      selectContentEntry(nextContentEntryUrl, true);
      editor.removeContentEntryEditorListener(this);
    }

    @Override
    public void navigationRequested(@NotNull ContentEntryEditor editor, VirtualFile file) {
      selectContentEntry(editor.getContentEntryUrl(), true);
      myRootTreeEditor.select(file);
    }

    private void removeContentEntryPanel(final String contentEntryUrl) {
      ContentEntryEditor editor = myEntryToEditorMap.get(contentEntryUrl);
      if (editor != null) {
        myEditorsPanel.remove(editor.getComponent());
        myEntryToEditorMap.remove(contentEntryUrl);
        myEditorsPanel.revalidate();
        myEditorsPanel.repaint();
      }
    }
  }

  private class AddContentEntryAction extends IconWithTextAction implements DumbAware {
    private final FileChooserDescriptor myDescriptor;

    AddContentEntryAction() {
      super(ProjectBundle.message("module.paths.add.content.action"),
            ProjectBundle.message("module.paths.add.content.action.description"), AllIcons.General.Add);
      myDescriptor = new FileChooserDescriptor(false, true, true, false, true, true) {
        @Override
        public void validateSelectedFiles(@NotNull VirtualFile[] files) throws Exception {
          validateContentEntriesCandidates(files);
        }
      };
      myDescriptor.putUserData(LangDataKeys.MODULE_CONTEXT, getModule());
      myDescriptor.setTitle(ProjectBundle.message("module.paths.add.content.title"));
      myDescriptor.setDescription(ProjectBundle.message("module.paths.add.content.prompt"));
      myDescriptor.putUserData(FileChooserKeys.DELETE_ACTION_AVAILABLE, false);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      FileChooser.chooseFiles(myDescriptor, myProject, myLastSelectedDir, files -> {
        myLastSelectedDir = files.get(0);
        addContentEntries(VfsUtilCore.toVirtualFileArray(files));
      });
    }

    @Nullable
    private ContentEntry getContentEntry(final String url) {
      final ContentEntry[] entries = getModel().getContentEntries();
      for (final ContentEntry entry : entries) {
        if (entry.getUrl().equals(url)) return entry;
      }

      return null;
    }

    private void validateContentEntriesCandidates(VirtualFile[] files) throws Exception {
      for (final VirtualFile file : files) {
        // check for collisions with already existing entries
        for (final String contentEntryUrl : myEntryToEditorMap.keySet()) {
          final ContentEntry contentEntry = getContentEntry(contentEntryUrl);
          if (contentEntry == null) continue;
          final VirtualFile contentEntryFile = contentEntry.getFile();
          if (contentEntryFile == null) {
            continue;  // skip invalid entry
          }
          if (contentEntryFile.equals(file)) {
            throw new Exception(ProjectBundle.message("module.paths.add.content.already.exists.error", file.getPresentableUrl()));
          }
          if (VfsUtilCore.isAncestor(contentEntryFile, file, true) && !ContentEntryEditor.isExcludedOrUnderExcludedDirectory(myProject, contentEntry, file)) {
            // intersection not allowed
            throw new Exception(
              ProjectBundle.message("module.paths.add.content.intersect.error", file.getPresentableUrl(),
                                    contentEntryFile.getPresentableUrl()));
          }
          if (VfsUtilCore.isAncestor(file, contentEntryFile, true)) {
            // intersection not allowed
            throw new Exception(
              ProjectBundle.message("module.paths.add.content.dominate.error", file.getPresentableUrl(),
                                    contentEntryFile.getPresentableUrl()));
          }
        }
        // check if the same root is configured for another module
        final Module[] modules = myModulesProvider.getModules();
        for (final Module module : modules) {
          if (myModuleName.equals(module.getName())) {
            continue;
          }
          ModuleRootModel rootModel = myModulesProvider.getRootModel(module);
          LOG.assertTrue(rootModel != null);
          final VirtualFile[] moduleContentRoots = rootModel.getContentRoots();
          for (VirtualFile moduleContentRoot : moduleContentRoots) {
            if (file.equals(moduleContentRoot)) {
              throw new Exception(
                ProjectBundle.message("module.paths.add.content.duplicate.error", file.getPresentableUrl(), module.getName()));
            }
          }
        }
      }
    }

  }

}
