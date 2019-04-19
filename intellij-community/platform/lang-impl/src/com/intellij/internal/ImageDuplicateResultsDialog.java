// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal;

import com.intellij.codeInsight.hint.ImplementationViewComponent;
import com.intellij.codeInsight.hint.PsiImplementationViewElement;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.PropertyName;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.NotNullFunction;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Konstantin Bulenkov
 */
public class ImageDuplicateResultsDialog extends DialogWrapper {
  private final Project myProject;
  private final List<VirtualFile> myImages;
  private final Map<String, Set<VirtualFile>> myDuplicates;
  private final Tree myTree;
  private final TreeSpeedSearch mySpeedSearch;
  private final ResourceModules myResourceModules = new ResourceModules();


  public ImageDuplicateResultsDialog(Project project, List<VirtualFile> images, Map<String, Set<VirtualFile>> duplicates) {
    super(project);
    myProject = project;
    myImages = images;
    PropertiesComponent.getInstance(myProject).loadFields(myResourceModules);
    myDuplicates = duplicates;
    setModal(false);
    myTree = new Tree(new MyRootNode());
    myTree.setRootVisible(true);
    MyCellRenderer renderer = new MyCellRenderer();
    myTree.setCellRenderer(renderer);
    mySpeedSearch = new TreeSpeedSearch(myTree, x -> renderer.getTreeCellRendererComponent(myTree, x.getLastPathComponent(), false, false, false, 0, false).toString());
    init();
    TreeUtil.expandAll(myTree);
    setTitle("Image Duplicates");
    TreeUtil.selectFirstNode(myTree);
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    final Action[] actions = new Action[4];
    actions[0] = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
      }
    };
    actions[0].putValue(Action.NAME, "Fix all");
    actions[0].putValue(DEFAULT_ACTION, Boolean.TRUE);
    actions[0].putValue(FOCUSED_ACTION, Boolean.TRUE);
    actions[1] = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
      }
    };
    actions[1].putValue(Action.NAME, "Fix selected");
    actions[2] = getCancelAction();
    actions[3] = getHelpAction();
    //return actions;
    return new Action[0];
  }

  @Override
  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new BorderLayout());
    DataManager.registerDataProvider(panel, dataId -> {
      final TreePath path = myTree.getSelectionPath();
      if (path != null) {
        Object component = path.getLastPathComponent();
        VirtualFile file = null;
        if (component instanceof MyFileNode) {
          component = ((MyFileNode)component).getParent();
        }
        if (component instanceof MyDuplicatesNode) {
          file = ((MyDuplicatesNode)component).getUserObject().iterator().next();
        }
        if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
          return file;
        }
        if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId) && file != null) {
          return new VirtualFile[]{file};
        }
      }
      return null;
    });

    final JBList list = new JBList(new ResourceModules().getModuleNames());
    final NotNullFunction<Object, JComponent> modulesRenderer =
      dom -> new JLabel(dom instanceof Module ? ((Module)dom).getName() : dom.toString(), PlatformIcons.SOURCE_FOLDERS_ICON, SwingConstants.LEFT);
    list.installCellRenderer(modulesRenderer);
    final JPanel modulesPanel = ToolbarDecorator.createDecorator(list)
      .setAddAction(button -> {
        final Module[] all = ModuleManager.getInstance(myProject).getModules();
        Arrays.sort(all, (o1, o2) -> o1.getName().compareTo(o2.getName()));
        final JBList modules = new JBList(all);
        modules.installCellRenderer(modulesRenderer);
        JBPopupFactory.getInstance().createListPopupBuilder(modules)
          .setTitle("Add Resource Module")
          .setNamerForFiltering(o -> ((Module)o).getName())
          .setItemChoosenCallback(() -> {
            final Object value = modules.getSelectedValue();
            if (value instanceof Module && !myResourceModules.contains((Module)value)) {
              myResourceModules.add((Module)value);
              ((DefaultListModel)list.getModel()).addElement(((Module)value).getName());
            }
            ((DefaultTreeModel)myTree.getModel()).reload();
            TreeUtil.expandAll(myTree);
          }).createPopup().show(button.getPreferredPopupPoint());
      })
      .setRemoveAction(button -> {
        final Object[] values = list.getSelectedValues();
        for (Object value : values) {
          myResourceModules.remove((String)value);
          ((DefaultListModel)list.getModel()).removeElement(value);
        }
        ((DefaultTreeModel)myTree.getModel()).reload();
        TreeUtil.expandAll(myTree);
      })
      .disableDownAction()
      .disableUpAction()
      .createPanel();
    modulesPanel.setPreferredSize(new Dimension(-1, 60));
    final JPanel top = new JPanel(new BorderLayout());
    top.add(new JLabel("Image modules:"), BorderLayout.NORTH);
    top.add(modulesPanel, BorderLayout.CENTER);

    panel.add(top, BorderLayout.NORTH);
    panel.add(new JBScrollPane(myTree), BorderLayout.CENTER);
    new AnAction() {

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile file = getFileFromSelection();
        if (file != null) {
          final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
          if (psiFile != null) {
            final ImplementationViewComponent viewComponent = new ImplementationViewComponent(Collections.singletonList(new PsiImplementationViewElement(psiFile)), 0);
            final TreeSelectionListener listener = e1 -> {
              final VirtualFile selection = getFileFromSelection();
              if (selection != null) {
                final PsiFile newElement = PsiManager.getInstance(myProject).findFile(selection);
                if (newElement != null) {
                  viewComponent.update(Collections.singletonList(new PsiImplementationViewElement(newElement)), 0);
                }
              }
            };
            myTree.addTreeSelectionListener(listener);

            final JBPopup popup =
              JBPopupFactory.getInstance().createComponentPopupBuilder(viewComponent, viewComponent.getPreferredFocusableComponent())
                .setProject(myProject)
                .setDimensionServiceKey(myProject, ImageDuplicateResultsDialog.class.getName(), false)
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(false)
                .setCancelCallback(() -> {
                  myTree.removeTreeSelectionListener(listener);
                  return true;
                })
                .setTitle("Image Preview")
                .createPopup();


            final Window window = ImageDuplicateResultsDialog.this.getWindow();
            popup.show(new RelativePoint(window, new Point(window.getWidth(), 0)));
            viewComponent.setHint(popup, "Image Preview");
          }
        }
      }
    }.registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), panel);

    int total = myDuplicates.values().stream().mapToInt(Set::size).sum() - myDuplicates.size();
    final JLabel label = new JLabel(
      "<html>Press <b>Enter</b> to preview image<br>Total images found: " + myImages.size() + ". Total duplicates found: " + total+"</html>");
    panel.add(label, BorderLayout.SOUTH);
    return panel;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "image.duplicates.dialog";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTree;
  }

  @Nullable
  private VirtualFile getFileFromSelection() {
    final TreePath path = myTree.getSelectionPath();
    if (path != null) {
      Object component = path.getLastPathComponent();
      VirtualFile file = null;
      if (component instanceof MyFileNode) {
        component = ((MyFileNode)component).getParent();
      }
      if (component instanceof MyDuplicatesNode) {
        file = ((MyDuplicatesNode)component).getUserObject().iterator().next();
      }
      return file;
    }
    return null;
  }


  private class MyRootNode extends DefaultMutableTreeNode {
    private MyRootNode() {
      children =
        myDuplicates.values().stream().map(files -> new MyDuplicatesNode(this, files)).collect(Collectors.toCollection(Vector::new));
    }
  }


  private static class MyDuplicatesNode extends DefaultMutableTreeNode {

    MyDuplicatesNode(DefaultMutableTreeNode node, Set<? extends VirtualFile> files) {
      super(files);
      setParent(node);
      children = files.stream().map(file -> new MyFileNode(this, file)).collect(Collectors.toCollection(Vector::new));
    }

    @Override
    public Set<VirtualFile> getUserObject() {
      return (Set<VirtualFile>)super.getUserObject();
    }
  }

  private static class MyFileNode extends DefaultMutableTreeNode {
    MyFileNode(DefaultMutableTreeNode node, VirtualFile file) {
      super(file);
      setParent(node);
    }

    @Override
    public VirtualFile getUserObject() {
      return (VirtualFile)super.getUserObject();
    }
  }

  private class MyCellRenderer extends ColoredTreeCellRenderer {
    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      if (value instanceof MyFileNode) {
        final VirtualFile file = ((MyFileNode)value).getUserObject();
        final Module module = ModuleUtil.findModuleForFile(file, myProject);
        if (module != null) {
          setIcon(PlatformIcons.CONTENT_ROOT_ICON_CLOSED);
          SearchUtil.appendFragments(mySpeedSearch.getEnteredPrefix(), "[" + module.getName() + "] ", SimpleTextAttributes.STYLE_BOLD, UIUtil.getTreeForeground(), UIUtil.getTreeBackground(), this);
        }
        SearchUtil.appendFragments(mySpeedSearch.getEnteredPrefix(), getRelativePathToProject(myProject, file), SimpleTextAttributes.STYLE_PLAIN, UIUtil.getTreeForeground(), UIUtil.getTreeBackground(), this);
      }
      else if (value instanceof MyDuplicatesNode) {
        final Set<VirtualFile> files = ((MyDuplicatesNode)value).getUserObject();
        for (VirtualFile file : files) {
          final Module module = ModuleUtil.findModuleForFile(file, myProject);

          if (module != null && myResourceModules.contains(module)) {
            append("Icons can be replaced to ");
            append(getRelativePathToProject(myProject, file),
                   new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, ColorUtil.fromHex("008000")));
            return;
          }
        }
        append("Icon conflict");
      } else if (value instanceof MyRootNode) {
        append("All conflicts");
      }
    }
  }

  private static String getRelativePathToProject(Project project, VirtualFile file) {
    final String path = project.getBasePath();
    assert path != null;
    final String result = FileUtil.getRelativePath(path, file.getPath().replace('/', File.separatorChar), File.separatorChar);
    assert result != null;
    return result;
  }



  static class ResourceModules {
    @PropertyName(value = "resource.modules", defaultValue = "icons")
    public String modules;

    public List<String> getModuleNames() {
      return Arrays.asList(StringUtil.splitByLines(modules == null ? "icons" : modules));
    }

    public boolean contains(Module module) {
      return getModuleNames().contains(module.getName());
    }

    public void add(Module module) {
      if (StringUtil.isEmpty(modules)) {
        modules = module.getName();
      } else {
        modules += "\n" + module.getName();
      }
    }

    public void remove(String value) {
      final List<String> names = new ArrayList<>(getModuleNames());
      names.remove(value);
      modules = StringUtil.join(names, "\n");
    }
  }
}
