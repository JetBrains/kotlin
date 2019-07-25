// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.libraries.ui.impl;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ComboBoxCellEditor;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * This dialog allows selecting paths inside selected archives or directories.
 * The tree is three-level:
 * <ul>
 * <li>The root is a fake node that just holds child nodes.</li>
 * <li>The second level is archives or directories selected on the previous selection step.</li>
 * <li>The third level are detected roots inside previous selection.</li>
 * </ul>
 *
 * @author max
 * @author Constantine.Plotnikov
 */
public class DetectedRootsChooserDialog extends DialogWrapper {
  private static final ColumnInfo ROOT_COLUMN = new TreeColumnInfo("");
  private static final ColumnInfo<VirtualFileCheckedTreeNode, String> ROOT_TYPE_COLUMN = new ColumnInfo<VirtualFileCheckedTreeNode, String>("") {
    @Override
    public String valueOf(VirtualFileCheckedTreeNode node) {
      final SuggestedChildRootInfo rootInfo = node.getRootInfo();
      return rootInfo != null ? rootInfo.getRootTypeName(rootInfo.getSelectedRootType()) : "";
    }

    @Override
    public TableCellRenderer getRenderer(VirtualFileCheckedTreeNode node) {
      final SuggestedChildRootInfo rootInfo = node.getRootInfo();
      if (rootInfo != null && isCellEditable(node)) {
        return new ComboBoxTableRenderer<>(rootInfo.getRootTypeNames());
      }
      return new DefaultTableCellRenderer();
    }

    @Override
    public TableCellEditor getEditor(VirtualFileCheckedTreeNode o) {
      final SuggestedChildRootInfo rootInfo = o.getRootInfo();
      if (rootInfo == null) return null;
      final ComboBoxCellEditor editor = new ComboBoxCellEditor() {
        @Override
        protected List<String> getComboBoxItems() {
          return Arrays.asList(rootInfo.getRootTypeNames());
        }
      };
      editor.setClickCountToStart(1);
      return editor;
    }

    @Override
    public boolean isCellEditable(VirtualFileCheckedTreeNode node) {
      final SuggestedChildRootInfo rootInfo = node.getRootInfo();
      return rootInfo != null && rootInfo.getDetectedRoot().getTypes().size() > 1;
    }

    @Override
    public void setValue(VirtualFileCheckedTreeNode node, String value) {
      final SuggestedChildRootInfo rootInfo = node.getRootInfo();
      if (rootInfo != null) {
        rootInfo.setSelectedRootType(value);
      }
    }
  };

  private CheckboxTreeTable myTreeTable;
  private JScrollPane myPane;
  private String myDescription;

  public DetectedRootsChooserDialog(Component component, Collection<SuggestedChildRootInfo> suggestedRoots) {
    super(component, true);
    init(suggestedRoots);
  }

  public DetectedRootsChooserDialog(Project project, Collection<SuggestedChildRootInfo> suggestedRoots) {
    super(project, true);
    init(suggestedRoots);
  }

  private void init(Collection<SuggestedChildRootInfo> suggestedRoots) {
    myDescription = XmlStringUtil.wrapInHtml(ApplicationNamesInfo.getInstance().getFullProductName() +
                    " just scanned files and detected the following " + StringUtil.pluralize("root", suggestedRoots.size()) + ".<br>" +
                    "Select items in the tree below or press Cancel to cancel operation.");
    myTreeTable = createTreeTable(suggestedRoots);
    myPane = ScrollPaneFactory.createScrollPane(myTreeTable);
    setTitle("Detected Roots");
    init();
  }

  private static CheckboxTreeTable createTreeTable(Collection<SuggestedChildRootInfo> suggestedRoots) {
    final CheckedTreeNode root = createRoot(suggestedRoots);
    CheckboxTreeTable treeTable = new CheckboxTreeTable(root, new CheckboxTree.CheckboxTreeCellRenderer(true) {
      @Override
      public void customizeRenderer(JTree tree,
                                        Object value,
                                        boolean selected,
                                        boolean expanded,
                                        boolean leaf,
                                        int row,
                                        boolean hasFocus) {
        if (!(value instanceof VirtualFileCheckedTreeNode)) return;
        VirtualFileCheckedTreeNode node = (VirtualFileCheckedTreeNode)value;
        VirtualFile file = node.getFile();
        String text;
        SimpleTextAttributes attributes;
        boolean isValid = true;
        if (leaf) {
          VirtualFile ancestor = ((VirtualFileCheckedTreeNode)node.getParent()).getFile();
          if (ancestor != null) {
            text = VfsUtilCore.getRelativePath(file, ancestor, File.separatorChar);
            if (StringUtil.isEmpty(text)) {
              text = File.separator;
            }
          }
          else {
            text = file.getPresentableUrl();
          }
          if (text == null) {
            isValid = false;
            text = file.getPresentableUrl();
          }
          attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
        }
        else {
          text = file.getPresentableUrl();
          attributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
        }
        final ColoredTreeCellRenderer textRenderer = getTextRenderer();
        textRenderer.setIcon(PlatformIcons.FOLDER_ICON);
        if (!isValid) {
          textRenderer.append("[INVALID] ", SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
        textRenderer.append(text, attributes);
      }
    }, new ColumnInfo[]{ROOT_COLUMN, ROOT_TYPE_COLUMN});

    int max = 0;
    for (SuggestedChildRootInfo info : suggestedRoots) {
      for (String s : info.getRootTypeNames()) {
        max = Math.max(max, treeTable.getFontMetrics(treeTable.getFont()).stringWidth(s));
      }
    }
    final TableColumn column = treeTable.getColumnModel().getColumn(1);
    int width = max + 20;//add space for combobox button
    column.setPreferredWidth(width);
    column.setMaxWidth(width);
    treeTable.setRootVisible(false);
    new TreeTableSpeedSearch(treeTable, o -> {
      Object node = o.getLastPathComponent();
      if (!(node instanceof VirtualFileCheckedTreeNode)) return "";
      return ((VirtualFileCheckedTreeNode)node).getFile().getPresentableUrl();
    });
    TreeUtil.expandAll(treeTable.getTree());
    return treeTable;
  }

  private static CheckedTreeNode createRoot(Collection<SuggestedChildRootInfo> suggestedRoots) {
    SuggestedChildRootInfo[] sortedRoots = suggestedRoots.toArray(new SuggestedChildRootInfo[0]);
    Arrays.sort(sortedRoots,
                (o1, o2) -> o1.getDetectedRoot().getFile().getPresentableUrl().compareTo(o2.getDetectedRoot().getFile().getPresentableUrl()));

    CheckedTreeNode root = new CheckedTreeNode(null);
    Map<VirtualFile, CheckedTreeNode> rootCandidateNodes = new HashMap<>();
    for (SuggestedChildRootInfo rootInfo : sortedRoots) {
      final VirtualFile rootCandidate = rootInfo.getRootCandidate();
      CheckedTreeNode parent = rootCandidateNodes.get(rootCandidate);
      if (parent == null) {
        parent = new VirtualFileCheckedTreeNode(rootCandidate);
        rootCandidateNodes.put(rootCandidate, parent);
        root.add(parent);
      }
      parent.add(new VirtualFileCheckedTreeNode(rootInfo));
    }
    return root;
  }

  @Override
  protected JComponent createTitlePane() {
    return new TitlePanel("Choose Roots", myDescription);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myPane;
  }

  public SuggestedChildRootInfo[] getChosenRoots() {
    return myTreeTable.getCheckedNodes(SuggestedChildRootInfo.class);
  }

  @NonNls
  @Override
  protected String getDimensionServiceKey() {
    return "DetectedRootsChooserDialog";
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTreeTable;
  }

  private static class VirtualFileCheckedTreeNode extends CheckedTreeNode {
    private final VirtualFile myFile;

    private VirtualFileCheckedTreeNode(VirtualFile file) {
      super(file);
      myFile = file;
    }

    VirtualFileCheckedTreeNode(SuggestedChildRootInfo rootInfo) {
      super(rootInfo);
      myFile = rootInfo.getDetectedRoot().getFile();
    }

    public VirtualFile getFile() {
      return myFile;
    }

    @Nullable
    private SuggestedChildRootInfo getRootInfo() {
      return userObject instanceof SuggestedChildRootInfo ? (SuggestedChildRootInfo)userObject : null;
    }
  }
}
