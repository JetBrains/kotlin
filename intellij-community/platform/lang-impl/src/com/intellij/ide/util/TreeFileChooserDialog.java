// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.util;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.BaseProjectTreeBuilder;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.impl.AbstractProjectTreeStructure;
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import com.intellij.ide.projectView.impl.ProjectTreeBuilder;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.ide.util.gotoByName.ChooseByNamePanel;
import com.intellij.ide.util.gotoByName.ChooseByNamePopupComponent;
import com.intellij.ide.util.gotoByName.GotoFileCellRenderer;
import com.intellij.ide.util.treeView.AlphaComparator;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class TreeFileChooserDialog extends DialogWrapper implements TreeFileChooser {
  private Tree myTree;
  private PsiFile mySelectedFile = null;
  private final Project myProject;
  private BaseProjectTreeBuilder myBuilder;
  private TabbedPaneWrapper myTabbedPane;
  private ChooseByNamePanel myGotoByNamePanel;
  @Nullable private final PsiFile myInitialFile;
  @Nullable private final PsiFileFilter myFilter;
  @Nullable private final FileType myFileType;

  private final boolean myDisableStructureProviders;
  private final boolean myShowLibraryContents;
  private boolean mySelectSearchByNameTab = false;

  public TreeFileChooserDialog(@NotNull Project project,
                               String title,
                               @Nullable final PsiFile initialFile,
                               @Nullable FileType fileType,
                               @Nullable PsiFileFilter filter,
                               final boolean disableStructureProviders,
                               final boolean showLibraryContents) {
    super(project, true);
    myInitialFile = initialFile;
    myFilter = filter;
    myFileType = fileType;
    myDisableStructureProviders = disableStructureProviders;
    myShowLibraryContents = showLibraryContents;
    setTitle(title);
    myProject = project;
    init();
    if (initialFile != null) {
      // dialog does not exist yet
      SwingUtilities.invokeLater(() -> selectFile(initialFile));
    }

    SwingUtilities.invokeLater(() -> handleSelectionChanged());
  }

  @Override
  protected JComponent createCenterPanel() {
    final DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode());
    myTree = new Tree(model);

    final ProjectAbstractTreeStructureBase treeStructure = new AbstractProjectTreeStructure(myProject) {
      @Override
      public boolean isFlattenPackages() {
        return false;
      }

      @Override
      public boolean isShowMembers() {
        return false;
      }

      @Override
      public boolean isHideEmptyMiddlePackages() {
        return true;
      }

      @NotNull
      @Override
      public Object[] getChildElements(@NotNull final Object element) {
        return filterFiles(super.getChildElements(element));
      }

      @Override
      public boolean isAbbreviatePackageNames() {
        return false;
      }

      @Override
      public boolean isShowLibraryContents() {
        return myShowLibraryContents;
      }

      @Override
      public boolean isShowModules() {
        return false;
      }

      @Override
      public List<TreeStructureProvider> getProviders() {
        return myDisableStructureProviders ? null : super.getProviders();
      }
    };
    myBuilder = new ProjectTreeBuilder(myProject, myTree, model, AlphaComparator.INSTANCE, treeStructure);

    myTree.setRootVisible(false);
    myTree.expandRow(0);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.setCellRenderer(new NodeRenderer());

    final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);
    scrollPane.setPreferredSize(JBUI.size(500, 300));

    myTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent e) {
        if (KeyEvent.VK_ENTER == e.getKeyCode()) {
          doOKAction();
        }
      }
    });

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        final TreePath path = myTree.getPathForLocation(e.getX(), e.getY());
        if (path != null && myTree.isPathSelected(path)) {
          doOKAction();
          return true;
        }
        return false;
      }
    }.installOn(myTree);

    myTree.addTreeSelectionListener(
      new TreeSelectionListener() {
        @Override
        public void valueChanged(final TreeSelectionEvent e) {
          handleSelectionChanged();
        }
      }
    );

    new TreeSpeedSearch(myTree);

    myTabbedPane = new TabbedPaneWrapper(getDisposable());

    final JPanel dummyPanel = new JPanel(new BorderLayout());
    String name = null;
    if (myInitialFile != null) {
      name = myInitialFile.getName();
    }
    PsiElement context = myInitialFile;
    myGotoByNamePanel = new ChooseByNamePanel(myProject, new MyGotoFileModel(), name, true, context) {
      @Override
      protected void close(final boolean isOk) {
        super.close(isOk);

        if (isOk) {
          doOKAction();
        }
        else {
          doCancelAction();
        }
      }

      @Override
      protected void initUI(final ChooseByNamePopupComponent.Callback callback,
                            final ModalityState modalityState,
                            boolean allowMultipleSelection) {
        super.initUI(callback, modalityState, allowMultipleSelection);
        dummyPanel.add(myGotoByNamePanel.getPanel(), BorderLayout.CENTER);
        //IdeFocusTraversalPolicy.getPreferredFocusedComponent(myGotoByNamePanel.getPanel()).requestFocus();
        if (mySelectSearchByNameTab) {
          myTabbedPane.setSelectedIndex(1);
        }
      }

      @Override
      protected void showTextFieldPanel() {
      }

      @Override
      protected void chosenElementMightChange() {
        handleSelectionChanged();
      }
    };

    myTabbedPane.addTab(IdeBundle.message("tab.chooser.project"), scrollPane);
    myTabbedPane.addTab(IdeBundle.message("tab.chooser.search.by.name"), dummyPanel);

    SwingUtilities.invokeLater(() -> myGotoByNamePanel.invoke(new MyCallback(), ModalityState.stateForComponent(getRootPane()), false));

    myTabbedPane.addChangeListener(
      new ChangeListener() {
        @Override
        public void stateChanged(final ChangeEvent e) {
          handleSelectionChanged();
        }
      }
    );

    return myTabbedPane.getComponent();
  }

  public void selectSearchByNameTab() {
    mySelectSearchByNameTab = true;
  }

  private void handleSelectionChanged(){
    final PsiFile selection = calcSelectedClass();
    setOKActionEnabled(selection != null);
  }

  @Override
  protected void doOKAction() {
    mySelectedFile = calcSelectedClass();
    if (mySelectedFile == null) return;
    super.doOKAction();
  }

  @Override
  public void doCancelAction() {
    mySelectedFile = null;
    super.doCancelAction();
  }

  @Override
  public PsiFile getSelectedFile(){
    return mySelectedFile;
  }

  @Override
  public void selectFile(@NotNull final PsiFile file) {
    // Select element in the tree
    ApplicationManager.getApplication().invokeLater(() -> {
      if (myBuilder != null) {
        myBuilder.selectAsync(file, file.getVirtualFile(), true);
      }
    }, ModalityState.stateForComponent(getWindow()));
  }

  @Override
  public void showDialog() {
    show();
  }

  private PsiFile calcSelectedClass() {
    if (myTabbedPane.getSelectedIndex() == 1) {
      return (PsiFile)myGotoByNamePanel.getChosenElement();
    }
    else {
      final TreePath path = myTree.getSelectionPath();
      if (path == null) return null;
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      final Object userObject = node.getUserObject();
      if (!(userObject instanceof ProjectViewNode)) return null;
      ProjectViewNode pvNode = (ProjectViewNode) userObject;
      VirtualFile vFile = pvNode.getVirtualFile();
      if (vFile != null && !vFile.isDirectory()) {
        return PsiManager.getInstance(myProject).findFile(vFile);
      }

      return null;
    }
  }


  @Override
  public void dispose() {
    if (myBuilder != null) {
      Disposer.dispose(myBuilder);
      myBuilder = null;
    }
    super.dispose();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "#com.intellij.ide.util.TreeFileChooserDialog";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTree;
  }

  private final class MyGotoFileModel implements ChooseByNameModel, DumbAware {
    private final int myMaxSize = WindowManagerEx.getInstanceEx().getFrame(myProject).getSize().width;
    @Override
    @NotNull
    public Object[] getElementsByName(@NotNull final String name, final boolean checkBoxState, @NotNull final String pattern) {
      GlobalSearchScope scope = myShowLibraryContents ? GlobalSearchScope.allScope(myProject) : GlobalSearchScope.projectScope(myProject);
      final PsiFile[] psiFiles = FilenameIndex.getFilesByName(myProject, name, scope);
      return filterFiles(psiFiles);
    }

    @Override
    public String getPromptText() {
      return IdeBundle.message("prompt.filechooser.enter.file.name");
    }

    @Override
    public String getCheckBoxName() {
      return null;
    }


    @NotNull
    @Override
    public String getNotInMessage() {
      return "";
    }

    @NotNull
    @Override
    public String getNotFoundMessage() {
      return "";
    }

    @Override
    public boolean loadInitialCheckBoxState() {
      return true;
    }

    @Override
    public void saveInitialCheckBoxState(final boolean state) {
    }

    @NotNull
    @Override
    public PsiElementListCellRenderer getListCellRenderer() {
      return new GotoFileCellRenderer(myMaxSize);
    }

    @Override
    @NotNull
    public String[] getNames(final boolean checkBoxState) {
      final String[] fileNames;
      if (myFileType != null && myProject != null) {
        GlobalSearchScope scope = myShowLibraryContents ? GlobalSearchScope.allScope(myProject) : GlobalSearchScope.projectScope(myProject);
        Collection<VirtualFile> virtualFiles = FileTypeIndex.getFiles(myFileType, scope);
        fileNames = ContainerUtil.map2Array(virtualFiles, String.class, file -> file.getName());
      }
      else {
        fileNames = FilenameIndex.getAllFilenames(myProject);
      }
      final Set<String> array = new THashSet<>();
      Collections.addAll(array, fileNames);

      final String[] result = ArrayUtilRt.toStringArray(array);
      Arrays.sort(result);
      return result;
    }

    @Override
    public boolean willOpenEditor() {
      return true;
    }

    @Override
    public String getElementName(@NotNull final Object element) {
      if (!(element instanceof PsiFile)) return null;
      return ((PsiFile)element).getName();
    }

    @Override
    @Nullable
    public String getFullName(@NotNull final Object element) {
      if (element instanceof PsiFile) {
        final VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
        return virtualFile != null ? virtualFile.getPath() : null;
      }

      return getElementName(element);
    }

    @Override
    public String getHelpId() {
      return null;
    }

    @Override
    @NotNull
    public String[] getSeparators() {
      return new String[] {"/", "\\"};
    }

    @Override
    public boolean useMiddleMatching() {
      return false;
    }
  }

  private final class MyCallback extends ChooseByNamePopupComponent.Callback {
    @Override
    public void elementChosen(final Object element) {
      mySelectedFile = (PsiFile)element;
      close(OK_EXIT_CODE);
    }
  }

  private Object[] filterFiles(final Object[] list) {
    Condition<PsiFile> condition = psiFile -> {
      if (myFilter != null && !myFilter.accept(psiFile)) {
        return false;
      }
      boolean accepted = myFileType == null || psiFile.getFileType() == myFileType;
      VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null && !accepted) {
        accepted = FileTypeRegistry.getInstance().isFileOfType(virtualFile, myFileType);
      }
      return accepted;
    };
    final List<Object> result = new ArrayList<>(list.length);
    for (Object o : list) {
      final PsiFile psiFile;
      if (o instanceof PsiFile) {
        psiFile = (PsiFile)o;
      }
      else if (o instanceof PsiFileNode) {
        psiFile = ((PsiFileNode)o).getValue();
      }
      else {
        psiFile = null;
      }
      if (psiFile != null && !condition.value(psiFile)) {
        continue;
      }
      else {
        if (o instanceof ProjectViewNode) {
          final ProjectViewNode projectViewNode = (ProjectViewNode)o;
          if (!projectViewNode.canHaveChildrenMatching(condition)) {
            continue;
          }
        }
      }
      result.add(o);
    }
    return ArrayUtil.toObjectArray(result);
  }
}
