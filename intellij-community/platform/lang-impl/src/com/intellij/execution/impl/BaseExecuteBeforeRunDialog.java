// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Key;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.SmartList;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;


public abstract class BaseExecuteBeforeRunDialog<T extends BeforeRunTask> extends DialogWrapper {
  private final Project myProject;
  private DefaultMutableTreeNode myRoot;

  public BaseExecuteBeforeRunDialog(final Project project) {
    super(project, true);
    myProject = project;
  }

  @Override
  protected void init() {
    super.init();
    setTitle(ExecutionBundle.message("execute.before.run.debug.dialog.title", getTargetDisplayString()));
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    myRoot = buildNodes();
    final Tree tree = new Tree(myRoot);

    final MyTreeCellRenderer cellRenderer = new MyTreeCellRenderer();

    tree.setCellRenderer(cellRenderer);
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    TreeUtil.installActions(tree);
    new TreeSpeedSearch(tree);

    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        int row = tree.getRowForLocation(e.getX(), e.getY());
        if (row >= 0) {
          Rectangle rowBounds = tree.getRowBounds(row);
          cellRenderer.setBounds(rowBounds);
          Rectangle checkBounds = cellRenderer.myCheckbox.getBounds();

          checkBounds.setLocation(rowBounds.getLocation());
          if (checkBounds.contains(e.getPoint())) {
            toggleNode(tree, (DefaultMutableTreeNode)tree.getPathForRow(row).getLastPathComponent());
            e.consume();
            tree.setSelectionRow(row);
          }
        }
      }
    });

    tree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          TreePath treePath = tree.getLeadSelectionPath();
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
          toggleNode(tree, node);
          e.consume();
        }
      }
    });

    expacndChecked(tree);

    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(tree);
    scrollPane.setPreferredSize(JBUI.size(400, 400));
    panel.add(scrollPane, BorderLayout.CENTER);
    return panel;
  }

  private static void expacndChecked(Tree tree) {
    TreeNode root = (TreeNode)tree.getModel().getRoot();
    Enumeration factories = root.children();
    ArrayList<TreeNode[]> toExpand = new ArrayList<>();
    while (factories.hasMoreElements()) {
      DefaultMutableTreeNode factoryNode = (DefaultMutableTreeNode)factories.nextElement();
      Enumeration configurations = factoryNode.children();
      while (configurations.hasMoreElements()) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)configurations.nextElement();
        ConfigurationDescriptor config = (ConfigurationDescriptor)node.getUserObject();
        if (config.isChecked()) {
          toExpand.add(factoryNode.getPath());
          break;
        }
      }
    }
    for (TreeNode[] treeNodes : toExpand) {
      tree.expandPath(new TreePath(treeNodes));
    }
  }

  private static void toggleNode(JTree tree, DefaultMutableTreeNode node) {
    Descriptor descriptor = (Descriptor)node.getUserObject();
    descriptor.setChecked(!descriptor.isChecked());
    tree.repaint();
  }

  @NotNull
  private DefaultMutableTreeNode buildNodes() {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(new Descriptor());
    RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(myProject);
    for (Map.Entry<ConfigurationType, Map<String, List<RunnerAndConfigurationSettings>>> entry : runManager.getConfigurationsGroupedByTypeAndFolder(false).entrySet()) {
      ConfigurationType type = entry.getKey();
      final Icon icon = type.getIcon();
      DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(new ConfigurationTypeDescriptor(type, icon, isConfigurationAssigned(type)));
      root.add(typeNode);
      final Set<String> addedNames = new THashSet<>();
      for (List<RunnerAndConfigurationSettings> list : entry.getValue().values()) {
        for (RunnerAndConfigurationSettings configuration : list) {
          final String configurationName = configuration.getName();
          if (!addedNames.add(configurationName)) {
            // add only the first configuration if more than one has the same name
            continue;
          }
          typeNode.add(new DefaultMutableTreeNode(new ConfigurationDescriptor(configuration.getConfiguration(), isConfigurationAssigned(configuration.getConfiguration()))));
        }
      }
    }
    return root;
  }

  private boolean isConfigurationAssigned(ConfigurationType type) {
    final RunManager runManager = RunManager.getInstance(myProject);
    for (ConfigurationFactory factory : type.getConfigurationFactories()) {
      final RunnerAndConfigurationSettings settings = ((RunManagerImpl)runManager).getConfigurationTemplate(factory);
      if (isConfigurationAssigned(settings.getConfiguration())) return true;
    }
    return false;
  }

  private boolean isConfigurationAssigned(RunConfiguration configuration) {
    for (T task : RunManagerEx.getInstanceEx(myProject).getBeforeRunTasks(configuration, getTaskID())) {
      if (isRunning(task))
        return true;
    }
    return false;
  }

  @Override
  protected void doOKAction() {
    final RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(myProject);
    for (Enumeration nodes = myRoot.depthFirstEnumeration(); nodes.hasMoreElements(); ) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)nodes.nextElement();
      final Descriptor descriptor = (Descriptor)node.getUserObject();
      final boolean isChecked = descriptor.isChecked();

      if (descriptor instanceof ConfigurationTypeDescriptor) {
        ConfigurationTypeDescriptor typeDesc = (ConfigurationTypeDescriptor)descriptor;
        for (ConfigurationFactory factory : typeDesc.getConfigurationType().getConfigurationFactories()) {
          RunnerAndConfigurationSettings settings = runManager.getConfigurationTemplate(factory);
          update(settings.getConfiguration(), isChecked, runManager);
        }
      }
      else if (descriptor instanceof ConfigurationDescriptor) {
        ConfigurationDescriptor configDesc = (ConfigurationDescriptor)descriptor;
        update(configDesc.getConfiguration(), isChecked, runManager);
      }
    }

    RunManagerImpl.getInstanceImpl(myProject).fireBeforeRunTasksUpdated();
    close(OK_EXIT_CODE);
  }

  protected abstract String getTargetDisplayString();

  protected abstract Key<T> getTaskID();

  protected abstract boolean isRunning(T task);

  private void update(RunConfiguration config, boolean enabled, RunManagerImpl runManager) {
    List<BeforeRunTask<?>> tasks = RunManagerImplKt.doGetBeforeRunTasks(config);
    BeforeRunTaskProvider<T> provider = BeforeRunTaskProvider.getProvider(myProject, getTaskID());
    if (provider == null) {
      return;
    }

    T task = provider.createTask(config);
    update(task);
    task.setEnabled(true);
    if (enabled) {
      if (!tasks.contains(task)) {
        tasks = new SmartList<>(tasks);
        tasks.add(task);
      }
    }
    else {
      if (tasks.contains(task)) {
        tasks = new SmartList<>(tasks);
        tasks.remove(task);
      }
    }
    runManager.setBeforeRunTasks(config, tasks);
  }

  protected abstract void update(T task);

  protected abstract void clear(T task);

  private static class Descriptor {
    private boolean myChecked;

    public final boolean isChecked() {
      return myChecked;
    }

    public final void setChecked(boolean checked) {
      myChecked = checked;
    }
  }

  private static final class ConfigurationTypeDescriptor extends Descriptor {
    private final ConfigurationType myConfigurationType;
    private final Icon myIcon;

    ConfigurationTypeDescriptor(ConfigurationType type, Icon icon, boolean isChecked) {
      myConfigurationType = type;
      myIcon = icon;
      setChecked(isChecked);
    }

    public ConfigurationType getConfigurationType() {
      return myConfigurationType;
    }

    public Icon getIcon() {
      return myIcon;
    }
  }

  private static final class ConfigurationDescriptor extends Descriptor {
    private final RunConfiguration myConfiguration;

    ConfigurationDescriptor(RunConfiguration configuration, boolean isChecked) {
      myConfiguration = configuration;
      setChecked(isChecked);
    }

    public ConfigurationType getConfigurationFactory() {
      return myConfiguration.getType();
    }

    public String getName() {
      return myConfiguration.getName();
    }

    public RunConfiguration getConfiguration() {
      return myConfiguration;
    }
  }


  private static final class MyTreeCellRenderer extends JPanel implements TreeCellRenderer {
    private final JLabel myLabel;
    public final JCheckBox myCheckbox;

    MyTreeCellRenderer() {
      super(new BorderLayout());
      myCheckbox = new JCheckBox();
      myLabel = new JLabel();
      add(myCheckbox, BorderLayout.WEST);
      add(myLabel, BorderLayout.CENTER);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
      Descriptor descriptor = (Descriptor)node.getUserObject();

      myCheckbox.setSelected(descriptor.isChecked());

      myCheckbox.setBackground(UIUtil.getTreeBackground());
      setBackground(UIUtil.getTreeBackground(selected, true));
      Color foreground = UIUtil.getTreeForeground(selected, true);
      setForeground(foreground);
      myCheckbox.setForeground(foreground);
      myLabel.setForeground(foreground);
      myCheckbox.setEnabled(true);

      if (descriptor instanceof ConfigurationTypeDescriptor) {
        ConfigurationTypeDescriptor configurationTypeDescriptor = (ConfigurationTypeDescriptor)descriptor;
        myLabel.setFont(tree.getFont());
        myLabel.setText(configurationTypeDescriptor.getConfigurationType().getDisplayName());
        myLabel.setIcon(configurationTypeDescriptor.getIcon());
      }
      else if (descriptor instanceof ConfigurationDescriptor) {
        ConfigurationDescriptor configurationTypeDescriptor = (ConfigurationDescriptor)descriptor;
        myLabel.setFont(tree.getFont());
        myLabel.setText(configurationTypeDescriptor.getName());
        myLabel.setIcon(null);

        if (((ConfigurationTypeDescriptor)((DefaultMutableTreeNode)node.getParent()).getUserObject()).isChecked()) {
          Color foregrnd = tree.getForeground();
          Color backgrnd = tree.getBackground();
          if (foregrnd == null) foregrnd = Color.black;
          if (backgrnd == null) backgrnd = Color.white;

          int red = (foregrnd.getRed() + backgrnd.getRed()) / 2;
          int green = (foregrnd.getGreen() + backgrnd.getGreen()) / 2;
          int blue = (foregrnd.getBlue() + backgrnd.getBlue()) / 2;
          Color halftone = new Color(red, green, blue);
          setForeground(halftone);
          myCheckbox.setForeground(halftone);
          myLabel.setForeground(halftone);
          myCheckbox.setEnabled(false);
        }
      }

      return this;
    }
  }
}
